# Sequences, Pathways, digestion

The progression system, Lord of the Mysteries style. Acting, potions, ingredients and rituals are **not** implemented yet; see "Extending" at the end.

## The model

| Word | Meaning | Where |
| --- | --- | --- |
| Pathway | The class a player follows, by its exact name: Wanderer, Worm Of Time, Hand Of Order, Attendant Of Mysteries, Frenzied Mage, Pale Emperor, Conqueror, Apocalypse, Dark Angel, Beauty Goddess, Hand Of God, Knight Of Misfortune, Abomination, Prince Of Abolition, Knowledge Emperor, Illuminator, Star Dragon, Key Of Stars (`"none"` until one is set). | `SoulCore.getPathway / setPathway` |
| Sequence | 9 (weakest) down to 0 (strongest), stored directly. Everyone starts at 9. | `SoulCore.getSequence / setSequence` |
| Digestion | 0 to 100 percent of the current Sequence. At 100 the player may run `/sequence advance`. | `SoulCore.getDigestion / setDigestion`, `Digestion` |
| Spirituality | The resource abilities spend (was "soul essence"; same numbers, same costs). | `SoulCore.getSpirituality / setSpirituality`, `Spells.paySpirituality` |
| power level | 1 at Sequence 9 up to 8 at Sequence 2 (see the table below). Used only for scaling (Wanderer star cap), never for gates. | `Beyonder.powerLevel` |
| power scale | The power level capped at 7. | `Beyonder.powerScale` |

All of it is saved in the player's persistent data under the tag `circle_of_inevitability` (keys `pathway`, `sequence`, `digestion`, `spirituality`), which is copied on death (`SoulCore.onPlayerClone`) and saved with the player, so it survives death, respawn and relog. The client gets Pathway, Sequence and digestion (whole percent) through `CSyncPathwayPacket`, sent by `PathwaySync` whenever one of them changes.

Ability gates read naturally: `if (!Beyonder.hasSequence(player, 6)) return;` means "needs Sequence 6 or stronger".

## The numbers of every Sequence

The [Omniscient Eye implementation](OMNISCIENT_EYE.md) provides Reading/Ritual Circle (9),
Deduction/Keen Eye (8), Reconstruction/Deductive Strike (7), stored Replication and Analyze (6),
Mirror Curse/Vile Thoughts/Improvise/Living Tome (5), and Golden Armor Art (4). Replication learns
only fully studied powers at the observer's Sequence or weaker. Fate and Cognizing (key 6, from 4)
and Omniscience (key 7, from 2) include the remaining abilities through White Tower (0).

One table in `Beyonder` (`SEQUENCES`, one row per Sequence) holds them. To rebalance a Sequence, change its row there and here.

| Sequence | Power level | Power scale | Max spirituality | Regen per tick |
| --- | --- | --- | --- | --- |
| 9 | 1 | 1 | 20 | 0.14 |
| 8 | 2 | 2 | 400 | 1.33 |
| 7 | 3 | 3 | 1980 | 5.4 |
| 6 | 4 | 4 | 6400 | 16 |
| 5 | 5 | 5 | 15500 | 41.7 |
| 4 | 6 | 6 | 32400 | 108 |
| 3 | 7 | 7 | 59780 | 343 |
| 2 | 8 | 7 | 78400 | refills at once |
| 1 | 8 | 7 | 78400 | refills at once |
| 0 | 8 | 7 | 78400 | refills at once |

Sequences 1 and 0 have no numbers of their own yet and repeat Sequence 2; they also unlock no new abilities yet. "Refills at once" is a full bar every tick.

Where the numbers come from: the old ascension stage ran 1..8. Sequence 9 has the numbers of old stage 1, Sequence 8 of stage 2, down to Sequence 2 with stage 8 (changed on 2026-10-11; before that Sequence 9 and 8 shared the stage 1 numbers). The ability gates did not move with them: a gate `stage >= N` is still `Beyonder.hasSequence(player, 9 - N)`.

Note: most abilities cost 250 or more, so at Sequence 9 (maximum 20) they cannot be paid for. Sequence 8 (maximum 400) is the first that can.

## Required Sequence of every ability key

Keys: G = ability 1, H = 2, J = 3, K = 4, L = 5, N = 6, M = 7 (default bindings).

| Pathway | G (1) | H (2) | J (3) | K (4) | L (5) | N (6) | M (7) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wanderer | Star Burst 9 | Dash 8 | Buff 7 | Seal 7 | Transport 7 | Orbital 5 | Cosmic Plague 3 |
| Key Of Stars | Door Opening 9 (sneak: Slip Free) | Tricks 8 (wheel: Flash, Black Screen, Loud Noise, Frost Ray, Gale, Tumble, Escape Trick, all at 8) | Star Divination 7 (sneak: Interference) | Record 6 (sneak: Replay) | Travel 5 (wheel: Traveler's Door, Blink, Invisible Hand, Star Anchor at 5, Wandering 3) | Secrets and Space 4 (wheel: Space Concealment, Transfiguration, Exile, Pocket Space at 4, Space Tearing 3) | Starlight 2 (wheel: Toy Box, Replication at 2, Space Fragmentation, Starlight Seal, Space-Time Labyrinth at 1, Alternate World, Omnipresence, Close the Door at 0) |
| Worm Of Time | Marauder Theft 9 (Life/Distance Theft 4) | Power Theft 6 | Parasite 4 | Observation 9 / Decryption 7 | Thought and Deceit 8 | Parasite and Destiny 4 | Time and Error 2 |
| Hand Of Order | Halt 9 (sneak: Presence) | Jurisdiction 8 (sneak: Recognition) | Interrogation 7 (wheel: Psychic Lashing, Psychic Piercing, Whip of Pain, all at 7) | Verdict 6 (wheel: Imprison, Exile, Death, Maintain Secrecy at 6, Deprivation, Execution at 4) | Decree 6 (wheel: Prohibition: Passage, Prohibition: Concealment at 6, Imperative: Reality, Contract at 4) | Punishment 5 (sneak: Sword of Judgment at 3) | Order 2 (wheel: Balance, Divide the Battlefield, Law: No Return at 2, Order Proxy, Quell Disorder at 1, Court of Judgment at 0) |
| Attendant Of Mysteries | Paper Dagger 9 | Flaming Jump 8 | Damage Transfer 7 | Air Cannon 6 | Countdown (Spirit Body Threads) 5 | Historical Void 4 | Grafting 3 |
| Frenzied Mage | Spear 9 | Flame 8 | Frost 7 | Hammer 6 | Meteor 5 | Entropy 4 | Abolition 3 |
| Pale Emperor | Root 9 | Door 8 | Serpent 7 | Spear Skull 7 | Spikes 6 | Wings 5 | Wraiths 4 |
| Conqueror | Hunter's Trap 9 | Provocation 8 | Fire Ravens 7 | Flame Transformation 6 | Cull 5 | Galvanization 4 | Fog of War 3 |
| Apocalypse | Fatal Step 9 | Black Flame 8 | Mirror World 7 | Silk Weaver 6 (wheel: silks at 6, 5, 4, 3) | Plague 5 | Unaging 4 | Catastrophe 3 (wheel: disasters at 3, 2, 1) |
| Dark Angel | Divination 9 | Whispers 8 | Shadow Binding 7 | Flesh Bomb 6 | Grazing 5 (7 slots of stolen abilities) | Cull of Spiritual Flesh 4 | Degeneration 3 |
| Beauty Goddess | Medicine Concoction 9 | Taming 8 | Wings of Darkness 7 | Potions 6 (wheel: potions at 6, 6, 5, 4) | Lunar Battlefield 5 | Gaze of Darkness 4 | Moon 3 (wheel: Door of Summoning 3, Creation 2, Beauty 1, Blood Moon 0) |
| Hand Of God | Warrior's Charge 9 | Gladiator's Flurry 8 | Grandmaster's Edge 7 | Dawn 6 (wheel: Light of Dawn, Dawn Armor, Sword of Dawn, Hurricane of Light, all at 6) | Protection 5 | Demon Hunting 4 (wheel: Eye, Alchemy, Mind Concealment, Giant Form, all at 4) | Silver and Twilight 3 (wheel: Mercury 3, Silver Rapier 3, Light Concealment 3, Twilight Sword 2, Divine Right Hand 1, Passage of Time 0, Decay 0, Divine Kingdom 0) |
| Abomination | Blood Chain 9 | Moonstruck 8 | Rending Pounce 7 | Zombie 6 (wheel: Raise the Dead, Grave Chill, Undying Flesh, Death Sentence, all at 6) | Wraith 5 (wheel: Mirror Step, Possession, Ghost Host, all at 5) | Chains 4 (wheel: Animate Objects 4, Grave Shackles 4, Thorned Mausoleum 0, Coffin of Ages 0, Thorn Garden 0) | Curses 3 (wheel: Curse by Connection 3, Cursed Artifact 3, Rite of Silence 3, Bane Mist 2, Ancient Blight 2, Abomination Form 1) |
| Knight Of Misfortune | Night Sight 9 | Midnight Poem 8 (wheel: Tranquilize, Lullaby, Pacify, Fear at 8, Agitating 6) | Nightmare 7 | Requiem 6 | Spirit Commanding 5 (wheel: Terror Banshee, Earthen Spirit, Gale Spirit at 5, Command Spirits 1) | Nightwatcher 4 (wheel: Night Domain, Hair Entanglement, Curse of Misfortune, Demonic Wolf Form at 4) | Darkness and Concealment 3 (wheel: Sword of Darkness, Nightmare World at 3, Concealed State, Secret Concealment, Concealed World at 2, Mantle of Darkness 1, Eternal Rest, Destination of All Things 0) |
| Knowledge Emperor | Mystery Prying 9 (sneak: Dowsing) | Weak-Point Strike 8 (sneak: Sidestep) | Hex 7 (sneak: Spellbolts; Knowledge Orbs from 2) | Read Scroll 6 (sneak: Inscribe Scroll) | Stars 5 (wheel: Starlight Cage, Stellar Self, Star Fall, Star Guard, all at 5) | Retellings and Prophecy 4 (wheel: Sleeping Beauty, Avalon, Achilles' Heel at 4, Pandora's Box, Foresight, Prophecy of Ruin at 3) | Knowledge 2 (wheel: Informatization, Erase at 2, Knowledge Overload, Lance of Knowledge, Condense Knowledge at 1, Embodiment of Knowledge, Indoctrination, Divine Kingdom at 0) |
| Illuminator | Compound Flask 9 (sneak: Recall) | Survey 8 (sneak: Ritual Circle) | Appraise 7 (sneak: Exploit Flaw) | Workshop 6 (wheel: Clockwork Rifle, Spirit Cannon, Mystic Brooch, all at 6) | Astronomy 5 (wheel: Star of Curses, Blessing of Stars, False Constellations, Meteor Shower, all at 5) | Alchemy and Arcana 4 (wheel: Living Doll, Golem, Mechanical Body at 4, Dissociation, Knowledge Spirit at 3) | Civilization 2 (wheel: Rewrite Principles, Colossus at 2, Scrolls of Civilization, Mathematical Maze, Masterwork at 1, Reality Knowledge, Illusory Book, Mechanical Heaven at 0) |
| Star Dragon | Star Chart 9 (sneak: Fate Glimpse 8) | Star Sacrifice 7 (wheel: Gravity Abnormality, Electromagnetic Attraction, Cosmic Void, Weakening Ray at 7, Sunlight Ray at 5) | Void Passage 6 (sneak: Piercing Ray) | Tidal Surge 5 (sneak: Scaled Form; the Heavy Body from 4) | Heavybringing 4 (wheel: Heavybringing, Fate Convergence, Heavy Sphere, Series of Rays, Remote Sight, all at 4) | Star Grazing 3 (sneak: Fission Pulse) | Cosmos 2 (wheel: Fusion Cluster, Radiant Angel Form at 2, Gravitational Time Dilation, Planetfall, Star Dragon Form at 1, Neutron Star, Singularity, Quantum Uncertainty at 0) |
| Prince Of Abolition | Verdict 9 (sneak: Brainwash) | Brute Force 8 (sneak: Iron Will) | Bribe 7 (wheel: Weaken, Charm, Arrogance, Connect, Coin Shot, all at 7) | Distort 6 (sneak: Seal; a gate where you look from 4; flips an enemy's shell from 2) | Paces 5 (sneak: Disorder) | The Fallen 8 (wheel: Raise Mausoleum 8, Corrosion 6, Step 6, Majesty 5, Bestowment 4, Exploit 4, Magnify 4, Mausoleum Salvo 4, Mausoleum Blast 4) | Sovereign 6 (wheel: Ruin 6, Frenzy 3, Entropy 2, Abolition 1, Exile 1, Inversion 0, Coronation 0, Decree 0) |

The number is the weakest Sequence that can use it (that Sequence and every stronger one). Beauty Goddess's two wheels (K, M): sneak recasts the last pick with no cooldown, for twice its cost; see [BEAUTY_GODDESS.md](BEAUTY_GODDESS.md). Hand Of God's three wheels (K, N, M) work the same way; its Protection (L) becomes a Cage at Sequence 2 and gains more at Sequence 1; see [HAND_OF_GOD.md](HAND_OF_GOD.md). So do Knight Of Misfortune's four wheels (H, L, N, M); its Nightmare (J) becomes Dream Invasion while the soul is out of the body; see [KNIGHT_OF_MISFORTUNE.md](KNIGHT_OF_MISFORTUNE.md). Abomination's four wheels (K, L, N, M) work the same way; a Possession is left with the Wraith key (L) again, and Abomination Form is ended by picking it again (free); see [ABOMINATION.md](ABOMINATION.md). The Prince Of Abolition's three wheels (J, N, M) are the other way round: sneak opens the wheel and a plain press recasts the last pick with no cooldown, for 3 times its cost at Sequences 9 to 7, twice at 6 and once from 5; from Sequence 4 no entry has a cooldown at all; see [PRINCE_OF_ABOLITION.md](PRINCE_OF_ABOLITION.md). Knowledge Emperor's three wheels (L, N, M) work like Hand Of God's; every cast of them and of its keys can be made free by the Whisper of the Hidden Sage or the Law of Three, costs half in its Divine Kingdom, and an Indoctrination shortens the wheels' cooldowns; see [KNOWLEDGE_EMPEROR.md](KNOWLEDGE_EMPEROR.md). The Hand Of Order's four wheels (J, K, L, M) work like Hand Of God's (picking the Court of Judgment again while it sits adjourns it, free); its single keys (G, H, N) have no cooldown and its wheels' sneak recast costs 3 times as much, and every cast costs 20% less while it stands in its own Jurisdiction; see [HAND_OF_ORDER.md](HAND_OF_ORDER.md). The Illuminator's four wheels (K, L, N, M) work like Hand Of God's too (picking the Mechanical Body, the Colossus or Mechanical Heaven again while it lasts is free: off, the Fusion Beam, or the end of the Kingdom); its single keys (G, H, J) have no cooldown and its wheels' sneak recast costs 3 times as much (the Fusion Beam keeps its own), every cast costs 10% less when the same ability was cast within the last 20 seconds (Quick Study), and Workshop casts from inside its own Ritual Circle cost 20% less and last 50% longer; see [ILLUMINATOR.md](ILLUMINATOR.md). The Star Dragon's three wheels (H, L, M) work like Hand Of God's too; its single keys (G, J, K, N) have no cooldown and its wheels' sneak recast costs 3 times as much, Scaled Form and the two Mythical Creature Forms are ended by casting them again (free), the Sunlight Ray costs half in daylight under the open sky, and the Condenser's Fusion Cluster costs half; see [STAR_DRAGON.md](STAR_DRAGON.md).

A stolen ability is the exception: Worm Of Time's Theft (H) casts what it stole whatever its Sequence, at no cost (`Beyonder.castUnbound`; see "Another Pathway's abilities" in [SPELL_KIT.md](SPELL_KIT.md)). The same table is in code as `PathwayAbilities.ALL`; a new ability gets a line in both.

Worm Of Time's Parasite (J) is the only key that works while its player is inside a host: J again puts them back out (`status/Parasite`; see "Parasite" in [SPELL_KIT.md](SPELL_KIT.md)). Inside a mob in deep parasitism, the walk keys, jump, sprint, mouse and both clicks steer it instead (`status/ParasiteControl`).

Worm Of Time's Theft (H) holds `THEFT_MAX_HELD` (5) stolen abilities at once and releases the newest first (sneak + H). From Sequence 6 it steals from players with a Pathway and from Beyonder mobs only: one of their Pathway's abilities, up to their Sequence (up to three distinct powers per attempt from Sequence 3) ([BEYONDER_MOBS.md](BEYONDER_MOBS.md)).

Worm Of Time's complete key map, wheels, passive progression, costs and gameplay adaptations are in
[WORM_OF_TIME.md](WORM_OF_TIME.md). Sneak + J enters initial parasitism (hiding and healing);
normal J enters deep parasitism (drain and mob steering). Both leave with J again.

Dark Angel's Grazing (L) keeps an ability from every Beyonder its owner kills (a player with a Pathway, or a Beyonder mob: one of its Pathway's abilities, up to its Sequence; [BEYONDER_MOBS.md](BEYONDER_MOBS.md)), in 7 slots that are kept for good (through death too). It casts the selected one whatever its Sequence, for three times that ability's own cost (`Beyonder.castUnbound(player, 3f, ...)`). Sneak + L picks the slot; by a soul that waits on its corpse (all slots full), it picks the slot that soul replaces.

## Renaming a Pathway

A player's Pathway is saved as its name, so a new name has to be the same text in every place that checks it:

| Where | What |
| --- | --- |
| The Pathway's class, `PATHWAY` near the top (e.g. `DarkAngel.java`) | the name its abilities check |
| `SequenceCommands.PATHWAYS` | the names `/sequence pathway` suggests (tab completion) |
| `PathwayAbilities.ALL` | the first text of each of its seven lines (theft, Grazing) |
| `SequencePassives` | any `effect("...")` / `custom("...")` line of that Pathway |
| `PathwayHudIcon` | its icon, if it has one |
| `DarkAngel.pathwayColor` | the color of its souls for Grazing |
| this file | the tables above |

Players who already have the old name keep it until `/sequence pathway <player> "<new name>"` sets the new one. The class and method names (`hangedManDivination`, `HangedManFx`) can stay as they are; only the text matters.

## Passives

What a Beyonder always has from a Sequence on (that Sequence and every stronger one), with no key press and no spirituality cost. All of them are one list at the top of `blasphemy_slate/beyonder/SequencePassives.java`.

| Passive | Who | From Sequence |
| --- | --- | --- |
| Saturation I (never hungry; vanilla also heals quickly while saturated) | every Pathway | 4 |
| Poison does not take (removed at every check) | Beauty Goddess | 9 |
| Strength I | Beauty Goddess | 8 |
| Night Vision, and half a heart back every second while hurt | Beauty Goddess | 7 |
| Its own potions and medicine last 1.5 times as long (in `BeautyGoddess`, `POTION_MASTERY`) | Beauty Goddess | 6 |
| Speed II while it is dark outside (the last one given runs out up to 30 s into the day); afterimages of moonlight while sprinting (in `BeautyGoddess`) | Beauty Goddess | 5 |
| Regeneration I | Beauty Goddess | 4 |
| A mob hunting you loses you now and then (30% each second) | Beauty Goddess | 1 |
| Strength I | Hand Of God | 9 |
| Supernatural damage (magic, wither, explosions, lightning, freezing) 25% less; blindness and nausea do not take (`status/SupernaturalResistance`) | Hand Of God | 8 |
| Weapon Mastery: attacks 10% faster, half a block more reach (in `HandOfGod`, `beyonder/Physique`) | Hand Of God | 7 |
| Giant's Physique: 15% taller, two more hearts (in `HandOfGod`) | Hand Of God | 6 |
| Resistance I; half knockback (in `HandOfGod`) | Hand Of God | 5 |
| Tracking: enemies you hit leave footprints only you see for 20 s; Spiritual Intuition: a chime only you hear when a mob starts hunting you out of sight (in `HandOfGod`) | Hand Of God | 4 |
| Dawn Aura: evil and undead within 6 blocks take 1.5 damage each second | Hand Of God | 3 |
| Twilight Resistance: wither does not take, nothing stops your healing, you cannot rot (`status/Decay`) | Hand Of God | 2 |
| Divine Touch: bare-handed blows do 6 more and the target cannot heal for 3 s (in `HandOfGod`) | Hand Of God | 1 |
| Twilight Aura: enemies within 16 blocks are slowed and weakened and cannot heal | Hand Of God | 0 |
| Night Vision; Nocturnality: in full dark (night, caves) Strength I, Speed I and half a heart back each second (in `KnightOfMisfortune`, `nocturnality`) | Knight Of Misfortune | 9 |
| A chime only you hear when a mob starts hunting you unseen or in the dark (in `KnightOfMisfortune`) | Knight Of Misfortune | 9 |
| Steady Hand: falls shorter than 6 blocks do not hurt; bows and crossbows draw 25% faster (in `KnightOfMisfortune`) | Knight Of Misfortune | 8 |
| Resonance: poems give back a quarter of their price and can be cast while silenced; spirituality comes back 25% faster (in `KnightOfMisfortune` and `KnightOfMisfortunePoems`) | Knight Of Misfortune | 7 |
| Spirit Sight: spirits, summons, the undead and sleepers within 16 blocks are marked for you alone | Knight Of Misfortune | 6 |
| Spirit Teeth: four spirits out at once (eight from Sequence 4) (in `KnightOfMisfortuneSpirits`) | Knight Of Misfortune | 5 |
| Deep Night: Strength II and Regeneration I in full dark, Weakness I in bright day sun; Veil: still in the dark for 3 s, you are `Unnoticed` | Knight Of Misfortune | 4 |
| Horror Aura: weaker enemies within 8 blocks (4 sneaking) tremble: weak mobs flee, weaker players are slowed, sick when they look at you | Knight Of Misfortune | 3 |
| Presence of Concealment: no ability can pick you in the dark or while you sneak; enemies within 10 blocks are always slowed and cannot sprint (in `KnightOfMisfortune`) | Knight Of Misfortune | 2 |
| Archangel Body: six more hearts, Strength I, Speed I; Ill Omen: whoever hurts you has a 25% chance to carry a level of Misfortune; Misfortune does not take on you (in `KnightOfMisfortune`) | Knight Of Misfortune | 1 |
| Eternal Slumber: sleep, fear and taunts do not take, supernatural damage halved; Horror: every 6 s whoever looks at you within 24 blocks flinches; Ill Omen always, as Catastrophe | Knight Of Misfortune | 0 |
| Strength I | Abomination | 9 |
| Lunatic Mind: fear and nausea do not take; Full Moon: on a full-moon night under the open sky Strength II and Speed I, Werewolf Form's madness fills twice as fast, Restraint cuts only 35% and Lunar Frenzy's crash lasts twice as long (in `Abomination`) | Abomination | 8 |
| Keen Senses: Speed I, Night Vision, and a sound only you hear when a mob starts hunting you out of sight (in `Abomination`) | Abomination | 7 |
| Unliving Body: Regeneration I; poison, hunger and freezing do not take; projectiles do 25% less (in `Abomination`) | Abomination | 6 |
| Weightless: no fall damage; spirits, ghosts and summons within 16 blocks are marked for you alone (in `Abomination`) | Abomination | 5 |
| Puppeteer: what you summon or take over lasts 1.5 times as long (in `Abomination`, `servantTicks`) | Abomination | 4 |
| Whispering Curse: whoever hits you in melee has a 15% chance to be cursed (Misfortune I); a hush does not stop your abilities (in `Abomination`) | Abomination | 3 |
| Bane Aura: enemies within 10 blocks carry Misfortune I; Misfortune, fear and decay do not take on you (`Abomination.baneAura`) | Abomination | 2 |
| Collection of Curses: the curses you lay last 1.5 times as long; a melee attacker is cursed 25% of the time (in `Abomination`) | Abomination | 1 |
| Chained Aura: enemies within 16 blocks are slowed and cannot teleport; a melee attacker takes 4 thorn damage (`Abomination.chainedAura`) | Abomination | 0 |
| Sequence Might: 5% more attack, defense, range, area and duration for every Sequence reached (5% at 9, 50% at 0), and 5% more attack and defense for every standing mausoleum; Law & Order: 2% less damage taken for every standing order (in `PrinceOfAbolition`) | Prince Of Abolition | 9 |
| Silver Tongue: Hero of the Village (better prices) | Prince Of Abolition | 9 |
| Physical Enhancement: Strength I and two more hearts; Mental Resistance: mind effects of this Pathway last half as long on you (hearts in `PrinceOfAbolition`, the rest in `PrinceOfAbolitionMarks`) | Prince Of Abolition | 8 |
| Everyone Has A Price: piglins never turn on you; Sanctuary round your mausoleums (in `PrinceOfAbolition`, `PrinceOfAbolitionMausoleums`) | Prince Of Abolition | 7 |
| Weakness Detection: a gold keyhole over the enemy you look at, for you alone; the first hit on a mob that was not after you is a crit (in `PrinceOfAbolition`) | Prince Of Abolition | 6 |
| Presence: weak hostile mobs (30 health or less) within 6 blocks are slowed and half as likely to pick you (`PrinceOfAbolition.presence`) | Prince Of Abolition | 5 |
| Favorable Ground: hidden or invisible enemies within 16 blocks cannot stay hidden near you, and the invisible ones glow (in `PrinceOfAbolition`) | Prince Of Abolition | 4 |
| Loose Order: projectiles coming at you drift up to 10 degrees off (in `PrinceOfAbolition`) | Prince Of Abolition | 2 |
| Beyond the World: damage with no attacker (lightning, explosions, magic, fire) is half lost (in `PrinceOfAbolition`) | Prince Of Abolition | 1 |
| Their Order: everything you leave standing lasts 25% longer, mausoleums too (`PrinceOfAbolition.duration`); Last Rebirth: with no mausoleum left but 3 orders standing, a killing blow puts you out of the world for 10 seconds and you rise where you fell, once every 10 minutes (`PrinceOfAbolitionMausoleums`) | Prince Of Abolition | 0 |
| Spiritual Intuition: a chime only you hear when a mob starts hunting you out of sight, and hidden things within 6 blocks show faintly to you; Whisper of the Hidden Sage: every 4 to 6 minutes your next ability costs nothing (`KnowledgeEmperor.mysteryPryerSenses`, the chime in `KnowledgeEmperor`) | Knowledge Emperor | 9 |
| Fighting Knowledge: attacks 10% faster, half a block more reach, every melee hit adds 5% Study (in `KnowledgeEmperor`, `beyonder/Physique`, `status/Study`) | Knowledge Emperor | 8 |
| Warlock's Mind: harmful effects put on you last a quarter less (in `KnowledgeEmperor`) | Knowledge Emperor | 7 |
| Starlight: at night under open sky, spirituality comes back 25% faster (`KnowledgeEmperor.starlight`) | Knowledge Emperor | 5 |
| Information Skin: damage with no attacker 25% less, knockback 25% less (`KnowledgeEmperor.informationSkin`, `status/SupernaturalResistance`, `beyonder/Physique`) | Knowledge Emperor | 4 |
| Clairaudience: a concealed enemy within 32 blocks that moves or strikes chimes from where it is, for you alone (`KnowledgeEmperor.clairaudience`) | Knowledge Emperor | 3 |
| Sage's Mind: nausea, blindness and darkness do not take; nobody can build Study on you (in `KnowledgeEmperor`) | Knowledge Emperor | 2 |
| Law of Three: the third cast in a row, each within 20 s of the last, is free and your hits do half again as much for 3 s (`KnowledgeEmperor.costFor`, `afterCast`) | Knowledge Emperor | 1 |
| Embodiment: enemies within 24 blocks gain 2% Study a second; your Study fades half as fast (`KnowledgeEmperor.hermitPresence`) | Knowledge Emperor | 0 |
| Quick Study: an ability cast again within 20 s of the last time costs 10% less (`Illuminator.costFor`) | Illuminator | 9 |
| Field Physique: two more hearts; falls, drowning, cacti and walls hurt a quarter less (in `Illuminator`) | Illuminator | 8 |
| Appraiser's Eye: the name and health of the enemy you look at (24 blocks) by the hotbar, in its weakness's color once Appraised (in `Illuminator`, `combat/HudCounter`) | Illuminator | 7 |
| Craftsman's Hands: the Workshop wheel's cooldowns 15% shorter, what it makes lasts 25% longer (`IlluminatorWorkshop`, `Illuminator.lasting`) | Illuminator | 6 |
| Starlight: at night under open sky, spirituality comes back 20% faster and you see in the dark (`Illuminator.starlight`) | Illuminator | 5 |
| Constructor's Bond: your constructs within 16 blocks heal 1% a second and are traced for you (`Illuminator.constructorsBond`) | Illuminator | 4 |
| Absorption: every enemy within 6 blocks loses 1 a second and you heal half of it, 2 a second at most (`Illuminator.absorption`) | Illuminator | 3 |
| Analysis: the concealed, unnoticed and invisible within 16 blocks are dragged into the open (`status/Exposed`) and marked for you, as are those with a Substitute ready (`Illuminator.analysis`) | Illuminator | 2 |
| Enlightenment Aura: allies within 16 blocks (you too) spend 10% less on every ability, and nausea and blindness on them last half as long (`Illuminator.auraCost`, `enlightenmentAura`, the effect event in `Illuminator`) | Illuminator | 1 |
| Reality Knowledge: your Appraised marks last twice as long; nobody can Appraise you, lock your abilities or build Study on you (in `Illuminator`) | Illuminator | 0 |
| Physical Enhancement: Strength I | Star Dragon | 9 |
| Stargazing: standing still for 3 s under the open sky brings spirituality back 30% faster, twice that at night (`StarDragon.stargazing`) | Star Dragon | 9 |
| Spiritual Intuition: a chime and a ring on the ground, for you alone, when a mob starts hunting you from out of sight (in `StarDragon`) | Star Dragon | 8 |
| Star's Embrace: two more hearts and half fall damage (in `StarDragon`) | Star Dragon | 7 |
| Sense of Passages: the concealed, unnoticed and invisible within 12 blocks are outlined for you and cannot stay unnoticed (`StarDragon.senseOfPassages`) | Star Dragon | 6 |
| Starlight Scales: Resistance I, half knockback, fast swimming in water (`StarDragon.starlightScales`, its footing in `StarDragon`) | Star Dragon | 5 |
| Dense Body: nothing pulls, pushes or lifts you (`Spells.addMoveImmunity`), no knockback, no fall damage (in `StarDragon`) | Star Dragon | 4 |
| Fission Touch: a blow you strike arcs on to the nearest other enemy within 4 blocks for a quarter of it (in `StarDragon`) | Star Dragon | 3 |
| Radiance: enemies within 8 blocks are burned 4 a second, twice the undead; only a glow is drawn (`StarDragon.radiance`) | Star Dragon | 2 |
| Gravity Authority: no Heaviness takes on you (`Weight.addImmunity`), and projectiles of the other side within 6 blocks fly at half speed (in `StarDragon`) | Star Dragon | 1 |
| Weight and Density: nothing roots you in place or freezes you (`Root.addImmunity`, `Freeze.addImmunity`), and your steps shake the ground (in `StarDragon`) | Star Dragon | 0 |

To add one, add a line to that list (and a row here):

```java
effect(4, MobEffects.SATURATION);                           // every Pathway, from Sequence 4, level I
effect(6, MobEffects.NIGHT_VISION, 1);                      // the last number is the level: 0 = I, 1 = II
effect("Pale Emperor", 5, MobEffects.FIRE_RESISTANCE, 0);   // one Pathway only (the exact Pathway name)
custom(3, player -> player.clearFire());                    // anything that is not a potion effect
custom("Wanderer", 2, player -> { ... });               // the same for one Pathway
```

How they behave:
- Checked once a second (`CHECK_EVERY_TICKS`) on the server for every living player. A `custom` action runs at every check while the player has the Sequence, so write it so that running it again changes nothing.
- An effect is given for `EFFECT_TICKS` (30 seconds) and given again when half is left, with no particles; `SHOW_ICON` decides whether its icon shows. Milk, death or an ability that strips effects only removes it until the next check.
- A stronger or longer effect of the same kind from somewhere else (a potion, an ability) is left alone.
- When the player no longer has the Sequence or the Pathway (only commands can do that), the effect runs out on its own within `EFFECT_TICKS`. A `custom` action just stops, so use it for things that need no undoing.
- A misspelt Pathway name stops the game with a message that lists the valid names, instead of a passive that silently never works.

## Digestion

Digestion rises from three sources, all in `blasphemy_slate/beyonder/DigestionRates.java`:

| Constant | Default | Meaning |
| --- | --- | --- |
| `SPEED` | 1.0 | Multiplies every rate. 2.0 halves the time, 0.5 doubles it. |
| `PERCENT_PER_MINUTE_PLAYED` | 0.9 | Time online, given every `TIME_STEP_TICKS` (20 = each second). |
| `PERCENT_PER_FULL_BAR_SPENT` | 0.9 | Spirituality spent on abilities, proportional to the cost as a share of the maximum. |
| `MAX_PERCENT_PER_SPEND` | 0.5 | Cap for a single spend. |
| `PERCENT_PER_HOSTILE_KILL` | 0.25 | Killing a mob that is an `Enemy` (zombies, skeletons...). |

Target: a full bar in about 45 minutes of normal active play, roughly 40% from time (0.9 x 45), 40% from spending about one full bar of spirituality a minute, 20% from a kill every 30 seconds. To tune, change `SPEED` first; change a single source to shift the balance. Any decrease of spirituality through `SoulCore.setSpirituality` counts as spending; commands and Sequence changes use `setSpiritualityQuietly` and give nothing.

## Advancing

`/sequence advance` (anyone, for themselves): at 100 percent the Sequence goes down by one, digestion resets to 0, the maximum spirituality follows the new Sequence, with a chat message and the level-up sound. Refused at Sequence 0 ("You are already at Sequence 0...") and below 100 percent ("Your current Sequence is only P% digested..."). There is no loss of control.

## Beyonder Characteristics (the item)

One item, `circle_of_inevitability:beyonder_characteristic`, is every Pathway and every Sequence: which one it is lives in its data component `circle_of_inevitability:characteristic` (`item/BeyonderCharacteristic`: `pathway`, `sequence`). Right-click consumes it. The creative tab "Beyonder Characteristics" has one of every Pathway, Sequence 9 to 0. In code, `BeyonderCharacteristicItem.of(pathway, sequence)` makes a named stack. By command (the name and description are added within a second once it is in a player's inventory, `item/CharacteristicLooks`):

```
/give @s circle_of_inevitability:beyonder_characteristic[circle_of_inevitability:characteristic={pathway:"Wanderer",sequence:5}]
```

What consuming one does (`blasphemy_slate/beyonder/CharacteristicConsumption`):

| The player | The characteristic | Result |
| --- | --- | --- |
| No Pathway | Sequence 9, any Pathway | Starts on that Pathway at Sequence 9. Safe. |
| Their Pathway, Sequence N | Sequence N - 1 | Advances (as `/sequence advance`), only at 100% digestion. Below that: refused, the item is kept. |
| Their Pathway, Sequence N | Sequence N or weaker | Refused, the item is kept. |
| Their Pathway (or none), Sequence N | Stronger than N - 1 | A gamble: `JUMP_CHANCE_PER_SKIPPED` (0.5) for every Sequence skipped. 5 -> 3: 50%, 5 -> 2: 25%. No Pathway counts as Sequence 10, so a Sequence 8 is a 50% gamble. |
| Another Pathway, Sequence N | Any | A gamble: `PATHWAY_SWITCH_CHANCE` (0.2) when it is Sequence N, times `JUMP_CHANCE_PER_SKIPPED` (0.5) for every Sequence it is apart, up or down. At 5: a 5 is 20%, a 4 or 6 is 10%, a 3 or 7 is 5%. On success: the new Pathway, at the stronger of the two Sequences. |

Every gamble uses the item up. Won: the new Pathway and Sequence, digestion at 0. Lost: the player dies and keeps their Pathway, Sequence and digestion (they are copied on death). The gambles ignore digestion; only the safe advance needs 100%. Both chances are constants at the top of `CharacteristicConsumption`.

## Commands

| Command | Who | Does |
| --- | --- | --- |
| `/sequence info` | everyone | Your Pathway, Sequence, digestion and spirituality (current of maximum). |
| `/sequence advance` | everyone | Advance yourself (see above). |
| `/sequence pathway <player> "<Pathway name>"` | op (level 2) | Set the Pathway. Tab completes every Pathway name (quotes needed). `"none"` clears it. |
| `/sequence set <player> <0-9>` | op | Set the Sequence. |
| `/sequence digestion <player> <0-100>` | op | Set digestion. |
| `/sequence spirituality <player> <amount>` | op | Set spirituality (clamped to the maximum). |
| `/soul corruption <player> [0-100]` | op | Show or set corruption (unchanged). |

The old `/soul get, setAspect, setstage, settier, setshards, addshards, addSoulEssence` are gone.

## Extending with acting or potions later

`AdvancementRequirement` is the extension point; `Digestion.requirement` is the one `/sequence advance` checks (default `AdvancementRequirement.DIGESTED`: digestion at 100 percent). Look for `TODO(acting/potions)`. To add a potion step, write a requirement (for example "drank the potion of the next Sequence") and combine it:

```java
Digestion.requirement = AdvancementRequirement.DIGESTED.and(new DrankPotionRequirement());
```

Acting would add a fourth digestion source: call `Digestion.add(player, percent)` from wherever an acting rule is followed, with its rate in `DigestionRates`.

## Snake Of Mercury

All seven player keys are implemented; native geometry/duration uses the material
Wheel, which obeys and spends a signed luck reserve.

| Key | Sequence | Normal / sneak |
| --- | --- | --- |
| 1 (G) | 9 | Danger Premonition / Spirit Vision |
| 2 (H) | 8 | Calculation / Machine Mind |
| 3 (J) | 7 | Signed Bestow Luck menu / Spin the Wheel |
| 4 (K) | 6 | Calamity Redirection / Calamity Attraction |
| 5 (L) | 5 | Misfortune wheel / last pick |
| 6 (N) | 3 | Chaos and Prophecy wheel / last pick |
| 7 (M) | 1 | Cycle wheel / last pick |

Misfortune Mage (4) unlocks Chain/Form and removes Spin's cooldown; Soothsayer (2)
adds prophecy entries; Wheel of Fortune (0) adds the capstone and Fate Authority.
Full controls, costs, signed transfers, passives, limitations and checks:
[SNAKE_OF_MERCURY.md](SNAKE_OF_MERCURY.md).
