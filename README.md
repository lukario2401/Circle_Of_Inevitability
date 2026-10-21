# Circle Of Inevitability

A Minecraft Forge mod by Luka (Lukario2401): a soul system (stage, tier, shards, Soul Essence, corruption)
and beyonder characteristics to take on: Key Of Stars, Worm Of Time, Hand Of Order, Attendant Of Mysteries,
Prince Of Abolition, Pale Emperor, Conqueror, Apocalypse (the Demoness), Dark Angel (the Hanged Man), Beauty Goddess (the Moon),
Hand Of God (the Twilight Giant), Knight Of Misfortune, Abomination (the Chained) and Knowledge Emperor (the Hermit).
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
/soul setAspect @s "Key Of Stars"
/soul setstage @s 7
/soul settier @s 7
/soul setshards @s 100000
```

The seven abilities are on G, H, J, K, L, N and M (category "Circle Of Inevitability" in Controls > Key Binds);
sneak while pressing a key for the second cast. Soul Essence is shown above the hotbar.

Any player can type `/damagefx` to turn damage numbers on or off for themself: the damage they deal is shown
in red above the target's head (`docs/DAMAGE_FX.md`).

This is a port of the author's 1.21 mod; `MIGRATION_NOTES.md` records how it was ported and what is
still to check.
