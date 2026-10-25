package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * The mob version of one ability of a Pathway (docs/BEYONDER_MOBS.md, "Mobs casting"). The player abilities are
 * written for players (the look, sneaking, chat, the hotbar), so a mob casts its own short version of them, which
 * is registered in MobAbilities. It keeps the player ability's Pathway, key, name and Sequence, so what a mob can
 * cast is always what a player of that Pathway and Sequence could.
 *
 *   MobAbilities.register(MobAbility.of(HandOfOrder.PATHWAY, 1, MobAbility.NORMAL)   // Halt: key 1, not sneaking
 *           .cost(HALT_COST)                  // spirituality (MobSpirituality)
 *           .cooldown(160)                    // ticks, for this mob
 *           .range(HALT_RANGE).sight()        // its target within 16 blocks, and seen
 *           .when((mob, target, sl) -> !Root.isRooted(target))     // only worth it then (optional)
 *           .cast(HandOfOrderMobs::halt));    // does it: true if it went off, false = refused, nothing spent
 *
 * MobCasting checks everything before cast() runs: the mob's Pathway, its Sequence, its spirituality, this
 * ability's cooldown, the range, sight and when(). cast() only does the ability. It pays and starts the cooldown
 * after cast() returns true. Use MobSpells for the mob side of the combat kit (its enemies, strikes, lines, facing).
 */
public final class MobAbility {

    public static final boolean NORMAL = false, SNEAK = true;

    /** Does the ability. True if it went off (it is then paid for), false if it refused (nothing is spent). */
    @FunctionalInterface
    public interface Cast {
        boolean run(Mob caster, LivingEntity target, ServerLevel sl);
    }

    /** Whether it is worth casting now (it is never cast when this says no). */
    @FunctionalInterface
    public interface When {
        boolean test(Mob caster, LivingEntity target, ServerLevel sl);
    }

    private final String pathway;
    private final int key;
    private final boolean sneak;
    private final PathwayAbilities.Ability ability;     // the player version (null if that key is not listed)
    private String entry = "";                          // a wheel's entry (a key with a wheel has one per entry)
    private String name;
    private int sequence;
    private float cost;
    private int cooldown = 100;
    private double range = 16.0;                        // 0 = any range (a cast on itself)
    private boolean sight;
    private int weight = 10;
    private When when;
    private Cast cast;

    private MobAbility(String pathway, int key, boolean sneak) {
        this.pathway = pathway;
        this.key = key;
        this.sneak = sneak;
        this.ability = PathwayAbilities.of(pathway, key);
        this.name = ability == null ? pathway + " " + key : ability.name();
        this.sequence = ability == null ? 9 : ability.sequence();
    }

    /**
     * The mob version of that Pathway's ability on that key (1..7, as in PathwayAbilities), normal or sneak form.
     * Its name and Sequence come from PathwayAbilities; a sneak form has its own name, give it with named(...), and
     * its own Sequence if it needs a stronger one, with sequence(...).
     */
    public static MobAbility of(String pathway, int key, boolean sneak) {
        return new MobAbility(pathway, key, sneak);
    }

    // ---------- building (once, when it is registered)

    public MobAbility named(String name) { this.name = name; return this; }

    /**
     * One entry of that key's wheel ("whip_of_pain", as the wheel names it). Mobs have no wheels: each entry is an
     * ability of its own, with its own cooldown, and a key with a wheel registers one MobAbility per entry.
     */
    public MobAbility entry(String entry) { this.entry = entry == null ? "" : entry; return this; }

    /** The weakest Sequence that casts it (default: the player ability's). */
    public MobAbility sequence(int sequence) { this.sequence = sequence; return this; }

    /** Spirituality it costs (use the player ability's constant). */
    public MobAbility cost(float cost) { this.cost = Math.max(0f, cost); return this; }

    /** Ticks before this mob can cast it again (times BeyonderMobRules.COOLDOWN_SCALE). */
    public MobAbility cooldown(int ticks) { this.cooldown = Math.max(0, ticks); return this; }

    /** Its target must be within this many blocks. */
    public MobAbility range(double blocks) { this.range = Math.max(0, blocks); return this; }

    /** No range: it is cast on itself or round itself (Slip Free). */
    public MobAbility anyRange() { this.range = 0; return this; }

    /** It must see its target (no walls between). */
    public MobAbility sight() { this.sight = true; return this; }

    /** How likely it is picked among the ones it can cast now (default 10; 20 = twice as likely). */
    public MobAbility weight(int weight) { this.weight = Math.max(0, weight); return this; }

    public MobAbility when(When when) { this.when = when; return this; }

    public MobAbility cast(Cast cast) { this.cast = cast; return this; }

    // ---------- asking

    public String pathway() { return pathway; }
    public int key() { return key; }
    public boolean sneak() { return sneak; }
    /** The wheel entry it is ("" for a key without a wheel). */
    public String entry() { return entry; }
    /** The player version in PathwayAbilities, or null. */
    public PathwayAbilities.Ability ability() { return ability; }
    public String name() { return name; }
    public int sequence() { return sequence; }
    public float cost() { return cost; }
    public int cooldown() { return cooldown; }
    public double range() { return range; }
    public boolean needsSight() { return sight; }
    public int weight() { return weight; }
    public When when() { return when; }
    public Cast cast() { return cast; }

    /**
     * One name per Pathway, key and form: "Hand Of Order/1", "Hand Of Order/1/sneak", and a wheel's entries
     * "Hand Of Order/3/whip_of_pain". Its cooldown is kept under it.
     */
    public String id() {
        return pathway + "/" + key + (entry.isEmpty() ? (sneak ? "/sneak" : "") : "/" + entry);
    }

    @Override
    public String toString() {
        return name + " (" + id() + ", Sequence " + sequence + ")";
    }
}
