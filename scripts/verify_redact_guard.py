#!/usr/bin/env python3
"""Screen a UI tree for a live GitHub device code before anything is retained.

Reads the raw `uiautomator dump /dev/tty` stream on stdin (XML followed by
the tool's trailer line), never touches disk, and writes the XML to stdout
only when it is safe to keep. Exit 0 safe, 1 device-code screen detected
(nothing written), 2 malformed. Stdlib only.
"""
import sys
import xml.etree.ElementTree as ET

CODE_SCREEN_MARKERS = (
    "Enter this code on GitHub",
    "Copy code & open GitHub",
    "Code expires in about",
)


def main() -> int:
    raw = sys.stdin.read()
    start = raw.find("<?xml")
    end = raw.rfind("</hierarchy>")
    if start < 0 or end < 0:
        print("redact-guard: no UI tree on stdin", file=sys.stderr)
        return 2
    xml = raw[start : end + len("</hierarchy>")]
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        print("redact-guard: malformed UI tree", file=sys.stderr)
        return 2
    texts = [(n.get("text") or "") for n in root.iter("node")]
    for marker in CODE_SCREEN_MARKERS:
        if any(marker in t for t in texts):
            print(f"redact-guard: GitHub device-code screen detected ({marker!r}); nothing written", file=sys.stderr)
            return 1
    sys.stdout.write(xml)
    return 0


if __name__ == "__main__":
    sys.exit(main())
