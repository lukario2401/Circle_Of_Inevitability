package net.lukario.circleofinevitability.fate;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.status.Luck;
import net.lukario.circleofinevitability.combat.DamageFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.status.Decree;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One damage hook: melee, owned projectiles and attributed ability damage all receive the same multiplier once. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class FateCombat {
    private static final String GRACE = "snake_netherite_grace";
    private FateCombat() {}

    @SubscribeEvent(priority = Priority.HIGH)
    public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        ServerPlayer dealer = DamageFx.abilityDealerOf(event.getEntity(), event.getSource(), sl);
        WheelTier attacker = dealer != null && dealer != event.getEntity() ? Fate.tier(dealer) : WheelTier.GOLD;
        event.setAmount(WheelTier.scaleDamage(event.getAmount(), attacker, Fate.tier(event.getEntity())));
    }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public static boolean revive(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel sl)
                || !Fate.applies(player) || net.lukario.circleofinevitability.status.DeathWard.isArmed(player) || Fate.peek(player) != WheelTier.NETHERITE
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || Spells.isPiercing(player)) return false;
        if (Decree.violate(player, Decree.Rule.RESURRECTION)) return false;
        // Consume this result instead of immediately rerolling another Netherite revival.
        Fate.set(player, WheelTier.GOLD);
        player.setHealth(Math.max(1f, player.getMaxHealth() * .5f));
        player.clearFire();
        player.getPersistentData().putLong(GRACE, sl.getGameTime() + 40);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 80, .7, .8, .7, .2);
        return true;
    }

    @SubscribeEvent(priority = Priority.HIGH)
    public static boolean grace(LivingAttackEvent event) {
        return event.getEntity().level() instanceof ServerLevel sl
                && event.getEntity().getPersistentData().getLongOr(GRACE, 0L) > sl.getGameTime()
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !Spells.isPiercing(event.getEntity());
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || !Fate.applies(player) || !player.isAlive() || player.tickCount % 5 != 0) return;
        Luck.add(player, LuckBalance.perSecond(Math.clamp(SoulCore.getSequence(player), 0, 9)) / 4);
        WheelTier tier = Fate.peek(player);
        // Short, nonstacking utility effects. Strength/Resistance are deliberately absent: damage is scaled above.
        switch (tier) {
            case NETHERITE -> {
                effect(player, MobEffects.SPEED, 1); effect(player, MobEffects.HASTE, 1);
                effect(player, MobEffects.FIRE_RESISTANCE, 0);
                effect(player, MobEffects.LUCK, 1);
            }
            case DIAMOND -> {
                effect(player, MobEffects.SPEED, 0); effect(player, MobEffects.HASTE, 0);
                effect(player, MobEffects.LUCK, 0);
            }
            case EMERALD -> { effect(player, MobEffects.SPEED, 0); effect(player, MobEffects.HASTE, 0); }
            case IRON -> { effect(player, MobEffects.UNLUCK, 0); }
            case STONE -> { effect(player, MobEffects.UNLUCK, 1); effect(player, MobEffects.SLOWNESS, 0); }
            case WOOD -> { effect(player, MobEffects.UNLUCK, 2); effect(player, MobEffects.SLOWNESS, 1); }
            default -> { }
        }
    }

    private static void effect(Player player, Holder<MobEffect> effect, int amplifier) {
        // Ten tick tails avoid erasing effects supplied by potions or other abilities when the Wheel changes.
        MobEffectInstance has = player.getEffect(effect);
        if (has == null || has.getAmplifier() < amplifier || has.getDuration() <= 5)
            player.addEffect(new MobEffectInstance(effect, 10, amplifier, true, false));
    }
}
