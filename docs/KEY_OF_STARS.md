# Key Of Stars: ability design

The Key Of Stars Pathway is the Door Pathway of Lord of the Mysteries, the Magician tarot card: Apprentice (9), Trickmaster (8), Astrologer (7), Scribe (6), Traveler (5), Secrets Sorcerer (4), Wanderer (3), Planeswalker (2), Key of Stars (1), Door (0). Source: the Door Pathway and Door Pathway/Abilities pages of the Lord of the Mysteries wiki.

Every key is written, with what grows on it and every passive ("What is built" at the end says how, and what was left out). This file was the plan: what every ability does, the numbers to start from, which existing parts of the spell kit build it, and which new reusable parts are worth writing first. Every look, sound and picture is already made ("Looks, sounds and pictures" at the end).

Pathway name in code: `"Key Of Stars"`. The first `KeyOfStars` class (Star Burst, Dash, Buff, Seal, Transport, Orbital, Cosmic Plague) is now the `Wanderer` Pathway (`Wanderer.java`, `"Wanderer"`); it is not part of this design.

## Theme

A pathway of freedom: doors, space, secrets, seals and copying what others can do. It walks through walls, appears elsewhere, hides things in places that do not exist, writes down other Beyonders' powers and uses them, and at the top seals, tears and replaces space itself.

Starlight is its light. Everything it does is an illusory Door: layers upon layers of doors made of starlight, and the Worms of Star (small glowing crescent worms) that crawl through them. Low Sequences are tricks and slipping away. Mid Sequences are travel, secrets and copying. High Sequences are Seals, gravity and whole alternate spaces.

Colors: pale blue-white starlight with gold highlights, deep indigo for the doors and the dark between the stars. Pathway color: `0x9DB4FF`.

Nothing here breaks blocks or lights real fire. Doors and Space only move, hide, hold and hurt creatures; the ground is never changed. (The one block change is Door Opening swinging a real door, trapdoor or gate open.)

## Pathway systems

Three small systems tie the abilities together. They replace Attendant Of Mysteries' stacks: there is no stack burst here.

| System | What it is | Where it shows up |
|---|---|---|
| Starlit | A starlight mark on a creature. It lasts 12 seconds, glows through walls for you and your allies, and counts as "initial control" in the book's sense. At most 3 marks at once; a new one replaces the oldest. | Put on by Star Divination, Invisible Hand, Space Concealment (everything caught). Needed by Exile and Starlight Seal. |
| Star Anchors | Places you saved: a faint star-door on the spot, kept for 10 minutes, at most 3 (5 from Wanderer). Saved with the player, with the dimension. | Star Anchor makes them, Traveler's Door goes to them, Wandering and Alternate World can use them later. |
| Recorded Powers | Up to 5 abilities of other Beyonders, copied and kept for one use each. They sit in the picture row right of the hotbar (the place Attendant's stacks use). | Record fills the slots, Replay empties them, Replication skips them. |

Chances and strength of a record (the book: weaker powers copy almost perfectly, stronger ones badly or not at all). `gap` = the ability's Sequence minus yours (a Sequence 9 ability seen by a Sequence 6 Beyonder is gap 3):

- Success chance: 95% if gap is 0 or more; minus 30% for every Sequence stronger than you; abilities 4 or more Sequences stronger never work. From Secrets Sorcerer (4) Angel-level abilities can work, at 15% at best.
- Strength when replayed: `0.5 + 0.1 * gap`, never below 0.3 and never above 1.0.
- Only active abilities count. Passives can't be recorded.

Status pictures above the Recorded slots (same idea as Attendant's `showStatus`): Interference, Transfiguration (shows layers left), Omnipresence, Alternate World.

## How it is laid out

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M), every key has a normal cast and a sneak cast. Four keys (2, 5, 6, 7) are wheels like Apocalypse's and Hand Of God's, where every entry has its own Sequence, cost and cooldown; on a wheel key, sneak casts the last pick again (`AbilityWheel.quickCast`).

The abilities do not each get a Sequence of their own. Sequence 8 has seven abilities (the tricks), Sequences 5 and 4 have four, Sequences 1 and 0 have three, and Sequences 9, 7, 6, 3 and 2 have two each. Sequences 3, 2, 1 and 0 are not left empty, and they also strengthen what is already there (see "Grows with the Sequence").

| Sequence | Abilities |
|---|---|
| Apprentice (9) | Door Opening, Slip Free |
| Trickmaster (8) | Flash, Black Screen, Loud Noise, Frost Ray, Gale, Tumble, Escape Trick |
| Astrologer (7) | Star Divination, Interference |
| Scribe (6) | Record, Replay |
| Traveler (5) | Traveler's Door, Blink, Invisible Hand, Star Anchor |
| Secrets Sorcerer (4) | Space Concealment, Transfiguration, Exile, Pocket Space |
| Wanderer (3) | Wandering, Space Tearing |
| Planeswalker (2) | Toy Box, Replication |
| Key of Stars (1) | Space Fragmentation, Starlight Seal, Space-Time Labyrinth |
| Door (0) | Alternate World, Omnipresence, Close the Door |

## Keys

| Key | Ability | Sequence | Normal cast | Sneak cast |
|---|---|---|---|---|
| 1 | Door Opening | Apprentice (9) | **Door Opening:** open an illusory door in the wall you face and step through it. You come out on the far side of a wall up to 6 blocks thick (there must be room to stand). Your items come with you, other people do not. Aimed at a real door, trapdoor or fence gate within 8 blocks (iron ones too) it just swings it open, for free. It works even when Teleport is rooted (the book says an Apprentice's Door Opening does); it does not work when everything is rooted. | **Slip Free:** you open the lock on yourself, or on the ally you aim at (12 blocks). Roots, holds, strings, slowness and harmful effects end at once, and nothing of that kind can be put on them again for 1.5 seconds. |
| 2 | Tricks | Trickmaster (8) | The Tricks wheel: Flash, Black Screen, Loud Noise, Frost Ray, Gale, Tumble, Escape Trick. Each has its own cost and cooldown. | The last trick again, without the wheel. |
| 3 | Astrology | Astrologer (7) | **Star Divination:** a crystal ball of starlight forms in your hand. Aimed at a creature within 48 blocks it becomes Starlit: you see its health, every effect on it and the last ability it used, and it shows through walls. Aimed at nothing, every creature within 40 blocks shows as a small star through walls for 8 seconds. If a disguise or a Faceless-style ability is ever added, the ball shows who is behind it. | **Interference:** for 20 seconds you are Unreadable: abilities that need to find, read or lock on to you (Gaze, Divination, Eye of Demon Hunting, homing shots, the threads of Spirit Body Threads) cannot pick you, and mobs further than 8 blocks forget you. The first ability that tries and fails is turned back: its caster is Starlit for 6 seconds. |
| 4 | Record | Scribe (6) | **Record:** for 8 seconds you watch. The first Beyonder ability another caster (player or Beyonder mob) uses within 32 blocks and in your sight is copied into a free Recorded slot, if the roll succeeds (see Pathway systems). A message appears: "I came, I saw, I recorded." | **Replay:** casts the oldest Recorded ability at once, from your hand toward your crosshair, free, at the reduced strength, and empties its slot. No stack, no cost. |
| 5 | Travel | Traveler (5) | The Travel wheel: Traveler's Door, Blink, Invisible Hand, Star Anchor, Wandering. Each has its own Sequence, cost and cooldown. | The last pick again, without the wheel. |
| 6 | Secrets and Space | Secrets Sorcerer (4) | The Secrets wheel: Space Concealment, Transfiguration, Exile, Pocket Space, Space Tearing. | The last pick again, without the wheel. |
| 7 | Starlight | Planeswalker (2) | The Starlight wheel, cast where you aim: Toy Box (2), Replication (2), Space Fragmentation (1), Starlight Seal (1), Space-Time Labyrinth (1), Alternate World (0), Omnipresence (0), Close the Door (0). | The last pick again, without the wheel. |

## Key 2 wheel: Tricks

All are Trickmaster (8). The book calls them "strange but relatively weak"; they are cheap and fast, and the real use is in what they set up (see Escape Trick, Starlit, Tumble into Frost Ray).

| Entry | What it does |
|---|---|
| Flash | A blinding flash in a 14 block cone, many times brighter than a camera. Enemies in it get Blindness for 3 seconds; mobs lose their target for 1 second. No damage. |
| Black Screen | A dark curtain (6 wide, 3 tall) appears 4 blocks in front of you for 8 seconds. Mobs cannot see through it, so a mob behind it loses you. You and allies walk through it freely. Projectiles pass. |
| Loud Noise | A deafening bang where you aim (24 blocks at most) or at your feet. Enemies within 8 blocks get Slowness I for 2 seconds and lose their target; mobs walk to where the bang was. A fake explosion shows. |
| Frost Ray | A ray of frost at arrow speed, 10 blocks. 14 damage, Slowness III for 3 seconds and freezing ticks, puts out fire on what it hits. Frost shows on the target; no ice is placed in the world. |
| Gale | A level 7 to 8 gale in a 12 block cone: enemies are shoved back about 6 blocks, arrows and thrown things are blown aside, fire on creatures goes out. |
| Tumble | A greased circle of 5 blocks radius, aimed up to 16 blocks away, for 6 seconds. Enemies in it slip: Slowness II, a sideways shove every half second, and one stagger (1 second) each 2 seconds. You and allies do not slip. |
| Escape Trick | After any other trick you used in the last 3 seconds (the diversion) you shift up to 10 blocks toward where you look, leaving a crumbling door behind. With no diversion it is a plain 4 block hop. A lesser Blink. |

## Key 5 wheel: Travel

| Entry | Sequence | What it does |
|---|---|---|
| Traveler's Door | Traveler (5) | Aim at a spot up to 48 blocks away. A door opens to the Spirit World and you step out of another at the spot. The further it is, the longer you wait inside (1 tick per 2 blocks). People touching you (within 2 blocks) come along. Aimed at one of your Star Anchors you go to it, at any distance in this dimension. |
| Blink | Traveler (5) | A hop of 8 blocks toward where you look, leaving an afterimage. Up to 3 in a row within 2 seconds. The hop makes you dodge the next hit (0.3 seconds). Phasing around a target is how you open and close the distance. |
| Invisible Hand | Traveler (5) | An unseen hand grabs what you look at, up to 24 blocks away. Dropped items, XP and arrows fly into your hand. A creature (not a boss) is yanked to your feet and staggered for 1 second, and is Starlit. |
| Star Anchor | Traveler (5) | Positioning: plant a Star Anchor on the spot you stand on (see Pathway systems). Cast it again while standing in one to pick it up. You always know the direction of your Anchors. |
| Wandering | Wanderer (3) | You enter the Astral World: for 8 seconds you are a trail of starlight flying at twice your sprint speed toward where you look. Blocks and creatures do not stop you; nothing sees, targets or hurts you; you cannot cast. You step out where you stop, or when you press the key again, with no fall damage. |

## Key 6 wheel: Secrets and Space

| Entry | Sequence | What it does |
|---|---|---|
| Space Concealment | Secrets Sorcerer (4) | Aimed at a spot (24 blocks) or at yourself: a sphere of 6 blocks radius is split off from the world for 10 seconds. From outside it cannot be seen into or targeted and projectiles stop at the shell. Inside, allies are hidden and slowly healed. Enemies caught in it are Starlit and cannot leave except through the Door, a lit illusory door in the shell on the side facing you. Casting it again shuts the Door: the shell closes completely for the rest of the time. |
| Transfiguration | Secrets Sorcerer (4) | You turn into layers of illusory Doors for 10 seconds. There are 5 layers. Every hit that would land on you (anything with an attacker, abilities included) tears one layer and you step 3 blocks aside unharmed. No layers left, it ends. |
| Exile | Secrets Sorcerer (4) | Needs a Starlit or held (rooted, marionette) enemy within 24 blocks. Doors swallow it and throw it into a space-time storm: for 8 seconds it cannot be seen, hit or act, then it comes back out of the same door on the same spot, hurt (40 damage). Bosses are gone for 3 seconds only. Uses up the Starlit mark. |
| Pocket Space | Secrets Sorcerer (4) | Open a door to your own Concealed space: your vanilla Ender Chest (27 slots), anywhere. |
| Space Tearing | Wanderer (3) | Aimed at an enemy within 8 blocks: a ring warns for half a second, then space tears around it. It and everything within 3 blocks take 80 damage and then burn in a Space-Time storm for 4 seconds (12 damage per second, Slowness II). |

## Key 7 wheel: Starlight

| Entry | Sequence | What it does |
|---|---|---|
| Toy Box | Planeswalker (2) | A circular veil opens where you aim (28 blocks) with an 8 block radius. Enemies in it with up to 150 maximum health shrink into wind-up toys for 8 seconds: a quarter of their size, stiff jerky movement, 60% slower, 20% of their damage. Players and bigger creatures are only slowed (Slowness III, 3 seconds). A new Toy Box releases the old one. |
| Replication | Planeswalker (2) | Copies the last Beyonder ability anyone used within 32 blocks in the past 5 seconds and casts it yourself, now, at 80% strength. It needs no slot and no roll. If nothing was cast, nothing is spent. |
| Space Fragmentation | Key of Stars (1) | Nine colored planets of starlight circle the spot you aim at (32 blocks, 12 block radius) for 6 seconds. Their gravity pulls enemies to the middle and shreds them (25 damage per second, Slowness II), bends projectiles away and slows enemy attacks by half. At the end the planets collapse in one shatter: 120 damage in 12 blocks. |
| Starlight Seal | Key of Stars (1) | Aimed at an enemy that is Starlit or held: starlight falls on it and every drop becomes a door that sinks into it. For 8 seconds it is Sealed: no abilities, no teleports. The seal repairs itself, so cleansing does not end it (Slip Free on the target does not either). Bosses for 4 seconds. Aimed at an ally: their connections are sealed for 12 seconds, so nothing can be Grafted, threaded, marked, Starlit or mind-controlled on them. |
| Space-Time Labyrinth | Key of Stars (1) | A maze of starlight fills 14 blocks around the spot you aim at for 15 seconds. Enemies inside are slowed by 50%, cannot teleport, and walking out of the edge brings them back in at the opposite side. You and allies walk freely. |
| Alternate World | Door (0) | The sky turns to a dream of stars and a kingdom of Doors covers 40 blocks around you for 60 seconds. Its rules are simple. No Exits: enemies cannot teleport or phase and loop at the edge. Open Doors: you and your allies use Door Opening, Escape Trick, Blink and Traveler's Door for free. Sealed: Seals and roots on enemies last twice as long. Cast it again to end it early. |
| Omnipresence | Door (0) | For 10 seconds you are everywhere at once. Six afterimages stand around you in a ring of 6 blocks and every ability you cast is repeated from each of them at 60% strength, for free. Anything aimed at you hits one of the seven of you at random; an afterimage that is hit fades and you take nothing. Blink is free while it lasts. |
| Close the Door | Door (0) | Every Door closes. For 15 seconds within 48 blocks every enemy loses teleports, Blink, phasing and every ability that travels. All Seals and roots you put on enemies refresh and last twice as long. Hidden enemies are forced out and cannot hide. |

## Starting numbers

Cost is Spirituality. Costs follow AttendantOfMysteries up to Sequence 3 (250 for Sequence 9 up to 12000 for Sequence 3); the higher ones are guesses, set them from your Spirituality at those Sequences. Cooldowns are in seconds.

| Entry | Cost | Cooldown |
|---|---|---|
| Door Opening / Slip Free | 250 / 400 | 3 (doors: none) / 20 |
| Flash / Black Screen / Loud Noise | 300 / 400 / 300 | 8 / 12 / 10 |
| Frost Ray / Gale / Tumble / Escape Trick | 600 / 500 / 700 / 500 | 3 / 8 / 14 / 6 |
| Star Divination / Interference | 1250 / 1250 | 12 / 30 |
| Record / Replay | 2450 / 0 | 15 / 1 |
| Traveler's Door / Blink | 3000 / 500 | 10 / 2 |
| Invisible Hand / Star Anchor | 1500 / 1000 | 6 / 5 |
| Wandering | 12000 | 40 |
| Space Concealment / Transfiguration | 6000 / 6000 | 25 / 40 |
| Exile / Pocket Space | 7000 / 250 | 30 / 3 |
| Space Tearing | 12000 | 15 |
| Toy Box / Replication | 18000 / 15000 | 30 / 10 |
| Space Fragmentation / Starlight Seal | 30000 / 25000 | 45 / 30 |
| Space-Time Labyrinth | 30000 | 60 |
| Alternate World / Omnipresence / Close the Door | 60000 / 45000 / 40000 | 120 / 90 / 90 |

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
|---|---|---|
| Door Opening | Astrologer (7) | People within 2 blocks come through with you. |
| Door Opening | Secrets Sorcerer (4) | Slip Free also ends Teleport roots, and Door Opening opens restrictions on your travel (the book's "Prohibition of Teleportation" door). |
| Door Opening | Wanderer (3) | Walls up to 16 blocks thick. Only special effects that forbid it stop you (for example a Protection Cage). |
| Escape Trick | Secrets Sorcerer (4) | The distance after a diversion grows to 14 blocks. |
| Record | Traveler (5) | 4 slots instead of 3. |
| Record | Secrets Sorcerer (4) | 5 slots, and Angel-level abilities can be recorded (at 15% chance, weakly). |
| Record | Planeswalker (2) | No failure roll for abilities at your own Sequence or weaker. Replication is its live twin. |
| Blink | Secrets Sorcerer (4) | For 3 seconds after a Blink your wheel abilities cost 30% less and cool down twice as fast (the book's "faster than normal" powers), and a Blink tears you out of mind control (Spirit Body Threads countdowns, charms). |
| Blink | Planeswalker (2) | Afterimages stay for 4 seconds and echo every ability you cast at 40% strength, up to 3 of them. Same part as Omnipresence. |
| Traveler's Door | Secrets Sorcerer (4) | Range 96 blocks. |
| Traveler's Door | Wanderer (3) | Range 192 blocks, and Anchors work across dimensions. |
| Traveler's Door | Planeswalker (2) | No waiting inside the door; you can open it to any place you can see. |
| Space Concealment | Wanderer (3) | Becomes Spatial Cage: instant cast, and you pick the single open side. |
| Space Concealment | Planeswalker (2) | 10 block radius, 20 seconds, like a Hostel. |
| Star Divination | Planeswalker (2) | Dimension Sight: range 128 blocks, and Starlit creatures show wherever they are in this dimension. |
| Transfiguration | Key of Stars (1) | 8 layers. |
| Exile | Wanderer (3) | 12 seconds. |
| Exile | Key of Stars (1) | 20 seconds, and up to 3 targets at once. |
| Space Tearing | Planeswalker (2) | Range 12 blocks, tears around you as well, radius 5. |
| Wandering | Planeswalker (2) | Becomes Symbolization: 12 seconds, and allies touching you come along. |
| Wandering | Door (0) | Becomes Conceptualization: you survive anything short of a Door, and enemies you fly through are wrapped in starlight and gone for 3 seconds. |
| Space Fragmentation | Door (0) | 40 block radius and much more damage (the book's planet-annihilation). |

## Passives

| Passive | From Sequence |
|---|---|
| Footprints of Freedom: cobwebs, sweet berry bushes and powder snow do not slow you, and roots and holds on you last 25% shorter | Apprentice (9) |
| Nimble: no fall damage under 6 blocks, and you cannot slip on Tumble | Trickmaster (8) |
| Spiritual Intuition: a soft chime when something starts hunting you or an ability is aimed at you, and a faint star when a Divination looks for you | Astrologer (7) |
| Quick Study: a small glint on any Beyonder ability cast near you that Record could copy, with the chance shown | Scribe (6) |
| Sense of Direction: your Star Anchors show as small stars at the edge of the screen. Safe Landing: no fall damage for 2 seconds after any teleport of this pathway | Traveler (5) |
| Starlit Eyes: blindness and nausea do not take, and illusions break at once. A faint orbit of Worms of Star around you | Secrets Sorcerer (4) |
| Astral Footing: Teleport roots on you last half as long, and you take no fall damage at all | Wanderer (3) |
| Afterimages: 15% of hits on you pass through an illusory door and miss | Planeswalker (2) |
| Lost Position: abilities cannot locate you from further than 24 blocks unless they are Starlit on you; you always see your own Starlit marks wherever they are in this dimension | Key of Stars (1) |
| Door of Doors: Door Opening, Escape Trick and Blink cost nothing and have no cooldown, and every Seal and root you apply lasts 50% longer | Door (0) |

## Built from the existing spell kit

| Ability | Built from |
|---|---|
| Door Opening, Escape Trick, Blink, Traveler's Door | `Spells.fits`, `Spells.freeSpotNear`, `Root.has(RootRestriction.TELEPORT)`, `FallGuard`, `SpellFx.shockRing`, a door `ModelFx`; Traveler's Door waits with `Later.run` or `Countdown` |
| Slip Free | `Spells.cleanse`; needs a way to clear roots (see new parts) |
| Flash, Loud Noise, Gale | `Spells.enemiesAround`, `Spells.push`, `ParticleShapes.cone`, `Spells.sound`, `MobEffects` |
| Frost Ray, Invisible Hand | `Shot` (like `AIR_SHOT`), `SlashFx` trail, `Spells.pull`, `SpellFx.tether` |
| Black Screen, Tumble | `ModelFx` and `SpellFx.runeCircle`, repeating pulses like `burnInFlame` |
| Star Divination, Interference | `MobEffects.GLOWING` (as in `stitchTogether`), `ScreenImages`, a crystal ball `ModelFx`, `Substitute`-style one-shot reaction |
| Record, Replay, Replication | `echo` and the `power` parameter (the pattern Attendant's projection already uses), plus the ability registry (new) |
| Transfiguration | `Substitute.arm(player, ticks, charges, handler)` with 5 charges |
| Exile, Toy Box, Space Concealment | `Marionette`-style health cap, `Concealment` for hidden allies, `Spells.enemiesAround`, plus new parts below |
| Space Tearing, Space Fragmentation | `SpellFx.warningCircle`, `Later.run` impact like `graftImpact`, `Spells.strike`, `Spells.pull` |
| Starlight Seal, Labyrinth, Alternate World, Close the Door | `Root.apply`, `RootRestriction.TELEPORT`, plus the zone part below |
| Omnipresence | `echo` generalized to several hands (see new parts) |
| Pocket Space | the vanilla Ender Chest menu |

## New reusable parts worth writing first

In the order they are needed. Each one is small and also helps other pathways.

| Part | What it is | Used by |
|---|---|---|
| `Teleport` helper | One place for every teleport: checks `Root.has(TELEPORT)` (except Door Opening), `Spells.fits`, picks the free spot, calls `FallGuard.protect`, plays the door/afterimage effect, and ends mind control if the Sequence allows. `flamingJump` in Attendant already repeats all of this by hand. | Door Opening, Escape Trick, Blink, Traveler's Door, Wandering, Transfiguration, Omnipresence; Attendant's Flaming Jump later |
| `Spells.spotBeyondWall(player, sl, maxThickness)` | Finds where Door Opening comes out: the first spot you can stand on past the wall, up to a thickness. | Door Opening |
| `Root.clear(entity)` or `Spells.breakControl(entity)` | Ends roots, holds, threads and other control on an entity. | Slip Free, Blink from Sequence 4, Starlight Seal |
| `RootRestriction.ABILITIES` | A root that only blocks casting (a silence), next to `TELEPORT` and `EVERYTHING`. | Starlight Seal, Close the Door |
| `Zone` | A timed sphere with an edge rule (`BLOCK`, `LOOP`, or `NONE`), an optional gap, and callbacks for creatures inside each tick. | Space Concealment (`BLOCK` with a gap), Labyrinth and Alternate World (`LOOP`), Close the Door (`NONE`), Black Screen. Hand Of God's Protection dome and Divine Kingdom can use the same one if they do not already |
| `Starlit` status | The mark: `Starlit.mark(target, caster, ticks)`, `Starlit.has(target, caster)`, shows through walls, limit of 3 per caster. | Divination, Invisible Hand, Exile, Seal, Space Concealment, passives |
| `Unreadable` status and `Targeting.canPick(caster, target)` | Abilities that look, read, lock on or find ask the gate first. Mind Concealment (Hand Of God) is the same thing without the reflection. | Interference, Hand Of God's Mind Concealment, Lost Position, every future "gaze" or "lock-on" |
| `AbilityRegistry` | Every ability registers `(id, pathway, sequence, cast(caster, sl, power, free))`, and every successful cast fires an event with the id and caster. Each Attendant ability already has an internal version with a `power` parameter and a start position; this makes them public. | Record, Replay, Replication, Interference's reflection, Quick Study. It is the one big piece, and no Door ability but these three needs it. |
| `SlotBar` | Pictures right of the hotbar by id prefix and count, taken from `showStackImage` and `refreshStackImages` in Attendant, with relog redraw. | Recorded Powers, Attendant's stacks, status pictures everywhere |
| `Banish` status | A creature leaves the world for N ticks (invisible, no hitbox, no AI, cannot be hurt), then comes back on the same spot with damage. | Exile, Conceptualization, any future time-stop |
| `Toy` status | Shrinks a creature with the scale attribute, slows it, cuts its damage, restores it after N ticks. | Toy Box |
| `Echo` (from Attendant's projection) | `echo`, `projectionHand` and the saved projection position moved into `combat/`, taking a list of hands instead of one. | Attendant's projection, Blink at Sequence 2, Omnipresence |
| `Afterimage` effect | `SpellFx.afterimage(sl, entity, ticks)`: a faded silhouette of the caster that fades out. | Blink, Escape Trick, Transfiguration, Omnipresence |
| `StarAnchors` | A list of saved `(dimension, position)` entries on the player, with add, remove and nearest-to-crosshair. | Star Anchor, Traveler's Door, Passive stars at the screen edge |

## Models and pictures

All made; see "Looks, sounds and pictures" at the end. The door is two models (`door_frame` and `door_leaf`, so the door can swing), the planets are the shared `orb` tinted nine ways with a `planet_ring` round some of them, and the Recorded slots use one page (`recorded`) and a gold one for Angel-level records (`recorded_angel`).

## Rules for every ability

- Same signature as Attendant's: `(Player player, Level level, ServerLevel sl, boolean bypassClassCheck)`, with a `ready(player, bypassClassCheck, sequence)` check per ability, and wheel entries each with their own Sequence. Method names like `keyOfStarsDoorOpening`.
- Nothing is spent when a cast cannot happen (no room, no target, nothing recorded): a message and `return`, like Attendant.
- Casting anything else ends Concealment-style hiding, as in Attendant; the exceptions are Wandering and Transfiguration, which are their own hiding.
- Anything copied by Record, Replay or Replication runs free, adds no stack and cannot be copied again.
- Starlit, Exile and Starlight Seal only work on enemies (`Spells.isEnemy`).

## Left out on purpose

Gas Transfer, Fog, Ignition, Object Manipulation and Electric Shock (the weaker tricks); peeking through a keyhole door in a wall; recording a creature's "state"; the Hostel and the Worm of Star charms and bullets; doors to the Mirror World, the Underworld, Dreams and the Cosmos (they need those places to exist); Replicating scenes; Position over Time, Destiny and Mind. Most fit the wheels later without changing the keys.

## Open questions

1. Record, Replay and Replication only work if every pathway's abilities are callable through one registry (`AbilityRegistry`). Is that acceptable, or should Record be limited to something simpler? (Answered: `PathwayAbilities` and `combat/Casts` instead of a new registry.)
2. The Starlight wheel has 8 entries. Is there a size limit on the wheel UI?
3. Should Star Anchors be visible to everyone, or only to their owner (if the effects layer can send to one player)?
4. Door Opening ignores Teleport roots here, as in the book. Keep that, or make it respect them? (Answered: kept.)
5. Costs from Sequence 2 up are guesses. What is the Spirituality pool at those Sequences?
6. Should Pocket Space be the vanilla Ender Chest storage, or its own? (Built as the Ender Chest for now.)

## What is built

The code is in `beyonder_characteristics/key_of_stars/`, one class per key that needs one: `KeyOfStars` (keys 1 and 3, the
Starlit mark, the passives, the cost, cooldown and free-travel rules, and the events), `KeyOfStarsTricks` (key 2),
`KeyOfStarsRecord` (key 4 and Quick Study), `KeyOfStarsTravel` (key 5), `KeyOfStarsSecrets` (key 6) and
`KeyOfStarsStarlight` (key 7). Every number is a constant at the top of its class, with the line of this plan it comes from.
`PathwayAbilities` lists all seven keys: 1, 2, 3 and 6 can be stolen; 4 (the pages are the Scribe's own), 5 (Wandering needs
its own key) and 7 (Omnipresence and the Alternate World run on this Pathway's rules) cannot.

| Part | State |
| --- | --- |
| 1 Door Opening / Slip Free | Built, with Astrologer (allies come along), Secrets Sorcerer (Teleport roots on you open) and Wanderer (walls up to 16). A real door, trapdoor or gate within 8 swings open or shut, free. |
| 2 Tricks (all seven) | Built. Escape Trick's longer shift from the Secrets Sorcerer too. Free with Door of Doors or in an Alternate World. |
| 3 Star Divination / Interference | Built, with Dimension Sight (Planeswalker). The reading tells health, Pathway and Sequence (players), effects and the last power it cast (`combat/Casts`). |
| 4 Record / Replay | Built on `combat/Casts`: the first power cast within 32 blocks and in sight while you watch, by any player of any Pathway, key or wheel entry. The roll and the strength are the ones under "Pathway systems"; Replay casts it as a copy (`Casts.recast`: free, no Sequence gate, no cooldown) with its hits at that strength (`combat/Echo`). 3 / 4 / 5 slots, Angel-level powers from the Secrets Sorcerer, no roll from the Planeswalker. The slots live in memory: kept through a relog and a death, not a server restart. Quick Study shows the chance beside the hotbar for every power Record could copy. |
| 5 Travel (all five) | Built, with every growth: Traveler's Door to Anchors in another dimension (Wanderer: look straight up; `Teleport.goTo`), Blink's quick powers (Secrets Sorcerer) and echoing afterimages (Planeswalker: up to 3, for 4 s, at 40%), Symbolization and Conceptualization. |
| 6 Secrets and Space (all five) | Built. Space Concealment is a `Dome` (enemies kept in, shots stopped) with a Door enemies may pass until you shut it; Spatial Cage (Wanderer) and the Hostel size (Planeswalker). Transfiguration is `Substitute` with one charge a layer. Exile is `status/Exile`. Pocket Space opens the vanilla Ender Chest. Space Tearing with the Planeswalker's tear round you. |
| 7 Starlight (all eight) | Built. Toy Box (`status/Toy`), Replication (`Casts.latestNear` and `Casts.recast` at 80%), Space Fragmentation (a `Zone`; Door: the planet-annihilation), Starlight Seal (`status/Seal`, on an ally `Seal.guard`), Space-Time Labyrinth and Alternate World (`combat/Enclosure`), Omnipresence (`combat/Echo` from six afterimages; a hit may strike one of them instead), Close the Door. |
| Starlit | Built (`status/Starlit`); you and allied players within 64 see your marks through walls, Planeswalker anywhere |
| Passives | All. Footprints of Freedom: roots 25% shorter, berry bushes and powder snow do not hurt; the slowing in cobwebs, bushes and powder snow is the client's own movement and is left out. Starlit Eyes: blindness and nausea do not take (there are no illusions in the mod to break yet). |

Left out or simpler than the plan:
- The Alternate World's sky does not change (that is the client's sky); its Doors, wall and stars do show.
- Close the Door doubles the Seals, roots and Starlit marks on enemies within 48 at the moment it shuts, whoever put the roots there.
- Echoes and copies weaken only hits. A copy of a power that roots, heals or moves is whole.
- An echo is skipped for this Pathway's travels (Door Opening, Escape Trick, the Travel wheel), Record, Space Concealment,
  Transfiguration, Exile, Pocket Space, Replication and the three Door powers: they would move you, or shape the world round you.
- Mobs do not use the keys, so a Beyonder mob's powers are never recorded or replicated.
- Pocket Space is the vanilla Ender Chest (27 slots), not a 54-slot space of its own.

New reusable parts (see SPELL_KIT.md): `combat/Teleport` (`throughWall`, `standNear`, `goTo` across dimensions,
`levelNamed`), `combat/Casts`, `combat/Echo`, `combat/Enclosure`, `status/Unbound`, `status/Starlit` (with `addImmunity`),
`status/Anchors`, `status/Seal`, `status/Toy`, `Root.addDurationScale`, `Unreadable.addTurnedAwayListener`,
`AbilityWheel.notCopyable`.

To try it: `/sequence pathway @s "Key Of Stars"`, `/sequence set @s 0`, `/sequence spirituality @s 100000`. Record needs a
second player casting near you.

## Looks, sounds and pictures

Everything the abilities need to look and sound right is in `KeyOfStarsFx.java` (`beyonder_characteristics/key_of_stars/`). It only shows things; the ability does the damage, holding, hiding, healing and moving, at the ticks its constants name. `/starsfx all` plays every display in game, `/starsfx <name>` one of them (TAB lists the names; there are also `real_door`, `divination_sweep`, `starlit`, `dimension_sight`, `blink_echo`, `conceptualization`, `spatial_cage`, `seal_ally`, `passives` and `statuses`).

| Ability | Normal cast | Sneak cast |
| --- | --- | --- |
| 1 Door Opening | `doorOpening(sl, player, from, to)`: a Door in the wall at `from`, another at `to` on the far side; step through at `DOOR_OPENED`. `doorStep(sl, who, at)` for each one who comes along (Astrologer); `realDoor(sl, at)` for a real door, trapdoor or gate | Slip Free: `slipFree(sl, caster, target)` (shows Unbound for 1.5 s); `travelUnbarred(sl, player)` when the Secrets Sorcerer opens a Teleport root |
| 2 Tricks (wheel) | Flash: `flash(sl, player, range)`, `blinded(sl, target, ticks)`. Black Screen: `blackScreen(sl, center, yaw, ticks, tag)` (down at `SCREEN_DOWN`), `blackScreenEnd`. Loud Noise: `loudNoise(sl, player, at)`, `deafened(sl, target, ticks)`. Frost Ray: `frostRay(sl, player, from, to)`, `frosted(sl, target, ticks, tag)`. Gale: `gale(sl, player, range)`, `galeHit(sl, target)`. Tumble: `tumble(sl, player, center, radius, ticks, tag)`, `tumbleTick` every half second, `slipped(sl, target, ticks)`, `tumbleEnd`. Escape Trick: `escapeTrick(sl, player, from, to, diversion)`. Icons `WHEEL_FLASH` ... `WHEEL_ESCAPE_TRICK` | the last pick again |
| 3 Astrology | Star Divination: `starDivination(sl, player, target)` (Starlit at `DIVINED`; null target = the sweep), `smallStar(owner, who, ticks)` for each creature the sweep finds | Interference: `interference(sl, player, ticks, tag)`, `interferenceTick` now and then, `interferenceTurned(sl, player, caster)` for the search turned back, `mobForgets(sl, mob)`, `interferenceEnd` |
| 4 Record | Record: `record(sl, player, ticks, tag)`, `recorded(sl, player, from, angel)` when a power is copied, `recordFailed(sl, player)`, `recordEnd`; the slots on screen with `recordedPages(player, angel[])` (call it whenever they change and after a relog) | Replay: `replay(sl, player)`, then the power itself |
| 5 Travel (wheel) | Traveler's Door: `travelersDoor(sl, player, from)` (inside at `TRAVEL_ENTERED`), `travelersArrive(sl, at, yaw)`. Blink: `blink(sl, player, from, to, chain)`, `blinkDodge(sl, player)`, `blinkEcho(sl, player, at, ticks, tag)` (Planeswalker), `quickened(player, ticks)` (Secrets Sorcerer). Invisible Hand: `invisibleHand(sl, player, target)` (grabs at `HAND_GRABS`), `invisibleHandItem(sl, player, at)`, `yanked(sl, target)`. Star Anchor: `starAnchor(sl, player, at, ticks, tag)`, `anchorLift(sl, at, tag)`, `anchorDirection(owner, anchor)` each second. Wandering: `wandering(sl, player, ticks, Form.X, tag)`, `wanderingTick` every 2 ticks, `conceptualized(sl, enemy)`, `wanderingEnd`. Icons `WHEEL_TRAVELERS_DOOR` ... `WHEEL_WANDERING` | the last pick again |
| 6 Secrets and Space (wheel) | Space Concealment: `spaceConcealment(sl, player, center, radius, ticks, doorYaw, cage, tag)` (closed at `CONCEALED`; `cage` = the Spatial Cage), `concealmentShut`, `concealedHeal(sl, ally)`, `concealmentEnd`. Transfiguration: `transfiguration(sl, player, layers, ticks, tag)`, `layerTorn(sl, player, from, to, layersLeft)`, `transfigurationEnd`. Exile: `exile(sl, player, target)` (swallowed at `EXILE_SWALLOWED`), `exiled(sl, target, at, ticks, tag)`, `exileReturn(sl, at, tag)`. Pocket Space: `pocketSpace(sl, player)`. Space Tearing: `spaceTearing(sl, player, target, radius)` (tears at `TEAR_LANDS`), `storm(sl, target, ticks, tag)`, `stormTick` each second. Icons `WHEEL_SPACE_CONCEALMENT` ... `WHEEL_SPACE_TEARING` | the last pick again |
| 7 Starlight (wheel) | Toy Box: `toyBox(sl, player, center, radius, ticks, tag)`, `toyed(sl, target, ticks, tag)` (shrunk at `TOY_SHRUNK`), `toyTick`, `toySlowed(sl, target, ticks)`, `toyReleased`, `toyBoxEnd`. Replication: `replication(sl, player, source)`. Space Fragmentation: `spaceFragmentation(sl, player, center, radius, ticks, tag)`, `fragmentationTick` every few ticks, `fragmentationCollapse`. Starlight Seal: `starlightSeal(sl, player, target, ticks, tag)` (Sealed at `SEALED_AT`), `sealRepaired(sl, target)`, `sealEnd`; on an ally `sealAlly(sl, player, ally, ticks, tag)`. Space-Time Labyrinth: `labyrinth(sl, player, center, radius, ticks, tag)` (risen at `LABYRINTH_RISEN`), `lost(sl, target, ticks)`, `labyrinthLoop(sl, from, to)`, `labyrinthEnd`. Alternate World: `alternateWorld(sl, player, center, radius, ticks, tag)` (unfolded at `WORLD_UNFOLDED`), `alternateWorldTick` each second, `alternateWorldEnd`. Omnipresence: `omnipresence(sl, player, ticks, tag)`, `afterimageAt(player, index)`, `omnipresenceEcho(sl, player, index)`, `afterimageStruck(sl, player, tag, index)`, `omnipresenceEnd`. Close the Door: `closeTheDoor(sl, player, center, radius, ticks, tag)` (shut at `DOORS_SHUT`), `doorsClosed(sl, target, ticks, tag)`, `forcedOut(sl, who)`. Icons `WHEEL_TOY_BOX` ... `WHEEL_CLOSE_THE_DOOR` | the last pick again |
| Starlit | `starlit(sl, caster, target, ticks, tag)`, `starlitSeen(viewer, who)` each second for the caster and each ally (only they see it through walls), `dimensionSight(owner, who)` (Planeswalker), `starlitEnd` | |
| Passives | `wormAura(sl, player, ticks, tag)` (Secrets Sorcerer), `intuition(owner, from)`, `divinationFelt(owner)`, `quickStudy(owner, caster)` (show the chance in the action bar), `safeLanding(sl, player)`, `illusionBroken(sl, player)`, `afterimageMiss(sl, player)`, `doorOfDoors(sl, player)` (only the owner sees or hears the ones that take `owner`) | |

Every timed cast can show its picture and clock above the hotbar: `KeyOfStarsFx.status(player, Status.X, ticks)`, and `statusEnd` to take it away (24 statuses: 12 for what the Beyonder holds, 12 for what is done to others when they are players; most displays above already show theirs). `layersLeft(player, layers, ticks)` shows Transfiguration with the layers left. The Pathway's icon beside the hotbar is registered in `PathwayHudIcon` under `"Key Of Stars"`, its soul color in `DarkAngel.pathwayColor`.

Things to know when the abilities are written:
- Models stuck to an entity (the Seal, the Starlit mark, the toy's key, the veil) are taken off with `ParticleShapes.clearModels(sl, entity, tag)`; the ones on the ground (the shell, the maze, the Alternate World) with the same tag from any spot, so give every cast its own tag. Omnipresence tags each afterimage `tag + "_" + index`.
- The Alternate World stands up to 36 doors (two models each) and a ring of up to 40 wall panels; at radius 40 that is the most models any display here makes at once.
- The planets and worms orbit by a far pivot and `spin`, so they need no server ticks; Space Fragmentation's pull only needs `fragmentationTick`.

What is in the repository, all of it generated by the scripts in `tools/model_gen/key_of_stars/` (see "Key Of Stars: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)):

| What | Where | Made by |
| --- | --- | --- |
| 28 model files: the Door (frame and leaf), the crystal ball, a Worm of Star, the Key of Stars, a toy's wind-up key, the Invisible Hand, the Scribe's book and quill; the pictures: the star gate, a sky map, a Star Anchor, Tumble's slick, the Labyrinth, Toy Box's veil, the Seal's ring, the kingdom of Doors, a planet's ring, layered doors, the Starlit mark, Interference's veil (animated), the storm (animated), a rift in space, a recorded page, Black Screen's curtain (animated), a panel of split-off space (animated), a barred door and an afterimage | `models/model_fx/key_of_stars/`, `textures/model_fx/key_of_stars/` | `models.py` (+ `textures.py`) |
| 25 wheel pictures (32 px), 24 status pictures, 2 Recorded pages and 8 layer pictures (16 px), the Pathway icon | `textures/gui/ability_menus/key_of_stars/`, `textures/gui/key_of_stars/`, `textures/gui/pathway_icons/key_of_stars.png` | `icons.py` |
| 8 particle pictures: `fx/star_door`, `fx/star_worm`, `fx/star_key`, `fx/wind_key`, `fx/sound_wave`, `fx/planet`, `fx/rift_crack`, `fx/maze` | `textures/particle/fx/` | `particles.py` |
| 48 sounds (51 files): a Door opening and someone stepping out, Slip Free's lock, the seven tricks, the crystal ball, the Starlit chime, Interference and a search turned back, Record watching, a power recorded, Replay, Replication, Traveler's Door and the arrival, three Blinks, the Invisible Hand, a Star Anchor planted and lifted, Wandering and its end, Space Concealment and the Door shutting, Transfiguration and a torn layer, Exile and the return, Pocket Space, the strain and the tear of Space Tearing, Toy Box and the clockwork, Space Fragmentation and its collapse, the Starlight Seal, the Labyrinth and a loop, the Alternate World, Omnipresence, Close the Door, the Intuition chime, Quick Study's glint and a Key of Stars turning | `sounds/key_of_stars/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` (`STARS_...`) | `sounds.py` |

