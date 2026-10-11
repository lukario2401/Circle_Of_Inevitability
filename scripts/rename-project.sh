#!/bin/sh
# Old project name -> Circle Of Inevitability, everywhere outside reference/ and the history in MIGRATION_NOTES.md.
#   package net.lukario.frogerealm -> net.lukario.circleofinevitability (folders moved with git mv)
#   class ForgeRealm -> CircleOfInevitability; keys forgerealmmod / forgerealm_x -> circle_of_inevitability / circle_of_inevitability_x;
#   FORGEREALM constants -> CIRCLE_OF_INEVITABILITY; prose "ForgeRealm", "Forge Realm", "Forged Realm" -> "Circle Of Inevitability".
# Usage: scripts/rename-project.sh   Idempotent (a second run changes nothing). Never touches reference/.
# Kept on purpose: the old project's name where it identifies the old 1.21 code (ForgeRealm1, lukario2401/ForgeRealm1,
# reference/java/net/lukario/frogerealm/, "the old code's forgerealmmod" in CLAUDE.md), MIGRATION_NOTES.md history,
# and the rules in scripts/renames.sh and this file, which have to name the old spellings to replace them.
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
case "$(pwd)" in */reference|*/reference/*) echo "refusing to run inside reference/" >&2; exit 1 ;; esac

OLD=src/main/java/net/lukario/frogerealm
NEW=src/main/java/net/lukario/circleofinevitability
if [ -d "$OLD" ]; then
    [ -e "$NEW" ] && { echo "both $OLD and $NEW exist; merge by hand" >&2; exit 1; }
    git mv "$OLD" "$NEW"
fi
[ -e "$NEW/ForgeRealm.java" ] && git mv "$NEW/ForgeRealm.java" "$NEW/CircleOfInevitability.java"

files=$( { find src -type f \( -name '*.java' -o -name '*.json' -o -name '*.toml' -o -name '*.mcmeta' -o -name '*.md' \);
           ls build.gradle gradle.properties settings.gradle CLAUDE.md docs/*.md scripts/rename-sequence.sh 2>/dev/null; } | sort -u)
for f in $files; do
    case "$f" in reference/*|*/reference/*) echo "refusing $f" >&2; exit 1 ;; esac
    perl -pi -e '
        my $f = $ARGV;
        next if $f eq "CLAUDE.md" && /old code.s forgerealmmod|reference\/java\/net\/lukario\/frogerealm/;
        s/net\.lukario\.frogerealm\b/net.lukario.circleofinevitability/g;
        s/(?<!reference\/java\/)net\/lukario\/frogerealm\b/net\/lukario\/circleofinevitability/g;
        s/\bforgerealmmod\b/circle_of_inevitability/g;
        s/\bforgerealm_/circle_of_inevitability_/g;
        s/\bFORGEREALM(_?)/CIRCLE_OF_INEVITABILITY$1/g;
        s/\bforge_realm\b/circle_of_inevitability/g;
        s/\bForge(?:d)? Realm\b/Circle Of Inevitability/g;
        if ($f =~ /\.(java|gradle)$/) { s/\bForgeRealm\b/CircleOfInevitability/g; }
        else { s/\bForgeRealm\b(?!1)/Circle Of Inevitability/g; }
        s/\bfrogerealm\b/circleofinevitability/g;
    ' "$f"
done
echo "rename-project: done"
