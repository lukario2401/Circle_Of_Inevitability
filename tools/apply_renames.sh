#!/bin/sh
# Applies the "by script" renames from MIGRATION_NOTES.md to freshly copied files.
# Usage: tools/apply_renames.sh <file.java>...
# Keep this list in sync with the API renames table in MIGRATION_NOTES.md.
set -eu

[ "$#" -gt 0 ] || { echo "usage: $0 <file>..." >&2; exit 1; }

for f in "$@"; do
    case "$f" in
        reference/*|*/reference/*) echo "refusing to edit $f (reference/ is read-only)" >&2; exit 1 ;;
    esac
    sed -i \
        -e 's/\bResourceLocation\b/Identifier/g' \
        -e 's/\bforgerealmmod\b/circle_of_inevitability/g' \
        -e 's/net\.minecraftforge\.eventbus\.api\.SubscribeEvent\b/net.minecraftforge.eventbus.api.listener.SubscribeEvent/g' \
        "$f"
done
