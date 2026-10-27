# Snake Of Mercury — first playable slice

Select `Snake Of Mercury` using `/sequence` at Sequence 9 or stronger. G casts Danger
Premonition; sneak-G casts Spirit Vision. Both cost 250 spirituality. Like Worm Of
Time's regular keys, neither has a fixed cooldown: recasting replaces its duration
rather than accumulating dodge charges. Future menu wheels still own their entry cooldowns.

The hotbar Wheel shows the next Omen and seconds until its next automatic stop.
It rerolls every eight seconds and after a successful native cast. Failed payment
leaves it unchanged. Fortune grants 1.25× duration and refunds half the spirituality
actually spent; Even grants normal duration; Misfortune grants .75× duration and
Slowness II for two seconds. Sequence changes immediately refresh the distribution.
Odds follow the supplied design (15/70/15 at 9–8, 30/60/10 at 7–4, 40/60/0 at 3,
50/50/0 at 2, 65/35/0 at 1). At Sequence 0 Fortune is the default; an explicit
Fortune/Even selection control belongs to the later capstone implementation.

Danger Premonition arms one avoided combat hit for eight seconds (three from Sequence
3). A visible mob targeting you flickers red. On a hit, a collision-checked sidestep
tries to move you two blocks; teleport restrictions are respected. The attacker
stumbles and gains one shared Misfortune level. Environmental damage still follows
the existing Substitute rules: this does not prevent drowning, lava or ordinary falls.

Spirit Vision lasts 15 seconds and reveals nearby living entities using the existing
Eye of Demon Hunting glowing-effect mechanism. Its range is 32 blocks, 64 from
Sequence 2. Silver/red motes indicate ordinary/high misfortune; a Snake player's
Fortune Omen adds gold motes. These world effects are visible to other nearby players,
as with the existing glowing-effect implementation. The targeted enemy's actual
Misfortune level appears beside the hotbar, including zero, and clears when you look
away. Snake players can always read this counter; borrowed Spirit Vision can read it
for its duration. No separate permanent above-head counters are added.

The existing shared Misfortune API and listeners remain in use. Its cap is now 10;
random checks are 5% per level per second, capped at 50%, with three seconds between
random mishaps. A Snake-applied buildup begins decaying after five seconds, losing
one level every three seconds. Accidents never increase their own buildup. Fumbles
weaken the next attack without damaging gear. Falling-stone/calamity damage is capped
at 6/10 (16 for catastrophe) and respects normal damage invulnerability frames.
Existing curse callers and explicit forced mishaps retain their entry points.
These balance changes also affect other pathways using shared Misfortune.

Only the Wheel, Key 1 and shared misfortune improvements are implemented here.
The PDF's other keys, silver-serpent form, timeline and capstone are future work.

## Validation

Run `./gradlew build` with Java 25 and the supplied HTTPS proxy/trust configuration.
The dependency-free distribution test can also run separately:

```sh
javac -d /tmp/wheel-tests src/main/java/net/lukario/circleofinevitability/fate/FateOdds.java tools/tests/FateOddsTest.java
java -cp /tmp/wheel-tests FateOddsTest
```

In a development game, select the pathway, aim at a hostile mob, cast G and let it
attack, then cast sneak-G. Verify one dodge, the counter change and decay, the
Omen reroll, spirit reveal expiry, and HUD cleanup after changing pathways. Repeat
at Sequence 3 for three dodge charges and with insufficient spirituality for no reroll.
