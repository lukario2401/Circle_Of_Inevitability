package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which mobs are Beyonders, how often, of which Pathway, of which Sequence, and how much stronger it makes them
 * (docs/BEYONDER_MOBS.md). Every number to rebalance is in this file. BeyonderMobs does the rest.
 *
 * A Rule is one kind of mob. Two are written below:
 *   HOSTILE  every hostile mob (anything that is an Enemy): 5% are Beyonders, Sequence 9 to 5.
 *   BOSS     the Wither, the Warden and the Ender Dragon: 50% are Beyonders, Sequence 4 to 0.
 * In each, the weight of a Sequence is how likely it is next to the others in that Rule (weight 60 against a total of
 * 100 = 60%). Stronger Sequences have smaller weights, so they are rarer.
 *
 * A custom mob later:
 *   - a hostile one (its class implements Enemy) is a HOSTILE Beyonder by itself, nothing to do;
 *   - to make it a boss, or give it its own odds, add a line to the static block at the bottom:
 *         register(ModEntities.MY_BOSS.get(), BOSS);
 *         register(ModEntities.MY_GHOUL.get(), Rule.chance(0.20).pathways(HandOfOrder.PATHWAY)
 *                 .sequence(8, 70, 0.5, 0.3).sequence(7, 30, 1.0, 0.5));
 *   - to keep a hostile mob from ever being one: register(EntityTypes.SILVERFISH, Rule.NEVER);
 *   (register can also be called from anywhere else, any time before the mob spawns, e.g. in common setup.)
 */
public final class BeyonderMobRules {

    private BeyonderMobRules() {}

    // =========================
    // The Pathways mobs can have. Add a Pathway's name here (as in SequenceCommands.PATHWAYS) and mobs start being it.
    // =========================
    public static final List<String> MOB_PATHWAYS = List.of(
            KeyOfStars.PATHWAY,         // "Key Of Stars"
            HandOfOrder.PATHWAY,        // "Hand Of Order"
            StarDragon.PATHWAY);        // "Star Dragon"

    // =========================
    // Every hostile mob
    //                                   Sequence  weight  extra health  extra damage   (0.5 = +50%)
    // =========================
    public static final Rule HOSTILE = Rule.chance(0.05)                           // 5% of hostile mobs
            .pathways(MOB_PATHWAYS)
            .sequence(9,        55,     0.25,         0.15)
            .sequence(8,        25,     0.50,         0.30)
            .sequence(7,        12,     1.00,         0.50)
            .sequence(6,         6,     1.50,         0.75)
            .sequence(5,         2,     2.00,         1.00);

    // =========================
    // Bosses: more likely to be Beyonders, and only they reach Sequence 4 and stronger.
    // Their health is already huge, so the extra is smaller.
    // =========================
    public static final Rule BOSS = Rule.chance(0.50)                              // 50% of bosses
            .pathways(MOB_PATHWAYS)
            .sequence(4,        55,     0.25,         0.15)
            .sequence(3,        25,     0.50,         0.30)
            .sequence(2,        12,     0.75,         0.50)
            .sequence(1,         6,     1.00,         0.75)
            .sequence(0,         2,     1.50,         1.00);

    // =========================
    // Looks (BeyonderMobs)
    // =========================

    /** The name above its head ("Zombie (Hand Of Order, Sequence 7)"). False: no name is given at all. */
    public static boolean SHOW_NAME = true;
    /** True: the name shows always, like a player's. False: only when you look straight at the mob. */
    public static boolean NAME_ALWAYS_VISIBLE = true;
    /** The faint aura of its Pathway's color: a few motes this often (ticks; 0 = no aura). */
    public static int AURA_EVERY_TICKS = 10;
    /** How many motes each time. */
    public static int AURA_MOTES = 2;

    /** Mobs an ability called up (Marionette.summon: The Fallen, Zombie...) are never Beyonders and drop nothing. */
    public static boolean SUMMONS_NEVER = true;

    // =========================
    // Which Rule a mob follows
    // =========================

    private static final Map<EntityType<?>, Rule> BY_TYPE = new HashMap<>();

    /** This kind of mob follows 'rule' (instead of HOSTILE, or at all if it is not hostile). Rule.NEVER: never one. */
    public static void register(EntityType<?> type, Rule rule) {
        BY_TYPE.put(type, rule);
    }

    /** The Rule for this mob: its own if one was registered, else HOSTILE for a hostile mob, else null (never). */
    public static Rule ruleFor(LivingEntity entity) {
        if (entity instanceof Player) return null;                          // players have their own Pathway
        Rule own = BY_TYPE.get(entity.getType());
        if (own != null) return own == Rule.NEVER ? null : own;
        return entity instanceof Enemy ? HOSTILE : null;
    }

    /** True for a mob that has the BOSS rule (or one of its own registered as a boss with register(type, BOSS)). */
    public static boolean isBossType(LivingEntity entity) {
        return BY_TYPE.get(entity.getType()) == BOSS;
    }

    static {
        register(EntityTypes.WITHER, BOSS);
        register(EntityTypes.WARDEN, BOSS);
        register(EntityTypes.ENDER_DRAGON, BOSS);
    }

    // =========================
    // Rule
    // =========================

    /**
     * One kind of mob's odds. Build it with Rule.chance(...).pathways(...).sequence(...)...; each sequence(...) line is
     * one Sequence it can be: its weight, and how much more health and damage it gets (0.5 = +50%, 0 = none).
     */
    public static final class Rule {

        /** Never a Beyonder (register a mob type with it to keep it out). */
        public static final Rule NEVER = new Rule(0);

        /** What one Sequence of this Rule gives: how likely it is, and the extra health and damage. */
        public record Tier(int sequence, int weight, double extraHealth, double extraDamage) {}

        private final double chance;
        private final List<String> pathways = new ArrayList<>();
        private final List<Tier> tiers = new ArrayList<>();

        private Rule(double chance) {
            this.chance = Math.max(0, Math.min(1, chance));
        }

        /** 0..1: how many of these mobs are Beyonders (0.05 = 5%). */
        public static Rule chance(double chance) {
            return new Rule(chance);
        }

        /** Which Pathways (each as likely as the next). */
        public Rule pathways(List<String> names) {
            pathways.addAll(names);
            return this;
        }

        public Rule pathways(String... names) {
            return pathways(List.of(names));
        }

        /** One Sequence it can be (see the class comment). Writing the same Sequence twice replaces it. */
        public Rule sequence(int sequence, int weight, double extraHealth, double extraDamage) {
            int clamped = Math.max(Beyonder.STRONGEST_SEQUENCE, Math.min(Beyonder.WEAKEST_SEQUENCE, sequence));
            tiers.removeIf(tier -> tier.sequence() == clamped);
            tiers.add(new Tier(clamped, Math.max(0, weight), extraHealth, extraDamage));
            return this;
        }

        public double chance() {
            return chance;
        }

        public List<String> pathways() {
            return List.copyOf(pathways);
        }

        public List<Tier> tiers() {
            return List.copyOf(tiers);
        }

        /** The Tier of that Sequence in this Rule, or null if this Rule has none for it. */
        public Tier tier(int sequence) {
            for (Tier tier : tiers) {
                if (tier.sequence() == sequence) return tier;
            }
            return null;
        }

        /** A random Pathway of this Rule, or null if it has none. */
        public String rollPathway(RandomSource random) {
            return pathways.isEmpty() ? null : pathways.get(random.nextInt(pathways.size()));
        }

        /** A random Sequence by the weights, or -1 if this Rule has none (or all weights are 0). */
        public int rollSequence(RandomSource random) {
            int total = 0;
            for (Tier tier : tiers) total += tier.weight();
            if (total <= 0) return -1;
            int pick = random.nextInt(total);
            for (Tier tier : tiers) {
                pick -= tier.weight();
                if (pick < 0) return tier.sequence();
            }
            return -1;
        }
    }
}
