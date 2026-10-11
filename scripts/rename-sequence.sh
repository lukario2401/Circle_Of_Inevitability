#!/bin/sh
# Old progression vocabulary -> Lord of the Mysteries style vocabulary (docs/SEQUENCES.md).
#   aspect -> Pathway, ascension stage -> Sequence (9 weakest .. 0 strongest, Sequence = 9 - old stage),
#   soul essence -> Spirituality, aspect tier -> power scale (derived from the Sequence), soul shards -> removed.
# Usage: scripts/rename-sequence.sh   (works on src/ only; never touches reference/)
# Idempotent: every rule maps a word that no longer exists after the first run, so a second run changes nothing.
# Hand work done after the first run (not by this script): SoulCore storage, SoulHandler, SoulCommands,
# deleting SoulShardPower, Beyonder helper. See MIGRATION_NOTES.md (Sequence rework).
#
# Exceptions (words kept on purpose, or renamed to a neutral word because they never meant the progression):
#   Pale Emperor curse "stage"      -> "depth"   (how deep a curse has sunk, not the player's progression)
#   Hand Of Order BindTier          -> BindLevel (bind strength chosen by distance)
#   particle / model "shard"        -> "splinter" (a texture shape: fx/shard.png -> fx/splinter.png)
#   comment wording "in stages"     -> "in steps" (Later, Zone, ModelFx, ParticleShapes, AttendantOfMysteries)
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
case "$(pwd)" in */reference|*/reference/*) echo "refusing to run inside reference/" >&2; exit 1 ;; esac

J=src/main/java/net/lukario/frogerealm
A=src/main/resources/assets/circle_of_inevitability

mv_if() { # git mv only when the source still exists (idempotent)
    if [ -e "$1" ] && [ ! -e "$2" ]; then mkdir -p "$(dirname "$2")"; git mv "$1" "$2"; fi
}

# ---------- files and folders ----------
if [ -d "$J/shadow_slave/soul_shards" ]; then
    mkdir -p "$J/shadow_slave/beyonder"
    for f in "$J"/shadow_slave/soul_shards/*; do git mv "$f" "$J/shadow_slave/beyonder/"; done
    rmdir "$J/shadow_slave/soul_shards" 2>/dev/null || true
fi
B=$J/shadow_slave/beyonder
mv_if "$B/AspectSync.java"                 "$B/PathwaySync.java"
mv_if "$B/SoulShardCommands.java"          "$B/SoulCommands.java"
[ -e "$B/SoulShardPower.java" ] && git rm -qf "$B/SoulShardPower.java"
mv_if "$J/network/CPlayerAspectPacket.java" "$J/network/CPlayerPathwayPacket.java"
mv_if "$J/network/CSyncAspectPacket.java"   "$J/network/CSyncPathwayPacket.java"
mv_if "$J/client/ClientAspectData.java"     "$J/client/ClientPathwayData.java"
mv_if "$J/client/AspectHudIcon.java"        "$J/client/PathwayHudIcon.java"
mv_if "$J/client/animation/PlayerAspectCache.java" "$J/client/animation/PlayerPathwayCache.java"
mv_if "$A/textures/gui/aspect_icons"        "$A/textures/gui/pathway_icons"
mv_if "$A/textures/particle/fx/shard.png"   "$A/textures/particle/fx/splinter.png"

# ---------- text ----------
find src -type f \( -name '*.java' -o -name '*.json' \) | sort | while read -r f; do
    case "$f" in *reference/*) echo "refusing $f" >&2; exit 1 ;; esac
    perl -0pi -e '
        my $f = $ARGV;
        # dropped item leftovers in the lang file
        s/^\s*"item\.circle_of_inevitability\.(soulessence|energy_shard|depleted_energy_shard)":[^\n]*\n//mg;
        s/\(their effect classes live in the dropped aspects;/(their effect classes live in classes of the old project that were dropped;/g;

        # packages and classes
        s/shadow_slave\.soul_shards/shadow_slave.beyonder/g;
        s/\bSoulShardCommands\b/SoulCommands/g;
        s/\bAspectSync\b/PathwaySync/g;
        s/\bCPlayerAspectPacket\b/CPlayerPathwayPacket/g;
        s/\bCSyncAspectPacket\b/CSyncPathwayPacket/g;
        s/\bClientAspectData\b/ClientPathwayData/g;
        s/\bAspectHudIcon\b/PathwayHudIcon/g;
        s/\bPlayerAspectCache\b/PlayerPathwayCache/g;

        # ability gates: old "stage >= N" is "Sequence <= 9 - N"
        s/SoulCore\.getAscensionStage\(player\) < (\d)\)/"!Beyonder.hasSequence(player, " . (9 - $1) . "))"/ge;
        s/SoulCore\.getAscensionStage\(player\) < (\w+)_STAGE\)/!Beyonder.hasSequence(player, $1_SEQUENCE))/g;
        s/private static final int (\w+)_STAGE = (\d);/"private static final int $1_SEQUENCE = " . (9 - $2) . ";"/ge;
        # only on the first run, while ready() still compares against the old stage
        if (/getAscensionStage\(player\) >= stage/) {
            s/ready\(player, bypassClassCheck, (\d)\)/"ready(player, bypassClassCheck, " . (9 - $1) . ")"/ge;
        }
        s/\(ascension stage (\d)\)/"(Sequence " . (9 - $1) . ")"/ge;
        s/(Ability \d - [^(\n]*)\(stage (\d)\)/"$1(Sequence " . (9 - $2) . ")"/ge;
        s/has reached the ascension stage the ability needs/has reached the Sequence the ability needs/g;
        s/boolean bypassClassCheck, int stage\)/boolean bypassClassCheck, int sequence)/g;
        s/return SoulCore\.getAscensionStage\(player\) >= stage;/return Beyonder.hasSequence(player, sequence);/g;
        # per-stage scaling: same numbers, input is the power level (the old stage, derived from the Sequence)
        s/stars cap at ascension stage/stars cap at the power level (Beyonder.powerLevel)/g;
        s/\bascensionStage\b/powerLevel/g;
        s/SoulCore\.getAscensionStage\(player\)/Beyonder.powerLevel(player)/g;

        # unrelated words, made neutral
        if ($f =~ m{/PaleEmperor\.java$}) {
            s/CURSE_STAGES/CURSE_DEPTHS/g; s/curseStage/curseDepth/g;
            s/one stage deeper/one step deeper/g; s/a stage deeper/a step deeper/g;
            s/\bstages\b/depths/g; s/\bstage\b/depth/g;
        }
        if ($f =~ m{/(Later|Zone|ModelFx|ParticleShapes|AttendantOfMysteries)\.java$}) {
            s/\bstages\b/steps/g; s/\bstage\b/step/g;
        }
        if ($f =~ m{/HandOfOrder\.java$}) {
            s/\bBindTier\b/BindLevel/g; s/\bBIND_TIERS\b/BIND_LEVELS/g; s/\btier\b/bindLevel/g;
        }
        s/"fx\/shard"/"fx\/splinter"/g;
        s/\b(GOLD|HIT|ICE)_SHARD\b/$1_SPLINTER/g; s/\bSHARD\b/SPLINTER/g;
        s/"shard(\d)"/"splinter$1"/g;
        s/\bshards\b/splinters/g unless $f =~ m{/beyonder/};
        s/\bshard\b/splinter/g unless $f =~ m{/beyonder/};

        # methods, fields, keys
        s/\bgetAspect\b/getPathway/g; s/\bsetAspect\b/setPathway/g; s/\bhasAspect\b/hasPathway/g;
        s/\baspectOf\b/pathwayOf/g; s/\bforAspect\b/forPathway/g;
        s/\bASPECT_KEY\b/PATHWAY_KEY/g; s/forgerealm_combo_aspect/forgerealm_combo_pathway/g;
        s/\bLAST_SENT_ASPECT\b/LAST_SENT_PATHWAY/g; s/onAspectSync/onPathwaySync/g;
        s/\bASPECTS\b/PATHWAYS/g; s/\bASPECT\b/PATHWAY/g;
        s/aspect_icons/pathway_icons/g;
        s/\bnewAspect\b/newPathway/g; s/\baspectName\b/pathwayName/g; s/\baspect_name\b/pathway_name/g;
        s/\bgetSoulEssence\b/getSpirituality/g; s/\bsetSoulEssence\b/setSpirituality/g;
        s/\bpayEssence\b/paySpirituality/g;

        # words in comments, javadoc and messages
        s/\ban aspect\b/a pathway/g; s/\bAn aspect\b/A pathway/g;
        s/\baspects\b/pathways/g; s/\bAspects\b/Pathways/g;
        s/\baspect\b/pathway/g; s/\bAspect\b/Pathway/g;
        s/\bSoul Essence\b/Spirituality/g; s/\bsoul essence\b/spirituality/g; s/\bSoul essence\b/Spirituality/g;
        s/\bRefund essence\b/Refund spirituality/g;
        s/\bessence\b/spirituality/g; s/\bEssence\b/Spirituality/g;
    ' "$f"
done
# the gates use Beyonder: import it next to SoulCore where needed
grep -rl 'Beyonder\.' src --include='*.java' | while read -r f; do
    grep -q '^package net.lukario.frogerealm.shadow_slave.beyonder;' "$f" && continue
    grep -q '^import net.lukario.frogerealm.shadow_slave.beyonder.Beyonder;' "$f" && continue
    sed -i '0,/^import net.lukario.frogerealm.shadow_slave.beyonder.SoulCore;/s//import net.lukario.frogerealm.shadow_slave.beyonder.Beyonder;\nimport net.lukario.frogerealm.shadow_slave.beyonder.SoulCore;/' "$f"
done
echo "rename-sequence: done"
