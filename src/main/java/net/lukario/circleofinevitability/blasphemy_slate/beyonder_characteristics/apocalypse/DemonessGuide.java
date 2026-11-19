package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse;

import net.lukario.circleofinevitability.menu.AbilityGuide;
import net.lukario.circleofinevitability.menu.AbilityGuide.Card;
import net.lukario.circleofinevitability.menu.AbilityGuide.Gesture;

import java.util.ArrayList;
import java.util.List;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse.Apocalypse.*;
import static net.lukario.circleofinevitability.menu.AbilityGuide.Gesture.*;

public final class DemonessGuide {
    private static final String ICON = "ability_menus/apocalypse/";

    private DemonessGuide() {}

    public static AbilityGuide guide() {
        List<Card> cards = new ArrayList<>();
        cards.add(core("Fatal Step", "web_swing", 9, 1, PRESS, STEP_COST, "Step behind your target and empower a melee hit.",
                "20 blocks. Your next melee hit within 2s deals 1.5× its damage plus 16."));
        cards.add(core("Mirror Shard", "ice_age", 9, 1, SNEAK, SHARD_COST, "Place a mirror facing the fight.",
                "Up to 3 shards for 60s. Each enemy in its 7-block front cone adds 8% charge per second. At 34% it weakens. At 67% it blinds and deals 3 damage per second."));
        cards.add(core("Black Flame", "black_flame_rain", 8, 2, PRESS, FLAME_COST, "Fire a cold flame that spreads to nearby enemies.",
                "28 blocks. Deals 22 damage, freezes for 0.6s, then burns for 3 damage each second for 5s. Up to 3 enemies within 5 blocks take half damage."));
        cards.add(core("Mirror Substitute", "cocoon", 8, 2, SNEAK, SUBSTITUTE_COST, "Let a mirror take the next hit and slip away.",
                "Waits 10s for one hit. Escape up to 5 blocks, hide for 1s and return 14 glass damage to the attacker."));
        cards.add(core("Mirror World", "ice_age", 7, 3, PRESS, MIRROR_COST, "Hide inside a mirror and choose an exit.",
                "Gain Speed III for up to 5s. Press again to exit up to 28 blocks away for free. Sneak and press to return to the entrance for 14 damage in 3.5 blocks."));
        cards.add(core("Silk Weaver", "web_of_fate", 6, 4, PRESS, 0, "Choose a silk technique or repeat your last weave.",
                "Six techniques unlock across Sequences 6–3. Opening the wheel is free. Sneak and press to repeat your last choice at its usual cost without a wheel cooldown."));
        cards.add(core("Plague", "black_death", 5, 5, PRESS, PLAGUE_COST, "Infect a target with a spreading plague.",
                "24 blocks. Deals 5 damage per second for 5s. At expiry or death, jumps to 2–3 enemies within 8 blocks. Damage grows 1.5× each generation, up to 4 generations and 24 victims."));
        cards.add(core("Curse", "threads_of_life", 5, 5, SNEAK, CURSE_COST, "Record an enemy’s wounds and replay them.",
                "24 blocks. Stores actual damage taken over 10s and replays 100% once. A fresh Curse replaces the previous debt."));
        cards.add(core("Unaging", "cocoon", 4, 6, PRESS, UNAGING_COST, "Prepare a ward that saves you from death.",
                "Lasts 60s. Return with 50% maximum health and 2s of concealment. Void and kill damage bypass the ward."));
        cards.add(core("Shatter", "hailstorm", 4, 6, SNEAK, SHATTER_COST, "Detonate all your mirrors at their current charge.",
                "12–70 damage per shard. At 34% charge, applies Weakness and Slowness for 5s. Full charge also roots victims for 2.5s."));
        cards.add(core("Catastrophe", "doomsday_sky", 3, 7, PRESS, 0, "Choose a disaster to unleash on aimed ground.",
                "40 blocks. Seven disasters unlock across Sequences 3–1. Opening is free. Sneak and press to repeat your last pick at its usual cost without a wheel cooldown."));
        for (var combo : ApocalypseCombos.ALL) {
            var copy = switch (combo.key()) {
                case "apocalypse_obsidian_lance" -> new Copy("black_flame_rain", "Charge a piercing lance of black fire.", "Release either key. At 0/2/4/6s, deal 18/30/42/54 damage to up to 1/2/3/4 enemies. Range 28 blocks. Applies a flame seal.");
                case "apocalypse_widows_crossing" -> new Copy("trip_web", "Reel a trespasser into your mirror.", "Tie a clear span of 1–24 blocks from your nearest shard. The first enemy gains a silk seal and is pulled in, then rooted for 2s.");
                case "apocalypse_fever_thread" -> new Copy("black_death", "Send plague along a silk seal.", "Aim within 24 blocks at your silk-sealed enemy. After 1s or on death it jumps once within 6 blocks. Damage per second is 5, then 7.5.");
                case "apocalypse_mirror_wake" -> new Copy("hailstorm", "Discharge a mirror in a forward fan.", "Aim at a charged mirror within 24 blocks. Spend half its charge for 20–50 damage in a 10-block, 70° fan.");
                case "apocalypse_omen_of_ruin" -> new Copy("doomsday_sky", "Trade a Despair harvest for catastrophe power.", "Aim within 24 blocks. For 5s, your next catastrophe landing within 6 blocks of the victim’s marked position gains 25 percentage points of power.");
                case "apocalypse_false_footsteps" -> new Copy("web_swing", "Send your reflection ahead to draw pursuit.", "Walks forward for 3s at 0.16 blocks per tick. Hostile mobs within 8 blocks turn to follow it.");
                case "apocalypse_black_wick" -> new Copy("trip_web", "Run black fire along your nearest tripwire.", "Requires your own wire end within 24 blocks. Fire advances up to 1 block per tick, deals 18 damage once per enemy and applies a flame seal. The wire can still catch normally.");
                case "apocalypse_cruel_accounting" -> new Copy("threads_of_life", "Collect your Curse before it expires.", "Within 24 blocks, settle 75% of your stored damage immediately. Ends the Curse and cancels its later payout.");
                case "apocalypse_borrowed_tomorrow" -> new Copy("cocoon", "Give your prepared Unaging ward to an ally.", "Aim within 24 blocks at an ally without a ward. Transfer up to 8s of your remaining protection. They revive with 50% maximum health.");
                case "apocalypse_eye_of_the_storm" -> new Copy("tornado", "Mark a refuge for your next catastrophe.", "Aim within 40 blocks. The 3-block circle lasts 5s. Allies and crouching players inside when you cast are sheltered for that entire catastrophe.");
                default -> throw new IllegalStateException("Missing Demoness guide entry for " + combo.key());
            };
            cards.add(Card.combo("Combos", combo, ICON + copy.icon, copy.summary, copy.details));
        }
        silk(cards, "snare", "Lay a web that catches passing enemies.", "Range 24 blocks, radius 2.5. Lasts 20s, roots each enemy once for 2s, then applies Slowness IV inside. Up to 3 webs.");
        silk(cards, "swing", "Reel yourself to an anchor or pull a light enemy.", "28 blocks. Targets with up to 40 maximum health are pulled to you. Heavy enemies and blocks pull you to them. Landing protection lasts up to 10s.");
        silk(cards, "trip", "Tie a damaging line between two points.", "Aim up to 20 blocks. Tie the free second end within 10s, 1–16 blocks from the first. Lasts 60s. First catch deals 24 damage and knocks down for 2s. Up to 2 lines.");
        silk(cards, "cocoon", "Bind an enemy or wrap yourself for protection.", "16 blocks. Holds an enemy for 4s. With no target, root yourself for 5s with Resistance III and Regeneration III.");
        silk(cards, "threads", "Drain a target and raise a fallen servant.", "16 blocks. Drain 5 health every 0.5s for 5s. A finishing drain can bind a mob with up to 150 maximum health as a servant for 30s.");
        silk(cards, "fate", "Link enemies under a web, then draw it closed.", "28 blocks. Covers a 6-block radius for 8s, shares 50% damage. Recast free from underneath to deal 40 damage and stun for 1.5s.");
        disaster(cards, "earthquake", "Break the ground and throw enemies upward.", "8-block radius, 30 damage. Staggers grounded victims for 1.5s after launch.");
        disaster(cards, "hailstorm", "Pelt enemies with slowing hail.", "7-block radius for 6s. Deals 3 damage every 0.5s and applies Slowness II.");
        disaster(cards, "tornado", "Draw enemies into a moving funnel.", "Travels 0.12 blocks per tick for 8s, pulling within 7 blocks. Its 2-block core deals 2 damage every 0.5s and throws for 22 damage.");
        disaster(cards, "ice_age", "Freeze the ground and suppress healing.", "9-block radius for 10s. Deals 2 damage per second, weakens and slows enemies. After 5s inside, freezes them for 3s.");
        disaster(cards, "black_death", "Raise a plague cloud that grows with each victim.", "Starts at 3 blocks, expands 0.8 per new victim, lasts 12s. Deals 5 damage per second and applies Weakness.");
        disaster(cards, "black_flame_rain", "Rain cursed fire over a target area.", "8-block radius for 8s. One drop every 2 ticks. Each deals 10 damage in 1.6 blocks, burns for 3s and applies Curse if absent.");
        disaster(cards, "doomsday_sky", "Call down meteors, black flame and lightning.", "14-block radius for 15s. During the barrage a strike falls every 6 ticks. Base damage is 30 for meteors, 12 for flame and 18 for lightning.");
        passive(cards, "Weave Despair", "black_death", 5, "Combine flame, silk and plague seals on one enemy.", "Apply all three within 8s to block healing and inflict Weakness II for 6s. Your weave has a 10s lockout on that victim.");
        passive(cards, "Harvest Despair", "threads_of_life", 5, "Spend your Despair with Fatal Step’s melee hit.", "Adds 24 damage, heals 4 health and grants 25% charge to your shards within 12 blocks of the victim.");
        passive(cards, "Mirror Ambush", "ice_age", 7, "Linger in Mirror World to empower your next strike.", "Stay inside for 2s. Your next melee hit within 3s gains 12 damage and a silk seal. Preparation cooldown 10s.");
        passive(cards, "Mirror Fuel", "earthquake", 3, "Spend nearby mirror charge to amplify a disaster.", "Shards within 12 blocks of impact give up to 15% more power each, up to 45% for three full shards. They remain standing.");
        return new AbilityGuide("Demoness", cards);
    }

    private record Copy(String icon, String summary, String details) {}

    private static Card core(String name, String icon, int sequence, int slot, Gesture gesture, int cost, String summary, String details) {
        return new Card("Abilities", name, ICON + icon, summary, details, sequence, cost, 0, gesture, slot, 0);
    }

    private static void silk(List<Card> cards, String key, String summary, String details) {
        cards.add(Card.wheel("Silk", 4, SILKS.entry(key), summary, details));
    }

    private static void disaster(List<Card> cards, String key, String summary, String details) {
        cards.add(Card.wheel("Disasters", 7, CATASTROPHES.entry(key), summary, details));
    }

    private static void passive(List<Card> cards, String name, String icon, int sequence, String summary, String details) {
        cards.add(new Card("Passives", name, ICON + icon, summary, details, sequence, 0, 0, PASSIVE, 0, 0));
    }
}
