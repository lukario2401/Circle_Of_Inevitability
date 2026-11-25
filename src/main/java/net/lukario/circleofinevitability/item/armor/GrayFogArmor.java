package net.lukario.circleofinevitability.item.armor;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Vestments of the Gray Fog (Fool pathway). Nothing here attacks: each piece is one step up the pathway.
 * <ul>
 *   <li>Seer's Top Hat, Spirit Vision: mobs hunting the wearer glow, as does anything invisible nearby; a chime
 *       (heard only by the wearer) warns when a new one locks on.</li>
 *   <li>Magician's Tailcoat, Paper Substitute: a hit that would kill or take 40% of max health is cancelled, a
 *       paper figurine burns in the wearer's place and they blink a few blocks away. Long recharge.</li>
 *   <li>Clown's Trousers, Acrobatics: a chance to dodge melee and projectile hits outright.</li>
 *   <li>Marionettist's Shoes, Strings from Above: no fall damage; holding sneak in midair drifts slowly down.</li>
 *   <li>Full set, Above the Gray Fog: crouch still for a second to vanish (status/Concealment) for up to 10 s.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GrayFogArmor {
    public static final String SET = "gray_fog";
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(42,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
            18, SoundEvents.ARMOR_EQUIP_LEATHER, 3f, .1f,
            TagKey.create(Registries.ITEM, InevitabilityArmor.id("repairs_gray_fog_armor")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, InevitabilityArmor.id(SET)));

    /** Game time at which the figurine is back. Kept on the coat (drawn by the client) and on the wearer. */
    public static final String PAPER_READY_KEY = "coi_paper_ready_at";
    public static final double SPIRIT_VISION_RANGE = 24, INVISIBLE_SIGHT_RANGE = 16;
    public static final float SUBSTITUTE_HEALTH_SHARE = .4f;
    public static final int PAPER_RECHARGE = 1200, PAPER_RECHARGE_FULL_SET = 800;
    public static final double BLINK_DISTANCE = 5;
    public static final float DODGE_CHANCE = .2f, DODGE_CHANCE_FULL_SET = .3f;
    public static final int VEIL_CROUCH_TICKS = 20, VEIL_TICKS = 200, VEIL_COOLDOWN = 300;

    private static final class State {
        Set<Integer> hunters = new HashSet<>();
        int crouchTicks;
        double lastX, lastZ;
        boolean veiling, drifting;
        long veilReadyAt;
    }
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    private GrayFogArmor() {}

    public static boolean wearing(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot).getItem() instanceof TentacleArmorItem item
                && item.setId().equals(SET) && item.slot() == slot;
    }
    public static boolean fullSet(LivingEntity entity) {
        for (EquipmentSlot slot : InevitabilityArmor.SLOTS) if (!wearing(entity, slot)) return false;
        return true;
    }
    private static boolean wearingAny(LivingEntity entity) {
        for (EquipmentSlot slot : InevitabilityArmor.SLOTS) if (wearing(entity, slot)) return true;
        return false;
    }

    // =========================
    // Tick: Spirit Vision, Strings from Above, Above the Gray Fog
    // =========================

    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity wearer = event.getEntity();
        if (!(wearer.level() instanceof ServerLevel level)) return;
        State state = STATES.get(wearer);
        if (state == null) {
            if (!wearingAny(wearer)) return;
            state = new State();
            STATES.put(wearer, state);
        }
        if (!wearer.isAlive() || wearer.isSpectator()) {
            endVeil(wearer, state, level.getGameTime());
            return;
        }
        long now = level.getGameTime();
        if (wearing(wearer, EquipmentSlot.HEAD)) {
            if ((wearer.tickCount + wearer.getId()) % 10 == 0) spiritVision(wearer, level, state);
        } else state.hunters.clear();
        if (wearing(wearer, EquipmentSlot.FEET)) drift(wearer, state); else state.drifting = false;
        veil(wearer, state, now);
    }

    /** Hunters and invisible things nearby glow; a new hunter rings a chime only the wearer hears. */
    static void spiritVision(LivingEntity wearer, ServerLevel level, State state) {
        Set<Integer> seen = new HashSet<>();
        boolean newcomer = false;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, wearer.getBoundingBox().inflate(SPIRIT_VISION_RANGE),
                mob -> mob.isAlive() && mob.getTarget() == wearer)) {
            glow(mob);
            seen.add(mob.getId());
            newcomer |= !state.hunters.contains(mob.getId());
        }
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, wearer.getBoundingBox().inflate(INVISIBLE_SIGHT_RANGE),
                other -> other != wearer && other.isAlive() && other.isInvisible() && !Concealment.isHidden(other)))
            glow(other);
        if (newcomer) notify(wearer, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, .6f);
        state.hunters = seen;
    }
    private static void glow(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, true, false));
    }

    /** Holding sneak while falling drifts down like a puppet held up by strings. */
    static void drift(LivingEntity wearer, State state) {
        boolean falling = !wearer.onGround() && !wearer.isInWater() && !wearer.isFallFlying()
                && wearer.isShiftKeyDown() && (state.drifting || wearer.fallDistance > 1);
        state.drifting = falling;
        if (falling) wearer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 6, 0, false, false));
    }

    /** Full set: crouch still for a second to step into the gray fog; standing up steps back out. */
    static void veil(LivingEntity wearer, State state, long now) {
        boolean crouching = fullSet(wearer) && wearer.isShiftKeyDown();
        if (!crouching) {
            state.crouchTicks = 0;
            endVeil(wearer, state, now);
        } else if (state.veiling) {
            // Revealed by an attack or the time running out.
            if (!Concealment.isHidden(wearer)) { state.veiling = false; state.veilReadyAt = now + VEIL_COOLDOWN; }
        } else {
            boolean still = wearer.onGround() && Math.abs(wearer.getX() - state.lastX) < .02 && Math.abs(wearer.getZ() - state.lastZ) < .02;
            state.crouchTicks = still ? state.crouchTicks + 1 : 0;
            if (state.crouchTicks >= VEIL_CROUCH_TICKS && now >= state.veilReadyAt && !Concealment.isHidden(wearer)) {
                Concealment.hide(wearer, VEIL_TICKS);
                state.veiling = Concealment.isHidden(wearer);     // a Decree can forbid it
                if (!state.veiling) state.veilReadyAt = now + VEIL_COOLDOWN;
            }
        }
        state.lastX = wearer.getX();
        state.lastZ = wearer.getZ();
    }
    private static void endVeil(LivingEntity wearer, State state, long now) {
        if (!state.veiling) return;
        state.veiling = false;
        state.veilReadyAt = now + VEIL_COOLDOWN;
        Concealment.reveal(wearer);
    }
    public static boolean veiled(LivingEntity wearer) {
        State state = STATES.get(wearer);
        return state != null && state.veiling;
    }

    // =========================
    // Strings from Above: no fall damage
    // =========================

    @SubscribeEvent public static void fall(LivingFallEvent event) {
        if (!event.getEntity().level().isClientSide() && wearing(event.getEntity(), EquipmentSlot.FEET))
            event.setDamageMultiplier(0);
    }

    // =========================
    // Acrobatics, then Paper Substitute (after Concealment and armed ability substitutes have had their say)
    // =========================

    @SubscribeEvent public static boolean dodge(LivingAttackEvent event) {
        LivingEntity wearer = event.getEntity();
        if (!(wearer.level() instanceof ServerLevel level) || !wearing(wearer, EquipmentSlot.LEGS)) return false;
        DamageSource source = event.getSource();
        if (!dodgeable(wearer, source, event.getAmount())) return false;
        float chance = fullSet(wearer) ? DODGE_CHANCE_FULL_SET : DODGE_CHANCE;
        if (wearer.getRandom().nextFloat() >= chance) return false;
        wearer.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 1, false, false));
        level.sendParticles(ParticleTypes.POOF, wearer.getX(), wearer.getY() + .4, wearer.getZ(), 8, .3, .2, .3, .02);
        level.playSound(null, wearer.blockPosition(), SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, .6f, 1.6f);
        return true;
    }
    /** Melee swings and projectiles only: not fire, falls, explosions or magic from nowhere. */
    public static boolean dodgeable(LivingEntity wearer, DamageSource source, float amount) {
        if (Spells.isPiercing(wearer) || !Substitute.isRealHit(wearer, source, amount)) return false;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        return source.is(DamageTypeTags.IS_PROJECTILE)
                || source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity();
    }

    @SubscribeEvent(priority = Priority.LOWEST) public static boolean substitute(LivingAttackEvent event) {
        LivingEntity wearer = event.getEntity();
        if (!(wearer.level() instanceof ServerLevel level) || !wearing(wearer, EquipmentSlot.CHEST)) return false;
        long now = level.getGameTime();
        if (now < paperReadyAt(wearer)) return false;
        float amount = event.getAmount();
        if (!Substitute.isRealHit(wearer, event.getSource(), amount) || Spells.isPiercing(wearer)) return false;
        if (amount < wearer.getHealth() + wearer.getAbsorptionAmount() && amount < wearer.getMaxHealth() * SUBSTITUTE_HEALTH_SHARE)
            return false;
        if (Decree.violate(wearer, Decree.Rule.STAND_INS)) return false;      // stand-ins are prohibited here
        burnFigurine(wearer, event.getSource(), level, now);
        return true;
    }

    /** The figurine takes the hit: it burns where the wearer stood, and the wearer blinks away from the danger. */
    static void burnFigurine(LivingEntity wearer, DamageSource source, ServerLevel level, long now) {
        long ready = now + (fullSet(wearer) ? PAPER_RECHARGE_FULL_SET : PAPER_RECHARGE);
        wearer.getPersistentData().putLong(PAPER_READY_KEY, ready);
        ItemStack coat = wearer.getItemBySlot(EquipmentSlot.CHEST);
        CustomData.update(DataComponents.CUSTOM_DATA, coat, tag -> tag.putLong(PAPER_READY_KEY, ready));
        coat.hurtAndBreak(3, wearer, EquipmentSlot.CHEST);
        // A short grace so the rest of a burst (a volley, a multi-hit) does not land the instant after.
        wearer.invulnerableTime = 20;

        Vec3 from = wearer.position();
        level.sendParticles(ParticleTypes.FLAME, from.x, from.y + 1, from.z, 24, .25, .5, .25, .02);
        level.sendParticles(ParticleTypes.WHITE_ASH, from.x, from.y + 1, from.z, 40, .35, .6, .35, .05);
        level.playSound(null, wearer.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, .8f, 1.4f);

        Vec3 away;
        var danger = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (danger != null && danger != wearer) away = from.subtract(danger.position()).multiply(1, 0, 1);
        else {
            double angle = wearer.getRandom().nextDouble() * Math.PI * 2;
            away = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }
        if (away.lengthSqr() < 1e-4) away = wearer.getLookAngle().multiply(-1, 0, -1);
        if (away.lengthSqr() < 1e-4) away = new Vec3(1, 0, 0);
        away = away.normalize().scale(BLINK_DISTANCE);
        Vec3 spot = Teleport.standNear(wearer, level, from.add(away), 3);
        if (spot == null) spot = Teleport.standNear(wearer, level, from.add(away.yRot((float)Math.PI / 2)), 3);
        if (spot != null && Teleport.of(wearer).fallGuard(40).go(level, spot))
            level.playSound(null, wearer.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, .8f, 1.2f);
        if (wearer instanceof ServerPlayer player)
            player.sendOverlayMessage(Component.translatable("message.circle_of_inevitability." + SET + ".substituted"));
    }

    /** When the figurine is back; 0 when it is ready. Honors both the coat and the wearer so swapping coats cannot skip it. */
    public static long paperReadyAt(LivingEntity wearer) {
        long onCoat = wearer.getItemBySlot(EquipmentSlot.CHEST).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getLongOr(PAPER_READY_KEY, 0L);
        return Math.max(onCoat, wearer.getPersistentData().getLongOr(PAPER_READY_KEY, 0L));
    }

    private static void notify(LivingEntity wearer, SoundEvent sound, float volume, float pitch) {
        if (!(wearer instanceof ServerPlayer player)) return;
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
    }
}
