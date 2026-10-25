package net.lukario.circleofinevitability.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * A trap: a hidden spot on the ground that waits, and springs once when an enemy steps on it. For snares,
 * mines, tripwires, a rune that goes off under whoever walks over it:
 *
 *   Trap.at(ground, 1.5)                            // its middle on the ground, and how far it reaches (blocks)
 *           .armsAfter(20)                          // ticks before it can spring (default 20)
 *           .lasts(1200)                            // ticks it waits before it is gone (default 1200)
 *           .onSpring(victim -> ...)                // someone stepped on it: this is the one who did
 *           .onWait(ticksLeft -> ...)               // every 20 ticks while it lies armed: a faint look, if any
 *           .onExpire(() -> ...)                    // it ended without springing
 *           .set(player, sl, "my_pathway_trap", 3); // this player may have 3 of these at once
 *
 * Every part after at(...) is optional. Build a new one for every cast (it remembers whether it sprang), so
 * don't store one in a constant.
 *
 * HOW MANY. The name and the number say how many traps of that kind one player can have lying around. Setting
 * one more than that replaces the oldest (its onExpire runs).
 *
 * WHO SPRINGS IT. The owner's enemies (Spells.isEnemy), as every other spell: .catches(entity -> ...) for
 * something else. Only the one who stepped on it is handed to onSpring; to catch everything standing on the
 * trap, ask Spells.enemiesAround there.
 *
 * A trap has no look of its own, so that it can stay hidden. Draw the setting of it yourself when you set
 * it, and what it looks like while it waits in onWait.
 *
 *   Trap.count(player, "my_pathway_trap", 3)       // how many of them lie armed or arming right now
 *   Trap.clear(player, "my_pathway_trap", 3);      // takes them all away (their onExpire runs)
 *   Trap.hiddenNear(sl, spot, 16)                  // where every trap (anyone's) lies within 16 blocks: for a sight
 *                                                  // that sees what is hidden
 *
 * It is a Zone underneath, so it ends by itself when its owner dies, logs out or goes to another dimension,
 * and it is not saved: if the server stops, the traps are gone.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Trap {

    /** How often a trap looks for someone standing on it, in ticks. */
    private static final int CHECK_EVERY = 2;

    private final Vec3 ground;
    private final double radius;
    private double height = 2.0;
    private int armsAfter = 20;
    private int lasts = 1200;
    private int showEvery = 20;
    private Predicate<LivingEntity> catches = null;
    private Consumer<LivingEntity> onSpring = null;
    private IntConsumer onWait = null;
    private Runnable onExpire = null;
    private boolean sprung = false;

    /** Every trap that was set and may still lie there (cleaned up as hiddenNear finds them gone). */
    private record Laid(LivingEntity owner, ServerLevel level, String slot, Vec3 ground) {}

    private static final List<Laid> LAID = new ArrayList<>();

    private Trap(Vec3 ground, double radius) {
        this.ground = ground;
        this.radius = radius;
    }

    /** A trap lying on the ground: 'ground' is its middle, 'radius' how far it reaches. Nothing happens until .set(...). */
    public static Trap at(Vec3 ground, double radius) {
        return new Trap(ground, radius);
    }

    // ---------- setting it up ----------

    /** How far above its ground it still catches someone, in blocks (default 2). */
    public Trap height(double blocks) {
        this.height = blocks;
        return this;
    }

    /** How long after it was set it can spring, in ticks. 0 = at once, also under someone standing there already. */
    public Trap armsAfter(int ticks) {
        this.armsAfter = Math.max(0, ticks);
        return this;
    }

    /** How long it waits before it is gone, in ticks (counted from when it was set). */
    public Trap lasts(int ticks) {
        this.lasts = Math.max(1, ticks);
        return this;
    }

    /** How often onWait runs, in ticks. */
    public Trap showEvery(int ticks) {
        this.showEvery = Math.max(CHECK_EVERY, ticks - ticks % CHECK_EVERY);
        return this;
    }

    /** Who springs it, instead of the owner's enemies. The owner never does. */
    public Trap catches(Predicate<LivingEntity> who) {
        this.catches = who;
        return this;
    }

    /** Runs once, when someone steps on the armed trap, with the one who did. The trap is gone by then. */
    public Trap onSpring(Consumer<LivingEntity> action) {
        this.onSpring = action;
        return this;
    }

    /** Runs every 'showEvery' ticks while the trap lies armed, with the ticks it has left: a glint, a hum. */
    public Trap onWait(IntConsumer action) {
        this.onWait = action;
        return this;
    }

    /** Runs when it ends without springing: its time is up, a newer trap took its place, or its owner died or left. */
    public Trap onExpire(Runnable action) {
        this.onExpire = action;
        return this;
    }

    /**
     * Sets the trap. 'name' is the kind of trap (start it with your Pathway) and 'max' how many of that kind
     * this player may have at once: one more replaces the oldest. Always give the same 'max' for the same name.
     * Returns the name of the Zone it lies in (Zone.isOpen, Zone.ticksLeft and Zone.close work on it).
     */
    public String set(LivingEntity owner, ServerLevel sl, String name, int max) {
        String slot = slotFor(owner, name, Math.max(1, max));
        sprung = false;

        Zone zone = Zone.at(ground, radius).height(height).lasts(lasts).every(CHECK_EVERY)
                .onInside((entity, ticksInside) -> {
                    if (lasts - Zone.ticksLeft(owner, slot) < armsAfter) return;     // not armed yet
                    sprung = true;
                    Zone.close(owner, slot);                                         // a trap springs once
                    if (onSpring != null) onSpring.accept(entity);
                })
                .onPulse(ticksLeft -> {
                    int age = lasts - ticksLeft;
                    if (onWait != null && age >= armsAfter && age % showEvery == 0) onWait.accept(ticksLeft);
                })
                .onEnd(() -> {
                    if (!sprung && onExpire != null) onExpire.run();
                });
        if (catches != null) zone.affects(catches);
        zone.open(owner, sl, slot);
        LAID.removeIf(laid -> laid.owner().getUUID().equals(owner.getUUID()) && laid.slot().equals(slot));
        LAID.add(new Laid(owner, sl, slot, ground));
        return slot;
    }

    // ---------- asking / stopping ----------

    /** How many traps of this kind the player has lying around right now (armed or still being armed). */
    public static int count(LivingEntity owner, String name, int max) {
        int count = 0;
        for (int i = 0; i < max; i++) {
            if (Zone.isOpen(owner, slotName(name, i))) count++;
        }
        return count;
    }

    /** Takes all of the player's traps of this kind away (their onExpire runs). */
    public static void clear(LivingEntity owner, String name, int max) {
        for (int i = 0; i < max; i++) {
            Zone.close(owner, slotName(name, i));
        }
    }

    /** Where the traps (of anyone, of any kind) lie within 'radius' of 'center', armed or not. */
    public static List<Vec3> hiddenNear(ServerLevel sl, Vec3 center, double radius) {
        LAID.removeIf(laid -> !Zone.isOpen(laid.owner(), laid.slot()));
        List<Vec3> found = new ArrayList<>();
        for (Laid laid : LAID) {
            if (laid.level() == sl && laid.ground().distanceToSqr(center) <= radius * radius) found.add(laid.ground());
        }
        return found;
    }

    // ---------- internals ----------

    private static String slotName(String name, int index) {
        return name + "_" + index;
    }

    /** A free place for one more trap of this kind, or the place of the oldest one when all are taken. */
    private static String slotFor(LivingEntity owner, String name, int max) {
        String oldest = slotName(name, 0);
        int leastLeft = Integer.MAX_VALUE;
        for (int i = 0; i < max; i++) {
            String slot = slotName(name, i);
            if (!Zone.isOpen(owner, slot)) return slot;
            int left = Zone.ticksLeft(owner, slot);
            if (left < leastLeft) {
                leastLeft = left;
                oldest = slot;
            }
        }
        return oldest;
    }
}
