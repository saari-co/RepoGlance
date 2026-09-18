#!/usr/bin/env python3
"""Refuse to persist a UI tree that carries a live GitHub device code.

Used by bin/verify-repoglance before any dump or capture is written. Exit 0
when the tree is safe to keep, 1 when it shows the device-code screen, 2 on
a malformed tree. Stdlib only.
"""
import sys
import xml.etree.ElementTree as ET

CODE_SCREEN_MARKERS = (
    "Enter this code on GitHub",
    "Copy code & open GitHub",
    "Code expires in about",
)


def main(path: str) -> int:
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError):
        return 2
    texts = [(n.get("text") or "") for n in root.iter("node")]
    for marker in CODE_SCREEN_MARKERS:
        if any(marker in t for t in texts):
            print(f"redact-guard: GitHub device-code screen detected ({marker!r}); refusing to persist", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
