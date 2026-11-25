package net.lukario.circleofinevitability.item.armor;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiPredicate;

/** Server-authoritative windup/strike/recovery loop reusable by future equipment and powers. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TentacleCombat {
    public static final String VISUAL_KEY = "coi_tentacle_strike";
    public record Profile(double range, float damage, int cooldownTicks, int windupTicks, int animationTicks) {
        public Profile {
            if (!Double.isFinite(range) || range <= 0 || !Float.isFinite(damage) || damage <= 0
                    || windupTicks < 1 || animationTicks <= windupTicks || cooldownTicks < animationTicks)
                throw new IllegalArgumentException("Invalid tentacle attack profile");
        }
    }
    /** What a landed strike does beyond the base hit. Defaults reproduce the Inevitability tentacles. */
    public interface Impact {
        default float damage(LivingEntity wearer, LivingEntity target, float base) { return base; }
        default void landed(ServerLevel level, LivingEntity wearer, LivingEntity target) {
            level.playSound(null, wearer.blockPosition(), SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, .7f, .65f);
        }
    }
    public static final Impact TENTACLES = new Impact() {};
    private static final class State {
        long readyAt, hitAt;
        UUID target;
        ItemStack source;
        Profile profile;
    }
    // Values contain no entity references, so unloaded wearers cannot be retained by this cache.
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    private TentacleCombat() {}

    /** Hostile species, plus neutral mobs actively attacking the wearer; always honor the mod's ally rules. */
    public static boolean hostile(LivingEntity wearer, LivingEntity target) {
        return target instanceof Mob mob && (target instanceof Enemy && !(target instanceof NeutralMob) || mob.getTarget() == wearer)
                && Spells.isEnemy(wearer, target);
    }
    public static boolean canHit(LivingEntity wearer, LivingEntity target, Profile profile,
                                 BiPredicate<LivingEntity, LivingEntity> targets) {
        return target != wearer && target.isAlive() && !target.isSpectator() && !target.isInvulnerable()
                && wearer.level() == target.level() && targets.test(wearer, target)
                && wearer.distanceToSqr(target) <= profile.range * profile.range && wearer.hasLineOfSight(target);
    }

    @SubscribeEvent public static void tickArmor(LivingEvent.LivingTickEvent event) {
        LivingEntity wearer = event.getEntity();
        if (!(wearer.level() instanceof ServerLevel)) return;
        if (wearer.isAlive() && UnshadowedArmor.fullSet(wearer)) UnshadowedArmor.cleanse(wearer);
        // One handler for every set: the per-wearer state is shared, so a set must never cancel another's strike.
        boolean inevitability = InevitabilityArmor.wearing(wearer, EquipmentSlot.CHEST);
        boolean unshadowed = !inevitability && UnshadowedArmor.wearing(wearer, EquipmentSlot.CHEST);
        if (!(inevitability || unshadowed) || !wearer.isAlive() || wearer.isSpectator()
                || Root.has(wearer, RootRestriction.ABILITIES) || Root.has(wearer, RootRestriction.ATTACK)) {
            cancel(wearer);
            return;
        }
        ItemStack chest = wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (inevitability) tick(wearer, chest,
                InevitabilityArmor.fullSet(wearer) ? InevitabilityArmor.FULL_SET : InevitabilityArmor.CHEST,
                TentacleCombat::hostile);
        else tick(wearer, chest, UnshadowedArmor.fullSet(wearer) ? UnshadowedArmor.FULL_SET : UnshadowedArmor.CHEST,
                TentacleCombat::hostile, UnshadowedArmor.IMPACT);
    }

    /** Call once each server tick. One shared cooldown per wearer prevents swapping items to spam attacks. */
    public static void tick(LivingEntity wearer, ItemStack source, Profile profile,
                            BiPredicate<LivingEntity, LivingEntity> targets) {
        tick(wearer, source, profile, targets, TENTACLES);
    }
    /** As above, with set-specific damage scaling and impact effects. */
    public static void tick(LivingEntity wearer, ItemStack source, Profile profile,
                            BiPredicate<LivingEntity, LivingEntity> targets, Impact impact) {
        if (!(wearer.level() instanceof ServerLevel level) || !wearer.isAlive()) return;
        long now = level.getGameTime();
        State state = STATES.computeIfAbsent(wearer, ignored -> new State());
        if (state.source != null && state.source != source) cancel(wearer);
        if (state.target != null) {
            if (now < state.hitAt) return;
            Entity found = level.getEntity(state.target);
            state.target = null;
            if (found instanceof LivingEntity target && canHit(wearer, target, profile, targets)) {
                var damage = wearer instanceof Player player ? wearer.damageSources().playerAttack(player)
                        : wearer.damageSources().mobAttack(wearer);
                // Use the weaker of queued/current profiles if pieces were removed during windup.
                float amount = impact.damage(wearer, target, Math.min(profile.damage, state.profile.damage));
                if (target.hurtServer(level, damage, amount)) {
                    source.hurtAndBreak(1, wearer, EquipmentSlot.CHEST);
                    impact.landed(level, wearer, target);
                }
            }
        }
        if (now < state.readyAt) return;
        clearVisual(state.source);
        state.source = null;
        // Stagger scans among wearers. Only a small nearby box is queried, five times a second while idle.
        if ((wearer.tickCount + wearer.getId()) % 4 != 0) return;
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, wearer.getBoundingBox().inflate(profile.range),
                        other -> canHit(wearer, other, profile, targets)).stream()
                .min(Comparator.<LivingEntity>comparingDouble(wearer::distanceToSqr).thenComparingInt(Entity::getId)).orElse(null);
        if (target == null) return;
        state.source = source;
        state.target = target.getUUID();
        state.profile = profile;
        state.hitAt = now + profile.windupTicks;
        state.readyAt = now + profile.cooldownTicks;
        // Vanilla equipment synchronization carries this small visual snapshot to tracking clients.
        // No client prediction, per-tick packets, or client-authored damage is involved.
        CompoundTag visual = new CompoundTag();
        visual.putLong("started", now);
        visual.putInt("duration", profile.animationTicks);
        visual.putInt("windup", profile.windupTicks);
        visual.putString("dimension", level.dimension().identifier().toString());
        visual.putDouble("x", target.getX());
        visual.putDouble("y", target.getY() + target.getBbHeight() * .5);
        visual.putDouble("z", target.getZ());
        CustomData.update(DataComponents.CUSTOM_DATA, source, tag -> tag.put(VISUAL_KEY, visual));
    }

    /** Interrupt immediately, keeping the cooldown so rapid unequip/re-equip cannot bypass recovery. */
    public static void cancel(LivingEntity wearer) {
        State state = STATES.get(wearer);
        if (state == null) return;
        clearVisual(state.source);
        state.source = null;
        state.target = null;
    }
    private static void clearVisual(ItemStack stack) {
        if (stack != null && stack.has(DataComponents.CUSTOM_DATA))
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(VISUAL_KEY));
    }
}
