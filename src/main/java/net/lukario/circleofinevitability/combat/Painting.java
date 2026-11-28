package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Painting: a picture hung in the world that takes hold of whoever keeps it in view. The Observer's Canvas wheel
 * (docs/pathways/observer/OBSERVER.md, "Paintings"), and any placed picture, glyph or sign that has to be looked at:
 *
 *   Painting.of("my_pathway_riot")
 *           .lasts(900)                                   // ticks it hangs (default 900)
 *           .stare(60)                                    // ticks an enemy must keep it in view (default 60)
 *           .range(20).halfAngle(55)                      // "in view": this close, this far off its look, clear line
 *           .onStare((hung, viewer, progress) -> ...)     // every 5 ticks while someone stares: 0..1
 *           .onSeen((hung, viewer) -> ...)                // it took hold of 'viewer' (hung.power() for its strength)
 *           .keeps()                                      // not spent when seen: each viewer is taken once, and
 *           .onKeepLooking((hung, viewer) -> ...)         // ...every second one of them keeps looking after
 *           .onField(20, hung -> ...)                     // a field Painting: this every 20 ticks from the moment it hangs
 *           .onEnd((hung, spent) -> ...)                  // spent (it took hold) or faded (its time, the owner gone, replaced)
 *           .hang(player, sl, spot, facing, 3);           // this owner may have 3 of this kind at once (a fourth
 *                                                         // replaces the oldest); returns the Hung, or null
 *
 * Its power fades over its life: power() is 1 when fresh and 1 - FADE (two thirds) at the end ("Beyonder weapons lose
 * their power over time"). Who it takes: the owner's enemies (Spells.isEnemy), or .viewers(entity -> ...).
 * How long the stare is can be changed for everyone (once, in a static block):
 *
 *   Painting.addStareRule((viewer, hung, ticks) -> ...)   // Observation cuts it, a Painting World makes it instant
 *
 *   Painting.of(owner, kind)    Painting.near(sl, spot, 16)    Painting.at(sl, spot, 4)    Painting.clear(owner, kind)
 *   hung.end()   hung.spend()   hung.ticksLeft()   hung.spot()   hung.facing()   hung.owner()
 *
 * A Painting has no look of its own: draw the canvas when you hang it (it is told nothing when it is drawn), and take
 * it away in onEnd. It ends when its owner dies, logs out or leaves the dimension. Not saved: a restart takes them down.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Painting {

    /** How much of its power a Painting has lost by the end of its life. */
    public static float FADE = 1f / 3f;
    /** How often the stares are checked, in ticks. */
    private static final int CHECK = 5;

    @FunctionalInterface
    public interface Stare {
        void run(Hung hung, LivingEntity viewer, float progress);
    }

    @FunctionalInterface
    public interface Seen {
        void run(Hung hung, LivingEntity viewer);
    }

    @FunctionalInterface
    public interface Field {
        void run(Hung hung);
    }

    @FunctionalInterface
    public interface Ended {
        void run(Hung hung, boolean spent);
    }

    /** Changes how many ticks 'viewer' has to stare at 'hung' ('ticks' is what it would be otherwise). */
    @FunctionalInterface
    public interface StareRule {
        int ticks(LivingEntity viewer, Hung hung, int ticks);
    }

    private static final List<Hung> HUNG = new ArrayList<>();
    private static final List<StareRule> STARE_RULES = new ArrayList<>();

    // ---------- what it is ----------

    private final String kind;
    private int lasts = 900;
    private int stare = 60;
    private double range = 20.0;
    private double halfAngle = 55.0;
    private boolean keeps = false;
    private int fieldEvery = 0;
    private Predicate<LivingEntity> viewers = null;
    private Stare onStare;
    private Seen onSeen;
    private Seen onKeepLooking;
    private Field onField;
    private Ended onEnd;

    private Painting(String kind) {
        this.kind = kind;
    }

    // =========================
    // Building one
    // =========================

    public static Painting of(String kind) {
        return new Painting(kind);
    }

    public Painting lasts(int ticks) { this.lasts = Math.max(1, ticks); return this; }

    public Painting stare(int ticks) { this.stare = Math.max(0, ticks); return this; }

    public Painting range(double blocks) { this.range = Math.max(1.0, blocks); return this; }

    public Painting halfAngle(double degrees) { this.halfAngle = Math.max(1.0, degrees); return this; }

    public Painting keeps() { this.keeps = true; return this; }

    public Painting viewers(Predicate<LivingEntity> who) { this.viewers = who; return this; }

    public Painting onStare(Stare action) { this.onStare = action; return this; }

    public Painting onSeen(Seen action) { this.onSeen = action; return this; }

    public Painting onKeepLooking(Seen action) { this.onKeepLooking = action; return this; }

    /** A field Painting: no stare, 'action' every 'every' ticks from the moment it hangs. */
    public Painting onField(int every, Field action) {
        this.fieldEvery = Math.max(1, every);
        this.onField = action;
        return this;
    }

    public Painting onEnd(Ended action) { this.onEnd = action; return this; }

    /**
     * Hangs it at 'spot' (the canvas's middle) with its front looking along 'facing'. The owner may have 'max' of this
     * kind at once; one more replaces the oldest (it fades). Null if the owner is not in that level.
     */
    public Hung hang(LivingEntity owner, ServerLevel sl, Vec3 spot, Vec3 facing, int max) {
        if (owner.level() != sl || !owner.isAlive()) return null;
        List<Hung> mine = of(owner, kind);
        while (max > 0 && mine.size() >= max) mine.remove(0).finish(false);
        Hung hung = new Hung(this, owner, sl, spot, facing.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : facing.normalize());
        HUNG.add(hung);
        return hung;
    }

    // =========================
    // A hung one
    // =========================

    public static final class Hung {
        private final Painting painting;
        private final LivingEntity owner;
        private final ServerLevel level;
        private final Vec3 spot;
        private final Vec3 facing;
        private final long born;
        private final String id = UUID.randomUUID().toString();
        private final Set<UUID> taken = new HashSet<>();
        private int age;
        private boolean over;

        private Hung(Painting painting, LivingEntity owner, ServerLevel level, Vec3 spot, Vec3 facing) {
            this.painting = painting;
            this.owner = owner;
            this.level = level;
            this.spot = spot;
            this.facing = facing;
            this.born = level.getGameTime();
        }

        public String kind() { return painting.kind; }
        public LivingEntity owner() { return owner; }
        public ServerLevel level() { return level; }
        public Vec3 spot() { return spot; }
        public Vec3 facing() { return facing; }
        public String id() { return id; }
        public long born() { return born; }
        public int lasts() { return painting.lasts; }
        public boolean isOver() { return over; }
        public boolean isField() { return painting.onField != null; }

        public int ticksLeft() {
            return over ? 0 : Math.max(0, painting.lasts - age);
        }

        /** 1 when fresh, 1 - FADE at the end of its life. */
        public float power() {
            return 1f - FADE * Math.min(1f, age / (float) painting.lasts);
        }

        /** Takes it down: faded. */
        public void end() {
            finish(false);
        }

        /** Takes it down: spent. */
        public void spend() {
            finish(true);
        }

        private void finish(boolean spent) {
            if (over) return;
            over = true;
            HUNG.remove(this);
            Gaze.forgetAll("painting_" + id);
            if (painting.onEnd != null) painting.onEnd.run(this, spent);
        }

        private boolean takes(LivingEntity one) {
            if (one == owner || !one.isAlive() || one.isSpectator()) return false;
            return painting.viewers != null ? painting.viewers.test(one) : Spells.isEnemy(owner, one);
        }

        private void tick() {
            age += CHECK;
            if (!owner.isAlive() || owner.isRemoved() || owner.level() != level) {
                finish(false);
                return;
            }
            if (age >= painting.lasts) {
                finish(false);
                return;
            }
            if (painting.onField != null) {
                if (age % painting.fieldEvery < CHECK) painting.onField.run(this);
                return;
            }
            AABB box = new AABB(spot, spot).inflate(painting.range);
            for (LivingEntity one : level.getEntitiesOfClass(LivingEntity.class, box, this::takes)) {
                if (over) return;
                boolean looking = Gaze.looking(one, spot, painting.halfAngle, painting.range);
                if (taken.contains(one.getUUID())) {
                    if (painting.onKeepLooking == null) continue;
                    if (Gaze.stare(one, "painting_" + id + "_after", looking, CHECK, 20) >= 1f) {
                        Gaze.forget(one, "painting_" + id + "_after");
                        painting.onKeepLooking.run(this, one);
                    }
                    continue;
                }
                int needed = painting.stare;
                for (StareRule rule : STARE_RULES) needed = rule.ticks(one, this, needed);
                float full = Gaze.stare(one, "painting_" + id, looking, CHECK, needed);
                if (!looking) continue;
                if (full < 1f) {
                    if (painting.onStare != null) painting.onStare.run(this, one, full);
                    continue;
                }
                Gaze.forget(one, "painting_" + id);
                taken.add(one.getUUID());
                if (painting.onSeen != null) painting.onSeen.run(this, one);
                if (!painting.keeps) {
                    finish(true);
                    return;
                }
            }
        }
    }

    // =========================
    // Asking
    // =========================

    public static void addStareRule(StareRule rule) {
        STARE_RULES.add(rule);
    }

    /** The owner's hung Paintings of this kind, oldest first. */
    public static List<Hung> of(LivingEntity owner, String kind) {
        List<Hung> result = new ArrayList<>();
        for (Hung hung : HUNG) {
            if (hung.owner.getUUID().equals(owner.getUUID()) && hung.painting.kind.equals(kind)) result.add(hung);
        }
        return result;
    }

    /** Every hung Painting (anyone's) within 'radius' of 'center', nearest first. */
    public static List<Hung> near(ServerLevel sl, Vec3 center, double radius) {
        List<Hung> result = new ArrayList<>();
        for (Hung hung : HUNG) {
            if (hung.level == sl && hung.spot.distanceToSqr(center) <= radius * radius) result.add(hung);
        }
        result.sort((a, b) -> Double.compare(a.spot.distanceToSqr(center), b.spot.distanceToSqr(center)));
        return result;
    }

    /** The nearest hung Painting within 'radius' of 'spot', or null. */
    public static Hung at(ServerLevel sl, Vec3 spot, double radius) {
        List<Hung> near = near(sl, spot, radius);
        return near.isEmpty() ? null : near.getFirst();
    }

    /** Takes down all of the owner's Paintings of this kind (they fade). */
    public static void clear(LivingEntity owner, String kind) {
        for (Hung hung : of(owner, kind)) hung.end();
    }

    /** Takes down every Painting the owner has, whatever its kind (they fade). */
    public static void clearAll(LivingEntity owner) {
        for (Hung hung : new ArrayList<>(HUNG)) {
            if (hung.owner.getUUID().equals(owner.getUUID())) hung.end();
        }
    }

    // =========================
    // Inside
    // =========================

    @SubscribeEvent
    public static void onPaintingLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel sl) || HUNG.isEmpty() || sl.getGameTime() % CHECK != 0) return;
        for (Hung hung : new ArrayList<>(HUNG)) {
            if (hung.level == sl && !hung.over) hung.tick();
        }
    }

    @SubscribeEvent
    public static void onPaintingLevelUnload(LevelEvent.Unload event) {
        HUNG.removeIf(hung -> hung.level == event.getLevel());
    }
}
