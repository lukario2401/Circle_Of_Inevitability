package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Reel;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.FallGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KnightOfMisfortune {

    public static final String PATHWAY = "Knight Of Misfortune";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Warrior's Charge (Sequence 9)
    private static final int CHARGE_SEQUENCE = 9;
    private static final int CHARGE_COST = 250;
    private static final double CHARGE_RANGE = 8.0;
    private static final double CHARGE_SPEED = 1.4;              // blocks per tick at most
    private static final int CHARGE_TICKS = 12;                  // the rush lets go after this long anyway
    private static final double CHARGE_REACH = 0.8;              // how close (beyond your body) an enemy counts as hit
    private static final float CHARGE_DAMAGE = 30f;              // the first one in your way...
    private static final double CHARGE_THROW = 1.4;              // ...is thrown back this hard...
    private static final double CHARGE_LIFT = 0.45;
    private static final double CHARGE_SHOVE = 0.7;

    private static final String CHARGING_UNTIL = "hand_of_god_charging_until";


    public static void knightOfMisfortuneRush(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, CHARGE_SEQUENCE)) return;

//        if (player.isShiftKeyDown()) {
//            if (!Spells.paySpirituality(player, BLOOD_COST)) return;
//            divineBlood(player, sl);
//            return;
//        }
        if (isActive(player, CHARGING_UNTIL)) return;                   // already rushing: let the key bounce off
        if (Root.has(player, RootRestriction.MOVE)) {
            player.sendSystemMessage(Component.literal("You are held fast and cannot charge."));
            return;
        }
        if (!Spells.paySpirituality(player, CHARGE_COST)) return;
        charge(player, sl);
    }

    static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    static boolean isActive(Entity entity, String key) {
        return entity.getPersistentData().getLongOr(key, 0L) > entity.level().getGameTime();
    }

    private static void charge(Player player, ServerLevel sl) {
        LivingEntity aimed = Spells.aimEnemy(player, sl, CHARGE_RANGE + 1);
        Vec3 look = Spells.flatLook(player);
        Vec3 end = player.position().add(look.scale(CHARGE_RANGE));
        long until = sl.getGameTime() + CHARGE_TICKS;
        player.getPersistentData().putLong(CHARGING_UNTIL, until);
        FallGuard.protect(player, 100);
        HandOfGodFx.chargeStart(sl, player);

        boolean[] struck = {false};
        Set<UUID> shoved = new HashSet<>();
        Reel.in(player, () -> {
                    if (struck[0] || !Spells.casterStillHere(player, sl)) return null;    // let go
                    return aimed != null && aimed.isAlive() && aimed.level() == sl ? aimed.position() : end;
                })
                .speed(CHARGE_SPEED).stopAt(0.6).lasts(CHARGE_TICKS)
                .onEnd(() -> {
                    if (player.getPersistentData().getLongOr(CHARGING_UNTIL, 0L) == until) player.getPersistentData().remove(CHARGING_UNTIL);
                })
                .start(sl);
        chargeStep(player, sl, until, struck, shoved, 0);
    }

    private static void chargeStep(Player player, ServerLevel sl, long until, boolean[] struck, Set<UUID> shoved, int step) {
        if (struck[0] || step > CHARGE_TICKS || !Spells.casterStillHere(player, sl)) return;
        if (player.getPersistentData().getLongOr(CHARGING_UNTIL, 0L) != until) return;      // the rush is over
        HandOfGodFx.chargeTrail(sl, player);
        Vec3 look = Spells.flatLook(player);
        Vec3 chest = player.getBoundingBox().getCenter();
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, chest, CHARGE_REACH + player.getBbWidth())) {
            Vec3 to = enemy.getBoundingBox().getCenter().subtract(chest);
            Vec3 flat = new Vec3(to.x, 0, to.z);
            double along = flat.dot(look);
            double aside = flat.subtract(look.scale(along)).length();
            boolean inTheWay = along > -0.2 && aside < 0.6 + enemy.getBbWidth() * 0.5;
            if (inTheWay && !struck[0]) {
                struck[0] = true;
                Spells.strike(player, enemy, CHARGE_DAMAGE);
                Spells.push(enemy, player.position(), CHARGE_THROW, CHARGE_LIFT);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                HandOfGodFx.chargeImpact(sl, player, enemy);
            } else if (!inTheWay && shoved.add(enemy.getUUID())) {
                Vec3 side = flat.subtract(look.scale(along));
                Vec3 from = side.lengthSqr() < 1.0E-4 ? player.position() : enemy.position().subtract(side.normalize());
                Spells.push(enemy, from, CHARGE_SHOVE, 0.25);
            }
        }
        if (struck[0]) {
            player.getPersistentData().remove(CHARGING_UNTIL);
            return;
        }
        Later.run(sl, 1, () -> chargeStep(player, sl, until, struck, shoved, step + 1));
    }
}
