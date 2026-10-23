package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

/**
 * Enfeebled: an entity deals less damage for a while, with everything (melee, arrows, abilities: every hit that
 * names it as the attacker, or as the owner of the projectile). The other half of Vulnerable. A mutation that saps
 * the body, a mind eroded, a hand that will not close. Works from any class/ability, on players and mobs:
 *
 *   Enfeebled.apply(target, "my_pathway_sapped", 0.3f, 200);   // 10 seconds: everything it does is 30% weaker
 *   Enfeebled.cut(entity)            // the biggest cut on it now (0.3f = it deals 30% less)
 *   Enfeebled.ticksLeft(entity, "my_pathway_sapped")
 *   Enfeebled.remove(target, "my_pathway_sapped")              // this reason ends; the others go on
 *   Enfeebled.clear(target)
 *
 * Every reason has a name (start it with your Pathway), so two abilities never cut each other short: the cut is the
 * biggest of the reasons still running. Applying a name again never makes it shorter or smaller. The cut is made
 * before the target's armor. Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Enfeebled {

    private static final String TAG = "circle_of_inevitability_enfeebled";     // {name: {Cut, Until}}
    private static final String CUT = "Cut";
    private static final String UNTIL = "Until";

    private Enfeebled() {}

    // =========================
    // API
    // =========================

    /** It deals 'cut' less (0.3f = 30% less) for 'ticks', for the reason 'name'. */
    public static void apply(LivingEntity target, String name, float cut, int ticks) {
        if (target.level().isClientSide() || ticks <= 0 || cut <= 0f) return;
        CompoundTag all = target.getPersistentData().getCompoundOrEmpty(TAG);
        long now = target.level().getGameTime();
        for (String key : new ArrayList<>(all.keySet())) {
            if (all.getCompoundOrEmpty(key).getLongOr(UNTIL, 0L) <= now) all.remove(key);   // old reasons go
        }
        CompoundTag had = all.getCompoundOrEmpty(name);
        CompoundTag one = new CompoundTag();
        one.putFloat(CUT, Math.min(1f, Math.max(cut, had.getFloatOr(CUT, 0f))));
        one.putLong(UNTIL, Math.max(now + ticks, had.getLongOr(UNTIL, 0L)));
        all.put(name, one);
        target.getPersistentData().put(TAG, all);
    }

    /** The biggest cut on it right now (0 = none). */
    public static float cut(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0f;
        CompoundTag all = entity.getPersistentData().getCompoundOrEmpty(TAG);
        long now = entity.level().getGameTime();
        float best = 0f;
        for (String key : all.keySet()) {
            CompoundTag one = all.getCompoundOrEmpty(key);
            if (one.getLongOr(UNTIL, 0L) > now) best = Math.max(best, one.getFloatOr(CUT, 0f));
        }
        return best;
    }

    public static int ticksLeft(Entity entity, String name) {
        if (entity == null) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getCompoundOrEmpty(name).getLongOr(UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    public static void remove(LivingEntity target, String name) {
        if (!target.getPersistentData().contains(TAG)) return;
        CompoundTag all = target.getPersistentData().getCompoundOrEmpty(TAG);
        all.remove(name);
        if (all.isEmpty()) target.getPersistentData().remove(TAG);
        else target.getPersistentData().put(TAG, all);
    }

    public static void clear(LivingEntity target) {
        target.getPersistentData().remove(TAG);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onEnfeebledHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || attacker == event.getEntity()) return;
        float cut = cut(attacker);
        if (cut > 0f) event.setAmount(event.getAmount() * (1f - cut));
    }
}
