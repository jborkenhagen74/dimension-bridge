#!/usr/bin/env python3
"""Adds a Fabric target to DimensionBridge's version matrix.

Example:
  python3 tools/add-fabric-target.py 26.3 unobfuscated 25 plain
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

VERSION_PATTERN = re.compile(r"[0-9A-Za-z][0-9A-Za-z._-]{0,31}")
VALID_LOOM = {"remap", "plain"}
VALID_JAVA = {17, 21, 25}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("minecraft")
    parser.add_argument("family")
    parser.add_argument("java", type=int)
    parser.add_argument("loom", choices=sorted(VALID_LOOM))
    args = parser.parse_args()

    if not VERSION_PATTERN.fullmatch(args.minecraft):
        parser.error("Ungültiger Minecraft-Versionsname")
    if not VERSION_PATTERN.fullmatch(args.family):
        parser.error("Ungültiger Familienname")
    if args.java not in VALID_JAVA:
        parser.error(f"Java muss eine der Versionen {sorted(VALID_JAVA)} sein")

    root = Path(__file__).resolve().parents[1]
    matrix_path = root / "gradle" / "fabric-targets.json"
    family_dir = root / "fabric-families" / args.family / "src" / "main" / "java"
    if not family_dir.is_dir():
        parser.error(f"API-Familie existiert nicht: {family_dir}")

    targets = json.loads(matrix_path.read_text(encoding="utf-8"))
    if any(item["minecraft"] == args.minecraft for item in targets):
        parser.error(f"Minecraft {args.minecraft} ist bereits enthalten")

    targets.append({
        "minecraft": args.minecraft,
        "family": args.family,
        "java": args.java,
        "loom": args.loom,
    })
    matrix_path.write_text(json.dumps(targets, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    project_dir = root / "fabric-versions" / args.minecraft
    project_dir.mkdir(parents=True, exist_ok=False)
    (project_dir / "build.gradle").write_text(
        "apply from: rootProject.file('gradle/fabric-version.gradle')\n",
        encoding="utf-8",
    )

    print(f"Fabric-Ziel {args.minecraft} ({args.family}, Java {args.java}, {args.loom}) hinzugefügt.")
    print("Danach eine passende Fabric-API-Version in gradle.properties pinnen oder die dynamische Auflösung testen.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
