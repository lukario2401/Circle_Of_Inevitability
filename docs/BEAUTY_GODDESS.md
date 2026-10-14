# Beauty Goddess: ability design

The Beauty Goddess Pathway is the Moon Pathway of Lord of the Mysteries: Apothecary (9), Beast Tamer (8), Vampire (7), Potions Professor (6), Scarlet Scholar (5), Shaman King (4), High Summoner (3), Life-Giver (2), Beauty Goddess (1), Moon (0).

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M), every ability has a normal cast and a sneak cast. Sequences 2, 1 and 0 are not left without abilities: key 7 is a wheel (like Apocalypse's Catastrophe) whose entries unlock at Sequences 3, 2, 1 and 0, and the passives below grow with every Sequence.

Pathway name in code: `"Beauty Goddess"`.

## Keys

| Key | Ability | Sequence | Normal cast | Sneak cast |
| --- | --- | --- | --- | --- |
| 1 | Medicine Concoction | Apothecary (9) | Throw a potion flask where you aim. It bursts into a cloud that heals you and your allies over time and cures poison. Enemies in the cloud are poisoned instead. | Spirit Vision: for a short time every living thing around you glows through walls with its health shown. |
| 2 | Taming | Beast Tamer (8) | The animal or weak mob you look at is tamed and fights for you for a while. Bosses and players resist and are only held for a moment. | Animal Sense: every animal and tamed ally around you is called to you, gets speed and strength, and attacks what you look at. |
| 3 | Wings of Darkness | Vampire (7) | Illusory bat wings carry you forward in a fast glide with no fall damage. At the end the wings burst into a swarm of bats that bite everything on the way. | Abyss Shackles: chains of shadow hold the target in place and drain its health a little each second. |
| 4 | Potions | Potions Professor (6) | The potion wheel: Invisibility Potion, Fire Breathing Potion, Sun Water, Shadow Potion. Each has its own Sequence, cost and cooldown. | The last potion again, without the wheel. |
| 5 | Lunar Battlefield | Scarlet Scholar (5) | A full moon rises over the spot you aim at. You and allies inside get speed, strength and regeneration, enemies inside are slowed. Press again inside it for Flash Teleportation: blink to where you aim, anywhere in the moonlight. | Spectral Moonlight Form: you turn into moonlight for a few seconds, hidden, fast and untouchable. Cast inside the full moon, your next death is also cancelled and you re-form on the spot. |
| 6 | Gaze of Darkness | Shaman King (4) | The target's reflection is caught in your eye: it is wrapped in solid darkness and held, and takes damage for as long as you hold the gaze. Ending it by crushing the eye hits it hard, and the eye grows back. | Bat Swarm Transformation: you split into a swarm of bats and fly for a few seconds. You cannot be killed in this form unless every bat is killed; you re-form where you end. |
| 7 | Moon Wheel | High Summoner (3) | The Moon wheel, cast where you aim: Door of Summoning (3), Creation (2), Beauty (1), Blood Moon (0). Each entry has its own Sequence, cost and cooldown. | The last one again, without the wheel. |

## Key 7 wheel

| Entry | Sequence | What it does |
| --- | --- | --- |
| Door of Summoning | High Summoner (3) | An illusory door opens where you aim and creatures from the Spirit World step out and fight for you for 30 seconds. Rarely something stranger comes through. |
| Creation | Life-Giver (2) | A Seed of Life is planted. For a while it heals you and your allies around it to full and grows plants. |
| Beauty | Beauty Goddess (1) | Pure beauty and gravitation: enemies in a big radius are pulled gently toward you and stand frozen in awe, unable to attack, and take extra damage while they stare. |
| Blood Moon | Moon (0) | The sky turns red. For a minute the Spirit World bleeds into the area: monsters rampage and attack each other, every enemy burns with moon damage over time, and you gain regeneration and strength. |

## Passives

| Passive | From Sequence |
| --- | --- |
| Resistance to poison and toxins | Apothecary (9) |
| Strength I | Beast Tamer (8) |
| Night Vision, faster regeneration | Vampire (7) |
| Your own potion effects last longer | Potions Professor (6) |
| Speed II at night, afterimages when you run | Scarlet Scholar (5) |
| Eyes grow back at once, strong regeneration | Shaman King (4) |
| Mobs never pick you out of a crowd (they hesitate when they see you) | Beauty Goddess (1) |

## What is built

Everything above. The abilities are in `blasphemy_slate/beyonder_characteristics/BeautyGoddess.java` (every number is a constant at the top of it), one line each in `PathwayAbilities.ALL`, a row in [SEQUENCES.md](SEQUENCES.md), and the passives in `SequencePassives`. Set the Pathway with `/sequence pathway <player> "Beauty Goddess"`.

Both wheels (keys 4 and 7): sneak casts the last pick again with no cooldown, for twice its cost (`QUICK_CAST_COST`; `AbilityWheel.quickCastCost`). Picking from the wheel costs the normal price and starts the cooldown.

How it works where the table above does not say:
- Medicine Concoction heals and cures you and every non-enemy in the cloud; enemies in it are poisoned.
- Taming uses `status/Marionette`: at most 3 at once, a minute each. A mob with more max health than 40 (+20 for every Sequence past 8) resists, as do players.
- Wings of Darkness: press the key again to land early. The bats go for the enemies you passed on the way, or the ones near where you land.
- Invisibility, Shadow and Moonlight Form hide you completely (`status/Concealment`: unseen and untouchable). Attacking ends them, and only one form (these three or the bat swarm) is on at a time.
- Lunar Battlefield: press the key again while you stand in your own moonlight to blink (Flash Teleportation). From outside it, the key raises a new moon in its place.
- Moonlight Form, cast in your own moonlight, cancels your next death while that moon is up (`status/DeathWard`).
- Gaze of Darkness holds as long as you can see the target, 6 seconds at most.
- Bat Swarm: 12 bats, 6 seconds of flight (`status/Flight`) and vanilla invisibility (your armor still shows). Every blow kills one bat per 4 damage instead of hurting you, and the bats bite whatever is close. When the last bat dies the rest of that blow lands on you. Press the key again to re-form early.
- Door of Summoning: two hounds (wolves) and a vex come through and fight for you for 30 seconds (`Marionette.summon`: they do not use up Taming's 3 slots). Then they vanish with no drops. One cast in about twelve also brings a ravager.
- Creation: from the moment it blooms, for 20 seconds, everyone who is not your enemy within 8 blocks is healed to full and cleansed every second. Eight spots a second under it grow as if bone-mealed. It does not bring anyone back.
- Blood Moon sets the monsters under it on each other every 5 seconds (`Taunt.brawl`).

## Looks, sounds and pictures (ready)

Everything an ability needs to look and sound right is in `BeautyGoddessFx.java` (next to the other pathway classes). It only shows things: the ability does the damage, healing, taming and summoning, at the ticks its constants name. `/beautyfx all` plays every display in game, `/beautyfx <name>` one of them.

| Ability | Normal cast | Sneak cast |
| --- | --- | --- |
| 1 Medicine Concoction | `throwFlask(sl, from, to, MEDICINE)` returns the ticks it flies; then `flaskShatter`, `medicineCloud` + `medicineCloudTick` every 10 ticks, `medicineHeal` on allies, `medicinePoison` on enemies | Spirit Vision: `spiritVision`, then `spiritVisionMark(sl, entity, ticks, health / max)` on everything seen |
| 2 Taming | `tame(sl, player, beast, ticks)`, tamed at `TAME_LANDS`; `tameResisted` for bosses and players, `tameEnd` | Animal Sense: `animalSense`, `animalSenseCalled` on each animal, `animalSenseHunt` on what you look at |
| 3 Wings of Darkness | `wings(sl, player, ticks, tag)`, at the end `wingsBurst` and `BAT_SEEKER.onHit(...).launchAt(...)` for the bats that bite | Abyss Shackles: `shackles(sl, target, ticks)`, held from `SHACKLES_LOCK`; `shacklesDrain` once a second, `shacklesEnd` |
| 4 Potions (wheel) | `drink(sl, player, color)`, the effect at `DRINK_DONE`; then `invisibility`, `fireBreath` + `fireBreathTick`, `throwFlask(..., SUN)` + `sunWater` + `sunWaterBurn`, or `shadowForm` + `shadowFormTick` + `shadowFormEnd`. Icons `WHEEL_INVISIBILITY`, `WHEEL_FIRE_BREATHING`, `WHEEL_SUN_WATER`, `WHEEL_SHADOW` | the same, without the wheel |
| 5 Lunar Battlefield | `lunarBattlefield(sl, center, radius, ticks, tag)`, risen at `MOON_RISEN`; `lunarBattlefieldPulse` every 2 s; `flashTeleport(sl, from, to)` for the blink | Spectral Moonlight Form: `moonlightForm`, `moonlightFormTick`, `moonlightFormEnd`; on a cancelled death `moonlightShatter` then `moonlightReform` (whole at `MOONLIGHT_REFORMS`) |
| 6 Gaze of Darkness | `gaze(sl, player, target, ticks, tag)`, held from `GAZE_GRIPS`; `gazeTick`; `gazeCrush` (hit at `GAZE_CRUSH_HITS`) or `gazeEnd` | Bat Swarm: `batSwarm`, `batSwarmTick`, `batSwarmBatLost` per bat killed, `batSwarmEnd` |
| 7 Moon wheel | Door of Summoning: `summoningDoor` (open at `DOOR_OPEN`), `spiritCharge`, `summonArrives`, `summonAnomaly`. Creation: `seedOfLife` (blooms at `SEED_BLOOMS`), `seedOfLifePulse`, `seedOfLifeHeal`, `seedOfLifeRevive`. Beauty: `beauty`, `beautyEnthralled`, `beautyPull`, `beautyEnd`. Blood Moon: `bloodMoon` (risen at `BLOOD_MOON_RISEN`), `bloodMoonTick`, `bloodMoonBurn`, `bloodMoonEmpower`, `bloodMoonEnd`. Icons `WHEEL_DOOR`, `WHEEL_CREATION`, `WHEEL_BEAUTY`, `WHEEL_BLOOD_MOON` | the last entry again |

Every timed cast can show its picture and clock above the hotbar: `BeautyGoddessFx.status(player, Status.X, ticks)`, and `statusEnd` to take it away. The Pathway's icon beside the hotbar is already registered in `PathwayHudIcon` under `"Beauty Goddess"`.

What is in the repository, all of it generated by the scripts in `tools/model_gen/beauty_goddess/` (see "Beauty Goddess: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)):

| What | Where | Made by |
| --- | --- | --- |
| 40 model files (each step of a moving model is a file): flask, bat, bat wing, shackle, chain, abyss pool, full moon, blood moon, moon beam, moon seal, halo, moonlight figure, gaze eye, darkness prison, the Door of Summoning (frame, doors, void), spirit beast, Seed of Life, crescent crown, goddess, blood pact mark | `models/model_fx/beauty_goddess/`, `textures/model_fx/beauty_goddess/` | `models.py` (+ `textures.py`) |
| 8 wheel pictures (32 px), 19 status pictures (16 px), the Pathway icon | `textures/gui/ability_menus/beauty_goddess/`, `textures/gui/beauty_goddess/`, `textures/gui/pathway_icons/beauty_goddess.png` | `icons.py` |
| 8 particle pictures: `fx/bat_0`, `fx/bat_1`, `fx/petal`, `fx/crescent`, `fx/bubble`, `fx/droplet`, `fx/sparkle`, `fx/leaf` | `textures/particle/fx/` | `particles.py` |
| 28 sounds (30 files): flask throw and shatter, bubbling, drinking, healing chimes, spirit vision, blood pact, wings and flaps, bat swarm, shackles, drain, invisibility, fire breath, sun water, shadow melt, moonrise, moon blink, moonlight form and re-form, gaze, eye crush, darkness, door opening, summon arrival, creation, beauty, blood moon | `sounds/beauty_goddess/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` | `sounds.py` |
