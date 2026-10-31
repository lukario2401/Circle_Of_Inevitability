# Omniscient Eye

All 26 active abilities in the supplied White Tower design are implemented. The player's latest replication
requirements take precedence: a stored power requires **100% study**, uses key 4, and has no replication cooldown.

Set up with `/sequence pathway @s "Omniscient Eye"` and `/sequence set @s <0-9>` (operators only).
The seven keys are G, H, J, K, L, N, M unless rebound.

| Key | Unlock | Normal | Sneak |
| --- | --- | --- | --- |
| 1 | 9 | Reading | Ritual Circle |
| 2 | 8 | Deduction | Keen Eye |
| 3 | 7 | Reconstruction | Deductive Strike |
| 4 | 6 | Replicate the selected fully studied power | Open the stored-ability list |
| 5 | 6 | Mystic Arts wheel | Repeat its last pick |
| 6 | 4 | Fate and Cognizing wheel | Repeat its last pick |
| 7 | 2 | Omniscience wheel | Repeat its last pick |

## Study and replication

Visible real casts within 32 blocks and a forward 120-degree field of view add study. A wall blocks observation.
Player and Beyonder-mob casts both count. Own casts, echoes and replicated casts do not feed recursive study.

Study gain, in percentage points, is `min(100, 5 * 2^(abilitySequence - observerSequence) / (10 - abilitySequence))`.
Lower Sequence numbers mean stronger powers. A power stronger than the observer cannot be studied, selected or
replicated. At Sequence 0, a Sequence 9 power is fully learned in one observation.

Every copyable wheel entry has its own stable ID, icon, name, Sequence and study record. Learning a wheel spell
replays **that exact entry**, never a random pick or the wheel-opening key. Entries marked `notCopyable()` retain
the shared kit's existing restrictions on powers that require private pathway state (such as Replication itself,
Open Source and the owner-controlled Tower/form toggles).

Sneak + K opens the scrollable list. Under-100% names and percentages are **red** and cannot be selected or cast.
Even a previously saved selection is checked again by the server. Percentages below 100 are rounded down for display,
so an incomplete 99.999% record never appears as 100%. Incomplete records can still be favorited.
Click a row to select; click its star to favorite. Order is favorites first, then study descending, then strongest
Sequence, then name. Missing icons are omitted. Study, favorites and selection survive death, relogging and restarts.

K casts the selected fully studied power in its stored normal/sneak form, from the player's current position and aim.
Replication neither checks nor starts key/wheel cooldowns. Its normal spirituality price is 1.5 times the original;
inside the caster's Tower it is the original price. Damage is full strength at 100%. The copied code retains the
observer's real Sequence gates, so it cannot unlock stronger upgrades indirectly. Refused original casts keep their
usual target/space/cost checks. Opening the menu refreshes its rows.

OP command (permission level 2): `/codex study <player> <ability-id>`. Tab completion lists IDs.
For example `/codex study @s key/Wanderer/2/false` or
`/codex study @s wheel/omniscient_eye_mystic_arts/golden_armor_art`.
It grants 100% immediately while retaining pathway/Sequence checks.

## Mystic Arts and lower abilities

Reading reports health, mood, hunted target and effects in chat, with a floating card ornament and an Appraisal mark.
From Sequence 8 it names weakness; higher Sequences identify Beyonders. First readings of each creature type every
10 minutes return 10 spirituality and a little experience. Ritual Circle chants at Sequences 9/8 (movement or damage
cancels it), then protects/cleanses allies, slows hostile projectiles and pushes summoned constructs across its edge.
The rite becomes instant at 7, wider/longer at 8, and discounts spells from 5.

Deduction marks and weakens the target and outlines projectile paths. Keen Eye shows hidden traps, outlines hostile
projectile paths and exposes enemies looked at directly. Reconstruction shows recent cast locations as ghost proxies
and player movement trails; it does not replay each original spell mesh. Deductive Strike steps forward, deals three
hits, and spends an existing mark for stronger hits and a fourth blow.

| Mystic Arts entry | Sequence | Cost | Cooldown | Behavior |
| --- | --- | --- | --- | --- |
| Analyze | 6 | 1500 | none | Focus for 2 seconds (1 from 5), then weakness and a 20-second +15% Appraisal mark |
| Mirror Curse | 5 | 3500 | 25s | Missing-health damage with a boss cap, followed by bleeding |
| Vile Thoughts | 5 | 3000 | 20s | Nausea, fear, 50% falter; extra damage against constructs/summons |
| Improvise | 5 | 4500 | 30s | Weakness-matched elemental hit/status, unmaking, lightning spread or force hit |
| Living Tome | 5 | 5000 | 45s | Animated page/ink construct, 60 health, 8 damage, 30s; death blinds nearby enemies |
| Golden Armor Art | 4 | 6000 | 40s | Self or aimed ally: 40% damage cut, 12 absorption and no knockback for 30s |

Living Tome grows to three simultaneous, larger constructs lasting 120 seconds at Sequence 0.

## Fate and Cognizing

| Entry | Sequence | Cost / cooldown | Behavior |
| --- | --- | --- | --- |
| Prophecy | 4 | 8000 / 40s | 12s predictive projectile aim and ghost warnings; 20s from Sequence 2 |
| Bookshelf Form | 4 | 9000 / 60s | 30s, 30% less damage, +8 max health, Resistance I; nearby enemies are read and their powers studied |
| Cognize | 3 | 10000 / 30s | Reveals enemies in a 32-block cone, reads defenses/powers, grants 50 study and a +25% mark |
| Leverage | 3 | 12000 / 35s | Strips defenses, +30% vulnerability for 8s, heavy for 6s; marked non-bosses are held for 2s |
| Decipher | 3 | 9000 / 25s | First flips a hostile dome on the aiming ray, else closes a hostile zone/trap, else frees a bound/sealed body |

Bookshelf Form lasts 40s at Sequence 3. At 2 it becomes a six-block phantom tower, lasts 60s and no longer builds
Madness. At 1 it becomes the spiral bookshelf with a gaze beam that grants study. Cast the form again to end it
without cost, even through cooldown. Incomplete forms build Madness (half as fast at 3); replication adds more.
A Madness break ends the form and weakens the caster. Ordinary form exit does not cause that penalty.

Prediction solves a constant-velocity intercept, steers supported ModelFx shots and newly launched entity projectiles,
and shows projectile paths and close attacking mobs as warning proxies. It does not simulate future AI decisions.

## Omniscience

| Entry | Sequence | Cost / cooldown | Behavior |
| --- | --- | --- | --- |
| Evade Disaster | 2 | 14000 / 45s | 15s freedom from bindings/effects, wards source-less damage, and conditionally escapes a hit over half max health |
| Wise Counsel | 2 | 16000 / 60s | 20s allied aura: 30% cheaper casts, prediction, and 20% less damage from the caster's marked enemies |
| Omniscience | 1 | 20000 / 30s | Inspects through walls to 200 blocks, or the latest connected enemy; location/defenses/powers, 50 study and +35% mark |
| Exploit | 1 | 22000 / 40s | First applicable flaw: summon, ward, concealment, fire creature, evil/undead, armor, healing, Beyonder power, old wound |
| Redefine | 1 | 18000 / 50s | Enemy: seal last key for 15s or mob abilities for 6s. Aimed ally: next cast free with 30% stronger damage |
| Calamity of Knowledge | 0 | 40000 / 90s | Marked enemies within 24 blocks take Madness +5, 25% max-health damage (bosses 8%), and armor durability loss; others get nausea |
| Open Source | 0 | 25000 / 120s | Lends the last performed copyable key variant/wheel entry to up to three allied players for one use within 60s |
| Tower of Omniscience | 0 | 60000 / 180s | 60s moving, 40-block kingdom: reveals enemies, strongest marks, instant full study of real casts inside, allied discounts and growing enemy Madness |

A recipient uses **`/castgrant`** to release an Open Source loan from any pathway. It costs triple the original price,
deals half-strength damage, ignores Sequence/cooldown gates and is consumed only after a successful cast. The command
respects ability roots/locks/decrees/seals. Loans end on death/logout/expiry and are not saved across server restarts.
A refused cast preserves the loan. A second grant replaces the first. Damage boosts/weakening carry through the existing
Echo-supported delayed hits; healing, utility effects and durations keep native behavior.

The Tower's own entry can be cast again to end it for free through cooldown. Its attached scenery follows the owner
and is cleared when it ends. Losing the pathway or Sequence 0 also ends it. Its study bonus still cannot learn powers
stronger than the observer. The Open Book passive taxes a hostile player's currently cast power by 25% when the nearby
White Tower has fully studied that exact ID.

## Shared mechanics, passives and validation

Madness is a shared 0..10 counter dropping one level per 5 seconds without additions: nausea from 3, falter from 6,
fear/friendly-side aggression at 9, a brief stun and 5% max-health hit at 10, then reset to 3. Bookshelf Form listens
for breaks to apply its own exit behavior. Counters and thresholds use `status/Madness`, not bespoke form logic.

Passives include shortened mind effects and nearby invisible outlines (8), attack speed/reach and Speed I (7),
rite regeneration (5), +4 max health (4), nearby concealment exposure (3), a major-hit escape rearming after 30s (2),
sight-based study (1), and Open Book (0). Attributes are removed when the pathway/Sequence no longer qualifies.
The optional melee combo and flavor-only spare ideas are not additional ability entries.

See [SPELL_KIT.md](SPELL_KIT.md#study-prediction-and-borrowed-powers) for reusable catalog, library, prediction,
inspection, Madness, conditional protection, one-shot casting modifiers and spell-loan APIs.

Run `./gradlew build` and `./gradlew runGameTestServer -PomniscientEyeTests`.
The 11 focused real-server tests cover study ordering/eligibility/storage/death cloning, exact wheel observation and
replay, incomplete/preselected rejection, no-cooldown paid replication, Fate/authority abilities, free toggle cleanup,
Tower study for keys and wheel entries, borrowed powers, one-shot cost consumption and shared kit rules.
The menu is compiled against the current client API; its appearance still needs an in-game visual playtest.
