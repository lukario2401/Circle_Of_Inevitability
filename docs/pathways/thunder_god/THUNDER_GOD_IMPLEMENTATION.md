# Thunder God: playable pathway

**Sailor (9) through Tyrant (0)** are implemented: all 48 planned abilities across five
wheels and two normal/sneak keys, plus Source of Wrath. See the preserved
[design](THUNDER_GOD.md) and [asset collection](THUNDER_GOD_ASSETS.md).

## Controls

| Key | Unlock | Normal | Sneak |
| --- | --- | --- | --- |
| G / 1 | 9 | Harpoon | Illusory Scales; repeat to dismiss freely |
| H / 2 | 8 | Raging Blow | Wrath; at Sequence 2 or stronger with ten Rage, Source of Wrath; repeat to end freely |
| J / 3 | 7 | Repeat last Tides cast | Open Tides |
| K / 4 | 6 | Repeat last Sky cast | Open Sky |
| L / 5 | 5 | Repeat last Thunder cast | Open Thunder |
| N / 6 | 5 | Repeat last Voice cast | Open Voice |
| M / 7 | 4 | Repeat last Storm cast | Open Storm |

With no saved selection, a normal wheel key opens the wheel. Selection survives logout.
Every wheel cast and recast pays its listed cost with no cooldown.
Cooldowns saved by older versions are ignored. Missing
targets, locked entries and Bolt Arrow without a bow refuse without payment.
Voice casts respect Silence. Active Incarnation of Light, Storm Hell and kingdoms can be
ended freely through their entry, without waiting.

Instant casts do not display ability icons over the crosshair. Timed Scales, Wrath and Source
indicators sit beside the hotbar; Rage remains there as a counter. Ability icons appear in
the wheel when choosing a power.

## Progression and costs

| Sequence | Added entries |
| --- | --- |
| 9 | Harpoon, Illusory Scales |
| 8 | Raging Blow, Wrath |
| 7 | Water Veil, Sweeping Wave, Tideheal |
| 6 | Windblade, Gale Burst, Gale Ride |
| 5 | Water Curtain, Water Sphere, Corrosive Rain, Wind Binding, Lightning Strike, Lightning Bolt Arrow, Siren Song, Discord, Thunderclap |
| 4 | Windblade Legion, Chain Lightning, Roar, Death Dirge, Hurricane, Tsunami |
| 3 | Sea Master, Maelstrom, Destructive Droplet, Sonic Boom, Cloud Sword, Lightning Storm, Wrath of the Sea |
| 2 | Source of Wrath, Magnetic Pole Pull, Electromagnetic Disruption, Sound of Horror, Requiem, Storm Incarnation |
| 1 | Incarnation of Light, Heavenly Punishment, Ocean of Lightning, Roar of the Thunder God, Storm Hell; Dance of Thunder upgrade |
| 0 | Starfall, Lightning Forest, Tyrant's Presence, Chasm of Storms, Dreadful Heaven, Banish into the Storm |

The five wheels hold 9 Tides, 9 Sky, 9 Thunder, 9 Voice and 8 Storm entries. Costs follow the [design's starting-number table](THUNDER_GOD.md#starting-numbers), except
Harpoon/Scales cost **10** each and Raging Blow/Wrath cost **200** each. The draft's 250/600
cannot be paid at their unlocks: Sailor's whole bar is 20, Folk of Rage's is 400. Those four
costs use half their unlock-level bar. Single keys have no extra cooldown.

Chasm of Storms halves the owner's Sky, Thunder and Storm costs while inside.
Dreadful Heaven makes the owner's Voice casts free with no cooldown, and
doubles enemy ability costs. Kingdoms are mutually exclusive sealed spheres, including the
owner, allies and projectiles; attacks and targeting cannot cross the boundary.

## Source of Wrath

At actual Sequence 2 or stronger, sneak + H with ten Rage spends the counter and 25,000
spirituality for twenty seconds. The effective casting Sequence improves by one, including
wheel gates and ability upgrades. Stored Sequence, health, spirituality pool and passive
unlocks do not change. Damage rises 25%, or 50% at actual Sequence 1.

Calm powers (Tideheal, Siren Song, Requiem and Cloud Sword's ally guard) are unavailable.
Area attacks can harm allies and aim scatters slightly. Expiry or voluntary ending empties
Rage and leaves ten seconds of Weakness II and Slowness I. Death/logout/pathway cleanup
releases the form and its temporary Sequence boost.

## Shared rules

Rage unlocks at Folk of Rage. Confirmed enemy hits add one point; repeating damage on the same
enemy from one cast grants only one point. A separate cast counts again. Incoming hits grant
one point at most every half second, including hits absorbed by scales. While Wrath is active,
melee blows grant an extra point. Drinking a potion or honey grants Tipsy for 30 seconds,
doubling Rage gains. Rage caps at ten, adds 2% damage per point, and loses one point per five
idle seconds. It is saved with the player and shown beside the hotbar; death or pathway loss
clears it. Full Rage and Wrath draw red ember eyes.

Raging Blow requires at least three Rage and an enemy within four blocks. It spends the
whole counter, damages and pushes the primary target and nearby enemies, and breaks the
primary target's guard at ten Rage. Siren Song's next-blow bonus lasts ten seconds and is
consumed only when Raging Blow has a valid target.

Water casts mark enemies Soaked for ten seconds. Water and natural rain also count as Soaked.
The mark extinguishes fire and prevents fire damage while active. Lightning gains 40% against
Soaked targets and 50% against evil/undead targets, combining multiplicatively. Water or metal
armor conducts up to three half-strength arcs to nearby Soaked enemies. Arcs do not recurse
and never choose allies. Paralysis lasts 0.3–1 seconds as specified by the cast, with a shared
three-second immunity window. Damage is credited to the caster. No vanilla lightning entity
is spawned. Copper, iron, gold, chainmail and netherite armor conduct; leather does not.

Illusory Scales grant temporary absorption and a damage reduction that increases with
advancement and wet conditions. At Ocean Songster, exhausting the absorption triggers one
burst. Wrath grants Strength II, Speed I, 30% attack speed, a 15% damage cut and fear immunity
for ten seconds (twenty from Sea King), followed by four seconds of Weakness I; Ocean Songster
gains three Rage immediately. Its aftermath does not add Slowness.

Water Veil and Water Sphere have independent, cleansable holds. Milk and Tideheal remove their
own effects without erasing unrelated roots. Bosses receive half-duration holds. Corrosive Rain
wears armor and starts Corrosion after four seconds inside. Water Curtain permits enemies to
cross slowly, stops hostile projectiles, and halves fire/explosion damage inside its sheet.
Allies remain unmarked and unharmed by these spells.

Gale Ride doubles running speed on the ground; jumping transitions it to flight. Air casts
fly immediately, with six seconds at Wind-blessed, twelve at Ocean Songster and up to 25 at
higher Sequences. Higher Sequences also increase flight speed and add a landing boom. Sneak
hovers and landing is protected. The existing Decree of Passage forbids both Flight and Glide, so
inside it Gale Ride supplies the grounded speed bonus rather than bypassing the Decree.
Lightning Strike without a target charges six weapon blows for ten seconds. Bolt Arrow needs
a bow/crossbow in either hand, uses no ammunition, has a one-second interruptible charge, and
damages up to two enemies along a 60-block ray.

## Higher powers and upgrades

Sea King expands Water Curtain, doubles wet Tideheal recovery, extends Wrath, upgrades
Lightning Strike and Thunderclap, and adds owner-only outlines to Downpour. Sea Master
creates three real aquatic summons and slows hostile projectile travel. Maelstrom captures
victims, preventing teleport and suppressing substitutes until the field ends. Destructive
Droplet charges before bursting and prevents survivors from healing for ten seconds.

Cloud Sword can guard an aimed ally: its wall blocks projectiles and melee paths crossing
the wall, while the ally gains a 30% damage cut behind it. Incarnation of Light moves as
untouchable light for up to six seconds or 150 blocks; sneak after release ends it early.
It steers gradually, stops at terrain, hits each victim once and bursts on exit. Starfall
warns before impact. Its teleport still respects movement and teleport restrictions.

Electromagnetic Disruption is silent and invisible. It blocks Connection, Prediction,
Dossier and Study reads inside and applies attack disruption through Falter. At Sequence 1,
Dance of Thunder also strips hiding, marks and affected connections. Death Dirge and Sound
of Horror lock Sealed Artifacts specifically, leaving ordinary item use available.

Lightning Storm can hit allies for half damage. Ocean of Lightning and Lightning Forest
leave electric ground; Forest destroys enemy barriers and reveals hidden bodies while
preserving allied barriers. Heavenly Punishment strips escape forms after its warning and
uses true damage. Storm Incarnation follows the caster's gaze. Storm Hell grounds flyers,
limits enemy sight, destroys hostile projectiles and rains local lightning.

Hurricane follows its owner, repels enemies/projectiles and blocks outside targeting;
from Sea King its ending leaves a small lightning storm. Tsunami grows at Sea King, can bend
toward a Wrath of the Sea victim and leaves floodwater as a local effect. Banish into the
Storm returns enemies hurt, wet and afraid; allies return with absorption. Cleanup releases
banished targets safely. Tyrant's Presence strips invisibility, prevents possession and
adds fear to storm hits.

## Passives and lifecycle

Sailor gains Strength I, Water Breathing, full water movement efficiency and half-duration
fear. Folk of Rage gains two hearts. Seafarer gains wet damage/defense and extra spirituality
recovery, plus Navigation coordinates and nearby weather. Wind-blessed gains Night Vision
and half fall damage. Ocean Songster draws aquatic animals and discourages hostile aquatic
targeting. Cataclysmic Interrer adds 10% damage reduction. Sea King adds 25% wet damage
and defense bonuses, stacking with Sea’s Favor. Calamity cuts damage with no attacker to 40%.
Thunder God is immune to lightning and gains Rage near landed bolts. Tyrant gains fear immunity and a threatening aura. Source does not grant these higher passive unlocks.

Timed world effects, summons, delayed hits, temporary Sequence boosts and viewer-only
outlines stop when their caster dies, logs out, changes dimension or loses the pathway.
Logout preserves saved Rage and wheel selections. Attribute passives are removed after a
pathway/Sequence loss. No ability places/breaks real blocks or changes global weather.

Minecraft has no water-pressure damage or petrification system to modify. The design's
excluded Mythical Creature Form, Earthquake and Snow Storm remain excluded.

## Try it and verify

```mcfunction
/sequence pathway @s "Thunder God"
/sequence set @s 0
/sequence spirituality @s 100000
```

Sneak + M opens Storm; sneak + L opens Thunder. Build Rage through combat, then sneak + H
for Source of Wrath. `/thunderfx` remains a cosmetic gallery; gameplay uses normal ability
keys. To test progression, set a weaker Sequence and check that higher wheel entries lock.

```sh
./gradlew build
./gradlew runGameTestServer -PthunderGodTests
```

Forty real-server GameTests cover unlocks, costs, immediate recasts, refusals, water/lightning
interactions and ally safety, Rage, cleansing, local rain, lifecycle cleanup, Source gates
and aftermath, kingdom sealing and discounts, information disruption, artifact-specific
locks, summons, banishment, light travel, warning delays, Chain Lightning opening bolts,
directional Cloud Sword defense and metal attraction. The standard build also runs existing
damage/particle serialization and swing-animation checks.
