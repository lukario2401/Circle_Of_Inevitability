package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.item.artifacts.uniqueness.ShadowSoldier;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Shadow soldiers rise from the ground around the wielder and hunt them, at every corruption level: every
 * everySeconds[level] seconds a wave of perWave[level], never more than maxAlive[level] at once. Their ranks are
 * drawn from ranks[level] (0 Levy, 1 Legionary, 2 Centurion, 3 Warlord): higher levels, stronger soldiers.
 */
public final class ShadowSoldiersDrawback implements UniquenessDrawback {
    private static final String CLOCK = "soldiers_clock";

    private final int[] everySeconds, perWave, maxAlive;
    private final int[][] ranks;
    private final int lifetimeTicks;

    public ShadowSoldiersDrawback(int[] everySeconds, int[] perWave, int[] maxAlive, int[][] ranks, int lifetimeTicks) {
        this.everySeconds = everySeconds;
        this.perWave = perWave;
        this.maxAlive = maxAlive;
        this.ranks = ranks;
        this.lifetimeTicks = lifetimeTicks;
    }

    @Override
    public Component describe() {
        return Component.literal("Shadow soldiers rise from the ground and hunt you; more and stronger as corruption grows. "
                + "Their blows ignore its protection.");
    }

    @Override
    public void second(Wielder w) {
        if (!UniquenessItem.active(this, w)) {
            w.state().putInt(CLOCK, 0);
            return;
        }
        int clock = w.state().getIntOr(CLOCK, 0) + 1;
        if (clock < w.at(everySeconds)) {
            w.state().putInt(CLOCK, clock);
            return;
        }
        w.state().putInt(CLOCK, 0);
        wave(w);
    }

    /** Raises a wave now (as many as the level allows). Returns how many rose. */
    public int wave(Wielder w) {
        var player = w.player();
        int alive = alive(w);
        int count = Math.min(w.at(perWave), w.at(maxAlive) - alive);
        int[] pool = ranks[Math.clamp(w.corruptionLevel(), 0, ranks.length - 1)];
        int risen = 0;
        for (int i = 0; i < count; i++) {
            Vec3 at = ShadowSoldier.spotNear(w.level(), player.position(), 3, 7, player.getRandom());
            if (at == null) continue;
            ShadowSoldier.rise(w.level(), at, pool[player.getRandom().nextInt(pool.length)], ShadowSoldier.SHADOW, player,
                    false, false, lifetimeTicks);
            risen++;
        }
        if (risen > 0)
            player.sendOverlayMessage(Component.literal(risen == 1 ? "A shadow soldier claws its way out of the ground."
                    : risen + " shadow soldiers claw their way out of the ground.").withStyle(ChatFormatting.DARK_RED));
        return risen;
    }

    /** The real (not illusory, not loyal) soldiers hunting this wielder now. */
    public static int alive(Wielder w) {
        return w.level().getEntitiesOfClass(ShadowSoldier.class, w.player().getBoundingBox().inflate(64),
                s -> s.isAlive() && !s.loyal() && !s.illusion() && w.player().getUUID().equals(s.owner())).size();
    }
}
