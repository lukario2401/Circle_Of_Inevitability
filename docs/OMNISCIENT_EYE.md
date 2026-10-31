# Omniscient Eye: first 13 abilities and replication

Set up with `/sequence pathway @s "Omniscient Eye"` and `/sequence set @s 6`
(operators only). Keys use the usual G, H, J, K, L bindings for keys 1–5.

| Key | Sequence | Normal | Sneak |
| --- | --- | --- | --- |
| 1 | 9 | Reading: health, mood, hunted target, effects, Appraisal mark; weakness from Sequence 8 | Ritual Circle: chant, allied protection/cleansing, slowed hostile projectiles and a barrier to summoned constructs |
| 2 | 8 | Deduction: Appraisal mark, weakness, reduced enemy damage, projectile forecasts | Keen Eye: hidden trap locations, projectile paths, exposes enemies looked at directly |
| 3 | 7 | Reconstruction: nearby casts and player movement trails replayed as ghosts | Deductive Strike: step and three hits; a marked target takes stronger hits and a fourth hit |
| 4 | 6 | Replicate the selected stored ability | Open the scrollable studied-ability list |
| 5 | 6 | Mystic Arts wheel (Analyze at 6; Mirror Curse, Vile Thoughts, Improvise, Living Tome at 5; Golden Armor Art at 4) | Repeat the last wheel pick |

This implements the plan's first 13 active abilities, with its old Imitate replaced by persistent
Replication and Analyze moved onto the Mystic Arts wheel. Keys 6–7 and the later 13 abilities remain
for the next implementation. The asset collection covers those later abilities already.

Reading shows a decorative floating information card; the actual inspected values are written in chat.
Reconstruction uses wireframe proxies for cast locations and recorded player trails, not exact replays
of every original spell mesh. Deduction forecasts projectile paths; it does not simulate future melee AI.
Improvise chooses an elemental weakness and applies the corresponding hit/status without changing terrain.
Living Tome uses an invisible summoned golem with the animated page-and-ink model.

## Studying powers

From Sequence 6 onward, observing a real, copyable player or Beyonder-mob cast within **32 blocks**
adds study. The caster must be in your forward 120-degree field of view, with line of sight.
Your own casts and replicated/echo casts never generate study. Normal and sneak forms are separate
records; wheel spells use their individual entry, name, icon and Sequence.

You cannot learn, select or replicate a power stronger than your current Sequence. Lower Sequence
numbers mean stronger powers. Sequence metadata comes from the shared ability catalog; individual
wheel entries have their own Sequence, while single-key variants use their key's catalog Sequence.
Powers marked as needing their original pathway's private state are excluded, including Replication itself.

Study gain per observation, in percentage points:

```text
min(100, 5 × 2^(abilitySequence - observerSequence) / (10 - abilitySequence))
```

This makes stronger observers faster and stronger abilities harder. Sequence 6 studying a Sequence 6
power gains 1.25% per observation; Sequence 0 studying a Sequence 9 power gains 100% immediately.
Study is capped at 100% and is never spent when casting. There is no slot limit or automatic forgetting.

Progress, favorites and the selected power are stored in the player's persistent soul NBT, so they
survive saving, relogging, server restarts and death through the existing SoulCore clone handler.
Changing pathways preserves the library but disables access. A Sequence downgrade preserves records
but prevents use of stronger powers.

## Stored-ability menu

Sneak + **key 4 / K** opens a vertically scrolling list. Each row shows the ability's name, Sequence,
study percentage, favorite star and icon when a corresponding texture is available. The selected row
is highlighted. Click a row to select it and close the menu; click its star to toggle its favorite.
Use the mouse wheel to scroll. Esc closes the menu.

Order is favorites first, then highest study, then strongest Sequence (lowest number), then name.
The server validates each choice and handles favorites; clients cannot grant study or request unknown powers.
Reopen the menu to refresh study gained while it was open.

Normal **key 4 / K** casts the selection in the stored normal/sneak form, aimed using your current position
and crosshair. Replication has **no cooldown**, including the copied ability's key/wheel cooldown.
The copied code keeps your real Sequence gates, so a weak stored power cannot unlock upgrades above your Sequence.
Each successful cast pays **1.5 times the original spirituality cost**. No target or insufficient
spirituality refuses the original cast as usual. Partial study scales replicated damage from 40% to 100%
using `0.4 + 0.6 × study / 100`; the shared Echo system carries this into supported delayed hits/projectiles.
Healing, durations and utility effects retain their original behavior, as with the existing Echo system.
The pathway's own Mystic Arts retain the planned per-entry cooldowns.

## Operator command

```mcfunction
/codex study <player> <ability-id>
/codex study @s key/Wanderer/2/false
/codex study @s wheel/omniscient_eye_mystic_arts/golden_armor_art
```

Requires OP permission level 2. Tab completion lists copyable catalog IDs. It immediately grants 100%
study, while enforcing the target's pathway and Sequence restrictions. Key IDs are
`key/<exact pathway name>/<1-7>/<true for sneak or false for normal>`; wheel IDs are
`wheel/<wheel-id>/<entry-key>`. Pathway names can contain spaces; the final argument consumes the rest of the command.

## Verification

```powershell
./gradlew build
./gradlew runGameTestServer -PomniscientEyeTests
```

The five focused real-server tests exercise study eligibility/rate/order, visible/behind-wall observation,
NBT-backed storage including the death clone handler, and server-side
selection checks, paid repeated replication with no cooldown or recursive observation, unfunded casts,
and the Mystic Arts' own cooldown and refused-target behavior. The menu compiles against the current
client UI API; its appearance and mouse interaction still benefit from a development-client playtest.
