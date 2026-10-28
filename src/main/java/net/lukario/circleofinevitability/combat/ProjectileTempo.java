package net.lukario.circleofinevitability.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Temporary projectile travel scaling. Collision traces use the slowed motion, with no cumulative slowdown. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class ProjectileTempo {
    private record Motion(Projectile shot, Vec3 original, Vec3 scaled) {}
    private static final List<ToDoubleFunction<Projectile>> RULES = new ArrayList<>();
    private static final Map<ServerLevel,List<Motion>> MOTION = new IdentityHashMap<>();
    private ProjectileTempo() {}

    /** Return a travel multiplier in (0,1]; overlapping rules use the slowest value. */
    public static void addRule(ToDoubleFunction<Projectile> rule) { RULES.add(rule); }

    @SubscribeEvent
    public static void before(TickEvent.LevelTickEvent.Pre event) {
        if (!(event.level() instanceof ServerLevel level) || RULES.isEmpty()) return;
        List<Motion> changed = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Projectile shot) || shot.onGround()) continue;
            double factor = 1;
            for (var rule : RULES) factor = Math.min(factor, Math.clamp(rule.applyAsDouble(shot), .05, 1));
            if (factor >= 1) continue;
            Vec3 original = shot.getDeltaMovement(), scaled = original.scale(factor);
            changed.add(new Motion(shot, original, scaled));
            shot.setDeltaMovement(scaled);
            shot.hurtMarked = true;
        }
        MOTION.put(level, changed);
    }

    @SubscribeEvent
    public static void after(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level)) return;
        List<Motion> changed = MOTION.remove(level);
        if (changed == null) return;
        for (Motion motion : changed) {
            Projectile shot = motion.shot;
            if (shot.isRemoved() || shot.onGround() || shot.getDeltaMovement().lengthSqr() < 1e-8) continue;
            // Retain acceleration/drag calculated by the projectile during its own tick.
            shot.setDeltaMovement(motion.original.add(shot.getDeltaMovement().subtract(motion.scaled)));
            shot.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) { MOTION.remove(event.getLevel()); }
}
