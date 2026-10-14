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
| Creation | Life-Giver (2) | A Seed of Life is planted. For a while it heals everything around it to full, grows plants, and brings fallen allies back at low health. |
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

To be built the way the other Pathways are: the key methods in a new `BeautyGoddess.java`, one line per ability in `PathwayAbilities.ALL`, a row in `docs/SEQUENCES.md`, and the passives in `SequencePassives`.
