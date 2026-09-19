#!/usr/bin/env python3
"""Fake-adb checks for bin/verify-repoglance, run by `./gradlew check`.

The helper runs under `set -euo pipefail`, so a pipeline whose first command
fails (a package that is not installed, a dumpsys line that is missing) used
to exit 1 with no message. Each case copies the helper and the device
registry into a temporary root, puts a scripted `adb` first on PATH, and
asserts the exit code and the output. Every non-zero exit must say why on
stderr.

Stdlib only; needs bash and perl, like the helper itself. Exit 0 on pass, 1
with every failure listed.
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HELPER = ROOT / "bin" / "verify-repoglance"
DEVICES = ROOT / ".claude" / "skills" / "verify-repoglance" / "devices.tsv"
SERIAL = "59151FDCG000JA"
PKG = "co.saari.repoglance"

# The phone's state lives in $FAKE_ADB_STATE: `installed.apk` exists when the
# package is installed, `started` after an `am start`, and a file named after
# a knob (e.g. `no-wakefulness`) switches that behaviour on. Every call is
# appended to `log`.
FAKE_ADB = r"""#!/usr/bin/env bash
S="$FAKE_ADB_STATE"; PKG=co.saari.repoglance
echo "$*" >> "$S/log"
[ "${1:-}" = "-s" ] && shift 2
on() { [ -e "$S/$1" ]; }
installed() { [ -e "$S/installed.apk" ]; }
case "$*" in
  devices) printf 'List of devices attached\n%s\tdevice\n\n' "$(cat "$S/serial")" ;;
  "shell getprop ro.serialno") cat "$S/serial" ;;
  "shell getprop ro.product.model") on shell-dead && { echo "adb: device offline" >&2; exit 1; }; echo "Pixel 10 Pro Fold" ;;
  "shell pm path $PKG") installed && echo "package:/data/app/~~x/$PKG-y/base.apk" || exit 1 ;;
  "shell dumpsys package $PKG") installed && echo "    versionName=0.9.0" || echo "Unable to find package: $PKG" ;;
  pull\ *) installed && cp "$S/installed.apk" "$3" || exit 1 ;;
  install\ -r\ *) on install-fails && { echo "adb: failed to install $3: Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]" >&2; exit 1; }
    cp "$3" "$S/installed.apk"; echo "Performing Streamed Install"; echo "Success" ;;
  "shell cmd package resolve-activity --components -n $PKG/.devlaunch.ScenarioLaunchActivity")
    installed && echo "$PKG/.devlaunch.ScenarioLaunchActivity" || { echo "No activity found"; exit 1; } ;;
  "shell cmd device_state state") on no-device-state && exit 1; echo "Committed state: DeviceState{identifier=0, name='CLOSED', app_accessible=true}" ;;
  "shell dumpsys power") on no-wakefulness || echo "  mWakefulness=Awake" ;;
  "shell dumpsys trust") on no-trust || echo "  deviceLocked=0" ;;
  "shell am force-stop "*) ;;
  "shell am start "*) installed || { echo "Error: Activity class does not exist." >&2; exit 1; }; touch "$S/started" ;;
  "shell dumpsys activity activities") on no-top-activity && exit 0
    on started && echo "  topResumedActivity=ActivityRecord{1a2b u0 $PKG/.MainActivity t42}" ;;
  "shell uiautomator dump /sdcard/verify-ui.xml") echo "UI hierchary dumped to: /sdcard/verify-ui.xml" ;;
  "shell cat /sdcard/verify-ui.xml") echo '<hierarchy><node resource-id="repoglance:fixture-home"/></hierarchy>' ;;
  "exec-out uiautomator dump /dev/tty") on no-ui-tree && exit 1
    printf '<?xml version="1.0"?><hierarchy><node resource-id="repoglance:fixture-home" text="acme/rocket" bounds="[0,0][10,10]"/></hierarchy>UI hierchary dumped to: /dev/tty\n' ;;
  "shell settings get global airplane_mode_on") echo 0 ;;
  *) echo "fake adb: unhandled: $*" >&2; exit 99 ;;
esac
"""


class Phone:
    def __init__(self, base: Path, knobs: tuple[str, ...], installed: bytes | None, local_apk: bytes | None):
        self.root = base / "repo"
        self.state = base / "state"
        bindir = base / "fakebin"
        for d in (self.root / "bin", self.root / DEVICES.parent.relative_to(ROOT), self.state, bindir):
            d.mkdir(parents=True)
        shutil.copy2(HELPER, self.root / "bin" / "verify-repoglance")
        shutil.copy2(DEVICES, self.root / DEVICES.relative_to(ROOT))
        (self.root / "scripts").mkdir()
        shutil.copy2(ROOT / "scripts" / "verify_redact_guard.py", self.root / "scripts")
        (bindir / "adb").write_text(FAKE_ADB)
        (bindir / "adb").chmod(0o755)
        (self.state / "serial").write_text(SERIAL)
        (self.state / "log").write_text("")
        for k in knobs:
            (self.state / k).touch()
        if installed is not None:
            (self.state / "installed.apk").write_bytes(installed)
        if local_apk is not None:
            apk = self.root / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
            apk.parent.mkdir(parents=True)
            apk.write_bytes(local_apk)
        self.env = {
            **os.environ,
            "PATH": f"{bindir}{os.pathsep}{os.environ['PATH']}",
            "FAKE_ADB_STATE": str(self.state),
            "VERIFY_SERIAL": SERIAL,
            "VERIFY_RUN_ID": "fake-adb",
            "PHONE_PROOF": str(base / "no-phone-proof.py"),
        }

    def run(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["bash", str(self.root / "bin" / "verify-repoglance"), *args],
            env=self.env, capture_output=True, text=True, timeout=60,
        )

    def log(self) -> str:
        return (self.state / "log").read_text()


# name, argv, knobs, installed apk bytes, local apk bytes, expected exit,
# strings stdout must contain, strings stderr must contain, extra check
CASES = [
    ("doctor: package absent, no local build", ["doctor"], (), None, None, 1,
     ["package=absent", "\napk_device=\n"],
     ["doctor: RepoGlance is not installed; run 'launch' to install"], None),
    ("doctor: package absent, local build present", ["doctor"], (), None, b"apk-v1", 1,
     ["package=absent", "\napk_device=\n"],
     ["doctor: RepoGlance is not installed; run 'launch' to install"], None),
    ("doctor: installed build matches", ["doctor"], (), b"apk-v1", b"apk-v1", 0,
     ["package=co.saari.repoglance versionName=0.9.0", "scenario_launcher_present=yes", "posture=CLOSED"], [], None),
    ("doctor: installed build differs", ["doctor"], (), b"apk-v0", b"apk-v1", 1,
     [], ["does not match the local build"], None),
    ("doctor: adb shell dead", ["doctor"], ("shell-dead",), b"apk-v1", b"apk-v1", 1,
     [], ["doctor: adb shell failed"], None),
    ("doctor: no wakefulness line", ["doctor"], ("no-wakefulness",), b"apk-v1", b"apk-v1", 1,
     ["wakefulness=unknown"], ["awake and unlocked"], None),
    ("doctor: no trust line or device_state", ["doctor"], ("no-trust", "no-device-state"), b"apk-v1", b"apk-v1", 1,
     ["posture=unknown", "device_locked=unknown"], ["awake and unlocked"], None),
    ("launch: package absent installs the local build",
     ["launch", "RATE_LIMITED", "navigator", "acme/rocket", "BOTH"], (), None, b"apk-v1", 0,
     ["co.saari.repoglance/.MainActivity"], [],
     lambda p: "install -r " in p.log() and (p.state / "installed.apk").read_bytes() == b"apk-v1"),
    ("launch: matching build is not reinstalled", ["launch", "MIXED", "navigator"], (), b"apk-v1", b"apk-v1", 0,
     ["co.saari.repoglance/.MainActivity"], [], lambda p: "install -r " not in p.log()),
    ("launch: install fails", ["launch", "MIXED", "navigator"], ("install-fails",), None, b"apk-v1", 1,
     [], ["launch: install failed", "INSTALL_FAILED_INSUFFICIENT_STORAGE"], None),
    ("launch: no local build", ["launch", "MIXED", "navigator"], (), None, None, 1,
     [], ["launch: build first"], None),
    ("launch: no top activity", ["launch", "MIXED", "navigator"], ("no-top-activity",), b"apk-v1", b"apk-v1", 1,
     [], ["launch: failed to reach navigator after 2 attempts"], None),
    ("capture: no wakefulness line", ["capture", "x"], ("no-wakefulness",), b"apk-v1", b"apk-v1", 1,
     [], ["capture: screen is unknown, not Awake"], None),
    ("dump: tree passes the guard", ["dump", "home"], (), b"apk-v1", b"apk-v1", 0, ["home.txt"], [], None),
    ("dump: no UI tree", ["dump", "home"], ("no-ui-tree",), b"apk-v1", b"apk-v1", 1,
     [], ["dump: no UI tree from the phone"], None),
    ("capture: no UI tree", ["capture", "x"], ("no-ui-tree",), b"apk-v1", b"apk-v1", 1,
     [], ["capture: no UI tree from the phone"], None),
    ("usage needs no phone", [], (), None, None, 2, [], ["bin/verify-repoglance doctor"],
     lambda p: p.log() == ""),
    ("cleanup: package absent", ["cleanup"], (), None, None, 0, ["cleanup: app stopped"], [], None),
]


def main() -> int:
    failures: list[str] = []
    for name, argv, knobs, installed, local_apk, code, outs, errs, extra in CASES:
        with tempfile.TemporaryDirectory(prefix="verify-helper-") as tmp:
            phone = Phone(Path(tmp), knobs, installed, local_apk)
            r = phone.run(*argv)
            why = []
            if r.returncode != code:
                why.append(f"exit {r.returncode}, want {code}")
            if r.returncode != 0 and not r.stderr.strip():
                why.append("non-zero exit with nothing on stderr")
            why += [f"stdout lacks {s!r}" for s in outs if s not in r.stdout]
            why += [f"stderr lacks {s!r}" for s in errs if s not in r.stderr]
            if "unhandled" in r.stderr:
                why.append("fake adb got a call it does not script")
            if extra and not extra(phone):
                why.append("extra check failed")
            status = "ok  " if not why else "FAIL"
            print(f"{status} {name}")
            if why:
                failures.append(f"{name}: {'; '.join(why)}\n--- stdout\n{r.stdout}--- stderr\n{r.stderr}--- adb log\n{phone.log()}")
    for f in failures:
        print(f, file=sys.stderr)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
