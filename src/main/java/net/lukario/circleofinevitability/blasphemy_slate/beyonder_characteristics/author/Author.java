package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The Spectator pathway: 26 casts from Spectator to Visionary. */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Author {
    public static final String PATHWAY = "Author";
    public static final AbilityWheel BASIC = wheel("basic", "Observation and Therapy");
    public static final AbilityWheel HYPNOSIS = wheel("hypnosis", "Hypnosis");
    public static final AbilityWheel DREAMS = wheel("dreams", "Dreams");
    public static final AbilityWheel MIND = wheel("mind", "Mind");
    public static final AbilityWheel AUTHORSHIP = wheel("authorship", "Authorship");
    static final List<AbilityWheel> WHEELS = List.of(BASIC, HYPNOSIS, DREAMS, MIND, AUTHORSHIP);
    private static boolean initialized;

    static {
        entry(BASIC, "observe", "Observe", 9, 4);
        entry(BASIC, "spectator_state", "Spectator State", 9, 6);
        entry(BASIC, "awe", "Awe", 8, 80);
        entry(BASIC, "frenzy", "Frenzy", 8, 100);
        entry(BASIC, "psychoanalysis", "Psychoanalysis", 7, 350);
        entry(BASIC, "placate", "Placate", 7, 400);
        entry(HYPNOSIS, "suggestion", "Suggestion", 6, 900);
        entry(HYPNOSIS, "sleep", "Sleep", 6, 1000);
        entry(HYPNOSIS, "psychological_invisibility", "Psychological Invisibility", 6, 1200);
        entry(HYPNOSIS, "battle_hypnosis", "Battle Hypnosis", 6, 1400);
        entry(DREAMS, "dream_entry", "Dream Entry", 5, 2200);
        entry(DREAMS, "nightmare", "Nightmare", 5, 2800);
        entry(DREAMS, "dream_maze", "Dream Maze", 5, 3200);
        entry(DREAMS, "dream_anchor", "Dream Anchor", 5, 2600);
        entry(MIND, "dragon_form", "Mind Dragon", 4, 6000);
        entry(MIND, "mind_storm", "Mind Storm", 4, 5000);
        entry(MIND, "manipulate", "Manipulate", 4, 6500);
        entry(MIND, "virtual_persona", "Virtual Persona", 4, 4500);
        entry(MIND, "mind_walk", "Mind Walk", 3, 8000);
        entry(MIND, "discern", "Discerner's Gaze", 2, 10000);
        entry(AUTHORSHIP, "foreshadow", "Foreshadow", 1, 12000);
        entry(AUTHORSHIP, "plot_twist", "Plot Twist", 1, 14000);
        entry(AUTHORSHIP, "fate_script", "Fate Script", 1, 16000);
        entry(AUTHORSHIP, "envision", "Envision", 0, 18000);
        entry(AUTHORSHIP, "life_creation", "Life Creation", 0, 22000);
        entry(AUTHORSHIP, "sea_of_minds", "Sea of Collective Subconscious", 0, 30000);
    }

    private Author() {}

    private static AbilityWheel wheel(String id, String title) {
        return AbilityWheel.create("author_" + id, title).pathway(PATHWAY).noCooldownsWhen(p -> true);
    }

    private static void entry(AbilityWheel wheel, String id, String name, int sequence, int cost) {
        wheel.entry(id, "ability_menus/author/" + id, name, sequence, cost, 0,
                (p, sl) -> perform(p, sl, id)).notCopyable();
    }

    /** Called at common setup, rather than lazily after someone first casts. */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        Cleansing.addListener(AuthorMind::cleanse);
        Slumber.addWakeListener((t, kind) -> AuthorMind.wake(t));
    }

    static boolean own(Player p) { return PATHWAY.equals(SoulCore.getPathway(p)); }

    static boolean eligible(ServerPlayer p, LivingEntity t, double range, boolean sight) {
        if (t == null || !p.isAlive() || !t.isAlive() || t.level() != p.level()
                || p.distanceToSqr(t) > range * range || !Spells.isEnemy(p, t) || !Spells.canBeChosen(p, t)) return false;
        if (t instanceof Player other && (other.isCreative() || other.isSpectator()
                || !p.canHarmPlayer(other))) return false;
        return !sight || p.hasLineOfSight(t);
    }

    static LivingEntity aim(ServerPlayer p, double range) {
        return Spells.aimLiving(p, p.level(), range, false, t -> eligible(p, t, range, true));
    }

    private static AbilityWheel.Result refuse(ServerPlayer p, String reason) {
        p.sendOverlayMessage(Component.literal(reason));
        return AbilityWheel.Result.REFUSED;
    }

    public static void key(Player player, Level level, ServerLevel sl, int key, boolean bypass) {
        if (!(player instanceof ServerPlayer p) || !own(p) || !p.isAlive()) return;
        if (key < 1 || key > 7 || !Beyonder.hasSequence(p, key == 7 ? 1 : 10 - key)) return;
        if (key <= 3) {
            String[][] keys = {{"observe", "spectator_state"}, {"awe", "frenzy"}, {"psychoanalysis", "placate"}};
            BASIC.cast(p, BASIC.entry(keys[key - 1][p.isShiftKeyDown() ? 1 : 0]));
        } else {
            AbilityWheel wheel = key == 4 ? HYPNOSIS : key == 5 ? DREAMS : key == 6 ? MIND : AUTHORSHIP;
            if (p.isShiftKeyDown()) wheel.quickCast(p); else wheel.open(p);
        }
    }

    private static AbilityWheel.Result perform(ServerPlayer p, ServerLevel sl, String id) {
        if (!own(p) || !p.isAlive() || p.isSpectator() || Root.has(p, RootRestriction.ABILITIES))
            return AbilityWheel.Result.REFUSED;
        initialize();
        var s = AuthorMind.state(p); long now = sl.getGameTime();
        if (AuthorAdvanced.IDS.contains(id)) return AuthorAdvanced.cast(p, s, id);
        LivingEntity t;
        switch (id) {
            case "observe" -> {
                t = aim(p, 24);
                if (t == null) return refuse(p, "Look at an enemy within 24 blocks.");
                AuthorMind.add(p, t, s.advanced.discern > now ? 2 : 1); s.observing = t.getUUID(); s.observationStart = now;
                AuthorFx.cast(p, t, id, "mind_eye");
            }
            case "spectator_state" -> {
                s.composure = now + 160; s.observationBonus = true;
                AuthorFx.cast(p, p, id, "observer_mask");
            }
            case "placate" -> {
                List<LivingEntity> friends = new ArrayList<>(); friends.add(p);
                Spells.livingAround(p, sl, p.position(), 8).stream().filter(e -> e != p && Spells.isFriend(p, e))
                        .filter(p::hasLineOfSight).sorted(Comparator.comparingDouble(p::distanceToSqr)).limit(7).forEach(friends::add);
                for (var friend : friends) {
                    Fear.end(friend); Slumber.wake(friend); AuthorMind.cleanse(friend);
                    AuthorMind.resolve(friend, 80); friend.heal(8);
                    AuthorFx.above(sl, friend, "lotus");
                }
                AuthorFx.sound(p, id);
            }
            case "psychological_invisibility" -> {
                Unnoticed.hide(p, 160); s.invisible = now + 160;
                AuthorFx.cast(p, p, id, "observer_mask");
            }
            case "dream_maze" -> {
                Vec3 at = Spells.aimGround(p, sl, 20);
                var support = net.minecraft.core.BlockPos.containing(at.add(0, -.05, 0));
                if (at.distanceToSqr(p.position()) > 20 * 20 || !sl.hasChunkAt(support)
                        || sl.getBlockState(support).getCollisionShape(sl, support).isEmpty()
                        || !Spells.clearLine(p, sl, p.getEyePosition(), at.add(0, .2, 0)))
                    return refuse(p, "Aim at visible ground within 20 blocks.");
                hostile(p, s); s.maze = at; s.mazeStarts = now + 20; s.mazeEnds = now + 140;
                AuthorFx.maze(sl, at, s.sequence <= 3 ? 8 : 6, true); AuthorFx.sound(p, id);
            }
            case "nightmare" -> {
                t = AuthorMind.entity(sl, s.link);
                if (s.linkUntil <= now || !eligible(p, t, 24, true)) return refuse(p, "A visible Dream Link within 24 blocks is required.");
                hostile(p, s); s.link = null;
                s.pending.add(new AuthorMind.Pending(id, t.getUUID(), now + 15, 0));
                AuthorFx.cast(p, t, id, "terror_eye");
            }
            default -> {
                double range = id.equals("awe") || id.equals("frenzy") ? 18 : id.equals("psychoanalysis") || id.equals("dream_entry") ? 24 : 20;
                t = aim(p, range);
                if (t == null) return refuse(p, "No visible enemy in range.");
                int needed = switch (id) {
                    case "frenzy", "suggestion" -> 2;
                    case "psychoanalysis" -> s.sequence <= 6 ? 2 : 3;
                    case "sleep", "battle_hypnosis" -> 3;
                    case "dream_entry" -> AuthorMind.sleeping(t) ? 0 : 3;
                    default -> 0;
                };
                if (AuthorMind.insight(p, t) < needed) return refuse(p, "This ability needs " + needed + " Insight.");
                if (id.equals("awe") && (AuthorMind.resolved(t) || Fear.isImmune(t) || Fear.isAfraid(t)))
                    return refuse(p, "That mind resists fear.");
                if ((id.equals("sleep") || id.equals("battle_hypnosis")) && !AuthorMind.canControl(t, id.equals("sleep")))
                    return refuse(p, "That mind resists control.");
                if (id.equals("frenzy") && AuthorMind.resolved(t)) return refuse(p, "That mind has Resolve.");
                if (Set.of("awe", "frenzy", "sleep", "battle_hypnosis", "suggestion").contains(id)
                        && AuthorAdvanced.resists(p, t)) return refuse(p, "A mental defense intercepts the influence.");
                if (id.equals("awe")) {
                    int bonus = AuthorMind.insight(p, t) >= 2 ? 2 : 0;
                    AuthorMind.spend(p, t, bonus);
                    s.pending.add(new AuthorMind.Pending(id, t.getUUID(), now + 6, bonus));
                } else if (id.equals("sleep")) {
                    AuthorMind.spend(p, t, needed);
                    s.pending.add(new AuthorMind.Pending(id, t.getUUID(), now + 15, 0));
                } else if (id.equals("battle_hypnosis")) {
                    if (!AuthorMind.control(p, t, false)) return AbilityWheel.Result.REFUSED;
                    AuthorMind.spend(p, t, needed);
                } else {
                    AuthorMind.spend(p, t, needed);
                    switch (id) {
                        case "frenzy" -> {
                            int ticks = t instanceof Player a && own(a) && SoulCore.getSequence(a) <= 7 ? 60 : 120;
                            Falter.apply(t, AuthorMind.token(p, id), t instanceof Player || Spells.isBoss(t) ? .2f : .25f, ticks);
                            AuthorMind.mark(p, t, id, ticks, 0);
                            if (t instanceof Mob mob) mob.setTarget(null);
                        }
                        case "psychoanalysis" -> {
                            AuthorAdvanced.strikeMental(p, t, 36);
                            if (t.isAlive()) Spells.keepEffect(t, MobEffects.WEAKNESS, 0, 100);
                        }
                        case "suggestion" -> AuthorMind.mark(p, t, id, 160, t instanceof Player || Spells.isBoss(t) ? .2f : .3f);
                        case "dream_entry" -> { s.link = t.getUUID(); s.linkUntil = now + 160; s.lostSight = 0; }
                        default -> { return AbilityWheel.Result.REFUSED; }
                    }
                }
                hostile(p, s);
                String model = switch (id) {
                    case "awe" -> "terror_eye";
                    case "frenzy", "psychoanalysis" -> "fractured_mask";
                    case "suggestion" -> "pendulum";
                    case "sleep" -> "dream_lantern";
                    case "battle_hypnosis" -> "thought_cage";
                    default -> "dream_gate";
                };
                AuthorFx.cast(p, t, id, model);
            }
        }
        return AbilityWheel.Result.DONE;
    }

    static void hostile(ServerPlayer p, AuthorMind.State s) {
        Unnoticed.end(p); s.invisible = 0;
    }

    static void tickPlayer(ServerPlayer p) {
        if (!own(p) || !p.isAlive()) { AuthorMind.clear(p); return; }
        var s = AuthorMind.state(p); ServerLevel sl = p.level(); long now = sl.getGameTime();
        s.insight.entrySet().removeIf(entry -> {
            var target = AuthorMind.entity(sl, entry.getKey());
            if (target == null || !eligible(p, target, 64, false)) return true;
            var gauge = entry.getValue();
            if (now >= gauge.decay) { gauge.pips--; gauge.decay = now + 80; }
            return gauge.pips <= 0;
        });
        if (s.observing != null) {
            LivingEntity t = AuthorMind.entity(sl, s.observing);
            if (t == null || aim(p, 24) != t) s.observing = null;
            else {
                long elapsed = now - s.observationStart;
                if (elapsed == 20 || elapsed == 40) AuthorMind.add(p, t, 1);
                if (elapsed >= 40) {
                    if (s.observationBonus && s.composure > now) { AuthorMind.add(p, t, 1); s.observationBonus = false; }
                    s.observing = null;
                }
            }
        }
        for (var pending : List.copyOf(s.pending)) {
            double range = pending.kind().equals("awe") ? 18 : pending.kind().equals("sleep") ? 20 : 24;
            LivingEntity t = AuthorMind.entity(sl, pending.target());
            // Losing sight during induction cancels it, even if sight is regained before the final tick.
            if (!eligible(p, t, range, true)) { s.pending.remove(pending); continue; }
            if (now < pending.due()) {
                if (now % 5 == 0) AuthorFx.above(sl, t, pending.kind().equals("sleep") ? "dream_lantern" : "terror_eye");
                continue;
            }
            s.pending.remove(pending);
            switch (pending.kind()) {
                case "awe" -> {
                    if (!AuthorMind.resolved(t) && !Fear.isImmune(t) && !Fear.isAfraid(t) && !AuthorAdvanced.resists(p, t)) {
                        int ticks = AuthorMind.duration(p, t, 60, true);
                        if (Fear.apply(t, p, ticks)) {
                            AuthorMind.mark(p, t, "awe", ticks, 0);
                            if (pending.insight() > 0) AuthorAdvanced.strikeMental(p, t, 12);
                        }
                    }
                }
                case "sleep" -> AuthorMind.control(p, t, true);
                case "nightmare" -> {
                    if (Slumber.isAsleep(t)) { Slumber.wake(t); AuthorMind.resolve(t, 120); }
                    AuthorAdvanced.strikeMental(p, t, 48);
                    if (t.isAlive() && !AuthorMind.resolved(t) && !Fear.isAfraid(t) && !AuthorAdvanced.resists(p, t)) {
                        int ticks = AuthorMind.duration(p, t, 40, true);
                        if (Fear.apply(t, p, ticks)) AuthorMind.mark(p, t, "awe", ticks, 0);
                    }
                }
                default -> { }
            }
        }
        if (s.link != null) {
            LivingEntity t = AuthorMind.entity(sl, s.link);
            if (!eligible(p, t, 32, false) || s.linkUntil <= now) s.link = null;
            else if (!p.hasLineOfSight(t)) {
                if (s.lostSight == 0) s.lostSight = now;
                if (now - s.lostSight >= 40) s.link = null; // Dream Recall (Sequence 5).
            } else {
                s.lostSight = 0;
                if (now % 5 == 0) AuthorFx.above(sl, t, "dream_gate");
            }
        }
        for (String key : List.copyOf(s.marks.keySet())) {
            var m = s.marks.get(key); LivingEntity t = AuthorMind.entity(sl, m.target());
            boolean ended = m.until() <= now || !eligible(p, t, 64, false)
                    || m.kind().equals("sleep") && !Slumber.owner(t).equals(m.token())
                    || m.kind().equals("awe") && (!Fear.isAfraid(t) || Fear.sourceOf(t) != p);
            if (ended) AuthorMind.remove(p, s, key, t);
            else if (now % 5 == 0) AuthorFx.above(sl, t, switch (m.kind()) {
                case "sleep" -> "dream_lantern"; case "battle_hypnosis" -> "thought_cage";
                case "suggestion" -> "pendulum"; default -> "fractured_mask";
            });
        }
        if (s.maze != null) {
            if (s.mazeEnds <= now) s.maze = null;
            else if (now % 5 == 0) {
                double radius = s.sequence <= 3 ? 8 : 6;
                AuthorFx.maze(sl, s.maze, radius, now < s.mazeStarts);
                if (now >= s.mazeStarts) for (var t : Spells.enemiesAround(p, sl, s.maze, radius).stream()
                        .filter(e -> eligible(p, e, 64, false))
                        .filter(e -> Spells.clearLine(p, sl, s.maze.add(0, .5, 0), e.getEyePosition()))
                        .sorted(Comparator.<LivingEntity>comparingDouble(e -> e.position().distanceToSqr(s.maze))
                                .thenComparing(Entity::getUUID)).limit(12).toList()) {
                    if (AuthorMind.resolved(t)) continue;
                    Spells.keepEffect(t, MobEffects.SLOWNESS, 1, 20);
                    Falter.apply(t, AuthorMind.token(p, "maze"), .2f, 20);
                    AuthorMind.mark(p, t, "maze", 20, 0);
                }
            }
        }
        if (now % 5 == 0) {
            LivingEntity t = aim(p, 24); AuthorFx.hud(p, t, t == null ? 0 : AuthorMind.insight(p, t));
            if (s.composure > now) AuthorFx.above(sl, p, "observer_mask");
        }
        AuthorAdvanced.tick(p, s);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player() instanceof ServerPlayer p) tickPlayer(p);
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0) return;
        LivingEntity victim = event.getEntity();
        AuthorAdvanced.hurt(event);
        if (victim instanceof ServerPlayer p) {
            var s = AuthorMind.STATES.get(p.getUUID());
            if (s != null && own(p)) {
                if (s.composure > p.level().getGameTime() && event.getSource().getDirectEntity() instanceof Mob)
                    event.setAmount(event.getAmount() * .8f);
                Entity attacker = event.getSource().getEntity();
                if (attacker instanceof LivingEntity t) {
                    var m = s.marks.get("suggestion" + t.getUUID());
                    if (m != null && m.until() > p.level().getGameTime() && eligible(p, t, 64, true)) {
                        event.setAmount(event.getAmount() * (1 - m.amount()));
                        AuthorMind.remove(p, s, "suggestion" + t.getUUID(), t);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0) return;
        AuthorMind.wake(event.getEntity());
        AuthorAdvanced.damaged(event);
        var s = AuthorMind.STATES.get(event.getEntity().getUUID());
        if (s != null) s.observing = null;
    }

    @SubscribeEvent
    public static void targeting(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof ServerPlayer p) || !own(p) || SoulCore.getSequence(p) > 8
                || !p.hasLineOfSight(event.getEntity()) || p.distanceToSqr(event.getEntity()) > 256) return;
        long now = p.level().getGameTime();
        if (p.getPersistentData().getLongOr("author_awareness_ready", 0L) > now) return;
        p.getPersistentData().putLong("author_awareness_ready", now + 100); AuthorFx.sound(p, "warning");
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        AuthorMind.cleanse(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer p) AuthorMind.clear(p);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        AuthorMind.cleanse(p); AuthorMind.clear(p);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        for (String key : event.getOriginal().getPersistentData().keySet())
            if (key.startsWith("author_") && key.endsWith("_last"))
                event.getEntity().getPersistentData().put(key, event.getOriginal().getPersistentData().get(key).copy());
        event.getEntity().getPersistentData().putFloat("author_strain", event.getOriginal().getPersistentData().getFloatOr("author_strain", 0f));
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) { AuthorMind.cleanse(p); AuthorMind.clear(p); }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide()) return;
        AuthorMind.cleanse(target);
        for (var s : AuthorMind.STATES.values()) {
            s.insight.remove(target.getUUID());
            if (target.getUUID().equals(s.observing)) s.observing = null;
            if (target.getUUID().equals(s.advanced.summon)) s.advanced.summon = null;
        }
        if (target instanceof ServerPlayer p) AuthorMind.clear(p);
    }

    @SubscribeEvent
    public static boolean rejoinSummon(EntityJoinLevelEvent event) {
        // Imagined life is never restored from saved chunks after unload/restart.
        return event.loadedFromDisk() && event.getEntity().getPersistentData().getBooleanOr("author_imagined", false);
    }

    @SubscribeEvent
    public static boolean interactSummonAt(PlayerInteractEvent.EntityInteractSpecific event) {
        return event.getTarget().getPersistentData().getBooleanOr("author_imagined", false);
    }

    @SubscribeEvent
    public static void attackPuppet(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var s = AuthorMind.STATES.get(p.getUUID());
        if (s != null && event.getTarget().getUUID().equals(s.advanced.puppet)
                && event.getTarget() instanceof LivingEntity t) AuthorMind.cleanse(t);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (var p : List.copyOf(event.getServer().getPlayerList().getPlayers())) AuthorMind.clear(p);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { AuthorMind.STATES.clear(); }
}
