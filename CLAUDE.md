# Circle Of Inevitability (Minecraft 26.2 port)

## What this is
A slimmed port of my Forge mod to Minecraft 26.2 (Forge 65.1.0, Java 25, official Mojang names, no mappings plugin).
Old project, read-only reference copy inside this repo (Minecraft 1.21, Forge 51): reference/java/net/lukario/frogerealm/
Kept: the 7 beyonder-characteristic classes and every helper (everything outside aspects, the expeditioner and items, plus the helper classes under item/custom: commands, detection, abilitys, other).
Dropped on purpose: every aspect, the expeditioner, every item. Never port those.

## Rules
- Behaviour must stay identical to the old code: same damage, timings, costs, cooldowns, hit detection. Do not redesign, rename or add features.
- Copy files from the old project with cp --no-preserve=mode, then edit only the lines that fail to compile. Do not retype whole files.
- Keep the same package paths and class names as the old code, under the renamed root package net.lukario.circleofinevitability (old net.lukario.frogerealm; the old main class ForgeRealm is CircleOfInevitability; scripts/renames.sh maps both for copied files).
- The mod id is circle_of_inevitability (the old code's forgerealmmod is renamed to it). Assets live under assets/circle_of_inevitability.
- Never modify anything under reference/. Work on the branch this session starts on. Never push to main and never force-push. Commit at the end of the stage and push that branch.
- Verify with ./gradlew build. You cannot see the game, so for anything visible tell me what to check in ./gradlew runClient.
- The shell is Linux in a cloud VM: use Unix-style commands and keep LF line endings. Run java -version first. It must say 25; if it does not, stop and tell me instead of porting.
- If a kept class needs a dropped class or an item, remove that reference and list it in MIGRATION_NOTES.md under Removed.

## Conventions to keep
- Per-player state lives in player.getPersistentData(); keep the same keys.
- Use Identifier (Mojang renamed ResourceLocation in 1.21.11) with the same namespaces and paths.
- Use the current AttributeModifier constructors.
- Every event subscriber inner class has a unique name.
- Reusable pieces stay in the shared helpers. Animation durations are constants that can change without breaking hit detection.
- Progression (Pathway, Sequence 9..0, digestion, spirituality, /sequence): read docs/SEQUENCES.md before touching it. The old words aspect, stage, tier, soul essence and shards are gone from src; do not bring them back (they remain only in reference/ and in the history parts of MIGRATION_NOTES.md).
- Custom weapon swing animations (client/animation): read docs/ANIMATIONS.md before adding a swing, a rule, a weapon type or a class to them.

## Stages (one per session; I will say which)
1 scaffold | 2 root, status, effects, handler, events, soul_shards | 3 combat, network, item/custom commands and other | 4 particles/fx, client, menu, screen, item/custom detection and abilitys (rendering and input) | 5 the seven classes, one at a time | 6 assets and cleanup

## Notes file
Read MIGRATION_NOTES.md at the start of every session. Update it at the end: renames found, decisions, what is still broken.

## Compact instructions
When compacting, keep the list of API renames and the current stage.