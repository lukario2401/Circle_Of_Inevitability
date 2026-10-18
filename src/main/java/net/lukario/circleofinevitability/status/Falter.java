package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Falter: for a while part of an entity's attacks go wide. Arrogance, a scrambled mind, a shaking aim, smoke in the
 * eyes. Works from any class/ability, on players and mobs, for every attack that has an attacker: melee, arrows,
 * abilities.
 *
 *   Falter.apply(target, "my_pathway_smoke", 0.4f, 120);    // 6 seconds: 40% of its attacks miss
 *   Falter.chance(entity)                                   // the miss chance right now (0..1)
 *   Falter.ticksLeft(entity, "my_pathway_smoke")    Falter.remove(target, "my_pathway_smoke")    Falter.clear(target)
 *   Falter.addListener((attacker, victim, name) -> ...)     // the look of a miss ('name': the one that made it miss)
 *
 * Every name is its own reason to miss, so two abilities never cut each other short: the chance is the biggest of
 * the reasons still running, and removing one leaves the others. Applying a name again starts it over with the new
 * numbers. Start the name with your Pathway. Damage that bypasses invulnerability (the void, /kill) is never missed.
 * Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Falter {

    /** Runs for every attack that missed. 'name' is the reason with the biggest chance. */
    @FunctionalInterface
    public interface Listener {
        void missed(LivingEntity attacker, LivingEntity victim, String name);
    }

    private static final String TAG = "circle_of_inevitability_falter";
    private static final String CHANCE = "_chance";
    private static final String UNTIL = "_until";
    private static final String NAMES = "names";                 // every reason's name, ';' between them

    private static final List<Listener> LISTENERS = new ArrayList<>();

    private Falter() {}

    /** 'chance' (0.4 = 40%) of its attacks miss for 'ticks', for this reason. */
    public static void apply(LivingEntity target, String name, float chance, int ticks) {
        if (target.level().isClientSide() || ticks <= 0 || chance <= 0f) return;
        CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
        tag.putFloat(name + CHANCE, Math.min(1f, chance));
        tag.putLong(name + UNTIL, target.level().getGameTime() + ticks);
        List<String> names = names(tag);
        if (!names.contains(name)) names.add(name);
        tag.putString(NAMES, String.join(";", names));
        target.getPersistentData().put(TAG, tag);
    }

    /** The chance that its next attack misses, right now. */
    public static float chance(Entity entity) {
        return entity == null ? 0f : strongest(entity).chance;
    }

    public static int ticksLeft(Entity entity, String name) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(name + UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    /** This one reason ends now; the others go on. */
    public static void remove(LivingEntity target, String name) {
        if (!target.getPersistentData().contains(TAG)) return;
        CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
        tag.remove(name + CHANCE);
        tag.remove(name + UNTIL);
        List<String> names = names(tag);
        names.remove(name);
        if (names.isEmpty()) {
            target.getPersistentData().remove(TAG);
            return;
        }
        tag.putString(NAMES, String.join(";", names));
        target.getPersistentData().put(TAG, tag);
    }

    private static List<String> names(CompoundTag tag) {
        List<String> names = new ArrayList<>();
        for (String name : tag.getStringOr(NAMES, "").split(";")) if (!name.isEmpty()) names.add(name);
        return names;
    }

    /** Every reason ends now. */
    public static void clear(LivingEntity target) {
        target.getPersistentData().remove(TAG);
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    private record Reason(String name, float chance) {}

    /** The live reason with the biggest chance (chance 0 when there is none). Tidies up the ones that ran out. */
    private static Reason strongest(Entity entity) {
        if (!entity.getPersistentData().contains(TAG)) return new Reason("", 0f);
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        long now = entity.level().getGameTime();
        Reason best = new Reason("", 0f);
        List<String> over = new ArrayList<>();
        for (String name : names(tag)) {
            if (tag.getLongOr(name + UNTIL, 0L) <= now) {
                over.add(name);
                continue;
            }
            float chance = tag.getFloatOr(name + CHANCE, 0f);
            if (chance > best.chance) best = new Reason(name, chance);
        }
        if (!over.isEmpty() && entity instanceof LivingEntity living) for (String name : over) remove(living, name);
        return best;
    }

    // returning true cancels the attack
    @SubscribeEvent
    public static boolean onFalterAttacked(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) return false;
        if (!attacker.getPersistentData().contains(TAG)) return false;
        Reason reason = strongest(attacker);
        if (reason.chance <= 0f || attacker.getRandom().nextFloat() >= reason.chance) return false;
        for (Listener listener : LISTENERS) {
            try {
                listener.missed(attacker, victim, reason.name);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A falter listener failed", e);
            }
        }
        return true;
    }
}
