package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;

/** Bounded advanced authorities. Scripts react to direct combat, never arbitrary world events. */
final class AuthorAdvanced {
    static final Set<String> IDS = Set.of("dream_anchor", "dragon_form", "mind_storm", "manipulate",
            "virtual_persona", "mind_walk", "discern", "foreshadow", "plot_twist", "fate_script",
            "envision", "life_creation", "sea_of_minds");
    static final String DRAGON = "author_dragon", STRAIN = "author_strain";
    // Server event dispatch is synchronous; this prevents authored/mental damage recursively arming scripts.
    private static int mentalDepth;

    record Script(String kind, UUID target, long until) {}
    record Delayed(String kind, UUID target, long due, Vec3 spot) {}
    static final class State {
        long persona, discern, anchorUntil, stormStarts, stormEnds, bladeUntil, meleeReady, seaStarts, seaEnds;
        long puppetUntil, summonUntil, summonAttack;
        UUID puppet, summon;
        Vec3 anchor, sea;
        Script script;
        int bladeHits, extension;
        final Set<UUID> anchored = new HashSet<>(), seaPaid = new HashSet<>();
        final List<Delayed> delayed = new ArrayList<>();
    }

    private AuthorAdvanced() {}

    static void strikeMental(ServerPlayer p, LivingEntity target, float amount) {
        mentalDepth++;
        try { Spells.strike(p, target, amount); }
        finally { mentalDepth--; }
    }

    private static AbilityWheel.Result refuse(ServerPlayer p, String why) {
        p.sendOverlayMessage(Component.literal(why)); return AbilityWheel.Result.REFUSED;
    }

    static boolean resists(ServerPlayer caster, LivingEntity target) {
        if (!(target instanceof ServerPlayer other) || !Author.own(other)) return false;
        var s = AuthorMind.STATES.get(other.getUUID());
        if (s == null) return false;
        long now = other.level().getGameTime();
        if (s.advanced.discern > now && AuthorMind.insight(other, caster) >= 3) return true;
        if (s.advanced.persona <= now) return false;
        s.advanced.persona = 0; AuthorMind.resolve(other, 120); AuthorFx.sound(other, "resist"); return true;
    }

    static AbilityWheel.Result cast(ServerPlayer p, AuthorMind.State owner, String id) {
        var a = owner.advanced; ServerLevel sl = p.level(); long now = sl.getGameTime();
        LivingEntity t;
        switch (id) {
            case "dream_anchor" -> {
                a.anchor = p.position(); a.anchorUntil = now + (owner.sequence <= 3 ? 240 : 200); a.anchored.clear();
            }
            case "dragon_form" -> {
                if (Form.current(p) != null || p.getPersistentData().getFloatOr(STRAIN, 0f) >= 100)
                    return refuse(p, "Let your mind recover before taking dragon form.");
                Form.of(DRAGON).lasts(owner.sequence <= 3 ? 600 : 400).flies().cutsDamage(.25f)
                        .attribute(Attributes.ENTITY_INTERACTION_RANGE, 1.5, AttributeModifier.Operation.ADD_VALUE)
                        .keepWhile(q -> Author.own(q) && SoulCore.getSequence(q) <= 4)
                        .onEnd((q, why) -> {
                            if (q.getPersistentData().getFloatOr(STRAIN, 0f) >= 100)
                                Spells.keepEffect(q, MobEffects.WEAKNESS, 1, 160);
                        }).take(p);
                for (var foe : enemies(p, p.position(), 6)) if (!AuthorMind.resolved(foe) && !resists(p, foe)) {
                    int ticks = AuthorMind.duration(p, foe, 40, true);
                    if (Fear.apply(foe, p, ticks)) AuthorMind.mark(p, foe, "awe", ticks, 0);
                }
            }
            case "mind_storm" -> { a.stormStarts = now + 20; a.stormEnds = now + 61; }
            case "virtual_persona" -> a.persona = now + 300;
            case "discern" -> a.discern = now + 200;
            case "plot_twist" -> a.script = new Script(id, null, now + 200);
            case "envision" -> { a.bladeUntil = now + 240; a.bladeHits = 0; a.extension = 0; }
            case "sea_of_minds" -> {
                a.sea = p.position(); a.seaStarts = now + 30; a.seaEnds = now + 270; a.seaPaid.clear();
            }
            case "life_creation" -> {
                Vec3 spot = safeGround(p, p.position().add(Spells.flatLook(p).scale(3)), 3);
                if (spot == null || !sl.hasChunkAt(net.minecraft.core.BlockPos.containing(spot)))
                    return refuse(p, "No safe place for an imagined drake.");
                Mob drake = Marionette.spawnSummon(EntityTypes.HUSK, p, sl, spot, p.getYRot(), 400);
                if (drake == null) return AbilityWheel.Result.REFUSED;
                dismiss(p, a.summon, true);
                drake.setNoAi(true); drake.setInvisible(true); drake.setPersistenceRequired();
                drake.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80); drake.setHealth(80);
                drake.getPersistentData().putBoolean("author_imagined", true);
                a.summon = drake.getUUID(); a.summonUntil = now + 400; a.summonAttack = now;
            }
            default -> {
                t = Author.aim(p, id.equals("mind_walk") ? 32 : 24);
                if (t == null) return refuse(p, "Look at an enemy in range.");
                int cost = id.equals("manipulate") || id.equals("fate_script") ? 5 : 3;
                if (AuthorMind.insight(p, t) < cost) return refuse(p, "This ability needs " + cost + " Insight.");
                if (!id.equals("mind_walk") && (AuthorMind.resolved(t) || resists(p, t)))
                    return refuse(p, "That mind resists this influence.");
                if (id.equals("mind_walk")) {
                    Vec3 spot = walkSpot(p, t);
                    if (spot == null) return refuse(p, "No safe, unsealed route through that mind.");
                    a.delayed.add(new Delayed(id, t.getUUID(), now + 12, spot));
                } else if (id.equals("manipulate")) {
                    if (t instanceof Player || Spells.isBoss(t)) AuthorMind.mark(p, t, "manipulate", 120, .2f);
                    else if (t instanceof Mob mob && mob instanceof Enemy && !(mob instanceof OwnableEntity)
                            && !Marionette.isMarionette(mob) && !Marionette.isSummoned(mob)) {
                        // Release before the generic marionette timer, whose default expiry kills its subject.
                        if (!Marionette.control(mob, p, 260)) return AbilityWheel.Result.REFUSED;
                        dismiss(p, a.puppet, false); a.puppet = mob.getUUID(); a.puppetUntil = now + 240;
                    } else return refuse(p, "Only an ordinary hostile creature can become a puppet.");
                } else a.script = new Script(id, t.getUUID(), now + 200);
                AuthorMind.spend(p, t, cost);
                AuthorFx.above(sl, t, id.equals("manipulate") ? "mind_crown" : id.equals("mind_walk") ? "dream_gate" : "script_seal");
            }
        }
        if (!Set.of("dream_anchor", "virtual_persona", "discern", "plot_twist").contains(id)) Author.hostile(p, owner);
        AuthorFx.sound(p, id);
        return AbilityWheel.Result.DONE;
    }

    private static Vec3 walkSpot(ServerPlayer p, LivingEntity target) {
        if (!Teleport.canGo(p)) return null;
        Vec3 spot = safeGround(p, target.position().add(2, 0, 0), 2);
        if (spot == null || spot.distanceToSqr(target.position()) > 9
                || !p.level().hasChunkAt(net.minecraft.core.BlockPos.containing(spot))
                || !Dome.crossedBy(p.level(), p.position(), spot).isEmpty()) return null;
        return spot;
    }

    private static Vec3 safeGround(ServerPlayer p, Vec3 wanted, int radius) {
        ServerLevel sl = p.level();
        for (int r = 0; r <= radius; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
            for (int dy : new int[]{0, 1, -1, 2}) {
                var block = net.minecraft.core.BlockPos.containing(wanted.add(dx, dy, dz));
                if (!sl.hasChunkAt(block) || !sl.getWorldBorder().isWithinBounds(block)) continue;
                Vec3 spot = new Vec3(block.getX() + .5, block.getY(), block.getZ() + .5);
                if (!sl.getBlockState(block.below()).getCollisionShape(sl, block.below()).isEmpty()
                        && Teleport.fits(p, sl, spot)) return spot;
            }
        }
        return null;
    }

    private static List<LivingEntity> enemies(ServerPlayer p, Vec3 at, double range) {
        return Spells.enemiesAround(p, p.level(), at, range).stream()
                .filter(t -> Author.eligible(p, t, 64, false))
                .filter(t -> Spells.clearLine(p, p.level(), at.add(0, .5, 0), t.getEyePosition()))
                .sorted(Comparator.<LivingEntity>comparingDouble(t -> t.position().distanceToSqr(at)).thenComparing(Entity::getUUID))
                .limit(12).toList();
    }

    private static List<LivingEntity> friends(ServerPlayer p, Vec3 at, double radius) {
        var list = new ArrayList<LivingEntity>();
        if (p.position().distanceToSqr(at) <= radius * radius
                && Spells.clearLine(p, p.level(), at.add(0, .5, 0), p.getEyePosition())) list.add(p);
        Spells.livingAround(p, p.level(), at, radius).stream().filter(t -> t != p && Spells.isFriend(p, t))
                .filter(t -> Spells.clearLine(p, p.level(), at.add(0, .5, 0), t.getEyePosition()))
                .sorted(Comparator.comparingDouble(t -> t.position().distanceToSqr(at))).limit(11).forEach(list::add);
        return list;
    }

    static void tick(ServerPlayer p, AuthorMind.State owner) {
        State a = owner.advanced; ServerLevel sl = p.level(); long now = sl.getGameTime();
        if (now % 20 == 0) {
            float strain = p.getPersistentData().getFloatOr(STRAIN, 0f);
            strain = Form.isIn(p, DRAGON) ? Math.min(100, strain + (owner.sequence <= 3 ? 3 : 5)) : Math.max(0, strain - 2);
            p.getPersistentData().putFloat(STRAIN, strain);
            if (strain >= 100 && Form.isIn(p, DRAGON)) Form.end(p);
        }
        if (a.stormEnds > now && now >= a.stormStarts && (now - a.stormStarts) % 20 == 0) {
            for (var t : Spells.enemiesInCone(p, sl, p.getEyePosition(), p.getLookAngle(), 10, 50).stream()
                    .filter(e -> Author.eligible(p, e, 10, true)).limit(12).toList()) {
                strikeMental(p, t, 22);
                if (now == a.stormStarts && t.isAlive()) AuthorMind.add(p, t, 1);
            }
            AuthorFx.above(sl, p, "mind_ripple");
        }
        if (a.anchor != null && a.anchorUntil <= now) a.anchor = null;
        if (a.anchor != null && now % 5 == 0) {
            AuthorFx.maze(sl, a.anchor, 6, false);
            for (var friend : friends(p, a.anchor, 6)) if (a.anchored.size() < 64 && a.anchored.add(friend.getUUID())) {
                Fear.end(friend); Slumber.wake(friend); AuthorMind.wake(friend);
            }
        }
        if (a.sea != null && a.seaEnds <= now) a.sea = null;
        if (a.sea != null && now >= a.seaStarts && (now - a.seaStarts) % 40 == 0) {
            for (var friend : friends(p, a.sea, 12)) friend.heal(4);
            for (var t : enemies(p, a.sea, 12)) {
                AuthorMind.add(p, t, 1);
                if (AuthorMind.insight(p, t) >= 5 && a.seaPaid.size() < 72 && a.seaPaid.add(t.getUUID())) {
                    AuthorMind.spend(p, t, 5); strikeMental(p, t, 36);
                    if (a.bladeUntil > now && a.extension < 4) { a.bladeUntil += 20; a.extension++; }
                }
            }
        }
        for (var d : List.copyOf(a.delayed)) {
            LivingEntity t = AuthorMind.entity(sl, d.target);
            if (d.kind.equals("mind_walk") && !Author.eligible(p, t, 32, true)
                    || d.kind.equals("counter") && !Author.eligible(p, t, 24, true)) { a.delayed.remove(d); continue; }
            if (now < d.due) continue;
            a.delayed.remove(d);
            switch (d.kind) {
                case "mind_walk" -> {
                    Vec3 spot = walkSpot(p, t);
                    if (spot != null) Teleport.of(p).fallGuard(40).go(sl, spot);
                }
                case "counter" -> { strikeMental(p, t, 40); refund(p, t); }
                case "escape" -> {
                    Vec3 spot = safeGround(p, d.spot, 2);
                    if (spot != null && spot.distanceToSqr(p.position()) <= 36
                            && Dome.crossedBy(sl, p.position(), spot).isEmpty()) Teleport.of(p).fallGuard(40).go(sl, spot);
                }
                case "slash" -> {
                    for (var foe : Spells.enemiesInCone(p, sl, p.getEyePosition(), p.getLookAngle(), 6, 50).stream()
                            .filter(e -> Author.eligible(p, e, 6, true)).limit(12).toList()) strikeMental(p, foe, 24);
                    AuthorFx.above(sl, p, "thought_shard");
                }
                default -> { }
            }
        }
        if (a.script != null && a.script.until <= now) a.script = null;
        tickCreatures(p, a, now);
        if (now % 5 != 0) return;
        if (Form.isIn(p, DRAGON)) dragon(sl, p, owner.sequence <= 3 ? "mind_dragon" : "mind_dragon_incomplete", 1.6f);
        if (a.persona > now) AuthorFx.above(sl, p, "persona");
        if (a.discern > now) AuthorFx.above(sl, p, "mind_eye");
        if (a.bladeUntil > now) ParticleShapes.modelOnTurning(sl, AuthorFx.model("envisioned_blade", 10).pivot(8, 0, 8), p, new Vec3(.7, .6, 0), 0, 0, 0);
        if (a.script != null) {
            LivingEntity t = AuthorMind.entity(sl, a.script.target);
            AuthorFx.above(sl, t == null ? p : t, "open_book");
        }
        if (a.stormStarts > now && a.stormEnds > now) AuthorFx.above(sl, p, "mind_ripple");
        if (a.sea != null) AuthorFx.maze(sl, a.sea, 12, now < a.seaStarts);
        if (a.discern > now) for (var entry : AuthorMind.STATES.entrySet()) {
            var state = entry.getValue();
            var caster = sl.getServer().getPlayerList().getPlayer(entry.getKey());
            if (!state.dimension.equals(sl.dimension()) || !Author.eligible(p, caster, 24, true)) continue;
            if (state.pending.stream().anyMatch(d -> p.getUUID().equals(d.target()) && d.due() - now <= 10))
                AuthorFx.above(sl, p, "terror_eye");
        }
    }

    private static void dragon(ServerLevel sl, LivingEntity body, String model, float scale) {
        ParticleShapes.modelOnTurning(sl, AuthorFx.model(model, 10).pivot(8, 0, 8).scale(scale)
                .frames(4).key(0, ModelFx.pose().frame(0)).key(3, ModelFx.pose().frame(1))
                .key(6, ModelFx.pose().frame(2)).key(9, ModelFx.pose().frame(3)).smooth(), body, Vec3.ZERO, 180, 0, 0);
    }

    private static void tickCreatures(ServerPlayer p, State a, long now) {
        LivingEntity puppet = AuthorMind.entity(p.level(), a.puppet);
        if (a.puppet != null && (puppet == null || a.puppetUntil <= now || p.distanceToSqr(puppet) > 1024)) {
            dismiss(p, a.puppet, false); a.puppet = null;
        }
        LivingEntity drake = AuthorMind.entity(p.level(), a.summon);
        if (a.summon == null) return;
        if (drake == null || a.summonUntil <= now || p.distanceToSqr(drake) > 1024) {
            dismiss(p, a.summon, true); a.summon = null; return;
        }
        if (now % 5 == 0) dragon(p.level(), drake, "imagined_drake", .7f);
        LivingEntity target = enemies(p, drake.position(), 16).stream().findFirst().orElse(null);
        if (target == null) return;
        Vec3 direction = target.position().subtract(drake.position());
        if (direction.lengthSqr() > 4) drake.setDeltaMovement(direction.normalize().scale(.22));
        else if (now >= a.summonAttack) {
            a.summonAttack = now + 30; strikeMental(p, target, 14);
        }
    }

    private static boolean direct(LivingHurtEvent e) {
        return mentalDepth == 0 && !Spells.isSpellHit(e.getEntity()) && e.getSource().getEntity() instanceof LivingEntity
                && e.getSource().getDirectEntity() == e.getSource().getEntity();
    }

    static void hurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity(); long now = victim.level().getGameTime();
        if (mentalDepth > 0 && victim instanceof Player p && Author.own(p) && SoulCore.getSequence(p) <= 4)
            e.setAmount(e.getAmount() * .85f);
        float cut = 0;
        for (var entry : AuthorMind.STATES.entrySet()) {
            ServerPlayer owner = victim.level() instanceof ServerLevel sl ? sl.getServer().getPlayerList().getPlayer(entry.getKey()) : null;
            var a = entry.getValue().advanced;
            if (owner != null && Author.own(owner) && owner.level() == victim.level() && a.anchor != null && a.anchorUntil > now
                    && Spells.isFriend(owner, victim) && victim.position().distanceToSqr(a.anchor) <= 36
                    && Spells.clearLine(owner, owner.level(), a.anchor.add(0, .5, 0), victim.getEyePosition())) cut = .2f;
        }
        if (cut > 0) e.setAmount(e.getAmount() * (1 - cut));
        if (victim instanceof ServerPlayer p && Author.own(p)) {
            var s = AuthorMind.STATES.get(p.getUUID());
            if (s != null) {
                var a = s.advanced;
                if (e.getSource().getEntity() instanceof LivingEntity attacker) {
                    var mark = s.marks.get("manipulate" + attacker.getUUID());
                    if (mark != null && mark.until() > now && Author.eligible(p, attacker, 64, true)) e.setAmount(e.getAmount() * .8f);
                }
                if (a.script != null && a.script.kind.equals("plot_twist") && a.script.until > now && direct(e)
                        && e.getSource().getEntity() instanceof LivingEntity attacker && Author.eligible(p, attacker, 64, true)) {
                    a.script = null; e.setAmount(e.getAmount() * .5f);
                    a.delayed.add(new Delayed("escape", null, now + 1, p.position().subtract(Spells.flatLook(p).scale(6))));
                    AuthorFx.sound(p, "script_trigger");
                }
            }
        }
        if (!direct(e) || !(e.getSource().getEntity() instanceof ServerPlayer p) || !Author.own(p)
                || !Author.eligible(p, victim, 8, true)) return;
        var s = AuthorMind.STATES.get(p.getUUID()); if (s == null) return;
        var a = s.advanced;
        if (a.meleeReady > now) return;
        boolean blade = a.bladeUntil > now, dragon = Form.isIn(p, DRAGON);
        if (!blade && !dragon) return;
        a.meleeReady = now + 10; e.setAmount(e.getAmount() + (blade ? 20 : 0) + (dragon ? 12 : 0));
        if (blade && ++a.bladeHits % 3 == 0) a.delayed.add(new Delayed("slash", null, now + 1, null));
    }

    static void damaged(LivingDamageEvent e) {
        LivingEntity victim = e.getEntity(); long now = victim.level().getGameTime();
        for (var entry : AuthorMind.STATES.entrySet()) {
            var a = entry.getValue().advanced;
            if (victim.getUUID().equals(a.puppet)) {
                ServerPlayer owner = ((ServerLevel)victim.level()).getServer().getPlayerList().getPlayer(entry.getKey());
                if (owner != null) dismiss(owner, a.puppet, false);
                a.puppet = null; AuthorMind.resolve(victim, 120);
            }
        }
        if (mentalDepth > 0 || Spells.isSpellHit(e.getEntity()) || !(e.getSource().getEntity() instanceof LivingEntity attacker)
                || e.getSource().getDirectEntity() != attacker) return;
        if (victim instanceof ServerPlayer p && Author.own(p)) {
            var s = AuthorMind.STATES.get(p.getUUID());
            if (s != null && s.advanced.script != null) {
                Script script = s.advanced.script;
                if (script.kind.equals("foreshadow") && script.until > now && attacker.getUUID().equals(script.target)
                        && Author.eligible(p, attacker, 24, true)) {
                    s.advanced.script = null;
                    s.advanced.delayed.add(new Delayed("counter", attacker.getUUID(), now + 10, null));
                    AuthorFx.above(p.level(), attacker, "quill"); AuthorFx.sound(p, "warning");
                }
            }
        }
        if (attacker instanceof ServerPlayer p && Author.own(p)) {
            var s = AuthorMind.STATES.get(p.getUUID());
            if (s != null && s.advanced.script != null) {
                Script script = s.advanced.script;
                if (script.kind.equals("fate_script") && script.until > now && victim.getUUID().equals(script.target)
                        && Author.eligible(p, victim, 24, true)) {
                    s.advanced.script = null; strikeMental(p, victim, 50); refund(p, victim); AuthorFx.sound(p, "script_trigger");
                }
            }
        }
    }

    private static void refund(ServerPlayer p, LivingEntity target) {
        var s = AuthorMind.STATES.get(p.getUUID());
        if (s != null && s.insight.containsKey(target.getUUID())) AuthorMind.add(p, target, 2);
    }

    static boolean cleanse(ServerPlayer p, AuthorMind.State owner, LivingEntity target) {
        State a = owner.advanced; boolean changed = false;
        if (a.script != null && target.getUUID().equals(a.script.target)) { a.script = null; changed = true; }
        if (target.getUUID().equals(a.puppet)) { dismiss(p, a.puppet, false); a.puppet = null; changed = true; }
        if (target.getUUID().equals(a.summon) && !target.isAlive()) a.summon = null;
        if (a.delayed.removeIf(d -> target.getUUID().equals(d.target))) changed = true;
        return changed;
    }

    private static void dismiss(ServerPlayer p, UUID id, boolean summon) {
        if (id == null) return;
        for (ServerLevel sl : p.level().getServer().getAllLevels()) {
            if (sl.getEntity(id) instanceof Mob mob && Marionette.isMarionetteOf(mob, p)) {
                Marionette.release(mob);
                if (summon && !mob.isRemoved()) mob.discard();
                else if (!summon && mob.isAlive()) AuthorMind.resolve(mob, 120);
                return;
            }
        }
    }

    static void clear(ServerPlayer p, AuthorMind.State owner) {
        State a = owner.advanced;
        dismiss(p, a.puppet, false); dismiss(p, a.summon, true);
        if (Form.isIn(p, DRAGON)) Form.end(p);
    }
}
