package net.lukario.circleofinevitability.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Contagion: something one enemy carries for a while and then passes on to the enemies nearest to it, which carry
 * it and pass it on in their turn, a little stronger each time. A plague, a curse that spreads, a fire that leaps
 * from body to body:
 *
 *   Contagion.spread()
 *           .lasts(100)                              // ticks one carries it before it passes on (default 100)
 *           .every(20, (victim, stage) -> ...)       // while carried: damage over time, a puff of smoke
 *           .passesTo(2, 3)                          // each time, to 2 or 3 new ones (nearest first; default 1)
 *           .reach(8)                                // how far it can leap (default 6)
 *           .generations(4)                          // the first one is stage 1; stage 4 passes it on no more (default 3)
 *           .maxVictims(24)                          // in all, for this one outbreak (default 12)
 *           .travel(10)                              // ticks a leap takes (default 10)
 *           .onCatch((victim, stage) -> ...)         // someone caught it: its mark
 *           .onRunOut((victim, stage) -> ...)        // someone's time with it is up, right before it passes on
 *           .onPass((from, to, stage) -> ...)        // it leaps: 'stage' is what 'to' will carry
 *           .start(player, sl, firstVictim);
 *
 * Every part after spread() is optional. Use 'stage' to make it stronger with every leap:
 *   float damage = 4f * (float) Math.pow(1.5, stage - 1);
 *
 * WHO CATCHES IT. The owner's enemies (Spells.isEnemy). Nobody catches the same outbreak twice, so it always
 * moves outward; two outbreaks are two separate things and can be on the same enemy at once.
 * WHEN IT PASSES ON. When one's time with it is up, and also when the one carrying it dies first (from where
 * the body fell) unless .notOnDeath(). It stops when its owner dies, logs out or leaves the dimension.
 *
 * Build a new one for every cast (the outbreak keeps its own list of who has had it), so don't store one in a
 * constant. Not saved: if the server stops, it is gone.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Contagion {

    /** Something that happens to one who carries it. 'stage' = 1 for the first one, +1 for every leap. */
    @FunctionalInterface
    public interface Carrier {
        void run(LivingEntity victim, int stage);
    }

    /** It leaps from one to another. 'stage' is what the new one will carry. */
    @FunctionalInterface
    public interface Leap {
        void run(LivingEntity from, LivingEntity to, int stage);
    }

    private int lasts = 100;
    private int every = 20;
    private Carrier onTick;
    private int passesMin = 1;
    private int passesMax = 1;
    private double reach = 6.0;
    private int generations = 3;
    private int maxVictims = 12;
    private int travel = 10;
    private boolean onDeath = true;
    private Carrier onCatch;
    private Carrier onRunOut;
    private Leap onPass;

    /** One outbreak: who it belongs to and who has had it. */
    private static final class Outbreak {
        final LivingEntity owner;
        final ServerLevel sl;
        final Set<UUID> caught = new HashSet<>();

        Outbreak(LivingEntity owner, ServerLevel sl) {
            this.owner = owner;
            this.sl = sl;
        }
    }

    private Contagion() {}

    /** A new contagion. Nothing happens until .start(...). */
    public static Contagion spread() {
        return new Contagion();
    }

    /** How long one carries it before it passes on, in ticks. */
    public Contagion lasts(int ticks) {
        this.lasts = Math.max(1, ticks);
        return this;
    }

    /** What it does to whoever carries it, every 'ticks' ticks (the first time 'ticks' after they caught it). */
    public Contagion every(int ticks, Carrier action) {
        this.every = Math.max(1, ticks);
        this.onTick = action;
        return this;
    }

    /** It passes on to this many new ones each time (nearest first). */
    public Contagion passesTo(int count) {
        return passesTo(count, count);
    }

    /** It passes on to between 'min' and 'max' new ones each time, at random (nearest first). */
    public Contagion passesTo(int min, int max) {
        this.passesMin = Math.max(0, min);
        this.passesMax = Math.max(this.passesMin, max);
        return this;
    }

    /** How far it can leap, in blocks (to the nearest part of the next one's body). */
    public Contagion reach(double blocks) {
        this.reach = Math.max(0.5, blocks);
        return this;
    }

    /** The highest stage there is: the one who carries this stage does not pass it on. */
    public Contagion generations(int stages) {
        this.generations = Math.max(1, stages);
        return this;
    }

    /** How many can catch this one outbreak in all, the first one included. */
    public Contagion maxVictims(int count) {
        this.maxVictims = Math.max(1, count);
        return this;
    }

    /** How long a leap takes, in ticks (the new one catches it then). */
    public Contagion travel(int ticks) {
        this.travel = Math.max(0, ticks);
        return this;
    }

    /** One who dies while carrying it takes it with them, instead of passing it on from where they fell. */
    public Contagion notOnDeath() {
        this.onDeath = false;
        return this;
    }

    /** Runs when someone catches it (the first one too). */
    public Contagion onCatch(Carrier action) {
        this.onCatch = action;
        return this;
    }

    /** Runs when someone's time with it is up, just before it passes on (not when they died first). */
    public Contagion onRunOut(Carrier action) {
        this.onRunOut = action;
        return this;
    }

    /** Runs as it leaps from one to the next. */
    public Contagion onPass(Leap action) {
        this.onPass = action;
        return this;
    }

    /** Starts the outbreak on 'first', which carries stage 1. */
    public void start(LivingEntity owner, ServerLevel sl, LivingEntity first) {
        if (first == null || !first.isAlive() || first.level() != sl) return;
        Outbreak outbreak = new Outbreak(owner, sl);
        outbreak.caught.add(first.getUUID());
        infect(outbreak, first, 1);
    }

    // =========================
    // Inside
    // =========================

    private void infect(Outbreak outbreak, LivingEntity victim, int stage) {
        if (onCatch != null) onCatch.run(victim, stage);
        carry(outbreak, victim, stage, 0);
    }

    private void carry(Outbreak outbreak, LivingEntity victim, int stage, int carried) {
        ServerLevel sl = outbreak.sl;
        if (!Spells.casterStillHere(outbreak.owner, sl)) return;
        if (victim.level() != sl) return;                                    // gone to another dimension
        if (!victim.isAlive()) {
            if (onDeath && victim.isDeadOrDying()) passOn(outbreak, victim, stage);
            return;
        }
        if (carried > 0 && onTick != null) onTick.run(victim, stage);
        if (!victim.isAlive()) {                                            // that was the end of it
            if (onDeath) passOn(outbreak, victim, stage);
            return;
        }
        if (carried >= lasts) {
            if (onRunOut != null) onRunOut.run(victim, stage);
            passOn(outbreak, victim, stage);
            return;
        }
        int step = Math.min(every, lasts - carried);
        Later.run(sl, step, () -> carry(outbreak, victim, stage, carried + step));
    }

    private void passOn(Outbreak outbreak, LivingEntity from, int stage) {
        if (stage >= generations) return;
        int room = maxVictims - outbreak.caught.size();
        if (room <= 0) return;
        int count = passesMin + (passesMax > passesMin ? outbreak.sl.getRandom().nextInt(passesMax - passesMin + 1) : 0);
        count = Math.min(count, room);
        if (count <= 0) return;

        Vec3 center = from.getBoundingBox().getCenter();
        List<LivingEntity> near = new ArrayList<>();
        for (LivingEntity other : Spells.enemiesAround(outbreak.owner, outbreak.sl, center, reach)) {
            if (other != from && !outbreak.caught.contains(other.getUUID())) near.add(other);
        }
        near.sort((a, b) -> Double.compare(a.distanceToSqr(center), b.distanceToSqr(center)));

        int next = stage + 1;
        for (int i = 0; i < Math.min(count, near.size()); i++) {
            LivingEntity to = near.get(i);
            outbreak.caught.add(to.getUUID());
            if (onPass != null) onPass.run(from, to, next);
            Later.run(outbreak.sl, travel, () -> {
                if (to.isAlive() && to.level() == outbreak.sl && Spells.casterStillHere(outbreak.owner, outbreak.sl)) {
                    infect(outbreak, to, next);
                }
            });
        }
    }
}
