# Thunder God: first half

Playable **Sailor (9) through Ocean Songster (5)**: 19 casts, four wheels, Rage,
Soaked/Shock, passives and the eight-step visual melee combo. The full pathway's
[design](THUNDER_GOD.md) and [asset collection](THUNDER_GOD_ASSETS.md) remain references.
Sequences 4–0, the Storm wheel, Source of Wrath and higher-sequence upgrades are not registered.
Stronger characters may use the first-half kit but gain no second-half abilities from it.

## Controls

| Key | Unlock | Normal | Sneak |
| --- | --- | --- | --- |
| G / 1 | 9 | Harpoon | Illusory Scales; press again to dismiss freely |
| H / 2 | 8 | Raging Blow | Wrath; press again to dismiss freely |
| J / 3 | 7 | Repeat last Tides cast | Open Tides |
| K / 4 | 6 | Repeat last Sky cast | Open Sky |
| L / 5 | 5 | Repeat last Thunder cast | Open Thunder |
| N / 6 | 5 | Repeat last Voice cast | Open Voice |
| M / 7 | — | Reserved for the later Storm wheel | Reserved |

With no saved selection, a normal wheel key opens the wheel. Every wheel cast and recast
pays the same full cost and observes the same cooldown. Selection and cooldowns survive logout.
A locked entry, a missing target or a Bolt Arrow cast without a bow is refused without payment
or a new cooldown. Voice casts and wheel selections respect Silence.

| Wheel | Entry | Unlock | Spirituality | Cooldown |
| --- | --- | --- | --- | --- |
| Tides | Water Veil | 7 | 1250 | 10 s |
| Tides | Sweeping Wave | 7 | 1250 | 8 s |
| Tides | Tideheal | 7 | 1500 | 14 s |
| Tides | Water Curtain | 5 | 3000 | 15 s |
| Tides | Water Sphere | 5 | 4000 | 20 s |
| Tides | Corrosive Rain | 5 | 4500 | 25 s |
| Sky | Windblade | 6 | 900 | 3 s |
| Sky | Gale Burst | 6 | 2450 | 12 s |
| Sky | Gale Ride | 6 | 1800 | 20 s |
| Sky | Wind Binding | 5 | 3500 | 15 s |
| Thunder | Lightning Strike | 5 | 3000 | 6 s |
| Thunder | Lightning Bolt Arrow | 5 | 4000 | 8 s |
| Voice | Siren Song | 5 | 3000 | 18 s |
| Voice | Discord | 5 | 3000 | 20 s |
| Voice | Thunderclap | 5 | 3500 | 14 s |

Single-key costs are **10** each for Harpoon and Illusory Scales, and **200** each for
Raging Blow and Wrath, with no additional cooldown. The draft's 250/600 values cannot be
paid at their intended unlocks: Sailor's whole bar is 20, Folk of Rage's is 400. These four
costs use half their unlock-level bar; the remaining draft costs are unchanged.

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
for ten seconds, followed by four seconds of Weakness I; Ocean Songster gains three Rage
immediately. Its aftermath does not add Slowness.

Water Veil and Water Sphere have independent, cleansable holds. Milk and Tideheal remove their
own effects without erasing unrelated roots. Bosses receive half-duration holds. Corrosive Rain
wears armor and starts Corrosion after four seconds inside. Water Curtain permits enemies to
cross slowly, stops hostile projectiles, and halves fire/explosion damage inside its sheet.
Allies remain unmarked and unharmed by these spells.

Gale Ride doubles running speed on the ground; jumping transitions it to flight. Air casts
fly immediately, with six seconds at Wind-blessed and twelve at Ocean Songster. Sneak hovers
and landing is protected. The existing Decree of Passage forbids both Flight and Glide, so
inside it Gale Ride supplies the grounded speed bonus rather than bypassing the Decree.
Lightning Strike without a target charges six weapon blows for ten seconds. Bolt Arrow needs
a bow/crossbow in either hand, uses no ammunition, has a one-second interruptible charge, and
damages up to two enemies along a 60-block ray.

## Passives and lifecycle

Sailor gains Strength I, Water Breathing, full water movement efficiency and half-duration
fear. Folk of Rage gains two hearts. Seafarer gains 10% damage, 10% damage reduction and
25% extra spirituality recovery while wet, plus a Navigation readout of coordinates and
nearby local rain. Wind-blessed gains Night Vision and half fall damage. Ocean Songster draws
nearby dolphins, fish, squid and turtles; hostile aquatic mobs hesitate to select the caster.
Minecraft has no water-pressure damage, so dive-depth and pressure immunity require no separate
world rule. Boats and rain impose no additional movement penalty here.

Timed world effects and delayed hits stop when their caster dies, logs out, changes dimension
or loses the pathway. Logout clears temporary forms/screens but preserves saved Rage and
wheel deadlines. Attribute passives are removed after a pathway/Sequence loss. The Storm
key remains unavailable even at Sequence 0. None of these abilities places/breaks real blocks
or changes global weather.

## Try it and verify

```mcfunction
/sequence pathway @s "Thunder God"
/sequence set @s 5
/sequence spirituality @s 15500
```

Sneak + J to choose Sweeping Wave, then sneak + L to choose Lightning Strike. Build Rage
with ordinary combat, use H for Raging Blow, and compare dry and wet targets. `/thunderfx`
remains the cosmetic gallery; gameplay uses the normal ability keys.

```sh
./gradlew build
./gradlew runGameTestServer -PthunderGodTests
```

Sixteen real-server GameTests cover unlock affordability, free dismissals, full-price recasts,
cooldown persistence, no-target/no-bow/Sequence/Silence refusals, water-lightning arcs and
friends, metal conduction, paralysis immunity, per-cast Rage and Tipsy, Rage decay, cleansing,
projectile curtains, siren wake/fear resistance, local rain, health-passive cleanup, delayed
healing cancellation, charged arrows and wave ally safety. The standard build also runs the
existing damage/particle serialization and swing-animation checks.
