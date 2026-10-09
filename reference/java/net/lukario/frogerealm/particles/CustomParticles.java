package net.lukario.frogerealm.particles;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class CustomParticles {
    public static void particleCircle(ServerLevel sl, Vec3 position, float range) {
        int points = Math.max(32, (int) (range * 12));

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;

            double x = position.x + Math.cos(angle) * range;
            double z = position.z + Math.sin(angle) * range;

            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    x,
                    position.y + 0.1,
                    z,
                    1,
                    0, 0, 0,
                    0
            );
        }
    }
}
