package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Craft;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Status;
import net.lukario.circleofinevitability.combat.Countdown;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Enclosure;
import net.lukario.circleofinevitability.combat.FollowerSummon;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Territory;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Key 1, the Territory wheel (docs/pathways/observer/OBSERVER.md, "Key 1: Territory"): Totem Territory and Borrowed
 * Power (Shaman 9), Ink Servants and Painting World (Pixie 5).
 */
final class ObserverTerritory {

    private ObserverTerritory() {}

    // =========================
    // Totem Territory (Shaman 9)
    // Stand still for the ritual (RITUAL_SECONDS; moving more than RITUAL_MOVE or being hit breaks it and nothing is
    // spent): a totem rises and your Territory opens round it (combat/Territory) until you plant another, die or log
    // out. Inside it you cast cheaper and your spirituality comes back faster (Observer's static block), and every
    // enemy that walks in is announced to you and glows for you.
    // =========================
    static final int TOTEM_SEQUENCE = Observer.SHAMAN;
    static final int TOTEM_COST = 250;
    static final int RITUAL_SECONDS = 3;
    static final double RITUAL_MOVE = 2.0;
    static final double RADIUS = 16.0;                      // Shaman
    static final double RADIUS_PAINTER = 20.0;              // Painter (7)
    static final double RADIUS_VISITOR = 28.0;              // Visitor (4)
    static final double RADIUS_SHADOW = 40.0;               // Dimensional Shadow (2): enemies inside cannot hide either
    static final double RADIUS_EYE = 64.0;                  // Sublunary Eye (0)
    static final double HEIGHT = 6.0;
    static final int SHELL_SEQUENCE = Observer.LITERATURE_ENTHUSIAST;   // a shell of light that stops enemy shots
    static final int PULSE_EVERY = 100;
    static final String SHELL = "observer_territory_shell";

    static final String RITUAL = "observer_ritual";
    static final String RITUAL_BROKEN = "observer_ritual_broken";

    // =========================
    // Borrowed Power (Shaman 9)
    // What you look at (BORROW_LOOK) decides the craft; nothing in view: the open sky (Gale), or the ground under
    // you indoors (Earth Spike). Inside your Territory every craft is BORROW_BOOST stronger.
    // =========================
    static final int BORROW_SEQUENCE = Observer.SHAMAN;
    static final int BORROW_COST = 250;
    static final double BORROW_LOOK = 12.0;
    static final float BORROW_BOOST = 1.5f;
    static final double GALE_LENGTH = 12.0;
    static final double GALE_ANGLE = 35.0;
    static final float GALE_DAMAGE = 6f;
    static final double SPIKE_LENGTH = 14.0;
    static final float SPIKE_DAMAGE = 14f;
    static final int SPIKE_ROOT = 30;
    static final double LASH_RANGE = 20.0;
    static final float LASH_DAMAGE = 12f;
    static final int LASH_SLOW = 80;
    static final double SNARE_RANGE = 20.0;
    static final double SNARE_RADIUS = 4.0;
    static final int SNARE_TICKS = 50;
    static final float SNARE_DAMAGE = 2f;
    static final int BEAST_TICKS = 300;

    // =========================
    // Ink Servants (Pixie 5)
    // Three servants circle you for SERVANT_TICKS; each strikes the nearest enemy within SERVANT_RANGE every
    // SERVANT_EVERY for SERVANT_DAMAGE, twice as far inside your Territory or Painting World. Casting again replaces them.
    // =========================
    static final int SERVANT_SEQUENCE = Observer.PIXIE;
    static final int SERVANT_COST = 2000;
    static final int SERVANT_TICKS = 1200;
    static final double SERVANT_RANGE = 6.0;
    static final int SERVANT_EVERY = 30;
    static final float SERVANT_DAMAGE = 8f;
    static final String SERVANT = "observer_ink_servant_";              // + 0, 1, 2 (combat/FollowerSummon names)
    static final String SERVANTS_UNTIL = "observer_servants_until";

    // =========================
    // Painting World (Pixie 5)
    // A canvas opens where you aim (WORLD_AIM) and a pocket world of WORLD_RADIUS round it stands for WORLD_TICKS
    // (combat/Enclosure): enemies inside cannot walk out (they come back in at the other side) and cannot teleport.
    // Paintings are taken at once inside it. Cast again from inside: it ends (free). Complete from Dimensional
    // Shadow (2): WORLD_RADIUS_SHADOW, WORLD_TICKS_SHADOW and Rules against the enemy (no passage, no hiding: Decree);
    // from Observer (1): WORLD_RADIUS_OBSERVER, WORLD_TICKS_OBSERVER.
    // =========================
    static final int WORLD_SEQUENCE = Observer.PIXIE;
    static final int WORLD_COST = 3000;
    static final double WORLD_AIM = 16.0;
    static final double WORLD_RADIUS = 10.0;
    static final int WORLD_TICKS = 300;
    static final double WORLD_RADIUS_SHADOW = 28.0;
    static final int WORLD_TICKS_SHADOW = 900;
    static final double WORLD_RADIUS_OBSERVER = 36.0;
    static final int WORLD_TICKS_OBSERVER = 1200;
    static final String WORLD = "observer_painting_world";              // the Enclosure's and the Decree's name

    /** Where each owner's Painting World stands. */
    private record World(Vec3 center, double radius, double height, String tag) {}

    private static final java.util.Map<java.util.UUID, World> WORLDS = new java.util.HashMap<>();

    // =========================
    // Totem Territory
    // =========================

    static AbilityWheel.Result totemTerritory(ServerPlayer player, ServerLevel sl) {
        if (Countdown.isRunning(player) && player.getPersistentData().getBooleanOr(RITUAL, false)) {
            Spells.tell(player, Component.literal("The ritual is already under way."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 start = player.position();
        String tag = ObserverFx.tagFor("totem_ritual");
        player.getPersistentData().putBoolean(RITUAL, true);
        player.getPersistentData().remove(RITUAL_BROKEN);
        ObserverFx.totemRitual(sl, player, RITUAL_SECONDS * 20, tag);
        Countdown.seconds(RITUAL_SECONDS)
                .colors(0xB8E07A, 0x6A9A3A)
                .keepWhile(() -> player.isAlive() && player.position().distanceToSqr(start) <= RITUAL_MOVE * RITUAL_MOVE
                        && !player.getPersistentData().getBooleanOr(RITUAL_BROKEN, false))
                .onFinish(() -> {
                    player.getPersistentData().remove(RITUAL);
                    if (!Observer.pay(player, TOTEM_COST, "totem_territory")) {     // nothing left to pay with
                        ObserverFx.totemRitualCancel(sl, player, tag);
                        return;
                    }
                    plant(player, sl, Spells.groundAt(player, sl, player.position()));
                })
                .onCancel(() -> {
                    player.getPersistentData().remove(RITUAL);
                    ObserverFx.totemRitualCancel(sl, player, tag);
                    Spells.tell(player, Component.literal("The ritual is broken; nothing is spent."));
                })
                .start(player);
        return AbilityWheel.Result.FREE;                                         // paid when the totem rises
    }

    /** How far this owner's Territory reaches. */
    static double radius(LivingEntity player) {
        if (Observer.grown(player, Observer.SUBLUNARY_EYE)) return RADIUS_EYE;
        if (Observer.grown(player, Observer.DIMENSIONAL_SHADOW)) return RADIUS_SHADOW;
        if (Observer.grown(player, Observer.VISITOR)) return RADIUS_VISITOR;
        if (Observer.grown(player, Observer.PAINTER)) return RADIUS_PAINTER;
        return RADIUS;
    }

    /** The totem rises at 'ground' and the Territory opens (a Beyonder mob could plant one this way too). */
    static void plant(LivingEntity player, ServerLevel sl, Vec3 ground) {
        double radius = radius(player);
        String tag = ObserverFx.tagFor("territory");
        boolean shell = Observer.grown(player, SHELL_SEQUENCE);
        ObserverFx.totem(sl, ground, radius, ObserverFx.FOREVER, tag);
        if (shell) ObserverFx.territoryShell(sl, ground, radius, ObserverFx.FOREVER, tag);
        ObserverFx.statusForever(player, Status.TERRITORY);
        Territory.at(ground, radius)
                .height(HEIGHT)
                .onEnter(enemy -> {
                    if (player instanceof ServerPlayer owner) ObserverFx.intruder(owner, enemy);
                })
                .onPulse(open -> {
                    if (open % PULSE_EVERY == 0) ObserverFx.territoryPulse(sl, ground, radius);
                    if (open % ObserverFx.FOREVER == 0) {                         // the looks last an hour: show them again
                        ObserverFx.totem(sl, ground, radius, ObserverFx.FOREVER, tag);
                        if (shell) ObserverFx.territoryShell(sl, ground, radius, ObserverFx.FOREVER, tag);
                    }
                    if (Observer.grown(player, Observer.DIMENSIONAL_SHADOW)) {
                        for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground, radius)) Exposed.apply(enemy, 30);
                    }
                })
                .onEnd(() -> {
                    ObserverFx.territoryEnd(sl, ground, tag);
                    ObserverFx.statusEnd(player, Status.TERRITORY);
                    Dome.close(player, SHELL);
                })
                .open(player, sl);
        if (shell) {
            Dome.at(ground, radius)
                    .lasts(Integer.MAX_VALUE / 4)
                    .stopsProjectiles()
                    .onBlock(at -> ObserverFx.territoryShellBlock(sl, at))
                    .keepWhile(() -> Territory.isOpen(player))
                    .raise(player, sl, SHELL);
        }
    }

    /** A blow on a Shaman in the ritual breaks it. */
    static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && player.getPersistentData().getBooleanOr(RITUAL, false)
                && event.getSource().getEntity() != null && event.getSource().getEntity() != player) {
            player.getPersistentData().putBoolean(RITUAL_BROKEN, true);
        }
    }

    // =========================
    // Borrowed Power
    // =========================

    static AbilityWheel.Result borrowedPower(ServerPlayer player, ServerLevel sl) {
        boolean boosted = Territory.inside(player, player);
        float boost = boosted ? BORROW_BOOST : 1f;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        // a creature in view (not a boss, not a player): Beast Call
        LivingEntity creature = lookedAt(player, sl, BORROW_LOOK);
        if (creature instanceof Mob beast && !(creature instanceof Player)) {
            if (Spells.isBoss(beast) || Marionette.isMarionette(beast)) {
                Spells.tell(player, Component.literal("That one will not answer the call."));
                return AbilityWheel.Result.REFUSED;
            }
            int ticks = Math.round(BEAST_TICKS * boost);
            if (!Marionette.control(beast, player, ticks)) {
                Spells.tell(player, Component.literal("That one will not answer the call."));
                return AbilityWheel.Result.REFUSED;
            }
            String tag = ObserverFx.tagFor("beast_call");
            ObserverFx.borrow(sl, player, beast.getBoundingBox().getCenter(), Craft.BEAST_CALL, boosted);
            ObserverFx.beastCall(sl, player, beast, ticks, tag);
            Later.run(sl, ticks, () -> ObserverFx.beastCallEnd(sl, beast, tag));
            return AbilityWheel.Result.DONE;
        }

        BlockHitResult hit = sl.clip(new ClipContext(eye, eye.add(look.scale(BORROW_LOOK)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY, player));
        Craft craft;
        Vec3 source;
        if (hit.getType() == HitResult.Type.MISS) {
            boolean sky = sl.canSeeSky(player.blockPosition().above());
            craft = sky ? Craft.GALE : Craft.EARTH_SPIKE;
            source = sky ? eye.add(look.scale(6)) : player.position();
        } else {
            craft = craftOf(sl.getBlockState(hit.getBlockPos()));
            source = hit.getLocation();
        }
        ObserverFx.borrow(sl, player, source, craft, boosted);
        return switch (craft) {
            case GALE -> gale(player, sl, boost);
            case EARTH_SPIKE -> earthSpike(player, sl, boost);
            case WATER_LASH -> waterLash(player, sl, boost);
            case THORN_SNARE -> thornSnare(player, sl, boost);
            case BEAST_CALL -> AbilityWheel.Result.REFUSED;                       // handled above
        };
    }

    /** What a block lends: water, ice and snow; leaves, wood, grass and crops; everything else is earth. */
    static Craft craftOf(BlockState state) {
        if (state.getFluidState().is(FluidTags.WATER) || state.is(BlockTags.ICE) || state.is(BlockTags.SNOW)
                || state.is(Blocks.POWDER_SNOW)) return Craft.WATER_LASH;
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.CROPS) || state.is(BlockTags.FLOWERS)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.MOSS_BLOCK))
            return Craft.THORN_SNARE;
        return Craft.EARTH_SPIKE;
    }

    /** The living thing nearest the crosshair's line within 'range', not behind a block (any kind), or null. */
    static LivingEntity lookedAt(LivingEntity player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = Spells.aimPoint(player, sl, range);
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        AABB area = new AABB(eye, end).inflate(1.0);
        for (LivingEntity one : sl.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && !e.isSpectator())) {
            AABB body = one.getBoundingBox().inflate(0.3);
            if (body.clip(eye, end).isEmpty() && !body.contains(eye)) continue;
            double d = one.distanceToSqr(eye);
            if (d < nearest) {
                nearest = d;
                best = one;
            }
        }
        return best;
    }

    private static AbilityWheel.Result gale(ServerPlayer player, ServerLevel sl, float boost) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        ObserverFx.gale(sl, player, GALE_LENGTH);
        for (LivingEntity enemy : Spells.enemiesInCone(player, sl, eye, look, GALE_LENGTH, GALE_ANGLE)) {
            Spells.strike(player, enemy, GALE_DAMAGE * boost);
            Spells.push(enemy, player.position(), 1.2 * boost, 0.35);
        }
        for (Projectile shot : Spells.hostileShotsAround(player, sl, eye.add(look.scale(GALE_LENGTH / 2)), GALE_LENGTH / 2 + 1)) {
            double speed = Math.max(0.6, shot.getDeltaMovement().length());
            shot.setDeltaMovement(look.scale(speed));
            shot.hurtMarked = true;
        }
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result earthSpike(ServerPlayer player, ServerLevel sl, float boost) {
        Vec3 from = player.position();
        Vec3 direction = Spells.flatLook(player);
        Vec3 to = from.add(direction.scale(SPIKE_LENGTH));
        ObserverFx.earthSpikes(sl, from, direction, SPIKE_LENGTH);
        for (LivingEntity enemy : Spells.enemiesOnLine(player, sl, from.add(0, 0.5, 0), to.add(0, 0.5, 0), 1.0)) {
            int delay = (int) Math.round(enemy.position().distanceTo(from)) * ObserverFx.EARTH_SPIKE_STEP;
            Later.run(sl, delay, () -> {
                if (!Spells.casterStillHere(player, sl) || !enemy.isAlive()) return;
                Spells.strike(player, enemy, SPIKE_DAMAGE * boost);
                Root.apply(enemy, Math.round(SPIKE_ROOT * boost), RootRestriction.MOVE);
            });
        }
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result waterLash(ServerPlayer player, ServerLevel sl, float boost) {
        LivingEntity target = Spells.aimEnemy(player, sl, LASH_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("The water asks: at whom?"));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 to = target.getBoundingBox().getCenter();
        ObserverFx.waterLash(sl, player, to);
        Vec3 from = player.getEyePosition();
        for (LivingEntity one : Spells.livingAround(player, sl, from.add(to).scale(0.5), from.distanceTo(to) / 2 + 1.5)) {
            one.clearFire();                                                     // puts out fire on everyone it passes
        }
        player.clearFire();
        Spells.strike(player, target, LASH_DAMAGE * boost);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.round(LASH_SLOW * boost), 1));
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result thornSnare(ServerPlayer player, ServerLevel sl, float boost) {
        Vec3 center = Spells.aimGround(player, sl, SNARE_RANGE);
        int ticks = Math.round(SNARE_TICKS * boost);
        String tag = ObserverFx.tagFor("thorn_snare");
        ObserverFx.thornSnare(sl, center, SNARE_RADIUS, ticks, tag);
        Spells.growPlants(player, sl, center, SNARE_RADIUS, 6);
        Zone.at(center, SNARE_RADIUS)
                .height(3)
                .lasts(ticks)
                .every(20)
                .onEnter(enemy -> Root.apply(enemy, ticks, RootRestriction.MOVE))
                .onInside((enemy, inside) -> Spells.strike(player, enemy, SNARE_DAMAGE * boost))
                .onEnd(() -> ObserverFx.thornSnareEnd(sl, center, tag))
                .open(player, sl, "observer_thorn_snare");
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Ink Servants
    // =========================

    static AbilityWheel.Result inkServants(ServerPlayer player, ServerLevel sl) {
        for (int i = 0; i < 3; i++) {
            FollowerSummon.hover(ObserverFx.INK_SERVANT, ObserverFx.servantOffset(i))
                    .strikes(SERVANT_RANGE * 2, SERVANT_EVERY, SERVANT_DAMAGE)
                    .lasts(SERVANT_TICKS)
                    .targets((owner, target) -> target.distanceTo(owner) <= SERVANT_RANGE * (boosted(owner) ? 2 : 1))
                    .onStrike((owner, target, from) -> ObserverFx.servantStrike(sl, from, target))
                    .summon(player, SERVANT + i);
        }
        player.getPersistentData().putLong(SERVANTS_UNTIL, sl.getGameTime() + SERVANT_TICKS);
        ObserverFx.inkServantsArrive(sl, player);
        ObserverFx.status(player, Status.INK_SERVANTS, SERVANT_TICKS);
        return AbilityWheel.Result.DONE;
    }

    /** Inside its owner's Territory or Painting World: the servants reach twice as far. */
    private static boolean boosted(LivingEntity owner) {
        return Territory.isOpen(owner) && Territory.inside(owner, owner) || inWorld(owner, owner);
    }

    /** Once a second: servants whose time is up dissolve. */
    static void servantsTick(ServerPlayer player, ServerLevel sl) {
        if (!player.getPersistentData().contains(SERVANTS_UNTIL)) return;
        if (Observer.isActive(player, SERVANTS_UNTIL) && FollowerSummon.isActive(player, SERVANT + 0)) return;
        player.getPersistentData().remove(SERVANTS_UNTIL);
        for (int i = 0; i < 3; i++) FollowerSummon.dismiss(player, SERVANT + i);
        ObserverFx.inkServantsLeave(sl, player);
        ObserverFx.statusEnd(player, Status.INK_SERVANTS);
    }

    // =========================
    // Painting World
    // =========================

    /** True while 'entity' is inside 'owner's Painting World. */
    static boolean inWorld(LivingEntity owner, Entity entity) {
        World world = WORLDS.get(owner.getUUID());
        if (world == null || !Enclosure.isOpen(owner, WORLD) || entity.level() != owner.level()) return false;
        Vec3 at = entity.position();
        double dx = at.x - world.center().x, dz = at.z - world.center().z;
        return dx * dx + dz * dz <= world.radius() * world.radius() && at.y >= world.center().y - 2
                && at.y <= world.center().y + world.height();
    }

    static AbilityWheel.Result paintingWorld(ServerPlayer player, ServerLevel sl) {
        if (inWorld(player, player)) {
            closeWorld(player);
            return AbilityWheel.Result.FREE;
        }
        boolean complete = Observer.grown(player, Observer.DIMENSIONAL_SHADOW);
        boolean observer = Observer.grown(player, Observer.OBSERVER);
        double radius = observer ? WORLD_RADIUS_OBSERVER : complete ? WORLD_RADIUS_SHADOW : WORLD_RADIUS;
        int ticks = observer ? WORLD_TICKS_OBSERVER : complete ? WORLD_TICKS_SHADOW : WORLD_TICKS;
        double height = complete ? Math.min(radius, 24) : Math.min(radius * 0.8, 10);
        Vec3 center = Spells.groundAt(player, sl, Spells.aimPoint(player, sl, WORLD_AIM));
        closeWorld(player);
        String tag = ObserverFx.tagFor("painting_world");
        WORLDS.put(player.getUUID(), new World(center, radius, height, tag));
        ObserverFx.paintingWorld(sl, center, radius, ticks, complete, tag);
        Enclosure.at(center, radius)
                .height(height)
                .lasts(ticks)
                .keeps(one -> Spells.isEnemy(player, one))
                .every(10)
                .onInside(enemy -> {
                    Root.apply(enemy, 15, RootRestriction.TELEPORT);
                    ObserverFx.status(enemy, Status.TRAPPED, 25);
                })
                .onLoop((enemy, from, to) -> ObserverFx.worldBarrierHit(sl, from))
                .onEnd(() -> {
                    World world = WORLDS.get(player.getUUID());
                    if (world != null && world.tag().equals(tag)) WORLDS.remove(player.getUUID());
                    ObserverFx.paintingWorldEnd(sl, center, radius, tag);
                    Decree.close(player, WORLD);
                })
                .open(player, sl, WORLD);
        if (complete) {
            Decree.at(center, radius)
                    .height(height)
                    .lasts(ticks)
                    .forbid(Decree.Rule.PASSAGE, Decree.Rule.CONCEALMENT)
                    .binds(one -> Spells.isEnemy(player, one))
                    .write(player, sl, WORLD);
        }
        return AbilityWheel.Result.DONE;
    }

    static void closeWorld(LivingEntity player) {
        if (Enclosure.isOpen(player, WORLD)) Enclosure.close(player, WORLD);
        if (Decree.isWritten(player, WORLD)) Decree.close(player, WORLD);
        WORLDS.remove(player.getUUID());
    }

    static void clearState(Player player) {
        player.getPersistentData().remove(RITUAL);
        player.getPersistentData().remove(RITUAL_BROKEN);
        player.getPersistentData().remove(SERVANTS_UNTIL);
    }
}
