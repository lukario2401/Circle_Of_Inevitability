# Worm Of Time / Error Pathway

Gameplay implementation based on the [Error Pathway abilities](https://lordofthemysteries.fandom.com/wiki/Error_Pathway/Abilities), using the same server spell tools, wheel menus, costs and cleanup conventions as Hand Of God, Prince Of Abolition and Hand Of Order. Sequence numbers decrease as power increases. This is a Minecraft adaptation of the progression, rather than a simulation of every narrative application of theft.

## Keys

| Key | Normal | Sneak | Unlock |
| --- | --- | --- | --- |
| G / 1 | Steal one item from the enemy's main hand; from Parasite onward, steal life instead | Observation at lower Sequences; from Parasite onward, steal distance and blink to a safe aimed position | 9; upgraded forms 4 |
| H / 2 | Steal a Beyonder power; Mentor of Deceit steals up to three different powers per attempt | Release the newest held power once, without its cost or Sequence requirement | 6 |
| J / 3 | Deep parasitism: drain the host and steer mob hosts | Initial parasitism: hide, see through the host, and heal without draining or steering | 4 |
| K / 4 | Superior Observation: sense nearby loose items | Decrypt the aimed enemy: reveal health, Pathway/Sequence and its latest recorded cast, briefly expose concealment, and improve subsequent power theft | 9 / 7 |
| L / 5 | Open Thought and Deceit | Recast the last choice | 8; individual entries unlock through 3 |
| N / 6 | Open Parasite and Destiny | Recast the last choice | 4; individual entries unlock through 1 |
| M / 7 | Open Time and Error | Recast the last choice | 2; time entries 1, Error entries 0 |

J again leaves either host mode after the one-second key debounce. Only J works inside a host. Mob steering uses the existing walk, jump, sprint, aim and attack controls; a player host keeps control of their own movement. Sneaking when entering chooses the mode; it cannot be changed inside the host.

## Progression and passives

| Sequence | Gameplay additions |
| --- | --- |
| 9 Marauder | Main-hand item theft, a ten-block loose-item sense, 10% movement speed and 15% attack speed |
| 8 Swindler | Swindler's Charm makes mobs lose interest and prevents direct spell targeting temporarily; attacking breaks it |
| 7 Cryptologist | Evidence-based decryption of an entity actually in sight; remembers that target for 30 seconds |
| 6 Prometheus | Beyonder power theft, 32-block observation with a private item-location mote, 20% movement speed and resistance to nausea |
| 5 Dream Stealer | Steal intent, dreams and an incoming attack; power theft reaches 48 blocks |
| 4 Parasite | Two host modes, life and distance theft, worm-body defense and eight extra maximum health points |
| 3 Mentor of Deceit | Area deception, autonomous avatars, up to three distinct stolen powers per successful attempt; prefers the victim's recent known power |
| 2 Trojan Horse of Destiny | Steal blessings/luck, exchange positions, and exploit a loophole to break ordinary roots and harmful effects |
| 1 Worm of Time | Aging, local slow/stop/acceleration, a short self-rewind, recent history reading and avatar succession |
| 0 Error | Temporarily leave reality and misdirect three incoming attacks to a chosen enemy; earlier powers retain their progression bonuses |

`might` is 1 below Sequence 4, then 2 / 2.5 / 3 / 4 / 5 at Sequences 4 / 3 / 2 / 1 / 0. It scales life theft and avatar health/damage. Worm defense provides 6 bodies at Sequence 4 and two more per advancement, each absorbing up to six damage according to `Swarm`'s shared rules.

## Single-key costs and rules

- Item theft: 250 spirituality, six-block aimed enemy, one main-hand item per cast. Creative/spectator inventories are excluded. A full thief inventory drops the transferred item normally; items are moved, never copied.
- Life theft: 1800 spirituality, 24 blocks, `12 × might` true damage; heal 60% of health actually removed. The old permanently escalating vulnerability counter is no longer used.
- Distance theft: 1500 spirituality, 24 blocks (48 from Sequence 1), collision-checked destination, respects teleport restrictions and grants five seconds of fall protection. These G casts have no cooldown.
- Observation: 150 spirituality; reports the nearest loose item within 10 or 32 blocks. It does not inspect unopened containers. Decryption costs 700 and has a separate Sequence 7 check; both have no cooldown.
- Power theft: 1500 spirituality with no cooldown per attempt, 32 blocks (48 from Sequence 5). Higher-ranking spirits resist: success is `max(0.15, 1 − 0.18 × Sequence gap + 0.25 if decrypted)`, capped implicitly by comparing to a random value below one. Failed resistance checks spend the attempt; absent targets/full storage do not.
- Up to five powers are held for twenty minutes. Sneak H releases the newest first. The victim's player key stays unavailable until release/expiry/death/logout/respec. A Beyonder mob supplies eligible abilities and is emptied for thirty seconds; this does not disable its AI abilities. At Sequence 3+, a successful attempt takes up to three distinct eligible powers, stopping when storage fills. Already-held player powers cannot be taken again. A failed stolen cast stays stored if it refused normally.
- Parasite entry costs 6000, reaches six blocks and lasts at most two minutes. Initial mode costs 900/second and heals one health/second. Deep mode costs 2600/second and drains two health/second without reducing the host below two health. A stronger Beyonder, creative/spectator player, occupied host or recently exited host is refused. Hosts resist re-entry for five seconds; parasite entry/exit has a one-second debounce. Host death ejects the parasite for eight magic damage. Leaving is free.

## Thought and Deceit wheel

Cooldowns below apply to menu selections. Sneak recasts ignore and do not start cooldowns, and cost three times the listed spirituality. Existing menu cooldowns remain in place; a sneak recast does not clear them. Target failures are free.

All regular G/H/K casts have no cooldown. Parasite keeps its one-second input debounce to prevent a held J press from entering and immediately leaving, plus the host's five-second resistance after exit. These are host/toggle rules rather than regular cast cooldowns.

| Entry | Sequence | Cost | Cooldown | Effect |
| --- | --- | --- | --- | --- |
| Swindler's Charm | 8 | 500 | 8 s | Six seconds unnoticed; attacks end it |
| Steal Intent | 5 | 2000 | 8 s | Aimed target at 32 blocks briefly loses attacks, item use and abilities; clears a mob's target |
| Steal Dream | 5 | 2400 | 12 s | Five-second interruptible sleep, reduced by shared player/boss hold rules; steals up to 1200 spirituality, at most 10% of what the target has |
| Mentor of Deceit | 3 | 6000 | 15 s | Up to 24 enemies within twelve blocks gain 35% attack falter for eight seconds and a short ability interruption |
| Steal Attack | 5 | 2400 | 12 s | Four-second stance; the next eligible hit is cancelled and its damage returned to its living enemy attacker, capped at `30 × might` |

## Parasite and Destiny wheel

| Entry | Sequence | Cost | Cooldown | Effect |
| --- | --- | --- | --- | --- |
| Worms of Time | 4 | 6000 | 20 s | Eight seconds of the shared Swarm defense, with segmented worm-like motes on body loss |
| Create Avatar | 3 | 9000 | 10 s | One autonomous spectral humanoid; `40 × might` health, `6 × might` attack damage, 20% faster walking, sixty-second lifetime and 48-block owner leash |
| Recall Avatars | 3 | Free | None | End all avatars; recover 10% of each avatar's remaining health, capped at eight health per avatar |
| Steal Fate | 2 | 15000 | 25 s | At 32 blocks steal up to three finite beneficial potion effects, at most ten seconds each and level III; give the victim 40% falter and the thief Luck II for ten seconds |
| Exchange Positions | 2 | 12000 | 12 s | Swap places with an aimed enemy within 32 blocks; both bodies must fit and both teleport restrictions are respected |
| Avatar Succession | 1 | 24000 | 60 s | Prepare a thirty-second escape from one fatal hit. A living, fitting avatar must still be present at the fatal blow; consume it, relocate and recover to 40% health with two seconds of grace |

Avatar limits are two at Sequence 3, three at Sequence 2 and four at Sequences 1/0. Creating another replaces the oldest. Avatars use the summon/construct ownership system, attack their owner's enemies, and make a small life-stealing strike against their nearby target every three seconds. Their models have a dark coat and pale monocle. They cannot carry stolen abilities, consume characteristics or advance independently. Succession fails against void/kill damage, piercing attacks, prohibited resurrection, teleport restrictions or a destroyed/missing avatar; it is checked at the actual fatal hit.

## Time and Error wheel

| Entry | Sequence | Cost | Cooldown | Effect |
| --- | --- | --- | --- | --- |
| Exploit Loophole | 2 | 12000 | 20 s | Break ordinary roots/harmful effects and remain unbound for three seconds; external prohibitions still apply |
| Steal Lifespan | 1 | 18000 | 10 s | Aimed 40-block target takes `25 × might` true damage, heals the thief for 40% actually removed, and suffers ten seconds of decay/weakness/slowness |
| Slow Time | 1 | 16000 | 20 s | Eight-second, twelve-block moving zone; slows enemies and hostile projectiles, with Mining Fatigue on enemies |
| Ancient Clock | 1 | 30000 | 30 s | Three-second local time stop; enemy actions/movement are restricted and up to 64 hostile projectiles are suspended. Shared player/boss hold durations and immunities apply |
| Accelerate Time | 1 | 16000 | 20 s | Eight seconds of Speed II/Haste II for the caster and up to 24 allies; subtract five seconds from finite beneficial potion effects on up to 24 nearby enemies |
| Reverse Time | 1 | 24000 | 30 s | Return to the caster's safe position three seconds earlier and recover lost health to that sample; does not restore spirituality, inventory, cooldowns, entities or blocks |
| Curtain of History | 1 | 8000 | 6 s | Report the most recent other recorded cast within 32 blocks and thirty seconds, with a clock at its recorded location |
| Error: Leave Reality | 0 | 40000 | 30 s | Three seconds in the shared Exile state: cannot interact with reality and cannot be attacked normally; returns to the original position |
| Error: Misdirection | 0 | 35000 | 25 s | Five-second, three-charge substitute; eligible incoming hits are cancelled and redirected to the chosen living enemy, capped at 150 damage each |

While ordinary roots silence abilities, M attempts only Exploit Loophole directly (normal cost/cooldown; sneak costs 3� and bypasses cooldown); it cannot cast other Time/Error entries through that hold. Exile, stolen/sealed keys and external ability prohibitions still prevent it.

Misdirection still consumes a defensive charge if its receiver dies or moves out of range; that charge cancels the hit without redirecting damage.

Temporal fields use the same slot, so a new slow/stop closes the previous one. Allies are excluded by the shared `Spells` targeting checks. A stopped target's movement and actions are restricted; this does not pause Minecraft's tick engine or every other mod's internal timers. Projectile ownership is reference-counted across overlapping clocks. Original velocity/gravity are restored on release or level unload, and a saved recovery tag prevents a projectile saved during a clock remaining frozen after reload.

Self-history is sampled every five ticks, capped to five seconds, cleared on death/respawn/logout, dimension changes, spectator mode and Pathway loss, and consumed on rewind. It never loads a past dimension or resurrects the caster. Historical reading uses the existing `Casts` log, not a replay recording of every world event.

## Implementation and assets

- `WormOfTime.java`: original power theft, the two host modes, their existing visuals, and the existing melee combo. G delegates to the new Marauder implementation.
- `worm_of_time/WormOfTimePowers.java`: observation, the Thought/Destiny wheels, passives, avatars and succession.
- `worm_of_time/WormOfTimeAuthority.java`: Time/Error wheel, bounded history and projectile suspension lifecycle.
- `worm_of_time/WormOfTimeFx.java`: common cyan-black threads, bursts, ancient clock and avatar displays. Costs and gameplay stay in the power classes.
- `WormOfTimeMobs.java`: combat adaptations for NPCs: life/distance theft, intent/dream theft, deceit, lifespan theft, a short target time stop and Error misdirection. Inventory theft, held-power management, host camera control, avatars and rewind are player-only.
- Keys 4–7 dispatch through their normal network handlers and existing restrictions; `PathwayAbilities` lists all seven keys. Stateful Destiny/Time keys and entries cannot be stolen or copied. Delayed menu picks recheck Pathway ownership unless executing an authorized copied/stolen cast.
- `tools/model_gen/worm_of_time/generate.py` regenerates twenty 32×32 RGBA wheel glyphs, three model JSONs and two model textures. Requires Pillow. Every wheel icon and model has its own Worm Of Time asset path.

Worm Of Time keeps its eight custom sword swings. Its six-hit melee FX chain now uses target-centered opening, return, rising, falling and cross cuts, followed by a random clock-break or time-fracture finisher. Cyan ribbons have thin white cores, short impact scars and seven small sparks per hit. Cross cuts and finishers briefly draw a clock dial at the victim; finishers add a contracting ring or three staggered afterimage cuts. Effects face the attacker-to-target direction and scale within 0.45�1.35 blocks so small mobs stay readable and large mobs do not produce oversized flashes. Delayed particles stay at the original hit location. No damage, cooldown or attack-charge rules change.

Melee visuals are implemented in `WormOfTimeFx.java` and wired through `WormOfTime.MELEE_COMBO`; theft and parasite transition visuals remain separate. See [CUSTOM_SWING_IMPLEMENTATION.md](CUSTOM_SWING_IMPLEMENTATION.md) for the sword render hooks. To check the effects in game, land six fully charged hits as Worm Of Time, then repeat on a small and large mob, from different directions and in first and third person. Check both random finishers.

## Verification

Run `gradlew.bat build`. A dedicated-server startup check uses a separate temporary world, not the player's save. Resource checks verify every wheel icon, model JSON and model texture reference. Client visuals and multiplayer combat still require in-game playtesting:

1. At each Sequence 9 through 0, inspect the seven keys and locked/unlocked wheel entries. Try a failed target/destination and check that no cast cost is spent. Repeat menu and sneak recasts during cooldown.
2. Verify one-item transfer with empty/full inventories. Test normal/creative victims. Decrypt a stronger Beyonder, steal/release its powers, fill all five slots, and check expiry, death, logout and Pathway changes restore its keys.
3. Compare initial/deep parasitism, mob steering, a player host, stronger-host refusal, host death, upkeep exhaustion, re-entry and camera/gamemode restoration.
4. Create/replace/recall avatars at their caps, leave their leash, change dimensions/Pathway, and test succession with a living versus destroyed avatar and prohibited resurrection.
5. Cast slow/stop around allies, enemies and arrows. Test overlapping clocks, leaving the radius, owner death/logout, chunk reload and server restart; verify projectiles resume with their original motion/gravity.
6. Rewind after taking damage, obstruct the old location, change dimensions, and try immediately after login. Verify no inventory/spirituality duplication or cooldown reset.
7. Check the clock, monocle avatar and every wheel icon in the client. Test Error's return to reality and misdirection against melee, projectiles, piercing damage and absent receivers.
