package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonFx.RayKind;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonMarks.Mark;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Ray;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Bleeding;
import net.lukario.circleofinevitability.status.Blind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Star Dragon, the rays (docs/STAR_DRAGON.md): the Weakening Ray and the Sunlight Ray (Star Sacrifice, key 2), the
 * Piercing Ray (key 3, sneak) and the Series of Rays (Heavybringer, key 5). Every ray is a combat/Ray: it shows its
 * line for StarDragonFx.RAY_WARNING ticks and then it is there at once and cannot be dodged. Its hits go through
 * StarDragon.rayHit (the Star Chart's calculated shot, the void's manifestation). The Dragon's thicker rays of the second
 * half go here too: the same Ray with RayKind.DRAGON and a wider width. Every number is right below.
 */
public final class StarDragonRays {

    private StarDragonRays() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Weakening Ray (Star Sacrificer)
    static final int WEAKENING_SEQUENCE = 7;
    static final int WEAKENING_COST = 1250;
    static final int WEAKENING_COOLDOWN = 120;
    private static final double WEAKENING_RANGE = 30.0;
    private static final double WEAKENING_WIDTH = 0.3;
    private static final float WEAKENING_DAMAGE = 40f;
    private static final int WEAKENED_TICKS = 120;
    private static final int MELT_SEQUENCE = 5;                 // Tidal Scholar: it melts flesh (StarDragonMarks.MELTING)

    // Sunlight Ray (Tidal Scholar)
    static final int SUNLIGHT_SEQUENCE = 5;
    static final int SUNLIGHT_COST = 3000;
    static final int SUNLIGHT_COOLDOWN = 200;
    private static final double SUNLIGHT_RANGE = 24.0;
    private static final double SUNLIGHT_WIDTH = 1.0;           // 2 blocks wide
    private static final float SUNLIGHT_DAMAGE = 80f;
    private static final float SUNLIGHT_UNDEAD = 2f;            // undead and creatures of darkness take this many times
    private static final int SUNLIGHT_BLIND = 40;
    private static final float SUNLIGHT_DAY_COST = 0.5f;        // in real daylight under open sky

    // Piercing Ray (Navigator, key 3 sneak)
    private static final int PIERCING_COST = 2450;
    private static final int PIERCING_COOLDOWN = 160;
    private static final double PIERCING_RANGE = 28.0;
    private static final double PIERCING_WIDTH = 0.5;
    private static final float PIERCING_DAMAGE = 40f;
    private static final int DIMMED_TICKS = 160;

    // Series of Rays (Heavybringer)
    static final int SERIES_SEQUENCE = 4;
    static final int SERIES_COST = 6000;
    static final int SERIES_COOLDOWN = 300;
    private static final double SERIES_RANGE = 30.0;
    private static final int SERIES_COUNT = 6;
    private static final double SERIES_FAN = 4.0;               // degrees between rays
    private static final float SERIES_DAMAGE = 20f;
    private static final int SERIES_WEAKENED = 100;
    private static final int MELT_TICKS = 100;                  // three stacks: it melts, and takes...
    private static final float MELT_PER_SECOND = 30f;           // ...this much a second for 5 seconds (very heavy)

    private static final String CD_PIERCING = "star_dragon_piercing_ray";

    // =========================
    // Weakening Ray: a thin dark-green ray from your eyes, WEAKENING_RANGE long, its line shown for RAY_WARNING ticks.
    // The first enemy on it takes WEAKENING_DAMAGE and is Weakened for WEAKENED_TICKS (it bleeds and cannot heal until a
    // cleanse; StarDragonMarks.WEAKENED). Tidal Scholar: it melts too (half its armor, 20% more damage taken).
    // =========================
    static AbilityWheel.Result weakeningRay(ServerPlayer player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition().add(0, -0.2, 0);
        Ray.from(eye, player.getLookAngle())
                .range(WEAKENING_RANGE).width(WEAKENING_WIDTH).warning(StarDragonFx.RAY_WARNING)
                .onWarn((from, to) -> StarDragonFx.rayWarning(sl, from, to, RayKind.WEAKENING))
                .onFire((from, to) -> StarDragonFx.rayFire(sl, from, to, RayKind.WEAKENING, 0.25f))
                .onHit((target, at) -> weaken(player, target, WEAKENING_DAMAGE, WEAKENED_TICKS))
                .fire(player, sl);
        return AbilityWheel.Result.DONE;
    }

    /** One weakening hit: the damage, a Weakened stack, and (Tidal Scholar) the melting. */
    private static void weaken(Player player, LivingEntity target, float damage, int ticks) {
        StarDragon.rayHit(player, target, damage);
        if (!target.isAlive()) return;
        StarDragonMarks.put(target, Mark.WEAKENED, player, ticks);
        if (StarDragon.grown(player, MELT_SEQUENCE)) StarDragonMarks.put(target, Mark.MELTING, player, ticks);
    }

    // =========================
    // Sunlight Ray: sunlight condenses in your hands for StarDragonFx.SUNLIGHT_FIRES, then a ray SUNLIGHT_RANGE long and
    // 2 wide: every enemy on it takes SUNLIGHT_DAMAGE (twice for the undead) and is blinded SUNLIGHT_BLIND. It costs half
    // in real daylight under the open sky (sunlightCost, the wheel's costBy).
    // =========================
    static AbilityWheel.Result sunlightRay(ServerPlayer player, ServerLevel sl) {
        StarDragonFx.sunlightCharge(sl, player);
        Later.run(sl, StarDragonFx.SUNLIGHT_FIRES, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Vec3 hands = player.getEyePosition().add(player.getLookAngle().scale(1.0)).add(0, -0.35, 0);
            Ray.from(hands, player.getLookAngle())
                    .range(SUNLIGHT_RANGE).width(SUNLIGHT_WIDTH).pierces()
                    .onFire((from, to) -> StarDragonFx.rayFire(sl, from, to, RayKind.SUNLIGHT, 2f))
                    .onHit((target, at) -> {
                        boolean dark = target.is(EntityTypeTags.UNDEAD);
                        StarDragon.rayHit(player, target, SUNLIGHT_DAMAGE * (dark ? SUNLIGHT_UNDEAD : 1f));
                        if (!target.isAlive()) return;
                        Blind.apply(target, SUNLIGHT_BLIND);
                        StarDragonFx.sunburnt(sl, target);
                    })
                    .fire(player, sl);
        });
        return AbilityWheel.Result.DONE;
    }

    /** The Sunlight Ray's cost: half in real daylight under the open sky. */
    static int sunlightCost(Player player, int cost) {
        if (!(player.level() instanceof ServerLevel sl)) return cost;
        return Spells.inDaylight(sl, player.blockPosition().above()) ? Math.round(cost * SUNLIGHT_DAY_COST) : cost;
    }

    // =========================
    // Piercing Ray (key 3, sneak): two beams of blue light from your eyes, PIERCING_RANGE long, through everyone on the
    // line: PIERCING_DAMAGE each, and Dimmed for DIMMED_TICKS (25% less dealt, a player's spirituality drains).
    // =========================
    static void piercingRay(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, CD_PIERCING, "Piercing Ray")) return;
        if (!StarDragon.pay(player, PIERCING_COST, CD_PIERCING)) return;
        Cooldowns.start(player, CD_PIERCING, PIERCING_COOLDOWN);
        Vec3 eye = player.getEyePosition();
        Ray.from(eye, player.getLookAngle())
                .range(PIERCING_RANGE).width(PIERCING_WIDTH).pierces()
                .onFire((from, to) -> StarDragonFx.piercingRay(sl, player, to))
                .onHit((target, at) -> {
                    StarDragon.rayHit(player, target, PIERCING_DAMAGE);
                    if (target.isAlive()) StarDragonMarks.put(target, Mark.DIMMED, player, DIMMED_TICKS);
                })
                .fire(player, sl);
    }

    // =========================
    // Series of Rays: the enemy you look at (SERIES_RANGE). Dark-green light gathers in your hands, then SERIES_COUNT
    // Weakening Rays in a narrow fan on it, one every StarDragonFx.SERIES_STEP: each SERIES_DAMAGE and a Weakened stack.
    // At StarDragonMarks.MELT_AT stacks its skin melts: MELT_PER_SECOND for MELT_TICKS (once a cast).
    // =========================
    static AbilityWheel.Result seriesOfRays(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, SERIES_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("The light in your hands has no one to descend on."));
            return AbilityWheel.Result.REFUSED;
        }
        StarDragonFx.seriesOfRays(sl, player);
        Set<UUID> melted = new HashSet<>();
        for (int i = 0; i < SERIES_COUNT; i++) {
            double turn = (i - (SERIES_COUNT - 1) / 2.0) * SERIES_FAN;
            Later.run(sl, StarDragonFx.SERIES_FIRES + i * StarDragonFx.SERIES_STEP, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                Vec3 hand = Spells.clearStart(player, sl, player.getEyePosition().add(Spells.rightOf(player).scale(0.3)).add(0, -0.3, 0));
                Vec3 aim = target.isAlive() ? target.getBoundingBox().getCenter().subtract(hand) : player.getLookAngle();
                Ray.from(hand, Spells.turned(aim, turn))
                        .range(SERIES_RANGE).width(0.6)
                        .onFire((from, to) -> StarDragonFx.rayFire(sl, from, to, RayKind.WEAKENING, 0.2f))
                        .onHit((hit, at) -> {
                            weaken(player, hit, SERIES_DAMAGE, SERIES_WEAKENED);
                            if (hit.isAlive() && StarDragonMarks.stacks(hit, Mark.WEAKENED) >= StarDragonMarks.MELT_AT
                                    && melted.add(hit.getUUID())) {
                                StarDragonMarks.put(hit, Mark.MELTING, player, MELT_TICKS);
                                Bleeding.apply(hit, player, MELT_TICKS, MELT_PER_SECOND);
                            }
                        })
                        .fire(player, sl);
            });
        }
        return AbilityWheel.Result.DONE;
    }
}
