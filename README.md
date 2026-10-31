# Circle Of Inevitability

A Minecraft Forge mod by Luka (Lukario2401): a soul system (stage, tier, shards, Soul Essence, corruption)
and beyonder characteristics to take on: Wanderer, Worm Of Time, Hand Of Order, Attendant Of Mysteries,
Prince Of Abolition, Pale Emperor, Conqueror, Apocalypse (the Demoness), Dark Angel (the Hanged Man), Beauty Goddess (the Moon),
Hand Of God (the Twilight Giant), Knight Of Misfortune, Abomination (the Chained), Knowledge Emperor (the Hermit), Illuminator (the Paragon), Star Dragon (the Condenser) and Key Of Stars (the Door).
They come
with abilities on seven keys, melee combos and their own particles, slashes and 3D effects.

- Minecraft 26.2, Forge 65.1.0 or newer, Java 25
- Mod id: `circle_of_inevitability`
- Needed on both the client and the server
- Source and issues: https://github.com/lukario2401/Circle_Of_Inevitability

## Building

```
./gradlew build
```

The jar is written to `build/libs/circle_of_inevitability-<version>.jar`. Put it in the `mods` folder of a
Forge 65 client or server.

`./gradlew runClient` and `./gradlew runServer` start a development game in `run/`.

## Playing

Ops set a player up with the `/soul` command, for example:

```
/soul setAspect @s "Wanderer"
/soul setstage @s 7
/soul settier @s 7
/soul setshards @s 100000
```

The seven abilities are on G, H, J, K, L, N and M (category "Circle Of Inevitability" in Controls > Key Binds);
sneak while pressing a key for the second cast. Soul Essence is shown above the hotbar.

Summon the animated Sequence 2 Star Dragon boss with `/stardragon summon ~ ~ ~8`, or the hard Sequence 0
Condenser with `/stardragon summon_condenser ~ ~ ~8`. Their attacks, chains, windups, cooldowns, forms and
manual controls are documented in [Star Dragon bosses](docs/STAR_DRAGON_BOSS.md).

Custom sword swings can be tuned live with `/swinganim`: repeat or hold a preview, edit keyframe numbers,
timing, easing and global placement, then save local overrides or export Java. See
[Live sword swing tuning](docs/SWING_TUNING.md) for commands and a walkthrough.

The **Sealed Artifacts** creative tab contains the Sun Brooch: right-click for purification, but manage
the heat it builds while carried. Obtain it with `/give @s circle_of_inevitability:sun_brooch`.
Powers, drawbacks and the extension guide live in [Sealed artifacts](docs/SEALED_ARTIFACTS.md).

Any player can type `/damagefx` to turn damage numbers on or off for themself: the damage they deal is shown
in red above the target's head (`docs/DAMAGE_FX.md`).

Ops can use `/targethealth` to toggle Illuminator's target health display for themself on any pathway,
or `/targethealth on` and `/targethealth off` to set it explicitly (permission level 2 required).
Look at an enemy within 24 blocks to see its name and current/maximum health beside the hotbar;
the display updates every five ticks and disappears when you look away or a block obstructs the view.
It uses the same targeting rules as Appraiser's Eye, excluding allies and targets concealed from abilities.
The preference survives death, reconnects and server restarts; command-granted access requires OP permission
while viewing. Turning it off leaves Illuminator's natural Appraiser's Eye passive active.

This is a port of the author's 1.21 mod; `MIGRATION_NOTES.md` records how it was ported and what is
still to check.

The [Omniscient Eye asset collection](docs/OMNISCIENT_EYE_ASSETS.md) supplies models, textures,
icons, particles and sound cues for the White Tower pathway design. The
[first 13 abilities and stored replication](docs/OMNISCIENT_EYE.md) are implemented on keys 1–5.
