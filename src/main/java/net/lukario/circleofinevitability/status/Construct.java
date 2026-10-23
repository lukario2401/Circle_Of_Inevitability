package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Construct: what is made rather than born. Golems of iron and snow, and whatever an ability builds (a doll, a golem of
 * brass, a clockwork soldier). Two halves:
 *
 * ASKING. Abilities that take things apart ask it (the Illuminator's Exploit Flaw and Dissociation):
 *
 *   if (Construct.isConstruct(target)) ...          // iron and snow golems, anything built or marked, anything a rule names
 *   Construct.mark(mob);                            // a creation of your own counts from now on (saved with it)
 *   Construct.addCheck(entity -> ...);              // once, in a static block: one more kind of made thing
 *
 * BUILDING. A vanilla mob called up for its owner (status/Marionette's summon: it fights for them and vanishes without
 * drops), given the body you say and hidden under a model of your own:
 *
 *   Mob golem = Construct.of(EntityTypes.IRON_GOLEM)
 *           .health(200).damage(20).lasts(1800).leash(40)
 *           .limit("my_pathway_golem", 1)               // one at a time: a second one ends the first (REPLACED)
 *           .onEnd((made, why) -> MyFx.golemEnd(...))   // DIED, TIME, REPLACED, LEASH, OWNER_GONE, ENDED
 *           .build(player, sl, feet, yaw);              // null if it could not be made there
 *   MyFx.golemBody(sl, golem, 1800, tag);               // the look is yours
 *
 *   Construct.ofOwner(player, "my_pathway_golem")    Construct.ownerOf(entity)    Construct.isBuilt(entity)
 *   Construct.ticksLeft(mob)    Construct.extend(mob, 20)    Construct.end(mob)    Construct.endAll(player, group)
 *
 * The vanilla body is invisible (no particles) and silent unless .shown(). lasts(0) is no timer at all. With a leash it
 * ends when its owner is further than that, in another dimension or gone. What it is built from is not saved: after a
 * restart what is left is a plain summon that runs out (Marionette's own timer, 'lasts' plus two seconds, or an hour).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Construct {

    /** Why a built construct ended. */
    public enum End { DIED, TIME, REPLACED, LEASH, OWNER_GONE, ENDED }

    @FunctionalInterface
    public interface Ended {
        void run(LivingEntity construct, End why);
    }

    private static final String TAG = "circle_of_inevitability_construct";       // true: made, not born
    private static final int NO_TIMER_SUMMON = 72000;                             // Marionette's own timer with no timer of ours
    private static final Identifier HEALTH_ID = Physique.id("construct_health");
    private static final Identifier DAMAGE_ID = Physique.id("construct_damage");
    private static final Identifier SPEED_ID = Physique.id("construct_speed");
    private static final Identifier SCALE_ID = Physique.id("construct_scale");
    private static final Identifier STEADY_ID = Physique.id("construct_steady");

    private static final List<Predicate<LivingEntity>> CHECKS = new ArrayList<>();

    /** One built construct: whose, which group, until when (Long.MAX_VALUE: no timer), its leash and its end. */
    private static final class Built {
        final Mob mob;
        final UUID owner;
        final String group;
        long until;
        final double leash;
        final Ended onEnd;

        Built(Mob mob, UUID owner, String group, long until, double leash, Ended onEnd) {
            this.mob = mob;
            this.owner = owner;
            this.group = group;
            this.until = until;
            this.leash = leash;
            this.onEnd = onEnd;
        }
    }

    private static final Map<UUID, Built> BUILT = new HashMap<>();

    // ---------- what it is made as ----------

    private final EntityType<? extends Mob> type;
    private float health = 0f;          // 0: its own
    private float damage = -1f;         // below 0: its own
    private float speed = 1f;
    private float scale = 1f;
    private int lasts = 600;            // 0: no timer
    private double leash = 0.0;         // 0: none
    private boolean shown = false;
    private boolean steady = false;
    private String group = "construct";
    private int limit = 0;              // 0: no limit
    private Ended onEnd = null;

    private Construct(EntityType<? extends Mob> type) {
        this.type = type;
    }

    /** A construct built on this vanilla mob. Nothing happens until .build(...). */
    public static Construct of(EntityType<? extends Mob> type) {
        return new Construct(type);
    }

    public Construct health(float health) { this.health = Math.max(1f, health); return this; }
    /** What its own blows do (mobs with no attack of their own ignore it). */
    public Construct damage(float damage) { this.damage = Math.max(0f, damage); return this; }
    /** Its walking speed, times its own. */
    public Construct speed(float multiplier) { this.speed = Math.max(0f, multiplier); return this; }
    /** Its size, times its own (the game's scale attribute: the body, reach and step go with it). */
    public Construct scale(float multiplier) { this.scale = Math.max(0.1f, multiplier); return this; }
    /** How long it stands, in ticks; 0 = until it dies or is ended. */
    public Construct lasts(int ticks) { this.lasts = Math.max(0, ticks); return this; }
    /** It ends when its owner is further than this (or gone). 0 = never. */
    public Construct leash(double blocks) { this.leash = Math.max(0.0, blocks); return this; }
    /** Its vanilla body is seen (by default it is invisible and silent, and only your model shows). */
    public Construct shown() { this.shown = true; return this; }
    /** No knockback. */
    public Construct steady() { this.steady = true; return this; }
    /** At most 'max' of this group for one owner: one more ends the oldest (REPLACED). */
    public Construct limit(String group, int max) { this.group = group; this.limit = Math.max(0, max); return this; }
    public Construct onEnd(Ended action) { this.onEnd = action; return this; }

    /** Builds it with its feet at 'feet', facing 'yaw', for 'owner'. Returns the mob, or null if it could not be made. */
    public Mob build(Player owner, ServerLevel sl, Vec3 feet, float yaw) {
        int summonTicks = lasts > 0 ? lasts + 40 : NO_TIMER_SUMMON;
        Mob mob = Marionette.spawnSummon(type, owner, sl, feet, yaw, summonTicks);
        if (mob == null) return null;
        mark(mob);
        if (scale != 1f) Physique.keep(mob, Attributes.SCALE, SCALE_ID, scale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
        if (speed != 1f) Physique.keep(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, speed - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
        if (steady) Physique.keep(mob, Attributes.KNOCKBACK_RESISTANCE, STEADY_ID, 1.0, AttributeModifier.Operation.ADD_VALUE, true);
        if (damage >= 0f) {
            AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) Physique.keep(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID, damage - attack.getValue(), AttributeModifier.Operation.ADD_VALUE, true);
        }
        if (health > 0f) {
            Physique.keep(mob, Attributes.MAX_HEALTH, HEALTH_ID, health - mob.getMaxHealth(), AttributeModifier.Operation.ADD_VALUE, true);
            mob.setHealth(mob.getMaxHealth());
        }
        if (!shown) {
            mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, summonTicks, 0, false, false));
            mob.setSilent(true);
        }
        long until = lasts > 0 ? sl.getGameTime() + lasts : Long.MAX_VALUE;
        if (limit > 0) {
            List<Mob> mine = ofOwner(owner, group);
            while (mine.size() >= limit) {
                Mob oldest = mine.remove(0);
                finish(oldest, End.REPLACED);
            }
        }
        BUILT.put(mob.getUUID(), new Built(mob, owner.getUUID(), group, until, leash, onEnd));
        return mob;
    }

    // =========================
    // API: asking
    // =========================

    /** True for something made: an iron or snow golem, anything built or mark(...)ed, anything an addCheck rule names. */
    public static boolean isConstruct(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (living.getType() == EntityTypes.IRON_GOLEM || living.getType() == EntityTypes.SNOW_GOLEM) return true;
        if (living.getPersistentData().getBooleanOr(TAG, false)) return true;
        for (Predicate<LivingEntity> check : CHECKS) if (check.test(living)) return true;
        return false;
    }

    /** From now on it counts as made (saved with it). */
    public static void mark(LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG, true);
    }

    /** One more kind of made thing: Construct.addCheck(entity -> MyGolems.isMine(entity)). */
    public static void addCheck(Predicate<LivingEntity> check) {
        CHECKS.add(check);
    }

    /** True for a construct built with build(...) that still stands. */
    public static boolean isBuilt(Entity entity) {
        return entity != null && BUILT.containsKey(entity.getUUID());
    }

    /** Who built it, or null. */
    public static UUID ownerOf(Entity entity) {
        Built built = entity == null ? null : BUILT.get(entity.getUUID());
        return built == null ? null : built.owner;
    }

    /** The constructs of this group the owner has standing, oldest first (group null: all of them). */
    public static List<Mob> ofOwner(Player owner, String group) {
        List<Built> found = new ArrayList<>();
        for (Built built : BUILT.values()) {
            if (built.owner.equals(owner.getUUID()) && (group == null || built.group.equals(group)) && built.mob.isAlive()) found.add(built);
        }
        found.sort((a, b) -> Integer.compare(b.mob.tickCount, a.mob.tickCount));      // the longest alive first
        List<Mob> mobs = new ArrayList<>();
        for (Built built : found) mobs.add(built.mob);
        return mobs;
    }

    /** Ticks until it ends by itself (0: not built; Integer.MAX_VALUE: no timer). */
    public static int ticksLeft(Entity entity) {
        Built built = entity == null ? null : BUILT.get(entity.getUUID());
        if (built == null) return 0;
        if (built.until == Long.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.max(0, built.until - entity.level().getGameTime());
    }

    /** It stands 'ticks' longer (a Divine Kingdom where made things do not run out). */
    public static void extend(Entity entity, int ticks) {
        Built built = entity == null ? null : BUILT.get(entity.getUUID());
        if (built != null && built.until != Long.MAX_VALUE) built.until += ticks;
    }

    /** Ends it now (ENDED): its onEnd runs, then it is gone without drops. */
    public static void end(LivingEntity entity) {
        if (entity instanceof Mob mob && isBuilt(mob)) finish(mob, End.ENDED);
    }

    /** Ends every construct of this group the owner has (group null: all). */
    public static void endAll(Player owner, String group) {
        for (Mob mob : ofOwner(owner, group)) finish(mob, End.ENDED);
    }

    // =========================
    // Internals
    // =========================

    private static void finish(Mob mob, End why) {
        Built built = BUILT.remove(mob.getUUID());
        if (built == null) return;
        if (built.onEnd != null) {
            try {
                built.onEnd.run(mob, why);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A construct's end failed", e);
            }
        }
        if (why != End.DIED && mob.isAlive()) mob.discard();
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onConstructLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (BUILT.isEmpty() || !(event.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        boolean leashes = now % 20 == 0;
        for (Built built : new ArrayList<>(BUILT.values())) {
            if (built.mob.level() != sl) continue;
            if (!built.mob.isAlive()) {
                BUILT.remove(built.mob.getUUID());
                continue;
            }
            if (built.until <= now) {
                finish(built.mob, End.TIME);
                continue;
            }
            if (!leashes) continue;
            Player owner = sl.getPlayerByUUID(built.owner);
            if (owner == null || !owner.isAlive()) {
                finish(built.mob, End.OWNER_GONE);
            } else if (built.leash > 0 && owner.distanceTo(built.mob) > built.leash) {
                finish(built.mob, End.LEASH);
            }
        }
    }

    @SubscribeEvent
    public static void onConstructDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide() && isBuilt(mob)) finish(mob, End.DIED);
    }

    @SubscribeEvent
    public static void onConstructOwnerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        for (Mob mob : ofOwner(event.getEntity(), null)) finish(mob, End.OWNER_GONE);
    }

    @SubscribeEvent
    public static void onConstructLevelUnload(LevelEvent.Unload event) {
        BUILT.values().removeIf(built -> built.mob.level() == event.getLevel());
    }
}
