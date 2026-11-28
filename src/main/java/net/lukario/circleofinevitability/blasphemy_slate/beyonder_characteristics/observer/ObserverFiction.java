package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Figure;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Status;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Reel;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Chosen;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.DamageLink;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Key 4, the Fiction wheel (docs/pathways/observer/OBSERVER.md, "Key 4: Fiction"): Painted Mark and Hide in Painting
 * (Painter 7), Painted Face, Manifest Knight and Manifest Archer (Literature Enthusiast 6), Fantasy Double (Observer 1).
 */
final class ObserverFiction {

    private ObserverFiction() {}

    // =========================
    // Painted Mark (Painter 7)
    // Paint a mark on the creature you look at (MARK_RANGE; a boss is marked half as long). Cast again while it is
    // marked to drive it (DRIVE_COST): a mob walks to where you aim or goes after the enemy you aim at; a player is
    // drawn DRIVE_PULL blocks toward where you aim. It must be within DRIVE_RANGE of you. One mark at a time (Chosen).
    // =========================
    static final int MARK_SEQUENCE = Observer.PAINTER;
    static final int MARK_COST = 1250;
    static final int DRIVE_COST = 600;
    static final double MARK_RANGE = 24.0;
    static final int MARK_TICKS = 600;
    static final double DRIVE_RANGE = 32.0;
    static final int DRIVE_TICKS = 100;                     // a driven mob keeps going this long
    static final double DRIVE_PULL = 6.0;
    static final String MARK = "observer_painted_mark";     // status/Chosen
    static final String MARK_TAG = "observer_painted_mark_tag";

    // =========================
    // Hide in Painting (Painter 7)
    // A canvas opens on the nearest wall or ground within HIDE_REACH and you step into it: hidden and untouchable
    // (Concealment), held in place (Root MOVE). HIDE_UPKEEP a second, HIDE_TICKS at most. Press again to step out (free);
    // casting or attacking ends it too. True Sight sees you.
    // =========================
    static final int HIDE_SEQUENCE = Observer.PAINTER;
    static final int HIDE_COST = 1250;
    static final int HIDE_UPKEEP = 40;
    static final int HIDE_TICKS = 1200;
    static final double HIDE_REACH = 3.0;
    static final String HIDE_UNTIL = "observer_hide_until";
    static final String HIDE_TAG = "observer_hide_tag";
    static final String HIDE_SPOT = "observer_hide_spot";
    static final String HIDE_ROOT = "observer_hide";

    // =========================
    // Painted Face (Literature Enthusiast 6)
    // For FACE_TICKS you (or the ally you aim at) are Unnoticed: mobs forget you, abilities cannot pick you. It ends when
    // the disguised one hits something (status/Unnoticed does that).
    // =========================
    static final int FACE_SEQUENCE = Observer.LITERATURE_ENTHUSIAST;
    static final int FACE_COST = 1500;
    static final int FACE_TICKS = 1200;
    static final double FACE_RANGE = 24.0;

    // =========================
    // Manifest Knight / Archer (Literature Enthusiast 6)
    // A figure of fiction (status/Construct, its body hidden under the figure), at share(...) of its stats: 50% at
    // Sequence 6, 65% at 4, 80% at 2, 100% at 0. One Knight, two Archers (a new one replaces the oldest).
    // =========================
    static final int KNIGHT_SEQUENCE = Observer.LITERATURE_ENTHUSIAST;
    static final int KNIGHT_COST = 2450;
    static final int KNIGHT_TICKS = 600;
    static final float KNIGHT_HEALTH = 80f;
    static final float KNIGHT_DAMAGE = 12f;
    static final String KNIGHTS = "observer_knight";
    static final int ARCHER_SEQUENCE = Observer.LITERATURE_ENTHUSIAST;
    static final int ARCHER_COST = 2450;
    static final int ARCHER_TICKS = 500;
    static final float ARCHER_HEALTH = 40f;
    static final float ARCHER_DAMAGE = 10f;
    static final double ARCHER_RANGE = 24.0;
    static final int ARCHER_EVERY = 30;                     // ticks between arrows
    static final double ARCHER_KEEPS = 6.0;                 // it steps back from an enemy this close
    static final String ARCHERS = "observer_archer";
    static final int ARCHER_LIMIT = 2;
    static final int ARCHER_LIMIT_SHADOW = 3;               // Dimensional Shadow (2)
    static final double LEASH = 40.0;
    static final String FIGURE_TAG = "observer_figure_tag";
    static final String FIGURE_SHOT = "observer_figure_shot";

    // =========================
    // Fantasy Double (Observer 1)
    // A fabricated self stands where you stand, with your name and gear, for DOUBLE_TICKS, and every enemy within
    // DOUBLE_TAUNT hunts it. You are hidden for DOUBLE_HIDDEN (Concealment). The connection stays: DOUBLE_SHARE of what
    // it takes is dealt to you (status/DamageLink). One at a time.
    // =========================
    static final int DOUBLE_SEQUENCE = Observer.OBSERVER;
    static final int DOUBLE_COST = 30000;
    static final int DOUBLE_TICKS = 400;
    static final int DOUBLE_HIDDEN = 200;
    static final double DOUBLE_TAUNT = 24.0;
    static final float DOUBLE_SHARE = 0.2f;
    static final String DOUBLES = "observer_fantasy_double";

    /** Who is wearing a Painted Face (to take the look away when it ends). */
    private static final Map<UUID, LivingEntity> FACES = new HashMap<>();

    static {
        Chosen.addListener(MARK, (chooser, target) -> {
            String tag = chooser.getPersistentData().getStringOr(MARK_TAG, "");
            chooser.getPersistentData().remove(MARK_TAG);
            if (target != null && target.level() instanceof ServerLevel sl && !tag.isEmpty()) ObserverFx.paintedMarkEnd(sl, target, tag);
            if (target != null) ObserverFx.statusEnd(target, Status.MARKED);
        });
    }

    // =========================
    // Painted Mark
    // =========================

    /** The mark's cost: a drive is cheaper. */
    static int markCost(Player player, int cost) {
        LivingEntity marked = Chosen.of(player, MARK);
        return marked != null && marked.distanceTo(player) <= DRIVE_RANGE ? DRIVE_COST : cost;
    }

    static AbilityWheel.Result paintedMark(ServerPlayer player, ServerLevel sl) {
        LivingEntity marked = Chosen.of(player, MARK);
        if (marked != null && marked.distanceTo(player) <= DRIVE_RANGE) return drive(player, sl, marked);
        LivingEntity target = Spells.aimEnemy(player, sl, MARK_RANGE);
        if (target == null) target = ObserverTerritory.lookedAt(player, sl, MARK_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("There is no one to paint a mark on."));
            return AbilityWheel.Result.REFUSED;
        }
        int ticks = Spells.isBoss(target) ? MARK_TICKS / 2 : MARK_TICKS;
        String tag = ObserverFx.tagFor("painted_mark");
        Chosen.choose(player, MARK, target, ticks);                              // the old mark (if any) is let go
        player.getPersistentData().putString(MARK_TAG, tag);
        ObserverFx.paintedMark(sl, player, target, ticks, tag);
        ObserverFx.status(target, Status.MARKED, ticks);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result drive(ServerPlayer player, ServerLevel sl, LivingEntity marked) {
        LivingEntity enemy = Spells.aimEnemy(player, sl, DRIVE_RANGE);
        if (marked instanceof Mob mob) {
            if (enemy != null && enemy != marked && Taunt.apply(mob, enemy, DRIVE_TICKS)) {
                ObserverFx.markDrive(sl, marked, enemy.getBoundingBox().getCenter());
                return AbilityWheel.Result.DONE;
            }
            Vec3 to = Spells.aimGround(player, sl, DRIVE_RANGE);
            ObserverFx.markDrive(sl, marked, to);
            for (int t = 0; t < DRIVE_TICKS; t += 10) {                          // its own will keeps pulling it back
                Later.run(sl, t, () -> {
                    if (!mob.isAlive() || mob.position().distanceToSqr(to) < 2.0) return;
                    mob.setTarget(null);
                    mob.getNavigation().moveTo(to.x, to.y, to.z, 1.3);
                });
            }
            return AbilityWheel.Result.DONE;
        }
        Vec3 aim = enemy != null && enemy != marked ? enemy.position() : Spells.aimPoint(player, sl, DRIVE_RANGE);
        Vec3 way = aim.subtract(marked.position());
        if (way.lengthSqr() < 0.25) return AbilityWheel.Result.REFUSED;
        Vec3 to = marked.position().add(way.normalize().scale(Math.min(DRIVE_PULL, way.length())));
        ObserverFx.markDrive(sl, marked, to);
        Reel.in(marked, () -> to).lasts(20).start(sl);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Hide in Painting
    // =========================

    static boolean isHidden(Player player) {
        return Observer.isActive(player, HIDE_UNTIL);
    }

    static AbilityWheel.Result hideInPainting(ServerPlayer player, ServerLevel sl) {
        if (isHidden(player)) {
            stepOut(player, sl);
            return AbilityWheel.Result.FREE;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        BlockHitResult hit = sl.clip(new ClipContext(eye, eye.add(look.scale(HIDE_REACH)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 spot;
        Vec3 facing;
        if (hit.getType() != HitResult.Type.MISS && hit.getDirection().getAxis().isHorizontal()) {
            Direction face = hit.getDirection();
            facing = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            spot = hit.getLocation().add(facing.scale(0.06));
        } else {
            Vec3 ground = Spells.groundAt(player, sl, player.position());
            if (ground.distanceTo(player.position()) > HIDE_REACH) {
                Spells.tell(player, Component.literal("There is no wall or ground near enough to paint a door on."));
                return AbilityWheel.Result.REFUSED;
            }
            facing = new Vec3(0, 1, 0);
            spot = ground.add(0, 0.05, 0);
        }
        String tag = ObserverFx.tagFor("hide_in_painting");
        player.getPersistentData().putLong(HIDE_UNTIL, sl.getGameTime() + HIDE_TICKS);
        player.getPersistentData().putString(HIDE_TAG, tag);
        player.getPersistentData().putString(HIDE_SPOT, spot.x + "," + spot.y + "," + spot.z + "," + facing.x + "," + facing.y + "," + facing.z);
        Concealment.hide(player, HIDE_TICKS);
        Root.applyOwned(player, HIDE_ROOT, HIDE_TICKS, Set.of(RootRestriction.MOVE));
        ObserverFx.hideInPainting(sl, player, spot, facing, HIDE_TICKS, tag);
        ObserverFx.status(player, Status.HIDDEN, HIDE_TICKS);
        return AbilityWheel.Result.DONE;
    }

    /** Out of the picture: the canvas fades. */
    static void stepOut(Player player, ServerLevel sl) {
        String tag = player.getPersistentData().getStringOr(HIDE_TAG, "");
        String[] at = player.getPersistentData().getStringOr(HIDE_SPOT, "").split(",");
        player.getPersistentData().remove(HIDE_UNTIL);
        player.getPersistentData().remove(HIDE_TAG);
        player.getPersistentData().remove(HIDE_SPOT);
        Root.removeOwned(player, HIDE_ROOT);
        if (Concealment.isHidden(player)) Concealment.reveal(player);
        ObserverFx.statusEnd(player, Status.HIDDEN);
        if (tag.isEmpty() || at.length != 6) return;
        try {
            Vec3 spot = new Vec3(Double.parseDouble(at[0]), Double.parseDouble(at[1]), Double.parseDouble(at[2]));
            Vec3 facing = new Vec3(Double.parseDouble(at[3]), Double.parseDouble(at[4]), Double.parseDouble(at[5]));
            ObserverFx.hideInPaintingEnd(sl, player, spot, facing, tag);
        } catch (NumberFormatException ignored) {
            // a broken value: no look to end
        }
    }

    /** Any other cast ends Hide in Painting. */
    static void castWhileHidden(ServerPlayer caster, Casts.Cast cast) {
        if (!caster.getPersistentData().contains(HIDE_TAG)) return;
        if (cast.entry() != null && "hide_in_painting".equals(cast.entry().key())) return;
        if (caster.level() instanceof ServerLevel sl) stepOut(caster, sl);
    }

    /** A swing ends it too. */
    static void swing(Player player) {
        if (player.getPersistentData().contains(HIDE_TAG) && player.level() instanceof ServerLevel sl) stepOut(player, sl);
    }

    // =========================
    // Painted Face
    // =========================

    static AbilityWheel.Result paintedFace(ServerPlayer player, ServerLevel sl) {
        LivingEntity ally = Spells.aimAlly(player, sl, FACE_RANGE);
        LivingEntity who = ally != null ? ally : player;
        String tag = ObserverFx.tagFor("painted_face");
        LivingEntity old = FACES.remove(who.getUUID());
        if (old != null) ObserverFx.paintedFaceEnd(sl, old, old.getPersistentData().getStringOr(FIGURE_TAG + "_face", ""));
        who.getPersistentData().putString(FIGURE_TAG + "_face", tag);
        Unnoticed.hide(who, FACE_TICKS);
        FACES.put(who.getUUID(), who);
        ObserverFx.paintedFace(sl, who, FACE_TICKS, tag);
        ObserverFx.status(who, Status.PAINTED_FACE, FACE_TICKS);
        return AbilityWheel.Result.DONE;
    }

    /** Once a second: faces that are no longer worn (it hit something, its time is up) lose their look. */
    private static void faces() {
        if (FACES.isEmpty()) return;
        FACES.values().removeIf(who -> {
            if (who.isAlive() && !who.isRemoved() && Unnoticed.isUnnoticed(who)) return false;
            if (who.level() instanceof ServerLevel sl) {
                ObserverFx.paintedFaceEnd(sl, who, who.getPersistentData().getStringOr(FIGURE_TAG + "_face", ""));
            }
            who.getPersistentData().remove(FIGURE_TAG + "_face");
            ObserverFx.statusEnd(who, Status.PAINTED_FACE);
            return true;
        });
    }

    // =========================
    // Manifest Knight / Archer
    // =========================

    /** How much of a figure's stats it has: the Fantasy is more real the higher the Sequence. */
    static float share(LivingEntity player) {
        if (Observer.grown(player, Observer.SUBLUNARY_EYE)) return 1.0f;
        if (Observer.grown(player, Observer.DIMENSIONAL_SHADOW)) return 0.8f;
        if (Observer.grown(player, Observer.VISITOR)) return 0.65f;
        return 0.5f;
    }

    static AbilityWheel.Result manifestKnight(ServerPlayer player, ServerLevel sl) {
        float share = share(player);
        Mob knight = figure(player, sl, Figure.KNIGHT, KNIGHTS, 1, KNIGHT_TICKS, KNIGHT_HEALTH * share, KNIGHT_DAMAGE * share, 1.0f);
        return knight == null ? AbilityWheel.Result.REFUSED : AbilityWheel.Result.DONE;
    }

    static AbilityWheel.Result manifestArcher(ServerPlayer player, ServerLevel sl) {
        float share = share(player);
        int limit = Observer.grown(player, Observer.DIMENSIONAL_SHADOW) ? ARCHER_LIMIT_SHADOW : ARCHER_LIMIT;
        Mob archer = figure(player, sl, Figure.ARCHER, ARCHERS, limit, ARCHER_TICKS, ARCHER_HEALTH * share, 0f, 0.9f);
        return archer == null ? AbilityWheel.Result.REFUSED : AbilityWheel.Result.DONE;
    }

    private static Mob figure(ServerPlayer player, ServerLevel sl, Figure figure, String group, int limit, int ticks,
                              float health, float damage, float speed) {
        Vec3 behind = Spells.flatLook(player).scale(figure == Figure.KNIGHT ? 1 : -1);
        Vec3 feet = Spells.freeSpotNear(player, sl, player.getEyePosition(), behind, 2.5);
        feet = feet == null ? player.position() : Spells.groundAt(player, sl, feet);
        String tag = ObserverFx.tagFor(group);
        Mob made = Construct.of(EntityTypes.HUSK)
                .health(health).damage(damage).speed(speed).lasts(ticks).leash(LEASH)
                .limit(group, limit)
                .onEnd((construct, why) -> {
                    if (construct.level() instanceof ServerLevel level) ObserverFx.manifestFade(level, construct, tag);
                })
                .build(player, sl, feet, player.getYRot());
        if (made == null) {
            Spells.tell(player, Component.literal("There is no room for it to step out of the page."));
            return null;
        }
        made.getPersistentData().putString(FIGURE_TAG, tag);
        made.getPersistentData().putString(FIGURE_TAG + "_figure", figure.name());
        ObserverFx.manifest(sl, made, figure, share(player), ticks, tag);
        return made;
    }

    /** The Knight's blows show as its swing. */
    static void hurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob knight) || !(knight.level() instanceof ServerLevel sl)) return;
        if (!Figure.KNIGHT.name().equals(knight.getPersistentData().getStringOr(FIGURE_TAG + "_figure", ""))) return;
        ObserverFx.knightStrike(sl, knight, event.getEntity(), knight.getPersistentData().getStringOr(FIGURE_TAG, ""),
                Construct.ticksLeft(knight));
    }

    /** Every 10 ticks, for an Observer: their Archers shoot, and step back from an enemy too close. */
    static void archers(ServerPlayer player, ServerLevel sl) {
        java.util.List<Mob> archers = Construct.ofOwner(player, ARCHERS);
        if (archers.isEmpty()) return;
        java.util.List<Mob> knights = Construct.ofOwner(player, KNIGHTS);
        Mob knight = knights.isEmpty() ? null : knights.getFirst();
        float damage = ARCHER_DAMAGE * share(player);
        long now = sl.getGameTime();
        for (Mob archer : archers) {
            if (!archer.isAlive() || archer.level() != sl) continue;
            LivingEntity target = archer.getTarget();
            if (target == null || !target.isAlive() || !Spells.isEnemy(player, target) || target.distanceTo(archer) > ARCHER_RANGE) {
                target = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity enemy : Spells.enemiesAround(player, sl, archer.position(), ARCHER_RANGE)) {
                    double d = enemy.distanceToSqr(archer);
                    if (d < best && Spells.clearLine(player, sl, archer.getEyePosition(), enemy.getBoundingBox().getCenter())) {
                        best = d;
                        target = enemy;
                    }
                }
            }
            if (target == null) continue;
            // it stays out of the press, behind the Knight when there is one
            if (target.distanceTo(archer) < ARCHER_KEEPS) {
                Vec3 back = knight != null && knight.isAlive()
                        ? knight.position().add(knight.position().subtract(target.position()).normalize().scale(3))
                        : player.position();
                archer.getNavigation().moveTo(back.x, back.y, back.z, 1.2);
            }
            if (now - archer.getPersistentData().getLongOr(FIGURE_SHOT, 0L) < ARCHER_EVERY) continue;
            archer.getPersistentData().putLong(FIGURE_SHOT, now);
            LivingEntity shot = target;
            Vec3 to = shot.getBoundingBox().getCenter();
            archer.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, to);
            ObserverFx.archerShot(sl, archer, to, archer.getPersistentData().getStringOr(FIGURE_TAG, ""), Construct.ticksLeft(archer));
            int flight = 7 + (int) (archer.distanceTo(shot) / 3);
            Later.run(sl, flight, () -> {
                if (archer.isAlive() && shot.isAlive() && Spells.casterStillHere(player, sl)) Spells.strike(player, shot, damage);
            });
        }
    }

    // =========================
    // Fantasy Double
    // =========================

    private static final EquipmentSlot[] GEAR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    static AbilityWheel.Result fantasyDouble(ServerPlayer player, ServerLevel sl) {
        String tag = ObserverFx.tagFor("fantasy_double");
        Mob dbl = Construct.of(EntityTypes.HUSK)
                .health(player.getMaxHealth()).damage(4f).speed(1.0f).lasts(DOUBLE_TICKS).leash(LEASH).shown()
                .limit(DOUBLES, 1)
                .onEnd((construct, why) -> {
                    if (construct.level() instanceof ServerLevel level) ObserverFx.fantasyDoubleEnd(level, construct, tag);
                    ObserverFx.statusEnd(player, Status.FANTASY_DOUBLE);
                })
                .build(player, sl, player.position(), player.getYRot());
        if (dbl == null) {
            Spells.tell(player, Component.literal("There is no room for your double here."));
            return AbilityWheel.Result.REFUSED;
        }
        for (EquipmentSlot slot : GEAR) {
            ItemStack worn = player.getItemBySlot(slot);
            if (!worn.isEmpty()) dbl.setItemSlot(slot, worn.copy());
        }
        dbl.setCustomName(player.getName());
        dbl.setCustomNameVisible(true);
        dbl.getPersistentData().putString(FIGURE_TAG, tag);
        DamageLink.redirect(dbl, player, DOUBLE_SHARE, DOUBLE_TICKS);
        Concealment.hide(player, DOUBLE_HIDDEN);
        ObserverFx.fantasyDouble(sl, player, dbl, DOUBLE_TICKS, tag);
        ObserverFx.status(player, Status.FANTASY_DOUBLE, DOUBLE_TICKS);
        huntDouble(player, sl, dbl);
        return AbilityWheel.Result.DONE;
    }

    /** Every enemy near hunts the double. */
    private static void huntDouble(ServerPlayer player, ServerLevel sl, Mob dbl) {
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, dbl.position(), DOUBLE_TAUNT)) {
            if (enemy instanceof Mob && enemy != dbl) Taunt.apply(enemy, dbl, 30);
        }
    }

    // =========================
    // Once a second, for an Observer
    // =========================

    static void tick(ServerPlayer player, ServerLevel sl) {
        faces();
        for (Mob dbl : Construct.ofOwner(player, DOUBLES)) {
            if (!dbl.isAlive() || dbl.level() != sl) continue;
            huntDouble(player, sl, dbl);
            ObserverFx.fantasyLink(sl, dbl, player);
        }
        if (!player.getPersistentData().contains(HIDE_TAG)) return;
        boolean out = !isHidden(player) || !Concealment.isHidden(player);        // its time, or something broke it
        if (!out) {
            int due = Observer.costFor(player, HIDE_UPKEEP, "hide_in_painting");
            out = !Spells.paySpirituality(player, due);
        }
        if (out) stepOut(player, sl);
    }

    static void clearState(Player player) {
        player.getPersistentData().remove(HIDE_UNTIL);
        player.getPersistentData().remove(HIDE_TAG);
        player.getPersistentData().remove(HIDE_SPOT);
        Root.removeOwned(player, HIDE_ROOT);
        player.getPersistentData().remove(MARK_TAG);
    }
}
