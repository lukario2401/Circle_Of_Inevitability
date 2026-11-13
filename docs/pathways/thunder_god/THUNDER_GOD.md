# Thunder God: ability design

**Implementation update:** Sequences 9–0, all five wheels and Source of Wrath are playable; see [controls, costs and tests](THUNDER_GOD_IMPLEMENTATION.md). The original draft below is preserved, including its starting numbers and deferred questions.

Design only · for the Circle Of Inevitability repo · drafted by Claude, November 1, 2026 · no code written yet

The Thunder God Pathway is the Tyrant Pathway of Lord of the Mysteries, the Hierophant tarot card: Sailor (9), Folk of Rage (8), Seafarer (7), Wind-blessed (6), Ocean Songster (5), Cataclysmic Interrer (4), Sea King (3), Calamity (2), Thunder God (1), Tyrant (0). Above the Sequence: God Almighty. Sefirah: Chaos Sea. Source: the Tyrant Pathway and Tyrant Pathway/Abilities pages of the Lord of the Mysteries wiki. The wiki lists no active ability for the Sailor (9) and almost none for the Sea King (3) beyond authorities, so those are designed from the Pathway's themes. Sequences 1 and 0 are written out in full, because the wiki has plenty for them.

Pathway name in code: `"Thunder God"` (named after Sequence 1, like `"Prince Of Abolition"` and `"Attendant Of Mysteries"`).

**Status: design.** This file is the plan the code will follow. Nothing here is built, and every number is a starting value.

## Theme

A storm that walks. The wiki calls it the Pathway of water and weather: sea, wind, lightning, sound, fear and calamity, "omnipotent in the sea, land and air". The Pathway is Tyranny (暴): temper first. Folk of Rage are angry, Wind-blessed are irascible as a gale, Ocean Songsters sing, and the top Sequences wear down the world with horror, thunder and weather. It instills fear and oppression in others and resists fear and oppression itself. Lightning is always **silver-white**; the sea is deep blue; anger reddens the eyes.

The ladder, in one line: a sailor with a harpoon, a brawler who gets angrier with every blow, a seafarer who throws water, a wind-rider, a singer who calls down lightning, a storm that roars, a sea king, a calamity that moves whole weather, a thunder god whose bolt cannot be escaped, and a tyrant whose presence alone makes people kneel.

Colors: storm-sea blue (Pathway color `0x10294A`), silver-white lightning (accent `0xD6ECFF`), deep teal for the water spells (`0x1F6F8B`), ember red for Rage (`0xC0392B`).

**World rule (same as Prince Of Abolition): nothing here breaks or places real blocks.** Water, rain, waves, whirlpools, storm clouds and kingdoms are statuses with visuals. Lightning is a drawn bolt plus damage, never a vanilla `LightningBolt` (which sets fires, turns villagers into witches and charges creepers). Weather is local and visual (a storm over a spot), not the world's rain/thunder state. See question 4 if you want the real one.

## Rage (this Pathway's own mechanic)

Attendant Of Mysteries has stacks, Prince Of Abolition has Order; Thunder God has **Rage**, the wiki's "hits".

- **Gain:** +1 for every enemy hit by a blow or ability of yours (once per enemy per cast), and +1 when you take a hit (at most once every half second). Mentioned by the wiki: being intoxicated helps, so while **Tipsy** (you drank any potion or honey bottle in the last 30 seconds) everything counts double.
- **Loss:** 1 every 5 seconds without gaining. Wrath, Raging Blow and Source of Wrath spend it (see below).
- **Cap:** 10. Shown next to the hotbar as one small ember sigil, the word **Rage**, then count and cap: `[sigil] Rage 6/10` (a `HudCounter`, like Prince's Order).
- **What it does by itself:** every point of Rage adds +2% to the damage you deal (+20% at 10). At 10 your eyes burn red (a look only).

Besides the +2% damage, only Raging Blow, Wrath and Source of Wrath use Rage, and only a few things feed it (Siren Song's bonus, Chasm of Storms, Authority of Thunder). So Rage rewards fighting close without making the rest of the kit depend on it.

## Soaked (the sea's mark, shared by the water and lightning spells)

Water and lightning are one trade here. **Soaked** is a short mark (10 seconds) put on an enemy by almost every water, rain and flood entry (Water Veil, Sweeping Wave, Water Sphere, Corrosive Rain, Tsunami, Maelstrom, Wrath of the Sea, every storm zone) and also by simply standing in water or in your own rain. A Soaked enemy:

- takes **+40% from every lightning hit** (the Shock rules below), and the bolt arcs to up to 3 other Soaked enemies within 5 blocks for half;
- is put out if burning and cannot be set alight by fire for the duration.

This is what makes the Pathway a combo: **water first, then thunder.** Allies are never Soaked by your spells.

**Shock** is the one rule every lightning entry shares: undead and evil things (the wiki's Purification) take +50%; a victim in metal armor or Soaked takes the arc described above; the hit paralyzes (cannot move) for 0.5 to 1 second, with a 3-second immunity window so lightning cannot lock someone forever; the damage is credited to you (`DamageFx.credit`) so the number shows as yours. Put it in one helper (see "New reusable parts").

## How it is laid out

It follows the Attendant Of Mysteries pattern: seven keys (G H J K L N M), every key has a normal cast and a sneak cast. Keys 1 and 2 are single abilities. Keys 3 to 7 are wheels like Apocalypse's and Prince Of Abolition's, where every entry has its own Sequence, cost and cooldown.

**Wheels work the Prince Of Abolition way: sneak opens the wheel, a normal cast recasts the last pick** (`AbilityWheel.quickCast`, flipped). With no pick yet, the normal cast opens the wheel.

**Unlike Prince, a recast pays the same price and waits out the same cooldown.** It is a shortcut, not a bypass. The reason is in the balance notes: spirituality refills at once from Sequence 2, so only cooldowns can limit the top entries.

The abilities do not each get a Sequence of their own. Sequence 5 has nine entries and Sequence 9 has two, and many entries grow with the Sequence (see "Grows with the Sequence"). No Sequence from 9 to 0 is left without something to press.

| Sequence | Abilities |
|---|---|
| Sailor (9) | Harpoon, Illusory Scales |
| Folk of Rage (8) | Raging Blow, Wrath |
| Seafarer (7) | Water Veil, Sweeping Wave, Tideheal |
| Wind-blessed (6) | Windblade, Gale Burst, Gale Ride |
| Ocean Songster (5) | Water Curtain, Water Sphere, Corrosive Rain, Wind Binding, Lightning Strike, Lightning Bolt Arrow, Siren Song, Discord, Thunderclap |
| Cataclysmic Interrer (4) | Windblade Legion, Chain Lightning, Roar, Death Dirge, Hurricane, Tsunami |
| Sea King (3) | Sea Master, Maelstrom, Destructive Droplet, Sonic Boom, Cloud Sword, Lightning Storm, Wrath of the Sea |
| Calamity (2) | Magnetic Pole Pull, Electromagnetic Disruption, Sound of Horror, Requiem, Storm Incarnation |
| Thunder God (1) | Heavenly Punishment, Ocean of Lightning, Roar of the Thunder God, Incarnation of Light, Storm Hell |
| Tyrant (0) | Lightning Forest, Tyrant's Presence, Starfall, Chasm of Storms, Dreadful Heaven, Banish into the Storm |

48 abilities in all: 4 on single keys and 44 in five wheels (9, 9, 9, 9 and 8 entries).

## Keys

| Key | Ability | Unlock | Normal cast | Sneak cast |
|---|---|---|---|---|
| 1 | Sailor's Kit | Sailor (9) | **Harpoon** | **Illusory Scales** |
| 2 | Rage | Folk of Rage (8) | **Raging Blow** | **Wrath** (from Calamity (2): **Source of Wrath**) |
| 3 | Tides wheel | Seafarer (7) | Recast the last Tides pick | Open the Tides wheel |
| 4 | Sky wheel | Wind-blessed (6) | Recast the last Sky pick | Open the Sky wheel |
| 5 | Thunder wheel | Ocean Songster (5) | Recast the last Thunder pick | Open the Thunder wheel |
| 6 | Voice wheel | Ocean Songster (5) | Recast the last Voice pick | Open the Voice wheel |
| 7 | Storm wheel | Cataclysmic Interrer (4) | Recast the last Storm pick | Open the Storm wheel |

### Key 1: Sailor's Kit (Sailor, 9)

| Cast | What it does |
|---|---|
| Normal: **Harpoon** | You throw a barbed iron harpoon on a rope, up to 24 blocks. Hitting an enemy: 30 damage and the rope hauls it 5 blocks toward you (a drag, it still fights). Hitting a block or the surface of water instead: the rope holds and you haul yourself along it to that spot, with no fall damage on arrival. It works in water as it does on land. The rope is drawn as a thin line to the harpoon for as long as it holds. |
| Sneak: **Illusory Scales** | The scales hidden beneath your skin surface for 10 seconds: 20% less damage and 6 absorption (3 hearts). In water or in rain the cut is 35%. Every Sequence after 9 adds 2 absorption and 2 points of cut, up to 24 absorption and 38% (53% wet) at Tyrant (0), as the wiki says "every Sequence advancement brings improvements". Cast again to drop them early. |

### Key 2: Rage (Folk of Rage, 8)

| Cast | What it does |
|---|---|
| Normal: **Raging Blow** | The terrifying blow of stored anger. It needs at least 3 Rage and spends all of it; with less it does not go off and costs nothing ("not angry enough"). One blow on the enemy in front of you (reach 4): **20 + 8 damage per Rage spent**, thrown back 6 blocks, and enemies within 2.5 blocks of it take half and are knocked aside. At 10 Rage it also breaks the shield or block stance for 5 seconds and staggers it for 1 second. The damage per Rage grows with the Sequence (see below). |
| Sneak: **Wrath** | You draw a gasp, your eyes burn and your muscles swell: for 10 seconds Strength II, Speed I, 30% faster swings, 15% less damage taken, and full fear resistance. Every blow in Wrath adds an extra +1 Rage. When it ends you pay: Weakness I for 4 seconds. Cast again to end it early. |

**Source of Wrath** (from Calamity (2), on the same sneak press): needs a full 10/10 Rage and spends it. For 20 seconds you act **one Sequence stronger**: this Pathway's wheel entries one Sequence beyond yours can be picked and cast, and everything you deal is +25% (Thunder God (1) at Source of Wrath reaches the Tyrant's entries, +50%). It does not raise your spirituality, passives or health ("it cannot enhance one's status"). **The price is rational thought:** while it lasts, calm entries (Tideheal, Siren Song, Requiem, Cloud Sword aimed at an ally) are refused, your area attacks also hit allies in them at full damage, and your aimed attacks scatter by up to 5 degrees. When it ends, Rage drops to 0 and you are Spent for 10 seconds (Weakness II, Slowness I). With less than 10 Rage the press is plain Wrath.

## Wheels

### Key 3 wheel: Tides (water, from Seafarer, 7)

| Entry | Sequence | What it does |
|---|---|---|
| Water Veil | Seafarer (7) | A film of water settles over the face of the enemy you look at (up to 20 blocks) and will not come off: 6 seconds, 5 damage a second, Soaked, and it cannot speak or cast (Silence). It sprints badly. Only a cleanse takes it off early (Tideheal, a milk bucket). Bosses: 3 seconds. |
| Sweeping Wave | Seafarer (7) | Azure lights cross in front of you and a huge wave sweeps forward, 18 blocks long and 6 wide: 45 damage, thrown back 6 blocks, everything hit is Soaked, and fires out. Allies in its path are untouched. |
| Tideheal | Seafarer (7) | You or the ally you look at (up to 24 blocks): 12 health over 6 seconds, burning put out, and one rot, poison or Veil taken off (`Spells.cleanse`). "Inferior to dedicated items": it heals a little and cleans a lot. The aqueous light that cleans surfaces. |
| Water Curtain | Ocean Songster (5) | A sheet of water 8 wide and 4 tall stands in front of you for 6 seconds: enemy projectiles stop at it, enemies pass through it slowly, and fire and explosion damage inside it is halved. You and your allies walk through freely. From Cataclysmic Interrer (4) it is a Water Wall (see below). |
| Water Sphere | Ocean Songster (5) | An enormous sphere of water, 5 blocks across, swallows the enemy you look at (up to 20 blocks): held in place, 6 damage a second, 5 seconds, Soaked. It can breathe nothing in it. Bosses: 2.5 seconds. |
| Corrosive Rain | Ocean Songster (5) | A rain cloud forms over the ground you aim at (up to 12 blocks) and rains for 10 seconds, 7 blocks wide: 6 damage a second, armor wears faster, everything in it is Soaked, and an enemy that stays 4 seconds starts to rot (`Corrosion`). Allies are untouched. See "Grows with the Sequence" for the Torrential Downpour it becomes. |
| Sea Master | Sea King (3) | The water around you obeys: a calm, windless lake spreads 16 blocks around you for 30 seconds. Enemy projectiles in it are slowed by 60%, you see where abilities were cast lately inside it (the wiki's lake that reflects the past), and three sea creatures rise from it and fight for you (guardians and drowned, see question 7). They vanish without drops. |
| Maelstrom | Sea King (3) | A whirlpool opens at the ground you aim at (up to 24 blocks): 10 blocks wide, 8 seconds, drawing everything in toward its center. Flyers are pulled too, and a teleport does not free you from it. At the center: 30 damage a second and held. A Substitute does not save them. Everything in it is Soaked and fire is put out. |
| Destructive Droplet | Sea King (3) | One glowing drop of water leaves your fingertip after a 1 second charge and flies up to 40 blocks. Where it lands it breaks apart: a silent flash 6 blocks wide, 140 damage, no block damage. Survivors are **Parched** for 10 seconds (Weakness II, Slowness I, no healing). A non-boss up to 60 maximum health dries to a husk and dies. |

### Key 4 wheel: Sky (wind and light, from Wind-blessed, 6)

| Entry | Sequence | What it does |
|---|---|---|
| Windblade | Wind-blessed (6) | Three nearly invisible blades fan out from your hand, 24 blocks, each 22 damage, passing through the first enemy to hit a second. Only a faint shimmer shows them. |
| Gale Burst | Wind-blessed (6) | A pressure wave explodes from your body: enemies within 8 blocks take 20 and are thrown outward, projectiles within 8 blocks are torn off course, and loose items and arrows are swept into your palm (the wiki's "objects flying into your palms"). |
| Gale Ride | Wind-blessed (6) | The wind gathers beneath your feet. On the ground, your running speed doubles for 8 seconds and falls do not hurt. In the air or after a jump it becomes flight for 6 seconds (hold sneak to hover, the Floating Wind). Where flight is refused (a Decree forbids it), the same cast becomes a Glide, as the wiki says. See "Grows with the Sequence" for how far it goes. |
| Wind Binding | Ocean Songster (5) | Spiraling winds bind the enemy you look at (up to 20 blocks): held for 3 seconds and drawn into the spiral. Enemies within 3 blocks of it are bound for half that. |
| Windblade Legion | Cataclysmic Interrer (4) | Hundreds of blades converge from every direction on the enemy you look at (up to 30 blocks), or where you aim: 12 waves in 2 seconds in a 5 block sphere, 8 damage each wave. They cannot be blocked from one side, and they nick armor. |
| Sonic Boom | Sea King (3) | Aimed at a spot up to 40 blocks away, a boom bursts there at once: 8 blocks wide, 90 damage, thrown 12 blocks away, deafened 1.5 seconds (cannot cast). |
| Cloud Sword | Sea King (3) | Clouds gather and weave into a massive sword. Aimed at an ally within 40 blocks it descends to stand before them for 15 seconds: a wall 3 wide that stops projectiles and melee, and 30% less damage for the ally behind it. Aimed at an enemy or the ground, it falls instead: 160 damage on a 3 wide line, thrown aside. |
| Incarnation of Light | Thunder God (1) | You become a body of energy for 6 seconds and fly where you look at nearly the speed of light (about 4 blocks a tick, faster than any Gale Ride, turning 12 degrees a tick), unseen and unhurt on the way. Every enemy you pass through takes 120 damage once, and the path burns for 3 seconds. You step out in a flash (sneak to leave early): 80 damage in 5 blocks. Up to 150 blocks. |
| Starfall | Tyrant (0) | You vanish into layers of entangled ball lightning, rise, and fall as a point of light carrying a storm onto the spot you aimed at (up to 160 blocks). It takes 2 seconds, and a warning ring 14 blocks wide shows for the last 1.5 so enemies can step out. At the center 400 damage, falling to 150 at the edge, everything thrown 14 blocks. The spot stays electrified for 10 seconds. You land at the center unhurt. |

### Key 5 wheel: Thunder (lightning, from Ocean Songster, 5)

Every bolt here (and every bolt in the other wheels) uses the Shock rules above. Magnetic Pole Pull and Electromagnetic Disruption are control, not lightning hits.

| Entry | Sequence | What it does |
|---|---|---|
| Lightning Strike | Ocean Songster (5) | A silver-white bolt falls on the enemy you look at (up to 40 blocks). It cannot be dodged once you cast it; the only defense is to leave your line of sight first. 55 damage and paralysis. "Can be cast with just sight, even from your back": it ignores which way you face (but still needs line of sight). Spreads along water and metal: the electric snakes. Aimed at no one, it instead charges your weapon: your next 6 blows in 10 seconds add 15 lightning damage and paralyze for 0.3 seconds. |
| Lightning Bolt Arrow | Ocean Songster (5) | Needs a bow or crossbow in either hand (no arrow used). Your hair unfurls, the sky darkens, and after a 1 second draw you loose a silver-white arrow that hits at once along 60 blocks: 95 damage, passing through up to two enemies, paralysis 0.5 seconds. Without a bow it is refused and costs nothing. |
| Chain Lightning | Cataclysmic Interrer (4) | Three bolts leave you at the three nearest enemies in sight and then chain on: each bolt starts at 60 damage, jumps up to 8 blocks to the next enemy for 70% of the last, four jumps at most. Soaked enemies pass the bolt on to two more. |
| Lightning Storm | Sea King (3) | A silver ball explodes into a "forest" of bolts, 9 blocks wide for 8 seconds, centered on the ground you aim at (up to 30 blocks). A bolt falls every 4 ticks on a random spot in it for 25 damage, and everyone inside is stunned for the first 1.5 seconds. It does not pick sides: allies in it take half (see question 3). |
| Magnetic Pole Pull | Calamity (2) | You turn a spot (up to 30 blocks) into a pole for 6 seconds: everything within 14 blocks is drawn in, enemies in metal armor twice as hard, and metal projectiles are pulled out of the air. The field rotates and its power builds for 4 seconds: after 3 seconds enemies are whirled round and at the end thrown out for 120 damage and 14 blocks. Flyers are pulled too. |
| Electromagnetic Disruption | Calamity (2) | A field 36 blocks wide stands around you for 20 seconds (or around a spot you aim at, up to 40 blocks). Inside it enemies' information powers fail: Connections end and cannot be made, Prediction, Dossier and Study return nothing, remote targets are lost, and 50% of their aimed abilities miss. It is invisible. From Thunder God (1) it is the **Dance of Thunder** (see below). |
| Heavenly Punishment | Thunder God (1) | A pillar of dark cloud forms over the enemy you look at (up to 80 blocks) for 1.2 seconds and a massive bolt falls, the cloud's only warning. 350 damage that nothing softens (`strikeTrue`), 60% in a 4 block radius, double against undead, rotting and withering targets (the wiki's Darkness, Death, Corruption and Filth). **It cannot be escaped by changing shape:** it also hits through Concealment, a Form and a Swarm and drags out an Exile in its area. |
| Ocean of Lightning | Thunder God (1) | A sea of lightning spreads 28 blocks around the spot you aim at (up to 50 blocks) for 20 seconds, knee-deep and crackling. Enemies in it take 20 a second and are slowed (Slowness II); metal armor doubles it; enemy projectiles in it die; Soaked enemies are stunned for 0.5 seconds every 3 seconds. It reaches 8 blocks up, so flyers are not safe. You and your allies move in it as in water, unhurt. |
| Lightning Forest | Tyrant (0) | Thousands of bolts as thick as anacondas fall in a 40 block radius for 5 seconds, about 400 damage to each enemy over that time. The forest tears every enemy barrier whose center lies in it (domes, walls, zones, seals) and every veil: Concealment and Exile in it end. Allies in it are unharmed. The ground stays electrified for 6 seconds. |

### Key 6 wheel: Voice (singing, roars and horror, from Ocean Songster, 5)

Each Voice entry is a sound: a Silenced caster cannot cast it. The wiki's songs cannot be blocked by plugging the ears (they work on the spirit body), so there is no "deaf" defense against them.

| Entry | Sequence | What it does |
|---|---|---|
| Siren Song | Ocean Songster (5) | A beautiful song reaches the spirit body of every enemy within 14 blocks: they fall into a daze for 6 seconds and stop attacking. It ends on the first damage they take. You are not dazed, and your next Raging Blow within 10 seconds does 30% more. |
| Discord | Ocean Songster (5) | A chaotic, unpleasant song breaks their reason: 8 seconds for every enemy within 14 blocks. A mob attacks whatever is nearest, each other included. A player takes 3 damage a second as their blood vessels throb, and 40% of their attacks go wide. |
| Thunderclap | Ocean Songster (5) | You simulate a thunderous boom. Enemies within 10 blocks are awed: stiff for 1.5 seconds, afraid for 3, 25 damage. From Sea King (3): 24 blocks and it awes spirit bodies too (ghosts, concealed and spirit things in range are revealed and staggered). |
| Roar | Cataclysmic Interrer (4) | A roar that could topple a ship: a sound wave 20 blocks long in a cone. 70 damage, thrown back 10 blocks, enemies on the ground are knocked flat for 1 second, shields broken for 5 seconds, and riders thrown from boats and mounts. |
| Death Dirge | Cataclysmic Interrer (4) | A melody of death spreads 16 blocks around you for 12 seconds and follows you. Enemies in it cannot use Sealed Artifacts (their activation is refused), and they are slowed and take 4 damage a second. A song that disrupts "the operation of Sealed Artifacts within the vicinity". |
| Sound of Horror | Calamity (2) | A sound only living things can hear: every enemy within 64 blocks is seized by extreme horror for 8 seconds (Fear, bosses 3 seconds) and Sealed Artifacts they hold are locked for 10. Your allies hear nothing. |
| Requiem | Calamity (2) | The ethereal song: the enemy you look at (up to 32 blocks) recalls everything. For 8 seconds pale arms tear at its spirit body: it cannot cast (Silence), 60% of its attacks fail, it takes 8 damage a second, and it gains 1 Madness a second, so the existing Madness thresholds apply (nausea, falter, fear, a stun at 10). |
| Roar of the Thunder God | Thunder God (1) | A sonic attack that spreads at the speed of lightning: it lands at once, a 30 block sphere around you. 120 damage, every enemy afraid for 6 seconds, half its attacks failing, and stunned for 1. Double against undead, rotting and withering targets (Darkness, Death, Corruption and Filth). |
| Tyrant's Presence | Tyrant (0) | A domineering, savage, terrifying aura for 30 seconds in 24 blocks. Everything hostile within it stiffens for 3 seconds at the start (bosses 1.5; a Sequence 0 enemy is not affected), cowers 25% slower after, loses any invisibility, and cannot possess anyone near you (Parasite is repelled). While it lasts, every lightning, wind and storm entry of yours also adds 2 seconds of fear to whatever it hits ("their storms contain immense horror"). |

### Key 7 wheel: Storm (calamity and kingdoms, from Cataclysmic Interrer, 4)

| Entry | Sequence | What it does |
|---|---|---|
| Hurricane | Cataclysmic Interrer (4) | Violent winds collide with the air, and a hurricane you can see swirls up around you: 6 blocks wide and 8 tall for 10 seconds, moving with you. Enemy projectiles and spells are thrown out of it, enemies inside are thrown outward and lifted, 20 damage a second, and sight is cut (no enemy outside can aim at you or use a gaze or line-of-sight power on you). From Sea King (3) it ends by shattering into a small Lightning Storm. |
| Tsunami | Cataclysmic Interrer (4) | A wave 10 blocks tall and 24 wide rolls 40 blocks ahead of you: 110 damage, thrown 15 blocks back, everything hit is Soaked, loose things swept away. Allies pass unharmed. From Sea King (3) the wave is 40 tall, 48 wide, rolls 80 blocks, and leaves the ground flooded for 10 seconds (slowed enemies, Soaked). |
| Wrath of the Sea | Sea King (3) | You mark the enemy you look at (up to 40 blocks) for 20 seconds: the sea rejects it. While water, rain or your storms are within 10 blocks of it a wave breaks on it every 2 seconds (30 damage, shoved back); it cannot enter water, swim, or stay in a boat; and your Tsunami bends toward it. |
| Storm Incarnation | Calamity (2) | Dark clouds gather overhead and form a face with brows and a mouth. For 20 seconds it floats 18 blocks wide the way you look (up to 60 blocks from you to start), raining and howling: a bolt every 5 ticks for 30, gales that push, and a booming roar every 5 seconds that stuns for 1. The storm of the wiki that can destroy a large city. |
| Storm Hell | Thunder God (1) | You designate a place (up to 80 blocks) and it is engulfed: a 40 block radius for 45 seconds. Hurricane winds, torrential rain (sight cut to 6 blocks), a bolt every second on a random enemy for 40, slowed enemies, no flying (winds slam flyers to the ground), and every enemy projectile dies. Cast again to end it. |
| Chasm of Storms | Tyrant (0) | The Divine Kingdom of Thunder and Calamity: a shell 30 blocks wide stands around you for 60 seconds and nothing crosses it. The sky inside is black and endless storm; every enemy inside takes a bolt for 80 every 3 seconds; your Rage fills; your Thunder, Sky and Storm entries cost 50% less and recover twice as fast. |
| Dreadful Heaven | Tyrant (0) | The Divine Kingdom of the Tyrant, "Sole Obedience to the Storm": the same shell and time. Every enemy inside is kept afraid (it cannot come near you, a mob will not target you), half of its attacks fail, and its abilities cost it double. Your Voice entries cost nothing inside it. Only one kingdom stands at a time. |
| Banish into the Storm | Tyrant (0) | You banish the enemy you look at (up to 40 blocks) into a never-ending storm for 10 seconds (bosses 4): it cannot be seen, hurt or act, as if in Exile, and returns where it left, afraid for 4 seconds, Soaked and for 100 damage. Aimed at an ally it hides them for 4 seconds and they return with 20 absorption. |

## Grows with the Sequence

These do not get their own key or entry. The same ability is stronger from the Sequence on.

| Ability | From | What changes |
|---|---|---|
| Illusory Scales | every Sequence | +2 absorption and +2% cut per Sequence after 9. At Ocean Songster (5), when the absorption is spent the scales burst: 30 damage and a 4 block shove to everything next to you |
| Harpoon | Seafarer (7) | Navigation: it curves up to 15 degrees toward an enemy near your crosshair, thrown "with your eyes closed" |
| Raging Blow | every Sequence | Damage per Rage is 8 + 2 for every Sequence stronger than 8: 24 per Rage at Tyrant (0) (260 at 10 Rage). At Sea King (3) the blow also sends a ring of silver lightning 5 blocks wide |
| Wrath | Ocean Songster (5) | The gasp: no draw, instant, and it adds +3 Rage |
| Wrath | Sea King (3) | Lasts 20 seconds |
| Wrath | Calamity (2) / Thunder God (1) | Becomes Source of Wrath at full Rage (see Key 2) |
| Water Curtain | Cataclysmic Interrer (4) | Becomes a Water Wall: 14 wide and 6 tall, 12 seconds, enemies who touch it take 15 damage, and it slows petrification |
| Corrosive Rain | Cataclysmic Interrer (4) | Becomes **Torrential Downpour**: 22 blocks wide for 20 seconds, enemies inside see only 6 blocks (Darkness) and cannot be targeted from outside; flames inside are weak |
| Corrosive Rain | Sea King (3) | Every drop is your eye: you see every enemy inside through walls |
| Tideheal | Sea King (3) | Doubled when it is raining or you or the ally are in water |
| Lightning Strike | Cataclysmic Interrer (4) | The **Thunder Strike**: 90 damage, paralysis 1 second, heavy purification damage |
| Gale Ride | Ocean Songster (5) | 12 seconds of flight, hover |
| Gale Ride | Cataclysmic Interrer (4) | Real flight and the Glide: up to 25 seconds, the game's stand-in for 500 km/h (1.5 blocks a tick) |
| Gale Ride | Sea King (3) / Calamity (2) | Faster than sound (2 blocks a tick) with a boom behind you (20 damage and a shove to enemies near the path), then missile speed (3 blocks a tick) |
| Thunderclap | Sea King (3) | See its entry: 24 blocks and spirit bodies |
| Hurricane | Sea King (3) | Shatters into a Lightning Storm when it ends |
| Tsunami | Sea King (3) | See its entry: 40 blocks tall |
| Electromagnetic Disruption | Thunder God (1) | The **Dance of Thunder**: the thunder is invisible and silent, and it also destroys information structures: on cast, every enemy in range loses its marks, Connections and Countdown on you, and its Concealment and Unnoticed |
| Roar | Thunder God (1) | Ends in a Thunderclap that awes everything it hit |

## Passives

What a Beyonder of the Pathway always has from a Sequence on, with no key press and no spirituality cost. They go in `SequencePassives`.

| Passive | From Sequence |
|---|---|
| Aquatic Affinity: Water Breathing and no slowing in water. Fear Resistance: fear and oppression last half as long. Balance: no slowing from boats or rain, and no knockdown stumble. Strength I | Sailor (9) |
| Physical Enhancement: two extra hearts and the Folk of Rage's strength; the storm of the wiki ("facing them is like facing a storm") | Folk of Rage (8) |
| Sea's Favor: in water, in rain or at your own storm you deal +10% damage, take 10% less, and your spirituality returns 25% faster. Navigation: you always know your coordinates and where the nearest rain or thunder is | Seafarer (7) |
| Wind-blessed: Night Vision, an air cushion (50% less fall damage), and a dive depth of 100 blocks | Wind-blessed (6) |
| Ocean Songster: breathe in water without limit, aquatic animals (dolphins, fish, squids, turtles) follow you and aquatic hostile mobs hesitate, fear no pressure | Ocean Songster (5) |
| Spiritual Intuition: a warning icon while a stronger Beyonder than you is within 32 blocks. Underwater vision. Scale Armor: your Illusory Scales are always faintly up (passive 10% cut) | Cataclysmic Interrer (4) |
| Home Turf: in water or at your own flood you deal +25% damage and take 25% less (stacks with Sea's Favor). You walk the seabed unhindered | Sea King (3) |
| Storm-born: damage with no attacker (lightning, drowning, freezing, fall, explosions) is 60% less | Calamity (2) |
| Authority of Thunder: lightning never hurts you, and each bolt that lands within 8 blocks of you gives +1 Rage (once a second) | Thunder God (1) |
| Tyrant's Aura: hostile mobs of up to 60 maximum health within 8 blocks hesitate (slowed by 25% and half as likely to pick you). Fear cannot touch you | Tyrant (0) |

## How the parts work together

- **Water first, then thunder.** Sweeping Wave, Water Sphere or a Tsunami Soak an enemy; Lightning Strike, Chain Lightning and the Bolt Arrow then do +40% and arc to everything else Soaked.
- **Corrosive Rain, then Lightning Storm** (or Hurricane, which ends in one): rain makes every bolt in the storm an arc.
- **Magnetic Pole Pull, then Lightning Forest** pulls everything into the middle of the strike.
- **Fear, then Punishment.** Roar of the Thunder God leaves a frightened crowd; Heavenly Punishment hits the one that runs.
- **Rage fights.** Siren Song or Discord, a few blows, then Raging Blow. Wrath builds Rage double. At 10 it also pays for Source of Wrath at Calamity (2).
- **Source of Wrath at Calamity (2) is a Thunder God (1) for 20 seconds**, and at Thunder God (1) it is a Tyrant for 20 seconds. That is the Pathway's "near a True Deity through anger".
- **Hurricane and Dreadful Heaven** are the defense, Chasm of Storms the attack; Banish into the Storm and Incarnation of Light are the way out.

## Melee combo (optional)

The registered `MeleeCombo` could have eight hits: Boarding Cut, Anchor Swing, Gale Slash, Squall Sweep, Tide Heave, Thunder Lunge, Rising Storm and Final Strike. It resets after more than 40 ticks without a qualifying hit. From Ocean Songster (5) every hit arcs a little lightning to the nearest enemy (5 damage, Shock rules). Visual only for the silver arcs and wind trails; no extra damage beyond that arc.

## Starting numbers

Costs follow the Attendant Of Mysteries curve (250 at Sequence 9 up to 12000 at Sequence 3). Sequences 2, 1 and 0 continue it by guess. Single keys have a cost only; wheel entries have a cost and a cooldown in seconds. **Cooldowns are kept on every entry** because spirituality refills at once from Sequence 2 and costs cannot limit anyone there (see the balance notes).

| Ability | Cost | Cooldown |
|---|---|---|
| Harpoon, Illusory Scales | 250, 250 | |
| Raging Blow, Wrath | 600, 600 | |
| Source of Wrath | 25000 | |
| Water Veil, Sweeping Wave, Tideheal | 1250, 1250, 1500 | 10, 8, 14 |
| Windblade, Gale Burst, Gale Ride | 900, 2450, 1800 | 3, 12, 20 |
| Water Curtain, Water Sphere, Corrosive Rain | 3000, 4000, 4500 | 15, 20, 25 |
| Wind Binding | 3500 | 15 |
| Lightning Strike, Lightning Bolt Arrow | 3000, 4000 | 6, 8 |
| Siren Song, Discord, Thunderclap | 3000, 3000, 3500 | 18, 20, 14 |
| Windblade Legion, Chain Lightning, Roar | 6000, 6000, 6000 | 12, 10, 12 |
| Death Dirge, Hurricane, Tsunami | 7000, 7000, 8000 | 30, 25, 30 |
| Sea Master, Maelstrom, Destructive Droplet | 12000, 12000, 14000 | 45, 35, 30 |
| Sonic Boom, Cloud Sword, Lightning Storm, Wrath of the Sea | 12000, 12000, 12000, 12000 | 10, 30, 30, 40 |
| Magnetic Pole Pull, Electromagnetic Disruption | 20000, 18000 | 25, 45 |
| Sound of Horror, Requiem, Storm Incarnation | 20000, 18000, 22000 | 45, 30, 60 |
| Heavenly Punishment, Ocean of Lightning, Roar of the Thunder God | 30000, 40000, 30000 | 40, 60, 40 |
| Incarnation of Light, Storm Hell | 30000, 40000 | 45, 120 |
| Lightning Forest, Tyrant's Presence, Starfall | 60000, 45000, 50000 | 90, 60, 90 |
| Chasm of Storms, Dreadful Heaven, Banish into the Storm | 50000, 50000, 25000 | 120, 120, 45 |

## Built from the spell kit

What exists today, so most of the Pathway is a few lines each:

| Entry | Kit parts |
|---|---|
| Harpoon, Wind Binding | `Shot`, `Reel`, `Spells.pull`, `Root` |
| Illusory Scales, Wrath, Source of Wrath | `ConjuredArmor`, `Form` (`.cutsDamage`, `.aftermath`, `.effect`), `Physique`, `Fear.addImmunity` |
| Raging Blow | `Spells.aimEnemy`, `Spells.strike`, `Spells.push`, `Root` |
| Water Veil, Water Sphere, Siren Song, Discord, Requiem | `Silence`, `Slumber`, `Taunt`, `Falter`, `Bleeding`, `Madness`, `Marks` |
| Sweeping Wave, Tsunami, Sonic Boom, Roar | `Spells.wave` + `SpellFx.steadyRing` |
| Tideheal | `Spells.cleanse`, `Cleansing`, potion effects |
| Corrosive Rain, Maelstrom, Ocean of Lightning, Lightning Storm, Death Dirge, Storm Hell | `Zone` (`onInside`, `onPulse`), `GravityField`, `Corrosion` |
| Water Curtain, Cloud Sword | `Wall` |
| Sea Master | `Zone`, `Spells.hostileShotsAround`, `Residue`, `Marionette.spawnSummon` |
| Destructive Droplet, Windblade, Windblade Legion | `Shot`, `Ray`, `Spells.enemiesAround`, `NoHeal` |
| Gale Ride, Incarnation of Light | `Flight`, `Glide`, `Transit.by(...).steered()`, `FallGuard` |
| Gale Burst, Hurricane | `Spells.push`, `Spells.looseThingsAround`, `Hurled`, `Spells.hostileShotsAround`, `Spells.addUnchoosableCheck` |
| Lightning Strike, Bolt Arrow, Chain Lightning | `Ray`, `Contagion`, `WeaponCoat`, `Vulnerable` |
| Magnetic Pole Pull | `Spells.pull`, `Weight`/`Gravity`, `hostileShotsAround` |
| Electromagnetic Disruption, Dance of Thunder | `Connection`, `Dispel`, `Concealment`, `Falter` |
| Heavenly Punishment | `Spells.strikeTrue`, `Concealment`, `Exile`, `Swarm` |
| Starfall | `Transit`, `Concealment`, `SpellFx.runeCircle` (the warning ring), `Zone` |
| Thunderclap, Sound of Horror, Roar of the Thunder God, Tyrant's Presence | `Fear`, `Root`, `Silence`, `Parasite`, `Concealment`, `AbilityLock`, `Spells.enemiesAround` |
| Storm Incarnation, Lightning Forest | `Zone`; the moving tornado (`tornadoTick`) and the falling-spot helpers (`fallingSpot`, `doomFalls`) of `Apocalypse.java` |
| Chasm of Storms, Dreadful Heaven | `Dome`, `Fear`, `Falter`, `AbilityWheel.costBy` / `cooldownBy` (the halved costs and cooldowns) |
| Wrath of the Sea, Banish into the Storm | `Marks`, `Exile` |
| Rage | `HudCounter`, `Marks`, `ScreenImages` |

## New reusable parts worth writing first

In this order, most abilities unlocked first. Each one is general, so other Pathways can use it too.

| Part | What it is | Unlocks |
|---|---|---|
| `Shock` (status/ or combat/) | One helper, `Shock.strike(player, target, damage)`: paralysis with a 3 second immunity, +50% against undead and evil, +40% and the arc against Soaked or metal-armored targets, `DamageFx.credit`. The one place that knows what lightning does. | Every lightning entry (the Thunder wheel, the bolts of the storm zones and kingdoms), the melee arcs, Authority of Thunder |
| `Soaked` (status/, a `Marks` kind) | The mark every water entry puts on enemies and every lightning entry reads. `Soaked.apply(target, ticks)`, `Soaked.isSoaked(entity)` (true also in water, in rain and in your own zones). | Water-then-thunder, Sea's Favor, Home Turf |
| `Rage` (status/) | The counter: add (with the Tipsy double), spend, decay, saved, shown by `HudCounter`, read by +2%-per-point damage. Same shape as Prince's `Orders` and Attendant's stacks. | Raging Blow, Wrath, Source of Wrath, Authority of Thunder |
| Cosmetic bolt (`SpellFx.bolt`) | A zigzag, branching silver-white bolt from a point to a point, thickness and life as parameters, with the sound. Never the vanilla bolt. | Every lightning entry |
| `SequenceBoost` (status/) | "N Sequences stronger for a while" read by the wheel gate and by any Sequence check the abilities make. | Source of Wrath |
| Wet check (`Soaked.isWet`) | One question: in water, in open-sky rain, or inside any Thunder God zone. | Passives, Tideheal doubling, Veil, Strike |
| Storm-spot picker | The falling-spot and doom-falls code in `Apocalypse.java` promoted into `combat/` ("things falling near random enemies in a circle") plus the moving-tornado tick. | Lightning Storm, Forest, Storm Incarnation, Storm Hell |
| Information hook | One check, `Information.blocked(entity)`, that `Connection`, `Prediction`, `Dossier` and `Study` ask before they work, like Prince's `Field.separated` hook in `Spells`. | Electromagnetic Disruption, Dance of Thunder |
| Aura toggle | Prince's Majesty-style timed aura with a start stiffening and an every-second tick. | Tyrant's Presence, Death Dirge, Chasm of Storms, Dreadful Heaven |
| Item-use root | A `RootRestriction` for using Sealed Artifacts (if one does not exist; Sealed Artifacts already respect roots that block item use). | Death Dirge, Sound of Horror |
| Wheel rules | As Prince's, but with the recast at full price and cooldown. | Every wheel |

## Registration (when the code is written)

For a new Pathway, the repo expects: `network/SKeyPressAbilityOneUsed` to `SevenUsed` branching on `"Thunder God"`, `PathwayAbilities.ALL`, `SequenceCommands.PATHWAYS`, `SequencePassives`, `PathwayHudIcon`, `MeleeCombos`, and a row for Thunder God in `docs/SEQUENCES.md` in the "Required Sequence of every ability key" table: 1: 9, 2: 8, 3: 7, 4: 6, 5: 5, 6: 5, 7: 4. Sequences 1 and 0 currently "unlock no new abilities yet" in `SEQUENCES.md`; the wheel entries gate themselves with their own Sequence, so that note changes once this Pathway exists.

## What needs looks, sounds and pictures

Modelled on `PrinceOfAbolitionFx`: one `ThunderGodFx` that only shows things. The big shared ones: the silver bolt and its sound, the rain cloud (with a face for Storm Incarnation), the wave crest, the whirlpool, the water film and sphere, the windblade shimmer, the cloud sword, the sky pillar of Heavenly Punishment, the kingdom shell (two tints), the Rage sigil and the Pathway icon in `PathwayHudIcon`. Wheel icons: 44.

## Left out on purpose

- **Mythical Creature Form** (a giant octopus with thick tentacles and silver-white lightning; partial at Cataclysmic Interrer (4), complete at Calamity (2)): the wiki gives its looks, but it needs a model. A `Form` with `.flies()` and `GiantForm` fits it when one exists.
- **Earthquake** (Cataclysmic Interrer (4), and "earthquakes powerful enough to destroy the Earth" at Tyrant (0)): Apocalypse already has Earthquake, Hailstorm and Tornado. Left to it. If you want it here too, call that helper with a silver flavor (question 5).
- **Snow Storm / extreme cold Calamity** (Calamity (2)): Apocalypse's Hopeless Ice Age covers it.
- **Eavesdrop with the wind, Navigation as a key, hearing prayers, sea creatures as scouts, octopus-ing aquatic life, seas surging to the clouds:** no gameplay, or needs chat access; flavor only.
- **Gust Grab and Desiccation** are parts of Gale Burst and Destructive Droplet, not entries of their own.
- **Spare ideas, dropped so the wheels stay at nine entries or fewer:** Storm Chant (singing that bends local winds and reveals the hidden), Slick (a lubricating, frictionless liquid), Silent Stream (an eroding jet of water), Thunder Cage (an `Enclosure` of lightning bars around one enemy). Any of them fits a wheel later.
- **Matter authority** (accelerating matter to near the speed of light): folded into Starfall.

## Open balance notes

- **From Sequence 2 spirituality refills at once** (`SEQUENCES.md`: "a full bar every tick"), so a cost of 60000 does not limit anyone there. Only cooldowns and conditions do, which is why every entry keeps a cooldown and recasts do not skip them. If you want Prince's cooldown-free top end, the costs must be raised against a pool that does not refill, or the refill rule changed.
- **Sequence 5 is crowded** (nine entries at Ocean Songster). If it plays heavy, move Thunderclap or Discord to Sequence 4.
- **Lightning is strong against Soaked targets** (+40% and arcs). It is the point, but with Rain, Tsunami and the Sea Master all making Soaked it may need a smaller bonus.
- **Source of Wrath** at Thunder God (1) lets you cast the Tyrant's entries. With Chasm of Storms and 60000-cost abilities available, 20 seconds and the Spent aftermath are the main limit.
- **Starfall, Lightning Forest and Chasm of Storms** are area attacks over 40 blocks. The warning rings and cooldowns are the counters.

## Questions for Luka

1. **Wheel convention.** I used Prince's (sneak opens, normal recasts). Want Hand Of God's (normal opens) instead?
2. **Cooldowns.** I kept them on every entry and made recasts full price. Do you want Prince's cheaper, cooldown-free recast instead (3x/2x/1x), or cooldowns gone from Sequence 4?
3. **Friendly fire.** The wiki says Lightning Storm "does not distinguish friend from foe". I made allies take half. Full, half or none?
4. **Weather.** Storm Hell, Storm Incarnation and Downpour only change the sky over their area (visual and local). Do you want them to switch the world's weather as well?
5. **Overlap with Apocalypse.** I left out Earthquake and Snow Storm and made Hurricane an eye around you, not a travelling tornado. OK, or reuse its disasters here?
6. **Mythical Creature Form.** Wanted now (needs a model) or later?
7. **Sea creatures for Sea Master.** Guardians and drowned work anywhere but are weak out of water; want dolphins, squids or a made-up water spirit instead?
8. **Source of Wrath as "one Sequence stronger".** It temporarily unlocks the next Sequence's entries (needs the `SequenceBoost` part). Fine, or just a damage and speed buff?
