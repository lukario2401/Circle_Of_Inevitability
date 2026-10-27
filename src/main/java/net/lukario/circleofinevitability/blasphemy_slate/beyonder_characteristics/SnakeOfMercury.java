package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.fate.Fate;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Misfortune;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.server.level.ServerLevel;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** First playable slice of the Fate pathway: previewed Wheel and both Premonition casts. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class SnakeOfMercury {
    public static final String PATHWAY = "Snake Of Mercury";
    private static final String VISION = "snake_of_mercury_vision", PREMONITION = "snake_of_mercury_premonition";
    static { Cooldowns.addFree(Cooldowns.keysStartingWith("snake_of_mercury_")); }
    private static final ParticleFx SILVER = ParticleFx.of("fx/spark").color(0xFFC9D3E0).glow().size(.08f).lifetime(12, 0);
    private SnakeOfMercury() {}

    public static void premonition(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!bypassClassCheck && !PATHWAY.equals(SoulCore.getPathway(player))) return;
        if (!Beyonder.hasSequence(player, 9)) return;
        // Like Worm Of Time: no fixed cooldown, full spirituality price must be affordable before Fortune refunds.
        if (!player.isShiftKeyDown() && net.lukario.circleofinevitability.status.Decree.violate(player, net.lukario.circleofinevitability.status.Decree.Rule.STAND_INS)) return;
        float before = SoulCore.getSpirituality(player);
        if (!Spells.paySpirituality(player, 250)) return;
        boolean own = PATHWAY.equals(SoulCore.getPathway(player)) && !Casts.isCopying(player);
        Fate.Omen omen = own ? Fate.peek(player) : Fate.Omen.EVEN;
        float power = omen.power;
        if (player.isShiftKeyDown()) {
            int ticks = Math.round(300 * power);
            player.getPersistentData().putLong(VISION, sl.getGameTime() + ticks);
            HudTimer.show(player, VISION, "Spirit Vision", ticks, 0xC9D3E0);
        } else {
            int ticks = Math.round(160 * power);
            Substitute.arm(player, ticks, Beyonder.hasSequence(player, 3) ? 3 : 1, (saved, source, amount, world) -> {
                Vec3 look = Spells.flatLook(saved);
                Vec3 side = new Vec3(-look.z, 0, look.x);
                Vec3 destination = Root.has(saved, RootRestriction.TELEPORT) ? null
                        : Spells.freeSpotNear(saved, world, saved.position(), side, 2);
                if (destination != null) {
                    saved.teleportTo(destination.x, destination.y, destination.z);
                    saved.fallDistance = 0;
                }
                if (source.getEntity() instanceof LivingEntity attacker && !Spells.isFriend(saved, attacker)) {
                    Spells.keepEffect(attacker, MobEffects.SLOWNESS, 1, 20);
                    // A narrowly avoided attack leaves the enemy off balance; this makes early misfortune usable.
                    Misfortune.add(player, attacker, 1);
                }
                HudTimer.show(player, PREMONITION, "Premonition", Substitute.ticksLeft(saved), 0xC9D3E0);
            });
            HudTimer.show(player, PREMONITION, "Premonition", ticks, 0xC9D3E0);
        }
        if (own) Fate.consume(player, omen, before - SoulCore.getSpirituality(player));
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel sl)
                || player.tickCount % 5 != 0) return;
        boolean own = PATHWAY.equals(SoulCore.getPathway(player)) && Beyonder.hasSequence(player, 9);
        boolean vision = player.getPersistentData().getLongOr(VISION, 0L) > sl.getGameTime();
        if (own) {
            Fate.Omen omen = Fate.peek(player);
            HudCounter.show(player, "snake_wheel", "snake_of_mercury/omen_" + omen.name().toLowerCase(java.util.Locale.ROOT), "Wheel: " + omen.label + " · next spin", (Fate.ticksLeft(player) + 19) / 20, 0, omen.color);
        } else HudCounter.hide(player, "snake_wheel");
        if (vision) {
            double range = Beyonder.hasSequence(player, 2) ? 64 : 32;
            // Same reveal mechanism as Eye of Demon Hunting; no teams or permanent visibility changes.
            for (LivingEntity seen : Spells.livingAround(player, sl, player.position(), range)) {
                if (seen != player) {
                    Spells.keepEffect(seen, MobEffects.GLOWING, 0, 10);
                    int color = Misfortune.level(seen) >= 6 ? 0xFFE75060 : 0xFFC9D3E0;
                    if (seen instanceof Player other && PATHWAY.equals(SoulCore.getPathway(other)) && Fate.peek(other) == Fate.Omen.FORTUNE)
                        color = 0xFFF4CE62;
                    ParticleShapes.burst(sl, SILVER.color(color), seen.position().add(0, seen.getBbHeight() + .2, 0), 2, .12, .02);
                }
            }
        }
        LivingEntity target = (own || vision) ? Spells.aimEnemy(player, sl, vision && Beyonder.hasSequence(player, 2) ? 64 : 32) : null;
        if (target == null) HudCounter.hide(player, "snake_target");
        else {
            int count = Misfortune.level(target);
            HudCounter.show(player, "snake_target", "", target.getDisplayName().getString() + " · Misfortune", count, 10,
                    count >= 6 ? 0xE75060 : 0xC9D3E0);
        }
        if (own && Substitute.isArmed(player)) {
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), 16)) {
                if (enemy instanceof Mob mob && mob.getTarget() == player && Spells.clearLine(player, sl, player.getEyePosition(), enemy.getEyePosition()))
                    ParticleShapes.burst(sl, SILVER.color(0xFFE75060), enemy.getEyePosition(), 3, .15, .02);
            }
        }
    }
}
