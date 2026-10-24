package net.lukario.circleofinevitability.root;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Roots: take away some or all of what an entity (mob or player) can do, for a while.
 * Works from any class/ability:
 *
 *   Root.apply(target, 100, RootRestriction.MOVE);                    // 5 seconds, can't walk
 *   Root.apply(target, 60, RootRestriction.MOVE, RootRestriction.ATTACK);
 *   Root.apply(target, 200, RootRestriction.EVERYTHING);              // presets: MOVEMENT, DISARM, SILENCE, EVERYTHING
 *
 *   Root.has(target, RootRestriction.MOVE)      // is it currently unable to move?
 *   Root.isRooted(target)                       // any restriction at all?
 *   Root.remove(target)                         // free it
 *   Root.remove(target, RootRestriction.ATTACK) // give back just one thing
 *
 * Something else can hold an entity as a root would, without a root's timer or its ring (a Decree's Rule against
 * teleporting): Root.addHold((entity, restriction) -> ...) once, in a static block. Root.has then says true for it,
 * so everything that asks Root.has refuses as for a root; isRooted, remove and ticksLeft only know real roots.
 *
 * Every restriction has its own timer, so roots stack nicely: applying a shorter root never
 * shortens a longer one that's already there. Saved with the entity, cleared on death.
 *
 *   Root.addImmunity((entity, restriction) -> ...)   // once, in a static block: while it says true, that
 *                                                    // restriction never takes on it (and one it had ends)
 *
 * A body too dense to hold (Star Dragon's Neutron Star and Weight and Density: MOVE, JUMP and TELEPORT), a mind that
 * no word binds (ABILITIES). Freeze has its own Freeze.addImmunity; a frozen player is rooted through here too.
 *
 *   Root.addDurationScale((entity, restriction) -> ...)   // once, in a static block: a root put on that entity lasts
 *                                                         // this many times as long (0.75 = a quarter shorter)
 *
 * Feet that no hold keeps for long (Key Of Stars' Footprints of Freedom, Astral Footing). Every rule that applies is
 * multiplied in; a root never lasts less than one tick.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Root {

    /** Faint glow ring at the feet of rooted entities. Set to false to handle visuals yourself. */
    public static boolean SHOW_PARTICLES = true;

    private static final String TAG = "circle_of_inevitability_root"; // restriction name -> ticks left
    private static final Identifier MOVE_MODIFIER = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "root_move");
    private static final Identifier JUMP_MODIFIER = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "root_jump");

    private static final ParticleFx ROOT_MARK = ParticleFx.of("fx/glow")
            .color(0xFFFFE08A).fadeOut()
            .size(0.12f).endSize(0.04f)
            .lifetime(12).friction(1f).gravity(-0.02f)
            .glow();

    private static final List<BiPredicate<Entity, RootRestriction>> HOLDS = new ArrayList<>();

    private Root() {}

    /** One more thing that holds entities as a root would (see the class comment). */
    public static void addHold(BiPredicate<Entity, RootRestriction> hold) {
        HOLDS.add(hold);
    }

    // =========================
    // API
    // =========================

    public static void apply(LivingEntity target, int ticks, RootRestriction... restrictions) {
        apply(target, ticks, restrictions.length == 0
                ? EnumSet.noneOf(RootRestriction.class)
                : EnumSet.copyOf(Arrays.asList(restrictions)));
    }

    public static void apply(LivingEntity target, int ticks, Set<RootRestriction> restrictions) {
        if (target.level().isClientSide() || ticks <= 0 || restrictions.isEmpty()) return;
        Set<RootRestriction> taking = EnumSet.noneOf(RootRestriction.class);
        for (RootRestriction restriction : restrictions) if (!isImmune(target, restriction)) taking.add(restriction);
        if (taking.isEmpty()) return;

        CompoundTag tag = rootTag(target, true);
        for (RootRestriction restriction : taking) {
            String key = restriction.name();
            tag.putInt(key, Math.max(tag.getIntOr(key, 0), scaledTicks(target, restriction, ticks)));
        }
        updateModifiers(target);
        if (taking.contains(RootRestriction.MOVE)) holdInPlace(target);
    }

    /** How much longer (or shorter) a root lasts on an entity: 1 = as given. */
    @FunctionalInterface
    public interface DurationScale {
        float scale(LivingEntity target, RootRestriction restriction);
    }

    private static final List<DurationScale> DURATION_SCALES = new ArrayList<>();

    /** Once, in a static block: roots put on some entities last longer or shorter (see the class comment). */
    public static void addDurationScale(DurationScale rule) {
        DURATION_SCALES.add(rule);
    }

    /** How long a root of 'ticks' really lasts on this entity, after every addDurationScale rule. */
    public static int scaledTicks(LivingEntity target, RootRestriction restriction, int ticks) {
        if (DURATION_SCALES.isEmpty()) return ticks;
        float scale = 1f;
        for (DurationScale rule : DURATION_SCALES) scale *= Math.max(0f, rule.scale(target, restriction));
        return Math.max(1, Math.round(ticks * scale));
    }

    private static final List<BiPredicate<LivingEntity, RootRestriction>> IMMUNE = new ArrayList<>();

    /** Once, in a static block: while it says true for an entity and a restriction, that restriction never takes on it. */
    public static void addImmunity(BiPredicate<LivingEntity, RootRestriction> immune) {
        IMMUNE.add(immune);
    }

    public static boolean isImmune(LivingEntity entity, RootRestriction restriction) {
        for (BiPredicate<LivingEntity, RootRestriction> rule : IMMUNE) if (rule.test(entity, restriction)) return true;
        return false;
    }

    public static void remove(LivingEntity target) {
        target.getPersistentData().remove(TAG);
        updateModifiers(target);
    }

    public static void remove(LivingEntity target, RootRestriction... restrictions) {
        CompoundTag tag = rootTag(target, false);
        if (tag == null) return;
        for (RootRestriction restriction : restrictions) tag.remove(restriction.name());
        if (tag.isEmpty()) target.getPersistentData().remove(TAG);
        updateModifiers(target);
    }

    public static boolean has(Entity entity, RootRestriction restriction) {
        if (entity == null) return false;
        CompoundTag tag = rootTag(entity, false);
        if (tag != null && tag.getIntOr(restriction.name(), 0) > 0) return true;
        for (BiPredicate<Entity, RootRestriction> hold : HOLDS) {
            if (hold.test(entity, restriction)) return true;
        }
        return false;
    }

    public static boolean isRooted(Entity entity) {
        if (entity == null) return false;
        CompoundTag tag = rootTag(entity, false);
        return tag != null && !tag.isEmpty();
    }

    /** Ticks left for one restriction (0 = not restricted). */
    public static int ticksLeft(Entity entity, RootRestriction restriction) {
        CompoundTag tag = rootTag(entity, false);
        return tag == null ? 0 : tag.getIntOr(restriction.name(), 0);
    }

    // =========================
    // Internals
    // =========================

    private static boolean immuneTo(LivingEntity entity, String key) {
        if (IMMUNE.isEmpty()) return false;
        try {
            return isImmune(entity, RootRestriction.valueOf(key));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static CompoundTag rootTag(Entity entity, boolean create) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) {
            if (!create) return null;
            data.put(TAG, new CompoundTag());
        }
        return data.getCompoundOrEmpty(TAG);
    }

    /** Speed/jump are done with attributes, which the player's own game respects too. */
    private static void updateModifiers(LivingEntity entity) {
        setModifier(entity, Attributes.MOVEMENT_SPEED, MOVE_MODIFIER, has(entity, RootRestriction.MOVE));
        setModifier(entity, Attributes.JUMP_STRENGTH, JUMP_MODIFIER, has(entity, RootRestriction.JUMP));
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id, boolean active) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        boolean present = instance.hasModifier(id);
        if (active && !present) {
            // -100% of the final value = 0
            instance.addTransientModifier(new AttributeModifier(id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!active && present) {
            instance.removeModifier(id);
        }
    }

    private static void holdInPlace(LivingEntity entity) {
        Vec3 motion = entity.getDeltaMovement();
        if (entity instanceof Player player) {
            if (player.isFallFlying()) player.stopFallFlying();
            if (player.getAbilities().flying && !player.isSpectator()) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            // only correct the player when something is actually pushing them (water, knockback...)
            if (motion.x * motion.x + motion.z * motion.z > 0.0001) {
                player.setDeltaMovement(0, Math.min(motion.y, 0), 0);
                player.hurtMarked = true;
            }
        } else {
            // flying mobs (no gravity) are frozen in the air, others can still fall
            entity.setDeltaMovement(0, entity.isNoGravity() ? 0 : Math.min(motion.y, 0), 0);
            if (entity instanceof Mob mob) mob.getNavigation().stop();
        }
    }

    // =========================
    // Timers + movement
    // =========================

    @SubscribeEvent
    public static void onRootLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        CompoundTag tag = rootTag(entity, false);
        if (tag == null) return;

        for (String key : Set.copyOf(tag.keySet())) {
            int left = tag.getIntOr(key, 0) - 1;
            if (left <= 0 || immuneTo(entity, key)) tag.remove(key);       // ran out, or it has become immune
            else tag.putInt(key, left);
        }
        if (tag.isEmpty()) entity.getPersistentData().remove(TAG);

        updateModifiers(entity); // also re-adds them after a relog
        if (has(entity, RootRestriction.MOVE)) holdInPlace(entity);

        if (SHOW_PARTICLES && isRooted(entity) && entity.tickCount % 10 == 0
                && entity.level() instanceof ServerLevel sl) {
            ParticleShapes.ring(sl, ROOT_MARK, entity.position().add(0, 0.1, 0),
                    Math.max(0.5, entity.getBbWidth() * 0.8), 10, 0.0);
        }
    }

    // =========================
    // Blocking actions
    // =========================

    private static boolean block(Entity entity, RootRestriction restriction) {
        if (!has(entity, restriction)) return false;
        if (entity instanceof Player player && !player.level().isClientSide()) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
        }
        return true;
    }

    // ATTACK: player melee
    @SubscribeEvent
    public static boolean onRootPlayerAttack(AttackEntityEvent event) {
        return block(event.getEntity(), RootRestriction.ATTACK);
    }

    // ATTACK: any damage whose attacker/owner is rooted (mob melee, arrows, abilities...)
    @SubscribeEvent
    public static boolean onRootLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        return attacker != null && has(attacker, RootRestriction.ATTACK);
    }

    // USE_ITEMS
    @SubscribeEvent
    public static boolean onRootRightClickItem(PlayerInteractEvent.RightClickItem event) {
        return block(event.getEntity(), RootRestriction.USE_ITEMS);
    }

    @SubscribeEvent
    public static boolean onRootUseItemStart(LivingEntityUseItemEvent.Start event) {
        return block(event.getEntity(), RootRestriction.USE_ITEMS);
    }

    // BLOCKS
    @SubscribeEvent
    public static boolean onRootRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        return block(event.getEntity(), RootRestriction.BLOCKS);
    }

    @SubscribeEvent
    public static boolean onRootLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        return block(event.getEntity(), RootRestriction.BLOCKS);
    }

    @SubscribeEvent
    public static boolean onRootBreakBlock(BlockEvent.BreakEvent event) {
        return block(event.getPlayer(), RootRestriction.BLOCKS);
    }

    @SubscribeEvent
    public static boolean onRootPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        return block(event.getEntity(), RootRestriction.BLOCKS);
    }

    // Forge 65: multi-block placements (beds, doors...) no longer reach the EntityPlaceEvent listener
    // (EntityPlaceEvent is not an InheritableEvent), so they need their own to stay blocked as before.
    @SubscribeEvent
    public static boolean onRootMultiPlaceBlock(BlockEvent.EntityMultiPlaceEvent event) {
        return block(event.getEntity(), RootRestriction.BLOCKS);
    }

    // TELEPORT (commands still work)
    @SubscribeEvent
    public static boolean onRootTeleport(EntityTeleportEvent event) {
        if (event instanceof EntityTeleportEvent.TeleportCommand
                || event instanceof EntityTeleportEvent.SpreadPlayersCommand) return false;
        return block(event.getEntity(), RootRestriction.TELEPORT);
    }

    // ABILITIES are checked in the SKeyPressAbility...Used packets, AbilityMenu and AbilityTextPrompt
}
