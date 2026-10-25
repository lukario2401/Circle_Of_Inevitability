package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.Seal;
import net.lukario.circleofinevitability.status.Starlit;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Key Of Stars, key 6: the Secrets and Space wheel (Secrets Sorcerer, Sequence 4). docs/KEY_OF_STARS.md, "Key 6 wheel".
 *
 *   Space Concealment   a sphere of space split off from the world where you aim (or round you): nothing outside
 *                       sees in or shoots in, allies inside are hidden and healed, enemies inside are Starlit and kept
 *                       in but for one Door in the shell; cast again, the Door shuts. Wanderer: Spatial Cage (at once,
 *                       the Door where you look). Planeswalker: bigger and longer, like a Hostel
 *   Transfiguration     you are layers of illusory Doors: every hit tears one and you are 3 blocks aside, unhurt
 *   Exile               a Starlit or held enemy is swallowed by Doors into a space-time storm, and comes back hurt
 *   Pocket Space        a Door to your own Concealed space: your Ender Chest, anywhere
 *   Space Tearing (3)   space tears round an enemy: a heavy hit to all near it, then a space-time storm burns them
 *
 * Sneak casts the last pick again without the wheel. Every entry's cost goes through KeyOfStars.wheelCostFor and every
 * cooldown through KeyOfStars.cooldownFor. The looks are KeyOfStarsFx's; every number is right below.
 */
public final class KeyOfStarsSecrets {

    private KeyOfStarsSecrets() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int SECRETS_SEQUENCE = 4;                      // the key

    // Space Concealment
    static final int CONCEAL_COST = 6000;
    static final int CONCEAL_COOLDOWN = 500;
    static final double CONCEAL_RANGE = 24.0;
    static final double CONCEAL_RADIUS = 6.0;
    static final int CONCEAL_TICKS = 200;
    static final int CAGE_SEQUENCE = 3;                 // Wanderer: Spatial Cage, at once, the Door where you look
    static final int HOSTEL_SEQUENCE = 2;               // Planeswalker: this big, this long
    static final double HOSTEL_RADIUS = 10.0;
    static final int HOSTEL_TICKS = 400;
    static final double DOOR_WIDTH = 1.8;               // an enemy this close to the open Door may pass it
    static final float CONCEAL_HEAL = 1.0f;             // allies inside heal this much a second

    // Transfiguration
    static final int TRANSFIG_COST = 6000;
    static final int TRANSFIG_COOLDOWN = 800;
    static final int TRANSFIG_TICKS = 200;
    static final int LAYERS = 5;
    static final int MORE_LAYERS_SEQUENCE = 1;          // Key of Stars: this many
    static final int MORE_LAYERS = 8;
    static final double LAYER_STEP = 3.0;               // a torn layer puts you this far aside

    // Exile
    static final int EXILE_COST = 7000;
    static final int EXILE_COOLDOWN = 600;
    static final double EXILE_RANGE = 24.0;
    static final int EXILE_TICKS = 160;
    static final int EXILE_LONGER_SEQUENCE = 3;         // Wanderer: this long
    static final int EXILE_LONGER = 240;
    static final int EXILE_LONGEST_SEQUENCE = 1;        // Key of Stars: this long, and this many at once
    static final int EXILE_LONGEST = 400;
    static final int EXILE_MANY = 3;
    static final int EXILE_BOSS = 60;
    static final float EXILE_DAMAGE = 40f;

    // Pocket Space
    static final int POCKET_COST = 250;
    static final int POCKET_COOLDOWN = 60;
    static final int POCKET_OPENS = 8;                  // the menu opens when the Door has opened

    // Space Tearing
    static final int TEARING_SEQUENCE = 3;
    static final int TEARING_COST = 12000;
    static final int TEARING_COOLDOWN = 300;
    static final double TEARING_RANGE = 8.0;
    static final double TEARING_RADIUS = 3.0;
    static final int TEARING_WIDE_SEQUENCE = 2;         // Planeswalker: this far, this wide, and round you too
    static final double TEARING_WIDE_RANGE = 12.0;
    static final double TEARING_WIDE_RADIUS = 5.0;
    static final float TEARING_DAMAGE = 80f;
    static final int STORM_SECONDS = 4;
    static final float STORM_DAMAGE = 12f;              // each second

    // =========================
    // Names and what is kept
    // =========================
    static final String CONCEAL_DOME = "key_of_stars_concealment";        // combat/Dome
    static final String TRANSFIG_UNTIL = "key_of_stars_transfiguration_until";

    /** A Space Concealment standing: where, how big, where its Door is and whether it is still open. */
    private static final class Concealed {
        final Vec3 center;
        final double radius;
        final float doorYaw;
        final Vec3 door;
        final String tag;
        boolean doorOpen = true;

        Concealed(Vec3 center, double radius, float doorYaw, String tag) {
            this.center = center;
            this.radius = radius;
            this.doorYaw = doorYaw;
            this.door = center.add(Vec3.directionFromRotation(0f, doorYaw + 180f).scale(radius * 0.98));
            this.tag = tag;
        }
    }

    private static final Map<UUID, Concealed> CONCEALED = new HashMap<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("key_of_stars_secrets", "Secrets and Space")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(KeyOfStars.QUICK_CAST_COST)     // sneak: no cooldown, 3x the cost
            .cooldownBy(KeyOfStars::cooldownFor)
            .entry("space_concealment", KeyOfStarsFx.WHEEL_SPACE_CONCEALMENT, "Space Concealment", SECRETS_SEQUENCE,
                    CONCEAL_COST, CONCEAL_COOLDOWN, KeyOfStarsSecrets::spaceConcealment)
            .costBy(KeyOfStars::wheelCostFor)
            .free(KeyOfStarsSecrets::canShutDoor)                                // shutting the Door: free
            .entry("transfiguration", KeyOfStarsFx.WHEEL_TRANSFIGURATION, "Transfiguration", SECRETS_SEQUENCE,
                    TRANSFIG_COST, TRANSFIG_COOLDOWN, KeyOfStarsSecrets::transfiguration)
            .costBy(KeyOfStars::wheelCostFor)
            .notCopyable()                                                       // its layers end on this Pathway's tick
            .entry("exile", KeyOfStarsFx.WHEEL_EXILE, "Exile", SECRETS_SEQUENCE, EXILE_COST, EXILE_COOLDOWN,
                    KeyOfStarsSecrets::exile)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("pocket_space", KeyOfStarsFx.WHEEL_POCKET_SPACE, "Pocket Space", SECRETS_SEQUENCE, POCKET_COST,
                    POCKET_COOLDOWN, KeyOfStarsSecrets::pocketSpace)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("space_tearing", KeyOfStarsFx.WHEEL_SPACE_TEARING, "Space Tearing", TEARING_SEQUENCE, TEARING_COST,
                    TEARING_COOLDOWN, KeyOfStarsSecrets::spaceTearing)
            .costBy(KeyOfStars::wheelCostFor);

    // =========================
    // Ability 6 - Secrets and Space
    // Normal: the Secrets wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void keyOfStarsSecrets(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KeyOfStars.ready(player, bypassClassCheck, SECRETS_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    /** A relog or a respawn: what was standing is gone. */
    static void clearState(Player player) {
        if (player.level() instanceof ServerLevel sl) endAll(player, sl);
    }

    /** Every tick for a Key Of Stars. */
    static void tick(Player player, ServerLevel sl) {
        if (player.tickCount % 10 == 0) concealmentTick(player, sl);
        transfigurationTick(player, sl);
    }

    // =========================
    // Space Concealment: aimed at a spot within CONCEAL_RANGE (or at nothing: round you), a half sphere of
    // CONCEAL_RADIUS is split off from the world for CONCEAL_TICKS (combat/Dome): enemies inside cannot leave and
    // projectiles from outside stop at the shell. Inside, you and your allies are hidden (status/Concealment) and heal
    // CONCEAL_HEAL a second; the enemies caught in it are Starlit. One Door stands in the shell on the side facing you:
    // an enemy at it may pass. Cast again while it stands and the Door is open: the Door shuts for the rest of the time,
    // free. The shell closes at KeyOfStarsFx.CONCEALED. Wanderer: Spatial Cage, closed at once, the Door on the side you
    // look toward. Planeswalker: HOSTEL_RADIUS and HOSTEL_TICKS.
    // =========================
    private static AbilityWheel.Result spaceConcealment(ServerPlayer player, ServerLevel sl) {
        Concealed standing = CONCEALED.get(player.getUUID());
        if (standing != null && Dome.isRaised(player, CONCEAL_DOME) && standing.doorOpen) {
            standing.doorOpen = false;
            KeyOfStarsFx.concealmentShut(sl, standing.center, standing.radius, standing.doorYaw, standing.tag);
            return AbilityWheel.Result.FREE;
        }
        boolean hostel = Beyonder.hasSequence(player, HOSTEL_SEQUENCE);
        boolean cage = Beyonder.hasSequence(player, CAGE_SEQUENCE);
        double radius = hostel ? HOSTEL_RADIUS : CONCEAL_RADIUS;
        int ticks = hostel ? HOSTEL_TICKS : CONCEAL_TICKS;
        Vec3 eye = player.getEyePosition();
        Vec3 aimed = Spells.aimPoint(player, sl, CONCEAL_RANGE);
        boolean onSelf = aimed.distanceTo(eye) >= CONCEAL_RANGE - 0.5;          // aimed at nothing: round you
        Vec3 center = onSelf ? player.position() : Spells.aimGround(player, sl, CONCEAL_RANGE);
        float doorYaw = onSelf || cage
                ? player.getYRot() - 180f                                       // the Door toward where you look
                : Spells.yawOf(player.position().subtract(center)) - 180f;      // the Door on the side facing you
        String tag = "ks_conceal_" + player.getId() + "_" + sl.getGameTime();
        Dome.close(player, CONCEAL_DOME);
        Concealed made = new Concealed(center, radius, doorYaw, tag);
        CONCEALED.put(player.getUUID(), made);
        KeyOfStarsFx.spaceConcealment(sl, player, center, radius, ticks, doorYaw, cage, tag);
        int risen = cage ? 2 : KeyOfStarsFx.CONCEALED;
        Later.run(sl, risen, () -> {
            if (!Spells.casterStillHere(player, sl) || CONCEALED.get(player.getUUID()) != made) return;
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, center, radius)) {
                KeyOfStars.starlit(player, enemy, KeyOfStars.STARLIT_TICKS);
            }
            Dome.at(center, radius)
                    .lasts(ticks - risen)
                    .keepsEnemiesIn()
                    .stopsProjectiles()
                    .outsiders(entity -> Spells.isEnemy(player, entity)
                            && !(made.doorOpen && entity.position().distanceTo(made.door) <= DOOR_WIDTH))
                    .onEnd(() -> {
                        KeyOfStarsFx.concealmentEnd(sl, center, radius, tag);
                        CONCEALED.remove(player.getUUID(), made);
                        KeyOfStarsFx.statusEnd(player, KeyOfStarsFx.Status.SPACE_CONCEALMENT);
                    })
                    .raise(player, sl, CONCEAL_DOME);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Space Concealment cast again: true while one stands with its Door open (shutting it is free). */
    private static boolean canShutDoor(Player player) {
        Concealed standing = CONCEALED.get(player.getUUID());
        return standing != null && standing.doorOpen && Dome.isRaised(player, CONCEAL_DOME);
    }

    /** Every half second: you and your allies inside are hidden and healed. */
    private static void concealmentTick(Player player, ServerLevel sl) {
        Concealed standing = CONCEALED.get(player.getUUID());
        if (standing == null || !Dome.isRaised(player, CONCEAL_DOME)) return;
        List<LivingEntity> inside = Spells.alliesAround(player, sl, standing.center, standing.radius);
        if (player.position().distanceTo(standing.center) <= standing.radius) inside.add(player);
        for (LivingEntity ally : inside) {
            Concealment.hide(ally, 15);
            if (player.tickCount % 20 == 0 && ally.getHealth() < ally.getMaxHealth()) {
                ally.heal(CONCEAL_HEAL);
                KeyOfStarsFx.concealedHeal(sl, ally);
            }
        }
    }

    // =========================
    // Transfiguration: for TRANSFIG_TICKS you are LAYERS layers of illusory Doors (Key of Stars: MORE_LAYERS). Every
    // hit that would land (anything with an attacker, a shot, a blast: status/Substitute) tears one instead, and you are
    // LAYER_STEP blocks aside, unhurt (unless a Teleport root holds you: then only the layer goes). No layers left, or
    // the time is up: it ends.
    // =========================
    private static AbilityWheel.Result transfiguration(ServerPlayer player, ServerLevel sl) {
        if (isTransfigured(player)) {
            player.sendSystemMessage(Component.literal("You are layers of Doors already."));
            return AbilityWheel.Result.REFUSED;
        }
        int layers = Beyonder.hasSequence(player, MORE_LAYERS_SEQUENCE) ? MORE_LAYERS : LAYERS;
        player.getPersistentData().putLong(TRANSFIG_UNTIL, sl.getGameTime() + TRANSFIG_TICKS);
        KeyOfStarsFx.transfiguration(sl, player, layers, TRANSFIG_TICKS, transfigTag(player));
        Substitute.arm(player, TRANSFIG_TICKS, layers, (saved, source, amount, level) -> {
            Vec3 from = player.position();
            Vec3 side = Spells.rightOf(player).scale(player.getRandom().nextBoolean() ? 1 : -1);
            Vec3 to = Teleport.canGo(player) ? Spells.freeSpotNear(player, level, from, side, LAYER_STEP) : null;
            if (to != null) KeyOfStars.teleport(player, level, to, false);
            int left = Substitute.charges(player);
            KeyOfStarsFx.layerTorn(level, player, from, to != null ? to : from, left);
            if (left <= 0) endTransfiguration(player, level);
        });
        return AbilityWheel.Result.DONE;
    }

    static boolean isTransfigured(Player player) {
        return player.getPersistentData().getLongOr(TRANSFIG_UNTIL, 0L) > player.level().getGameTime();
    }

    private static void transfigurationTick(Player player, ServerLevel sl) {
        long until = player.getPersistentData().getLongOr(TRANSFIG_UNTIL, 0L);
        if (until == 0L) return;
        if (until <= sl.getGameTime() || !Substitute.isArmed(player)) endTransfiguration(player, sl);
    }

    private static void endTransfiguration(Player player, ServerLevel sl) {
        player.getPersistentData().remove(TRANSFIG_UNTIL);
        Substitute.disarm(player);
        KeyOfStarsFx.transfigurationEnd(sl, player, transfigTag(player));
    }

    private static String transfigTag(Player player) {
        return "ks_transfig_" + player.getId();
    }

    // =========================
    // Exile: an enemy within EXILE_RANGE that you have Starlit, or that is held (rooted, Sealed). Doors swallow it at
    // KeyOfStarsFx.EXILE_SWALLOWED into a space-time storm (status/Exile): for EXILE_TICKS (Wanderer EXILE_LONGER, Key of
    // Stars EXILE_LONGEST; a boss EXILE_BOSS) it cannot be seen, hit or act, then it comes back out of the same Door on
    // the same spot and takes EXILE_DAMAGE. The Starlit mark is used up. Key of Stars: up to EXILE_MANY at once (the
    // one aimed at and the nearest others you have Starlit).
    // =========================
    private static AbilityWheel.Result exile(ServerPlayer player, ServerLevel sl) {
        LivingEntity aimed = Spells.aimEnemy(player, sl, EXILE_RANGE);
        if (aimed == null || !caught(player, aimed)) {
            player.sendSystemMessage(Component.literal(aimed == null ? "No enemy stands where you look."
                    : "It is not caught: Starlit it or hold it first."));
            return AbilityWheel.Result.REFUSED;
        }
        Set<LivingEntity> exiled = new LinkedHashSet<>();
        exiled.add(aimed);
        if (Beyonder.hasSequence(player, EXILE_LONGEST_SEQUENCE)) {
            for (LivingEntity other : Starlit.marked(player)) {
                if (exiled.size() >= EXILE_MANY) break;
                if (other.level() == sl && other.distanceTo(player) <= EXILE_RANGE && Spells.isEnemy(player, other)) exiled.add(other);
            }
        }
        int ticks = Beyonder.hasSequence(player, EXILE_LONGEST_SEQUENCE) ? EXILE_LONGEST
                : Beyonder.hasSequence(player, EXILE_LONGER_SEQUENCE) ? EXILE_LONGER : EXILE_TICKS;
        for (LivingEntity target : exiled) exileOne(player, sl, target, ticks);
        return AbilityWheel.Result.DONE;
    }

    /** Starlit by this player, or held (rooted, Sealed). */
    private static boolean caught(Player player, LivingEntity target) {
        return Starlit.has(target, player) || Root.isRooted(target) || Seal.isSealed(target);
    }

    private static void exileOne(ServerPlayer player, ServerLevel sl, LivingEntity target, int ticks) {
        Starlit.consume(target, player);
        int time = Spells.isBoss(target) ? EXILE_BOSS : ticks;
        KeyOfStarsFx.exile(sl, player, target);
        Later.run(sl, KeyOfStarsFx.EXILE_SWALLOWED, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            Vec3 at = target.position();
            String tag = "ks_exile_" + target.getId();
            if (!Exile.banish(target, time, at, (who, level) -> {
                KeyOfStarsFx.exileReturn(level, at, tag);
                if (player.isAlive() && player.level() == level) Spells.strike(player, who, EXILE_DAMAGE);
                else who.hurt(who.damageSources().magic(), EXILE_DAMAGE);
            })) return;
            KeyOfStarsFx.exiled(sl, target, at, time, tag);
        });
    }

    // =========================
    // Pocket Space: a small Door opens onto your own Concealed space: your Ender Chest, wherever you are.
    // (The vanilla Ender Chest: its 27 slots are the ones every ender chest opens.)
    // =========================
    private static AbilityWheel.Result pocketSpace(ServerPlayer player, ServerLevel sl) {
        KeyOfStarsFx.pocketSpace(sl, player);
        Later.run(sl, POCKET_OPENS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, who) -> ChestMenu.threeRows(id, inventory, player.getEnderChestInventory()),
                    Component.literal("Pocket Space")));
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Space Tearing (Wanderer): aimed at an enemy within TEARING_RANGE. A ring warns for KeyOfStarsFx.TEAR_LANDS, then
    // space tears round it: it and every enemy within TEARING_RADIUS take TEARING_DAMAGE and burn in a space-time storm
    // for STORM_SECONDS (STORM_DAMAGE a second, Slowness II). Planeswalker: TEARING_WIDE_RANGE, TEARING_WIDE_RADIUS,
    // and space tears round you as well.
    // =========================
    private static AbilityWheel.Result spaceTearing(ServerPlayer player, ServerLevel sl) {
        boolean wide = Beyonder.hasSequence(player, TEARING_WIDE_SEQUENCE);
        double range = wide ? TEARING_WIDE_RANGE : TEARING_RANGE;
        double radius = wide ? TEARING_WIDE_RADIUS : TEARING_RADIUS;
        LivingEntity target = Spells.aimEnemy(player, sl, range);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No enemy stands close enough where you look."));
            return AbilityWheel.Result.REFUSED;
        }
        KeyOfStarsFx.spaceTearing(sl, player, target, radius);
        if (wide) KeyOfStarsFx.spaceTearing(sl, player, player, radius);
        Later.run(sl, KeyOfStarsFx.TEAR_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Set<LivingEntity> torn = new LinkedHashSet<>();
            Vec3 at = target.isAlive() ? target.position() : null;
            if (at != null) {
                if (Spells.isEnemy(player, target)) torn.add(target);
                torn.addAll(Spells.enemiesAround(player, sl, at, radius));
            }
            if (wide) torn.addAll(Spells.enemiesAround(player, sl, player.position(), radius));
            for (LivingEntity one : torn) {
                Spells.strike(player, one, TEARING_DAMAGE);
                storm(player, sl, one);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    /** The space-time storm: STORM_DAMAGE and Slowness II each second for STORM_SECONDS. */
    private static void storm(Player player, ServerLevel sl, LivingEntity target) {
        if (!target.isAlive()) return;
        String tag = "ks_storm_" + target.getId();
        KeyOfStarsFx.storm(sl, target, STORM_SECONDS * 20, tag);
        for (int s = 1; s <= STORM_SECONDS; s++) {
            Later.run(sl, s * 20, () -> {
                if (!target.isAlive() || target.level() != sl) return;
                KeyOfStarsFx.stormTick(sl, target);
                Spells.keepEffect(target, MobEffects.SLOWNESS, 1, 25);
                if (player.isAlive() && player.level() == sl) Spells.strike(player, target, STORM_DAMAGE);
            });
        }
    }

    /** The entries a copy from elsewhere (an echo, combat/Echo) never repeats: they are where you are, or yours alone. */
    static boolean isOwnSpace(String entryKey) {
        return entryKey.equals("space_concealment") || entryKey.equals("transfiguration")
                || entryKey.equals("pocket_space") || entryKey.equals("exile");
    }

    /** All of this player's Space Concealments and Transfiguration end (a relog). */
    static void endAll(Player player, ServerLevel sl) {
        Dome.close(player, CONCEAL_DOME);
        CONCEALED.remove(player.getUUID());
        if (player.getPersistentData().contains(TRANSFIG_UNTIL)) endTransfiguration(player, sl);
    }
}
