package net.lukario.frogerealm.root;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

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
 * Every restriction has its own timer, so roots stack nicely: applying a shorter root never
 * shortens a longer one that's already there. Saved with the entity, cleared on death.
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Root {

    /** Faint glow ring at the feet of rooted entities. Set to false to handle visuals yourself. */
    public static boolean SHOW_PARTICLES = true;

    private static final String TAG = "forgerealm_root"; // restriction name -> ticks left
    private static final ResourceLocation MOVE_MODIFIER = ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "root_move");
    private static final ResourceLocation JUMP_MODIFIER = ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "root_jump");

    private static final ParticleFx ROOT_MARK = ParticleFx.of("fx/glow")
            .color(0xFFFFE08A).fadeOut()
            .size(0.12f).endSize(0.04f)
            .lifetime(12).friction(1f).gravity(-0.02f)
            .glow();

    private Root() {}

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

        CompoundTag tag = rootTag(target, true);
        for (RootRestriction restriction : restrictions) {
            String key = restriction.name();
            tag.putInt(key, Math.max(tag.getInt(key), ticks));
        }
        updateModifiers(target);
        if (restrictions.contains(RootRestriction.MOVE)) holdInPlace(target);
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
        return tag != null && tag.getInt(restriction.name()) > 0;
    }

    public static boolean isRooted(Entity entity) {
        if (entity == null) return false;
        CompoundTag tag = rootTag(entity, false);
        return tag != null && !tag.isEmpty();
    }

    /** Ticks left for one restriction (0 = not restricted). */
    public static int ticksLeft(Entity entity, RootRestriction restriction) {
        CompoundTag tag = rootTag(entity, false);
        return tag == null ? 0 : tag.getInt(restriction.name());
    }

    // =========================
    // Internals
    // =========================

    private static CompoundTag rootTag(Entity entity, boolean create) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) {
            if (!create) return null;
            data.put(TAG, new CompoundTag());
        }
        return data.getCompound(TAG);
    }

    /** Speed/jump are done with attributes, which the player's own game respects too. */
    private static void updateModifiers(LivingEntity entity) {
        setModifier(entity, Attributes.MOVEMENT_SPEED, MOVE_MODIFIER, has(entity, RootRestriction.MOVE));
        setModifier(entity, Attributes.JUMP_STRENGTH, JUMP_MODIFIER, has(entity, RootRestriction.JUMP));
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, boolean active) {
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

        for (String key : Set.copyOf(tag.getAllKeys())) {
            int left = tag.getInt(key) - 1;
            if (left <= 0) tag.remove(key);
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
            player.displayClientMessage(Component.literal("You are bound by Order."), true);
        }
        return true;
    }

    // ATTACK: player melee
    @SubscribeEvent
    public static void onRootPlayerAttack(AttackEntityEvent event) {
        if (block(event.getEntity(), RootRestriction.ATTACK)) event.setCanceled(true);
    }

    // ATTACK: any damage whose attacker/owner is rooted (mob melee, arrows, abilities...)
    @SubscribeEvent
    public static void onRootLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && has(attacker, RootRestriction.ATTACK)) event.setCanceled(true);
    }

    // USE_ITEMS
    @SubscribeEvent
    public static void onRootRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (block(event.getEntity(), RootRestriction.USE_ITEMS)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRootUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (block(event.getEntity(), RootRestriction.USE_ITEMS)) event.setCanceled(true);
    }

    // BLOCKS
    @SubscribeEvent
    public static void onRootRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (block(event.getEntity(), RootRestriction.BLOCKS)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRootLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (block(event.getEntity(), RootRestriction.BLOCKS)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRootBreakBlock(BlockEvent.BreakEvent event) {
        if (block(event.getPlayer(), RootRestriction.BLOCKS)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRootPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (block(event.getEntity(), RootRestriction.BLOCKS)) event.setCanceled(true);
    }

    // TELEPORT (commands still work)
    @SubscribeEvent
    public static void onRootTeleport(EntityTeleportEvent event) {
        if (event instanceof EntityTeleportEvent.TeleportCommand
                || event instanceof EntityTeleportEvent.SpreadPlayersCommand) return;
        if (block(event.getEntity(), RootRestriction.TELEPORT)) event.setCanceled(true);
    }

    // ABILITIES are checked in the SKeyPressAbility...Used packets, AbilityMenu and AbilityTextPrompt
}
