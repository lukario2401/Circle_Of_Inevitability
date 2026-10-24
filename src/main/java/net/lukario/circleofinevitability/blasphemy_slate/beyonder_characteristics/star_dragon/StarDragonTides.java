package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonFx.Status;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Form;
import net.lukario.circleofinevitability.status.Weight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Star Dragon, the Tidal Scholar's key 4 (docs/STAR_DRAGON.md): Tidal Surge and Scaled Form (the Heavy Body from the
 * Heavybringer). Scaled Form is a status/Form, the same manager the Radiant Angel and the Star Dragon forms use
 * (StarDragonForms). Every number is right below; the looks are StarDragonFx's.
 */
public final class StarDragonTides {

    private StarDragonTides() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Tidal Surge
    private static final int SURGE_COST = 3000;
    private static final int SURGE_COOLDOWN = 200;
    private static final double SURGE_HALF_WIDTH = 3.5;          // a crest 7 wide...
    private static final double SURGE_HEIGHT = 5.0;              // ...and 5 tall
    private static final float SURGE_DAMAGE = 80f;
    private static final double SURGE_SHOVE = 1.0;
    private static final int SURGE_WEIGHT = 1;                   // Heavy (status/Weight)...
    private static final int SURGE_HEAVY_TICKS = 60;             // ...for 3 seconds
    private static final int PARRY_TICKS = 10;                   // the crest beside you parries the first blow this long...
    private static final int PARRY_STAGGER = 20;                 // ...and staggers its sender

    // Scaled Form (and the Heavy Body from the Heavybringer)
    private static final int SCALES_COST = 3000;
    private static final int SCALES_COOLDOWN = 60;
    private static final int SCALES_TICKS = 600;                 // 30 seconds...
    private static final int HEAVY_BODY_SEQUENCE = 4;
    private static final int HEAVY_BODY_TICKS = 900;             // ...45 for the Heavy Body
    private static final double SCALES_REACH = 1.0;
    private static final double HEAVY_BODY_REACH = 2.5;
    private static final double HEAVY_BODY_SIZE = 1.78;          // 1.8 blocks to 5 (Attributes.SCALE, of the base)
    private static final int SCALED_HEAVY = 40;                  // every blow makes the target Heavy this long
    private static final double HEAVY_BODY_SLOWS = 6.0;          // the Heavy Body slows enemies this close

    // =========================
    // Names
    // =========================
    private static final String PARRY_UNTIL = "star_dragon_parry_until";
    private static final String SCALED = "star_dragon_scaled_form";              // its Form
    private static final String HEAVY_BODY = "star_dragon_heavy_body";           // its Form
    private static final String CD_SURGE = "star_dragon_tidal_surge";
    private static final String CD_SCALES = "star_dragon_scaled_form";

    // =========================
    // Tidal Surge: gravity whips the air into a wave. A crest 7 wide and 5 tall rolls StarDragonFx.SURGE_REACH forward
    // in StarDragonFx.SURGE_TICKS; everything it touches takes SURGE_DAMAGE once, is thrown on and made Heavy for
    // SURGE_HEAVY_TICKS. While it is still beside you (PARRY_TICKS), the first melee blow aimed at you is parried and
    // its sender staggered. It works anywhere, not only near water.
    // =========================
    static void tidalSurge(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, CD_SURGE, "Tidal Surge")) return;
        if (!StarDragon.pay(player, SURGE_COST, CD_SURGE)) return;
        Cooldowns.start(player, CD_SURGE, SURGE_COOLDOWN);
        float yaw = player.getYRot();
        Vec3 forward = Spells.flatLook(player);
        Vec3 side = Spells.rightOf(player);
        Vec3 start = player.position().add(forward.scale(1.2));
        player.getPersistentData().putLong(PARRY_UNTIL, sl.getGameTime() + PARRY_TICKS);
        StarDragonFx.tidalSurge(sl, player);
        Set<UUID> struck = new HashSet<>();
        for (int t = 1; t <= StarDragonFx.SURGE_TICKS; t++) {
            double along = StarDragonFx.SURGE_REACH * t / StarDragonFx.SURGE_TICKS;
            Later.run(sl, t, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                Vec3 front = start.add(forward.scale(along));
                for (LivingEntity enemy : Spells.enemiesAround(player, sl, front.add(0, 1, 0), SURGE_HALF_WIDTH + 1.5)) {
                    Vec3 off = enemy.position().subtract(front);
                    if (Math.abs(off.dot(side)) > SURGE_HALF_WIDTH || Math.abs(off.dot(forward)) > 1.6) continue;
                    if (off.y < -1.5 || off.y > SURGE_HEIGHT) continue;
                    if (!struck.add(enemy.getUUID())) continue;
                    StarDragon.hit(player, enemy, SURGE_DAMAGE);
                    if (!enemy.isAlive()) continue;
                    Spells.push(enemy, front.subtract(forward.scale(2)), SURGE_SHOVE, 0.35);
                    Weight.put(enemy, SURGE_WEIGHT, SURGE_HEAVY_TICKS);
                    StarDragonFx.surgeHit(sl, enemy);
                    StarDragonFx.heavy(sl, enemy, SURGE_HEAVY_TICKS, "sd_heavy_" + enemy.getId());
                    if (enemy instanceof Player shown) StarDragonFx.status(shown, Status.HEAVY, SURGE_HEAVY_TICKS);
                }
            });
        }
    }

    /** Before a blow lands on a Tidal Scholar: the crest still beside them parries the first melee blow. */
    static boolean parries(Player player, ServerLevel sl, DamageSource source) {
        if (!StarDragon.isActive(player, PARRY_UNTIL)) return false;
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity striker) || source.getDirectEntity() != attacker) return false;   // melee only
        player.getPersistentData().remove(PARRY_UNTIL);
        Root.apply(striker, Spells.holdLength(striker, PARRY_STAGGER), RootRestriction.MOVE, RootRestriction.ATTACK);
        StarDragonFx.surgeParry(sl, player, striker);
        return true;
    }

    // =========================
    // Scaled Form (key 4, sneak): starlight scales over you for SCALES_TICKS, a humanoid lizard (status/Form): Strength
    // II, Resistance II, SCALES_REACH more reach, and every blow you land makes the target Heavy for SCALED_HEAVY. Cast
    // it again to turn back (free). Heavybringer: the Heavy Body, 5 blocks tall, Resistance III, HEAVY_BODY_REACH more
    // reach, enemies within HEAVY_BODY_SLOWS slowed, HEAVY_BODY_TICKS.
    // =========================
    static void scaledForm(Player player, ServerLevel sl) {
        if (Form.isIn(player, SCALED) || Form.isIn(player, HEAVY_BODY)) {
            Form.end(player);                                                    // turning back is free
            return;
        }
        if (!Cooldowns.ready(player, CD_SCALES, "Scaled Form")) return;
        if (!StarDragon.pay(player, SCALES_COST, CD_SCALES)) return;
        Cooldowns.start(player, CD_SCALES, SCALES_COOLDOWN);
        boolean heavy = StarDragon.grown(player, HEAVY_BODY_SEQUENCE);
        int ticks = heavy ? HEAVY_BODY_TICKS : SCALES_TICKS;
        String tag = scalesTag(player);
        Status status = heavy ? Status.HEAVY_BODY : Status.SCALED_FORM;
        Form form = Form.of(heavy ? HEAVY_BODY : SCALED)
                .lasts(ticks)
                .effect(MobEffects.STRENGTH, 1)
                .effect(MobEffects.RESISTANCE, heavy ? 2 : 1)
                .attribute(Attributes.ENTITY_INTERACTION_RANGE, heavy ? HEAVY_BODY_REACH : SCALES_REACH, AttributeModifier.Operation.ADD_VALUE)
                .keepWhile(one -> SoulCore.getPathway(one).equals(StarDragon.PATHWAY) || one.getPersistentData().getBooleanOr(STOLEN, false))
                .onEnd((one, why) -> {
                    one.getPersistentData().remove(STOLEN);
                    if (one.level() instanceof ServerLevel level) StarDragonFx.scaledFormEnd(level, one, tag);
                    StarDragonFx.statusEnd(one, status);
                });
        if (heavy) {
            form = form.attribute(Attributes.SCALE, HEAVY_BODY_SIZE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                    .everySecond(one -> heavyBodyTick(one));
        }
        player.getPersistentData().putBoolean(STOLEN, !SoulCore.getPathway(player).equals(StarDragon.PATHWAY));
        form.take(player);
        StarDragonFx.scaledForm(sl, player, ticks, heavy, tag);
        StarDragonFx.status(player, status, ticks);
    }

    /** Set while a form was taken through a stolen cast, so it is not ended for being another Pathway's. */
    private static final String STOLEN = "star_dragon_form_stolen";

    /** Every second of the Heavy Body: enemies near it are slowed, the ground looks pressed down. */
    private static void heavyBodyTick(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), HEAVY_BODY_SLOWS)) {
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, 40);
        }
        StarDragonFx.heavyBodyTick(sl, player);
    }

    /** A blow of the Scaled Form (melee, by the one in it) makes the target Heavy. */
    static void hurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player striker) || source.getDirectEntity() != striker) return;
        if (!Form.isIn(striker, SCALED) && !Form.isIn(striker, HEAVY_BODY)) return;
        LivingEntity target = event.getEntity();
        if (target == striker || !(target.level() instanceof ServerLevel sl)) return;
        Weight.put(target, 1, SCALED_HEAVY);
        StarDragonFx.scaledBlow(sl, target);
        StarDragonFx.heavy(sl, target, SCALED_HEAVY, "sd_heavy_" + target.getId());
        if (target instanceof Player shown) StarDragonFx.status(shown, Status.HEAVY, SCALED_HEAVY);
    }

    private static String scalesTag(Player player) {
        return "sd_scales_" + player.getId();
    }

    /** After a relog or a death. */
    static void clearState(Player player) {
        player.getPersistentData().remove(PARRY_UNTIL);
        player.getPersistentData().remove(STOLEN);
    }
}
