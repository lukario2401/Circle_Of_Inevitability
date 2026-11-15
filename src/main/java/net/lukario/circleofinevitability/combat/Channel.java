package net.lukario.circleofinevitability.combat;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Owned, interruptible channel. Call tick from the spell's server tick and close on cleanup.
 * Pulses receive a zero-based index. The lifetime condition must include the owner's cast identity.
 * Each instance releases only its own root, even when interrupted by another spell's stun. */
public final class Channel implements AutoCloseable {
    private final LivingEntity caster;
    private final ServerLevel level;
    private final String root = "channel_" + java.util.UUID.randomUUID();
    private final BooleanSupplier condition;
    private final IntConsumer pulse;
    private final Runnable end;
    private final long until;
    private final int period;
    private long next;
    private int index;
    private boolean closed;

    public Channel(LivingEntity caster, int duration, int period, int firstDelay, boolean rooted,
                   BooleanSupplier condition, IntConsumer pulse, Runnable end) {
        if (!(caster.level() instanceof ServerLevel server)) throw new IllegalArgumentException("Server channel required");
        this.caster=caster; this.level=server; this.period=Math.max(1,period);
        this.condition=condition; this.pulse=pulse; this.end=end;
        until=server.getGameTime()+Math.max(1,duration); next=server.getGameTime()+Math.max(0,firstDelay);
        if(rooted)Root.applyOwned(caster,root,duration,Set.of(RootRestriction.MOVE,RootRestriction.JUMP));
    }
    public boolean active() { return !closed; }
    public void tick() {
        if(closed)return;
        long now=level.getGameTime();
        if(now>=until || !caster.isAlive() || caster.isRemoved() || caster.level()!=level
                || Silence.isSilenced(caster) || Root.has(caster,RootRestriction.ATTACK) || !condition.getAsBoolean()) {
            close(); return;
        }
        if(now>=next) {
            next=now+period;
            try { pulse.accept(index++); } catch(RuntimeException error) { close(); throw error; }
        }
    }
    @Override public void close() {
        if(closed)return; closed=true; Root.removeOwned(caster,root); if(end!=null)end.run();
    }
}
