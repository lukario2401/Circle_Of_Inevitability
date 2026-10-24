# Spell kit: ready-made parts for abilities

The Attendant Of Mysteries abilities are built from small tools that any Pathway can use: something that flies and hits, a countdown on the screen, a line between two moving things, damage that goes somewhere else, taking over a mob, vanishing, cheating death.

Nothing needs registering. Call them from your ability method. The 3D models themselves are covered in [MODEL_FX.md](MODEL_FX.md).

| I want | Use | Lives in |
|---|---|---|
| The enemy under the crosshair, everyone in a circle, the ground I aim at | [`Spells`](#1-spells-aiming-and-enemies) | `combat/` |
| A model that flies and hits things (dagger, bolt, shell) | [`Shot`](#2-shot-a-model-that-flies-and-hits) | `combat/` |
| A model with an animation of my own that hits what it reaches | [`AnimatedShot`](#animatedshot-a-model-that-hits-along-its-own-animation) | `combat/` |
| 5, 4, 3, 2, 1 on the screen and then something happens | [`Countdown`](#3-countdown) | `combat/` |
| A small clock next to the hotbar ("Revive 27s") | [`HudTimer`](#hudtimer-a-small-clock-next-to-the-hotbar) | `combat/` |
| A small counter next to the hotbar ("[sigil] Order 4/9") | [`HudCounter`](#hudcounter-a-small-counter-next-to-the-hotbar) | `combat/` |
| Ground that keeps doing something to whoever stands on it, worse the longer they stay (cursed land, a healing circle, an aura) | [`Zone`](#zone-ground-that-keeps-doing-something) | `combat/` |
| A hidden spot that springs once when an enemy steps on it (a snare, a mine, a rune) | [`Trap`](#trap-a-hidden-spot-that-springs-once) | `combat/` |
| A shell round a spot that enemies and their projectiles cannot cross, and that shelters allies inside (a dome of light, a barrier, a cage) | [`Dome`](#dome-a-shell-that-holds-a-line) | `combat/` |
| A flat wall in the air that enemies and their projectiles cannot cross, either way (a gate slammed shut, a curtain of fire) | [`Wall`](#wall-a-flat-barrier-in-the-air) | `combat/` |
| Is this spot inside my zone? Keep some entities out of other players' zones | [`Zone.covers`, `Zone.addShelter`](#zone-ground-that-keeps-doing-something) | `combat/` |
| Anyone's domes at a spot or on a line, and turning one inside out against its owner | [`Dome.raisedAt`, `crossedBy`, `flip`](#dome-a-shell-that-holds-a-line) | `combat/` |
| Something one enemy carries for a while and then passes on to the nearest ones, stronger each time (a plague, a spreading curse) | [`Contagion`](#contagion-it-passes-from-one-enemy-to-the-next) | `combat/` |
| A potion effect kept up for as long as something lasts | [`Spells.keepEffect`](#1-spells-aiming-and-enemies) | `combat/` |
| A line that keeps joining two moving things (thread, chain, beam) | [`SpellFx.tether`](#4-spellfx-looks-that-many-abilities-share) | `combat/` |
| Rings, warning circles, a burst of fire or of any color | [`SpellFx`](#4-spellfx-looks-that-many-abilities-share) | `combat/` |
| A wave that spreads and hits each enemy as it gets there | [`Spells.wave`](#1-spells-aiming-and-enemies) + `SpellFx.steadyRing` | `combat/` |
| Everyone the caster can see, or everyone in a cone | [`Spells.enemiesInSight`, `enemiesInCone`](#1-spells-aiming-and-enemies) | `combat/` |
| Teleport to where the caster looks | [`Spells.blinkSpot`](#1-spells-aiming-and-enemies) | `combat/` |
| Draw someone along a line to a point, fast and straight (a grappling line, a whip that yanks, a web pulling everyone in) | [`Reel`](#reel-drawn-along-a-line) | `combat/` |
| Travel inside something you throw and step out where it lands (merge with a flame, ride a bolt), straight or steered with your look | [`Transit`](#transit-travel-inside-a-shot) | `combat/` |
| A model that flies into an enemy and cannot miss | [`SpellFx.homing`](#models-on-an-entity) | `combat/` |
| Count hits, stacks or charges on an entity, and show them | [`Marks`](#marks) + [`SpellFx.overHead`](#models-on-an-entity) | `status/` |
| A mark under someone that follows them (cursed, warded), changing as it grows | [`SpellFx.circleUnder`](#models-on-an-entity) | `combat/` |
| Put a ring or a field of things on uneven ground | [`Spells.groundNear`](#1-spells-aiming-and-enemies) | `combat/` |
| Blind something (mobs too) | [`Blind`](#blind) | `status/` |
| Make something take more damage for a while (a curse) | [`Vulnerable`](#vulnerable) | `status/` |
| Make a mob go after one chosen entity ("come at me"), or set mobs on each other | [`Taunt`](#taunt) | `status/` |
| Drag something under the ground and give it back | [`Burial`](#burial) | `status/` |
| Let a player fly for a while | [`Flight`](#flight) | `status/` |
| Carry a player fast along their look for a while, and know which enemies they passed (wings, a dive, a leap on the wind) | [`Glide`](#glide) | `status/` |
| Spread someone over many small bodies: blows kill those instead, and only the last one falling lets damage through (a bat swarm, a flock of paper birds) | [`Swarm`](#swarm) | `status/` |
| Wings (or anything worn) that turn with the player | `ParticleShapes.modelOnTurning`, `PaleEmperorFx.wings(sl, player, ticks, tag)` ([MODEL_FX.md](MODEL_FX.md)) | `particles/fx/` |
| A crown (or anything on the head) that looks where the head looks | `ParticleShapes.modelOnHead`, `PaleEmperorFx.crown(sl, player, ticks, tag)` ([MODEL_FX.md](MODEL_FX.md)) | `particles/fx/` |
| Damage that lands on someone else, or is shared by a group | [`DamageLink`](#damagelink) | `status/` |
| "The next hit does not land, this happens instead" | [`Substitute`](#substitute) | `status/` |
| "My next normal hit is special" (a backstab, a charged blow) | [`EmpoweredHit`](#empoweredhit) | `status/` |
| Write down everything someone takes for a while, then hit them with all of it at once (a curse that comes due) | [`StoredDamage`](#storeddamage) | `status/` |
| Nothing heals them for a while (frostbite, a festering wound) | [`NoHeal`](#noheal) | `status/` |
| An open wound: damage once a second for a while, counted as the caster's (a chain of blood, claws, thorns) | [`Bleeding`](#bleeding) | `status/` |
| A rot that eats deeper every second until it is cleansed, and stops all healing after a while (corrosion, a curse that festers) | [`Corrosion`](#corrosion) | `status/` |
| A status of your own that every cleanse (and a milk bucket) takes off | [`Cleansing`](#cleansing) | `status/` |
| Part of someone's attacks go wide for a while (arrogance, a scrambled mind, smoke in the eyes) | [`Falter`](#falter) | `status/` |
| Who died near a spot lately, and raising them again as what they were (risen dead, a sentence carried out) | [`Fallen`](#fallen-the-dead-that-can-rise) | `combat/` |
| Who a player has touched lately (hit, cursed, chained), to reach them again anywhere in the world (a curse through a connection, a scrying) | [`Connection`](#connection) | `status/` |
| Lift loose things (dropped items, arrows, armor stands, boats) and throw them at enemies (a poltergeist, a puppeteer) | [`Hurled`, `Spells.looseThingsAround`](#hurled-loose-things-thrown) | `combat/` |
| Take over a mob | [`Marionette`](#marionette) | `status/` |
| Call up creatures that fight for you for a while, then vanish without drops (a door of summoning, risen dead) | [`Marionette.spawnSummon`](#marionette) | `status/` |
| Everyone in a circle, enemies and allies alike, or only the caster's side | [`Spells.livingAround`, `alliesAround`](#1-spells-aiming-and-enemies) | `combat/` |
| Make plants grow in a circle, as bone meal does | [`Spells.growPlants`](#1-spells-aiming-and-enemies) | `combat/` |
| Vanish: unseen, untargeted, unhurt | [`Concealment`](#concealment) | `status/` |
| Throw someone (or yourself) out of the world for a while: unseen, unhurt, unable to act, back where they left or where you say | [`Exile`](#exile) | `status/` |
| Leave your body and live inside a mob or a player, which carries you | [`Parasite`](#parasite) | `status/` |
| Cheat death once | [`DeathWard`](#deathward) | `status/` |
| Throw someone into the air without fall damage | [`FallGuard`](#fallguard) | `status/` |
| Hold in place, freeze | `Root`, `Freeze` (already there) | `root/` |
| Nothing roots or freezes someone while something is true (a body too dense to hold, a mind no word binds) | [`Root.addImmunity`, `Freeze.addImmunity`](#root-and-freeze-immunities) | `root/` |
| Less supernatural damage for a while, and blindness, nausea, roots, freezing and taunts shaken off | [`SupernaturalResistance`](#supernaturalresistance) | `status/` |
| A Beyonder's body: taller, more hearts, longer reach, faster swings, on while something is true | [`Physique`](#physique-a-beyonders-body) | `blasphemy_slate/beyonder/` |
| Rot: no healing, wither, weakness, armor and weapons wearing out, less maximum health (Twilight, a curse of age) | [`Decay`](#decay) | `status/` |
| Put something to sleep (wakes when hit, or deeply, or for good), or leave it limp and unwilling to fight | [`Slumber`](#slumber) | `status/` |
| Make something afraid of one entity: a mob runs from it, a player is slowed and cannot sprint at it | [`Fear`](#fear) | `status/` |
| Hush an entity's power: no abilities for a player, no attacks at all for a mob | [`Silence`](#silence) | `status/` |
| Take an entity's luck: levels of Misfortune, and every second a mishap may befall it (a stumble, a fumble, a shot gone wide, a falling stone, a calamity) | [`Misfortune`](#misfortune) | `status/` |
| How dark is it for this entity (0..1), and night forced on a place or a body | [`Nocturnality`](#nocturnality) | `status/` |
| Nobody (or nobody outside) can pick these as an ability's target: a veil, a sealed world | [`Spells.addUnchoosableCheck`](#1-spells-aiming-and-enemies) | `combat/` |
| A power that works though the caster is silenced (a poem in the mind) | [`Silence.addCastsThrough`](#silence) | `status/` |
| "My next few hits are special" (a poison, holy oil, a concoction on the blade) | [`WeaponCoat`](#weaponcoat) | `status/` |
| Minds slide off you: mobs forget you and abilities cannot choose you, though you are seen and can be hurt | [`Unnoticed`](#unnoticed) | `status/` |
| A bigger body for a while, with more reach and step, and a madness that can break it (a giant, a titan) | [`GiantForm`](#giantform) | `status/` |
| A player takes another shape for a while: attributes and effects kept up while it lasts, flying, less damage taken, a beat of its own, a weakness after, a clean end on death, logout or a lost Pathway | [`Form`](#form) | `status/` |
| Armor that is not an item: less damage, a layer of absorption, no knockback (plates of light, a coat of silver) | [`ConjuredArmor`](#conjuredarmor) | `status/` |
| A weapon that is not an item: while it is drawn, the player's normal hits are its hits (a sword of light, claws of flame) | [`ConjuredWeapon`](#conjuredweapon) | `status/` |
| The light sources near a spot or near the crosshair, and whether a spot is in daylight | [`Spells.lightNear`, `lightAimed`, `inDaylight`](#1-spells-aiming-and-enemies) | `combat/` |
| The reflections (still water, ice, glass, polished metal) near a spot or near the crosshair | [`Spells.reflectionNear`, `reflectionAimed`, `isReflective`](#1-spells-aiming-and-enemies) | `combat/` |
| Is it a full-moon night? | [`Spells.isFullMoonNight`](#1-spells-aiming-and-enemies) | `combat/` |
| A hit that no shield, substitute or death ward stops | [`Spells.strikeThrough`](#1-spells-aiming-and-enemies) | `combat/` |
| A hit that nothing softens at all: no raised shield, armor, Protection or Resistance (a decree, an execution) | [`Spells.strikeTrue`](#1-spells-aiming-and-enemies) | `combat/` |
| "Burns evil and undead" in one call | [`Spells.isEvil`](#1-spells-aiming-and-enemies) | `combat/` |
| Particles only one player sees (a trail for the hunter) | `ParticleShapes.spawnFor(viewer, particle, at, velocity)` | `particles/fx/` |
| Do something a few ticks from now | `Later.run(sl, ticks, () -> ...)` (already there) | `combat/` |
| One key, many spells: a round menu where each has its own Sequence, cost and cooldown, and a sneak press casts the last pick again | [`AbilityWheel`](#abilitywheel-many-spells-on-one-key) | `menu/` |
| Every ability of every Pathway as a list, to pick one or cast one by its place | [`PathwayAbilities`](#another-pathways-abilities-listing-casting-and-stealing-them) | `blasphemy_slate/beyonder_characteristics/` |
| Cast an ability without its Sequence and without its cost | [`Beyonder.castUnbound`](#another-pathways-abilities-listing-casting-and-stealing-them) | `blasphemy_slate/beyonder/` |
| Let a player hold an ability that is not theirs, and take it from its owner meanwhile | [`StolenAbilities`](#another-pathways-abilities-listing-casting-and-stealing-them) | `blasphemy_slate/beyonder_characteristics/` |
| Something a Pathway always has from a Sequence on, with no key press (a potion effect, or code of your own once a second) | `SequencePassives` ([SEQUENCES.md](SEQUENCES.md#passives)) | `blasphemy_slate/beyonder/` |
| Damage that names no attacker (`damageSources().magic()`, an explosion or lightning with no owner) showing as the caster's damage number | `DamageFx.credit(player, target)` right before it ([DAMAGE_FX.md](DAMAGE_FX.md)) | `combat/` |
| Know an enemy better and better (0 to 100%, one gauge per caster): every hit on it grows stronger the more you know, and it fades when left alone | [`Study`](#study) | `status/` |
| Keep spells on paper: a pouch of scrolls, each one a stored cast that goes off for free when read; any spell can be one | [`Scrolls`](#scrolls) | `status/` |
| A few bodies circling a player (stars, blades, motes) that each stop a projectile and come back, and strike enemies that come close | [`Orbit`](#orbit-bodies-that-circle-and-guard) | `combat/` |
| Launch one of those bodies yourself (a cluster flung, an asteroid thrown on a blow) and have it back later | [`Orbit.release`](#orbit-bodies-that-circle-and-guard) | `combat/` |
| The projectiles of the other side near a spot, to slow, stop, hang or bend them | [`Spells.hostileShotsAround`](#1-spells-aiming-and-enemies) | `combat/` |
| "Less each time": how often a player used something lately, as a multiplier (a story told too often, a trick that wears thin) | [`Tellings`](#tellings-less-each-time) | `combat/` |
| Ground (or an aura) where gravity is wrong: it pulls things in, presses them down, lifts and holds them, slows them | [`GravityField`](#gravityfield-gravity-gone-wrong) | `combat/` |
| An instant beam at light speed that shows its line first and cannot be dodged once it fires, through one or everyone on it | [`Ray`](#ray-a-beam-at-light-speed) | `combat/` |
| Take every blessing and every ward off someone at once, or unmake a summon (a deletion, a word of unmaking) | [`Dispel`](#dispel) | `status/` |
| Nothing can hurt, choose or hold someone for a while, though they are seen, and what would have hurt them is added up | [`Invulnerable`](#invulnerable) | `status/` |
| "I have studied you": one caster's mark on an enemy for a while, its weakness, and every ability hit of theirs on it stronger (more again with the kind it is weak to) | [`Appraisal`](#appraisal) | `status/` |
| What something is weak to, as data: rules read from the top, the first that fits wins | [`WeaknessTable`](#appraisal) | `status/` |
| Someone cannot hide for a while: concealment, unnoticed and invisibility end and cannot take hold | [`Exposed`](#exposed) | `status/` |
| Someone deals less damage for a while, with everything (the other half of `Vulnerable`) | [`Enfeebled`](#enfeebled) | `status/` |
| Someone is heavier or lighter for a while: pressed down, floating, lifted off the ground | [`Gravity`](#gravity) | `status/` |
| Someone is Heavy for a while: slowed, no jumping or flying, falling like a stone and hurt more by falls, its shots slowed | [`Weight`](#weight) | `status/` |
| Someone cannot be picked out from further than a few blocks: far mobs forget them, far abilities cannot choose them | [`Unreadable`](#unreadable) | `status/` |
| Build a creature that fights for you: a vanilla body of your choice, hidden under your model, with its own health, blow, size, timer, leash and a limit per owner (dolls, golems) | [`Construct.of(...).build(...)`](#construct) | `status/` |
| Is this made rather than born (golems, constructs, an ability's own creations)? | [`Construct`](#construct) | `status/` |
| No abilities for a while, but still melee: a player's keys refused, a mob's projectiles and special hits stopped | [`AbilityLock`](#abilitylock) | `status/` |
| Where abilities were cast lately (who, Pathway, Sequence, where) | [`Residue`](#residue-where-abilities-were-cast) | `combat/` |
| Every ability of every Pathway costs a player more or less while something lasts (an eroded mind, a blessing, a kingdom) | [`Spells.addCostScale`](#1-spells-aiming-and-enemies) | `combat/` |
| Every cooldown a player starts is longer or shorter while something lasts | [`Cooldowns.addScale`](#cooldowns-for-single-keys) | `combat/` |
| Where a player stood, and how hurt they were, a few seconds ago (a step back in time) | [`History`](#history-where-a-player-was) | `combat/` |
| A cooldown on a single ability key, with the "ready again in" message | [`Cooldowns`](#cooldowns-for-single-keys) | `combat/` |
| The ally under the crosshair (a ward pinned on a friend) | [`Spells.aimAlly`](#1-spells-aiming-and-enemies) | `combat/` |
| Make one mob forget one entity for a while: it drops it as its target and cannot pick it again (a word that makes a hunter lose you) | [`Forgotten`](#forgotten) | `status/` |
| Remember ONE target a player picked, under a name, for a while (a Convict, a quarry, the one memorized), and do something when it ends | [`Chosen`](#chosen) | `status/` |
| Seal one ability key of a player for a while, and know which keys they used last (a power stripped away) | [`KeySeal`](#keyseal) | `status/` |
| How much an entity is bending the order of things right now, as a score (hiding, wards, links, roots, puppets, recent casts) | [`Disorder`](#disorder-how-much-order-is-bent) | `combat/` |
| Ground where Rules are in force for your enemies (no teleporting, hiding, stand-ins, revival, puppets, healing, shots, abilities, or a Reality that weakens magic), each broken Rule a counted Violation you punish | [`Decree`](#decree-ground-where-rules-are-in-force) | `status/` |
| Hold an entity as a root would, without a root's timer or ring, while something says so (every `Root.has` check obeys) | [`Root.addHold`](#root-and-freeze) | `root/` |
| Tell an ability's hit (`Spells.strike...`) from a plain swing in a hurt event | [`Spells.isSpellHit`](#1-spells-aiming-and-enemies) | `combat/` |
| Make a zone last longer, or end every enemy zone near a spot (a Quell) | [`Zone.extend`, `Zone.closeNear`](#zone-ground-that-keeps-doing-something) | `combat/` |
| Move someone from here to there at once, asking whether they may (Teleport roots, Decrees), with the fall taken off; the wall in front and the far side of it | [`Teleport`](#teleport-from-here-to-there) | `combat/` |
| Every lock on someone springs open: roots, harmful effects, fire, control, and nothing of that kind takes for a moment (a lock picked, a mind shaken free) | [`Unbound`](#unbound) | `status/` |
| A few creatures "caught" by one player at once (at most 3), seen by them and their allies, needed by what comes next | [`Starlit`](#starlit) | `status/` |
| Places a player saved, to go back to (waypoints, recall points), kept through death | [`Anchors`](#anchors) | `status/` |
| Roots on someone last longer or shorter while something is true (feet no hold keeps for long) | [`Root.addDurationScale`](#root-and-freeze-immunities) | `root/` |
| Know when an ability's aiming was turned away from an Unreadable entity (turn the search back) | [`Unreadable.addTurnedAwayListener`](#unreadable) | `status/` |

All of it runs on the server. `sl` is the `ServerLevel` your ability method gets.

---

## The shape of an ability

Every key press calls the ability method of **every** Pathway class, so each one starts by checking that the player follows its Pathway. After that: check the Sequence, check that the cast makes sense, take the cost, do it.

```java
private static final String PATHWAY = "My Pathway";             // the exact name the player's Pathway is set to

public static void myPathwayIceLance(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
    if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return;
    if (!Beyonder.hasSequence(player, 7)) return;               // needs Sequence 7 or a stronger one (a lower number)

    LivingEntity target = Spells.aimEnemy(player, sl, 24);      // who am I looking at?
    if (target == null) {
        player.sendSystemMessage(Component.literal("No one stands before you."));
        return;                                                 // nothing spent
    }
    if (!Spells.paySpirituality(player, 800)) return;           // says "Not enough spirituality." by itself

    if (player.isShiftKeyDown()) {
        // sneak cast
    } else {
        Spells.strike(player, target, 20f);
    }
}
```

`SoulCore` and `Beyonder` are in `blasphemy_slate/beyonder/`. Sequences, what each one can pay for and the Sequence every existing ability needs are in [SEQUENCES.md](SEQUENCES.md); add a new ability to its table.

Then add it to the packet of the key it belongs to (`network/SKeyPressAbilityOneUsed` ... `SevenUsed`): one `import static` at the top and one line next to the others.

```java
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.MyPathway.myPathwayIceLance;

myPathwayIceLance(player, level, serverLevel, false);
```

Give it a line in `PathwayAbilities.ALL` too (the same name and Sequence), so that it can be listed and stolen.

A new Pathway also goes into `SequenceCommands.PATHWAYS`, so that `/sequence pathway` offers its name. `Conqueror.java` is the smallest Pathway class to copy: one ability, and a list at its top of every place the next one touches.

Two habits worth keeping:

- **Check first, pay after.** A cast that cannot work (no target, no room) should cost nothing.
- **Messages go to chat** (`player.sendSystemMessage`). The line above the hotbar is rewritten every tick by the spirituality display, so a message there is gone at once.

---

## 1. Spells: aiming and enemies

`Spells` answers the questions every ability asks.

Who can be **picked** as a target (by `aimEnemy`, `enemiesInSight` and the aiming of `aimFrom` / `aimGround`) is `Spells.canBeChosen(chooser, target)`: not someone `Unnoticed`, and not someone an `addUnchoosableCheck` rule hides. What hits an area still catches them.

```java
Spells.addUnchoosableCheck((chooser, target) -> MyVeil.covers(target) && !MyVeil.covers(chooser));   // once, in a static block
```

(`chooser` is null when the caller does not say.) Knight Of Misfortune uses it for Secret Concealment, the Concealed World and the Servant of Concealment's presence.

```java
LivingEntity target = Spells.aimEnemy(player, sl, 24);            // the enemy under the crosshair, or null
Vec3 spot = Spells.aimGround(player, sl, 24);                     // the ground being aimed at
for (LivingEntity enemy : Spells.enemiesAround(player, sl, spot, 4)) {
    Spells.strike(player, enemy, 12f);
    Spells.push(enemy, spot, 0.9, 0.4);
}
```

| Call | What you get |
|---|---|
| `aimEnemy(player, sl, range)` | The enemy the player is looking at (not through walls), or `null`. It passes over one whose mind is concealed (`Unnoticed`), as do `aimFrom`, `aimGround` and `enemiesInSight` |
| `aimAlly(player, sl, range)` | The ally the player is looking at (not through walls), or `null`: the caster's side (`isFriend`: team mates, pets, marionettes, summons), never the caster. For a ward pinned on a friend, a blessing, a step back sent to someone else |
| `aimPoint(player, sl, range)` | Where the crosshair ends: on the first block in the way, else `range` blocks out |
| `aimGround(player, sl, range)` | The ground being aimed at. Under the first enemy in the way, if there is one. Aimed at the open sky, with no ground within 24 blocks below, it is a point in the air |
| `aimFrom(player, sl, from, range)` | The direction from `from` to what the crosshair is on. For things thrown from a hand or by a summon standing beside the player: thrown along the plain look direction they land a step to the side |
| `groundAt(player, sl, point)` | Top of the blocks under a point |
| `enemiesAround(player, sl, center, radius)` | Every enemy whose body is within `radius` of a point |
| `livingAround(player, sl, center, radius)` | Everything alive there, enemies and allies alike (not the caster, spectators or armor stands): a sight that sees all, a splash that hurts some and heals others |
| `alliesAround(player, sl, center, radius)` | Only the caster's side there: team mates, pets, marionettes and summons. The caster is not in it |
| `growPlants(player, sl, ground, radius, spots)` | `spots` random places in the circle grow as if bone-mealed (crops ripen, saplings grow, grass flowers). Call it every second for a garden. Returns how many grew |
| `enemiesOnLine(player, sl, from, to, grow)` | Every enemy the line passes through, nearest first. `grow` makes the line fatter |
| `enemiesInSight(player, sl, range, halfAngle)` | Every enemy the caster can see: within `range`, at most `halfAngle` degrees from the crosshair (45 is about the whole screen), not behind blocks. Nearest first |
| `enemiesInCone(player, sl, from, direction, range, halfAngle)` | Every enemy in a cone from any point along any direction (a breath, a roar out of a summon's mouth). Walls do not matter. Nearest first |
| `wave(player, sl, ground, radius, ticks, height, enemy -> ...)` | A wave spreading from a spot: runs your code once for each enemy, at the moment the wave gets to it. Draw it with `SpellFx.steadyRing` and the same radius and ticks |
| `blinkSpot(player, sl, range)` | Where the player's feet go to be at what the crosshair is on: on the ground they aim at, in front of a wall, or out in the open air. Never inside a block. `null` if there is no room |
| `groundUnder(player, sl, entity)` | The ground an entity stands on or hangs just above: where spikes or hands for it should appear |
| `groundNear(player, sl, spot, reach)` | The ground at a spot whose height you only roughly know: the top of the blocks within `reach` above or below it, or `null` (a cliff, a pit, open air). For a ring or a field of things on uneven land |
| `spotAround(center, yaw, distance)` | The spot `distance` blocks from a point the way `yaw` looks. For rings: `spotAround(center, yaw + i * 360f / count, radius)` |
| `firstEnemyOnLine(...)` | The nearest of those, or `null` |
| `isEnemy(player, other)` | The rule itself (see below) |
| `isFriend(player, other)` | The caster's side (the caster too), judged as if nobody were concealed: for a wall that must keep hidden enemies out as well (`!isFriend`) |
| `isEvil(entity)` | Undead, every monster (`Enemy`), and what an `addEvilCheck(entity -> ...)` rule names. For "burns evil and undead" |
| `canBeChosen(entity)` | `false` for one an ability may not pick as its target (an `Unnoticed` mind) though it is there. The aiming above already asks it; ask it yourself when you pick a target another way |
| `strikeThrough(player, target, damage)` | Like `strike`, but no `Dome` shield softens it, no `Substitute` takes it and no `DeathWard` saves from it (none of them is used up). `isPiercing(entity)` is `true` while one lands: a shield or ward of your own should let it through too |
| `strikeTrue(player, target, damage)` | Like `strikeThrough`, and nothing softens it either: a raised shield is dropped first, and the target loses what the hit comes to after the damage events (bonuses such as `Vulnerable` or a Pathway's might, the target's own Pathway defenses) with nothing taken off by armor, Protection, Resistance or absorption hearts. Something that cancels the hit outright (`Concealment`, `Exile`, a `Falter` miss) still stops it. Decree (`PrinceOfAbolitionSovereign`) |
| `isSpellHit(entity)` | `true` while any of the three strikes above is landing on it. Such a hit looks like a player's attack to the game; this tells it from a swing in a hurt event (an Imperative: Reality halves only abilities) |
| `cleanse(entity)` | Every harmful potion effect off, the fire out, and every status that listens to [`Cleansing`](#cleansing) (Corrosion, and any of your own) |
| `lightNear(sl, center, radius, minLight)` | The blocks giving off at least `minLight` light (torch 14, lantern and glowstone 15) within `radius` (8 at most: every block is looked at), nearest first |
| `lightAimed(player, sl, range, around, minLight)` | The light source nearest to what the crosshair is on, within `around` of it, or `null` |
| `inDaylight(sl, pos)` | `true` by day for a spot under the open sky: sunlight as a light source |
| `isReflective(sl, pos)` | `true` for a block that shows a reflection: still water at the surface (a source block with no water on top), ice of every kind, glass and glass panes (stained and tinted too), and blocks of polished metal (iron, gold, unweathered copper, netherite, diamond) |
| `reflectionNear(sl, center, radius)` | The reflective blocks within `radius` (8 at most, as for `lightNear`), nearest first |
| `reflectionAimed(player, sl, range, around)` | The reflection nearest to what the crosshair is on, within `around` of it, or `null`. The crosshair stops on still water here, so looking at a lake finds the lake |
| `looseThingsAround(player, sl, center, radius)`, `isLoose(player, entity)` | The things within `radius` a power could lift and throw (combat/Hurled), nearest first: dropped items, projectiles (flying or stuck in a wall), armor stands, and boats, minecarts and the like that nobody rides. Never anything alive or a block; things already being thrown are left out |
| `isFullMoonNight(sl)` | `true` at night when the moon is full: the world's first night and every eighth after it (`MOON_DAYS`). Counted from the world's age, so sleeping through a night or `/time set` does not move it |
| `clearStart(player, sl, wanted)` | `wanted`, or just in front of the wall if a wall is between it and the player's eyes. For things that appear beside the player |
| `clearLine(player, sl, from, to)` | `true` when nothing solid is between two points |
| `freeSpotNear(player, sl, from, direction, distance)` | A spot to blink to: about `distance` blocks from `from`, in or near `direction`, where the player fits. `null` if walled in |
| `fits(player, sl, feet)` | `true` if the player's body fits with their feet there. Check it before a teleport |
| `strike(player, target, damage)` | Damage from the player that always counts, even right after another hit. The target becomes "what the player last hit", which marionettes and tamed wolves go after |
| `push(target, from, strength, lift)` | Throws the target away from a point |
| `pull(target, to, maxSpeed, lift)` | Drags it toward a point, slowing as it gets there. Call it every couple of ticks for a steady pull |
| `keepEffect(entity, MobEffects.X, amplifier, ticks)` | A potion effect you put on again and again while something lasts (a zone, an aura, a beam). It leaves the effect alone while more than half of `ticks` is left, so wither and poison keep hurting (started afresh every few ticks they never would), and it never replaces a stronger effect by a weaker one |
| `cleanse(entity)` | Removes every harmful potion effect and puts out fire |
| `harmfulEffects(entity)` | The harmful effects on it right now |
| `paySpirituality(player, cost)` | Takes the spirituality (after every `addCostScale` rule) and returns `true`. If there is too little it takes nothing, tells the player and returns `false` |
| `addCostScale(player -> 1.5f)`, `costFor(player, cost)` | A rule, once in a static block, for what every ability of every Pathway costs a player now (1.5 = half again, 0.8 = 20% less); rules multiply. `paySpirituality` and the wheels (their check, their price and the cost under each icon) ask it; a stolen cast is never scaled. The Illuminator's Eroded, Enlightenment, Enlightenment Aura and Mechanical Heaven |
| `addMoveImmunity(entity -> ...)`, `cannotBeMoved(entity)` | A rule, once in a static block: while it says true, `push` and `pull` leave that entity where it is (and every field, wave and shove built on them, `GravityField`'s lifting too). The Star Dragon's Dense Body, Heavybringing and Neutron Star |
| `hostileShotsAround(player, sl, center, radius)` | The projectiles in flight within `radius` of a point that are not the caster's side's (one with no owner counts; one lying still or stuck in a wall does not). For a field that slows them, stops them, makes them hang or bends them in: the Star Dragon's Time Dilation, storm, Gravity Authority and Singularity |
| `isBoss(entity)`, `holdLength(target, ticks)` | A boss: the dragon, the wither, the warden, an elder guardian, or anything with more than `BOSS_HEALTH` (150) maximum health. `holdLength` is the shared rule for how long a hold lasts: `BOSS_HOLD` (half) on a boss, `PLAYER_HOLD` (70%) on a player, all of it on anything else. For lifts, cages, voids, freezes in time |
| `sound(sl, at, SoundEvents.X, volume, pitch)` | A sound at a spot |
| `casterStillHere(player, sl)` | `false` once the player died, left or changed dimension. Check it in the later steps of a spell |
| `yawOf(direction)`, `flatLook(player)`, `rightOf(player)`, `turned(direction, degrees)`, `UP` | Directions. `turned` is how you make a fan of projectiles |
| `turnToward(from, to, maxDegrees)` | `from` turned toward `to` by at most that many degrees, up and down too. Call it every tick to steer something that cannot turn on the spot: `heading = Spells.turnToward(heading, player.getLookAngle(), 12)` |

**Who is an enemy.** Anything alive except: the caster, what the caster rides or owns (pets, tamed horses), the caster's marionettes, team mates, armor stands, spectators and whoever is concealed.

If a new Pathway has summons of its own, teach the rule about them once (for example in a `static { }` block of its class), and every spell built on `Spells` leaves them alone:

```java
Spells.addAllyCheck((caster, other) -> MySummons.belongsTo(other, caster));
```

---

## 2. Shot: a model that flies and hits

For thrown daggers, bolts, shells, cards. You describe it once, like a `ModelFx`, and fire it from an ability.

**The model's top (up in Blockbench) must be its front.** A dagger is modelled standing on its handle, tip up.

```java
private static final ModelFx KNIFE_MODEL = ModelFx.of("my_pathway/knife")
        .pivot(8, 8, 8).glow().spin(30f);                // spins around its own length as it flies

private static final SlashFx KNIFE_STREAK = SlashFx.line("slash/smooth")
        .color(0xA0C0E0FF).core(0xE0FFFFFF)
        .width(0.12f).taper(SlashFx.Taper.COMET);

private static final Shot KNIFE = Shot.of(KNIFE_MODEL)
        .speed(3.0)              // blocks per tick
        .range(32)               // how far it flies if nothing is in the way
        .width(0.4)              // how close its path must pass to an enemy's body to hit it
        .tip(0.5)                // blocks from the model's pivot to its tip (see below)
        .trail(KNIFE_STREAK)     // a streak along its path
        .stick(40);              // stays stuck in a block it hits for 2 seconds
```

In the ability:

```java
Vec3 hand = Spells.clearStart(player, sl, player.getEyePosition().add(Spells.rightOf(player).scale(0.35)).add(0, -0.25, 0));
KNIFE
        .onHit((target, at) -> Spells.strike(player, target, 12f))
        .onEnd((at, hitBlock) -> {
            if (hitBlock) Spells.sound(sl, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.4f, 1.8f);
        })
        .fire(player, sl, hand, Spells.aimFrom(player, sl, hand, 32));
```

| Option | What it does |
|---|---|
| `Shot.of(model)` | A shot that looks like this `ModelFx`. Give it a model without keys of its own (`.spin(...)` is fine) |
| `.model(other)` | Another look for the same shot (bigger, tinted...) |
| `.speed(blocksPerTick)` | 1 = 20 blocks a second |
| `.range(blocks)` | How far it flies when nothing stops it |
| `.width(blocks)` | How fat its path is. `0.3` to `0.5` for small things, more for big shells |
| `.tip(blocks)` | Distance from the model's pivot to its front end, at the scale you play it. With it, the tip stops at the target. Without it, the pivot does, and the model ends up half inside the wall |
| `.pierce()` | Flies through every enemy on its path (each is hit once) instead of stopping at the first |
| `.windup(ticks)` | Appears and hangs in the air this long before it leaves |
| `.stick(ticks)` | Stays stuck in the block it hits. A model with `.spin(...)` stops turning there |
| `.trail(line)` | A `SlashFx.line` drawn along its path as it flies |
| `.onHit((target, at) -> ...)` | Runs for each enemy it reaches, when it gets there |
| `.onEnd((at, hitBlock) -> ...)` | Runs where its flight ends. `hitBlock` is `true` on a block, `false` on an enemy or at the end of its range. This is where an explosion goes |
| `.fire(player, sl, from, direction)` | Fires it. Returns how many ticks from now its flight ends |

Every method returns a new copy, like `ModelFx`. `KNIFE.range(12)` on its own line does nothing; fire the result.

**A fan of them:**

```java
Vec3 look = Spells.aimFrom(player, sl, hand, 32);
for (int i = 0; i < 5; i++) {
    double turn = (i / 4.0 - 0.5) * 40;                  // 5 knives spread over 40 degrees
    KNIFE.onHit((target, at) -> Spells.strike(player, target, 12f))
            .fire(player, sl, hand, Spells.turned(look, turn));
}
```

**An exploding shell:** hit in `onHit`, splash in `onEnd` (see `airCannon` in `AttendantOfMysteries.java`).

**How it works.** The whole flight is worked out the moment it is fired: where the first block is and which enemies are on the path. The model is told to fly exactly that far and the hits are delivered with `Later.run` when it gets there. An enemy that moved more than about 2 blocks off the path by then is missed. A mob that walks *into* the path after the shot was fired is not hit, so keep shots fast (2 blocks per tick or more).

### AnimatedShot: a model that hits along its own animation

A `Shot` flies in a straight line at one speed. When the model should do more than that (hover and turn before it leaves, swell up, start slowly, fly in a curve) you animate the `ModelFx` with keys as always, and an `AnimatedShot` plays it and hits whatever the model reaches.

**The model's front (north in Blockbench) faces the target.** In its keys, `forward(...)` is "toward what was aimed at"; `up(...)` and `right(...)` are across it.

```java
// 1. the animation, like any other ModelFx
private static final ModelFx SPEAR_MODEL = ModelFx.of("my_pathway/spear")
        .pivot(8, 8, 8).glow().lifetime(80).fade(0, 4)
        .during(8, 50, ModelFx.pose().spin(360), ModelFx.Ease.IN_OUT)        // turns once, standing on its end
        .during(50, 60, ModelFx.pose().pitch(90), ModelFx.Ease.IN_OUT)       // tips over: the point looks forward
        .during(64, 76, ModelFx.pose().forward(26f), ModelFx.Ease.IN);       // and flies

// 2. what kind of shot it is
private static final AnimatedShot SPEAR = AnimatedShot.of(SPEAR_MODEL)
        .width(0.6)              // how close its path must pass to an enemy's body to hit it
        .tip(1.8)                // blocks from the model's pivot to its front end
        .pierce();               // goes through enemies instead of stopping at the first
```

In the ability:

```java
Vec3 from = Spells.clearStart(player, sl, player.position().add(0, 2.2, 0));     // above the caster's head
SPEAR
        .onHit((target, at) -> Spells.strike(player, target, 40f))
        .fire(player, sl, from);                                                 // at what the crosshair is on
```

Something that bursts on the first thing it meets is the same without `.pierce()`, with the blast in `onStop`:

```java
SKULL.onStop((at, hitBlock) -> {
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, at, 4.5)) Spells.strike(player, enemy, 30f);
            SpellFx.fireBurst(sl, at, 4.5);
        })
        .fire(player, sl, from);
```

| Option | What it does |
|---|---|
| `AnimatedShot.of(model)` | A shot that looks and moves like this `ModelFx` |
| `.model(other)` | Another model for the same shot |
| `.width(blocks)` | How fat its path is |
| `.tip(blocks)` | Distance from the model's pivot to its front end, at the scale you play it. With it the point reaches the enemy and the wall; without it the pivot does |
| `.pierce()` | Goes through enemies (each is hit once) instead of stopping at the first |
| `.throughBlocks()` | Blocks do not stop it |
| `.anyDirection()` | Also hits while it moves sideways or backward (a blade that swings round, something that comes back) |
| `.onHit((target, at) -> ...)` | Runs for each enemy it reaches, at that moment |
| `.onStop((at, hitBlock) -> ...)` | Runs where it is stopped: by a block, or by the first enemy when it does not pierce. The model is already gone |
| `.onExpire(at -> ...)` | Runs when the animation ends and nothing stopped it |
| `.onMove(at -> ...)` | Runs every tick it flies, with where it is: a trail of particles, a hum. Not while it hovers, turns on the spot or draws back. The first time it runs is the moment it is let fly, so that is where a "whoosh" goes |
| `.fire(player, sl, from)` | Plays it aimed at what the crosshair is on, up and down too. Returns how many ticks the animation lasts |
| `.fire(player, sl, from, direction)` | The same along a direction of your own |
| `.ticks()`, `.reach()` | How long the animation lasts, and how far forward its front end gets |

Every method returns a new copy, like `Shot`.

Good to know:

- **Nothing names a tick.** It asks the model where it is (`ModelFx.poseAt`) every tick from the first to the last. Change the keys, the timing or the lifetime of the model and the hits follow by themselves.
- **It only hits while it moves forward.** Hovering, turning on the spot and drawing back before a throw hurt no one. `.anyDirection()` switches that off.
- **It is stopped by blocks and by the first enemy**, and its model is taken away at that moment. It does not need a tag from you for that: every shot gets its own.
- **It is aimed once, when it is fired.** A model cannot be turned after it appeared, so one that hovers for 3 seconds still flies at what the crosshair was on when the key was pressed.
- It sees enemies that walk into its way while it flies, which a `Shot` does not.
- The model's own turning (`pitch`, `spin`...) is only for the look. What counts for the hits is where its pivot goes.

---

## 3. Countdown

Big numbers in the middle of the player's screen, then something happens.

```java
Countdown.seconds(5)
        .colors(0xE8C872, 0xD83A4A)                      // the numbers, and the last one (the "1"). 0xRRGGBB
        .onSecond(left -> Spells.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.6f - left * 0.2f))
        .keepWhile(() -> target.isAlive() && player.distanceToSqr(target) < 32 * 32)
        .onFinish(() -> Spells.strike(player, target, 60f))
        .onCancel(() -> player.sendSystemMessage(Component.literal("It slipped away.")))
        .start(player);
```

| Part | What it does |
|---|---|
| `Countdown.seconds(n)` | A countdown from `n`. Nothing happens until `.start(player)` |
| `.color(rgb)` / `.colors(rgb, lastRgb)` | Color of the numbers; the second form gives the final "1" its own |
| `.silent()` | Counts without showing numbers (when the ability shows a picture instead) |
| `.onSecond(left -> ...)` | Runs each time a number shows: `n`, ..., 2, 1 |
| `.onTick(ticksLeft -> ...)` | Runs every tick while it counts |
| `.keepWhile(() -> ...)` | Checked every tick. The moment it is `false`, the countdown is cancelled |
| `.onFinish(() -> ...)` | It reached 0 |
| `.onCancel(() -> ...)` | It stopped early: `keepWhile` failed, the player died or left, or `Countdown.cancel(player)` |
| `.start(player)` | Starts it |

`Countdown.isRunning(player)`, `Countdown.ticksLeft(player)` and `Countdown.cancel(player)` work from anywhere.

Good to know:

- A player has **one countdown at a time**. Starting another cancels the one that is running. If your ability should not interrupt itself, begin with `if (Countdown.isRunning(player)) return;`.
- Build a new one for every cast. Do not keep one in a constant: it holds its own count.
- It is not saved. A server restart in the middle of one simply drops it.

### HudTimer: a small clock next to the hotbar

A word and the time that is left, to the right of the hotbar: `Revive` over `27s`. For anything that runs for a while and that the player should be able to keep an eye on: a ward, a buff, a transformation, a cooldown.

```java
HudTimer.show(player, "my_pathway_ward", "Ward", 600, 0x9CFFD2);     // 30 seconds, in this color (0xRRGGBB)
HudTimer.hide(player, "my_pathway_ward");                            // it ended early
```

- The first text is its **name**. Showing a timer whose name is on screen already starts it again (new time, new word), so call `show` again to make it longer. Timers with different names stand above one another. Start the name with your Pathway.
- It only **shows** time. It counts down on the player's screen by itself and goes away at 0, and nothing happens when it does. What the time is for is yours to do (`DeathWard`, `Flight`, `Zone`, `Later.run`...). Remember to `hide` it when the thing ends early.
- Its last five seconds are red. Under a minute it reads `27s`, from a minute on `1:05`.
- Only that player sees it. It is gone when they log out.

`Countdown` or `HudTimer`? `Countdown` is loud, sits in the middle of the screen and runs your code at the end. `HudTimer` is quiet, sits by the hotbar and only shows.

### HudCounter: a small counter next to the hotbar

A picture, a word and a number out of a cap, to the left of the hotbar: `[sigil] Order 4/9`. For anything a Pathway counts and the player should keep an eye on, when a number reads better than a row of pictures (the Prince Of Abolition's standing orders, charges, stacks).

```java
HudCounter.show(player, "my_pathway_charges", "my_pathway/charge", "Charges", 3, 5, 0xE0B04A);   // picture under textures/gui/, "" = none
HudCounter.hide(player, "my_pathway_charges");
```

- The first text is its **name**: showing it again with the same name updates it, so call `show` whenever the number changes. Counters with different names stand above one another. Start the name with your Pathway.
- `max` 0 or less shows the number alone (`Charges 3`). The color is the number's (0xRRGGBB); the word is grey.
- It stays until you `hide` it or the player logs out. It only **shows**: what the number does is yours.
- `HudTimer` is on the right of the hotbar and counts down by itself; `HudCounter` is on the left and changes only when you send it.

---

## Zone: ground that keeps doing something

A round piece of ground that acts, again and again, on whoever stands in it, and knows how long each of them has been there. Cursed land, a healing circle, a storm, a slowing field, an aura around a player.

```java
Zone.at(ground, 8)                                       // its middle on the ground, and how far it reaches
        .height(5)                                       // how far above the ground it still catches someone
        .lasts(300)                                      // 15 seconds
        .every(10)                                       // it acts twice a second
        .onInside((enemy, ticksInside) -> {
            int depth = Math.min(5, 1 + ticksInside / 40);               // deeper every 2 seconds they stay
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, depth - 1, 60);
        })
        .onPulse(ticksLeft -> ParticleShapes.ring(sl, MIST, ground.add(0, 0.2, 0), 8, 28, 0))
        .open(player, sl, "my_pathway_land");
```

| Part | What it does |
|---|---|
| `Zone.at(ground, radius)` | A zone lying on the ground. Nothing happens until `.open(...)` |
| `Zone.around(entity, radius)` | A zone that goes with an entity (an aura). Its middle is always at that entity's feet. It ends when the entity dies |
| `.height(blocks)` | How far above its ground it still catches someone (default 4). It always reaches 1.5 blocks below, for slopes |
| `.lasts(ticks)` | How long it stays (default 200) |
| `.every(ticks)` | How often it acts (default 10) |
| `.affects(entity -> ...)` | Who it acts on, instead of the owner's enemies. The owner is never one of them |
| `.countsTo(ticks)` | The time inside is not counted past this |
| `.onEnter(entity -> ...)` | Someone stepped in (also those in it when it opens) |
| `.onInside((entity, ticksInside) -> ...)` | Every beat, for each one in it. `ticksInside` is 0 when they have just come in |
| `.onLeave(entity -> ...)` | Someone stepped out, or the zone ended with them in it |
| `.onPulse(ticksLeft -> ...)` | Every beat, once: the look and the sound of the zone. The first time at once |
| `.onEnd(() -> ...)` | It is over, however it ended |
| `.open(player, sl, name)` | Opens it |

`Zone.isOpen(player, name)`, `Zone.ticksLeft(player, name)`, `Zone.ticksInside(player, name, entity)` and `Zone.close(player, name)` work from anywhere. So do these, for questions asked outside the zone's own beat (a hurt event, a wheel entry's cost):

| Call | What you get |
|---|---|
| `Zone.covers(player, name, spot)` | `true` if the spot is in that zone right now: within its radius across, and between 1.5 blocks under its ground and its height above. "Is the attacker standing in my field?" (Entropy's swing, `PrinceOfAbolitionSovereign.entropySwing`) |
| `Zone.centerOf(player, name)`, `radiusOf`, `ticksOpen` | Where its middle is now (an aura moves), how far it reaches, how long it has been open (a field that grows stronger the longer it stands) |
| `Zone.addShelter((zoneOwner, entity) -> ...)` | Once, in a static block: while it says `true`, no zone of `zoneOwner` acts on `entity` (it is skipped as if it stood outside). A pocket out of the world, a sanctuary no field reaches (the Black Emperor's Abolition) |
| `Zone.extend(player, name, ticks)` | It lasts that much longer (a second layer of a Prohibition). `false` if there is none |
| `Zone.closeNear(sl, spot, radius, owner -> ...)` | Ends every zone whose middle is within `radius` of `spot` and whose owner the test names; returns how many. `Decree`s are zones, so they end too (Quell Disorder: `owner -> !Spells.isFriend(player, owner)`) |

Good to know:

- **Worse the longer they stay.** `ticksInside` grows while someone stays, so `1 + ticksInside / 40` is a depth that goes up every 2 seconds. Stepping out does not wipe it: it falls back as fast as it grew, so hopping out and in again does not help. `.countsTo(200)` stops the count at 200, so it starts falling from there the moment they leave.
- **Who it acts on.** The owner's enemies, as every other spell. A zone for allies: `.affects(entity -> !Spells.isEnemy(player, entity))`. The owner themself is never in it; do the owner's part in `onPulse`.
- **The name** tells one player's zones apart. Opening one with a name that player already has open closes the old one first (its `onEnd` runs), so casting again moves the zone. Start the name with your Pathway.
- Build a new one for every cast. Do not keep one in a constant: it holds its own count.
- It ends by itself when its owner dies, logs out or changes dimension. It is not saved: a server restart drops it.
- A zone has no look of its own. Draw it in `onPulse`, or play models when you open it. If it can end early, give those models a tag of their own and clear them in `onEnd` (see [tags](#6-several-models-on-one-entity-tags)).
- Put potion effects on with `Spells.keepEffect`, not `addEffect`: a wither that is started afresh every half second never gets to hurt.

---

## Trap: a hidden spot that springs once

A spot on the ground that waits and goes off under the first enemy to step on it. A snare, a mine, a tripwire, a rune.

```java
Trap.at(ground, 1.5)                                     // its middle on the ground, and how far it reaches
        .armsAfter(20)                                   // it cannot spring during its first second
        .lasts(1200)                                     // gone after a minute if nobody stepped on it
        .onSpring(victim -> {
            Spells.strike(player, victim, 40f);
            Root.apply(victim, 60, RootRestriction.MOVEMENT);
        })
        .onWait(ticksLeft -> ParticleShapes.ring(sl, GLINT, ground.add(0, 0.1, 0), 0.9, 6, 0))
        .set(player, sl, "my_pathway_trap", 3);          // this player may have 3 of these at once
```

| Part | What it does |
|---|---|
| `Trap.at(ground, radius)` | A trap lying on the ground. Nothing happens until `.set(...)` |
| `.height(blocks)` | How far above its ground it still catches someone (default 2) |
| `.armsAfter(ticks)` | How long after it was set it can spring (default 20). 0 = at once, also under someone standing there |
| `.lasts(ticks)` | How long it waits before it is gone (default 1200) |
| `.catches(entity -> ...)` | Who springs it, instead of the owner's enemies. The owner never does |
| `.onSpring(victim -> ...)` | Runs once, with the one who stepped on it. The trap is gone by then |
| `.onWait(ticksLeft -> ...)` | Runs every `showEvery` ticks while it lies armed: a glint, a hum |
| `.showEvery(ticks)` | How often `onWait` runs (default 20) |
| `.onExpire(() -> ...)` | It ended without springing: its time is up, a newer trap took its place, or its owner died or left |
| `.set(player, sl, name, max)` | Sets it. `max` is how many traps with this name the player may have at once |

`Trap.count(player, name, max)` and `Trap.clear(player, name, max)` work from anywhere. `Trap.hiddenNear(sl, center, radius)` gives where every trap (anyone's, of any kind) lies near a spot, for a sight that sees what is hidden (Knight Of Misfortune's Spirit Vision).

Good to know:

- **One more than `max` replaces the oldest** (its `onExpire` runs). Always give the same `max` for the same name. Start the name with your Pathway.
- **Only the one who stepped on it is handed to `onSpring`.** To catch everything standing on the trap, ask `Spells.enemiesAround(player, sl, ground.add(0, 0.5, 0), radius)` there (`springTrap` in `Conqueror.java`).
- **It has no look of its own**, so that it can stay hidden. Draw the setting of it when you set it (`SpellFx.warningCircle` for as long as it takes to arm) and what gives it away in `onWait`.
- Animals are enemies like everything else that is not on the caster's side, so a cow springs it too. `.catches(...)` narrows that.
- Build a new one for every cast. Do not keep one in a constant: it remembers whether it sprang.
- It is a `Zone` underneath (`set` returns that zone's name): it ends when its owner dies, logs out or changes dimension, and a server restart drops it.

Something that goes off after a time instead of under a foot needs no trap: `Later.run(sl, ticks, () -> ...)` with a `SpellFx.warningCircle` of the same length (`plantCharge` in `Conqueror.java`).

---

## Dome: a shell that holds a line

A shell round a spot that things cannot cross the wrong way: enemies are kept out (or in), their projectiles end at it, and allies inside take less harm. A dome of light, a barrier, a cage, the walls of a kingdom. Used by Hand Of God's Protection and Cage (`protection`, `cage` in `HandOfGod.java`).

```java
Dome.at(ground, 6)                                       // a half sphere on the ground (Dome.sphere(middle, r): a whole one)
        .lasts(300)
        .keepsEnemiesOut()                               // enemies cannot come in; those inside when it rises are put out
        .stopsProjectiles()                              // projectiles of enemies (or of nobody) end at the shell
        .shields(0.6f)                                   // allies inside take 60% less damage (not the owner)
        .wardsEffects()                                  // no harmful potion effect can be put on an ally inside
        .holdsOwner()                                    // the owner cannot leave it
        .onBlock(at -> HandOfGodFx.domeHit(sl, at))      // something was stopped, at this spot on the shell
        .onEnd(() -> HandOfGodFx.protectionEnd(sl, ground, tag))
        .raise(player, sl, "my_pathway_dome");
```

| Part | What it does |
|---|---|
| `Dome.at(ground, radius)` / `Dome.sphere(middle, radius)` | Half a sphere on the ground, or a whole one (it may reach into the ground). Nothing happens until `.raise(...)` |
| `.lasts(ticks)` | How long it stands (default 200) |
| `.keepsEnemiesOut()` | Outsiders inside are put out at the shell, those outside cannot come in |
| `.keepsEnemiesIn()` | Outsiders cannot cross either way: those inside stay in, those outside stay out (a cage) |
| `.stopsProjectiles()` | A projectile of an outsider, or of nobody, that would cross the shell ends there (with `keepsEnemiesIn` also on its way out). Allies' projectiles go through |
| `.shields(fraction)` | Allies inside take that much less damage. The owner is not shielded |
| `.wardsEffects()` | No harmful potion effect can be put on an ally (or the owner) inside |
| `.holdsOwner()` | The owner cannot leave it |
| `.outsiders(entity -> ...)` | Who is kept out or in, instead of the owner's enemies. `entity -> !Spells.isFriend(player, entity)` also keeps out concealed enemies |
| `.keepWhile(() -> ...)` | Checked every tick: it falls the moment this is false (the owner walked away) |
| `.onBlock(at -> ...)` | Runs with the spot on the shell where a projectile was stopped or someone was put back |
| `.onEnd(() -> ...)` | It fell, however it ended |
| `.raise(player, sl, name)` | Raises it. One of the same name and player falls first |

`Dome.isRaised(player, name)`, `ticksLeft`, `center` (the ground under it, or the middle of a sphere), `isInside(player, name, entity)`, `close`, `weaken(player, name, ticks)` (projectiles go through and the shield is halved), `isWeakened`, and `renew(player, name)` (its whole time again) work from anywhere.

Anyone's domes, for powers that act on other people's shells:

| Call | What it does |
|---|---|
| `Dome.raisedAt(sl, spot)` | Every dome whose inside holds that spot, as `Dome.Raised(owner, name, center, radius, whole)` |
| `Dome.crossedBy(sl, from, to)` | Every dome whose shell the line passes through, nearest first: what a player aims at when they aim at a shell. `Dome.crossedBy(sl, player.getEyePosition(), Spells.aimPoint(player, sl, 24))` |
| `Dome.flip(owner, name)` | Turns it inside out: from now on it is a cage for its own owner and the owner's side (neither they nor their projectiles cross it), it no longer shields or wards anyone, and everyone else crosses freely. It keeps its time and its look. `false` if there is none or it was flipped already; `Dome.isFlipped(owner, name)` asks. Duke of Entropy's Distort (`PrinceOfAbolition`) |

Good to know:

- **Crossing is checked every tick** from where each one is, so walking, being knocked back, an ender pearl or any other teleport all count. Someone put back stands just on their own side of the shell and is pushed off it. Creative players and riders (their mount is moved instead) are left alone.
- **Who is who:** outsiders are the owner's enemies unless `.outsiders(...)` says otherwise. Everyone else is an ally and crosses freely both ways.
- A shot that gets through between two ticks still does not land on someone sheltered when its shooter stands outside (`LivingAttackEvent`).
- **It has no look of its own.** Draw it when you raise it (`HandOfGodFx.dome` and `HandOfGodFx.sphere` draw a shell of light tiles in any color) and clear the look in `onEnd`.
- It falls when its owner dies, logs out or changes dimension. Build a new one for every cast. Not saved: a server restart drops it.

---

## Wall: a flat barrier in the air

The flat cousin of `Dome`: a wall standing in the air that enemies cannot cross, either way, and whose plane their projectiles end at. A gate slammed shut, a curtain of fire, a line of shields, a ward across a corridor. Used by the Prince Of Abolition's Seal from Earl of the Fallen (`gate` in `PrinceOfAbolition.java`).

```java
Wall.at(middle, player.getYRot(), 10, 5)                 // the middle of its bottom edge, the way its face looks, width, height
        .lasts(200)
        .stopsProjectiles()                              // projectiles of enemies (or of nobody) end at it
        .onBlock(at -> MyFx.ripple(sl, at))              // something was stopped, at this spot on the wall
        .onEnd(() -> MyFx.wallEnd(sl, middle, tag))
        .raise(player, sl, "my_pathway_wall");
```

| Part | What it does |
|---|---|
| `Wall.at(middle, yaw, width, height)` | A wall whose bottom edge's middle is `middle`. `yaw` is a direction like a player's: `player.getYRot()` stands it across the player's view, face toward them. Nothing happens until `.raise(...)` |
| `.lasts(ticks)` | How long it stands (default 200) |
| `.stopsProjectiles()` | A projectile of an outsider (or of nobody) whose next step goes through the wall ends there, both ways |
| `.outsiders(entity -> ...)` | Who cannot cross, instead of the owner's enemies. The owner always can |
| `.onBlock(at -> ...)` | Runs with the spot on the wall where someone was put back or a projectile stopped |
| `.onEnd(() -> ...)` | It fell, however it ended |
| `.raise(player, sl, name)` | Raises it. One of the same name and player falls first |

`Wall.isRaised(player, name)`, `ticksLeft`, `close` and `renew` (its whole time again) work from anywhere.

Good to know:

- **Crossing its plane is what counts**, checked every tick, so walking, being knocked through or a teleport across all count; someone put back stands just on their own side and is pushed off it. Its ends and its top are open: going round or over is allowed.
- It has no thickness and no look of its own. Draw it when you raise it (`PrinceOfAbolitionFx.gate` draws a lattice 10 wide and 5 tall) and clear the look in `onEnd`.
- It falls when its owner dies, logs out or changes dimension. Build a new one for every cast. Not saved.

---

## Orbit: bodies that circle and guard

A few small bodies circling a player for a while. Each can stop one hostile projectile (and is back some time later), and one of them strikes an enemy that comes too close every so often. Guarding stars, orbiting blades, motes of light, a ring of shields. Knowledge Emperor's Star Guard (`KnowledgeEmperorStars.starGuard`).

```java
String tag = "my_pathway_guard_" + player.getId() + "_" + sl.getGameTime();
MyFx.guard(sl, player, 600, tag);                                   // the models, put down once
Orbit.around(player, 4)                                             // four bodies
        .radius(1.2).height(1.1).speed(6f)                          // how far out, how high above the feet, degrees a tick
        .lasts(600)
        .catchesProjectiles(100)                                    // each stops one, and is back 5 seconds later
        .strikes(3.0, 40, (enemy, index) -> Spells.strike(player, enemy, 15f))   // within 3 blocks, every 2 seconds
        .onTick(age -> MyFx.guardTick(sl, player, tag, age))        // every tick: move the models (ParticleShapes.moveModel)
        .onCatch((index, projectile, at) -> MyFx.blocked(sl, tag, index, at))   // take that body's model away
        .onBack((index, ticksLeft, age) -> MyFx.back(sl, tag, index, ticksLeft, age))   // put it back
        .onEnd(() -> MyFx.guardEnd(sl, player, tag))
        .open(player, sl, "my_pathway_guard");
```

| Part | What it does |
|---|---|
| `Orbit.around(player, count)` | `count` bodies round a player. Nothing happens until `.open(...)` |
| `.radius(blocks)`, `.height(blocks)`, `.speed(degrees)` | Where the ring is: body `i` sits at `age * speed + i * 360 / count` degrees round the feet, `radius` out and `height` up (`spot(i, age)`). Use the same three numbers in the look and it lines up |
| `.lasts(ticks)` | How long (default 200) |
| `.catchesProjectiles(returnTicks)` | A projectile of anyone not on the owner's side (or of nobody) within `radius + 1` of the owner is stopped by the ready body nearest to it, which is away for `returnTicks`. A hit by a projectile too fast to be seen first is cancelled the same way |
| `.strikes(range, everyTicks, (enemy, index) -> ...)` | Every `everyTicks`, the nearest enemy within `range` of the owner's body is struck by the ready body nearest to it (your code does the damage) |
| `.onTick(age -> ...)`, `.onCatch(...)`, `.onBack(...)`, `.onEnd(...)` | The look. It draws nothing itself |
| `.open(player, sl, name)` | Opens it; one with the same name is closed first |

`Orbit.isOpen(player, name)`, `ticksLeft`, `ready` (bodies there now), `age` (what `onTick` was last given), `firstReady` (a body that is there, or -1), `close`. It ends when its time is up or its owner dies, logs out or changes dimension. Not saved.

**Sending one away yourself.** `Orbit.release(player, name, index, awayTicks)` launches body `index`: it returns where the body was (fire a `Shot` from there), or `null` if it is away already, and the body is back `awayTicks` later through `onBack`, or never in this orbit with `0`. The orbit draws nothing, so take its model away yourself. The Star Dragon's asteroid (`StarDragonForms`, thrown on a blow, back 3 seconds later) and Fusion Cluster (`StarDragonSky`, all of them launched after a second of circling):

```java
int i = Orbit.firstReady(player, "my_pathway_rock");
Vec3 from = i < 0 ? null : Orbit.release(player, "my_pathway_rock", i, 60);     // back in 3 seconds
if (from != null) ROCK_SHOT.onHit((target, at) -> Spells.strike(player, target, 40f)).fire(player, sl, from, way);
```

---

## GravityField: gravity gone wrong

A round piece of ground, or an aura round someone, where gravity is not what it should be. It is a `Zone` underneath (its beat, time, owner and name, `Zone.ticksLeft`, `Zone.covers`...), with the pulling, pressing and lifting done for you. Star Dragon's Gravity Abnormality, Heavybringing, Time Dilation, the Neutron Star and the Singularity (`StarDragonGravity`).

```java
GravityField.at(ground, 8)                        // on the ground; GravityField.around(player, 16) goes with them
        .lasts(100).every(4)                      // 5 seconds, five times a second
        .presses(2)                               // Heavy, level 2 (status/Weight), while they stay
        .bossesOnlySlowed(Spells::isBoss)         // these are only slowed, never lifted or pulled
        .onInside((enemy, ticksInside) -> ...)    // anything more, every beat
        .onPulse(ticksLeft -> MyFx.tick(sl, ground))
        .onEnd(() -> MyFx.end(sl, ground))
        .raise(player, sl, "my_pathway_gravity");
```

| Part | What it does |
|---|---|
| `GravityField.at(ground, radius)` / `around(entity, radius)` | On the ground, or round an entity's feet wherever it goes |
| `.height(blocks)`, `.lasts(ticks)`, `.every(ticks)`, `.affects(entity -> ...)` | As for `Zone` (default beat 5) |
| `.presses(level)` | Heavy at that level (`Weight`) while inside and a moment after |
| `.lifts(blocks)` | Lifted that high off the ground under them and held there (no gravity) while inside |
| `.liftsFor(entity -> ticks)` | How long each one is held up, counted in ticks inside: `one -> Spells.holdLength(one, 100)` |
| `.slamsDown(speed)` | A lifted one is thrown down at that speed when it leaves or the field ends, instead of being set down gently (its fall hurts) |
| `.pulls(speed)` | Drawn toward the middle, at most that many blocks a tick (through `Spells.pull`) |
| `.slows(amplifier)` | Slowness while inside |
| `.bossesOnlySlowed(entity -> ...)` | Those are only slowed (Slowness II at least): never pressed, lifted or pulled |
| `.onEnter`, `.onInside`, `.onLeave`, `.onPulse`, `.onEnd` | As for `Zone` (`onLeave` runs after a lifted one was put down or slammed) |
| `.raise(player, sl, name)` | Raises it. One of the same name closes first |

`GravityField.isRaised(player, name)` and `GravityField.close(player, name)`. Allies and the owner are never pressed, pulled or lifted. Whatever `Spells.addMoveImmunity` holds is neither pulled nor lifted. A lifted one put down gently takes no damage from the drop. Build a new one for every cast.

## Ray: a beam at light speed

An instant beam from a point along a direction. It cannot be dodged once it fires, so it shows where it will go first and fires a few ticks later along that same line. `Shot` is for things that fly and can be seen coming; a `Ray` is there at once. Star Dragon's Weakening, Piercing, Sunlight and Series of Rays (`StarDragonRays`).

```java
Ray.from(player.getEyePosition(), player.getLookAngle())
        .range(30).width(0.3)                    // how far, how fat its line is for hitting
        .warning(8)                              // the line shows 8 ticks first (0: at once)
        .onWarn((from, to) -> MyFx.aim(sl, from, to))
        .onFire((from, to) -> MyFx.beam(sl, from, to))
        .onHit((target, at) -> Spells.strike(player, target, 40f))
        .fire(player, sl);                       // returns the ticks until it fires
```

| Part | What it does |
|---|---|
| `Ray.from(from, direction)` | A ray starting there, along that direction |
| `.range(blocks)`, `.width(blocks)` | How far it reaches, how close to its line a body must be to be hit |
| `.warning(ticks)` | How long its line shows before it fires |
| `.pierces()` | Through every enemy on its line (each hit once); otherwise it stops at the first |
| `.throughBlocks()` | Walls do not stop it |
| `.along(from, direction)` | The same ray from another start or direction (a fan: `Spells.turned`) |
| `.onWarn((from, to) -> ...)`, `.onFire((from, to) -> ...)` | Its looks: the aim line, and the beam the moment it fires (`to` is where it stopped) |
| `.onHit((target, at) -> ...)`, `.onEnd(end -> ...)` | Each one it hits; where it ended |
| `end(player, sl)` | Where its line ends now (the first block, or its range) |
| `.fire(player, sl)` | Fires it |

The line is fixed at the cast; who is on it is worked out when it fires, so stepping off the line in time saves you. Every method returns a new copy, like `Shot`, so a `Ray` in a constant is safe. Nothing happens if the caster died, left or changed dimension before it fired.

## Tellings: less each time

A player's use history of something, kept for a while and turned into a multiplier. The Knowledge Emperor's Obscurity (`KnowledgeEmperorRetellings.tell`): a story told often loses its power.

```java
float strength = Tellings.strength(player, "my_pathway_tale", 6000, 0.2f, 0.4f);   // 1, less 0.2 for each telling in the
                                                                                 // last 5 minutes, never under 0.4
Tellings.tell(player, "my_pathway_tale");                                        // one more, now
int told = Tellings.count(player, "my_pathway_tale", 6000);
```

Ask `strength` before you `tell`, so the first is at full power. `sinceLast(player, key)` (ticks, `Integer.MAX_VALUE` for never) and `forget(player, key)`. Kept in the player's persistent data, so it survives a relog. A key may hold anything but commas.

---

## History: where a player was

Where every player stood, which way they looked and how much health they had, over the last 10 seconds (`KEEP_TICKS`), written every 2 ticks (`EVERY`). For anything that goes back in time: a step back, a wound undone, an echo that walks your path again. The Illuminator's Recall (`Illuminator.recall`).

```java
History.Moment then = History.asFarAs(player, 60);      // 3 seconds ago, or as far back as it goes (null: nothing yet)
if (then != null && Spells.fits(player, sl, then.position())) {
    float lost = Math.max(0f, then.health() - player.getHealth());
    player.teleportTo(then.position().x, then.position().y, then.position().z);
    player.heal(lost * 0.5f);
    History.forget(player);                              // a step back cannot be stepped back again
}
```

`ago(player, ticks)` (exactly that far back, or `null`), `asFarAs(player, ticks)`, `healthLost(player, ticks)`, `forget(player)`. A `Moment` is `position`, `yRot`, `xRot`, `health`, `tick`, `dimension`. Only players are followed; dying, logging out or changing dimension starts the record afresh. Not saved.

## Disorder: how much order is bent

`Disorder.of(entity)` is a score from 0 up, worked out each time it is asked: +1 for each of Concealment, a Substitute or DeathWard armed, a DamageLink, any root, being a marionette or summon; for a player +1 for each marionette they hold and each ability they cast in the last 10 seconds (`Residue`, at most 3). The Hand Of Order's Sword of Judgment (it strikes the most disorderly hardest), Disorder Sense and Recognition.

```java
int disorder = Disorder.of(target);
Disorder.addSource(entity -> MyStatus.isOn(entity) ? 1 : 0);      // once, in a static block: one more thing that counts
```

Each Violation of a `Decree` it stands in counts +1 too (at most `Disorder.VIOLATIONS_MAX`, 3).

## Decree: ground where Rules are in force

A round piece of ground where Rules hold the owner's enemies (never the owner or their allies). What a Rule forbids is refused there, and every try is a **Violation**: counted one by one per Decree, so the owner can punish each one harder than the last. The Hand Of Order's Prohibitions, Imperative: Reality, Law: No Return and Court of Judgment.

```java
Decree.at(ground, 14)                                    // its middle on the ground, and how far it reaches
        .forbid(Decree.Rule.PASSAGE, Decree.Rule.CONCEALMENT)
        .lasts(240)                                      // ticks (default 300)
        .height(8)                                       // how far up it still holds (default 6)
        .binds(enemy -> !isStronger(enemy))              // of the owner's enemies, only these (default: all)
        .onViolation((who, rule, count, sl) -> {         // count = its 1st, 2nd, 3rd... Violation in this Decree
            if (count >= 2) Spells.strikeTrue(player, who, 30f * count);
        })
        .onEnter(enemy -> ...)                           // one it binds stepped in
        .onPulse(40, ticksLeft -> MyFx.decreeTick(...))  // every 40 ticks, once: its look
        .onEnd(() -> MyFx.decreeEnd(...))
        .write(player, sl, "my_pathway_decree");
```

| Rule | What it stops for those it binds, while they stand in it |
|---|---|
| `PASSAGE` | Teleports (ender pearls, chorus fruit, every ability that asks `Root.has(.., TELEPORT)`: Flaming Jump, the Sword of Judgment...), flying, `Flight`, `Glide`, elytra, levitation |
| `CONCEALMENT` | `Concealment.hide` is refused; anyone hidden inside is pulled out (`Exposed`) |
| `STAND_INS` | `Substitute.arm` is refused, and one armed before does not take the hit inside |
| `RESURRECTION` | `DeathWard.arm` is refused, an armed ward does not save them, a totem of undying does not work |
| `MARIONETTES` | Their marionettes stand frozen inside; `Marionette.control` is refused |
| `HEALING` | No healing at all |
| `PROJECTILES` | Their projectiles end at the edge, or as they are shot |
| `ABILITIES` | A player's ability keys refuse (`Decree.blocksKey` in the key packets); a mob's special attacks are locked (`AbilityLock`) |
| `REALITY` | Not a ban but a weight, on **everyone** inside, allies too: ability damage `Decree.REALITY_ABILITY` (0.5), plain weapon, arrow and fall damage `REALITY_PLAIN` (1.25), abilities cost `REALITY_COST` (1.5). The owner's own casts are spared |

While it is written: `Decree.isWritten`, `ticksLeft`, `close`, `extend(player, name, ticks)`, `forbid(player, name, rule)` (one more Rule, now: a Court writing its clauses), `rules`, `centerOf`, `radiusOf`, `violationsIn(player, name, entity)`. From anywhere:

```java
if (Decree.violate(entity, Decree.Rule.STAND_INS)) return;   // a status of your own: refused here, and the Violation counted
Decree.forbids(entity, Decree.Rule.PASSAGE)                  // only asks; nothing is counted
Decree.violations(entity)   Decree.inReality(entity)
Decree.addImmunity((owner, entity, rule) -> ...)            // once, in a static block: some cannot be bound
```

- The same Rule broken again by the same one within `Decree.VIOLATION_GAP` (20 ticks) is one Violation, so a heal every tick or a held key does not climb the ladder at once.
- `onViolation` runs inside whatever caught the try (a heal, a teleport, an attack). Put damage a tick later (`Later.run(sl, 1, ...)`), as `HandOfOrderDecrees.punish` does.
- `PASSAGE` holds through `Root.addHold`: no root ring, nothing counted toward `Disorder` as a root.
- It is a `Zone` underneath: writing one with a name the owner has closes the old one first, it ends with its owner, and `Zone.closeNear` ends it. Not saved.

## Residue: where abilities were cast

Every time a player spends spirituality on an ability (`SoulCore.setSpirituality` going down: `SoulCore` writes it itself), a cast is written where they stand: who, their Pathway and Sequence, where, when. At most one a second for each player, kept a minute (`KEEP_TICKS`), not saved.

```java
for (Residue.Cast cast : Residue.near(sl, center, 64, 1200)) {   // newest first
    MyFx.trace(viewer, cast.at(), DarkAngel.pathwayColor(cast.pathway()));
}
```

`Cast` is `caster`, `pathway`, `sequence`, `at`, `dimension`, `tick`, and `age(sl)`. The Illuminator's Survey from Arcane Scholar.

## Cooldowns: for single keys

The wheels keep their own cooldowns; a single ability key can have one too, with the wheels' message.

```java
if (!Cooldowns.ready(player, "my_pathway_flask", "Compound Flask")) return;   // "Compound Flask is ready again in 1.5s."
if (!Spells.paySpirituality(player, 250)) return;
Cooldowns.start(player, "my_pathway_flask", 40);
```

`left(player, key)` (ticks, 0 = ready), `reset(player, key)`, `seconds(ticks)` ("1.5s"). `Cooldowns.addScale(player -> 1.5f)` (once, in a static block) makes every cooldown a player starts longer or shorter while it says so, single keys and every wheel alike (`AbilityWheel.cooldownFor` asks `Cooldowns.scaled`): the enemies in a Mechanical Heaven. Check it with the other "can it be cast" checks and start it after the cast went off. Saved on the player in game time. The Illuminator's keys 1 to 3 use it.

## Contagion: it passes from one enemy to the next

Something one enemy carries for a while and then passes on to the enemies nearest to it, which carry it and pass it on in their turn, stronger each time. A plague, a curse that spreads, a fire that leaps from body to body.

```java
Contagion.spread()
        .lasts(100)                                       // each one carries it for 5 seconds
        .every(20, (victim, stage) -> Spells.strike(player, victim, 5f * (float) Math.pow(1.5, stage - 1)))
        .passesTo(2, 3)                                   // then it jumps to 2 or 3 of the nearest
        .reach(8)
        .generations(4)                                   // the 4th one to carry it passes it on no more
        .maxVictims(24)
        .travel(10)                                       // a jump takes half a second
        .onCatch((victim, stage) -> DemonessFx.plagueMark(sl, victim, 100, stage, "my_pathway_plague"))
        .onRunOut((victim, stage) -> DemonessFx.plagueBloom(sl, victim))
        .onPass((from, to, stage) -> DemonessFx.plagueJump(sl, from, to))
        .start(player, sl, target);
```

| Part | What it does |
|---|---|
| `Contagion.spread()` | A new one. Nothing happens until `.start(...)` |
| `.lasts(ticks)` | How long one carries it before it passes on (default 100) |
| `.every(ticks, (victim, stage) -> ...)` | What it does to whoever carries it, every `ticks` ticks (the first time `ticks` after they caught it) |
| `.passesTo(count)`, `.passesTo(min, max)` | To how many new ones it passes each time, nearest first (default 1) |
| `.reach(blocks)` | How far it can jump (default 6) |
| `.generations(stages)` | The first one carries stage 1, every jump adds one; the one who carries the last stage keeps it (default 3) |
| `.maxVictims(count)` | How many can catch this outbreak in all (default 12) |
| `.travel(ticks)` | How long a jump takes; the new one catches it then (default 10) |
| `.notOnDeath()` | One who dies while carrying it takes it with them. Without this it jumps on from where the body fell |
| `.onCatch`, `.onRunOut`, `.onPass` | Someone caught it (the first one too) / someone's time with it is up, right before it jumps on / it jumps |

Good to know:

- **`stage` is how it gets stronger**: 1 on the first one, 2 on the ones it jumped to, and so on. Scale your damage, the size of the mark, its color with it.
- **Nobody catches the same outbreak twice**, so it always moves outward. Two casts are two outbreaks: both can be on the same enemy.
- It stops when its owner dies, logs out or leaves the dimension. Build a new one for every cast; it is not saved.

Used in `plague` in `Apocalypse.java`.

---

## Fallen: the dead that can rise

Every mob that dies is written down for a while: what it was, where it fell, which way it faced, when. Players, bosses (`Fallen.MAX_HEALTH`, 150, or more) and summons that vanish are left out.

```java
for (Fallen.Body body : Fallen.near(sl, spot, 10, 200)) {        // fell within 10 blocks in the last 10 seconds, nearest first
    if (!Fallen.take(body)) continue;                             // taken: no other cast raises it
    showItRising(body.where());
    Later.run(sl, 14, () -> Fallen.rise(body, player, sl, 600));  // up again as what it was, fighting for the player
}
```

| Part | What it does |
|---|---|
| `Fallen.near(sl, center, radius, maxAgeTicks)` | The bodies that fell there lately, nearest first |
| `Fallen.take(body)` | Takes it off the list so nothing finds it again. `false` if something took it first |
| `Fallen.rise(body, master, sl, ticks)` | Spawns what it was where it fell (a skeleton as a skeleton, a baby as a baby) as a `Marionette.summon` of `master` for `ticks`: it fights for them and vanishes after, with no drops. `null` if it cannot (not a mob, no room, it never really died) |
| `body.type()`, `where()`, `yaw()`, `tick()`, `age(sl)`, `baby()`, `id()` | What was written down |

- Bodies are forgotten after `Fallen.KEEP_TICKS` (400). Nothing is saved: a restart forgets them all.
- Take a body when you start showing it rise and `rise` it when the look is done; `rise` takes it too if nobody did.
- A death that a `DeathWard` cheats is written down all the same, but `rise` sees that it is still up and does nothing.
- Used by Raise the Dead and Death Sentence (`raiseTheDead`, `sentenceFalls` in `AbominationZombie.java`).

---

## Hurled: loose things thrown

A loose thing in the world (a dropped item, an arrow, an armor stand, a boat) lifted and thrown at an enemy by a power: a poltergeist, a puppeteer's strings. Find them with `Spells.looseThingsAround`, then throw each one.

```java
List<Entity> things = Spells.looseThingsAround(player, sl, player.position(), 16);   // nearest first
for (int i = 0; i < things.size(); i++) {
    Hurled.at(things.get(i), player, enemy)
            .risesFor(12 + i * 2)                       // floats up first, one after another (default 0)
            .speed(1.4)                                 // blocks a tick (default 1.2)
            .damage(10f)                                // to the first enemy it touches (default 6)
            .lasts(60)                                  // ticks of flight before it gives up and falls (default 60)
            .onLaunch(thing -> MyFx.whoosh(sl, thing))
            .onTrail(thing -> MyFx.trail(sl, thing))    // every tick it flies
            .onHit((thing, victim) -> MyFx.thud(sl, thing.position()))
            .launch(sl);                                // false if it cannot be thrown (gone, already flying)
}
```

| Part | What it does |
|---|---|
| `Hurled.at(thing, owner, target)` | A new throw. Nothing happens until `.launch(sl)` |
| `.risesFor(ticks)` | It floats straight up this long before it flies |
| `.speed(blocks)`, `.damage(amount)`, `.lasts(ticks)` | How fast, how hard, and how long it may fly |
| `.onLaunch`, `.onTrail`, `.onHit` | It starts flying / every tick of the flight / it hit someone (after the damage) |
| `Hurled.isFlying(entity)` | Being thrown right now |

- **What it carries itself** flies straight at the target's middle every tick with no gravity, hits the first of the owner's enemies it touches (`Spells.strike`, so the hit is the owner's), bounces off and drops. It falls when it bumps into a wall, when its time is up, or when the target is gone. A dropped item cannot be picked up while it flies.
- **A projectile is different**: its owner becomes the thrower and it is shot at the target, then flies and hits as it always would (an arrow does an arrow's damage). `damage` and `onHit` are not used for it. An arrow stuck in a wall is pulled out first.
- Nothing here touches blocks. Not saved: if the server stops mid-flight, the thing falls where it was.
- Used by Animate Objects (`animateObjects` in `AbominationChains.java`).

---

## Reel: drawn along a line

An entity is drawn straight to a point, fast, the way a grappling line pulls: a player swinging to a ledge, a mob yanked in, everyone tangled dragged to the middle.

```java
FallGuard.protect(player, 200);                         // no fall damage when they land
Reel.in(player, () -> anchor)                           // the point, asked again every tick (null = let go)
        .speed(1.6)                                     // blocks per tick at most (default 1.4)
        .stopAt(1.2)                                    // close enough: arrived (default 1.2)
        .lasts(30)                                      // lets go after this long anyway (default 40)
        .onArrive(() -> Spells.sound(sl, anchor, SoundEvents.SPIDER_STEP, 1f, 1f))
        .start(sl);

Reel.in(mob, () -> Spells.casterStillHere(player, sl) ? player.position() : null).speed(1.2).start(sl);   // to a moving point
```

- It goes straight at the point, up and down too, slowing a little as it gets close. Every tick its fall counter is reset.
- It does not go through walls: it lets go once it has not moved for `Reel.STUCK_TICKS` ticks (6).
- It stops when the entity dies or leaves the level. `.onEnd(() -> ...)` runs however it ended.
- `Spells.pull` is the soft version: sideways only, call it again and again for a steady drag (a tornado drawing things in).

Used in `webSwing` and `drawInFate` in `Apocalypse.java`.

---

## Teleport: from here to there

The one place to move someone at once. It asks whether they may go (a Teleport root, or a hold such as a Decree against
passage), puts them there, takes the fall off and, if asked, ends the control on them. Every Key Of Stars door, hop and blink.

```java
Teleport.of(player).go(sl, there);                          // false (nothing happens) when rooted
Teleport.of(player).fallGuard(40).unbinding().go(sl, there);   // no fall damage for 2 s; mind control lets go (Unbound)
Teleport.of(player).ignoringTeleportRoots().go(sl, there);  // an Apprentice's Door Opening
Teleport.canGo(entity)                                      // ask first, so a refusal costs nothing
Teleport.fits(entity, sl, feet)                             // its body fits there (any entity, not only players)
Teleport.standNear(entity, sl, wanted, 3)                   // the nearest spot within 3 blocks where it fits, or null
Teleport.WallPass pass = Teleport.throughWall(player, sl, 6);   // the wall right ahead (no thicker than 6): face() and exit()
Teleport.addListener((entity, from, to) -> ...)             // once: told after every teleport made through here
```

`Spells.blinkSpot` (toward the crosshair) and `Spells.freeSpotNear` (to the side) find where to go.

## Transit: travel inside a shot

The caster becomes part of a `Shot` they throw, travels with it and steps out where it lands. A flame they merge with, a bolt they ride, a card they fold into.

```java
Transit.by(LANCE.onHit((target, at) -> Spells.strike(player, target, 30f)))   // any Shot; it still hurts what it hits
        .onDepart(from -> SpellFx.fireBurst(sl, from, 2.5))                  // the caster's feet where they left
        .onArrive((at, hitBlock) -> SpellFx.fireBurst(sl, at, 2.5))          // their feet where they came out
        .go(player, sl, hand, Spells.aimFrom(player, sl, hand, 20));
```

| Part | What it does |
|---|---|
| `Transit.by(shot)` | Travel inside this shot. Nothing happens until `.go(...)` |
| `.visible()` | The caster stays seen and can be hurt on the way (default: concealed) |
| `.steered(degreesPerTick)` / `.steered()` | They ride it: body and camera fly with the shot, and it turns toward where they look, at most that many degrees a tick (`.steered()` = `Transit.STEER_DEGREES`, 12; 180 = at once). See "Steered" below |
| `.letGoOnSneak()` | Steered: sneaking during the flight ends it where they are |
| `.onDepart(from -> ...)` | Runs right before they leave |
| `.onArrive((at, hitBlock) -> ...)` | Runs right after they came out. `hitBlock` is `true` when the shot ended on a block |
| `.go(player, sl, from, direction)` | Fires the shot and sends the caster with it. `false` (nothing fired) when they may not travel |
| `Transit.canGo(player)` | `false` while they are rooted with `TELEPORT`. Check it before paying, so a refused cast costs nothing |

Good to know:

- **On the way** the caster is concealed (unseen, unhurt, mobs forget them). Concealment ends when they come out.
- **Straight** (the default): the shot flies on its own, the caster is held where they stood, and they appear where it lands.
- **Where they come out** (straight) is never inside a block: a step back from where the shot stopped, else on the ground under it, else the nearest free spot. With no room at all they stay where they were (`onArrive` still runs). No fall damage for `Transit.LANDING_TICKS` (100) afterwards, since they may come out in the air at a wall.
- The shot keeps its `onHit`. Its `onEnd` is taken over: put the end into `onArrive`.
- A caster who dies or leaves on the way does not come out; `onArrive` does not run.

**Steered.** With `.steered(...)` nothing is fired ahead: the caster's body is the shot. Each tick it turns toward their look (`Spells.turnToward`), looks ahead for a block or an enemy, and gives the body the speed to get there, the way `Spells.push` does, so their own game moves them smoothly and the camera goes along. The shot's model is held in front of their eyes (`ParticleShapes.modelInFront`, `Transit.RIDE_AHEAD` 1.6 blocks ahead and `RIDE_BELOW` 0.45 under the line of sight) and turns with their look; they see it in first person, and while they are concealed the others see only it.

```java
Transit.by(LANCE.speed(1.2).range(30).onHit((target, at) -> Spells.strike(player, target, 30f)))
        .steered(10)                       // 10 degrees a tick: a full turn round in under a second
        .letGoOnSneak()                    // sneak to step out early
        .onArrive((at, hitBlock) -> SpellFx.fireBurst(sl, at, 2.5))
        .go(player, sl, hand, player.getLookAngle());   // 'hand' is not used when steered
```

- It flies at the shot's `speed` for as far as its `range`, measured along the way it really went. `Shot.range(Transit.UNLIMITED)` = until something stops it (at most `Transit.MAX_RIDE_TICKS`, an hour). Slower is easier to steer: 1 to 1.5 blocks a tick is a good start.
- It ends at a block, at the first enemy (`onHit` first; `.pierce()` goes on through), at the end of its range, when they are rooted (`MOVE` or `TELEPORT`), when they sneak with `.letGoOnSneak()`, and when they have been stuck for a few ticks (a step the look-ahead missed). They stop there: no landing spot is searched, they are where the flight brought them.
- The shot's `trail` is drawn along the way they flew, piece by piece. `windup` and `stick` are not used.

---

## 4. SpellFx: looks that many abilities share

Visual only. Nothing here hurts anyone. Colors are ARGB (`0xAARRGGBB`); the alpha is how strong it is.

| Call | Looks like |
|---|---|
| `SpellFx.warningCircle(sl, ground, radius, color, ticks)` | A turning rune circle: "something lands here" |
| `SpellFx.runeCircle(sl, ground, radius, color, ticks, spin)` | The same with your own turning speed (degrees per tick, negative = the other way) |
| `SpellFx.shockRing(sl, ground, radius, color, ticks, delay)` | A ring racing outward along the ground |
| `SpellFx.closingRing(sl, ground, radius, color, ticks, delay)` | A ring closing in on a point: something is drawn in |
| `SpellFx.steadyRing(sl, ground, radius, color, ticks, delay)` | A ring growing at one speed all the way. `Spells.wave` with the same radius and ticks hurts each enemy exactly when it touches them |
| `SpellFx.fireBurst(sl, center, radius)` | An explosion of fire: flash, ring, embers, smoke |
| `SpellFx.blast(sl, center, radius, color, spark)` | The same in a color of your own, throwing your own particles (`null` = none) |
| `SpellFx.risingRings(sl, ground, radius, height, count, color)` | Rings one above the other, each a little later and narrower: something rises, is lifted, comes back |
| `SpellFx.homing(sl, model, from, target, ticks)` | A model that flies into an entity and cannot miss (see below) |
| `SpellFx.circleUnder(sl, entity, radius, color, ticks, tag)` | A rune circle that stays under an entity and goes where it goes (see below) |
| `SpellFx.overHead(sl, entity, model, count, tag)` | A ring of small models over a head, one for each mark (see below) |
| `SpellFx.tether(sl, line, entityA, entityB, ticks)` | A line that keeps joining two entities |
| `SpellFx.tether(sl, line, () -> pointA, () -> pointB, ticks)` | The same between any two moving points |

Ready-made parts to build your own from: `SpellFx.RUNE_CIRCLE`, `SHOCK_RING`, `FIRE_BLAST`, `FLAMES` (models) and `FIRE_EMBER`, `FIRE_SMOKE` (particles).

### Models on an entity

**`homing`: something that cannot miss.** The model is stuck to the *target* and only its keys bring it in from where it started, so if the target runs, the model's whole way moves with it. It arrives after `ticks` ticks, slowly at first and then faster. The hit is yours to do:

```java
ModelFx ghost = GHOST.lifetime(18).fade(3, 6)
        .during(12, 18, ModelFx.pose().forward(1f));           // goes on through the target as it fades
SpellFx.homing(sl, ghost, from, target, 12);
Later.run(sl, 12, () -> {
    if (target.isAlive()) Spells.strike(player, target, 12f);
});
```

Give it a model whose own keys do not move it (frames, turning and fading are fine) and a lifetime a little longer than `ticks`. Its front faces the target and its pivot ends at the target's feet. Good for spirits and curses; for something that should be dodgeable use a `Shot` or an `AnimatedShot`.

**`overHead`: show a count.** One small model for each mark, in a ring over the entity's head. Call it again with the same tag and the ring is replaced; count 0 removes it. Give the model the lifetime of the marks and it vanishes with them:

```java
int hits = Marks.add(target, "my_pathway_hits", 10, 300);
SpellFx.overHead(sl, target, hits >= 3 ? FEATHER.color(0xFFFFC83C) : FEATHER, hits, "my_pathway_hits");
```

**`circleUnder`: a mark that follows.** A turning rune circle under an entity, for as long as you say: someone is cursed, warded, chosen. Call it again with the same tag and it is replaced, so a mark can grow and change color as what it stands for gets worse. `ParticleShapes.clearModels(sl, entity, tag)` takes it away early:

```java
SpellFx.circleUnder(sl, enemy, 0.8 + depth * 0.12, COLORS[depth - 1], ticksLeft, "my_pathway_curse");
```

Everyone sees it, also under a player's own feet. It is not drawn under someone who is concealed.

### Tethers

A tether is a `SlashFx.line` that is drawn again every 2 ticks between two points you describe. Both ends are asked for each time, so the line follows whatever they are attached to.

```java
private static final SlashFx CHAIN = SlashFx.line("slash/smooth")
        .color(0xC0FF4A5A).core(0xE0FFE0E0)              // outside and middle
        .width(0.06f).taper(SlashFx.Taper.UNIFORM);

SpellFx.tether(sl, CHAIN, player, target, 100);          // middle to middle, 5 seconds
```

**When an end returns `null`, the line is gone for good.** That is how you tie a tether to a condition:

```java
// from the caster's hand to the top of the target's head, for as long as the countdown runs
SpellFx.tether(sl, CHAIN,
        () -> Countdown.isRunning(player) ? SpellFx.handOf(player, sl) : null,
        () -> SpellFx.topOf(target, sl), 100);
```

`SpellFx.middleOf(entity, sl)`, `topOf(entity, sl)` and `handOf(player, sl)` give the usual ends. Each returns `null` once the entity is dead, gone or in another dimension, so a tether between two entities ends by itself.

Tips: `.translucent()` on the line makes dark colors possible (the default adds light, so black is invisible). A dark line with a pale `core` shows against the sky as well as in a cave. `"slash/dashed"` gives a stitched look.

---

## 5. Statuses

Each of these is one class in `status/` that looks after itself: it counts its own time, reacts to the right events and cleans up when the entity dies or leaves. You only call it.

### DamageLink

Makes damage go somewhere else. Two kinds.

**Redirect:** part of what one entity takes is dealt to another instead.

```java
DamageLink.redirect(player, target, 0.5f, 100);    // 5 seconds: half of every hit on the player lands on the target
DamageLink.redirectTarget(player);                 // who it goes to right now, or null
DamageLink.redirectTicksLeft(player);
DamageLink.clearRedirect(player);
```

**Share:** a group feels each other's pain. When one is hurt, each of the others takes part of it too.

```java
DamageLink.share(enemies, 0.5f, 200, player);      // 10 seconds; 'player' gets the kills (may be null)
DamageLink.isShared(entity);
DamageLink.sharedWith(entity);                     // the others in its group that are still alive
DamageLink.unshare(entity);                        // takes one out; the rest stay linked
DamageLink.addShareListener((from, to, amount, credit) -> ...);   // each time pain passes on: its look (Grave Shackles)
```

- The amounts are what the entity really lost, after its armor.
- Passed-on damage is never passed on again, so links cannot bounce damage around forever.
- A link ends when its time is up or when either end dies. Not saved.
- A small puff of sparks shows on whoever receives passed-on damage. `DamageLink.SHOW_PARTICLES = false` turns it off.
- A share needs at least 2 in the group. An entity that joins a new group leaves its old one.

### Substitute

The next hit does not land, and you decide what happens instead.

```java
Substitute.arm(player, 200, 1, (saved, source, amount, level) -> {
    // the hit was cancelled. source.getEntity() is the attacker (may be null), 'amount' is what it would have done
    Vec3 away = Spells.freeSpotNear(player, level, saved.position(), Spells.flatLook(player).scale(-1), 5);
    if (away != null) saved.teleportTo(away.x, away.y, away.z);
});
```

`200` is how long it stays ready, `1` is how many hits it takes before it is used up.

`Substitute.isArmed(entity)`, `charges(entity)`, `ticksLeft(entity)`, `disarm(entity)`.

- **What counts as a hit:** damage from an attacker (melee, arrows, abilities), projectiles and explosions.
- **What goes through without using it up:** fire, falling, drowning, poison and the like; anything in creative mode; the void and `/kill`.
- Arming again replaces the old one. Gone on death, on logout and after a restart.

### EmpoweredHit

The next time this player hits something with a normal attack (fist or weapon, not an ability), that hit is changed. A backstab, a charged blow, a poisoned blade.

```java
EmpoweredHit.arm(player, "my_pathway_backstab", 40, (attacker, target, amount, level) -> {
    // the hit is landing right now; 'amount' is what it would do
    ParticleShapes.burst(level, SPARK, target.getBoundingBox().getCenter(), 12, 0.05, 0.2);
    return amount * 1.5f + 16f;                      // what it does instead
});
```

`40` is how long it waits for the hit. `EmpoweredHit.isArmed(player, name)`, `ticksLeft(player, name)`, `disarm(player, name)`.

- Only real swings count. `Spells.strike` also looks like a player attack to the game, so abilities never use it up.
- A swing the game cancels (a rooted player, a concealed target) does not use it up either.
- Several names can be armed at once; one swing uses all of them. Arming a name again replaces it. Gone on death, logout and after a restart.

### StoredDamage

For a while, everything an entity takes is written down; when the time is up, the whole sum comes due at once.

```java
StoredDamage.start(target, player, 200,
        (victim, amount, total, level) -> { /* a hit was written down: a throb of its mark */ },
        (victim, total, level) -> StoredDamage.deal(victim, player, total));      // time is up: all of it again
```

`StoredDamage.isStoring(entity)`, `total(entity)`, `ticksLeft(entity)`, `release(entity)` (comes due now), `cancel(entity)` (nothing comes due).

- What is written down is what the entity really lost, after armor and everything else.
- `StoredDamage.deal(victim, owner, amount)` hits as magic from the owner: armor does not take its share a second time, and damage dealt this way is never written down, so a curse does not feed on its own payment.
- The part that comes due can wait for a look of its own: `Later.run(level, 4, () -> StoredDamage.deal(...))` (`curse` in `Apocalypse.java`).
- One per entity: starting another replaces it. It ends with nothing due when the entity dies. Not saved.

### NoHeal

Nothing heals the entity for a while: food, regeneration, potions, a mob's own healing.

```java
NoHeal.apply(target, 100);         // 5 seconds; applying it again only ever makes it last longer
NoHeal.isApplied(entity);   NoHeal.ticksLeft(entity);   NoHeal.remove(target);
```

Health set straight away (`setHealth`, a `DeathWard` bringing someone back) is not healing and still works. Saved with the entity. Used by the Hopeless Ice Age (`iceAge` in `Apocalypse.java`).

### Bleeding

An open wound: the entity loses health once a second for a while, and the damage counts as the one who opened it (`DamageFx.credit`).

```java
Bleeding.apply(target, player, 80, 2f);          // 4 seconds, 2 a second; false if it cannot bleed
Bleeding.isBleeding(entity);   Bleeding.ticksLeft(entity);   Bleeding.perSecond(entity);   Bleeding.remove(target);
Bleeding.addImmunity(entity -> ...);             // some cannot bleed (a body that feels no pain)
Bleeding.addListener((victim, amount, source) -> ...);   // the look and sound of each drip
```

- Applying again makes it last longer if the new time is longer and keeps the bigger of the two numbers, so a scratch never softens a deep wound. The newest source gets the credit.
- Magic damage: armor does not soften it. It ends when the target dies, or at once when it becomes immune. Creative players do not bleed.
- The look is a listener's: Abomination shows `AbominationFx.bleed` for the wounds its own players open. Saved with the entity. Used by Blood Chain, Rending Pounce and Werewolf Form's claws (`Abomination.java`).

### Corrosion

A rot that eats deeper the longer it is left. The entity loses health once a second, a little more every second, until it is cleansed or dies; after a while (`Corrosion.NO_HEAL_AFTER`, 5 seconds) it cannot heal at all. The damage counts as the caster's.

```java
Corrosion.apply(target, player, 2f, 1f);           // 2 a second, 1 more every second, until cleansed
Corrosion.apply(target, player, 2f, 1f, 200);      // the same, but it also wears off after 10 seconds
Corrosion.isCorroding(entity)   Corrosion.perSecond(entity)   Corrosion.ticksCorroding(entity)   Corrosion.blocksHealing(entity)
Corrosion.remove(target)
Corrosion.addImmunity(entity -> ...)
Corrosion.addListener(new Corrosion.Listener() {   // every method optional: its look
    @Override public void started(LivingEntity victim, Player source) { ... }
    @Override public void bit(LivingEntity victim, float amount, Player source) { ... }
    @Override public void ended(LivingEntity victim, Corrosion.End why) { ... }   // CLEANSED, TIME, DIED, REMOVED
});
```

- **What ends it:** anything that calls `Spells.cleanse` (every cleanse ability), a milk bucket (both through [`Cleansing`](#cleansing)), death, or the time limit when you gave one.
- Applying again keeps the bigger bite and the faster growth and never restarts its clock, so the no-healing time counts from the first touch. To make a second hit bite deeper: `Corrosion.apply(target, player, Corrosion.perSecond(target) + 5f, 1.5f)` (Ruin).
- `Corrosion.MAX_PER_SECOND` (40) caps a single bite; `Corrosion.EVERY` is the beat (20 ticks).
- Magic damage: armor does not soften it. Creative players do not rot. Saved with the entity. Used by Corrosion and Ruin (`PrinceOfAbolitionFallen`, `PrinceOfAbolitionSovereign`); their look is a listener in `PrinceOfAbolition`.

### Cleansing

The one place that says what "cleansed" means, so a status of your own can be washed off by every cleanse in the mod without any of them knowing about it.

```java
static { Cleansing.addListener(MyStatus::remove); }   // once: every cleanse now takes MyStatus off too
Spells.cleanse(entity);                               // what cleanse abilities call: harmful effects, fire, and the listeners
Cleansing.cleanseStatuses(entity);                    // only the listening statuses
```

Drinking a milk bucket runs the listeners too (`Cleansing.MILK_CLEANSES`). Corrosion listens. Bleeding, Decay and the rest do not on purpose (a wound is not a curse); give them a listener if a Pathway should wash them off.

### Falter

For a while part of an entity's attacks go wide: melee, arrows and abilities, everything that has an attacker. Every reason has a name, so two abilities never cut each other short: the chance is the biggest of the reasons still running.

```java
Falter.apply(target, "my_pathway_smoke", 0.4f, 120);   // 6 seconds: 40% of its attacks miss
Falter.chance(entity)    Falter.ticksLeft(entity, "my_pathway_smoke")
Falter.remove(target, "my_pathway_smoke")              // this reason ends; the others go on
Falter.clear(target)
Falter.addListener((attacker, victim, name) -> ...)    // the look of a miss
```

Damage that bypasses invulnerability is never missed. Saved with the entity. The Prince Of Abolition's Arrogance, Disorder, Corrosion's greed and Anxious use it (`PrinceOfAbolitionMarks`: a kind with a `miss` share).

### SupernaturalResistance

For a while the supernatural has less hold on an entity: supernatural damage is cut, and blindness and nausea cannot be put on it. A grant that shakes off also ends roots, freezing, `Blind`, taunts and darkness on it every tick.

```java
SupernaturalResistance.grant(player, "my_pathway_resist", 160, 0.4f, true,        // 8 seconds, 40% less, shakes off
        shaken -> HandOfGodFx.resistanceShrug(sl, player));                        // something was shaken off (may be null)
SupernaturalResistance.grant(player, "my_pathway_innate", 40, 0.25f, false, null);  // a passive: renewed every second
```

`SupernaturalResistance.has(entity)`, `cut(entity)`, `ticksLeft(entity, name)`, `end(entity, name)` and `isSupernatural(source)`.

- Supernatural damage: magic (also what an ability deals when it names no attacker), wither, explosions, lightning, freezing, sonic booms, thorns and dragon's breath. Melee, arrows, falls, fire and drowning are not.
- Several grants can be on one entity under different names: the biggest cut counts, and any grant that shakes off does.
- Works on mobs too. Not saved; gone on death and logout.

### Decay

The entity rots for a while: it cannot heal (`NoHeal`), it has wither and weakness, its armor and what it holds lose durability every second (items, never blocks), and its maximum health is lower by a part of itself.

```java
Decay.apply(target, 200, 0.2f);                 // 10 seconds, 20% of its health gone meanwhile; false if it cannot rot
Decay.apply(enemy, 40, 0.1f + ticksInside / 400f);   // every second in a Zone: the longer they stay, the deeper
Decay.isDecaying(entity);   Decay.depth(entity);   Decay.ticksLeft(entity);   Decay.cure(target);
Decay.addImmunity(entity -> ...);               // some cannot rot (a Pathway with authority over Decay)
```

- Applying it again never makes it shorter or shallower. `Decay.MAX_DEPTH` (0.6) is the most health it can take; the health comes back when it ends.
- `WEAR_EVERY` and `WEAR` say how fast the gear wears out.
- Saved with the entity. Used by Hand Of God's Decay, Twilight Sword, Divine Right Hand, the Glory Hurricane and Alchemy's Decay coating; Glory and up are immune (`HandOfGodEvents` in `HandOfGod.java`).

### WeaponCoat

Something on a player's weapon (or fists) that changes their next few hits: `EmpoweredHit`, but for several hits.

```java
WeaponCoat.arm(player, "my_class_poison", 10, 400, (attacker, target, amount, sl) -> {   // 10 hits, or 20 seconds
    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
    return amount + 4f;                                                                    // what the hit does instead
});
WeaponCoat.isArmed(player, name);   WeaponCoat.charges(player, name);   WeaponCoat.ticksLeft(player, name);   WeaponCoat.disarm(player, name);
```

- Only real swings use a charge, as for `EmpoweredHit`. Several coats under different names are all used by each hit.
- Inside the coat's part, add to `amount` rather than calling `Spells.strike` on the same target; hurt others a tick later (`Later.run`).
- The look is yours: Hand Of God's Alchemy shows a glow on the hand every few ticks while `isArmed` (`HandOfGodDemonHunting.tick`). Not saved.

### Unnoticed

Minds slide off the entity, but eyes do not: the opposite half of `Concealment`.

```java
Unnoticed.hide(player, 300);         // 15 seconds
Unnoticed.isUnnoticed(entity);   Unnoticed.ticksLeft(entity);   Unnoticed.end(player);
```

- Mobs forget it and cannot target it, and abilities cannot choose it: `Spells.aimEnemy`, `enemiesInSight` and the aiming of `aimFrom` and `aimGround` pass over it (`Spells.canBeChosen`). What only catches whoever is there (`enemiesAround`, a blast, a shot that flies into it) still does.
- It is drawn and can be hurt as usual. It ends the moment it hurts anything, with a swing or an ability.
- Saved with the entity. Used by Hand Of God's Mind Concealment.

### Slumber

Sleep that works on players and mobs alike: a lullaby, a soul put to rest, a body that will not get up.

```java
Slumber.put(target, 160, Slumber.Kind.SLEEP);       // 8 seconds; the first blow wakes it
Slumber.put(target, 160, Slumber.Kind.DEEP);        // the first DEEP_HITS (2) blows do not wake it
Slumber.put(target, 160, Slumber.Kind.LIMP);        // awake, but it cannot attack, use abilities or jump, and crawls
Slumber.put(target, 160, Slumber.Kind.ETERNAL);     // only the time or Slumber.wake ends it
Slumber.isAsleep(entity);   Slumber.isLimp(entity);   Slumber.kind(entity);   Slumber.ticksLeft(entity);   Slumber.wake(target);
Slumber.addImmunity(entity -> ...);                 // some cannot be put to sleep
Slumber.addWakeListener((entity, kind) -> ...);     // it woke (hurt, time up or wake): clear the look of its sleep
```

- Asleep (SLEEP, DEEP, ETERNAL) it is held completely (`Root` EVERYTHING); a player's screen goes dark (`Blind`), a mob forgets what it hunted and picks no one. `put` returns `false` for a creative player, the dead and the immune.
- Putting it to sleep again never makes it shorter; a new kind replaces the old one. `wake` lifts the root kinds it set, also one that something else set at the same time.
- The look is yours (`KnightOfMisfortuneFx.slumber` and `wake`). Saved with the entity. Used by the Knight Of Misfortune's poems, Dream Invasion, Requiem and Command Spirits.

### Fear

The other half of `Taunt`: something is afraid of one entity for a while.

```java
Fear.apply(victim, caster, 120);    // 6 seconds; false if it cannot be frightened
Fear.isAfraid(entity);   Fear.sourceOf(entity);   Fear.ticksLeft(entity);   Fear.end(victim);   Fear.addImmunity(entity -> ...);
```

- A mob runs `FLEE_DISTANCE` away from what it fears (at `FLEE_SPEED`), drops it as a target and cannot pick it again; past `SAFE_DISTANCE` it stops running but still will not go after it.
- A player gets Slowness I and Darkness and cannot sprint toward what they fear.
- Afraid again never makes it shorter; a new source replaces the old one. It ends when the source dies or leaves. Saved with the entity. Used by the Fear poem, the Terror Banshee and Terror dreams.

### Silence

An entity's power is hushed for a while: a player cannot use abilities (every ability key refuses) but can still fight with weapons; a mob cannot attack at all, in melee or with what it shoots.

```java
Silence.apply(target, 100);   Silence.isSilenced(entity);   Silence.ticksLeft(entity);   Silence.lift(target);
```

It is `root/Root` underneath (ABILITIES, and ATTACK for a mob): saved with the entity, Root's ring shows, and the player is told "You are bound by Order." when they press a key. Used by Requiem, the Lullaby a player shakes off and the Despair edict.

Some power needs no voice. Let one through a silence (or any root with ABILITIES) on its key, and let its wheel's picks through:

```java
Silence.addCastsThrough((player, key) -> key == 2 && MyPathway.resonates(player));   // once, in a static block
AbilityWheel.create(...)....castsWhileSilenced(player -> MyPathway.resonates(player));  // its wheel (AbilityMenu has it too)
```

The ability keys ask `Silence.blocksKey(player, key)` first. Knight Of Misfortune's poems use it from Sequence 7.

### Misfortune

An entity's luck is taken: it carries 1 to 5 levels, and every second it rolls a mishap with a 10% chance per level.

```java
Misfortune.add(target, player, 3, 3, 400);          // 3 levels at once, at most 3, for 20 seconds from now
Misfortune.add(target, player, 1, 5, 400);          // one more, up to 5 (a field that worsens it each beat)
Misfortune.add(target, player, 2, 5, 400, true);    // Catastrophe: its worst mishap is a disaster
Misfortune.level(entity);   Misfortune.ticksLeft(entity);   Misfortune.clear(target);   Misfortune.befall(victim, Misfortune.Mishap.STUMBLE);
Misfortune.sourceId(entity);                        // who laid it (the last to add to it), or null
Misfortune.addImmunity(entity -> ...);              // some cannot be cursed
Misfortune.addHalfLevels(entity -> ...);            // some carry only half (bosses)
Misfortune.addListener((victim, mishap, source) -> ...);   // the look and sound of each mishap
```

| Mishap | From level | What happens |
|---|---|---|
| `STUMBLE` | 1 | Slowness IV for a second and a little trip |
| `FUMBLE` | 2 | What it holds and wears loses 6 durability; its next blow is 30% weaker |
| `BACKFIRE` | 3 | The next thing it shoots flies wide (up to 30 degrees) |
| `FALLING_STONE` | 4 | Magic damage, 5% of its maximum health, counted as the curser's (`DamageFx.credit`) |
| `CALAMITY` | 5 | 15% of its maximum health; with Catastrophe 30%, and it is thrown up for a fall it did not see coming |

The damage is the status's own; the look is a listener's (Knight Of Misfortune's shows the mishaps it caused). Adding never lowers the level and makes all of it last again. Saved with the entity. Every number is a `public static` at the top of the class. Used by the Curse of Misfortune, the Night Domain, Misfortune, Catastrophe and Ill Omen, and by every Abomination curse (through `Abomination.curse`, which never shortens a longer curse and makes the cursed one a `Connection`).

### Study

How well one caster knows one entity, from 0 to 100%. Every caster has a gauge of their own on everything; two casters never share theirs. Everything a caster does to what they know hits harder: every full 10% is 5% more damage, 50% at most (`BONUS_PER_TEN`, `MOST_BONUS`). The bonus is put on in a `LivingHurtEvent` for every hit whose attacker (or the owner of the projectile) has a gauge on the one hurt, melee and abilities alike, so nothing has to multiply anything by hand. Left alone for 10 seconds (`IDLE_TICKS`), a gauge falls 5 a second (`DECAY_PER_SECOND`). Knowledge Emperor is built on it.

```java
Study.add(player, target, 25);               // 25% more (100 at most); returns the new value
Study.strike(player, target, 20f, 10);       // Spells.strike (with the bonus), then 10% more
if (Study.knowsWeakness(player, target)) ... // 50% or more (Study.WEAKNESS)
if (Study.isUnderstood(player, target)) ...  // 100% (Study.FULL)
Study.hold(player, target, 100, 160);        // at least 100% for 8 seconds, whatever it was; it does not fall meanwhile
Study.set(player, target, 100);              // straight to a value
int had = Study.take(player, target);        // all of it gathered up, and forgotten
List<Study.Known> taken = Study.takeAround(player, sl, center, 32);
for (Study.Known known : Study.knownBy(player)) { known.target(); known.percent(); }   // most known first
```

`get(caster, target)`, `bonus(caster, target)`, `forget(caster, target)`, `forgetAll(caster)`, `isImmune(target)`. Rules, once in a static block:

```java
Study.addImmunity(entity -> ...);                     // nobody can build Study on it (a mind that cannot be read)
Study.addDecayScale(caster -> 0.5);                   // this caster's gauges fall at half the speed
Study.addListener(new Study.Listener() {              // the look: Study shows nothing itself
    public void changed(Player caster, LivingEntity target, int before, int after) { ... }
    public void forgotten(Player caster, LivingEntity target) { ... }
});
```

Not saved: a restart forgets everything; a caster who logs out or dies forgets what they knew, and a gauge on something that dies is gone. `hold` works on the immune too (it is a power over that one entity, not study). Knowledge Emperor's listener draws an eye over each enemy that only the caster sees (`KnowledgeEmperor.showStudy`).

### Scrolls

Spells kept on paper. A player carries a pouch of scrolls, oldest first; reading one casts its spell at once, for free. Any spell can be a scroll: register it once with a key, written as a function of who reads it and how strong it is, with no payment inside. The same code then runs when it is cast by hand, read from a scroll, repeated by an echo or empowered by a passive (see "Write a spell as a function" under "Tricks worth copying").

```java
// once, in a static block: key (no commas), name, what it costs by hand, the spell
Scrolls.register("my_pathway_hex", "Hex", 1250, (reader, sl, power) -> hex(reader, sl, power));

// cast by hand: pay, cast, and remember it as the last Scrollable spell (what an Inscribe writes)
if (Spells.paySpirituality(player, 1250) && hex(player, sl, 1f)) Scrolls.cast(player, "my_pathway_hex");

String dropped = Scrolls.add(player, "my_pathway_hex", 3);   // into the pouch, 3 at most: the one that fell out, or null
String key = Scrolls.takeOldest(player);                      // null when empty
Scrolls.Kind kind = Scrolls.kind(key);                        // kind.name(), kind.cost(), kind.spell()
if (!kind.spell().cast(reader, sl, 1f)) Scrolls.putBack(player, key);   // it found nothing to do: back in front, unspent
Scrolls.addListener((player, pouch) -> MyFx.showPouch(player, pouch));  // the pouch changed: its pictures
```

`pouch(player)` (a copy), `size`, `isFull(player, capacity)`, `lastCast(player)` (a `Kind` or null), `clear`, `show(player)` (tell the listeners again, after a relog). A spell returns `true` when it went off and `false` when it found nothing to do. The pouch and the last cast are kept in the player's persistent data, so they survive a relog; dying burns them. Keys that are no longer registered are left out when the pouch is read, so register every kind before any pouch is read (Knowledge Emperor does it in its events' static block).

### Dispel

Everything that keeps someone safe, off at once. Knowledge Emperor's Erase (`KnowledgeEmperorKnowledge.erase`).

```java
if (Dispel.isConjured(target)) {                 // a summon, or what a rule names (a decoy, a projection)
    Dispel.unmake(target);                       // gone, no drops
} else {
    int boons = Dispel.boons(target);            // every beneficial potion effect off
    int wards = Dispel.statuses(target);         // Substitute, DeathWard, DamageLink (both kinds), Concealment,
}                                                // Unnoticed, Invulnerable, a marionette's strings
```

A Pathway with a ward of its own adds it once, in a static block: `Dispel.addRule(entity -> MyWard.isUp(entity) && MyWard.breakIt(entity))` (true when something was taken off), and `Dispel.addConjuredCheck(entity -> ...)` for its decoys. Nothing is saved; it only acts when called.

### Invulnerable

For a while nothing can hurt, choose or hold an entity, and what would have hurt it is added up. Unlike `Concealment` it is still seen and its models drawn. Knowledge Emperor's Informatization.

```java
Invulnerable.grant(player, 120);                 // 6 seconds (granting again keeps the longer)
float taken = Invulnerable.end(player);          // ends now; everything it absorbed
```

`isGranted(entity)`, `ticksLeft`, `absorbed` (so far). Every hit is cancelled (not the void or `/kill`) and its amount added; mobs that had it as their target forget it and cannot pick it again; `Spells` aiming passes over it; every `Root` on it is lifted. When its time runs out it simply ends: call `end` yourself to read what it took. Not saved.

### Appraisal

One caster has read an entity for a while: they know what it is weak to, and their **ability** hits on it land harder, by the mark's own bonus and by `MATCH_BONUS` (50%) more when the hit is of the kind it is weak to. Every caster has marks of their own. The Illuminator is built on it (`Illuminator.appraise`); it fits any "I have studied you, now I know where to strike".

```java
Appraisal.mark(player, target, "corrosion", 240, 0.15f);   // 12 s: weak to corrosion, +15% from my abilities
Appraisal.strike(player, target, 36f, "corrosion");         // Spells.strike as an ability hit of that kind: +15% and +50%
Appraisal.strikeTrue(player, target, 90f, null);            // Spells.strikeTrue, an ability hit of no kind: +15% only
Appraisal.as("pierce", () -> Spells.strikeThrough(player, target, 30f));   // any damage code, counted the same way
Appraisal.weakness(player, target);   Appraisal.isMarked(player, target);   Appraisal.markedBy(player);
Appraisal.spend(player, target);      // a finisher used it up (false: there was none)
Appraisal.wipe(target);               // every caster's mark off it
Appraisal.addListener(new Appraisal.Listener() { ... });    // marked / ended(caster id, target, End): the look
Appraisal.addImmunity(entity -> ...);                       // nobody can mark it (mark returns false)
```

- Only hits made inside `strike`, `strikeTrue` or `as` get the bonus: a melee swing does not, nor damage that comes later on its own (bleeding, corrosion). `isAbilityHit()` and `hitKind()` say what is landing right now, for a status of your own that cares.
- The kind is any word your Pathway uses. What a target is weak to comes from a **`WeaknessTable`** of your own:

```java
private static final WeaknessTable WEAKNESS = WeaknessTable.otherwise("pierce")
        .rule("pierce", one -> one instanceof Player)
        .rule("starlight", one -> one.is(EntityTypeTags.UNDEAD) || Marionette.isSummoned(one))
        .rule("corrosion", one -> Construct.isConstruct(one) || one.getArmorValue() >= 8);
String weak = WEAKNESS.of(target);    // the first rule that fits, from the top; else "pierce"
```

- A mark ends when its time is up, when it is spent or wiped, when the target dies and when the caster logs out (`End`: `TIME`, `SPENT`, `WIPED`, `DIED`, `CASTER_GONE`). Not saved.

### Exposed

Someone cannot hide for a while: a `Concealment` on it ends, an `Unnoticed` ends, invisibility comes off and cannot be put on, every tick while it lasts. A survey that drags the hidden out, a mark that will not let go. The Illuminator's Survey and Appraise.

```java
Exposed.apply(target, 60);                 // 3 seconds; longer is kept
Exposed.isExposed(entity)   Exposed.ticksLeft(entity)   Exposed.end(target)
Exposed.addListener(one -> ...)            // it just pulled someone out of hiding: the look
Exposed.addException(one -> ...)           // hiding that must be left alone (Exile already is)
```

An ability of your own that hides something on purpose while it may be Exposed (a body written into a book, a soul inside a host) needs an `addException`. Saved with the entity.

### Enfeebled

The other half of `Vulnerable`: something **deals** less damage for a while, with everything that names it as the attacker (melee, its arrows, its abilities).

```java
Enfeebled.apply(target, "my_pathway_sapped", 0.3f, 200);   // 10 s: 30% less
Enfeebled.cut(entity)   Enfeebled.ticksLeft(entity, name)   Enfeebled.remove(target, name)   Enfeebled.clear(target)
```

Every reason has a name; the biggest running cut counts, and applying a name again never makes it shorter or smaller. Made before the target's armor. Saved with the entity. The Illuminator's Mutation (`IlluminatorAstronomy`); the planned Eroded uses it too.

### Gravity

Something is heavier or lighter for a while, through the game's own gravity attribute (no potion swirls, and a player's own screen agrees).

```java
Gravity.set(enemy, "my_pathway_crush", 3.0, 40);   // three times as heavy: it drops like a stone
Gravity.set(ally, "my_pathway_float", 0.3, 40);    // long, slow jumps and falls
Gravity.set(enemy, "my_pathway_lift", -0.15, 40);  // below 0: it falls upward, slowly (lifted off the ground)
Gravity.factor(entity)   Gravity.ticksLeft(entity, name)   Gravity.clear(entity, name)   Gravity.clearAll(entity)
```

Reasons multiply together. It does not stop fall damage: lift someone with `FallGuard` as well. Saved with the entity. Blessing of Stars presses enemies down with it; Rewrite Principles and Mechanical Heaven are planned on it.

### Weight

Heaviness: something is made Heavy for a while, at a level from 1 to `MAX_LEVEL` (3). Star Dragon's Pressed, Tidal Surge, Scaled Form's blows, Heavybringing and the Neutron Star; Gravity Authority is an `addImmunity` rule.

```java
Weight.put(enemy, 1, 60);          // Heavy for 3 seconds
Weight.put(enemy, 3, 100);         // very heavy
Weight.isHeavy(entity)   Weight.level(entity)   Weight.ticksLeft(entity)   Weight.end(target)
Weight.addImmunity(entity -> ...)  // some cannot be made heavy (and what they had ends)
```

While Heavy: slowed (`SLOW_PER_LEVEL`, 20% a level, `SLOW_MOST` 70%, through the movement speed attribute), no jumping, no flying (creative-style flight and elytra stop), falling like a stone (`status/Gravity`, `GRAVITY_PER_LEVEL`) with falls hurting `FALL_PER_LEVEL` (30%) more a level, and what it shoots flies at `SHOT_SPEED` (60%). Putting it on again keeps the higher level and the later end. Saved with the entity; all the numbers are `public static` at the top of the class.

### Unreadable

Someone cannot be picked out from further than a few blocks: mobs further away cannot target it (and those hunting it from further lose it), and abilities cannot **choose** it from further (`Spells.canBeChosen`; with no chooser named, not at all). It is seen and can be hurt; what hits an area still catches it.

```java
Unreadable.apply(ally, 20, 6.0);       // 1 s, 6 blocks: put it on again every half second from a zone
Unreadable.isUnreadable(entity)   Unreadable.range(entity)   Unreadable.ticksLeft(entity)   Unreadable.end(entity)
```

Saved with the entity. The Illuminator's False Constellations, the Key Of Stars' Interference.

Something that should know when an ability's aiming passed over an Unreadable entity because the chooser was too far
(the Key Of Stars turns the first search back on its caster):

```java
Unreadable.addTurnedAwayListener((chooser, target) -> { ... });   // once, in a static block; aiming asks often, so act once
```

### Unbound

Every lock on an entity springs open at once, and for a moment nothing of that kind takes on it again. The Key Of Stars' Slip Free.

```java
Unbound.free(target, 30, false);     // roots (not a Teleport one), harmful effects, fire, Cleansing statuses and control end;
                                     // for 1.5 s no root and no harmful effect takes on it
Unbound.free(target, 30, true);      // Teleport roots too
Unbound.breakControl(target);        // only the control (mind control, strings): see addBreaker
Unbound.isUnbound(entity)   Unbound.ticksLeft(entity)
Unbound.addBreaker(entity -> MyCharm.isCharmed(entity) && MyCharm.end(entity));   // once: a kind of control it ends
```

A hold that is not a root (a Decree's Rule) is not its to open. Nobody registers a breaker yet: give one to every control a
Pathway adds (a charm, a puppet's string on a player), and `free` and `breakControl` end it.

### Starlit

One player's mark on a few creatures (at most `Starlit.MAX_PER_CASTER`, 3; a new one past that replaces the oldest). It says
"caught": what comes next can need it (the Key Of Stars' Exile and Starlight Seal), and the Pathway draws it for the caster and
their allies.

```java
Starlit.mark(target, player, 240);           Starlit.has(target, player)     Starlit.hasAny(target)
Starlit.ticksLeft(target, player)            Starlit.consume(target, player)  // used up: true if it was there
Starlit.marked(player)                       Starlit.markers(target)
Starlit.addListener((caster, target) -> ...) // once: a mark ended (time up, replaced, consumed, died, logged out)
```

Nothing shows by itself (`KeyOfStars.starlit` marks and shows it). Not saved.

### Anchors

Places a player saved, each name its own list (start it with your class). Kept through death.

```java
Anchors.Anchor dropped = Anchors.add(player, "my_class_marks", sl, spot, 12000, 3);   // 10 minutes, at most 3 (the oldest goes)
Anchors.list(player, "my_class_marks")                         // the live ones, oldest first
Anchors.removeNear(player, "my_class_marks", sl, spot, 1.5)    // picked up (or null)
Anchors.aimed(player, "my_class_marks", 10)                    // the one in this dimension nearest the crosshair, 10 degrees at most
anchor.at()   anchor.isIn(level)   anchor.until()
```

Going to one in another dimension is not in the kit yet. The Key Of Stars' Star Anchors.

### Construct

What is made rather than born: iron and snow golems, anything built or `mark`ed, anything an `addCheck` rule names. Abilities that take things apart ask `isConstruct` (the Illuminator's Exploit Flaw and Dissociation).

```java
Construct.isConstruct(target)   Construct.mark(mob)   Construct.addCheck(entity -> ...)
```

It also **builds** them: a vanilla mob called up for its owner (`Marionette`'s summon: it fights for them and vanishes without drops), with the body you say, invisible and silent so only your model shows.

```java
Mob golem = Construct.of(EntityTypes.IRON_GOLEM)
        .health(200).damage(20).steady()            // its health, its blow, no knockback
        .speed(1.2f).scale(0.75f)                   // times its own (optional)
        .lasts(1800).leash(40)                      // 0 = no timer; gone when the owner is further than 40 (or gone)
        .limit("my_pathway_golem", 1)               // one at a time: a new one ends the oldest (REPLACED)
        .onEnd((made, why) -> MyFx.golemEnd(sl, made, tag))   // DIED, TIME, REPLACED, LEASH, OWNER_GONE, ENDED
        .build(player, sl, feet, yaw);              // null if it could not be made there
MyFx.golemBody(sl, golem, 1800, tag);               // the look is yours
```

`.shown()` keeps the vanilla body visible (a decoy that looks like a person). `ofOwner(player, group)` (oldest first; `null` = all), `ownerOf(entity)`, `isBuilt(entity)`, `ticksLeft(mob)`, `extend(mob, ticks)` (a kingdom where made things do not run out), `end(mob)`, `endAll(player, group)`. After a restart a built one is a plain summon that runs out (`lasts` plus two seconds, or an hour). The Illuminator's Living Doll and Golem (`IlluminatorAlchemy`).

### AbilityLock

No abilities for a while, but the hands still work: a player's ability keys are refused (`Root`'s ABILITIES), a mob's projectiles vanish as they are fired and every hit it makes that is not its own body striking is stopped, so it fights only in melee. Unlike `Silence`, a mob keeps its melee.

```java
AbilityLock.apply(target, 160);        // false: immune or dead
AbilityLock.isLocked(entity)   AbilityLock.ticksLeft(entity)   AbilityLock.lift(target)
AbilityLock.addImmunity(entity -> ...)
```

Saved with the entity. The Illuminator's Mathematical Maze and Reality Knowledge (`IlluminatorCivilization`).

### Connection

Who a player is connected to: someone they hit, cursed, chained or sentenced lately. For a curse that reaches across the world, a scrying, a mark that finds its bearer anywhere.

```java
Connection.touch(player, target);                   // connected now (every hit a player lands does this by itself)
Connection.has(player, target, 12000);              // touched in the last 10 minutes?
Connection.ticksSince(player, target);              // how long ago (-1 = never, or forgotten)
Connection.latest(player, 12000);                   // the newest one still alive anywhere on the server, or null
Connection.all(player, 12000);                      // all of them alive anywhere, newest first
Connection.find(server, id);                        // a living entity by id, in any dimension
Connection.clear(player, target);   Connection.clearAll(player);
Connection.addListener((owner, target, fresh) -> ...);   // fresh = new, or it had run out: a look only the owner sees
```

- The ability says how long ago still counts, so one connection serves them all (10 minutes for one, 30 for a stronger one). Nothing older than `KEEP_TICKS` (30 minutes) is kept, and each player keeps their `MAX_PER_OWNER` (64) newest.
- Hits connect by themselves: any damage whose source is a player (melee, arrows, spells), once it really lands. `Connection.HITS_CONNECT = false` leaves it to `touch` alone.
- A mob that dies is forgotten by everyone; a player is not (they come back). Time is the server's, so it counts the same in every dimension. Not saved.
- Used by Curse by Connection and Cursed Artifact (`AbominationCurses.java`), and every Abomination curse (`Abomination.curse`) and Death Sentence make one.

### Nocturnality

How dark it is for an entity, as one number, and night forced on a place or a body.

```java
float dark = Nocturnality.darkness(entity);          // 0 = full light ... 1 = pitch black (block light, and sky light at a quarter by night)
Nocturnality.isDark(entity);                          // at least DARK (0.75): full dark
Nocturnality.forceNight(sl, center, 20, 800);         // 40 s of night 20 blocks round a spot: darkness() is 1 there
Nocturnality.forceNight(entity, 200);                 // night that goes with the entity (saved with it)
Nocturnality.isForcedNight(entity);   Nocturnality.inDaySun(entity);   // the bright sun of day under the open sky
```

It only changes what `darkness()` says: the sky the players see is not touched. Forced night on a place is not saved. Used by Knight Of Misfortune's Nocturnality, Deep Night, Night Domain and Nightmare World.

### GiantForm

The entity swells to a bigger body for a while, with longer reach and step, and a madness that can rise until it breaks the form.

```java
GiantForm.of(1.5)                       // 1.5 times the size it has now (of(0.5): half, a doll's body)
        .reach(1.5).stepHeight(1.0)     // more reach and step (optional)
        .lasts(800)                     // 0 = until GiantForm.end(...)
        .madness(1f / 30f)              // madness each second (0..1); 0 = none
        .everySecond((giant, madness) -> ...)
        .onMad(giant -> ...)            // madness is full: the form ends right after
        .onEnd((giant, why) -> ...)     // TIME, MAD, ENDED, REPLACED, DIED, LEFT
        .grow(player);
GiantForm.isGiant(entity);   GiantForm.madness(entity);   GiantForm.scale(entity);   GiantForm.ticksLeft(entity);   GiantForm.end(entity);
```

- The body is attribute modifiers with fixed ids, so one form at a time; it is on top of what the entity already has (a Pathway's own `Physique` stays).
- It ends, and shrinks back, on death, logout and a change of dimension. Nothing is saved: after a restart nobody is left giant.
- The look is yours (`HandOfGodFx.giantForm` shows the one-eyed Giant round the caster; show it before `grow`, so it grows from the old size). Used by Hand Of God's Giant Form, the Abomination's Werewolf Form, and Abomination Form, which shrinks to half.

### Root and Freeze immunities

Some bodies cannot be held. A rule, once in a static block, for as long as it says true:

```java
Root.addImmunity((entity, restriction) -> restriction == RootRestriction.MOVE && isAnchored(entity));   // per restriction
Freeze.addImmunity(entity -> isAnchored(entity));                                                     // no ice takes
Root.isImmune(entity, restriction)   Freeze.isImmune(entity)
```

Roots that last longer or shorter on some bodies (the Key Of Stars' Footprints of Freedom, a quarter shorter, and Astral
Footing, Teleport roots half as long):

```java
Root.addDurationScale((entity, restriction) -> isSlippery(entity) ? 0.75f : 1f);   // once; every rule that applies is multiplied in
Root.scaledTicks(entity, restriction, ticks)                                       // what a root of 'ticks' really lasts on it
```

A restriction it is immune to is left out when a root is applied, and one it already had ends at its next tick; ice it already had breaks. A frozen player is rooted through `Root` too, so give both rules. A hold you put on yourself must get past your own rule (the Star Dragon's Singularity holds a Condenser while it gathers: its rule says false while `StarDragonGravity.isGathering`). The Star Dragon's Weight and Density and Neutron Star (MOVE, JUMP and TELEPORT, and no freezing), in `StarDragon`'s events.

### Form

A player takes another shape for a while: the body of that shape (attribute changes and potion effects, kept up while it lasts), a weakness afterwards if you want, and a clean end whatever ends it. One manager for every transformation, so each is a few lines. Star Dragon's Scaled Form and Heavy Body (`StarDragonTides`), Radiant Angel Form and Star Dragon Form (`StarDragonForms`).

```java
Form.of("my_pathway_scales")
        .lasts(600)                                                      // 0 = until Form.end(player)
        .attribute(Attributes.ENTITY_INTERACTION_RANGE, 1.0, AttributeModifier.Operation.ADD_VALUE)
        .effect(MobEffects.STRENGTH, 1)                                  // Strength II while it lasts
        .aftermath(200)                                                  // Weakness I and Slowness I for 10 s after it
        .flies()                                                         // it can fly while it lasts (status/Flight)
        .cutsDamage(0.4f)                                                // 40% less damage taken (a ConjuredArmor under its name)
        .keepWhile(player -> SoulCore.getPathway(player).equals(PATHWAY))
        .everySecond(player -> MyFx.aura(...))
        .every(10, player -> MyFx.glow(...))                             // its own beat: a burn every half second, a wingbeat
        .onEnd((player, why) -> MyFx.formEnd(...))                       // why: TIME, ENDED, REPLACED, LOST, DIED, LEFT
        .take(player);
```

`Form.isIn(player, name)`, `current(player)` (its name or `null`), `ticksLeft(player)` (-1 = until ended), `end(player)`, `inAftermath(player)`. One form at a time: taking another ends the first (`REPLACED`). The aftermath follows `TIME` and `ENDED` only. The flight, the damage cut and the beat end with the form, whatever ends it. A form cast again to turn back: end it and return `FREE` from a wheel entry that has `.free(player -> Form.isIn(player, name))`, so the cooldown does not stand in the way (the Radiant Angel and the Star Dragon). Its model is yours to wear (`ParticleShapes.modelOnTurning`) and take off in `onEnd`. Not saved: after a restart nobody is left transformed.

### ConjuredArmor

Armor that is not an item: it weighs nothing and cannot be taken off or dropped.

```java
ConjuredArmor.of("my_class_bone_shell")
        .lasts(900)                     // 0 = until ConjuredArmor.takeOff(...)
        .cut(0.4f)                      // 40% less damage from everything
        .projectileCut(0.6f)            // projectiles 60% less (the bigger of the two counts for them)
        .absorption(8f).regrow(40)      // a layer of absorption, one point back every 2 s without a hit
        .steady()                       // no knockback, slowness does not take
        .onHit((wearer, amount) -> ...)
        .onEnd(wearer -> ...)           // clear its look here
        .wear(entity);
ConjuredArmor.isWorn(entity, name);   ConjuredArmor.ticksLeft(entity, name);   ConjuredArmor.takeOff(entity, name);
```

- Several under different names: the biggest cut counts and the layers add up. Works on mobs too. Not saved.
- Used by Hand Of God's Dawn Armor and Silver Armor (`dawnArmor` in `HandOfGodDawn.java`), by Mercury Liquefaction on an ally, by Abomination's Undying Flesh (`undyingFlesh` in `AbominationZombie.java`, with `projectileCut`), by the Star Dragon's Neutron Star (`steady`), and by every `Form` with `.cutsDamage(...)` (the Star Dragon Form).

### ConjuredWeapon

A weapon that is not an item: nothing is put in the hand, so nothing can be dropped, stolen or duplicated. While it is drawn, the player's normal hits are its hits.

```java
ConjuredWeapon.of("my_class_flame_blade")
        .lasts(800)                     // 0 = until put away
        .damage(6f).reach(1.0)          // more on every hit, more reach
        .upkeep(20, 25)                 // 25 spirituality a second; it fades when that cannot be paid (optional)
        .onHit((attacker, target, amount, charge, sl) -> amount)   // charge: how full the attack bar was (1 = full swing)
        .onEnd((wielder, why) -> ...)   // TIME, PUT_AWAY, NO_SPIRITUALITY, REPLACED, DIED, LEFT
        .draw(player);
ConjuredWeapon.isDrawn(player, name);   ConjuredWeapon.ticksLeft(player, name);   ConjuredWeapon.putAway(player, name);
```

- `damage` is scaled by the attack bar, so a spam click adds little. As for `WeaponCoat`, add to `amount` in `onHit` and hurt others a tick later.
- The look is yours (a model held beside the body with `ParticleShapes.modelOnTurning`, cleared in `onEnd`). Not saved.
- Used by Hand Of God's Sword of Dawn (`drawSword`, `purifyingHit` in `HandOfGodDawn.java`).

### Marionette

A mob whose strings a player holds. It stops hunting its master and fights for them.

```java
if (Marionette.control(mob, player, 600)) { ... }  // 30 seconds. false if it could not be taken
Marionette.isMarionette(entity);
Marionette.isMarionetteOf(entity, player);
Marionette.of(player);                             // the marionettes that player holds right now
Marionette.ticksLeft(entity);
Marionette.release(mob);                           // lets go: it is its old self again
Marionette.collapse(mob);                          // cuts the strings the way running out of time does
Marionette.summon(mob, player, 600);               // a creature an ability just spawned: fights the same way, does not
                                                   // count toward MAX_PER_MASTER, no control bar, vanishes (no drops) when it ends
Mob hound = Marionette.spawnSummon(EntityTypes.WOLF, player, sl, feet, yaw, 600);   // spawns one there and summons it (null if it could not)
Marionette.isSummoned(entity);
```

What a marionette does:

- It attacks, in this order: what its master last hit, what last hit its master, the fight it is already in, what last hit the marionette, the nearest monster within 16 blocks. It never picks a target of its own.
- With nothing to fight it walks back to its master.
- It cannot target or hurt its master or the master's other marionettes, and spells built on `Spells` leave the caster's marionettes alone.
- Mobs that cannot fight (a cow, a villager) are walked up to the enemy and made to hit it.

When it ends: the time is up, the puppet dies, or its master dies, logs out or leaves the dimension. When the time runs out the puppet drops dead and its master gets the kill. In the other cases it is just let go.

Settings (change them once, for everyone):

| Setting | Default | Meaning |
|---|---|---|
| `Marionette.DIES_WHEN_TIME_RUNS_OUT` | `true` | `false` = it simply becomes itself again |
| `Marionette.MAX_PER_MASTER` | `3` | Taking one more lets go of the one with the least time left |

The look (a wooden control bar turning above it, strings down to its body) comes with it and is sized to the mob. The status is saved with the mob, so a marionette is still one after a reload; the control bar is not shown again, though.

Limits: a marionette creeper still breaks blocks when it goes off. A few bosses choose their target in their own way (the warden) and can slip the strings, so put a health limit on what may be taken, as `takeHold` does.

### Concealment

An entity steps out of the world for a while.

```java
Concealment.hide(player, 80);      // 4 seconds
Concealment.isHidden(entity);
Concealment.ticksLeft(entity);
Concealment.reveal(player);        // steps back out now
```

While hidden:

- Nobody sees it: body, armor, held items, name and the 3D models stuck to it are not drawn at all, on anyone's screen.
- Nothing can hurt it (except the void and `/kill`).
- Mobs forget it and cannot pick it as a target. Spells built on `Spells` pass over it.
- It can still move. A hidden player who hits something with a normal attack steps back out.

Your abilities decide whether casting ends it. If it should, call `Concealment.reveal(player)` at the start of the cast (it does nothing when the player is not hidden).

Settings: `Concealment.SHOW_PARTICLES` (the puff of fog), `Concealment.BREAKS_ON_ATTACK`.

It works on mobs too, and it is saved with the entity.

### Exile

Someone is put out of the real world for a while and comes back where they left, or where you say. A banishment, a step into a parallel world, a pocket of nowhere.

```java
Exile.banish(target, 120);                                    // 6 seconds; back where it stood
Exile.banish(player, 80, there, (who, sl) -> MyFx.back(sl, who));   // back at 'there', and this runs when it is back
Exile.isExiled(entity)    Exile.ticksLeft(entity)    Exile.bringBack(target)
Exile.addListener(new Exile.Listener() { ... })               // banished / returned: the look of every exile
```

- While out it is hidden, unhurt and untargeted (`Concealment`) and can do nothing at all (`Root`, `EVERYTHING`); it hangs where it was. Coming back takes off the hiding and every hold.
- Banishing what is out already only keeps it out longer. Saved with the entity (the end and the spot); the code given to `banish` does not survive a restart.
- Used by Exile and Last Rebirth (`PrinceOfAbolitionSovereign`, `PrinceOfAbolitionMausoleums`).

### DeathWard

The next time this entity would die, it doesn't.

```java
DeathWard.arm(player, 1200, 0.5f, (saved, source, level) -> {
    // runs at the moment death was cheated: a blast, a teleport, a message...
    SpellFx.shockRing(level, saved.position(), 6, 0xF0FFE9A8, 12, 0);
});
```

`1200` is how long the ward waits, `0.5f` is the health they come back with (half), and the last part may be `null`.

`DeathWard.isArmed(entity)`, `ticksLeft(entity)`, `disarm(entity)`.

- Saves from everything except the void and `/kill`. A totem of undying in the hand is used first.
- For 2 seconds afterwards nothing can hurt the saved entity, so what killed it does not simply do it again (`DeathWard.GRACE_TICKS`).
- Arming again replaces the old ward. Gone on logout and after a restart.
- To let the player see how long it still waits: `HudTimer.show(player, "my_pathway_ward", "Ward", 1200, 0x9CFFD2)` when you arm it, and `HudTimer.hide(...)` in the part that runs when it saves them.

### FallGuard

The next landing does no fall damage. For abilities that throw their own caster, or a friend, into the air.

```java
player.setDeltaMovement(0, 2.0, 0);
player.hurtMarked = true;              // makes the player's game accept the push
FallGuard.protect(player, 400);        // safe until they land, for at most 20 seconds
```

`FallGuard.isProtected(entity)`, `FallGuard.remove(entity)`. It ends at the first landing or when the time runs out. Saved with the entity.

### Marks

A number an entity carries for a while: how often your spikes hit it, how many charges a player built up.

```java
int hits = Marks.add(target, "my_pathway_hits", 10, 300);    // one more, 10 at most. All of them last 15 seconds again
float damage = 10f + 5f * (hits - 1);                      // the more it carries, the more this hurts

int paid = Marks.take(target, "my_pathway_hits");            // a finisher: how many it carried, and they are gone
Spells.strike(player, target, 15f * paid);
```

`Marks.count(entity, name)`, `ticksLeft(entity, name)`, `clear(entity, name)`, and `add(entity, name, amount, max, ticks)` for several at once.

- Every name is its own counter. Start the name with your Pathway.
- Each `add` makes all the marks of that name last their full time again. When the time runs out without a new one, they are forgotten together.

#### One player's marks, dropping one at a time

Give the owner (the player who marks) and how many ticks it takes for one mark to wear off. Each player has his own count on the same mob; other players' marks never count for you. Nothing marks by itself: your ability calls `add` when it hits.

```java
Marks.add(target, player, "conqueror_cull", 5, 60);        // one more of yours, 5 at most; one wears off every 3 seconds
int marks = Marks.count(target, player, "conqueror_cull");

switch (Marks.take(target, player, "conqueror_cull")) {    // a finisher that does more the more marks there are
    case 0 -> { /* not marked */ }
    case 1, 2 -> { /* ... */ }
    default -> { /* 3 or more */ }
}
```

Also `add(target, player, name, amount, max, dropEvery)`, `clear(target, player, name)`, `ticksLeft(target, player, name)` (until all are gone) and `ticksToNextDrop(target, player, name)`. Every `add` starts the wear-off clock again, so marks only drop while you stop marking it. They are not shown: use `SpellFx.overHead` with `count(...)` if you want them seen.
- Saved with the entity. To show them, see `SpellFx.overHead` above.

### Forgotten

A mob forgets ONE entity for a while: it drops it as its target at once and cannot pick it again until the time is up. Everyone else it may still hunt. Unlike `Unnoticed` (every mind slides off the hidden one) and `Unreadable` (nobody far away can pick it out), this is one mob and one target. The Hand Of Order's Halt and Maintain Secrecy.

```java
for (Mob hunter : Forgotten.huntersOf(sl, player, 32)) Forgotten.apply(hunter, player, 100);   // 5 s: they lose you
Forgotten.isForgotten(mob, player)   Forgotten.ticksLeft(mob, player)   Forgotten.remember(mob, player)
```

Applying again keeps the longer time. Not saved.

### Chosen

ONE target a player has picked under a name, for a while: "the enemy I looked at", kept for later. A player has at most one under each name; choosing another lets the first go. The Hand Of Order's Convict (Punishment), the one memorized by Recognition, the Contract's signer and the one weighed by Balance.

```java
Chosen.choose(player, "my_pathway_quarry", target, 600);       // 30 s; an old quarry is let go
LivingEntity quarry = Chosen.of(player, "my_pathway_quarry");    // null: none, dead, or in another world
Chosen.is(target, player, name)   Chosen.ticksLeft(player, name)   Chosen.release(player, name)   Chosen.choosers(target, name)
Chosen.addListener("my_pathway_quarry", (chooser, target) -> MyFx.quarryEnd(...));   // once, in a static block
```

The listener runs whenever a choice ends: its time is up, the target died (then `target` is null), another was chosen, it was released, the chooser died or logged out. Clean up looks and modifiers there, in one place. `Marks` counts how much an entity carries; `Chosen` remembers which entity one player meant. Not saved.

### KeySeal

ONE ability key of a player is dead for a while; the others still work (`Silence` and `AbilityLock` shut every key). The Hand Of Order's Deprivation strips the power used last.

```java
List<Integer> keys = KeySeal.lastUsed(target, 3);   // up to three different keys it pressed lately, the latest first
for (int key : keys) KeySeal.seal(target, key, 600);
KeySeal.lastUsed(target)   KeySeal.isSealed(player, 3)   KeySeal.ticksLeft(player, 3)   KeySeal.unseal(player, 3)   KeySeal.unsealAll(player)
```

The ability keys ask `Decree.blocksKey(player, key)` first (a Decree that prohibits abilities), then `KeySeal.blocksKey(player, key)`, and then call `KeySeal.pressed(player, key)` (`network/SKeyPressAbility...Used`), so every press is remembered and a sealed key is refused with a message. Only players have keys: for a mob's special attacks use `AbilityLock`. Saved with the player.

### Blind

Blindness that works on mobs too.

```java
Blind.apply(target, 80);           // 4 seconds
Blind.apply(target, 80, 5.0);      // ...and a mob still sees what is within 5 blocks
```

A player's screen goes dark (the normal effect). A mob does not care about that effect, so it also loses what it was hunting, and until it can see again it only goes after what is right next to it (`Blind.MOB_SEES`, 3 blocks). `Blind.isBlind(entity)`, `ticksLeft(entity)`, `remove(target)`. Saved with the entity.

### Vulnerable

An entity takes more damage from everything for a while: a curse, broken armor, a mark of weakness.

```java
Vulnerable.add(target, 0.10f, 0.60f, 200);     // 10% more, on top of what is there, 60% at most. Lasts 10 seconds from now
Vulnerable.add(target, 0.25f, 0.25f, 100);     // a fixed 25% that does not stack: the same number twice
```

`Vulnerable.extra(entity)` (0.3 = it takes 30% more right now), `ticksLeft(entity)`, `remove(target)`. The damage is raised before armor is counted. Saved with the entity.

An `add` never lowers or shortens what is already on the target, so two abilities can use it on the same enemy: a fixed 20% from one does not undo a stacked 60% from another.

### Taunt

A mob is made to go after one entity you choose, and nothing else, for a while: a provocation, a decoy, or mobs set on each other.

```java
Taunt.apply(mob, player, 200);                 // 10 seconds: it comes for the caster
Taunt.apply(mob, otherMob, 200);               // it goes after another mob instead
Taunt.brawl(mobs, 200, player.getRandom());    // each mob in the list goes after a random other one of them
```

`Taunt.isTaunted(entity)`, `targetOf(entity)` (who it must go after, or `null`), `ticksLeft(entity)`, `remove(mob)`.

- **Only mobs.** A player has no AI to steer, so `apply` returns `false` for one (and for marionettes, and when the target is itself, dead or concealed). Pair it with something that works on players too when that matters: Provocation adds `Vulnerable` to whatever it hits.
- While it lasts the mob turns down every other target, and it is pointed at its target again every `Taunt.REAIM_EVERY` ticks, also mobs that keep their target in a brain (piglins).
- It ends early when the target dies, leaves the dimension or gets further away than `Taunt.GIVE_UP_DISTANCE` (48), or becomes a creative player.
- `Blind` and `Concealment` win while they last: a blind mob is left alone until it sees again, and nothing is made to go after someone hidden.
- Monsters and neutral mobs (wolves, golems) know how to fight what they target. A cow that is taunted only looks at it; to make anything walk over and hit, see how `Marionette.think` does it.
- `brawl` needs two mobs that can be taunted and returns how many it set; a new taunt replaces the old one. Saved with the entity.

### Burial

An entity is pulled down into the ground, held there and put back where it stood.

```java
if (Burial.bury(target, 20, 40)) { ... }       // under in 1 second, stays for 2. false if it cannot be buried
```

- While it is under it can do nothing (it is rooted for that time) and the ground chokes it the way it chokes anything buried: the game's own damage, about 2 a second.
- When something visible drags it down, give that thing's depth and the two go down together: arms that sink 3.4 blocks with `Ease.IN` in 20 ticks are `Burial.bury(target, 20, 40, 3.4)`. The entity stops going down once all of it is under.
- Not buried: what is buried already, anything taller than `Burial.MAX_HEIGHT` (4) or wider than `MAX_WIDTH` (3), creative players and spectators. `Burial.canBury(target)` tells you beforehand.
- `Burial.isBuried(entity)`, `ticksLeft(entity)`, `release(target)`.
- Saved with the entity: one that is under when the world closes still comes back up when it opens again.

### Flight

A player can fly for a while the way a creative player does.

```java
Flight.grant(player, 400);                                 // 20 seconds. They are lifted off the ground at once
PaleEmperorFx.wings(sl, player, 400, "my_pathway_wings");    // something to show for it (any model works)
```

`Flight.has(player)`, `ticksLeft(player)`, `end(player)`. However it ends, the fall after it does no damage. Creative players and spectators keep their own flying; for them only the time is counted. Saved with the player, so a flight goes on after logging out and in. Models are not: show the wings again when they log in or change dimension (`wingsAgain` in `PaleEmperor.java`).

### Glide

A player is carried along where they look for a while, fast, sinking a little, with no fall damage after.

```java
Glide.start(player, 50, 0.95, 0.08);                   // 50 ticks, 0.95 blocks a tick, sinking 0.08 a tick
Glide.start(player, 50, 0.95, 0.08, 2.5, (glider, passed) -> {
    // at the end: every enemy that came within 2.5 blocks on the way (still alive), the first one passed first
});
BeautyGoddessFx.wings(sl, player, 50, "my_pathway_wings"); // something to show for it
```

`Glide.isGliding(entity)`, `ticksLeft(entity)`, `end(player)` (lands them now; the callback still runs). Looking down dives faster (at most `Glide.MAX_DIVE`), looking up climbs. The landing after it is safe for `Glide.LANDING_GUARD_TICKS`. Not saved: dying or logging out drops the glide without its callback. Used by Wings of Darkness (`wings`, `wingsBurst` in `BeautyGoddess.java`).

### Swarm

For a while someone is many small bodies. A blow kills some of them instead of hurting them: one for every `perBody` damage, at least one. While one is left they cannot be hurt; the blow that kills the last one lands with what is left of it.

```java
Swarm.start(player, 120, 12, 4f, new Swarm.Hooks() {     // 6 seconds, 12 bodies, one dies per 4 damage
    @Override public void bodyLost(LivingEntity who, ServerLevel sl, Vec3 at, int left) { /* a bat bursts at 'at' */ }
    @Override public void over(LivingEntity who, ServerLevel sl, Swarm.End why) { /* TIME, BROKEN or ENDED */ }
});
```

`Swarm.isActive(entity)`, `bodiesLeft(entity)`, `ticksLeft(entity)`, `end(entity)` (its `over` runs with `End.ENDED`). Both hooks are optional. It is only the counting: flying, invisibility and the look are yours to add. The void and `/kill` go through. Works on mobs too. Not saved: dying or logging out drops it without `over`. Used by Bat Swarm Transformation (`batSwarm`, `reform` in `BeautyGoddess.java`).

### Parasite

A player leaves their own body and lives inside another living thing, a mob or a player. The host carries them and keeps acting on its own, unless they steer it (a mob only, see "Steering the host" below). Worm Of Time's Parasite (`wormOfTimeAbility3`) uses it.

```java
Parasite.enter(serverPlayer, host, 2400, HOOKS);     // two minutes at most (0 = no limit). False if refused
Parasite.leave(player, Parasite.End.LEFT);           // back out, standing behind the host
```

`Parasite.isInside(player)`, `hostOf(player)`, `ticksInside(player)`, `isHost(entity)`, `parasiteIn(entity)`, `canEnter(player, host)`.

`Parasite` only moves the player in, carries them and puts them back out. **What the parasite does to its host is the `Hooks` you pass to `enter`.** Every method is optional:

```java
private static final Parasite.Hooks HOOKS = new Parasite.Hooks() {
    @Override public void entered(ServerPlayer parasite, LivingEntity host, ServerLevel sl) { ... }              // once
    @Override public void tick(ServerPlayer parasite, LivingEntity host, ServerLevel sl, int ticksInside) { ... } // every tick
    @Override public void left(ServerPlayer parasite, LivingEntity host, ServerLevel sl, Parasite.End why) { ... } // once
    @Override public boolean allowsKey(int key) { return key == 3; }   // ability keys that work from inside
    @Override public ParasiteControl.Settings control(LivingEntity host) { return STEERING; }   // null = it only rides along
    @Override public void struck(ServerPlayer parasite, LivingEntity host, LivingEntity target, ServerLevel sl) { ... } // after a steered strike
};
```

- While inside, the parasite has no body: nobody sees it, nothing hurts it (except the void and `/kill`), nothing bumps into it, mobs forget it, and `Spells.isEnemy` passes over it. It sees through the host's eyes.
- The ability keys do nothing from inside (`Parasite.blocksKey(player, key)`, asked by every key next to `StolenAbilities.blocksKey`), except the ones `allowsKey` names.
- It ends by `leave(...)`, when the time is up, when the host dies or is gone, or when the parasite dies or logs out. `left(...)` is told which (`Parasite.End`: `LEFT`, `TIME_UP`, `OUT_OF_POWER`, `HOST_DIED`, `HOST_GONE`, `PARASITE_GONE`). The parasite already has its body back then, so `left` can hurt it. `host` is null there if it is no longer in the world.
- `tick` may call `Parasite.leave(parasite, Parasite.End.OUT_OF_POWER)` itself (an upkeep that could not be paid).
- One host carries one parasite. A parasite cannot be a host.
- How it is done: the parasite is in spectator mode with its view on the host. Holding sneak takes the view off the host for as long as it is held; that is the game's doing. The game mode from before is kept in the player's saved data and given back however it ends, also at the next login if the server stopped without warning. Who is in whom is not saved: a restart ends every parasitism.
- Settings: `Parasite.OUT_DISTANCE` (how far behind the host they come out), `Parasite.LANDING_TICKS` (no fall damage for that long afterwards).

#### Steering the host

If the `Hooks` give `Settings` from `control(host)` and the host is a `Mob`, the parasite steers it (`status/ParasiteControl`; the client side is `client/ClientParasiteControl`, the packets `SParasiteControlPacket` and `CParasiteControlPacket`). A player host is never steered; it only carries its parasite.

```java
private static final ParasiteControl.Settings STEERING = new ParasiteControl.Settings(
        40,      // idleTicks: this long without input and the host acts on its own again
        1.0,     // walkSpeed (1 = its normal walking speed)
        1.4,     // sprintSpeed
        3.0,     // reach of a hit, blocks from its eyes
        20,      // strikeRest: ticks between two hits
        2f,      // bareDamage: a hit of a host with no attack of its own (a cow)
        24,      // shotRange of a host that shoots (skeleton, pillager, witch, snow golem)
        30,      // shotRest
        24);     // huntRange of right click
```

| The parasite | The host |
| --- | --- |
| walk keys | walks that way, relative to where the parasite looks. In the air or in water (a bee, a fish, a mob that swims) it goes along the look, up and down too |
| jump | jumps; in the air or in water it rises |
| sprint | walks at `sprintSpeed` |
| mouse | looks there. The parasite's view turns at once; the host's head follows a moment later |
| left click | strikes what the parasite looks at: a mob that shoots (`RangedAttackMob`) shoots it, any other hits it with its own attack (`doHurtTarget`) if it is within `reach`. A host with no attack of its own deals `bareDamage`. `Hooks.struck` is called after |
| right click | on something: hunts it on its own until it dies or another is picked. On nothing: it stops hunting |

- **Your input wins.** While the parasite presses, turns the mouse or clicks, the host's own walking, looking and jumping are switched off (its goals with the `MOVE`, `LOOK` and `JUMP` flags do not run and its path is dropped). After `idleTicks` with no input they are on again and it acts on its own, still hunting what it was told to. Holding sneak (the view out of the host) also lets it be.
- What it strikes or hunts is picked like `Spells.aimEnemy`, from the host's eyes along the parasite's look, not through walls, and never the parasite's allies or its own marionettes.
- The clicks never reach the game while steering, so a spectator's click does not jump the view into what it hits.

To add to what Worm Of Time's parasite does, go to "Parasite: what it does" in `WormOfTime.java`: `parasiteEntered`, `parasiteTick`, `parasiteLeft`, `parasiteStruck` (after a steered strike) and `parasiteRefusal` (who cannot be taken). Each has a `TODO(parasite)` line where new effects go; its numbers are the `PARASITE_` constants at the top of the file.

### Root and Freeze

These were already there and go well with the rest: `Root.apply(target, 80, RootRestriction.EVERYTHING)` holds something in place (see `root/Root.java` for the single restrictions), `Freeze.apply(target, 80)` puts it in ice.

`Root.addHold((entity, restriction) -> ...)`, once in a static block, holds entities as a root would while it says `true`, with no timer and no ring: `Root.has` then answers `true`, so every check of it refuses (the ability keys, teleports, Flaming Jump). `isRooted`, `remove` and `ticksLeft` only know real roots. A `Decree`'s PASSAGE is one.

### Physique: a Beyonder's body

Attribute changes (size, health, reach, attack speed, footing) that are on while something is true and off when it is not. Safe to call every second or every tick: it only touches the attribute when the value has to change. In `blasphemy_slate/beyonder/`.

```java
private static final Identifier TALL = Physique.id("my_pathway_tall");
private static final Identifier HEARTS = Physique.id("my_pathway_hearts");
boolean mine = SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, 6);
Physique.keep(player, Attributes.SCALE, TALL, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, mine);   // 15% taller
Physique.keep(player, Attributes.MAX_HEALTH, HEARTS, 4.0, AttributeModifier.Operation.ADD_VALUE, mine);       // two more hearts
```

`Physique.remove(entity, attribute, id)` and `Physique.has(entity, attribute, id)`. The modifiers are not saved with the player, so call `keep` once a second for everyone (not only those who have it, or it is never taken off): `bodyTick` in `HandOfGod.java` does this for Weapon Mastery, Giant's Physique, the Guardian's footing and the Grandmaster's Edge.

---

## AbilityWheel: many spells on one key

A round menu of spells on one key. Each entry has its own Sequence, spirituality cost and cooldown, all written under its icon; entries above the player's Sequence are greyed out. The last pick is saved on the player, so a sneak press casts it again without the screen.

```java
private static final AbilityWheel SPELLS = AbilityWheel.create("my_pathway_spells", "Spells")
        .entry("fire",  "my_pathway/fire",  "Fireball", 6, 1250, 40,  MyPathway::fireball)
        .entry("storm", "my_pathway/storm", "Storm",    4, 5000, 200, MyPathway::storm)
        .onCast((player, sl, entry) -> addStack(player, sl));          // after every cast that went off

public static void myPathwaySpells(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
    if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return;
    if (!Beyonder.hasSequence(player, 6)) return;                      // the key's own Sequence
    if (player.isShiftKeyDown()) SPELLS.quickCast(player);             // the last pick (none yet: the wheel)
    else SPELLS.open(player);
}

private static AbilityWheel.Result fireball(ServerPlayer player, ServerLevel sl) {
    LivingEntity target = Spells.aimEnemy(player, sl, 24);
    if (target == null) {
        player.sendSystemMessage(Component.literal("No one stands before you."));
        return AbilityWheel.Result.REFUSED;                            // nothing is taken
    }
    Spells.strike(player, target, 20f);
    return AbilityWheel.Result.DONE;                                   // the cost is taken, the cooldown starts
}
```

| Part | What it does |
|---|---|
| `.entry(key, icon, name, sequence, cost, cooldownTicks, cast)` | One spell. `key` names it in the save data: keep it once players have used it. `icon` as for `AbilityMenu` (a PNG under `textures/gui/`, no `.png`) |
| `.free(player -> ...)` | After an entry: while this says true, that entry skips the cost and cooldown checks (the second press of a two-part spell) |
| `.onCast((player, sl, entry) -> ...)` | Runs after every cast that returned `DONE` |
| `.castsWhileSilenced(player -> ...)` | While this says true, a pick from the wheel is not stopped by a root with ABILITIES (pair it with `Silence.addCastsThrough` for the key) |
| `.radius(pixels)` | How far from the middle of the screen the icons sit (default 72) |
| `.quickCastCooldown(false)` | A sneak recast (`quickCast`) ignores cooldowns: it works while the pick is cooling down and starts none. The cost is still paid. Picks from the wheel keep their cooldowns |
| `.quickCastCost(2f)` | A sneak recast costs that many times the entry's cost (default 1). With `.quickCastCooldown(false)`: no cooldown, double the price (Beauty Goddess's two wheels) |
| `.quickCastCostBy(player -> ...)` | The recast price read from the player instead of one fixed number: 3 at Sequence 9, 1 from Sequence 5 (the Prince Of Abolition's wheels, `PrinceOfAbolition.recastCost`) |
| `.noCooldownsWhen(player -> ...)` | While this says true for a player, no entry has a cooldown for them, picked from the wheel or recast (the Prince Of Abolition from Sequence 4) |
| `.cooldownBy((player, ticks) -> ...)` | Every entry's cooldown worked out when it starts and shown under its icon; never longer than the entry's own. Knowledge Emperor's three wheels: 30% shorter while Indoctrination enlightens you (`KnowledgeEmperor.cooldownFor`) |
| `.costBy((player, cost) -> ...)` | After an entry: its cost worked out when it is cast and shown under its icon. `cost` is what it costs otherwise (already times a recast's price). Raise Mausoleum costs 100 more for every mausoleum standing; a Bribe on a Connected enemy costs half |
| `open(player)`, `quickCast(player)`, `cast(player, entry)` | Show it / cast the last pick / cast one entry with every check |
| `lastPick(player)`, `entry(key)`, `entries()`, `unlocked(player)`, `cooldownLeft(player, entry)`, `reset(player)` | Asking about it |
| `resetCooldown(player, key)` | That one entry is ready again now (the Illuminator's arm cannon fires again at once: `IlluminatorWorkshop`) |

What the cast returns:

- `DONE`: it went off. The cost is taken, the cooldown starts, `onCast` runs.
- `FREE`: it went off but costs nothing and starts no cooldown (Trip-web's second end, Web of Fate's recast).
- `REFUSED`: it could not (no target, no room). Nothing is taken; say why in the cast.

The wheel checks the Sequence, the cooldown and that the spirituality is there **before** the cast and takes it **after**, so a cast only checks what is its own. Cast through `Beyonder.castUnbound` (a stolen ability) there is no screen: the thief's last pick, or a random entry, goes off at once. The last pick and the cooldowns are saved on the player.

It is built on `AbilityMenu` (`menu/`), which any menu can use for the same look: `.ring(radius)` lays its icons round a circle, and `.details(player -> ...)` and `.lockedWhen(player -> ...)` after an option give it a second line and grey it out.

Used for keys 4 and 7 in `Apocalypse.java` (`SILKS`, `CATASTROPHES`). A new disaster there is one line in `DISASTERS` and a method.

**The other way round** (sneak opens the wheel, a plain press recasts): call `open` on sneak and `quickCast` otherwise. `quickCast` opens the wheel by itself while there is no last pick. The Prince Of Abolition's three wheels do this (`PrinceOfAbolitionBribes`, `PrinceOfAbolitionFallen`, `PrinceOfAbolitionSovereign`).

---

## Another Pathway's abilities: listing, casting and stealing them

Three small parts. Worm Of Time's Theft (`wormOfTimeAbility2` in `WormOfTime.java`) uses all three.

### PathwayAbilities: every ability as a list

```java
for (PathwayAbilities.Ability ability : PathwayAbilities.of("Wanderer")) { ... }     // its seven, in key order
PathwayAbilities.Ability dash = PathwayAbilities.of("Wanderer", 2);                  // the one on key 2 (H), or null
dash.cast().run(player, level, sl);                                                      // casts it for this player
```

| An ability has | Meaning |
|---|---|
| `pathway()` | The exact Pathway name |
| `key()` | 1 to 7: the ability keys G H J K L N M |
| `name()` | What players are told it is called |
| `sequence()` | The weakest Sequence that can use it |
| `sneakForm()` | `true` if it does something else when cast while sneaking |
| `stealable()` | `false` if it only works for its own Pathway: it spends that Pathway's stars (Star Burst, Orbital), opens its menu (Decree, Ability 5, Words), or does nothing yet |
| `cast()` | Casts it for a player, whatever their Pathway is |

- `cast()` skips the "is this your Pathway" check and nothing else. The ability still needs its Sequence and its spirituality, and it still tells the player why it refuses.
- Which form comes out depends on whether the player is sneaking at that moment, as with a key press.
- `PathwayAbilities.ALL` is the whole list. The ability keys do not use it; they still call the methods from the `SKeyPressAbility...Used` packets.
- A new ability gets one line in `PathwayAbilities.ALL`, with the Sequence its method checks.

### Beyonder.castUnbound: no Sequence, no cost

```java
boolean wentOff = Beyonder.castUnbound(player, () -> ability.cast().run(player, level, sl));
```

While the part in the brackets runs, `Beyonder.hasSequence` is `true` for that player and nothing is taken from their spirituality. So a Sequence 8 player can cast a Sequence 3 ability once.

- **It tells you whether the ability went off.** Every ability checks first and pays after, so one that tried to pay has been cast, and one that did not has refused (no target, no room). `false` means: nothing happened, do not use it up.
- **Only what runs inside is free.** What the ability does later (`Later.run`, an upkeep each tick, like the Pale Emperor's underworld) is paid as usual.
- It is for that one player only. Everyone else is gated and pays as always.
- For this to work, a new ability must check its Sequence with `Beyonder.hasSequence` and pay through `SoulCore` or `Spells.paySpirituality`, as all of them do.
- **A paid version:** `Beyonder.castUnbound(player, 3f, () -> ...)` still opens every Sequence gate, but the cast costs 3 times what the ability itself costs, and is refused with the usual message when the player cannot pay that. While it runs the ability sees a third of the player's spirituality, and every point it takes comes off three times. `0f` is the free cast above. Used by Grazing in `HangedMan.java`.

### StolenAbilities: who holds what

```java
StolenAbilities.Held held = StolenAbilities.hold(thief, ability, false, victim, 1200, 5);   // for a minute, 5 held at most. null if full
StolenAbilities.Held newest = StolenAbilities.newest(thief);                                // what a release casts, or null
StolenAbilities.spend(thief, newest);                                                       // used up: the victim has it back
```

`false` is "its normal form"; `true` holds the sneak form. `victim` is the player it was taken from, or `null` when it was taken from a mob or from nobody.

`StolenAbilities.count(thief)`, `isFull(thief, max)`, `holds(thief, held)`, `isTaken(victim, ability)` and `blocksKey(player, key)` work from anywhere.

- **The victim cannot use that key meanwhile.** Each ability key asks `StolenAbilities.blocksKey(player, key)` before anything else; it tells the player and the key press does nothing. They get the ability back when it is spent, when its time is up, or when the holder dies or logs out.
- **How many can be held is given with each `hold`**, like the time: the last number (`0` or less = no limit). The ability that steals decides it; Worm Of Time's Theft uses `THEFT_MAX_HELD` (5) and refuses a new theft when full. Ask `isFull(thief, max)` with the same number before paying. The same ability of the same player is never held twice at once: check `isTaken` before `hold`.
- **Newest first.** `newest(thief)` is always the one taken last; once it is spent, the one taken before it is next.
- Each held ability has a `slot()` (0, 1, 2...: the lowest one free when it was taken) that stays the same for as long as it is held. Use it to show each in a place and with a tag of its own, as the lights over the thief's head do.
- Both players see a clock by the hotbar (`HudTimer`): the holder with the ability's name, the victim with `Lost: ...`.
- Nothing is saved. A server restart gives everything back.

To release a held ability in the form it was stolen in, whatever the player is doing, set the sneak for the length of the cast, as `castStolen` in `WormOfTime.java` does:

```java
boolean wasSneaking = player.isShiftKeyDown();
player.setShiftKeyDown(held.sneakForm());
try {
    return Beyonder.castUnbound(player, () -> held.ability().cast().run(player, level, sl));
} finally {
    player.setShiftKeyDown(wasSneaking);
}
```

---

## 6. Several models on one entity: tags

A frozen mob can also carry a mark and a marionette's strings. `ParticleShapes.clearModels(sl, mob)` would wipe all of them. Give each effect its own tag and remove only that one:

```java
ModelFx mark = MARK.tag("my_pathway_mark");
ParticleShapes.clearModels(sl, target, "my_pathway_mark");       // cast again: one mark, not two
ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.4, 0), 0f, 0f, 0f);
```

Tags in use: `freeze`, `marionette`, Worm Of Time's `worm_of_time_stolen_<slot>` (one light over the thief's head for each stolen ability) and `worm_of_time_emptied` (the mark under a mob that was stolen from), the Attendant's `aom_strings`, `aom_transfer`, `aom_projection`, `aom_miracle`, and the Pale Emperor's `pale_emperor_hits`, `pale_emperor_wings`, `pale_emperor_crown`, `pale_emperor_curse` (the mark under those on cursed land), `pale_emperor_revive` (the mark of the second life), `pale_emperor_wraith_curse` (the mark under those the wraiths cursed), `pale_emperor_revive_wings` and `pale_emperor_land_<player UUID>` (what the cursed land put on the ground: seal, headstones, arms).

A tag also lets you remove a model that is **not** on an entity before its time is up, for something that must vanish the moment it hits. An `AnimatedShot` does this by itself; this is for when you build something of your own. Give every cast a tag of its own, or one cast would remove the models of another:

```java
String tag = "my_skull_" + UUID.randomUUID();
ParticleShapes.model(sl, SKULL.tag(tag), from, yaw, 0f, 0f);
// later, where it hit:
ParticleShapes.clearModels(sl, where, tag);
```

## Seeker: a model that hunts an enemy

A summoned thing that goes after someone: a raven, a ghost, a wisp, a homing bolt. It flies at its enemy, curves after it when it runs, and hits it on contact. A `Shot` is worked out once and flies straight; a `Seeker` is steered by the server every tick, so its path bends toward wherever its enemy is now.

The one-line version (default speed, turn and life):

```java
Seeker.send(player, sl, GHOST_MODEL, from, target, 10f);     // flies from 'from' at 'target', 10 damage on contact
```

Or describe it once, like a `Shot` (every method returns a new copy):

```java
private static final ModelFx RAVEN_MODEL = ModelFx.of("conqueror/fire_raven").frames(3).smooth()
        .scale(0.8f).pivot(8, 2.5f, 9).glow().unshaded().aura(0xFFFF6A1A, 0.07f, 3);
private static final Seeker RAVEN = Seeker.of(RAVEN_MODEL)   // its FRONT (north in Blockbench) looks where it flies
        .speed(0.9)              // blocks per tick
        .turn(14)                // degrees a tick it can turn toward its enemy
        .life(80)                // ticks before it gives up and bursts
        .width(0.5)              // how close it must come to an enemy's body to hit it
        .flap(6)                 // a model with frames: 0 -> last -> 0 every 6 ticks (wings beating)
        .trail(SpellFx.FIRE_EMBER, 1)
        .burst(SpellFx.FIRE_EMBER, 14);

RAVEN.onHit((target, at) -> Spells.strike(player, target, 10f))
     .launch(player, sl, from, startDirection, target);       // startDirection null = straight at it

RAVEN.onHit(...).launchAt(player, sl, from, enemies, 6, 2);   // a flock of 6, 2 ticks apart, shared out over the enemies
```

| Part | What it does |
|---|---|
| `Seeker.of(model)` | A Seeker that looks like this `ModelFx`. Its front (north) looks the way it flies. A plain model: not `.following(...)` |
| `.pointsWithTop()` | For models that point with their top (up in Blockbench), like a `Shot`: spears, bolts |
| `.speed(blocksPerTick)` | How fast (default 0.8) |
| `.turn(degreesPerTick)` | How sharply it can turn (default 12). Small = wide curves, a quick enemy can dodge it; 180 = turns on the spot |
| `.life(ticks)` | How long it hunts before it bursts (`Ending.EXPIRED`, default 100) |
| `.windup(ticks)` | Hangs in the air, drifting slowly the way it starts, before it goes |
| `.width(blocks)` | How close it must come to a body to hit (default 0.5) |
| `.seek(blocks)` | How far it looks for a new enemy when it has none (default 24); it gives up on one twice that far away |
| `.keepTarget()` | Never takes another enemy: if its own is gone, it bursts (`Ending.LOST`) |
| `.throughWalls()` | Flies through blocks (default: a block stops it, `Ending.WALL`) |
| `.flap(ticks)` | For a model with `.frames(n)`: frame 0 -> last -> 0 every `ticks` ticks, smooth with `.smooth()` |
| `.trail(particle, perTick)` / `.burst(particle, count)` | Particles left behind it / where it ends, whatever the reason |
| `.onHit((target, at) -> ...)` | What happens to the enemy it reaches |
| `.onEnd((at, why) -> ...)` | Runs where it ends: `why` is `HIT`, `WALL`, `LOST`, `EXPIRED` or `GONE` (`GONE` = the caster died or left; `onHit`/`onEnd` do not run then) |
| `.launch(owner, sl, from, startDirection, target)` | Sends one. `target` may be `null`: it flies along `startDirection` and takes the first enemy it finds |
| `.launchAt(owner, sl, from, enemies, count, everyTicks)` | A flock: `count` of them, `everyTicks` apart, fanned out left and right of the look and a little up, each enemy gets its first before any gets a second |

Good to know:

- It hits the **first enemy it touches**, not only its own. Then it is gone.
- Its enemy dies, hides (`Concealment`) or gets too far away: it turns to the **nearest enemy left** that it can see. With none for 12 ticks it bursts.
- It cannot turn on the spot, so an enemy that dodges sideways at the last moment can make it **miss and come round again**. Close to its enemy it turns much harder, so it never circles round one forever.
- **How it is drawn:** the model is played once and then moved every tick with `ParticleShapes.moveModelFacing` (see docs/MODEL_FX.md, "Models the server moves"). The client glides it between those spots like an entity and keeps playing the model's own keys and frames. A model that should do something of its own while it flies (spin, wings) does it in its keys; `.flap(...)` adds the wing beats for you.

---

## 7. Where each tool is used

Every tool has a worked example in `AttendantOfMysteries.java`:

| Tool | Look at |
|---|---|
| `Shot` | `throwDagger` (a fan of them: `paperDaggers`), `airCannon` (a shell with a blast) |
| `AnimatedShot` | `paleEmperorSpearSkull` in `PaleEmperor.java` (a spear that pierces, a skull that bursts; both leave a trail with `.onMove`) |
| `Countdown` | `seizeThreads`, `threadWeb` (silent, with a picture) |
| `SpellFx.tether` | `seizeThreads`, `damageTransfer`, `stitchTogether` |
| `DamageLink.redirect` | `damageTransfer` |
| `DamageLink.share` | `stitchTogether` |
| `Substitute` | `readySubstitute` and `figurineEscape` |
| `Marionette` | `takeHold` |
| `Concealment` | `hideInHistory`, `miracleReturn`; kept up for as long as the caster wants in `enterUnderworld` / `underworldTick` (`PaleEmperor.java`) |
| `DeathWard` | `miracle` and `miracleReturn` |
| `FallGuard` | `attendantOfMysteriesAirCannon` |
| `Spells.pull` | `graftImpact` |
| `Spells.enemiesInSight`, `Marks`, `SpellFx.overHead`, `Blind` | `paleEmperorSpikes`, `spikesHit` in `PaleEmperor.java` |
| `Burial` | `handsTake` in `PaleEmperor.java` |
| `Spells.wave`, `SpellFx.steadyRing` | `screech` in `PaleEmperor.java` |
| `SpellFx.blast` | `skullBursts` in `PaleEmperor.java` |
| `Flight`, `PaleEmperorFx.wings` | `spreadWings`, `wingsTick` in `PaleEmperor.java` |
| `Spells.blinkSpot` | `stepThere` in `PaleEmperor.java` |
| `SpellFx.homing`, `Vulnerable` | `wraithFlies`, `curse` in `PaleEmperor.java` |
| `Zone`, `Spells.keepEffect`, `PaleEmperorFx.crown` (`modelOnHead`) | `cursedLand`, `landCurses` in `PaleEmperor.java` |
| `HudTimer` with `DeathWard` | `secondLife`, `secondLifeShows`, `risesAgain` in `PaleEmperor.java` |
| `SpellFx.circleUnder` | `landCurses` (it grows and reddens with the depth), `secondLifeShows`, `curse` (brighter with every wraith) in `PaleEmperor.java` |
| `SpellFx.tether` from a fixed point to many moving things | `gateDrags` in `PaleEmperor.java` (chains from the gate to those it pulls) |
| `Spells.groundNear` | `landGraves`, `landBeats` in `PaleEmperor.java` |
| `SpellFx.risingRings` | `risesAgain` in `PaleEmperor.java` |
| Model tags | `stringUp`, `summonProjection` |
| `PathwayAbilities`, `Beyonder.castUnbound`, `StolenAbilities` | `steal`, `pickToSteal`, `releaseStolen`, `castStolen` in `WormOfTime.java` |
| `Trap`, a fuse with `Later.run`, a blast that is weaker at its rim | `setTrap`, `springTrap`, `plantCharge`, `burnFuse`, `fireBlast` in `Conqueror.java` |
| `Taunt` (on the caster, and `brawl`) with `Vulnerable` and `SpellFx.circleUnder` | `provoke`, `repugnantAura`, `provokeShows`, `auraShows` in `Conqueror.java` |
| `Transit.steered` with a `Shot` whose range grows with digestion; `Concealment` with a decoy that blows up | `flameTransformation`, `flameRange`, `misdirection`, `fuseBlast` in `Conqueror.java` |
| `ConquerorFx` displays timed to the ability (`trapSet` / `trapSnap`, `powderKeg` with sparks at `kegFuse`, `battleCry`, `flameFigure`) | `setTrap`, `springTrap`, `plantCharge`, `provokeShows`, `misdirection` in `Conqueror.java` |
| `Seeker.launchAt` (a flock of fire ravens with flapping wings), a dash with `FallGuard` | `fireRavens`, `ravenStrikes`, `dash`, `dashTrail` in `Conqueror.java` |
| `SpellFx.tether`, `SpellFx.closingRing`, a model that circles over a head (a pivot away from its middle, and `.spin`) | `theftShows`, `flyInto` in `WormOfTime.java` |
| `Parasite` with `HudTimer`, an upkeep paid once a second | `parasitize`, `parasiteEntered`, `parasiteTick`, `parasiteLeft` in `WormOfTime.java` |
| `Contagion` (a plague that jumps on, stronger each time) | `plague` in `Apocalypse.java` |
| `AbilityWheel` (two wheels of spells, a last pick that the fifth stack reuses) | `SILKS`, `CATASTROPHES`, `lastCatastrophe` in `Apocalypse.java` |
| `Zone` that holds once then slows (a snare), that gets worse every second until frozen, one whose size grows with every victim | `laySnare`, `iceAge`, `blackDeath` in `Apocalypse.java` |
| A line between two points that springs on the first enemy through it, tied with two casts | `tripWeb`, `watchTripLine` in `Apocalypse.java` |
| `Reel` (a grappling line, a pull to the middle), `DamageLink.share` growing as the web catches more | `webSwing`, `tangle`, `drawInFate` in `Apocalypse.java` |
| A drain that heals the caster, and a mob it would kill rising as a `Marionette` instead | `drain`, `riseAsServant` in `Apocalypse.java` |
| Things falling near random enemies in a circle (meteors, lightning), a moving tornado that lifts and throws | `fallingSpot`, `doomFalls`, `tornadoTick` in `Apocalypse.java` |
| `StoredDamage` (a curse that comes due) | `curse` in `Apocalypse.java` |
| `EmpoweredHit` (a backstab), a teleport behind someone that faces them | `fatalStep`, `spotBehind` in `Apocalypse.java` |
| `Substitute` with a mirror that rises and shatters at the attacker | `readyMirrorSubstitute`, `mirrorEscape` in `Apocalypse.java` |
| `Concealment` that the same key ends (out where you aim, or back where you went in) | `enterMirrorWorld`, `exitWhereYouAim`, `returnFromMirror`, `mirrorWorldTick` in `Apocalypse.java` |
| Things on the ground that charge while enemies stand in front of them, and go off together | `placeShard`, `chargeShards`, `shatterShards` in `Apocalypse.java` |
| `Glide` with a `Seeker` flock at the end that goes for everyone passed | `wings`, `wingsBurst` in `BeautyGoddess.java` |
| `Swarm` with `Flight` and invisibility | `batSwarm`, `swarmTick`, `reform` in `BeautyGoddess.java` |
| `Marionette.spawnSummon` (creatures out of a door, one after another), `Spells.growPlants` in a `Zone` | `doorOfSummoning`, `creation` in `BeautyGoddess.java` |
| `AbilityWheel.quickCastCooldown(false).quickCastCost(2f)` (sneak recast: no cooldown, double price) | `POTIONS`, `MOON_WHEEL` in `BeautyGoddess.java` |
| `Concealment` forms that end each other (only one at a time), `DeathWard` armed only inside your own `Zone` | `endForms`, `moonlightForm` in `BeautyGoddess.java` |
| `Dome` (a dome that keeps out, a sphere that holds in, mended in place), `DamageLink.redirect` from every ally | `protection`, `cage`, `mendDome`, `transference` in `HandOfGod.java` |
| `Zone` for friend and foe alike (`affects(one -> true)`), `ConjuredArmor` timed or until cast again, `ConjuredWeapon` with an upkeep and a crescent on a full swing | `lightOfDawn`, `dawnTouches`, `dawnArmor`, `drawSword`, `purifyingHit` in `HandOfGodDawn.java` |
| A hurricane that rolls along a line tick by tick and hits each one once | `releaseHurricane`, `hurricaneReaches` in `HandOfGodDawn.java` |
| `WeaponCoat` chosen by the target, `Unnoticed`, `GiantForm` with madness, reading an enemy into chat | `alchemy`, `coatFor`, `mindConcealment`, `giantForm`, `giantSecond`, `read` in `HandOfGodDemonHunting.java` |
| `Concealment` riding along with a host, `Reel` while hidden, `Spells.lightNear` / `lightAimed`, `Spells.strikeThrough`, `Burial`, `StoredDamage` paid back twice, a `Zone` that weakens what enemies deal | `mercuryLiquefaction`, `mercuryStep`, `lightConcealment`, `twilightSword`, `slam`, `passageOfTime`, `divineKingdom`, `kingdomCut` in `HandOfGodTwilight.java` |
| `Reel` that drags an enemy in and a `Root` that holds it until a fixed moment, `Bleeding`, `Reel` on the caster for a leap, `GiantForm` + `ConjuredWeapon` claws, `Taunt` or `Fear` by what the target is, `Spells.isFullMoonNight` | `chainOne`, `restraint`, `moonstruck`, `pounce`, `claws`, `werewolfForm`, `fullMoonTick` in `Abomination.java` |
| `Fallen` (the dead rise, reserved first), `Zone.around` that freezes each one once, `ConjuredArmor.projectileCut`, one `Countdown` for several targets | `raiseTheDead`, `graveChill`, `undyingFlesh`, `deathSentence` in `AbominationZombie.java` |
| `Hurled` things shared out among the enemies in sight, `DamageLink.share` with a listener for its look, a `Root` renewed while the caster stands still, `Dome.sphere` with a `Zone` inside, a seal that is `Slumber` + `Root` + its own "untouchable" mark (not `Concealment`, which would hide the coffin too), two `Zone`s on one spot (foes and friends) | `animateObjects`, `graveShackles`, `thornedMausoleum`, `coffinOfAges`, `thornGarden` in `AbominationChains.java` |
| `Connection` (the one in sight, else the newest anywhere), `Trap` seen by its owner only, `Zone` + `Taunt.brawl` + `Nocturnality.forceNight`, `Contagion` that deepens by stage, a shrinking `GiantForm` whose end is a `Misfortune.befall` on everyone with your curse (`Misfortune.sourceId`), a wheel entry that ends itself for free (`.free(...)`) | `curseByConnection`, `cursedArtifact`, `riteOfSilence`, `baneMist`, `ancientBlight`, `abominationForm` in `AbominationCurses.java` |
| `Spells.reflectionNear` / `reflectionAimed` with `Concealment` on the way, `Parasite` entered a moment after the cast (paid back if it fails) with an upkeep and the key that steps out, `Marionette.control` on wild vexes before summoning the rest | `mirrorStep`, `possession`, `HOOKS`, `ghostHost` in `AbominationWraith.java` |
| `Study` (every hit on what you know is stronger; an eye only the caster sees through a listener), `Scrolls` (four kinds registered at once, read later, written by hand), `Orbit` with `KnowledgeEmperorFx.starGuardTick` as its look, a wheel whose every entry's cost goes through one rule (`.costBy(KnowledgeEmperor::costFor)`: a free next cast), `Substitute` + `EmpoweredHit` (slip a blow, the next hit is a strike), `Spells.strikeTrue` for "ignores armor" | `pry`, `spellbolts`, `readScroll`, `inscribe`, `sidestep`, `costFor` / `pay` / `afterCast` in `KnowledgeEmperor.java`; `cage`, `stellarSelf`, `fall`, `starGuard` in `KnowledgeEmperorStars.java` |
| `Tellings` (Obscurity), `Slumber` with a stronger waking blow, an armor-free hit on one marked target (as `strikeTrue` does, by hand), `Countdown` with a rebound, `Invulnerable` + a lure to a decoy, `Dispel`, `Study.takeAround`, `Concealment` that ends on any other cast, `AbilityWheel.cooldownBy`, a `Zone` that halves costs and refills a pouch | `beauty`, `achillesHeel`, `prophecyOfRuin` in `KnowledgeEmperorRetellings.java`; `informatization`, `erase`, `condenseKnowledge`, `embodiment`, `indoctrination`, `divineKingdom` in `KnowledgeEmperorKnowledge.java` |
| `Corrosion` with a listener for its look, `Falter` through a mark's miss share, `Wall` (a gate), `Dome.crossedBy` + `Dome.flip`, `Zone.covers` (a field that swings damage and makes casts free), `Zone.addShelter`, `Exile` (an enemy, an ally, yourself), `Spells.strikeTrue`, `AbilityWheel.free` for a recast that closes | `corrosion`, `blast`, `magnify`, `exploit` in `PrinceOfAbolitionFallen.java`; `ruin`, `entropy`, `abolition`, `exile`, `decree` in `PrinceOfAbolitionSovereign.java`; `gate`, `aimedShell`, the events in `PrinceOfAbolition.java` |
| `Appraisal` with a `WeaknessTable` and a listener for the lens, every hit routed through one helper that names its kind (`hit`), `Exposed`, `History` (a step back with half the lost health), `Cooldowns` on single keys, one cost rule for keys and wheels (`costFor` / `pay` / `afterCast`, with `addCostRule` for later), a `Zone` that helps allies and slows enemies and counts its own beats, `Zone.covers` for a bonus to casts made inside it | `appraise`, `exploitFlaw`, `flawGives`, `recall`, `survey`, `ritualCircle`, `costFor`, `lasting` in `Illuminator.java` |
| `Construct` builders with a material table and a decoy (`.shown()` with copied gear and `Taunt`), creative flight with fuel as a jetpack, `Concealment` + `Spells.blinkSpot` as a steam form, a beam through blocks that touches the hidden too (`Spells.isFriend`), `Enfeebled` + `Spells.addCostScale` (Eroded), a wheel entry that is free while what it made lasts (`.free(...)`); `GiantForm` + `ConjuredArmor` that end each other, a held target kept in its cube by hand, `AbilityLock`, `Exile` with a toll on return, a `Zone` that keeps constructs going (`Construct.extend`) and scales costs and cooldowns (`Cooldowns.addScale`) | `livingDoll`, `golem`, `mechanicalBody`, `bodyTick`, `dissociation`, `knowledgeSpirit` in `IlluminatorAlchemy.java`; `rewritePrinciples`, `colossus`, `mathematicalMaze`, `illusoryBook`, `mechanicalHeaven` in `IlluminatorCivilization.java` |
| A hitscan volley that carries the target's weakness, a `Zone` used as a turret (it fires on its own beat, a `Shot` each time), `ConjuredArmor` as a ward that bursts when used up, `Spells.aimAlly`, `AbilityWheel.resetCooldown`; a ladder of curses (`Bleeding` + `NoHeal`, `Enfeebled` + `Vulnerable`), `Gravity` in a `Zone`, `Unreadable` put on again by a `Zone`, a shower that is stronger under another of your zones | `clockworkRifle`, `spiritCannon`, `mysticBrooch` in `IlluminatorWorkshop.java`; `starOfCurses`, `blessingOfStars`, `falseConstellations`, `meteorShower` in `IlluminatorAstronomy.java` |

## Tricks worth copying

All in `AttendantOfMysteries.java`, with the method to look at in brackets.

**Write a spell as a function of where it starts and how hard it hits** (`paperDaggers`, `airCannon`, `flameBurst`). Then the player can cast it and so can anything else: the Historical Void projection repeats them from its own hand at half power with one line each (`echo`). One thing to watch: a copy that repeats a *blast* should do it at once. A moment later it would only find what the first blast threw away.

**Cast again to grow** (`juggle`). A counter and a timer in the player's persistent data. Every cast inside the window adds one; the timer is counted down in the class's tick event.

**Jump points** (`conjureFlame`, `flameToJumpTo`, `aimAngle`). A list of spots you made, and "the one nearest to my crosshair, within 15 degrees" picks between them. Works for any "teleport to my mark" ability.

**Something that keeps pulsing** (`burnInFlame`). A method that does one pulse and then asks `Later.run` to call it again, until its ticks are used up.

**Two models, one flight** (`needleFlight`). The needle and its thread are two models that get the same keys from one method, so they move as one. Use it whenever a solid part and a glowing part belong together.

**Strings on a held enemy** (`stringUp`). Three thin models stuck to the target with `modelOn`, two of them rolled a few degrees outward.

**Timing from one set of numbers** (`draggingArm`, `handsTake` in `PaleEmperor.java`). A few constants (`HANDS_OUT`, `HANDS_GRASP`, `HANDS_PULL`, `HANDS_UNDER`) build the model's keys AND say when the damage lands and the burial starts. Change one number and the animation and what happens move together.

**Stay hidden until something happens** (`enterUnderworld`, `underworldTick` in `PaleEmperor.java`). `Concealment.hide` for a short time, topped up every tick for as long as a flag on the player is set. The flag is cleared by casting again, by dealing damage (a `LivingHurtEvent` whose attacker carries the flag) or when the upkeep cannot be paid.

**A zone that gets worse step by step** (`cursedLand`, `landCurses`, `curseDepth` in `PaleEmperor.java`). One small method turns the time someone has stood in the zone into a depth, and everything the zone does is written from that depth. "It just reached this depth" is `depth > curseDepth(ticksOnLand - LAND_BEAT)`: use it for what should happen once (a sound, a hand that grabs). What must happen once per zone for each enemy is remembered in a `Set<UUID>` made at the cast.

**Everything one cast put down, under one name** (`cursedLand`, `landCloses` in `PaleEmperor.java`). The seal, the headstones and the arms of a cursed land all carry the same tag, made from the caster's id, so one `ParticleShapes.clearModels(sl, ground, tag)` takes all of it away when the land ends early or is cast again. What the cast has to remember (who was told, who was grabbed, which mark each one shows) lives in one small `record` that is handed to every method of the spell.

**What is seen and heard, apart from what happens** (`gateShows` / `gateDrags`, `serpentShows`, `chainsBite` in `PaleEmperor.java`). The pull, the damage and the root are in one method; the rings, mist, sparks and sounds that go with them are in another that only shows things. Either can be changed, or switched off by removing one line, without touching the other. Their moments are the same named ticks the models are built from (`GATE_OPENS`, `SERPENT_BREAKS_OUT`, `SERPENT_BITES`...), so moving one number moves the animation and what is seen and heard at it together.

**Small pictures for what is active** (`showStatus`). `ScreenImages.show(...)` with a duration fades by itself; `ScreenImages.hide(player, id)` removes it early when the effect is used up. The pictures are 16x16 PNGs in `textures/gui/attendant_of_mysteries/`.

---

## Recipe: a mark that goes off after 3 seconds

```java
private static void timeBomb(Player player, ServerLevel sl, LivingEntity target) {
    SpellFx.tether(sl, CHAIN, () -> Countdown.isRunning(player) ? SpellFx.handOf(player, sl) : null,
            () -> SpellFx.middleOf(target, sl), 60);

    Countdown.seconds(3)
            .color(0xFF7A2A)
            .onSecond(left -> SpellFx.closingRing(sl, target.position(), 3, 0xD0FF7A2A, 10, 0))
            .keepWhile(() -> target.isAlive() && target.level() == sl)
            .onFinish(() -> {
                Vec3 center = target.getBoundingBox().getCenter();
                for (LivingEntity enemy : Spells.enemiesAround(player, sl, center, 4)) {
                    Spells.strike(player, enemy, 30f);
                    Spells.push(enemy, center, 1.0, 0.4);
                }
                SpellFx.fireBurst(sl, center, 4);
                Spells.sound(sl, center, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.2f);
            })
            .start(player);
}
```

## Recipe: chain everything in a circle together

```java
private static void chainGang(Player player, ServerLevel sl, Vec3 ground) {
    List<LivingEntity> caught = Spells.enemiesAround(player, sl, ground, 6);
    if (caught.size() < 2) return;

    DamageLink.share(caught, 0.5f, 200, player);
    Vec3 knot = ground.add(0, 1.2, 0);
    for (LivingEntity enemy : caught) {
        // each chain is there for as long as that one is still linked to the others
        SpellFx.tether(sl, CHAIN, () -> knot,
                () -> DamageLink.isShared(enemy) ? SpellFx.middleOf(enemy, sl) : null, 200);
    }
    SpellFx.runeCircle(sl, ground, 6, 0xA0FF4A5A, 200, 1.5f);
}
```

## Recipe: slip away when hit, and leave a blast behind

```java
private static void smokeStep(Player player, ServerLevel sl) {
    Substitute.arm(player, 200, 1, (saved, source, amount, level) -> {
        Vec3 was = saved.position();
        Vec3 away = Spells.freeSpotNear(player, level, was, Spells.flatLook(player).scale(-1), 6);
        if (away != null) {
            saved.teleportTo(away.x, away.y, away.z);
            saved.fallDistance = 0;
        }
        Concealment.hide(saved, 40);                           // gone for 2 seconds
        for (LivingEntity enemy : Spells.enemiesAround(player, level, was.add(0, 1, 0), 3)) {
            Spells.strike(player, enemy, 15f);
        }
        SpellFx.shockRing(level, was, 3, 0xC0C8CCD4, 8, 0);
    });
}
```

---

## Making a status of your own

Copy `status/FallGuard.java`: it is the smallest one (about 70 lines) and has every part.

```java
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Bleeding {

    private static final String TAG = "circle_of_inevitability_bleeding";      // ticks left, saved with the entity

    private Bleeding() {}

    public static void apply(LivingEntity target, int ticks) {
        if (target.level().isClientSide() || ticks <= 0) return;
        target.getPersistentData().putInt(TAG, Math.max(ticks, target.getPersistentData().getIntOr(TAG, 0)));
    }

    public static boolean has(Entity entity) {
        return entity != null && entity.getPersistentData().getIntOr(TAG, 0) > 0;
    }

    public static void remove(LivingEntity target) {
        target.getPersistentData().remove(TAG);
    }

    @SubscribeEvent
    public static void onBleedingLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.getPersistentData().contains(TAG)) return;
        int left = entity.getPersistentData().getIntOr(TAG, 0) - 1;
        if (left <= 0) {
            remove(entity);
            return;
        }
        entity.getPersistentData().putInt(TAG, left);
        if (left % 20 == 0) entity.hurt(entity.damageSources().magic(), 2f);      // once a second
    }
}
```

The `@Mod.EventBusSubscriber` line is all the registering there is. `@SubscribeEvent` is `net.minecraftforge.eventbus.api.listener.SubscribeEvent`.

**Where to keep the status:**

| Keep it in | When | Example |
|---|---|---|
| `entity.getPersistentData()` | It is only numbers and should survive a reload | `FallGuard`, `Concealment`, `Marionette`, `Root`, `Freeze` |
| A `Map<UUID, ...>` in the class | It carries code to run later (a `-> { }` you were given), which cannot be saved, or it ties several entities together for a short time | `Substitute`, `DeathWard`, `Countdown`, `Zone`, `DamageLink`, `Parasite` (plus one saved value: the game mode to give back) |

A map needs cleaning up: remove the entry on `LivingDeathEvent` and on `PlayerEvent.PlayerLoggedOutEvent`.

Reading from the persistent data is `getIntOr(TAG, 0)` (also `getLongOr`, `getFloatOr`, `getDoubleOr`, `getBooleanOr`, `getStringOr`): the second value is what you get when nothing is stored.

**Which event does what:**

| You want to | Event | How |
|---|---|---|
| Count time, do something every tick | `LivingEvent.LivingTickEvent` | |
| Stop a hit before it lands | `LivingAttackEvent` | return `true` (see below) |
| Change how much a hit does, before armor | `LivingHurtEvent` | `event.setAmount(...)`, as `Vulnerable` does |
| See or change what gets through, after armor | `LivingDamageEvent` | `event.getAmount()`, `event.setAmount(...)`, as `DamageLink` does |
| Stop a death | `LivingDeathEvent` | return `true` **and** give health back with `setHealth`, as `DeathWard` does |
| Stop fall damage | `LivingFallEvent` | return `true` |
| Stop a mob from picking a target | `LivingChangeTargetEvent` | return `true`. `getNewTarget()` is who it wanted |
| React to a player's normal attack | `AttackEntityEvent` | `event.getEntity()` is the player |

**Cancelling.** A handler that may stop its event returns `boolean` instead of `void`: `true` stops it, `false` lets it through. There is no `event.setCanceled(true)`. This is the one in `FallGuard`:

```java
@SubscribeEvent
public static boolean onFallGuardLanding(LivingFallEvent event) {
    LivingEntity entity = event.getEntity();
    if (entity.level().isClientSide() || !isProtected(entity)) return false;      // not ours: let it through
    remove(entity);
    return true;                      // cancelled: no damage and no fall sound for this landing
}
```

**Rules for handlers.** The dev runs have strict checks on (`eventbus.api.strictRuntimeChecks` in `build.gradle`), so the game stops at start with an error when one of these is broken:

- Every handler is `static` and has `@SubscribeEvent`. A method that takes an event but has no `@SubscribeEvent` is an error too.
- The class has at least one handler.
- Only an event that can be stopped may have a `boolean` handler. A handler that never stops anything stays `void`.

And one that gives no error: a handler for a parent event is not called for its child events (with a few exceptions, such as `PlayerInteractEvent`), so name the event you mean.

Start every handler with a server check (`if (entity.level().isClientSide()) return;`, or `return false;`), and give handlers names nobody else has (`onBleedingLivingTick`, not `onTick`), as the other statuses do.

If one status must get its say before another, add a priority: `@SubscribeEvent(priority = Priority.HIGH)` (`net.minecraftforge.eventbus.api.listener.Priority`). `Concealment` cancels hits at `HIGH`, so that a hit on someone hidden never uses up their `Substitute`, which listens at `LOW`.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| A shot flies sideways or tail first | Its model's top must be its front. Model it standing up, tip up |
| A shot stops with its middle on the target, or sinks into the wall | Set `.tip(...)`: blocks from the pivot to the tip at the scale you play it |
| A shot thrown from the hand lands beside the crosshair | Fire it along `Spells.aimFrom(player, sl, hand, range)` instead of `player.getLookAngle()` |
| A shot starts inside the wall the player stands next to | Wrap the start in `Spells.clearStart(player, sl, ...)` |
| `onHit` never runs for an enemy that was on the path | It moved more than 2 blocks out of the way before the shot arrived. Make the shot faster |
| `KNIFE.range(12);` changed nothing | Every method returns a new copy. Fire the result |
| An animated shot flies sideways or tail first | Its model's front must be north in Blockbench, and it must fly with `forward(...)`, not `up(...)` |
| An animated shot hits nothing | It only hits while it moves forward. Moving with `up(...)` or `right(...)` alone needs `.anyDirection()` |
| An animated shot vanishes the moment it starts to move | It started inside a block. Wrap the start in `Spells.clearStart(player, sl, ...)` |
| I changed the animation and the hits are late or early | They should not be: they follow the model. Check that the `AnimatedShot` is made from the model you changed |
| My spell hurts my own summons | `Spells.addAllyCheck(...)` |
| Hits in a row only count once | Use `Spells.strike`, not `target.hurt` |
| The countdown never shows numbers | `.silent()` is on, or another countdown replaced it |
| A second countdown killed the first | One per player. Check `Countdown.isRunning(player)` first |
| A tether never appears | One of its ends returned `null` at the very start |
| A dark tether is invisible | Add `.translucent()` to the line |
| A tether cannot be seen from far away | Slash trails are only sent within 32 blocks (models reach 512) |
| The substitute did not react to fire or a fall | By design: attacks, projectiles and explosions only |
| The death ward did not save from the void or `/kill` | By design |
| A wither or poison I keep putting on never hurts | Every `addEffect` starts its clock again, and it only hurts on certain ticks of that clock. Use `Spells.keepEffect` |
| A zone does nothing to my allies / to me | It acts on the owner's enemies. Give it `.affects(...)`; the owner's own part goes in `onPulse` |
| The models of a zone stay when it ends early | Give them a tag of their own and clear it in `.onEnd(...)` |
| A HudTimer still counts though the thing is over | It only shows time. Call `HudTimer.hide(player, name)` where the thing ends early |
| A message I show above the hotbar never appears | The line above the hotbar is rewritten every tick by the spirituality display. Use chat (`sendSystemMessage`), a `HudTimer` or `ScreenImages` |
| My crown or mask is not there in first person | By design: what is worn on the head (`modelOnHead`) is hidden from its wearer in first person. Press F5. For something held in front of the eyes on purpose, give the model `.seenByWearer()` (or use `ParticleShapes.modelInFront`) |
| A Seeker shows up but never moves, or moves in jumps | Its model has `.following(...)` (Seeker removes it), or the game is not sending its moves: each one is a particle packet, so check that particles are not switched off for models. Jumps are lag: the client glides between the spots the server sends |
| A Seeker flies tail first or belly up | Its model's front must be north in Blockbench and its top up. A model that points with its top needs `.pointsWithTop()` |
| A Seeker circles round its enemy | It should not: close to its enemy it turns harder. A very fast one with a tiny `.turn(...)` can still overshoot once; raise the turn |
| A model on someone concealed is gone | By design: what is stuck to a concealed entity is not drawn. Give it `.shownWhileHidden()` when it is what they turned into |
| A steered transit ends at once | They looked at the ground or a wall close by (it hits it), are rooted, or were sneaking with `.letGoOnSneak()` (the first tick does not count) |
| A transit leaves the caster behind | They are rooted with `TELEPORT` (`go` returned `false`), or they died or left on the way |
| A taunted mob ignores the taunt | It is blind, its target is hidden, too far (48 blocks) or in creative, or it is a marionette. Players cannot be taunted at all |
| A marionette stands still | It only attacks what you hit, what hits you or it, or monsters within 16 blocks. Hit something |
| Removing one model from a mob removed the others too | Give each a `.tag(...)` and clear by tag |
| "Not enough spirituality." is in chat, not above the hotbar | The line above the hotbar is rewritten every tick by the spirituality display |
| Something in a `-> { }` of a countdown, zone, substitute or death ward broke | The error is in the game log ("A countdown failed", "A zone failed", ...). The game goes on and that one effect ends |

The code lives in `src/main/java/net/lukario/circleofinevitability/combat/` (`Spells`, `SpellFx`, `Shot`, `AnimatedShot`, `Countdown`, `HudTimer`, `Zone`, `Trap`, `Dome`, `Wall`, `Transit`, `Later`) and `.../status/` (one file per status: `DamageLink`, `Substitute`, `Marionette`, `Concealment`, `DeathWard`, `FallGuard`, `Marks`, `Blind`, `Vulnerable`, `Taunt`, `Burial`, `Flight`, `SupernaturalResistance`, `Decay`, `WeaponCoat`, `Unnoticed`, `GiantForm`, `ConjuredArmor`, `ConjuredWeapon`, `Bleeding`, `Corrosion`, `Cleansing`, `Falter`, `Exile`, `Study`, `Scrolls`, `Dispel`, `Invulnerable`, `Appraisal`, `WeaknessTable`, `Exposed`, `Enfeebled`, `Gravity`, `Unreadable`, `Construct`, `AbilityLock`). `combat/History` remembers where players were, `combat/Cooldowns` keeps single keys' cooldowns, `combat/Residue` where abilities were cast. `combat/Orbit` circles a player with guarding bodies, `combat/Tellings` counts uses. `combat/Fallen` remembers the dead, `combat/Hurled` throws loose things, and `status/Connection` remembers who touched whom. `client/ClientConcealment` and `network/CConcealPacket` are the part of `Concealment` that runs on the players' screens, and `client/HudTimerOverlay` and `network/CHudTimerPacket` are that part of `HudTimer`; you should not need to touch them.
