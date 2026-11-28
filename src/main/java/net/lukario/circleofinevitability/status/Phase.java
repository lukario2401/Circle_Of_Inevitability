package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Casts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Phase: an entity slips between the Spirit World and reality. It is hidden and untouchable (status/Concealment), a
 * player may fly while it lasts (status/Flight), and it cannot touch the world: any attack, item or block use, or cast
 * brings it back. A Pixie's Phase, a ghost, anything "untouchable until it acts". Works from any class/ability:
 *
 *   Phase.enter(player, 1200, "my_pathway_phase");          // 60 seconds; false when a Decree forbids hiding
 *   Phase.bring(ally, player, 1200, "my_pathway_phase");    // an ally goes along and comes back when the leader does
 *   Phase.isPhased(entity)    Phase.ticksLeft(entity)    Phase.source(entity)    Phase.leave(entity)
 *   Phase.addSeer((viewer, phased) -> ...)                  // a power that sees the phased (it does not hurt them)
 *   Phase.seenBy(viewer, phased)                            // ask before showing a phased one to someone
 *   Phase.addCastException(cast -> ...)                     // a cast that does not bring them back (the toggle itself)
 *   Phase.addListener((entity, why) -> ...)                 // once, in a static block: the look of coming back
 *
 * Upkeep is the ability's own (pay it once a second and call leave when it cannot be paid). It ends on death and on
 * logout. Not saved: nobody is left between worlds after a restart (Concealment's own time still runs out).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Phase {

    /** Why a Phase ended. */
    public enum Why { TIME, LEFT, ACTED, BROKEN, LEAD_LEFT, DIED }

    private static final String UNTIL = "circle_of_inevitability_phase_until";
    private static final String SOURCE = "circle_of_inevitability_phase_source";
    private static final String LEAD = "circle_of_inevitability_phase_lead";
    private static final String FLEW = "circle_of_inevitability_phase_flew";

    private static final List<BiPredicate<LivingEntity, LivingEntity>> SEERS = new ArrayList<>();
    private static final List<Predicate<Casts.Cast>> CAST_EXCEPTIONS = new ArrayList<>();
    private static final List<BiConsumer<LivingEntity, Why>> LISTENERS = new ArrayList<>();

    static {
        Casts.addListener((caster, sl, cast) -> {
            if (!isPhased(caster)) return;
            for (Predicate<Casts.Cast> exception : CAST_EXCEPTIONS) {
                if (exception.test(cast)) return;
            }
            leave(caster, Why.ACTED);
        });
    }

    private Phase() {}

    // =========================
    // API
    // =========================

    /** Into the Phase for 'ticks'. Already phased: only the time changes. False when it could not hide (a Decree). */
    public static boolean enter(LivingEntity entity, int ticks, String source) {
        if (ticks <= 0 || !entity.isAlive() || !(entity.level() instanceof ServerLevel sl)) return false;
        Concealment.hide(entity, ticks);
        if (!Concealment.isHidden(entity)) return false;
        entity.getPersistentData().putLong(UNTIL, sl.getGameTime() + ticks);
        entity.getPersistentData().putString(SOURCE, source == null ? "" : source);
        if (entity instanceof Player player && !Flight.has(player)) {
            Flight.grant(player, ticks);
            entity.getPersistentData().putBoolean(FLEW, true);
        }
        return true;
    }

    /** 'companion' goes along with 'lead' and comes back when 'lead' does. */
    public static boolean bring(LivingEntity companion, LivingEntity lead, int ticks, String source) {
        if (!enter(companion, ticks, source)) return false;
        companion.getPersistentData().putString(LEAD, lead.getUUID().toString());
        return true;
    }

    public static boolean isPhased(Entity entity) {
        return entity != null && entity.getPersistentData().getLongOr(UNTIL, 0L) > entity.level().getGameTime();
    }

    public static int ticksLeft(Entity entity) {
        if (entity == null) return 0;
        return (int) Math.max(0L, entity.getPersistentData().getLongOr(UNTIL, 0L) - entity.level().getGameTime());
    }

    /** What put it there ("" when it is not phased). */
    public static String source(Entity entity) {
        return isPhased(entity) ? entity.getPersistentData().getStringOr(SOURCE, "") : "";
    }

    /** Back into reality now. Does nothing if it is not phased. */
    public static void leave(LivingEntity entity) {
        leave(entity, Why.LEFT);
    }

    public static void leave(LivingEntity entity, Why why) {
        if (!entity.getPersistentData().contains(UNTIL)) return;
        entity.getPersistentData().remove(UNTIL);
        entity.getPersistentData().remove(SOURCE);
        entity.getPersistentData().remove(LEAD);
        boolean flew = entity.getPersistentData().getBooleanOr(FLEW, false);
        entity.getPersistentData().remove(FLEW);
        if (Concealment.isHidden(entity)) Concealment.reveal(entity);
        if (flew && entity instanceof Player player && Flight.has(player)) Flight.end(player);
        for (BiConsumer<LivingEntity, Why> listener : LISTENERS) listener.accept(entity, why);
    }

    /** A power that sees the phased. Once, in a static block. */
    public static void addSeer(BiPredicate<LivingEntity, LivingEntity> seer) {
        SEERS.add(seer);
    }

    /** True when 'viewer' can see 'phased' (or it is not phased at all). */
    public static boolean seenBy(LivingEntity viewer, LivingEntity phased) {
        if (!isPhased(phased)) return true;
        for (BiPredicate<LivingEntity, LivingEntity> seer : SEERS) {
            if (seer.test(viewer, phased)) return true;
        }
        return false;
    }

    /** A cast that does not bring a phased caster back. Once, in a static block. */
    public static void addCastException(Predicate<Casts.Cast> exception) {
        CAST_EXCEPTIONS.add(exception);
    }

    /** Runs whenever someone comes back out of a Phase. Once, in a static block. */
    public static void addListener(BiConsumer<LivingEntity, Why> listener) {
        LISTENERS.add(listener);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onPhaseLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.tickCount % 5 != 0 || !entity.getPersistentData().contains(UNTIL)) return;
        if (!isPhased(entity)) {
            leave(entity, Why.TIME);
            return;
        }
        if (!Concealment.isHidden(entity)) {
            leave(entity, Why.BROKEN);                       // something pulled it out of hiding
            return;
        }
        String lead = entity.getPersistentData().getStringOr(LEAD, "");
        if (lead.isEmpty() || !(entity.level() instanceof ServerLevel sl)) return;
        try {
            Entity leader = sl.getEntity(UUID.fromString(lead));
            if (!isPhased(leader)) leave(entity, Why.LEAD_LEFT);
        } catch (IllegalArgumentException e) {
            leave(entity, Why.LEAD_LEFT);
        }
    }

    @SubscribeEvent
    public static void onPhaseAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide()) leave(event.getEntity(), Why.ACTED);
    }

    @SubscribeEvent
    public static void onPhaseUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getEntity().level().isClientSide()) leave(event.getEntity(), Why.ACTED);
    }

    @SubscribeEvent
    public static void onPhaseUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().level().isClientSide()) leave(event.getEntity(), Why.ACTED);
    }

    @SubscribeEvent
    public static void onPhaseUseEntity(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getEntity().level().isClientSide()) leave(event.getEntity(), Why.ACTED);
    }

    @SubscribeEvent
    public static void onPhaseDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) leave(event.getEntity(), Why.DIED);
    }

    @SubscribeEvent
    public static void onPhaseLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        leave(event.getEntity(), Why.LEFT);
    }
}
