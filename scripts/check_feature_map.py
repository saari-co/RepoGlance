#!/usr/bin/env python3
"""Structural gate for the verify-repoglance skill (GrillTrack feature-map-015).

Fails when the feature map cannot be trusted as a navigation contract:
- SKILL.md is missing a required section;
- features/README.md lists a file that does not exist, or a feature file is
  not listed in the index;
- a feature file lacks the four fixed H2s in order, or has extra H2s;
- a helper the skill names is missing or not executable;
- the .cursor symlink does not resolve to the skill directory.

Stdlib only. Exit 0 on pass, 1 with every failure listed.
"""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SKILL = ROOT / ".claude" / "skills" / "verify-repoglance"
SKILL_SECTIONS = ["Launch", "Doctor", "Drive", "Evidence", "Cleanup", "Helpers"]
FEATURE_H2S = [
    "Sub-features",
    "How to get to it (user POV)",
    "Driving it with verify-repoglance",
    "Gotchas",
]


def h2s(text: str) -> list[str]:
    return [m.group(1).strip() for m in re.finditer(r"^## (.+)$", text, re.M)]


def main() -> int:
    failures: list[str] = []
    skill_md = SKILL / "SKILL.md"
    if not skill_md.exists():
        failures.append(f"missing {skill_md.relative_to(ROOT)}")
    else:
        text = skill_md.read_text()
        if not text.startswith("---"):
            failures.append("SKILL.md: missing YAML frontmatter")
        if not re.search(r"^name:\s*verify-repoglance\s*$", text, re.M):
            failures.append("SKILL.md: frontmatter name must be verify-repoglance")
        found = h2s(text)
        for section in SKILL_SECTIONS:
            if section not in found:
                failures.append(f"SKILL.md: missing '## {section}'")
        for helper in sorted(set(re.findall(r"`(bin/[A-Za-z0-9_./-]+)", text))):
            path = ROOT / helper
            if not path.exists():
                failures.append(f"SKILL.md names helper {helper} which does not exist")
            elif not os.access(path, os.X_OK):
                failures.append(f"helper {helper} is not executable")

    features = SKILL / "features"
    index = features / "README.md"
    if not index.exists():
        failures.append(f"missing {index.relative_to(ROOT)}")
        listed: set[str] = set()
    else:
        listed = set(re.findall(r"\]\(\./([A-Za-z0-9_-]+\.md)\)", index.read_text()))
        for name in sorted(listed):
            if not (features / name).exists():
                failures.append(f"features/README.md lists {name} which does not exist")
    on_disk = {p.name for p in features.glob("*.md") if p.name != "README.md"} if features.exists() else set()
    for name in sorted(on_disk - listed):
        failures.append(f"features/{name} is not listed in features/README.md")
    for name in sorted(on_disk):
        text = (features / name).read_text()
        if not re.match(r"^# .+", text):
            failures.append(f"features/{name}: must start with an H1 title")
        found = h2s(text)
        if found != FEATURE_H2S:
            failures.append(
                f"features/{name}: H2s must be exactly {FEATURE_H2S}, found {found}"
            )
        drive = re.search(r"^## Driving it with verify-repoglance\n(.*?)(?=^## |\Z)", text, re.M | re.S)
        if drive:
            first_line = next((line for line in drive.group(1).splitlines() if line.strip()), "")
            if first_line.strip() != "Preconditions:":
                failures.append(
                    f"features/{name}: driving section must start with 'Preconditions:' "
                    f"(first non-blank line is {first_line.strip()!r})"
                )

    link = ROOT / ".cursor" / "skills" / "verify-repoglance"
    if not link.is_symlink():
        failures.append(".cursor/skills/verify-repoglance must be a symlink to the skill directory")
    elif link.resolve() != SKILL.resolve():
        failures.append(".cursor/skills/verify-repoglance does not resolve to .claude/skills/verify-repoglance")

    if failures:
        print("\n".join(f"feature-map: {f}" for f in failures), file=sys.stderr)
        return 1
    print(f"feature-map: ok ({len(on_disk)} features, {len(SKILL_SECTIONS)} skill sections)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
