package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Caster-local, bounded Insight and source-owned effects. All entity references are resolved per tick. */
final class AuthorMind {
    static final String RESOLVE = "author_resolve_until";
    static final Map<UUID, State> STATES = new HashMap<>();

    static final class Gauge {
        int pips;
        long touched, decay;
        Gauge(int pips, long now, int idle) { this.pips = pips; touched = now; decay = now + idle; }
    }

    static final class State {
        final ResourceKey<Level> dimension;
        final int sequence;
        final Map<UUID, Gauge> insight = new LinkedHashMap<>();
        final Map<String, Mark> marks = new HashMap<>();
        final List<Pending> pending = new ArrayList<>();
        final AuthorAdvanced.State advanced = new AuthorAdvanced.State();
        UUID observing, link;
        long observationStart, composure, linkUntil, lostSight, invisible;
        boolean observationBonus;
        Vec3 maze;
        long mazeStarts, mazeEnds;
        State(ServerPlayer p) { dimension = p.level().dimension(); sequence = SoulCore.getSequence(p); }
    }

    record Mark(String kind, UUID target, long until, float amount, String token) {}
    record Pending(String kind, UUID target, long due, int insight) {}

    private AuthorMind() {}

    static State state(ServerPlayer p) {
        State old = STATES.get(p.getUUID());
        if (old != null && (!old.dimension.equals(p.level().dimension()) || old.sequence != SoulCore.getSequence(p)))
            clear(p);
        return STATES.computeIfAbsent(p.getUUID(), id -> new State(p));
    }

    static LivingEntity entity(ServerLevel sl, UUID id) {
        return id != null && sl.getEntity(id) instanceof LivingEntity e && e.isAlive() ? e : null;
    }

    static int insight(ServerPlayer p, LivingEntity target) {
        State s = STATES.get(p.getUUID());
        Gauge g = s == null ? null : s.insight.get(target.getUUID());
        return g == null ? 0 : g.pips;
    }

    static void add(ServerPlayer p, LivingEntity target, int amount) {
        State s = state(p); long now = p.level().getGameTime();
        if (!s.insight.containsKey(target.getUUID()) && s.insight.size() >= 8) {
            UUID oldest = s.insight.entrySet().stream().min(Comparator.comparingLong(e -> e.getValue().touched))
                    .orElseThrow().getKey();
            s.insight.remove(oldest);
        }
        int before = insight(p, target);
        s.insight.put(target.getUUID(), new Gauge(Math.min(5, before + amount), now, s.sequence <= 2 ? 400 : 240));
        AuthorFx.sound(p, "insight_gain");
        if (before < 3 && before + amount >= 3)
            Spells.sound(p.level(), target.position(), net.lukario.circleofinevitability.sounds.ModSounds.AUTHOR.get("warning").get(), .35f, 1f);
    }

    static boolean spend(ServerPlayer p, LivingEntity target, int amount) {
        if (insight(p, target) < amount) return false;
        if (amount == 0) return true;
        Gauge g = state(p).insight.get(target.getUUID());
        g.pips -= amount;
        if (g.pips == 0) state(p).insight.remove(target.getUUID());
        AuthorFx.sound(p, "insight_spent");
        return true;
    }

    static boolean resolved(LivingEntity target) {
        return target.getPersistentData().getLongOr(RESOLVE, 0L) > target.level().getGameTime();
    }

    static void resolve(LivingEntity target, int ticks) {
        target.getPersistentData().putLong(RESOLVE, Math.max(target.getPersistentData().getLongOr(RESOLVE, 0L),
                target.level().getGameTime() + ticks));
    }

    static int duration(ServerPlayer p, LivingEntity t, int ticks, boolean fear) {
        if (t instanceof Player || Spells.isBoss(t)) ticks = Math.min(ticks, fear ? 40 : 30);
        if (Beyonders.isBeyonder(t) && Beyonders.sequenceOf(t) < SoulCore.getSequence(p)) ticks /= 2;
        if (t instanceof Player a && Author.own(a) && SoulCore.getSequence(a) <= (fear ? 7 : 6)) ticks /= 2;
        return Math.max(1, ticks);
    }

    static boolean sleeping(LivingEntity t) { return Slumber.isAsleep(t); }

    static boolean canControl(LivingEntity t, boolean sleep) {
        if (resolved(t) || Slumber.isAsleep(t)) return false;
        for (var r : RootRestriction.EVERYTHING) if (Root.isImmune(t, r)) return false;
        if (sleep && Slumber.isImmune(t)) return false;
        return STATES.values().stream().noneMatch(s -> s.marks.values().stream()
                .anyMatch(m -> m.target.equals(t.getUUID()) && m.kind.equals("battle_hypnosis")));
    }

    static String token(ServerPlayer p, String kind) { return "author_" + kind + "_" + p.getUUID(); }

    static void mark(ServerPlayer p, LivingEntity t, String kind, int ticks, float amount) {
        State s = state(p);
        String key = kind + t.getUUID();
        // Refresh an existing mark without ending another caster's effect.
        s.marks.put(key, new Mark(kind, t.getUUID(), p.level().getGameTime() + ticks, amount, token(p, kind)));
    }

    static boolean control(ServerPlayer p, LivingEntity t, boolean sleep) {
        if (!canControl(t, sleep) || AuthorAdvanced.resists(p, t)) return false;
        int ticks = duration(p, t, sleep ? 100 : 60, false);
        String kind = sleep ? "sleep" : "battle_hypnosis";
        if (sleep) {
            if (!Slumber.putOwned(t, ticks, Slumber.Kind.SLEEP, token(p, kind))) return false;
        } else {
            Root.applyOwned(t, token(p, kind), ticks, RootRestriction.EVERYTHING);
            if (t instanceof Mob mob) { mob.setTarget(null); mob.getNavigation().stop(); }
        }
        mark(p, t, kind, ticks, 0);
        return true;
    }

    static void remove(ServerPlayer p, State s, String key, LivingEntity t) {
        Mark m = s.marks.remove(key);
        if (m == null || t == null) return;
        switch (m.kind) {
            case "sleep" -> {
                if (Slumber.owner(t).equals(m.token)) Slumber.wake(t);
                resolve(t, 120);
            }
            case "battle_hypnosis" -> { Root.removeOwned(t, m.token); resolve(t, 120); }
            case "awe" -> { if (Fear.sourceOf(t) == p) Fear.end(t); resolve(t, 120); }
            case "frenzy", "maze" -> Falter.remove(t, m.token);
            default -> { }
        }
    }

    static void cleanse(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel sl)) return;
        boolean affected = false;
        for (var entry : STATES.entrySet()) {
            State s = entry.getValue();
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null) continue;
            for (String key : List.copyOf(s.marks.keySet())) if (s.marks.get(key).target.equals(target.getUUID())) {
                remove(p, s, key, target); affected = true;
            }
            if (target.getUUID().equals(s.link)) { s.link = null; affected = true; }
            if (s.pending.removeIf(a -> target.getUUID().equals(a.target))) affected = true;
            if (AuthorAdvanced.cleanse(p, s, target)) affected = true;
        }
        if (affected) resolve(target, 120);
    }

    static void wake(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel sl)) return;
        for (var entry : STATES.entrySet()) {
            State s = entry.getValue();
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null) continue;
            for (String key : List.copyOf(s.marks.keySet())) {
                Mark m = s.marks.get(key);
                if (m.target.equals(target.getUUID()) && (m.kind.equals("sleep") || m.kind.equals("battle_hypnosis")))
                    remove(p, s, key, target);
            }
        }
    }

    static void clear(ServerPlayer p) {
        State s = STATES.remove(p.getUUID());
        if (s == null) return;
        AuthorAdvanced.clear(p, s);
        ServerLevel sl = p.level().getServer().getLevel(s.dimension);
        if (sl != null) for (String key : List.copyOf(s.marks.keySet()))
            remove(p, s, key, entity(sl, s.marks.get(key).target));
        if (s.invisible > 0 && Unnoticed.ticksLeft(p) <= Math.max(0, s.invisible - p.level().getGameTime())) Unnoticed.end(p);
        net.lukario.circleofinevitability.screen.ScreenImages.hide(p, "author_insight");
    }
}
