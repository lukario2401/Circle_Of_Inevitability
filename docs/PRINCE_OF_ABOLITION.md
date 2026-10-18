# Prince Of Abolition: ability design

Built · design and code for the Circle Of Inevitability repo · drafted by Claude, October 18, 2026 · revised twice, first half of the code October 18, 2026, second half the same day

The Prince Of Abolition Pathway is the Black Emperor Pathway of Lord of the Mysteries, the Emperor tarot card: Lawyer (9), Barbarian (8), Briber (7), Baron of Corruption (6), Mentor of Disorder (5), Earl of the Fallen (4), Frenzied Mage (3), Duke of Entropy (2), Prince of Abolition (1), Black Emperor (0). Source: the Black Emperor Pathway and Black Emperor Pathway/Abilities pages of the Lord of the Mysteries wiki. The wiki lists no abilities for the Frenzied Mage (3) and only one or two for Sequences 2, 1 and 0, so those are designed from the Pathway's themes.

**Status: built.** Every Sequence from Lawyer (9) to Black Emperor (0) is written, with its looks, sounds and pictures (see "What is built"). The rest of this file is the design the code follows; "Where the code differs from the design" lists the few places it does not, yet.

Pathway name in code: `"Prince Of Abolition"`.

## What changed in this revision

- Still seven keys. The extra abilities live in the wheels (keys 6 and 7); no new keys.
- Mausoleums revive only you. No raising the dead. Resurrection is replaced by **Coronation**.
- Mausoleums expire after 2 minutes.
- Sentence was cut to keep the wheels playable. The new direct hits are Coin Shot, Ruin, Mausoleum Salvo and Decree.
- Earlier changes stay: Sequence Might (+5% per Sequence), mausoleum bonus (+5% attack and defense each), flipped wheels (sneak opens, normal recasts), recast cost 3x, 2x, 1x and no cooldowns from Sequence 4, the `Order 4/9` display, and Seal as an illusory door.

## Theme

A rule-bending Pathway. It does not break the order, it finds the loophole in it. Lawyers make law and words into weapons, Barbarians settle by force what the law cannot, Bribers pay for effects. From Sequence 6 the Beyonder Distorts words, actions, intent and the paths of things in flight, Disorders perception, the order of actions and distance, Exploits and Magnifies the rules, lets Entropy loose, and at Sequence 1 Abolishes people, places and itself out of the real world. The Black Emperor turns everything into its opposite and does not stay dead while "their" order stands. Majesty runs through all of it: others want to kneel.

Colors: jet black (the Pathway's official color), imperial gold for orders, seals, mausoleums and the crown, ash grey for Disorder and Entropy, the colorless fog of a parallel world for Abolition. Pathway color: `0x16121F`; accent: `0xE0B04A`.

Nothing here breaks blocks or places real ones. Seals, doors, shells, mausoleums and the Abolition pocket are statuses with visuals, and the ground is never changed.

## Sequence Might

Every Sequence you reach adds **+5% attack, +5% defense, +5% range, +5% AOE and +5% duration**, counted from Sequence 9. Lawyer (9) is +5%, Barbarian (8) +10%, and so on up to **+50% at Black Emperor (0)**.

- Attack: damage you deal.
- Defense: damage you take is divided by (1 + total defense bonus), so +50% defense means about a third less damage, not half.
- Range: aim and reach distances (the numbers below are the base values).
- AOE: radius, width and height of everything you make (rings, fields, walls, shells).
- Duration: everything you put on others and everything you leave standing (marks, Bribes, Seals, fields, pockets, mausoleums). Cooldowns and costs are not changed.

These are stat multipliers read by the spell kit, not status effects: they show no icon and cannot be stripped, cleansed or Exploited away. All numbers in this file are the base values before Sequence Might and mausoleum bonuses.

## Order (this Pathway's own mechanic)

Attendant Of Mysteries has stacks; this Pathway has Order. Everything you leave standing in the world is one of your standing orders: a Compromised mark, a Bribe, a Connection, a Seal, an Entropy field, an Abolition pocket, a mausoleum. Nine at most, like the Black Emperor's nine mausoleums.

It is shown next to the hotbar as one small black-and-gold sigil, then the word **Order**, then the count and the cap: `[sigil] Order 4/9` (drawn with ScreenImages like the other status pictures). It does three things: each standing order cuts the damage you take by 2%, Majesty lasts a second longer for every 2 orders, and Last Rebirth only works while enough orders stand. A mausoleum is also one order, so it counts here too, on top of its own bonus below.

## Mausoleums

The Black Emperor's nine mausoleums are the heart of the Pathway. A mausoleum is a black stele 3 blocks tall, trimmed in gold, with a gold sigil floating above it. It is a status with a visual, not a block. **Mausoleums belong to you alone: they never help, heal or revive anyone else.**

- **Raise Mausoleum** (Sequence 8, Fallen wheel): raise one on the ground where you look (up to 12 blocks). Nine at most; raising a tenth lets the oldest crumble.
- **It lasts 2 minutes.** Then it crumbles and its Order is gone. Sequence Might's +5% duration per Sequence and Their Order (+25%, Black Emperor) lengthen it: at Black Emperor a mausoleum stands 120 s x 1.5 x 1.25 = 225 s. Exploit doubles the remaining time of every standing mausoleum, up to 2 extra minutes.
- **It can be destroyed.** 80 health, regenerates 2 per second when nobody touched it for 5 seconds; enemies may strike it.
- **Power:** each standing mausoleum gives you **+5% attack and +5% defense**, up to +45% each at nine. This is a stat bonus, not an effect: no icon, no cleansing. It stacks with Sequence Might and adds on top of Law & Order.
- **Rebirth (revival, only you):** while at least one mausoleum stands, a killing blow does not end you. You slip out of the real world as in Exile for 10 seconds, then rise from the nearest mausoleum at half health with a Majesty pulse around it. That mausoleum is destroyed.
- **Sanctuary** (from Sequence 7): within 8 blocks of a mausoleum, you and your allies recover health (half a heart per second) and you regain spirituality 50% faster; enemies in it are slowed by 15%.
- **Step** (from Sequence 6): travel from where you stand to a mausoleum. See the Fallen wheel.
- **Salvo** (from Sequence 4): they fire at your target. See the Fallen wheel.
- **Coronation** (Sequence 0): they crown you. See the Sovereign wheel.

## How it is laid out

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M), every ability has a normal cast and a sneak cast. Three keys (3, 6, 7) are wheels like Apocalypse's, where every entry has its own Sequence, cost and cooldown.

**Wheels work this way: sneak opens the wheel, a normal cast recasts the last pick** (AbilityWheel.quickCast, flipped).

Recasting the last pick never has a cooldown, and its cost falls as you advance:

| Your Sequence | Recast cost | Cooldowns |
|---|---|---|
| 9 to 7 | 3x | recast has none; picked from wheel as normal |
| 6 | 2x | same |
| 5 | 1x | same |
| 4 and up | 1x | **gone entirely, on every ability** |

From Sequence 4 on, only spirituality limits you, so the Sequence 3, 2, 1 and 0 entries are balanced by cost alone (their cooldown column shows a dash).

The abilities do not each get a Sequence of their own. Sequences 3 and 2 have one each but also strengthen what is already there (see "Grows with the Sequence"). Sequences 2, 1 and 0 are not left without abilities.

| Sequence | Abilities |
|---|---|
| Lawyer (9) | Verdict, Brainwash |
| Barbarian (8) | Brute Force, Iron Will, Raise Mausoleum |
| Briber (7) | Weaken, Charm, Arrogance, Connect, Coin Shot |
| Baron of Corruption (6) | Distort, Seal, Corrosion, Ruin, Step |
| Mentor of Disorder (5) | Disorder, Paces, Majesty |
| Earl of the Fallen (4) | Bestowment, Exploit, Magnify, Mausoleum Salvo, Mausoleum Blast |
| Frenzied Mage (3) | Frenzy |
| Duke of Entropy (2) | Entropy |
| Prince of Abolition (1) | Abolition, Exile |
| Black Emperor (0) | Inversion, Coronation, Decree |

## Keys

| Key | Ability | Sequence | Normal cast | Sneak cast |
|---|---|---|---|---|
| 1 | Verdict | Lawyer (9) | A black gavel falls from above on the enemy you look at, up to 20 blocks away, and cannot be dodged: 40 damage and a short stagger. It finds the loophole in the enemy: it is Compromised for 10 seconds, takes 25% more damage from you, and your Bribes and Disorder last 50% longer on it. | **Brainwash**: a few quiet words for the enemy you look at (up to 16 blocks). A mob forgets whom it was hunting and cannot pick you out for 8 seconds; a player trusts you for 5 seconds: it cannot lock aimed abilities onto you and its blows on you are 30% weaker. Bosses (more than 150 maximum health) shrug it off. |
| 2 | Brute Force | Barbarian (8) | Where the law fails, force rules. One blow with your whole body on the enemy in front of you (reach 4): 60 damage, thrown back 5 blocks, shield and block stance broken for 5 seconds. Enemies within 2 blocks of it take half and are knocked aside. | **Iron Will**: Mental Resistance. For 8 seconds every mind effect (Brainwash, Charm, Arrogance, Bestowment, Corrosion, Majesty, taunts) ends on you at once and cannot be put on you, and you cannot be staggered. |
| 3 | Bribe wheel | Briber (7) | Recast the last Bribe pick, no wheel. | Open the Bribe wheel: Weaken, Charm, Arrogance, Connect, Coin Shot. |
| 4 | Distort | Baron of Corruption (6) | Twist what you look at, up to 24 blocks. A projectile or spell in flight (arrow, fireball, Shot) becomes yours and flies back at whoever sent it. An enemy has its intent Distorted for 5 seconds: its next 3 attacks, melee, ranged or spell, land on its nearest ally instead, or miss if it has none. | **Seal**: you summon an illusory door and close it, and the space is Sealed. A door of black fog with gold trim rises in front of you, swings open for a heartbeat, and you slam it shut. A room of 6 blocks radius around you holds for 8 seconds. Nothing crosses its shell but you and your allies: enemies cannot come in or go out, projectiles and spells stop at it, and enemies already inside are held in. |
| 5 | Disorder (a plain press is Paces, sneak is Disorder: swapped in play) | Mentor of Disorder (5) | Bring chaos into the enemy's order. The enemy you look at (up to 20 blocks) has its perception and the order of its actions scrambled for 6 seconds: 40% of its attacks and spells miss, its selected hotbar slot jumps about, and a mob loses its target. If you are held (rooted, strung up, counted down on), the cast frees you instead: every hold, thread and countdown on you ends and none takes for 3 seconds. | **Paces**: Disorder the distance. You cross up to 24 blocks in a few paces, to where you aim, and a fake version of you stays where you stood for 4 seconds. Enemies go for it, and when it is hit it bursts and Disorders everything within 3 blocks for 2 seconds. |
| 6 | Fallen wheel | Barbarian (8) | Recast the last Fallen pick, no wheel. | Open the Fallen wheel: Raise Mausoleum (8), Corrosion (6), Step (6), Majesty (5), Bestowment (4), Exploit (4), Magnify (4), Mausoleum Salvo (4), Mausoleum Blast (4). |
| 7 | Sovereign wheel | Baron of Corruption (6) | Recast the last pick, no wheel. | Open the Sovereign wheel: Ruin (6), Frenzy (3), Entropy (2), Abolition (1), Exile (1), Inversion (0), Coronation (0), Decree (0). |

### Key 3 wheel: Bribe

A Bribe is actual or symbolic. With nothing in your off hand it is symbolic. Holding a gold ingot or an emerald spends one and makes it actual: 50% longer, and it spreads to every enemy within 3 blocks of the target.

| Entry | Sequence | What it does |
|---|---|---|
| Weaken | Briber (7) | A coin, a gift, or something that only looks like one is pressed on the enemy you look at (up to 16 blocks). For 12 seconds its attacks are 35% weaker, it takes 25% more damage from you, and the holds it puts on you (roots, threads, taunts) last half as long. |
| Charm | Briber (7) | The enemy warms to you. For 10 seconds it cannot bring itself to fight you: a mob stops targeting you and wanders off, a player deals 50% less damage to you. Every second there is a 5% chance it turns on its nearest ally instead, and a mob that does is your marionette for 5 seconds. |
| Arrogance | Briber (7) | The enemy swells with pride and underestimates you for 12 seconds: 30% of its attacks miss, its ranged attacks scatter, and a mob never retreats or uses healing. |
| Connect | Briber (7) | A thin black thread ties you to the enemy you look at (up to 32 blocks) for 30 seconds. While it holds you see the enemy through walls, your aimed abilities reach it at up to 3 times their range, and your other Bribes on it cost half. |
| **Coin Shot** (direct hit) | Briber (7) | You flick a coin and it flies like a bullet along a 24 block line: 30 damage to the first enemy it meets, then it ricochets once to the nearest enemy within 6 blocks for half. It cannot be dodged by strafing (hitscan). A held gold ingot or emerald makes it actual: +50% damage and the first enemy is also Weakened for 6 seconds. |

### Key 6 wheel: The Fallen (and the mausoleums)

| Entry | Sequence | What it does |
|---|---|---|
| **Raise Mausoleum** | Barbarian (8) | Raise a mausoleum where you look (up to 12 blocks). It lasts 2 minutes, gives +5% attack and defense, and revives you once (see "Mausoleums"). |
| Corrosion | Baron of Corruption (6) | Everyone within 10 blocks of you turns dark and greedy. For 10 seconds enemies lose their discipline: a mob picks a random target nearby 40% of the time (mostly its own friends) and 20% of all enemy attacks go wide (a mind effect). And a rot sets in that does not wear off: 2 damage a second, 1 more every second, until it is cleansed (any cleanse ability, or a milk bucket) or they die; after 5 seconds of it they cannot heal at all (status/Corrosion). Your allies are not touched. |
| **Step** | Baron of Corruption (6) | You step through a mausoleum: look at one (up to 40 blocks) and you vanish into a gold door and come out of it at once. With none in view, the nearest one. From Sequence 4 you bring allies within 3 blocks with you. |
| Majesty | Mentor of Disorder (5) | Your presence presses down on everyone within 10 blocks. Enemies kneel for 1.5 seconds (Slowness IV, no jumping, sprinting or attacking), plus a second for every 2 standing orders; mobs of up to 60 maximum health stay down for 6 seconds. |
| Bestowment | Earl of the Fallen (4) | Bestow a feeling on the enemy you look at (up to 16 blocks) for 10 seconds. The feeling is picked from the target: Sluggish (slow, slow attacks) for fast things, Anxious (aim shakes, cannot sprint) for archers and casters, Defeated (damage cut by 60%, mobs drop their target) for anything under 40% health, Rash (attacks 40% faster, takes 40% more damage, cannot stop moving forward) for slow brutes, Avaricious (ignores everything to chase the nearest dropped gold, emerald or diamond) for the rest. |
| Exploit | Earl of the Fallen (4) | Exploit a state; which one depends on what you do it to. In the air: the state of being off the ground is stretched and you hang and glide for 8 seconds (hold jump to rise, which is flight). Looking at an enemy (up to 24 blocks): its states end ahead of time, so good effects, shields and charges are stripped and any countdown it runs is cut short. Otherwise: everything running on you (buffs, concealment, your own seals and fields) has its remaining time doubled, up to 30 seconds; standing mausoleums have theirs doubled by up to 2 minutes. |
| Magnify | Earl of the Fallen (4) | The next blow or ability within 10 seconds is magnified: 2.5 times the damage, 1.5 times the range and area, and a hit that would leave a non-boss under 25% health kills it on the spot. A magnified Bribe travels as a thrown flower and lands as a quake: everything within 6 blocks is shaken and gets the Bribe. Cast with a Connected enemy in view (up to 100 blocks), Magnify seizes it at once: it is pulled 10 blocks toward you and held for 2 seconds. |
| **Mausoleum Salvo** (direct hit) | Earl of the Fallen (4) | Every standing mausoleum fires a gold beam at the enemy you look at (up to 40 blocks from you): 35 damage each, 0.15 seconds apart. If other enemies stand within 8 blocks of the target, the beams share out between them. With no mausoleum it does nothing and costs nothing. |
| **Mausoleum Blast** | Earl of the Fallen (4) | Every mausoleum within 24 blocks of you goes off: its seams glow gold for a moment, then it bursts, 90 damage to every enemy within 5 blocks of it, thrown away from it. An enemy between two takes both. The mausoleums are gone, orders and all, so Rebirth has less to rise from. With none near it does nothing and costs nothing. |

### Key 7 wheel: Sovereign

| Entry | Sequence | What it does |
|---|---|---|
| **Ruin** (direct hit) | Baron of Corruption (6) | A black-and-gold beam leaves your hand along a line 24 blocks long: 70 damage to every enemy on it, hitscan. Whatever it hits starts to rot (status/Corrosion): 5 damage a second, 1.5 more every second, until it is cleansed or dies, and after 5 seconds of it no healing at all. A second Ruin on a rotting enemy bites 5 deeper. |
| Frenzy | Frenzied Mage (3) | You throw your arms open and a storm of black bolts bursts out: 12 bolts, each Distorted to curve onto a different enemy within 24 blocks (the same ones again if there are fewer). Each does 25 damage and Disorders what it hits for 2 seconds. With no enemies they fly out in a ring and fade. |
| Entropy | Duke of Entropy (2) | Order comes undone around the spot you aim at (up to 24 blocks): a field of 16 blocks radius for 20 seconds that grows stronger the longer it stands (full after 8 seconds). Enemies in it: projectiles and spells fly off course, movement stumbles (Slowness I and random shoves), and the damage they deal swings between half and one and a half times what it should be. You and your allies are untouched and physics bends for you: Exploit and Magnify cost nothing inside it. |
| Abolition | Prince of Abolition (1) | You abolish the area around you: a sphere of 12 blocks radius slips out of the real world for 15 seconds, a grey fog without sound. Nothing outside reaches in and nothing inside reaches out: abilities, projectiles, summons, lightning and explosions stop at the shell, and nobody outside can see or target what is inside. Everyone inside can still fight each other. Cast it again to close the pocket early. |
| Exile | Prince of Abolition (1) | You abolish one thing. Aimed at an entity (up to 24 blocks) it is thrown out of the real world for 6 seconds: it cannot be seen, hurt, targeted or act, and returns where it left (bosses: 2 seconds). Aimed at an ally it saves them from whatever was coming. Aimed at nothing you abolish yourself for 4 seconds and return where you aim, up to 20 blocks away. |
| Inversion | Black Emperor (0) | Everything within 20 blocks of you is turned into its opposite for 10 seconds. Damage enemies deal to you and your allies heals you instead; healing they receive hurts them; good effects on them become their opposites (Strength to Weakness, Speed to Slowness, Regeneration to Poison). Light turns to shadow: enemies are in Darkness. |
| **Coronation** | Black Emperor (0) | The mausoleums crown you. For 20 seconds your mausoleum bonus doubles (+10% attack and defense each), a gold Majesty pulse rolls out of you every 5 seconds, and you cannot be knocked back or pulled. It needs at least one standing mausoleum and does not use any up. |
| **Decree** (direct hit) | Black Emperor (0) | You speak one word and the enemy you look at (up to 40 blocks) takes 300 damage that nothing stops: shields, blocks and Protection do not hold it. A non-boss left under 40% health dies. Each standing mausoleum adds 10% damage. |

**Last Rebirth** (passive, Black Emperor): with no mausoleum left but at least 3 other orders standing, a killing blow does not end you: you slip out of the real world for 10 seconds and rise where you fell at 25% health, once every 10 minutes.

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on. (Sequence Might, +5% per Sequence, is on top of all of them.)

| Ability | From | What changes |
|---|---|---|
| Seal becomes a gate | Earl of the Fallen (4) | Distortion changes in kind: the door no longer needs to stand beside you. You summon the illusory door anywhere you look, up to 24 blocks, and it slams shut at once. A wall 10 blocks wide and 5 tall in thin air for 10 seconds, instead of a room around you. |
| Bribes | Earl of the Fallen (4) | Every Bribe, actual or not, also reaches enemies within 3 blocks of the target, and Charm's chance to turn on allies doubles. |
| Disorder | Earl of the Fallen (4) | It reaches the structure of things and the standard of measure: Paces carries you 48 blocks, and for 2 seconds after Disorder or Paces everything aimed at you misses. |
| Step | Earl of the Fallen (4) | You can bring allies within 3 blocks with you. |
| Distort | Duke of Entropy (2) | It distorts rules, not only acts: aimed at an enemy shell (a dome, a Seal, a Protection, a Kingdom) it flips it, and what it kept out now keeps its owner in. |
| Exploit | Duke of Entropy (2) | Inside an Entropy field a jump becomes a leap to the Moon: thrown up to 60 blocks in an arc, with no fall damage until you land. |
| Abolition | Black Emperor (0) | The shell also keeps other fields and domains from covering what is inside. |

## Passives

| Passive | From Sequence |
|---|---|
| Law & Order: each standing order cuts the damage you take by 2% | Lawyer (9) |
| Silver Tongue: villagers' prices are lower (Hero of the Village I, hidden) and a grudge held by a neutral mob ends after 10 seconds | Lawyer (9) |
| Physical Enhancement and Mental Resistance: Strength I, two extra hearts, and mind effects last half as long on you | Barbarian (8) |
| Everyone Has A Price: piglins never turn on you | Briber (7) |
| Weakness Detection: the enemy you look at shows its health and every effect on it, and the first hit on an enemy that has not noticed you is a crit | Baron of Corruption (6) |
| Presence: weak hostile mobs (up to 30 maximum health) within 6 blocks hesitate, slowed by 25% and half as likely to pick you | Mentor of Disorder (5) |
| Favorable Ground: hidden or invisible enemies within 16 blocks glow for you and cannot stay hidden near you | Earl of the Fallen (4) |
| Loose Order: projectiles and spells aimed at you drift up to 10 degrees off | Duke of Entropy (2) |
| Beyond the World: damage with no attacker (lightning, explosions, magic, fire) is 50% less | Prince of Abolition (1) |
| Their Order: everything you leave standing lasts 25% longer, mausoleums included | Black Emperor (0) |
| Last Rebirth: see the Sovereign wheel | Black Emperor (0) |

## How the parts work together

- Verdict first: a Compromised enemy takes 25% more, and Bribes and Disorder last longer on it. Verdict, Weaken, then Magnify is the basic kill.
- Connect, then Magnify, seizes an enemy from up to 100 blocks; Seal then closes the room around the two of you. Exile or Abolition cut the rest of the fight away.
- Exploit stretches a standing Seal, Entropy field, Abolition pocket and every mausoleum; everything that stays standing adds to Order, which feeds Majesty.
- Raise mausoleums early and the Pathway pays you back at every step: +5% attack and defense each, Sanctuary to rest in, Step to retreat through, Salvo to fire from, and Rebirth as a safety net for you alone. They only last 2 minutes, so a fight is about keeping them up.
- Raise, Coronation, then Decree: a full ring of mausoleums doubled by the crown, then one word ends the enemy.
- Seal around a mausoleum cluster: the room is yours, the aura heals you and your allies, and Salvo fires from inside the shell.

## Melee combo

Same pattern as Attendant Of Mysteries (MeleeCombo.forPathway, registered in combat/MeleeCombos). Colors: gold head 0xF2E0B04A, dark violet middle 0xEE2A1F3D, ash grey tail 0xA05A5A66. Hit 1: a ring sweeping around you, gold edge on black. Hit 2: a tilted ring the other way. Hit 3, random: a gold rune circle stamped on the ground under the target, or two crossed black crescents on the target.

## Starting numbers

Costs follow the Attendant Of Mysteries curve (250 at Sequence 9 up to 12000 at Sequence 3). Sequences 2, 1 and 0 continue it by guess, so tune them against the spirituality pool there. Single keys have a cost only; wheel entries also have a cooldown in seconds, which stops applying at Sequence 4 (so entries from Sequence 3 on show a dash). Everything else is in the descriptions above.

| Ability | Cost | Cooldown |
|---|---|---|
| Verdict, Brainwash | 250 | |
| Brute Force, Iron Will | 600 | |
| Weaken, Charm, Arrogance, Connect | 1250, 1500, 1250, 800 | 8, 14, 12, 6 |
| Coin Shot | 1000 | 3 |
| Distort, Seal | 2450 | |
| Disorder, Paces | 3000 | |
| Raise Mausoleum (+100 per mausoleum already standing) | 700 | 5 |
| Corrosion, Step, Majesty, Bestowment, Exploit, Magnify, Mausoleum Salvo, Mausoleum Blast | 2450, 800, 3000, 6000, 4000, 5000, 4000, 5000 | 20, 4, 18, 12, 15, 20, 8, 10 |
| Ruin | 2800 | 10 |
| Frenzy, Entropy, Abolition, Exile, Inversion, Coronation, Decree | 12000, 20000, 30000, 15000, 45000, 30000, 25000 | — |

Recast cost: 3x (Sequence 9 to 7), 2x (Sequence 6), 1x (Sequence 5 and up), always with no cooldown.

## Built from the spell kit

What exists today, taken from Attendant Of Mysteries:

- Spells (aimEnemy, aimGround, enemiesAround, strike, push, pull, cleanse, fits, freeSpotNear): Verdict, Brute Force, Brainwash, Iron Will, the Bribes, Coin Shot, Ruin, Paces, Majesty, Corrosion, Frenzy, Exile, Decree.
- ModelFx, ParticleShapes, SlashFx: the gavel, the door, the fake self (like the paper figurine decoy), the thrown flower, the black bolts, the mausoleum, the crown, the melee combo.
- SpellFx (shockRing, closingRing, runeCircle, tether): Connect's thread, the Seal and Entropy circles, the Salvo beams, every burst.
- Root and RootRestriction: the stagger, being held, Majesty's kneel, Exile (Root.has(TELEPORT) also stops Paces and Step).
- Concealment (hiding in Exile, Step and Abolition), Marionette (Charm's turned mobs), DeathWard (Rebirth), FallGuard (Exploit's flight and leap), Countdown (Disorder breaking one), Later (the decoy's burst, the 10-second rise, the 2-minute mausoleum expiry), ScreenImages (the Order sigil and number).

Brute Force and Paces need nothing new.

## New reusable parts worth writing first

In this order, most abilities unlocked first. Each one is general, so other Pathways can use it too.

| Part | What it is | Unlocks |
|---|---|---|
| Might (status/) | One place that returns a player's attack, defense, range, AOE and duration multipliers: 5% per Sequence plus 5% per mausoleum for attack and defense. Everything reads it; nothing is a potion effect. | Sequence Might, Mausoleums, every ability |
| Mark (status/) | One small status on an entity: kind, who put it there, when it ends. Compromised, the four Bribes, Connection, Corroded and Bestowment's five feelings are all kinds of it. | Verdict, Bribes, Corrosion, Ruin, Bestowment |
| Mind (status/) | One check for every mind effect: Mind.resists(entity, kind) covers Iron Will, Mental Resistance, bosses (150 maximum health) and the short immunities, so each ability asks once. | Brainwash, Charm, Arrogance, Corrosion, Bestowment, Majesty, Iron Will |
| Mausoleum (status/) | A standing stele: owner, position, health, end time. Raise, Step, Salvo, Sanctuary, Rebirth and Coronation all read this one registry, and it is the part that expires after 2 minutes. | Raise Mausoleum, Step, Salvo, Coronation, Sanctuary, Rebirth |
| Field (status/) | A region (sphere or wall) with a caster, an end time, optional shell rules (who passes, what stops, which way round) and an optional effect every tick that fades in. Same part as Hand Of God's Protection and Divine Kingdom domes if those are built first. | Seal, Entropy, Abolition, Inversion |
| Two hooks in Spells (combat/) | aimEnemy, enemiesAround and strike ask Field.separated(a, b), so any ability of any Pathway respects Seal and Abolition with no code of its own. strike is also the one place that reads Might, Compromised, Magnify, Entropy's swing, Inversion and Law & Order. | Seal, Abolition, Exile, Verdict, Magnify, Entropy, Inversion |
| Shot additions (combat/) | redirect(shot, direction, newOwner), curve(toward) for homing arcs, and scatter(degrees). | Distort, Frenzy, Entropy, the magnified Bribe |
| Beam (combat/) | A hitscan line: origin, direction, length, width, damage, who it hits and what it pierces. | Coin Shot, Ruin, Salvo, Decree |
| Orders (status/) | Orders.count(player), a registry that Mark, Field and Mausoleum add to; the sigil and number come from ScreenImages. | Law & Order, Majesty, Last Rebirth |
| DeathWard return point | DeathWard.arm can send the saved player to a position after a delay, not only revive them on the spot. | Rebirth, Last Rebirth |
| extend and end on every status | Each timed status can be stretched or cut short from one call. | Exploit, Their Order, mausoleum expiry |
| Recast rules (AbilityWheel) | Flip the wheel (sneak opens, normal recasts) and read cost and cooldown from the player's Sequence: 3x, 2x, 1x, no cooldown. | Every wheel |

## Left out on purpose

- Mythical Creature Form, a curly-haired baboon: the wiki gives only the name and it needs a model. It stays out for now.
- Collapsing buildings with Disorder and magnifying wind into hurricanes: both change the world, which nothing here does.
- Raising the dead with mausoleums: mausoleums are yours alone.
- Sentence (four-gavel direct hit): cut so the wheels stay small (the Fallen wheel has 9 entries since Mausoleum Blast).

## Open balance notes

- Cooldowns vanish at Sequence 4, so from Sequence 3 on only spirituality limits the Sovereign wheel entries. Their costs are the thing to tune.
- At Black Emperor with nine mausoleums up, attack is up about 95% and defense about 95% (defense divided by 1.95) before Law & Order, and Coronation doubles the mausoleum part. Mausoleums last only 2 minutes, which is the main limit; cut Sequence Might or the bonus if it is too strong in play.
- Salvo and Decree scale with the mausoleum count, so they reward standing still near stelae. Enemies destroying a mausoleum (80 health) or the 2-minute expiry are the counters.

## Looks, sounds and pictures (ready)

Everything the abilities need to look and sound right is in `PrinceOfAbolitionFx.java` (`beyonder_characteristics/prince_of_abolition/`). It only shows things; the ability does the damage, holding, sealing, hiding and reviving, at the ticks its constants name. `/abolitionfx all` plays every display in game, `/abolitionfx <name>` one of them.

| Ability | Normal cast | Sneak cast |
| --- | --- | --- |
| 1 Verdict | `verdict(sl, player, target)` (strikes at `VERDICT_LANDS`), then `compromised(sl, target, ticks, tag)`, `compromisedEnd` | Brainwash: `brainwash(sl, player, target, ticks, tag)` (lands at `WORDS_LAND`), `brainwashEnd` |
| 2 Brute Force | `bruteForce(sl, player, target)` (lands at `BRUTE_LANDS`) | Iron Will: `ironWill`, `mindShrug` each time a mind effect is shaken off, `ironWillEnd` |
| 3 Bribe (wheel) | `bribe(sl, player, target, Bribe.X, actual)` (lands at `BRIBE_LANDS`), `bribed(sl, target, Bribe.X, ticks, tag)`, `bribedEnd`, `bribeSpread` for an actual Bribe, `charmTurn(sl, turned, ally)`, `connect` + `connectEnd`. Coin Shot: `coinShot(sl, player, hitAt, actual)`, `ricochet(sl, from, to)`. Icons `WHEEL_WEAKEN`, `WHEEL_CHARM`, `WHEEL_ARROGANCE`, `WHEEL_CONNECT`, `WHEEL_COIN_SHOT` | the last pick again |
| 4 Distort | `distortShot(sl, player, at, backToward)` on a projectile; `distortIntent(sl, player, target, ticks, tag)`, `distortedStrike(sl, attacker, victim)`, `distortIntentEnd`. Duke of Entropy: `shellFlip(sl, center, radius)` | Seal: `seal(sl, player, center, radius, ticks, tag)` (the door slams at `SEAL_SLAMS`), `sealEnd`. Earl of the Fallen: `gate(sl, player, middle, yaw, ticks, tag)` (slams at `GATE_SLAMS`) |
| 5 Disorder | `disorder(sl, player, target, ticks, tag)`, `disorderEnd`; `unbound(sl, player)` when it frees the Beyonder | Paces: `paces(sl, player, from, to, ticks, tag)`, `decoyBurst(sl, at, tag)`; Earl of the Fallen: `untouchable(sl, player)` |
| 6 The Fallen (wheel) | Raise Mausoleum: `raiseMausoleum(sl, ground, yaw, ticks, tag)` (standing at `MAUSOLEUM_RISEN`), `mausoleumHit`, `mausoleumCrumble(sl, ground, yaw, tag)`; Sanctuary: `sanctuaryPulse`, `sanctuaryHum`, `sanctuaryMend`. `corrosion` + `corroded`. Step: `step(sl, player, from, to)` (out at `STEP_OUT`). `majesty` + `kneel`. `bestowment(sl, player, target, Feeling.X, ticks, tag)` (lands at `BESTOW_LANDS`). Exploit: `exploitGlide` + `exploitGlideEnd`, `exploitStrip`, `exploitExtend`, `moonLeap` (Duke of Entropy). Magnify: `magnify` + `magnifyEnd`, `magnifiedHit`, `magnifiedFlower` (returns its flight ticks) then `quake`, `seize`. Salvo: `salvo(sl, tops, target)` (beam i at `SALVO_STEP * i`), `salvoHit`. Icons `WHEEL_RAISE_MAUSOLEUM` ... `WHEEL_MAUSOLEUM_SALVO` | the last pick again |
| 7 Sovereign (wheel) | `ruin(sl, player, to)` (fires at `RUIN_FIRES`), `ruinHit`. `frenzy(sl, player, targets)` (bolt i at `FRENZY_FIRST + i * FRENZY_STEP`), `frenzyHit`. `entropy` + `entropyPulse(sl, center, radius, strength)` + `entropyEnd`. `abolition` (closed at `ABOLITION_CLOSED`) + `abolitionEnd`. `exile(sl, who)`, `exileReturn`. `inversion` + `inversionPulse` + `inverted`. `coronation` (crowned at `CROWNED`) + `coronationPulse` every 5 seconds + `coronationEnd`. `decree(sl, player, target, mausoleums)` (falls at `DECREE_LANDS`). Icons `WHEEL_RUIN` ... `WHEEL_DECREE` | the last pick again |
| Death | `rebirthFall(sl, player)`, then `rebirthRise(sl, player, ground)` at the mausoleum (and `mausoleumCrumble` on it); `lastRebirth(sl, player)` | |
| Passives | `weaknessDetection(viewer, target)` (only the viewer sees it), `presence`, `looseOrder`, `beyondTheWorld` | |
| Melee | `combo(sl, player, target, step)` for hits 0, 1 and 2 | |

**Order** is shown as `[sigil] Order 4/9` to the left of the hotbar: `PrinceOfAbolitionFx.order(player, count)` whenever the count changes, `orderRaised(sl, player)` for a new standing order (a small crown and a chime only the Beyonder hears), `orderEnd(player)` to hide it. It is drawn by the new, reusable `combat/HudCounter` (see [SPELL_KIT.md](SPELL_KIT.md)).

Every timed cast can show its picture and clock above the hotbar: `PrinceOfAbolitionFx.status(player, Status.X, ticks)`, and `statusEnd` to take it away (28 statuses: 14 for what the Beyonder holds, 14 for what is done to others when they are players; `statusOf(Feeling)` and `statusOf(Bribe)` pick the right one). The Pathway's icon beside the hotbar is registered in `PathwayHudIcon` under `"Prince Of Abolition"`.

What is in the repository, all of it generated by the scripts in `tools/model_gen/prince_of_abolition/` (see "Prince Of Abolition: ready-made displays" in [MODEL_FX.md](MODEL_FX.md)):

| What | Where | Made by |
| --- | --- | --- |
| 25 model files: the gavel, a mausoleum (whole and cracked), the illusory door (4 frames, closed to wide open), the crown, the thrown flower, a black bolt, the fake self of Paces; and the pictures: the seal of the Black Emperor, the Compromised scales, the Lawyer's script, the Bribe coin, the Disorder clock, the Inversion sigil, the Decree word, the mausoleum sigil, Bestowment's mask, a coin, a Seal lattice tile (animated), an Abolition fog tile (animated), the Entropy field (animated) and a beam (animated) | `models/model_fx/prince_of_abolition/`, `textures/model_fx/prince_of_abolition/` | `models.py` (+ `textures.py`) |
| 21 wheel pictures (32 px), 28 status pictures (16 px), the Order sigil, the Pathway icon | `textures/gui/ability_menus/prince_of_abolition/`, `textures/gui/prince_of_abolition/`, `textures/gui/pathway_icons/prince_of_abolition.png` | `icons.py` |
| 8 particle pictures: `fx/coin`, `fx/section`, `fx/crown`, `fx/cog`, `fx/keyhole`, `fx/stele`, `fx/twist`, `fx/gavel` | `textures/particle/fx/` | `particles.py` |
| 41 sounds (43 files): Verdict's gavel, Brainwash, Brute Force, Iron Will, bribes, Weaken, Charm, Arrogance, Connect, Coin Shot and its ricochet, Distort and a shot turned back, the illusory door rising, the Seal slamming and breaking, Corrosion, Ruin, Step, Disorder, Paces, the fake self bursting, Majesty, Bestowment, Exploit, Magnify and its quake, a mausoleum rising and crumbling, Salvo, Sanctuary, Rebirth, Frenzy, Entropy, Abolition, Exile and the return, Inversion, Coronation, Decree and a new standing order | `sounds/prince_of_abolition/`, `sounds.json`, subtitles in `lang/en_us.json`, registered in `sounds/ModSounds.java` (`ABOLITION_...`) | `sounds.py` |

## What is built

The game code is in `beyonder_characteristics/prince_of_abolition/`, next to `PrinceOfAbolitionFx`:

| Class | What is in it |
| --- | --- |
| `PrinceOfAbolition` | The Pathway's name, Sequence Might (`attack`, `defense`, `range`, `area`, `duration`; Magnify widens range and area while it waits), the recast rules of the wheels (`recastCost`, `noCooldowns`), the events (damage in and out, Abolition's shell, Inversion, Distorted blows, mobs that will not pick you, Beyond the World, cleanup on login, respawn and logout), keys 1 (Verdict / Brainwash), 2 (Brute Force / Iron Will), 4 (Distort, the shell flip / Seal, the gate) and 5 (Paces / Disorder), every passive, the look of the rot and of this Pathway's misses, and the melee combo |
| `PrinceOfAbolitionMarks` | The small statuses put on others: Compromised, Brainwashed, Weakened, Charmed, Arrogant, Connected, Distorted, Disordered, Corrupted (Corrosion's greed), Kneeling, and Bestowment's Sluggish, Anxious, Defeated, Rash, Avaricious. One kind, who put it there, when it ends; mind marks do not take through Iron Will and last half as long on a Barbarian of this Pathway; a kind with a miss share hands it to `status/Falter` |
| `PrinceOfAbolitionOrders` | Order: every standing order with its end, nine counted at most, and the `[sigil] Order 4/9` counter |
| `PrinceOfAbolitionMausoleums` | The mausoleums: raising, the 2-minute life, health and who can strike them, the +5% bonus (doubled while crowned), Sanctuary, Rebirth, Last Rebirth, detonating (Blast) and stretching (Exploit) |
| `PrinceOfAbolitionBribes` | Key 3, the Bribe wheel: Weaken, Charm, Arrogance, Connect, Coin Shot, actual Bribes, and the magnified Bribe (a thrown flower that lands as a quake) |
| `PrinceOfAbolitionFallen` | Key 6, the Fallen wheel: Raise Mausoleum, Corrosion, Step, Majesty, Bestowment, Exploit, Magnify, Mausoleum Salvo, Mausoleum Blast |
| `PrinceOfAbolitionSovereign` | Key 7, the Sovereign wheel: Ruin, Frenzy, Entropy, Abolition, Exile, Inversion, Coronation, Decree |

New parts of the spell kit this Pathway brought, each usable by any other Pathway (see [SPELL_KIT.md](SPELL_KIT.md)):

| Part | What it is | Used here by |
| --- | --- | --- |
| `status/Corrosion` | A rot that grows every second until cleansed, and stops all healing after 5 seconds | Corrosion, Ruin |
| `status/Cleansing` | What "cleansed" means: `Spells.cleanse` and a milk bucket take off every status that listens | Corrosion's rot |
| `status/Falter` | A share of someone's attacks go wide, one named reason at a time | Arrogance, Disorder, Corrosion, Anxious |
| `status/Exile` | Out of the world for a while: unseen, unhurt, unable to act, back where it left or where you say | Exile, Last Rebirth |
| `combat/Wall` | A flat barrier in the air that enemies and their projectiles cannot cross | Seal's gate (Earl of the Fallen) |
| `Zone.covers`, `centerOf`, `radiusOf`, `ticksOpen`, `addShelter` | Asking whether a spot is in a zone; keeping some entities out of other players' zones | Entropy, Black Emperor's Abolition |
| `Dome.raisedAt`, `crossedBy`, `flip` | Anyone's domes at a spot or on a line; turning one into a cage for its owner | Distort's shell flip (Duke of Entropy) |
| `Spells.strikeTrue` | A hit no raised shield, armor, Protection or Resistance softens | Decree |
| `AbilityWheel.quickCastCostBy`, `noCooldownsWhen`, `costBy` | The first half's wheel options (recast price by Sequence, no cooldowns, a cost worked out at the cast) | all three wheels |

Also changed in the first half: the seven key packets, `PathwayAbilities`, `SequenceCommands`, `SequencePassives` (Silver Tongue, Strength I, Presence), `MeleeCombos` and Dark Angel's soul colors know the Pathway, and `FrenziedMage.java` checks "Frenzied Mage" (it still checked the old name "Prince Of Abolition", so the two Pathways shared their keys).

### Where the code differs from the design above

Each of these is the nearest thing the spell kit can do today without breaking blocks, new entities or Minecraft calls the repository does not use yet. Change them when the parts exist.

- **Disorder:** a player's view swims (Nausea) instead of the hotbar slot jumping about.
- **Weaken:** the holds a Weakened enemy puts on you do not yet last half as long (holds do not know who laid them).
- **Arrogance:** the misses work; mobs retreating or healing is not touched, and ranged attacks miss rather than scatter.
- **Charm:** a mob that turns on its own side attacks its nearest ally for 5 seconds (status/Taunt) rather than becoming your marionette, which would drop it dead when the time runs out. Players cannot be turned.
- **Connect:** the enemy glows (Glowing), which everyone sees, not only you.
- **Paces:** the fake self is a picture, so nothing can strike it. It bursts when an enemy walks into it, and the mobs that hunted you are sent to it.
- **Iron Will:** it shakes off this Pathway's mind marks, fear, sleep and nausea, and gives full knockback resistance. Other stagger is not stopped. Corrosion's rot is not a mind effect: Iron Will stops the greed but not the rot.
- **Silver Tongue:** Hero of the Village only; neutral mobs' grudges are not shortened.
- **Weakness Detection:** the keyhole and the crit; the health and effect readout is not written.
- **Mausoleum health:** a hostile mob that hunts you (or one of your allies) and stands next to a mausoleum strikes it once a second with its own attack damage; an enemy player swinging at one within 3.5 blocks strikes it with theirs. Explosions and enemy abilities do not hurt them.
- **Mausoleums** only count, fire and revive in the world their owner is in, and crumble when the owner logs out.
- **Brute Force's broken guard:** the shield comes down and no item can be used for 5 seconds.
- **Bestowment:** Anxious slows instead of stopping sprinting and shaking the aim (40% of its attacks go wide); Avaricious on a player cuts their damage by 30% (there is no mob mind to steer); Rash's "cannot stop moving forward" is a shove every second.
- **Exploit on yourself:** good potion effects, hiding, flight, a Coronation and the mausoleums are stretched; your own Seal, gate and fields are not (their looks are drawn for a fixed time).
- **Magnify:** the 1.5 times range and area count for every cast while it waits, not only the one it is spent on.
- **Entropy:** projectiles turn off course; abilities of other Pathways that are not projectiles do not.
- **Abolition:** blows, aimed abilities, projectiles, mobs' attention and explosions with a source are stopped at the shell; nobody outside is kept from seeing in (there is no per-viewer hiding).
- **Coronation:** you cannot be knocked back; pulls by other abilities are not stopped.
- **The gate** is 10 by 5 blocks whatever the Sequence, the size its lattice is drawn at.

Before playing, run `./gradlew build`: this code was compiled only against stand-ins of the Minecraft and Forge classes it uses, since the build server could not be reached from where it was written. Then try each key in game with `/sequence pathway "Prince Of Abolition"` and `/abolitionfx` for the looks.
