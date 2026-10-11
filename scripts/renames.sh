#!/bin/sh
# Mechanical 1.21/Forge 51 -> 26.2/Forge 65 renames for freshly copied files.
# Since the project rename the package is net.lukario.circleofinevitability: copy a file from
# reference/java/net/lukario/frogerealm/<path> to src/main/java/net/lukario/circleofinevitability/<path>; the rules below
# fix its package, imports, ForgeRealm and the forgerealm keys. Old progression words (aspect, stage, tier, essence,
# shards) still need scripts/rename-sequence.sh, and that script's gates and storage are hand work (docs/SEQUENCES.md).
# Usage: scripts/renames.sh [file.java ...]   (no arguments = every .java file under src/)
#
# Includes every rule of tools/apply_renames.sh, plus the renames found in later stages.
# Every rule is idempotent: running the script twice changes nothing the second time.
# Keep this list in sync with the API renames table in MIGRATION_NOTES.md.
# Anything that changes code structure (cancelling events, kill/hurt needing a ServerLevel,
# multi-line calls) is NOT done here: fix it by hand and note it in MIGRATION_NOTES.md.
# The phase rules assume Phase.END only (TickEvent.X.Post); a Phase.START check (or a ClientTickEvent listener with
# no phase check, which ran on both phases) needs .Pre by hand.
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)

if [ "$#" -eq 0 ]; then
    set -- $(find "$root/src" -name '*.java' | sort)
fi

# a single call argument with no comma, allowing one level of nested parentheses: TAG, restriction.name()
ARG='(([^(),]|\([^()]*\))+)'

for f in "$@"; do
    abs=$(cd "$(dirname "$f")" && pwd)/$(basename "$f")
    case "$abs" in
        "$root"/reference/*|*/reference/*) echo "refusing to edit $f (reference/ is read-only)" >&2; exit 1 ;;
    esac
    [ -f "$abs" ] || { echo "no such file: $f" >&2; exit 1; }

    sed -i -E \
        -e 's/\bResourceLocation\b/Identifier/g' \
        -e 's/\bforgerealmmod\b/circle_of_inevitability/g' \
        -e 's/\bforgerealm_/circle_of_inevitability_/g' \
        -e 's/net\.lukario\.frogerealm\b/net.lukario.circleofinevitability/g' \
        -e 's/\bForgeRealm\b/CircleOfInevitability/g' \
        -e 's/net\.minecraftforge\.eventbus\.api\.SubscribeEvent\b/net.minecraftforge.eventbus.api.listener.SubscribeEvent/g' \
        -e 's/\bGuiGraphics\b/GuiGraphicsExtractor/g' \
        -e 's/net\.minecraftforge\.eventbus\.api\.EventPriority\b/net.minecraftforge.eventbus.api.listener.Priority/g' \
        -e 's/\bEventPriority\./Priority./g' \
        -e 's/\bTickEvent\.PlayerTickEvent event\b/TickEvent.PlayerTickEvent.Post event/g' \
        -e '/^\s*if \(event\.phase != TickEvent\.Phase\.END\) return;\s*$/d' \
        -e 's/event\.phase != TickEvent\.Phase\.END \|\| //g' \
        -e 's/\bevent\.player\b([^(])/event.player()\1/g' \
        -e 's/\bTickEvent\.LevelTickEvent event\b/TickEvent.LevelTickEvent.Post event/g' \
        -e 's/(^|[^.])\bevent\.level\b([^(])/\1event.level()\2/g' \
        -e 's/\bResourceLocationArgument\b/IdentifierArgument/g' \
        -e 's/\.readResourceLocation\(\)/.readIdentifier()/g' \
        -e 's/\.writeResourceLocation\(/.writeIdentifier(/g' \
        -e 's/\bMobSpawnType\b/EntitySpawnReason/g' \
        -e 's/\blevel\.random\b/level.getRandom()/g' \
        -e 's/\.serverLevel\(\)/.level()/g' \
        -e 's/\bMobEffects\.MOVEMENT_SPEED\b/MobEffects.SPEED/g' \
        -e 's/\bMobEffects\.MOVEMENT_SLOWDOWN\b/MobEffects.SLOWNESS/g' \
        -e 's/\bMobEffects\.DIG_SPEED\b/MobEffects.HASTE/g' \
        -e 's/\bMobEffects\.DIG_SLOWDOWN\b/MobEffects.MINING_FATIGUE/g' \
        -e 's/\bMobEffects\.DAMAGE_BOOST\b/MobEffects.STRENGTH/g' \
        -e 's/\bMobEffects\.HEAL\b/MobEffects.INSTANT_HEALTH/g' \
        -e 's/\bMobEffects\.HARM\b/MobEffects.INSTANT_DAMAGE/g' \
        -e 's/\bMobEffects\.JUMP\b/MobEffects.JUMP_BOOST/g' \
        -e 's/\bMobEffects\.CONFUSION\b/MobEffects.NAUSEA/g' \
        -e 's/\bMobEffects\.DAMAGE_RESISTANCE\b/MobEffects.RESISTANCE/g' \
        -e "/ArgumentType\./!s/\.getInt\($ARG\)/.getIntOr(\1, 0)/g" \
        -e "/ArgumentType\./!s/\.getLong\($ARG\)/.getLongOr(\1, 0L)/g" \
        -e "/ArgumentType\./!s/\.getFloat\($ARG\)/.getFloatOr(\1, 0f)/g" \
        -e "/ArgumentType\./!s/\.getDouble\($ARG\)/.getDoubleOr(\1, 0.0)/g" \
        -e "/ArgumentType\./!s/\.getBoolean\($ARG\)/.getBooleanOr(\1, false)/g" \
        -e "/ArgumentType\./!s/\.getString\($ARG\)/.getStringOr(\1, \"\")/g" \
        -e "s/\.getCompound\($ARG\)/.getCompoundOrEmpty(\1)/g" \
        -e 's/\.getAllKeys\(\)/.keySet()/g' \
        -e 's/\.isClientSide\b([^(])/.isClientSide()\1/g' \
        -e 's/\.putUUID\(([^,()]+), /.store(\1, UUIDUtil.CODEC, /g' \
        -e 's/\.hasUUID\(([^()]+)\)/.read(\1, UUIDUtil.CODEC).isPresent()/g' \
        -e 's/\.getUUID\(([^()]+)\)/.read(\1, UUIDUtil.CODEC).orElseThrow()/g' \
        -e 's/\.displayClientMessage\((.*), true\);/.sendOverlayMessage(\1);/' \
        -e 's/\.displayClientMessage\((.*), false\);/.sendSystemMessage(\1);/' \
        -e 's/\b([a-z]+) -> \1\.hasPermission\(0\)/Commands.hasPermission(Commands.LEVEL_ALL)/g' \
        -e 's/\b([a-z]+) -> \1\.hasPermission\(1\)/Commands.hasPermission(Commands.LEVEL_MODERATORS)/g' \
        -e 's/\b([a-z]+) -> \1\.hasPermission\(2\)/Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)/g' \
        -e 's/\b([a-z]+) -> \1\.hasPermission\(3\)/Commands.hasPermission(Commands.LEVEL_ADMINS)/g' \
        -e 's/\b([a-z]+) -> \1\.hasPermission\(4\)/Commands.hasPermission(Commands.LEVEL_OWNERS)/g' \
        -e 's/\bTickEvent\.ClientTickEvent event\b/TickEvent.ClientTickEvent.Post event/g' \
        -e 's/\.getTimer\(\)\.getGameTimeDeltaPartialTick\(/.getDeltaTracker().getGameTimeDeltaPartialTick(/g' \
        -e 's/\bTextureSheetParticle\b/SingleQuadParticle/g' \
        -e 's/\bLightTexture\.FULL_BRIGHT\b/LightCoordsUtil.FULL_BRIGHT/g' \
        -e 's/^import net\.minecraft\.client\.renderer\.LightTexture;/import net.minecraft.util.LightCoordsUtil;/' \
        -e '/^\s*RenderSystem\.(enableBlend|disableBlend|defaultBlendFunc)\(\);\s*$/d' \
        -e 's/\bguiGraphics\.drawString\(/guiGraphics.text(/g' \
        -e 's/\bguiGraphics\.drawCenteredString\(/guiGraphics.centeredText(/g' \
        -e 's/\.pose\(\)\.pushPose\(\)/.pose().pushMatrix()/g' \
        -e 's/\.pose\(\)\.popPose\(\)/.pose().popMatrix()/g' \
        -e 's/\bMinecraft\.getInstance\(\)\.setScreen\(/Minecraft.getInstance().gui.setScreen(/g' \
        -e '/\bEntityType\.(STREAM_)?CODEC\b/!s/\bEntityType\.([A-Z][A-Z0-9_]*)\b/EntityTypes.\1/g' \
        "$abs"

    # the EntityTypes rule above needs this import
    if grep -q '\bEntityTypes\.' "$abs" && ! grep -q '^import net\.minecraft\.world\.entity\.EntityTypes;' "$abs"; then
        sed -i -E '0,/^import /s//import net.minecraft.world.entity.EntityTypes;\nimport /' "$abs"
    fi

    # the UUID rules above need this import
    if grep -q 'UUIDUtil\.CODEC' "$abs" && ! grep -q '^import net\.minecraft\.core\.UUIDUtil;' "$abs"; then
        sed -i -E '0,/^import /s//import net.minecraft.core.UUIDUtil;\nimport /' "$abs"
    fi
done
