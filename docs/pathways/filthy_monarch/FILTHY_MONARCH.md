# Filthy Monarch: ability design

Design only · for the Circle Of Inevitability repo · drafted by Claude, November 13, 2026 · no code written yet

The Filthy Monarch is the Abyss Pathway of Lord of the Mysteries (the Devil pathway, tarot card XV, "Criminal Pathway" and "Pathway of Devils" in the wiki): Criminal (9), Unwinged Angel (8), Serial Killer (7), Devil (6), Desire Apostle (5), Demon (4), Blatherer (3), Bloody Archduke (2), Filthy Monarch (1), Abyss (0). Above the Sequence: Father of Devils. Sefirah: Tenebrous World. Source: the Abyss Pathway and Abyss Pathway/Abilities pages of the Lord of the Mysteries wiki. The wiki gives the Criminal (9) only a strong body and weapon skill, and the Blatherer (3) only an authority (the voice), so those are designed from the Pathway's themes. Sequences 2, 1 and 0 are written out in full.

**Which names are the wiki's and which are mine.** Most entry names come from the wiki (Sulfur-style fire as "poisonous flames", Flame Cage, Hellfire, Acid Swamp, Corrosive Fog, Filthy Illusion, Desire Control, Desire Incarnation, Desire Symbiosis, Shadow-Shift, Mental Shock, Seed of Malice, Devil Worship, Killing Spree, the Language of Foulness words, Blather, Flames of the Abyss, The Corroder, the Abyssal Sea). **Made up because the wiki has no named ability:** Lawless Lunge, Dirty Trick, Sulfur Flame, Blood Ignition, Blood Burst, Tide of Filth, the Offerings counter and the four wheel titles. The wiki gives the Filthy Monarch (1) only its authorities (Filth, Corruption, Corrosion, Flames), the King of Filth and a form, so Tide of Filth is built from those.

Pathway name in code: `"Filthy Monarch"` (named after Sequence 1, like `"Prince Of Abolition"` and `"Thunder God"`).

**Status: design.** This file is the plan the code will follow. Nothing here is built, and every number is a starting value.

## Theme

Devils: malice, desire, blood and filth. The wiki calls it the Pathway of the criminal who stops holding back: a Criminal who still has a conscience, an Unwinged Angel who has lost it, a Serial Killer who kills to please the Devils, a Devil who loses control, an Apostle who plays on desire, a Demon who wins by cunning, a Blatherer whose voice is a weapon, a Bloody Archduke who cannot be killed while its malice lives, a Monarch of filth, corrosion and flame, and the Abyss itself, "the Abyss is me, and I am the Abyss".

It is a **combat Pathway**: most abilities are one press, one clearly named attack with damage, and the few that are not (forms, shields) make the next attacks hit harder. Say what it does in its name: a Fireball is a fireball, Die is a word that kills.

Colors: venetian red for the Pathway (`0xC80815`, the wiki's symbol color), pale sulfur blue for the Abyss flame (`0x7FD8FF`, core `0xE6FAFF`), abyss black (`0x120A0E`), congealed blood (`0x8A0F1A`) and a sickly filth green-brown (`0x5C5A1E`). The Abyss fire is **pale blue and black, never red-orange**, so it does not look like Conqueror's fire. Soul color (Grazing, Beyonder mobs): sulfur blue `0xB07FD8FF`, not red, because Dark Angel's is already crimson.

**World rule (same as Prince Of Abolition and Thunder God): nothing here breaks or places real blocks.** Lava, swamps, sludge, fog and kingdoms are statuses with visuals. Fire is a look and a burning status on entities, not real fire on blocks.

## Depravity (this Pathway's own mechanic)

Attendant Of Mysteries has stacks, Prince Of Abolition has Order, Thunder God has Rage; the Filthy Monarch has **Depravity**, the wiki's Seed of Depravity.

- **A Seed** is a mark on an enemy, put there by your abilities (each entry below says "+N Seeds"). Up to **5 Seeds** per enemy, yours only (another Filthy Monarch's Seeds do not count for you). One wears off every 6 seconds once you stop planting. Stored with `Marks.add(target, player, "filthy_monarch_depravity", 5, 120)`.
- **What a Seed does by itself:** the enemy takes **+4% damage from you** for every Seed (+20% at 5). Shown as small black seed sigils over its head (`SpellFx.overHead` with the count).
- **What spends them:** a few finishers read the count and clear it (`Marks.take`): **Die**, **Blasphemy**, **Blood Ignition** and **Blather**. They are stronger the more Seeds there are. Nothing else needs them, so the Pathway plays fine with no Seeds and plays better with them.
- **How they are planted:** Sulfur Flame, Sulfur Fireball, Hellfire, Flames of the Abyss, Corruption, Desire Control, Mental Shock, Shadow-Shift, Filthy Illusion, Seed of Malice, Desire Incarnation, the Abyssal Sea, and, from Desire Apostle (5), every third melee hit on the same enemy.

## How it is laid out

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M), every key has a normal cast and a sneak cast. Keys 1 to 3 are single abilities with a normal and a sneak form. Keys 4 to 7 are wheels (`AbilityWheel`) where every entry has its own Sequence, cost and cooldown.

**Wheels work the Prince Of Abolition way: sneak opens the wheel, a normal press casts the last pick again.** With no pick yet, the normal press opens the wheel. That means in a fight you pick once and then just press the key to keep dealing damage (question 1 if you want the other convention).

**Cooldowns, as you asked:**

- **Keys 1 to 3 have no cooldown at all**, normal or sneak.
- **Recasting the same wheel entry has no cooldown** (`quickCastCooldown(false)`). It costs the same as the first cast (`quickCastCost(1)`).
- Picking an entry **from the open wheel** starts that entry's cooldown (listed per wheel below). It is what stops you from hopping between three heavy entries; it never touches the entry you are repeating. (Question 2: say so if you want no cooldowns on picks either; every entry then lists 0.)

**Hover text:** every wheel entry has a one-line description, shown when the mouse is over its icon on the wheel (`AbilityWheel.description(text)` right after `.entry(...)`). The text is in the tables of each wheel, written to say plainly what the entry does.

The abilities do not each get a Sequence of their own. Sequence 6 and Sequence 4 have many entries, Sequence 3 has one and the Sequences 9 to 7 have one key each. No Sequence from 9 to 0 is left without something to press.

| Sequence | Abilities |
|---|---|
| Criminal (9) | Lawless Lunge, Dirty Trick |
| Unwinged Angel (8) | Sulfur Flame, Sink |
| Serial Killer (7) | Killing Spree, Devil Worship |
| Devil (6) | Sulfur Fireball, Devil Form, Sword of Lava, Flame Cage, Volcanic Eruption, Toxic Vapor, Slow, Die, Corruption |
| Desire Apostle (5) | Shadow-Shift, Desire Control, Mental Shock, Desire Incarnation, Desire Symbiosis |
| Demon (4) | Hellfire, Filthy Illusion, Confusion, Sloth, Blasphemy, Acid Swamp |
| Blatherer (3) | Blather |
| Bloody Archduke (2) | Seed of Malice, Blood Ignition, Blood Burst |
| Filthy Monarch (1) | Flames of the Abyss, Tide of Filth, The Corroder |
| Abyss (0) | Abyssal Sea, The Abyss |

35 abilities in all: 6 on three single keys, and 29 in four wheels (8, 7, 7 and 7 entries).

## Keys

| Key | Ability | Unlock | Normal cast | Sneak cast |
|---|---|---|---|---|
| 1 | Criminal's Kit | Criminal (9) | **Lawless Lunge** | **Dirty Trick** |
| 2 | Demonic Attribute | Unwinged Angel (8) | **Sulfur Flame** | **Sink** |
| 3 | Serial Killer | Serial Killer (7) | **Killing Spree** | **Devil Worship** |
| 4 | Devil Arts wheel | Devil (6) | Recast the last Devil Arts pick | Open the Devil Arts wheel |
| 5 | Desire and Malice wheel | Desire Apostle (5) | Recast the last pick | Open the wheel |
| 6 | Foul Tongue wheel | Devil (6) | Recast the last pick | Open the wheel |
| 7 | Blood and Filth wheel | Demon (4) | Recast the last pick | Open the wheel |

### Key 1: Criminal's Kit (Criminal, 9). Cost 10 each

| Cast | What it does |
|---|---|
| Normal: **Lawless Lunge** | You launch yourself at the enemy you look at (it must be within 8 blocks and in sight) and hit it with whatever you hold: **40 damage plus the held item's own attack damage**, and it is staggered for half a second. You land beside it with no fall damage. Any item works, a sword, a bow, a spoon or an empty hand (the wiki's "a spoon"): an empty hand does the 40 alone. No enemy in reach: refused, nothing spent. |
| Sneak: **Dirty Trick** | A fistful of grit and blood in the face of the enemy you look at (within 6 blocks): **15 damage**, blinded for 3 seconds and 40% of its attacks miss for 3 seconds. The Criminal does not fight fair. |

### Key 2: Demonic Attribute (Unwinged Angel, 8). Cost 200 each

| Cast | What it does |
|---|---|
| Normal: **Sulfur Flame** | A jet of pale blue sulfur fire out of your palm, a cone 9 blocks long and 70 degrees wide: **45 damage**, everything hit burns for 4 seconds (4 a second), is poisoned for 4 seconds and takes **+1 Seed**. The wiki's "poisonous flames". Nothing real is lit. |
| Sneak: **Sink** | You raise your arms and press both palms down: the enemy you look at (up to 20 blocks) sinks: **10 damage**, Slowness III and no jumping for 4 seconds (bosses 2). If it is in the air it is slammed to the ground for 20 more. The wiki's Slowness, "as though the wings they used for flying had been snapped". |

### Key 3: Serial Killer (Serial Killer, 7). Costs 700 and 900

The Serial Killer's offerings: every enemy you kill adds one **Offering** (up to 5, each lasts 60 seconds). Shown by a small counter beside the hotbar, `[sigil] Offerings 3/5` (`HudCounter`). The wiki: special killings please the Devils, and a ritual that falls short still works, only weaker.

| Cast | What it does |
|---|---|
| Normal: **Killing Spree** (700) | Three rapid cuts at everything in a 60 degree cone, 4 blocks in front of you: **18 damage each** (54 in all), each cut opens a wound (Bleeding, 2 a second for 4 seconds). Then the bloodlust: for 8 seconds Speed II and **+25% damage**. Every kill while it lasts adds 3 seconds (up to 20). It works with no enemy in front of you, as just the bloodlust. |
| Sneak: **Devil Worship** (900) | A sigil of black and blue fire flares at your feet and a Devil's projection climbs out of the Abyss and fights for you for 20 seconds (it hits what you last hit): **120 health, 25 damage a swipe**, and every Offering you hold adds 20% to both. With 3 or more Offerings it also hurls a Sulfur Fireball every 4 seconds (28 damage). With **no** Offering the ritual is "cut short": it comes anyway, for 10 seconds at half strength. One projection at a time: a new one replaces the old. It vanishes without drops. |

## Wheels

### Key 4 wheel: Devil Arts (flame and the Devil's body, from Devil, 6)

The wiki's Devil: Devil Transformation and its spells. The hover text is what the wheel shows.

| Entry | Sequence | What it does | Hover text |
|---|---|---|---|
| Sulfur Fireball | Devil (6) | Fireballs of pale blue sulfur fire fly where you look (up to 40 blocks) and burst on the first enemy or block: **3 of them in a fan, 28 damage each** in a 3 block burst, poisons and burns for 3 seconds, **+1 Seed** per enemy hit (once per cast). In **Devil Form: a barrage of 12** at 16 damage each. The wiki: 3 outside the transformation, 10 to 20 inside. | Fire 3 sulfur fireballs (12 in Devil Form). Each bursts for damage, poison and fire. |
| Devil Form | Devil (6) | Your body changes: ram horns, deep black armored skin, bat wings of pale blue flame, nearly 3 blocks tall. For 60 seconds: Strength II, Resistance I, Speed I, 1.5 blocks more reach, half knockback, your blows burn for 3 seconds, and Sulfur Fireball becomes the barrage. Cast again to drop it (free). If it runs out you are Spent for 5 seconds (Weakness I, Slowness I): "a Devil's greatest weakness is that they lose control". | Become a 3 block Devil for 60 s: strong, tough, fast. Fireballs become a barrage. |
| Sword of Lava | Devil (6) | A scimitar of molten fire forms in your hand and you cut a wide arc in front of you (reach 5, 140 degrees): **85 damage** that armor does not soften (it is fire), burning for 4 seconds. Cast again right away for the next swing. | A wide cut with a blade of lava: 85 damage in front of you, ignores armor. |
| Flame Cage | Devil (6) | A blue flame lights in your palm. Aimed at an enemy (up to 18 blocks) it solidifies into a cage of flame bars around it: **held for 3 seconds** (bosses 1.5), 10 damage a second, and its own projectiles stop at the bars. Aimed at nothing: a cage of flame stands in front of you for 4 seconds, 3 blocks wide, and catches every enemy projectile that touches it (the wiki's caught bullets). | Cage the enemy you look at in flame bars for 3 s. At nothing: a flame screen that catches arrows. |
| Volcanic Eruption | Devil (6) | You mark the ground you aim at (up to 22 blocks). A warning ring 7 blocks wide shows for 1 second, then the ground erupts in a column of fire: **120 damage at the middle, 80 at the rim**, everything thrown 8 blocks up and burning for 5 seconds. The ground glows for 4 seconds after (8 a second). | A warned eruption: 80 to 120 damage in 7 blocks, throws enemies into the air. |
| Toxic Vapor | Devil (6) | A cloud of highly toxic black vapor billows where you aim (up to 20 blocks), 6 blocks wide for 10 seconds: Poison II and **8 damage a second** to enemies inside, blinded for the first second. You and your allies are not affected. From Demon (4) it becomes **Corrosive Fog** (see "Grows with the Sequence"). | A poison cloud for 10 s: 8 damage a second to everything inside. |
| Hellfire | Demon (4) | The Demon's flame: after a 1 second warning circle under each, pillars of pale blue fire burst under **up to 8 of the nearest enemies within 16 blocks** that you can see: **110 damage** each and burning for 5 seconds (6 a second), **+1 Seed**. It burns the soul: Fire Resistance and water do not help against it. | Pillars of hellfire under the 8 nearest enemies in sight: 110 damage each. |
| Flames of the Abyss | Filthy Monarch (1) | The Monarch's own flame, black fire edged with pale blue that burns, corrodes, poisons and corrupts at once. You stand still and pour a torrent for 3 seconds, a line 30 blocks long and 5 wide that goes through everything: **90 damage a second**, every enemy in it burns, is poisoned and **Corroded** (rot, armor wears) and takes **+1 Seed a second**. Most Beyonders only defend against burning, so Fire Resistance and the like stop only a third of it. From Abyss (0): 50 blocks, 10 wide, 5 seconds. | A 3 s torrent of abyssal flame: 90 damage a second, burns, poisons and corrodes. |

Costs and cooldowns (cooldown only when picked from the wheel):

| Entry | Cost | Cooldown |
|---|---|---|
| Sulfur Fireball, Devil Form, Sword of Lava | 1200, 2500, 1800 | 4 s, 30 s, 6 s |
| Flame Cage, Volcanic Eruption, Toxic Vapor | 1800, 2500, 1500 | 10 s, 12 s, 10 s |
| Hellfire | 6000 | 15 s |
| Flames of the Abyss | 30000 | 20 s |

### Key 5 wheel: Desire and Malice (the mind, shadows and the seed, from Desire Apostle, 5)

| Entry | Sequence | What it does | Hover text |
|---|---|---|---|
| Shadow-Shift | Desire Apostle (5) | You liquefy into countless black shadows and re-form where you aim (up to 24 blocks, never inside a block). The shadows rake every enemy on the path: **30 damage and +1 Seed**. A shadow of you stays behind for 1.5 seconds, then bursts: **40 damage** in 3 blocks. The wiki's Shadow-Shifting, also an escape. | Flow into shadow and re-form up to 24 blocks away, hurting everything on the way. |
| Desire Control | Desire Apostle (5) | The strongest feeling of the enemy you look at (up to 24 blocks) breaks loose: **40 damage and +3 Seeds**. For 5 seconds it cannot control itself: a mob attacks whatever is nearest (another mob, too), a player's blows go wide 40% of the time and it cannot sprint. If this brings it to 5 Seeds, its desire erupts beyond what the body can take: **+100 damage** and a 1 second stagger. From Demon (4) it hits everything within 10 blocks of the target as well. | Inflame an enemy's desire: damage, 3 Seeds, it loses control for 5 s. |
| Mental Shock | Desire Apostle (5) | Your two horns ignite and a wave hits the minds of every enemy in a cone 12 blocks long and 70 degrees wide: **55 damage that armor does not soften**, stagger for 1 second, afraid for 2 seconds and **+1 Seed**. The wiki: a forced attack on the psyche that induces emotion "in an unavoidable manner". | Burn your horns: a mental blast in a cone. 55 damage, stagger, fear. |
| Desire Incarnation | Desire Apostle (5) | Your body becomes a shadowy black liquid of every dark feeling. For 6 seconds you flow: **80% less damage taken**, Speed II, mobs lose track of you (you can still strike), and every enemy within 2.5 blocks takes **25 damage a second**, is slowed and gets **+1 Seed a second**. Cast again to re-form (free). | Become black liquid for 6 s: take 80% less damage and burn enemies you flow through. |
| Desire Symbiosis | Desire Apostle (5) | You tie yourself to a desire of the enemy you look at (up to 24 blocks) for 12 seconds. While it lives within 40 blocks: **25% of every blow you take lands on it instead**, and the first blow that would kill you leaves you on 1 health with 1.5 seconds of invulnerability (the link then snaps). "You cannot be completely killed while the desire lasts." | Link your life to an enemy: it takes 25% of your damage and you survive one killing blow. |
| Filthy Illusion | Demon (4) | Three phantoms of filth, in your shape, rush out of you and hunt the nearest enemies (24 blocks). Each bursts on contact: **50 damage** in 3 blocks and **+1 Seed**. For 6 seconds mobs that were hunting you go after the phantoms instead. | Send 3 filth phantoms after the nearest enemies. They burst for 50 damage each. |
| Seed of Malice | Bloody Archduke (2) | You plant a Seed of Malice in the psyche of the enemy you look at (up to 32 blocks): **40 damage, +2 Seeds** and 3 seconds of pure fear (the mind goes blank). It stays for 90 seconds. If you would die in that time while it lives, you do not: you dissolve into black liquid and rise out of its mind, igniting its blood and desire. **It takes 50% of its current health** (bosses 10% of their maximum) and you stand at its feet with 40% health, invulnerable for 2 seconds. If it dies first, the seed rots away. One at a time. "Malice never dies, neither do Demons." | Plant a malice seed in an enemy: if you die, you rise from its mind and it takes half its health. |

| Entry | Cost | Cooldown |
|---|---|---|
| Shadow-Shift, Desire Control, Mental Shock | 2500, 3000, 3500 | 6 s, 10 s, 12 s |
| Desire Incarnation, Desire Symbiosis | 4000, 4500 | 20 s, 30 s |
| Filthy Illusion | 6000 | 18 s |
| Seed of Malice | 25000 | 60 s |

### Key 6 wheel: Foul Tongue (the Language of Foulness, from Devil, 6)

Every entry is a word of the Devil Language, said aloud. A Silenced caster cannot use them (`Silence` blocks the key), and nothing here needs a weapon. From the Blatherer (3) every single-target word reaches twice as far and does not need line of sight (see "Grows with the Sequence").

| Entry | Sequence | What it does | Hover text |
|---|---|---|---|
| Slow | Devil (6) | You say "Slow": every enemy within 8 blocks goes numb and nearly stops (Slowness V) for 2 seconds (bosses 1) and takes **15 damage**. The wiki: everyone within 7 to 8 meters, for 2 seconds. | Everything within 8 blocks nearly stops for 2 s and takes 15 damage. |
| Die | Devil (6) | You say "Die": the enemy you look at (up to 24 blocks) feels an invisible hand close on its heart: **60 damage plus 30 for every Seed**, and all Seeds are spent. It coughs rusty blood (Bleeding, 3 a second for 4 seconds). With 3 or more Seeds, a non-boss at 30% health or less **dies outright**. | A word of death: 60 damage plus 30 per Seed. Kills weak enemies with 3+ Seeds. |
| Corruption | Devil (6) | You say "Corruption": a viscous black liquid coats everything within 4 blocks of the enemy you look at (or the spot you aim at, up to 22 blocks) and bursts into corrupted black fog: **45 damage** at once and **+2 Seeds**, then the fog stays for 5 seconds (6 a second). | A burst of black fog on a spot: 45 damage, then 6 a second. Plants 2 Seeds. |
| Confusion | Demon (4) | You say "Confusion": every enemy within 12 blocks is confused for 6 seconds. 40% of its attacks go wide, a mob attacks whatever is nearest (its friends too) and a player's view swims. | Confuse everything within 12 blocks for 6 s: they miss and fight each other. |
| Sloth | Demon (4) | You say "Sloth": the enemy you look at (up to 24 blocks) loses all will to act for 8 seconds: Slowness III, Weakness II, attacks 50% slower, and **12 damage a second** while it lingers. | An enemy goes lazy for 8 s: slow, weak, hits half as fast, and rots. |
| Blasphemy | Demon (4) | You say "Blasphemy": the enemy you look at (up to 24 blocks) recoils in mind and body: **130 damage that armor does not soften** and Silenced for 3 seconds (no abilities). **Each Seed adds 20 damage and 1 second of silence**, and is spent. | A forbidden word: 130 damage and a 3 s silence. More with Seeds. |
| Blather | Blatherer (3) | The Blatherer's voice slips into the ear of an enemy and cannot be shut out. It picks the enemy you look at (up to 40 blocks), or, with none, **the enemy you carry the most Seeds on within 100 blocks, through walls and out of sight**: **150 damage plus 40 for every Seed** (spent), confused for 3 seconds, and cursed: it takes **20% more damage from you for 12 seconds**. The wiki: a voice projected from far away that kills a target in a safe place. | Your voice reaches an enemy anywhere: 150 damage plus 40 per Seed, then a curse. |

| Entry | Cost | Cooldown |
|---|---|---|
| Slow, Die, Corruption | 1000, 2000, 2000 | 5 s, 6 s, 8 s |
| Confusion, Sloth, Blasphemy | 5000, 4500, 6000 | 15 s, 12 s, 10 s |
| Blather | 12000 | 8 s |

### Key 7 wheel: Blood and Filth (blood, corrosion and the Abyss, from Demon, 4)

| Entry | Sequence | What it does | Hover text |
|---|---|---|---|
| Acid Swamp | Demon (4) | The ground where you aim (up to 24 blocks) turns into a stinking acid swamp, 8 blocks wide for 12 seconds. Enemies in it sink (Slowness II), take **14 damage a second**, their armor wears three times faster, and after 3 seconds inside they start to **Corrode**, worse the longer they stay. | An acid swamp for 12 s: slows, burns armor, 14 damage a second, then rot. |
| Blood Ignition | Bloody Archduke (2) | You ignite the blood and desire of the enemy you look at (up to 32 blocks). It bleeds from every opening and burns from within for 6 seconds: **25 damage a second plus 10 for every Seed** (spent), it cannot heal, and enemies within 3 blocks of it take half. Bosses take at most 3% of their maximum health a second. | Ignite an enemy's blood: 25 damage a second for 6 s, more with Seeds. No healing. |
| Blood Burst | Bloody Archduke (2) | You open your veins: **20% of your current health is the price** (it never kills you, and it is refused below 6 health) and your blood bursts out in all directions, 9 blocks wide: **120 damage**, thrown 6 blocks back, everything hit is Corroded and poisoned (Poison II, 6 seconds). Your blood regrows: Regeneration II for 5 seconds. The wiki's Blood Sovereign: own blood used to corrode and pollute. | Burst your blood out: pay 20% health, 120 damage in 9 blocks, corrodes. |
| Tide of Filth | Filthy Monarch (1) | The King of Filth calls the unclean: a tide of foul sludge 20 blocks wide rolls 24 blocks ahead of you: **150 damage**, thrown aside, everything hit is Corroded. The sludge stays for 8 seconds (Slowness II, 12 a second) and up to 6 hands of filth rise out of it and hold the nearest enemies in it for 2 seconds. | A wave of sludge: 150 damage, corrodes, leaves slowing filth with grasping hands. |
| The Corroder | Filthy Monarch (1) | A thin beam of continuous corrosion at the enemy you look at (up to 40 blocks) for 4 seconds: **30, 60, 120 and 240 damage** in the four seconds (450 in all) and it Corrodes. It eats enemy barriers: a shell or wall the beam crosses (a `Dome` or `Wall`) is weakened at once and falls after 2 seconds of the beam. "Even the strongest barrier will be penetrated." | A 4 s corrosion beam that doubles every second (450 total) and eats enemy barriers. |
| Abyssal Sea | Abyss (0) | Your Divine Kingdom, the Abyssal Sea of the Heart: a shell 32 blocks wide stands around you for 60 seconds and nothing crosses it either way. Inside, black sea and pale flame: every enemy gains **+1 Seed a second** and takes **15 damage a second for every Seed**; at 5 Seeds it sees what it fears most (afraid of you for 3 seconds, once every 10). **Every entry of this Pathway costs half** while you stand in it. Cast again to lift it (free). | A 32 block kingdom for 60 s: Seeds build, damage grows, your costs halve. |
| The Abyss | Abyss (0) | "The Abyss is me, and I am the Abyss." For 30 seconds you become its form, whatever each viewer fears or craves most: **+50% damage**, **40% less damage taken**, every blow plants a Seed and **heals you for 5% of the damage**, your entries cost 25% less, and every 6 seconds each enemy within 12 blocks is afraid of you for 2 seconds. Cast again to end it (free). | Become the Abyss for 30 s: +50% damage, 40% less damage, blows heal you. |

| Entry | Cost | Cooldown |
|---|---|---|
| Acid Swamp | 6000 | 15 s |
| Blood Ignition, Blood Burst | 18000, 15000 | 12 s, 10 s |
| Tide of Filth, The Corroder | 30000, 30000 | 20 s, 15 s |
| Abyssal Sea, The Abyss | 50000, 40000 | 120 s, 90 s |

## Grows with the Sequence

These do not get their own entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
|---|---|---|
| Lawless Lunge | every Sequence | +10% damage for every Sequence stronger than 9. At Devil (6) reach 12 and the target burns for 3 seconds |
| Sulfur Flame | Devil (6) | 12 blocks long and Poison II. At Demon (4) 16 blocks long and +2 Seeds |
| Sink | Desire Apostle (5) | Enemies within 3 blocks of the target sink too, for half the time |
| Killing Spree | Devil (6) | Lasts 12 seconds. At Blatherer (3) every kill during it heals you 10% of your health |
| Devil Worship | Demon (4) / Bloody Archduke (2) | Two projections at once, three from Bloody Archduke |
| Sulfur Fireball | Demon (4) | The balls home on the nearest enemy near their path (a little) |
| Devil Form | Demon (4) | Becomes **Demon Form**, a partial Mythical Creature Form: 1.9 times your size, Strength III, and the barrage is 20 balls at 14 damage |
| Toxic Vapor | Demon (4) | Becomes **Corrosive Fog**: 9 blocks wide, enemies Corrode after 3 seconds inside and their armor wears twice as fast |
| Desire Control | Demon (4) | Hits every enemy within 10 blocks of the target too: no need to wait for a feeling, as the wiki says |
| Every single-target word of the Foul Tongue (Die, Sloth, Blasphemy) | Blatherer (3) | Reach doubled and no line of sight needed |
| Flames of the Abyss | Abyss (0) | See its entry: 50 blocks, 10 wide, 5 seconds |
| Seeds | Abyss (0) | Last 12 seconds each instead of 6 |

## Passives

What a Beyonder of the Pathway always has from a Sequence on, with no key press and no spirituality cost. They go in `SequencePassives`.

| Passive | From Sequence |
|---|---|
| Criminal Body: Strength I, and a chime only you hear when a mob starts hunting you out of sight (keen instincts). Any held item counts as a weapon | Criminal (9) |
| Coldblooded: fear and nausea last half as long, two extra hearts. "They lose their conscience": no fear of the Seeds you plant | Unwinged Angel (8) |
| Interference: divination, study, prediction and connections on you return nothing (`Information.blocked`) | Serial Killer (7) |
| Devil's Skin: Resistance I, Speed I, poison does not take, fire and curse damage is halved. **Danger Premonition (Malice Perception):** a chime and a red marker, only for you, on every enemy within 32 blocks that has picked you as its prey or is about to hit you; blows from a marked source are 10% weaker | Devil (6) |
| Desire Harvest: every third melee hit on the same enemy plants a Seed | Desire Apostle (5) |
| Cunning: mind effects (fear, confusion, nausea, charm) last half as long on you; Danger Premonition reaches 48 blocks and warns two seconds earlier. Saturation I (every Pathway, from 4) | Demon (4) |
| Corrupting Voice: every Seed on an enemy rots it for 2 damage a second, credited to you | Blatherer (3) |
| Single Drop: a killing blow turns you into blood and you re-form 3 seconds later on 30% health, once every 5 minutes. Blood Sovereign: Bleeding and Corrosion do not take on you; below half health you mend 1 a second | Bloody Archduke (2) |
| King of Filth: enemies within 12 blocks cannot heal naturally and lose 1 a second. A purifying power (the Sun's) is exempt: leave a hook for White Angel | Filthy Monarch (1) |
| Malice Never Dies: every enemy that dies within 24 blocks heals you 5% of your maximum health. The first blow on you every 10 seconds makes whoever struck you afraid for 2 seconds | Abyss (0) |

## How the parts work together

- **Seeds, then Die.** Sulfur Flame, Fireballs and Corruption plant Seeds quickly; Die turns them into 200 damage and a kill on the weak.
- **Devil Form, then Sulfur Fireball.** The 12-ball barrage is the Pathway's bread and butter from Sequence 6. Press once to fill the screen, press again to repeat it.
- **Flame Cage, then Volcanic Eruption.** The cage holds it in the circle of the eruption.
- **Slow, then Sword of Lava or Killing Spree.** A stopped enemy cannot dodge a wide cut.
- **Offerings, then Devil Worship.** Kill a few mobs, then call a Devil with full strength.
- **Seeds, then Blather.** Wound the one that flees, then finish it with the voice from across the map.
- **Seed of Malice before a big fight** (it costs a lot and lasts 90 seconds): the revive is the Pathway's answer to a bad fight.
- **Abyssal Sea and The Abyss** are the Sequence 0 pair: one makes the field, the other makes you the weapon.

## Melee combo (optional)

The registered `MeleeCombo` could have six hits: Rusty Cut, Gut Slash, Backhand, Rising Cleave, Throat Rip and Devil's Smash, resetting after more than 40 ticks without a hit. From Devil (6) every hit throws a spark of pale blue flame at the target. Visual only, no extra damage.

## Starting numbers

Costs follow the Attendant Of Mysteries curve (Sequence 6 about 1500, 5 about 3000, 4 about 6000, 3 about 12000). Sequences 2, 1 and 0 continue it by guess; the pool from Sequence 2 is 78400 and refills at once. **The three first keys are set to costs that can be paid at their own unlock Sequence** (Sequence 9 holds a bar of 20, Sequence 8 holds 400, Sequence 7 holds 1980), as Thunder God had to do after its first draft: 10, 200, and 700 and 900.

## Built from the spell kit

What exists today, so most of the Pathway is a few lines each:

| Entry | Kit parts |
|---|---|
| Lawless Lunge | `Spells.aimEnemy`, `Spells.strike`, `Root`, `FallGuard`, the Iron Charge step of `Conqueror.java` (`ironChargeStep`) |
| Dirty Trick | `Spells.aimEnemy`, `Blind`, `Falter` |
| Sulfur Flame, Sword of Lava | `Spells.enemiesInCone`, `Spells.strike`, `Spells.keepEffect`, `SlashFx.arc` |
| Sink | `Spells.aimEnemy`, `Root` (JUMP), `Spells.holdLength`, `Spells.push` |
| Killing Spree | `Bleeding`, potion effects, `HudCounter`, `LivingDeathEvent` |
| Devil Worship | `Marionette.spawnSummon`, `Shot`, `Marks` |
| Sulfur Fireball | `Shot` fan (`Spells.turned`), `Spells.enemiesAround`, `Seeker` for the homing |
| Devil Form, Desire Incarnation, The Abyss | `Form`, `GiantForm` (the size), `Physique`, `Spells.addUnchoosableCheck` |
| Flame Cage | `Root`, `Wall` (stops projectiles), `Enclosure` |
| Volcanic Eruption, Hellfire | `SpellFx.warningCircle`, `Later`, `Spells.enemiesAround`, `Spells.enemiesInSight`, `Spells.push` |
| Toxic Vapor, Corrosive Fog, Acid Swamp, Tide of Filth, Corruption | `Zone` (`onInside` with ticks inside), `Corrosion`, `Spells.keepEffect`, `Spells.wave` + `SpellFx.steadyRing` |
| Flames of the Abyss, The Corroder | `Ray` (repeated), `Spells.enemiesOnLine`, `Corrosion`, `Dome.crossedBy`, `Dome.weaken`, `Dome.close`, `Wall.close` |
| Shadow-Shift | `Teleport`, `Spells.blinkSpot`, `Spells.enemiesOnLine`, `Later` |
| Desire Control, Confusion | `Taunt.brawl`, `Falter`, `Spells.enemiesAround` |
| Mental Shock | `Spells.enemiesInCone`, `Fear`, `Root` |
| Desire Symbiosis, Seed of Malice | `DamageLink.redirect`, `DeathWard`, `Teleport` |
| Filthy Illusion | `Seeker.launchAt` (the flock pattern of Fire Ravens), `Taunt` |
| Slow, Sloth, Blasphemy | `Root`, `Silence`, `Falter`, `Vulnerable`, `Spells.holdLength` |
| Die, Blather | `Marks.take`, `Bleeding`, `Spells.strikeThrough` |
| Blood Ignition | `Bleeding`, `NoHeal`, `Spells.isBoss` |
| Blood Burst | `Spells.enemiesAround`, `Corrosion`, `Spells.push`, potion effects |
| Abyssal Sea | `Dome.sphere`, `Zone.around`, `Fear`, `AbilityWheel.costBy` (the halved costs, as Chasm of Storms) |
| Depravity | `Marks` (the owner kind), `SpellFx.overHead` |
| Offerings | `Marks`, `HudCounter` |

## New reusable parts worth writing first

In this order, most abilities unlocked first. Each one is general, so other Pathways can use it too. Casts should be written against `LivingEntity` where they can (as `Apocalypse.java` does), so Beyonder mobs can cast them if you want that later.

| Part | What it is | Unlocks |
|---|---|---|
| `Depravity` (status/) | A thin wrapper over the owner kind of `Marks`: `plant(caster, target, n)`, `count`, `spend` (returns the number and clears), and `bonus(caster, target)` read by one line in the hurt event. Shows the sigils overhead, saved with the entity. Same shape as Prince's `Orders` and Thunder God's `Rage`. | Every Seed, Die, Blasphemy, Blast, Blather, Corrupting Voice |
| `AbyssFire` (combat/) | The one place that knows what the Abyss flame does, as `Shock` does for lightning: `AbyssFire.strike(caster, target, damage, burnTicks)` puts burn, poison and (from Sequence 1) Corrosion on it; fire-only defenses (Fire Resistance, water) stop only the burn share; damage credited with `DamageFx.credit`. | Sulfur Flame, Fireball, Sword of Lava, Hellfire, Flames of the Abyss, Cage, Eruption |
| `FoulTongue` (this Pathway) | "Say a word": picks the target (looked at; from Blatherer the marked one at twice the range through walls), refuses while Silenced, plays the sound. | The whole Foul Tongue wheel |
| `Offerings` (this Pathway) | Counter of kills in the last 60 seconds, 5 at most, with its `HudCounter`. | Killing Spree, Devil Worship |
| Volley (combat/) | `Shot` fan with a count, a spread and optional light homing: `Volley.fan(shot, player, sl, from, look, count, spreadDegrees)`. | Sulfur Fireball, the Demon Form barrage, projections' fireballs |
| Channel (combat/) | "Hold and pour": runs a tick function every N ticks for T seconds, keeps the caster still, ends on death, Silence or stun. | Flames of the Abyss, The Corroder, Abyssal Sea upkeep |
| Blood price (`Spells.payBlood`) | Takes a share of the caster's current health, never kills, refuses below a floor. `DarkAngel.ritualCircle` does this by hand today. | Blood Burst, any later blood cost |
| Linked ward (`DeathWard`, extended) | `DeathWard.arm` with a condition (a target that must live), so the ward is void when the linked enemy dies. | Desire Symbiosis, Seed of Malice |
| Filthy field (`Zone` flavor) | The "worse the longer they stay" rule: `Zone.onInside` with ticks inside raising `Corrosion` and armor wear. | Acid Swamp, Tide of Filth, Fogs, Corruption |
| Wheel rules | `AbilityWheel` with `.pathway(...)`, `.sequenceBy(...)`, `.quickCastCooldown(false)` and `.quickCastCost(1)`; every entry `.entry(...)` then `.description(...)`; sneak opens, normal `quickCast`s. | Every wheel |

## Registration (when the code is written)

For a new Pathway, the repo expects: `network/SKeyPressAbilityOneUsed` to `SevenUsed` branching on `"Filthy Monarch"`, `PathwayAbilities.ALL` (seven lines; keys 1 to 3 are stealable with a sneak form, the four wheels are `OWN_PATHWAY_ONLY`), `SequenceCommands.PATHWAYS`, `SequencePassives`, `PathwayHudIcon`, `MeleeCombos`, `DarkAngel.pathwayColor` (one case for the soul color) and a row for Filthy Monarch in `docs/SEQUENCES.md` in the "Required Sequence of every ability key" table: 1: 9, 2: 8, 3: 7, 4: 6, 5: 5, 6: 6, 7: 4. Suggested classes, like Thunder God's: `FilthyMonarch` (keys 1 to 3, events, shared), `FilthyMonarchDevil`, `FilthyMonarchDesire`, `FilthyMonarchTongue`, `FilthyMonarchBlood` (one per wheel), `FilthyMonarchFx` (only looks and sounds), `status/Depravity`.

## What needs looks, sounds and pictures

Modelled on `PrinceOfAbolitionFx`: one `FilthyMonarchFx` that only shows things. The shared ones: the pale blue sulfur flame (jet, ball, pillar, cage bars), the black liquid and sludge (shadows, Desire Incarnation, the Tide), the corrupted black fog, the Seed sigil, the Devil Form (horns, wings of blue flame, black skin) and its Demon Form version, the Devil's projection, the filth phantoms, the congealed blood spray, the corrosion beam, the kingdom shell, the Abyss form (a silhouette that changes), the Offerings sigil and the Pathway icon in `PathwayHudIcon`. Wheel icons: 29.

## Left out on purpose

- **Full Mythical Creature Forms** (Bloody Archduke: a body of congealed blood that liquefies and coalesces; Filthy Monarch: a nightmare of filth in a black abyss): they need models. Devil Form and Demon Form carry the idea until then. A `Form` with `GiantForm` fits them later.
- **Respond to prayers** (Blatherer) and **Danger Premonition one to two days early** (Demon): no gameplay; Danger Premonition is the passive.
- **Projection that does not rely on the Abyss** (Demon spell): folded into Filthy Illusion.
- **Summoning filth to help** (King of Filth): simplified to the hands of filth of the Tide.
- **Desire Symbiosis to a certain desire**: simplified to a link with an enemy.
- **Spare ideas, dropped so the wheels stay at eight entries or fewer:** a Poisonous Flame Manipulation that steers burning enemies, a Language of Foulness "Curse" word that stores damage, a Filth-Summon familiar. Any of them fits a wheel later.

## Open balance notes

- **Recasts have no cooldown, and from Sequence 2 spirituality refills at once** (`SEQUENCES.md`), so at Sequence 2, 1 and 0 only the animation limits how fast you can repeat an entry. The big ones keep a wind-up (warning rings, 1 to 4 second channels) for that reason. If it plays too fast, the fix is a recast price (`quickCastCost`, as Beauty Goddess does) rather than a cooldown.
- **Seeds add +4% each and many entries plant them.** If everything is at 5 Seeds all the time, lower the bonus to +3% or the wear-off to 4 seconds.
- **Devil Form plus the barrage** is 12 balls at 16 damage from Sequence 6 for 60 seconds. If that outdamages everything of its Sequence, drop the barrage to 8 balls.
- **Seed of Malice** is a revive that also hurts. It needs a living, hunted target, lasts 90 seconds and is one at a time. Say if it is too strong.
- **Pale blue fire next to Conqueror's red fire** is a visual split only; Sulfur Fireball and Conqueror's fireball do similar things.
- **Sequence 3 is thin** (Blather and a passive). Add an entry if it feels empty.

## Questions for Luka

1. **Wheel convention.** I used Prince's (sneak opens, normal recasts the last pick). Want Apocalypse's (normal opens, sneak recasts) instead?
2. **Cooldowns on wheel picks.** I read "no cooldown when recasting the same ability" as: picking from the open wheel has a short cooldown, repeating the same entry has none. Do you want that, or none at all? ("None at all" is one line in code: `.noCooldownsWhen(p -> true)`, as Thunder God and Author do.)
3. **Recast price.** A recast costs the same as the first cast. At Sequence 2 and above that makes spamming limited only by animation. Keep, or make a recast cost more (for example double)?
4. **Offerings.** Devil Worship is full strength only after recent kills (otherwise half, 10 seconds). Keep, or always full strength?
5. **Seed of Malice.** The revive that takes half the enemy's health with it: keep, tone down, or drop?
6. **Depravity.** Seeds on enemies are the only Pathway mechanic. Fine, or do you want a resource on yourself too (like Rage)?
