package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Parasite;
import net.lukario.circleofinevitability.status.ParasiteControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Abomination, key 5: the Wraith wheel (Wraith, Sequence 5). docs/ABOMINATION.md, "Key 5 wheel".
 *
 *   Mirror Step   step into the nearest reflection and out of the one you aim at, unseen on the way
 *   Possession    leave your body and ride inside the mob you look at, steering it (status/Parasite); from
 *                 Disciple of Silence (3) a player can carry you too, from Ancient Bane (2) anything can.
 *                 The key again steps out
 *   Ghost Host    ghosts rise from the Spirit World and fight for you; vexes and phantoms near you are taken over
 *                 in place of some of them. Six from Ancient Bane (2), and what they touch carries Misfortune
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost).
 */
public final class AbominationWraith {

    private AbominationWraith() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int WRAITH_SEQUENCE = 5;                        // the key
    private static final int WRAITH_KEY = 5;
    private static final float QUICK_CAST_COST = 2f;

    // Mirror Step
    private static final int MIRROR_SEQUENCE = 5;
    private static final int MIRROR_COST = 3000;
    private static final int MIRROR_COOLDOWN = 200;
    private static final double MIRROR_IN = 6.0;                 // a reflection this close to step into...
    private static final double MIRROR_OUT = 32.0;               // ...and one this far to come out of
    private static final double MIRROR_AROUND = 3.0;             // how near the crosshair the far one must be
    private static final int MIRROR_TRAVEL = 20;                 // unseen on the way this long
    private static final int MIRROR_HINTS = 8;                   // a refused step shows this many reflections near you

    // Possession
    private static final int POSSESSION_SEQUENCE = 5;
    private static final int POSSESSION_COST = 4000;
    private static final int POSSESSION_COOLDOWN = 600;
    private static final double POSSESSION_RANGE = 16.0;
    private static final float POSSESSION_MAX_HEALTH = 150f;     // nothing stronger can be ridden (until Ancient Bane)
    private static final int POSSESSION_TICKS = 1200;            // at most (longer with Puppeteer)
    private static final int POSSESSION_BEAT = 20;
    private static final int POSSESSION_UPKEEP = 100;            // spirituality every beat
    private static final int POSSESSION_PLAYERS_SEQUENCE = 3;    // Disciple of Silence: players carry you (not steered)
    private static final int POSSESSION_ANY_SEQUENCE = 2;        // Ancient Bane: any mob, a boss too
    private static final int POSSESSION_KEY_REST = 20;           // the key does not step out in the first second inside
    private static final ParasiteControl.Settings STEERING = new ParasiteControl.Settings(
            40,      // idle this long and the host acts on its own again
            1.0,     // walk speed (its own)
            1.4,     // sprint speed
            3.0,     // reach of a strike
            20,      // ticks between two strikes
            3f,      // the strike of a host with no attack of its own
            24,      // shot range of a host that shoots
            30,      // ticks between two shots
            24);     // right click: how far it can be told to hunt

    // Ghost Host
    private static final int GHOSTS_SEQUENCE = 5;
    private static final int GHOSTS_COST = 3500;
    private static final int GHOSTS_COOLDOWN = 400;
    private static final int GHOSTS_COUNT = 3;
    private static final int GHOSTS_BANE_SEQUENCE = 2;           // Ancient Bane: six, and their touch carries Misfortune
    private static final int GHOSTS_BANE_COUNT = 6;
    private static final int GHOSTS_TICKS = 400;                 // they serve this long (longer with Puppeteer)
    private static final double GHOSTS_TAKE_RANGE = 24.0;        // vexes and phantoms this close are taken over
    private static final int GHOSTS_EVERY = 4;                   // ticks between two ghosts coming out

    // =========================
    // What is saved on the player
    // =========================
    private static final String POSSESSION_TAG = "abomination_possession_tag";

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("abomination_wraith", "Wraith")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .castsWhileSilenced(Abomination::castsThroughHush)
            .entry("mirror_step", AbominationFx.WHEEL_MIRROR_STEP, "Mirror Step", MIRROR_SEQUENCE,
                    MIRROR_COST, MIRROR_COOLDOWN, AbominationWraith::mirrorStep)
            .entry("possession", AbominationFx.WHEEL_POSSESSION, "Possession", POSSESSION_SEQUENCE,
                    POSSESSION_COST, POSSESSION_COOLDOWN, AbominationWraith::possession)
            .entry("ghost_host", AbominationFx.WHEEL_GHOST_HOST, "Ghost Host", GHOSTS_SEQUENCE,
                    GHOSTS_COST, GHOSTS_COOLDOWN, AbominationWraith::ghostHost);

    // =========================
    // Ability 5 - Wraith
    // Normal: the Wraith wheel. Sneak: the last pick again, without the wheel.
    // Inside a host (Possession): the key, sneaking or not, steps back out (free).
    // =========================
    public static void abominationWraith(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!Abomination.ready(player, bypassClassCheck, WRAITH_SEQUENCE)) return;
        if (isPossessing(player)) {
            if (Parasite.ticksInside(player) >= POSSESSION_KEY_REST) Parasite.leave(player, Parasite.End.LEFT);
            return;
        }
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Mirror Step: step into the nearest reflection within MIRROR_IN (still water, ice, glass, polished metal: see
    // Spells.isReflective) and come out of the one you aim at, up to MIRROR_OUT away, unseen for MIRROR_TRAVEL on the
    // way (status/Concealment). There has to be a reflection at both ends and room to stand at the far one; if not,
    // it costs nothing and the reflections near you glint (only you see them).
    // =========================
    private static AbilityWheel.Result mirrorStep(ServerPlayer player, ServerLevel sl) {
        if (Root.has(player, RootRestriction.TELEPORT)) {
            player.sendSystemMessage(Component.literal("Something holds you to this side of the glass."));
            return AbilityWheel.Result.REFUSED;
        }
        List<BlockPos> near = Spells.reflectionNear(sl, player.position(), MIRROR_IN);
        if (near.isEmpty()) {
            player.sendSystemMessage(Component.literal("There is no reflection near you to step into."));
            return AbilityWheel.Result.REFUSED;
        }
        BlockPos far = Spells.reflectionAimed(player, sl, MIRROR_OUT, MIRROR_AROUND);
        Vec3 exit = far == null ? null : stepOut(player, sl, far);
        if (far == null || exit == null || exit.distanceTo(player.position()) > MIRROR_OUT + 2) {
            player.sendSystemMessage(Component.literal(far == null
                    ? "There is no reflection where you look to step out of."
                    : "There is no room to step out of that reflection."));
            hints(player, near);
            return AbilityWheel.Result.REFUSED;
        }

        Vec3 door = doorOf(sl, near.get(0));
        Vec3 outDoor = doorOf(sl, far);
        AbominationFx.mirrorEnter(sl, player, door);
        Later.run(sl, AbominationFx.MIRROR_OPENS, () -> {
            if (!Spells.casterStillHere(player, sl) || Root.has(player, RootRestriction.TELEPORT)) return;
            Concealment.hide(player, MIRROR_TRAVEL + 5);
            FallGuard.protect(player, MIRROR_TRAVEL + 60);
            player.teleportTo(exit.x, exit.y, exit.z);
            player.fallDistance = 0;
            AbominationFx.status(player, Status.MIRROR, MIRROR_TRAVEL);
            Later.run(sl, MIRROR_TRAVEL, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                Concealment.reveal(player);
                AbominationFx.mirrorExit(sl, player, outDoor);
            });
        });
        return AbilityWheel.Result.DONE;
    }

    /** Where to stand coming out of a reflection: on it (water, ice, a metal floor), else beside it (a pane, a wall). */
    private static Vec3 stepOut(Player player, ServerLevel sl, BlockPos mirror) {
        Vec3 onTop = Vec3.atBottomCenterOf(mirror.above());
        if (Spells.fits(player, sl, onTop)) return onTop;
        List<BlockPos> sides = new ArrayList<>();
        for (Direction side : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            sides.add(mirror.relative(side));
        }
        sides.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(player.position())));   // the side facing you first
        for (BlockPos pos : sides) {
            Vec3 ground = Spells.groundNear(player, sl, Vec3.atBottomCenterOf(pos), 2);
            if (ground != null && Spells.fits(player, sl, ground)) return ground;
        }
        return null;
    }

    /** Where the ripple shows: on top of a reflection one walks on, at the foot of one that stands. */
    private static Vec3 doorOf(ServerLevel sl, BlockPos mirror) {
        boolean floor = sl.getBlockState(mirror.above()).isAir() || !sl.getFluidState(mirror).isEmpty();
        return floor ? Vec3.atBottomCenterOf(mirror.above()) : Vec3.atBottomCenterOf(mirror);
    }

    /** A refused step: the reflections near the player glint, for them alone. */
    private static void hints(ServerPlayer player, List<BlockPos> near) {
        for (int i = 0; i < Math.min(MIRROR_HINTS, near.size()); i++) {
            AbominationFx.reflectionHint(player, Vec3.atCenterOf(near.get(i)).add(0, 0.5, 0));
        }
    }

    // =========================
    // Possession: you leave your body and ride inside the mob you look at (POSSESSION_RANGE; not a boss or anything
    // with more than POSSESSION_MAX_HEALTH) for POSSESSION_TICKS at most (status/Parasite). You see through its eyes
    // and steer it (status/ParasiteControl): walk keys, jump, left click to strike, right click to pick what it
    // hunts. It costs POSSESSION_UPKEEP every second; when that cannot be paid you are cast out. The key again steps
    // out. Your ghost reaches the host at AbominationFx.GHOST_ARRIVES; you are held still until then.
    // Disciple of Silence (3): a player can be your host too (it carries you, unseen, and is not steered).
    // Ancient Bane (2): any mob can be your host, a boss included.
    // =========================
    private static AbilityWheel.Result possession(ServerPlayer player, ServerLevel sl) {
        LivingEntity host = Spells.aimEnemy(player, sl, POSSESSION_RANGE);
        String refusal = refusal(player, host);
        if (refusal != null) {
            player.sendSystemMessage(Component.literal(refusal));
            return AbilityWheel.Result.REFUSED;
        }
        AbominationFx.possessionDive(sl, player, host);
        Root.apply(player, AbominationFx.GHOST_ARRIVES, RootRestriction.EVERYTHING);
        Later.run(sl, AbominationFx.GHOST_ARRIVES, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            int ticks = Abomination.servantTicks(player, POSSESSION_TICKS);
            if (refusal(player, host) != null || !Parasite.enter(player, host, ticks, HOOKS)) {
                // it slipped away on the way: what was paid comes back
                SoulCore.setSpiritualityQuietly(player, SoulCore.getSpirituality(player) + POSSESSION_COST);
                player.sendSystemMessage(Component.literal("Your ghost found no way in and came back."));
            }
        });
        return AbilityWheel.Result.DONE;
    }

    /** Why this host cannot be ridden, as the text the player is told, or null if it can. */
    private static String refusal(Player player, LivingEntity host) {
        if (host == null) return "There is no one before you to possess.";
        if (!host.isAlive() || host.level() != player.level()) return "It is gone.";
        boolean person = host instanceof Player;
        if (person && !Beyonder.hasSequence(player, POSSESSION_PLAYERS_SEQUENCE)) return "A person's soul is still too strong for you to ride.";
        if (!person && !Beyonder.hasSequence(player, POSSESSION_ANY_SEQUENCE) && host.getMaxHealth() > POSSESSION_MAX_HEALTH) {
            return "It is too strong to be ridden.";
        }
        if (Parasite.isHost(host)) return "Something rides inside it already.";
        if (!Parasite.canEnter(player, host)) return "You cannot enter that.";
        return null;
    }

    private static boolean isPossessing(Player player) {
        return Parasite.isInside(player) && player.getPersistentData().contains(POSSESSION_TAG);
    }

    private static final Parasite.Hooks HOOKS = new Parasite.Hooks() {
        @Override
        public void entered(ServerPlayer parasite, LivingEntity host, ServerLevel sl) {
            int ticks = Abomination.servantTicks(parasite, POSSESSION_TICKS);
            String tag = "ab_possession_" + parasite.getId() + "_" + sl.getGameTime();
            parasite.getPersistentData().putString(POSSESSION_TAG, tag);
            AbominationFx.possessed(sl, host, ticks, tag);
            AbominationFx.status(parasite, Status.POSSESSION, ticks);
            boolean steers = host instanceof Mob;
            parasite.sendSystemMessage(Component.literal("You ride inside " + host.getName().getString() + "."
                    + (steers ? " Move and look to steer it, left click to strike, right click to pick what it hunts." : " It carries you, unseen.")
                    + " Press the Wraith key again to step out."));
        }

        @Override
        public void tick(ServerPlayer parasite, LivingEntity host, ServerLevel sl, int ticksInside) {
            if (ticksInside % POSSESSION_BEAT != 0) return;
            if (!Spells.paySpirituality(parasite, POSSESSION_UPKEEP)) {
                Parasite.leave(parasite, Parasite.End.OUT_OF_POWER);
                return;
            }
            AbominationFx.possessionTick(sl, host);
        }

        @Override
        public void left(ServerPlayer parasite, LivingEntity host, ServerLevel sl, Parasite.End why) {
            String tag = parasite.getPersistentData().getStringOr(POSSESSION_TAG, "");
            parasite.getPersistentData().remove(POSSESSION_TAG);
            AbominationFx.statusEnd(parasite, Status.POSSESSION);
            if (why == Parasite.End.PARASITE_GONE) return;              // died or logged out: nothing more to show
            if (host != null) AbominationFx.possessionExit(sl, host, parasite, tag);
            parasite.sendSystemMessage(Component.literal(switch (why) {
                case TIME_UP -> "Your ghost cannot hold on any longer.";
                case OUT_OF_POWER -> "Your spirituality runs dry and you are cast out.";
                case HOST_DIED -> "Your host died under you.";
                case HOST_GONE -> "Your host is gone.";
                default -> "You step out of your host.";
            }));
        }

        @Override
        public boolean allowsKey(int key) {
            return key == WRAITH_KEY;                                    // the Wraith key steps back out
        }

        @Override
        public ParasiteControl.Settings control(LivingEntity host) {
            return STEERING;
        }
    };

    // =========================
    // Ghost Host: GHOSTS_COUNT ghosts rise from the Spirit World round you (at AbominationFx.GHOSTS_RISEN) and fight
    // for you for GHOSTS_TICKS (vexes, as status/Marionette summons: they pass through walls and vanish after). Any
    // vex or phantom within GHOSTS_TAKE_RANGE that is not yours is taken over for the same time instead, in place of
    // one of them. Ancient Bane (2): GHOSTS_BANE_COUNT, and what they touch carries a level of Misfortune.
    // =========================
    private static AbilityWheel.Result ghostHost(ServerPlayer player, ServerLevel sl) {
        boolean bane = Beyonder.hasSequence(player, GHOSTS_BANE_SEQUENCE);
        int count = bane ? GHOSTS_BANE_COUNT : GHOSTS_COUNT;
        int ticks = Abomination.servantTicks(player, GHOSTS_TICKS);
        AbominationFx.ghostHost(sl, player, count);
        AbominationFx.status(player, Status.GHOST_HOST, ticks);
        Later.run(sl, AbominationFx.GHOSTS_RISEN, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            int left = count;
            List<Mob> wild = sl.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(GHOSTS_TAKE_RANGE),
                    mob -> mob.isAlive() && (mob.getType() == EntityTypes.VEX || mob.getType() == EntityTypes.PHANTOM)
                            && !Marionette.isMarionetteOf(mob, player) && mob.distanceTo(player) <= GHOSTS_TAKE_RANGE);
            wild.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
            for (Mob mob : wild) {
                if (left <= 0) break;
                if (!Marionette.control(mob, player, ticks)) continue;          // another's marionette: not taken
                AbominationFx.ghostTakenOver(sl, mob, ticks, "ab_ghost_" + mob.getId());
                if (bane) mob.getPersistentData().putBoolean(Abomination.BANE_TOUCH, true);
                left--;
            }
            for (int i = 0; i < left; i++) {
                float yaw = player.getYRot() + i * 360f / Math.max(1, left);
                Later.run(sl, i * GHOSTS_EVERY, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    Vec3 at = Spells.clearStart(player, sl, Spells.spotAround(player.position(), yaw, 1.8).add(0, 1.2, 0));
                    Mob ghost = Marionette.spawnSummon(EntityTypes.VEX, player, sl, at, yaw, ticks);
                    if (ghost == null) return;
                    AbominationFx.ghostServant(sl, ghost);
                    if (bane) ghost.getPersistentData().putBoolean(Abomination.BANE_TOUCH, true);
                    Later.run(sl, ticks - 1, () -> {
                        if (ghost.isAlive()) AbominationFx.ghostFades(sl, ghost.position());
                    });
                });
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Called from Abomination
    // =========================

    /** After a relog or a death: start clean (a possession does not survive either: status/Parasite ends it). */
    static void clearState(Player player) {
        player.getPersistentData().remove(POSSESSION_TAG);
    }
}
