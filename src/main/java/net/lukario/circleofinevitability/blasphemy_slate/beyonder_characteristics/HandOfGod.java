package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.HandOfGodFx.Status;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Reel;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.DamageLink;
import net.lukario.circleofinevitability.status.Decay;
import net.lukario.circleofinevitability.status.EmpoweredHit;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.SupernaturalResistance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hand Of God: the Twilight Giant Pathway of Lord of the Mysteries (docs/HAND_OF_GOD.md). Seven keys in the
 * Attendant Of Mysteries pattern, from Warrior (Sequence 9) up to Twilight Giant (Sequence 0); every ability has
 * a normal and a sneak cast:
 *
 *   1 Warrior's Charge   (Warrior)        rush up to 8 blocks; the first enemy in your way is hit hard and thrown
 *                                                                              | sneak: Divine Blood
 *   2 Gladiator's Flurry (Pugilist)       five very fast blows on the enemy in front of you, the last throws it
 *                                                                              | sneak: Supernatural Resistance
 *   3 Grandmaster's Edge (Weapon Master)  15 s: faster, longer swings that cut everything in front; every third
 *                                         swing in a row is a finishing cut     | sneak: Riposte
 *   4 Dawn               (Dawn Paladin)   the Dawn wheel: Light of Dawn, Dawn Armor, Sword of Dawn, Hurricane of
 *                                         Light (HandOfGodDawn)                 | sneak: the last pick again
 *   5 Protection         (Guardian)       a dome of Dawn light round you: enemies kept out, projectiles stopped,
 *                                         allies shielded; from Glory (2) a Cage that holds enemies in
 *                                                                              | sneak: Damage Transference
 *   6 Demon Hunting      (Demon Hunter)   the Demon Hunting wheel: Eye of Demon Hunting, Alchemy, Mind
 *                                         Concealment, Giant Form (HandOfGodDemonHunting) | sneak: the last pick again
 *   7 Silver and Twilight (Silver Knight) the Silver and Twilight wheel: Mercury Liquefaction, Silver Rapier, Light
 *                                         Concealment, Twilight Sword, Divine Right Hand, Passage of Time, Decay,
 *                                         Divine Kingdom (HandOfGodTwilight)    | sneak: the last pick again
 *
 * The three wheels each have a class of their own beside this one, with their numbers at the top; this class
 * ticks them and starts them clean after a relog or a death. Every look and sound is in HandOfGodFx (see /handfx);
 * the ability classes only decide who is hurt, held and helped, and when. The reusable pieces come from docs/SPELL_KIT.md: combat/Reel (the charge), combat/Dome (Protection
 * and the Cage), status/SupernaturalResistance, status/Substitute + status/EmpoweredHit (Riposte),
 * status/DamageLink (Damage Transference: a redirect from each ally to the guardian), beyonder/Physique (the
 * body passives and the Grandmaster's reach and speed), root/Root, combat/Spells, combat/Later.
 *
 * The passives that are a potion effect or a check once a second are in SequencePassives; the ones that react to
 * hits and mobs (Tracking, Spiritual Intuition, Divine Touch, Twilight Resistance) and the body (Physique) are here.
 * Nothing here breaks blocks or lights real fire. The numbers for every ability are right below.
 */
public class HandOfGod {

    public static final String PATHWAY = "Hand Of God";

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
    private static final double CHARGE_SHOVE = 0.7;              // ...and those you brush past are shoved aside
    // sneak: Divine Blood
    private static final int BLOOD_COST = 250;
    private static final float BLOOD_HEAL = 0.4f;                // of your maximum health, at once
    private static final int BLOOD_TICKS = 200;                  // Strength II and Speed II this long
    private static final int BLOOD_LEVEL = 1;

    // Ability 2 - Gladiator's Flurry (Sequence 8)
    private static final int FLURRY_SEQUENCE = 8;
    private static final int FLURRY_COST = 300;
    private static final double FLURRY_RANGE = 4.0;
    private static final int FLURRY_BLOWS = 5;
    private static final int FLURRY_EVERY = 3;                   // ticks between two blows
    private static final float FLURRY_DAMAGE = 8f;               // each blow
    private static final int FLURRY_STAGGER = 6;                 // each blow holds it this long...
    private static final double FLURRY_THROW = 1.1;              // ...and the last one throws it
    private static final double FLURRY_LIFT = 0.35;
    // sneak: Supernatural Resistance
    private static final int RESIST_COST = 300;
    private static final int RESIST_TICKS = 160;
    private static final float RESIST_CUT = 0.4f;                // supernatural damage 40% less (status/SupernaturalResistance)

    // Ability 3 - Grandmaster's Edge (Sequence 7)
    private static final int EDGE_SEQUENCE = 7;
    private static final int EDGE_COST = 700;
    private static final int EDGE_TICKS = 300;
    private static final double EDGE_ATTACK_SPEED = 0.5;         // swings 50% faster...
    private static final double EDGE_REACH = 1.5;                // ...and reach this much further
    private static final double EDGE_ARC_RANGE = 4.0;            // each swing that lands also cuts everyone in front...
    private static final double EDGE_ARC_ANGLE = 60.0;           // ...this many degrees to each side...
    private static final float EDGE_ARC_SHARE = 0.6f;            // ...for this much of the swing's damage
    private static final float EDGE_MIN_CHARGE = 0.7f;           // a swing with less of the attack bar does not count
    private static final int EDGE_COMBO_GAP = 30;                // the third swing in a row within this...
    private static final float EDGE_FINISHER = 24f;              // ...is a finishing cut with this much more
    // sneak: Riposte
    private static final int RIPOSTE_COST = 700;
    private static final int RIPOSTE_TICKS = 30;                 // the stance (status/Substitute: the next hit is turned aside)
    private static final int RIPOSTE_STAGGER = 30;               // the attacker cannot move or strike this long
    private static final int RIPOSTE_WINDOW = 40;                // your next hit within this long is the counter...
    private static final float RIPOSTE_COUNTER = 30f;            // ...with this much more

    // Ability 5 - Protection (Sequence 5)
    private static final int PROTECTION_SEQUENCE = 5;
    private static final int PROTECTION_COST = 3000;             // Sequence 5 holds 15500
    private static final int PROTECTION_COOLDOWN = 600;
    private static final double PROTECTION_RADIUS = 6.0;
    private static final int PROTECTION_TICKS = 300;
    private static final float PROTECTION_SHIELD = 0.6f;         // allies inside take 60% less damage
    private static final int PROTECTION_WEAKEN = 60;             // each of your attacks weakens it this long
    // Glory (2): it closes into a Cage
    private static final int CAGE_SEQUENCE = 2;
    private static final double CAGE_RADIUS = 7.0;
    private static final int CAGE_TICKS = 600;                   // at most; it falls once you are further than...
    private static final double CAGE_LEASH = 10.0;               // ...this from its shell
    // Hand of God (1): teleports, concealment and gliding cannot cross it; sneak inside it mends it
    private static final int WARD_SEQUENCE = 1;
    private static final int MEND_COST = 1500;
    // sneak: Damage Transference
    private static final int TRANSFER_COST = 2500;
    private static final int TRANSFER_COOLDOWN = 600;
    private static final int TRANSFER_TICKS = 400;
    private static final double TRANSFER_RADIUS = 16.0;
    private static final float TRANSFER_SHARE = 0.5f;            // half of what each ally takes lands on you instead
    private static final double TRANSFER_LEASH = 24.0;           // an ally further than this is let go
    private static final int TRANSFER_MOST_RESISTANCE = 2;       // Resistance I for one ally ... III for three or more

    // Passives that need more than SequencePassives' check once a second
    private static final int MASTERY_SEQUENCE = 7;               // Weapon Mastery: faster attacks, a little more reach
    private static final double MASTERY_ATTACK_SPEED = 0.1;
    private static final double MASTERY_REACH = 0.5;
    private static final int PHYSIQUE_SEQUENCE = 6;              // Giant's Physique: taller, two more hearts
    private static final double PHYSIQUE_SCALE = 0.15;
    private static final double PHYSIQUE_HEALTH = 4.0;
    private static final int FOOTING_SEQUENCE = 5;               // Guardian: half knockback (Resistance I is in SequencePassives)
    private static final double FOOTING = 0.5;
    private static final int TRACKING_SEQUENCE = 4;              // Tracking: enemies you hit leave footprints only you see
    private static final int TRACKING_TICKS = 400;
    private static final double TRACKING_STEP = 0.9;
    private static final int INTUITION_SEQUENCE = 4;             // Spiritual Intuition: a chime when something starts hunting you unseen
    private static final int INTUITION_REST = 60;
    private static final int TWILIGHT_RESISTANCE_SEQUENCE = 2;   // wither does not take (no healing stopped: SequencePassives)
    private static final int DIVINE_TOUCH_SEQUENCE = 1;          // Divine Touch: bare-handed blows decay
    private static final float DIVINE_TOUCH_DAMAGE = 6f;
    private static final int DIVINE_TOUCH_NO_HEAL = 60;
    // used from SequencePassives
    public static final float INNATE_RESIST_CUT = 0.25f;          // Pugilist: supernatural damage 25% less
    private static final double DAWN_AURA_RADIUS = 6.0;          // Silver Knight: evil this close slowly burns
    private static final float DAWN_AURA_DAMAGE = 1.5f;
    private static final double TWILIGHT_AURA_RADIUS = 16.0;     // Twilight Giant: enemies this close are slowed,
                                                                 // weakened and cannot heal

    // =========================
    // Saved on the player (game times are "until" this tick)
    // =========================
    private static final String CHARGING_UNTIL = "hand_of_god_charging_until";
    private static final String BLOOD_UNTIL = "hand_of_god_blood_until";
    private static final String FLURRY_UNTIL = "hand_of_god_flurry_until";
    private static final String RESIST_UNTIL = "hand_of_god_resist_until";
    private static final String EDGE_UNTIL = "hand_of_god_edge_until";
    private static final String EDGE_COMBO = "hand_of_god_edge_combo";
    private static final String EDGE_LAST = "hand_of_god_edge_last";
    private static final String RIPOSTE_UNTIL = "hand_of_god_riposte_until";
    private static final String PROTECTION_READY = "hand_of_god_protection_ready";
    private static final String DOME_TAG = "hand_of_god_dome_tag";
    private static final String TRANSFER_READY = "hand_of_god_transfer_ready";
    private static final String INTUITION_READY = "hand_of_god_intuition_ready";

    // Names of the parts that a player has one of
    private static final String DOME = "hand_of_god_protection";
    private static final String RESIST_GRANT = "hand_of_god_resistance";
    private static final String INNATE_GRANT = "hand_of_god_innate";
    private static final String RIPOSTE_COUNTER_HIT = "hand_of_god_riposte";

    // Attribute modifiers (beyonder/Physique)
    private static final Identifier MASTERY_SPEED_ID = Physique.id("hand_of_god_mastery_speed");
    private static final Identifier MASTERY_REACH_ID = Physique.id("hand_of_god_mastery_reach");
    private static final Identifier PHYSIQUE_SCALE_ID = Physique.id("hand_of_god_physique_scale");
    private static final Identifier PHYSIQUE_HEALTH_ID = Physique.id("hand_of_god_physique_health");
    private static final Identifier FOOTING_ID = Physique.id("hand_of_god_footing");
    private static final Identifier EDGE_SPEED_ID = Physique.id("hand_of_god_edge_speed");
    private static final Identifier EDGE_REACH_ID = Physique.id("hand_of_god_edge_reach");

    /** A real swing of a player (not an ability's strike), waiting for the damage it does in the same tick. Not saved. */
    private record Swing(UUID target, long tick, float charge) {}
    private static final Map<UUID, Swing> SWINGS = new HashMap<>();

    /** Who each guardian covers with Damage Transference. Not saved. */
    private record Cover(List<UUID> allies, long until, String tag) {}
    private static final Map<UUID, Cover> COVERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_PASS_SHOWN = new HashMap<>();

    /** Enemies a Demon Hunter hit and is tracking: target -> track. Not saved. */
    private static final class Track {
        final UUID hunter;
        long until;
        Vec3 lastPrint;

        Track(UUID hunter, long until, Vec3 lastPrint) {
            this.hunter = hunter;
            this.until = until;
            this.lastPrint = lastPrint;
        }
    }
    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class HandOfGodEvents {

        static {
            // Glory's Twilight Resistance: one who holds the authority of Decay does not rot (status/Decay)
            Decay.addImmunity(entity -> entity instanceof Player player && isHandOfGod(player, TWILIGHT_RESISTANCE_SEQUENCE));
        }

        @SubscribeEvent
        public static void onHandOfGodTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            // these run out even if the Pathway is taken away in the meantime (or the cast was stolen)
            if (player.tickCount % 5 == 0 && isActive(player, EDGE_UNTIL)) HandOfGodFx.edgeTick(sl, player);
            if (player.tickCount % 5 == 0) trackingTick(player, sl);
            HandOfGodDemonHunting.tick(player, sl);
            if (player.tickCount % 20 == 0) {
                bodyTick(player);
                coverTick(player, sl);
                HandOfGodDawn.tick(player, sl);
            }
        }

        // LOWEST: whatever cancels the swing (a root, a concealed target) has had its say. A cancelled swing never gets here.
        @SubscribeEvent(priority = Priority.LOWEST)
        public static void onHandOfGodSwing(AttackEntityEvent event) {
            Player player = event.getEntity();
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getPathway(player).equals(PATHWAY) && !isActive(player, EDGE_UNTIL)) return;
            SWINGS.put(player.getUUID(), new Swing(event.getTarget().getUUID(), sl.getGameTime(), player.getAttackStrengthScale(0.5f)));
            if (Dome.isRaised(player, DOME)) weakenDome(player, sl);
        }

        @SubscribeEvent
        public static void onHandOfGodHurt(LivingHurtEvent event) {
            LivingEntity target = event.getEntity();
            if (!(target.level() instanceof ServerLevel sl)) return;
            HandOfGodTwilight.kingdomCut(event);                                 // an enemy in a Divine Kingdom deals less
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player) return;
            Swing swing = SWINGS.remove(player.getUUID());
            if (swing == null || !swing.target().equals(target.getUUID()) || swing.tick() != sl.getGameTime()) return;
            event.setAmount(landed(player, sl, target, event.getAmount(), swing.charge()));
        }

        // LOW: after DamageLink has passed part of the wound on, so the thread shows only what really moved
        @SubscribeEvent(priority = Priority.LOW)
        public static void onHandOfGodDamage(LivingDamageEvent event) {
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel sl) || COVERS.isEmpty()) return;
            LivingEntity guardian = DamageLink.redirectTarget(victim);
            if (guardian == null || !COVERS.containsKey(guardian.getUUID())) return;
            if (!COVERS.get(guardian.getUUID()).allies().contains(victim.getUUID())) return;
            long now = sl.getGameTime();
            Long shown = LAST_PASS_SHOWN.get(victim.getUUID());
            if (shown != null && now - shown < 10) return;
            LAST_PASS_SHOWN.put(victim.getUUID(), now);
            HandOfGodFx.transferenceTaken(sl, victim, guardian);
        }

        /** Spiritual Intuition: a mob starts hunting a Demon Hunter from where they cannot see it. */
        @SubscribeEvent
        public static void onHandOfGodTarget(LivingChangeTargetEvent event) {
            if (!(event.getNewTarget() instanceof ServerPlayer hunter) || event.getEntity().level().isClientSide()) return;
            LivingEntity mob = event.getEntity();
            if (!isHandOfGod(hunter, INTUITION_SEQUENCE)) return;
            long now = hunter.level().getGameTime();
            if (hunter.getPersistentData().getLongOr(INTUITION_READY, 0L) > now) return;
            Vec3 toMob = mob.position().subtract(hunter.position());
            boolean behind = toMob.horizontalDistanceSqr() > 0.01 && Spells.flatLook(hunter).dot(new Vec3(toMob.x, 0, toMob.z).normalize()) < 0.2;
            if (!behind && hunter.hasLineOfSight(mob)) return;                       // seen coming: no warning needed
            hunter.getPersistentData().putLong(INTUITION_READY, now + INTUITION_REST);
            HandOfGodFx.intuition(hunter);
        }

        /** Twilight Resistance: wither does not take. */
        @SubscribeEvent
        public static void onHandOfGodEffect(MobEffectEvent.Applicable event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
            if (event.getEffectInstance().getEffect() != MobEffects.WITHER) return;
            if (isHandOfGod(player, TWILIGHT_RESISTANCE_SEQUENCE)) event.setResult(Result.DENY);
        }

        // what was on the screen and the body does not survive relogging or dying: start clean
        @SubscribeEvent
        public static void onHandOfGodLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onHandOfGodRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onHandOfGodLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            Player player = event.getEntity();
            SWINGS.remove(player.getUUID());
            endCover(player, null);
            TRACKS.values().removeIf(track -> track.hunter.equals(player.getUUID()));
            HandOfGodDemonHunting.forget(player);
            HandOfGodTwilight.forget(player);
        }
    }

    // =========================
    // Ability 1 - Warrior's Charge
    // Normal: lower your shoulder and rush up to CHARGE_RANGE blocks (at the enemy you look at, if one is that close).
    //         The first enemy in your way takes CHARGE_DAMAGE and is thrown back, which ends the rush; everything
    //         you brush past on the way is shoved aside. No fall damage until you land.
    // Sneak:  Divine Blood. You heal BLOOD_HEAL of your maximum health at once and get Strength II and Speed II
    //         for BLOOD_TICKS.
    // =========================
    public static void handOfGodWarriorsCharge(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, CHARGE_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, BLOOD_COST)) return;
            divineBlood(player, sl);
            return;
        }
        if (isActive(player, CHARGING_UNTIL)) return;                   // already rushing: let the key bounce off
        if (Root.has(player, RootRestriction.MOVE)) {
            player.sendSystemMessage(Component.literal("You are held fast and cannot charge."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, CHARGE_COST)) return;
        charge(player, sl);
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

    /** Every tick of the rush: who is in the way (the first one is hit and the rush ends) and who is brushed past. */
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

    private static void divineBlood(Player player, ServerLevel sl) {
        long until = sl.getGameTime() + BLOOD_TICKS;
        player.getPersistentData().putLong(BLOOD_UNTIL, until);
        player.heal(player.getMaxHealth() * BLOOD_HEAL);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, BLOOD_TICKS, BLOOD_LEVEL));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, BLOOD_TICKS, BLOOD_LEVEL));
        String tag = "hog_blood_" + player.getId();
        HandOfGodFx.divineBloodEnd(sl, player, tag);                    // cast again: one glow, not two
        HandOfGodFx.divineBlood(sl, player, BLOOD_TICKS, tag);
        HandOfGodFx.status(player, Status.DIVINE_BLOOD, BLOOD_TICKS);
    }

    // =========================
    // Ability 2 - Gladiator's Flurry
    // Normal: FLURRY_BLOWS very fast blows on the enemy in front of you (within FLURRY_RANGE), FLURRY_EVERY ticks
    //         apart. Each one staggers it; the last one throws it back. Works with fists or any weapon.
    // Sneak:  Supernatural Resistance. For RESIST_TICKS blindness, nausea, freezing, roots and taunts end on you
    //         at once and cannot be put on you, and supernatural damage (magic, explosions, abilities that name no
    //         attacker) is cut by RESIST_CUT.
    // =========================
    public static void handOfGodGladiatorsFlurry(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, FLURRY_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, RESIST_COST)) return;
            supernaturalResistance(player, sl);
            return;
        }
        if (isActive(player, FLURRY_UNTIL)) return;                     // still striking
        LivingEntity target = Spells.aimEnemy(player, sl, FLURRY_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one within reach of your fists."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, FLURRY_COST)) return;
        flurry(player, sl, target);
    }

    private static void flurry(Player player, ServerLevel sl, LivingEntity target) {
        player.getPersistentData().putLong(FLURRY_UNTIL, sl.getGameTime() + (long) FLURRY_BLOWS * FLURRY_EVERY);
        for (int i = 0; i < FLURRY_BLOWS; i++) {
            int blow = i;
            Later.run(sl, i * FLURRY_EVERY, () -> {
                if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
                if (player.distanceTo(target) > FLURRY_RANGE + 2) return;               // it got away
                boolean last = blow == FLURRY_BLOWS - 1;
                Spells.strike(player, target, FLURRY_DAMAGE);
                HandOfGodFx.flurryBlow(sl, player, target, last ? 4 : Math.min(blow, 3));
                if (last) {
                    Spells.push(target, player.position(), FLURRY_THROW, FLURRY_LIFT);
                } else {
                    Root.apply(target, FLURRY_STAGGER, RootRestriction.MOVE);
                }
            });
        }
    }

    private static void supernaturalResistance(Player player, ServerLevel sl) {
        long until = sl.getGameTime() + RESIST_TICKS;
        player.getPersistentData().putLong(RESIST_UNTIL, until);
        String tag = "hog_resist_" + player.getId();
        SupernaturalResistance.grant(player, RESIST_GRANT, RESIST_TICKS, RESIST_CUT, true,
                shaken -> HandOfGodFx.resistanceShrug(sl, player));
        HandOfGodFx.resistanceEnd(sl, player, tag);                     // cast again: one seal, not two
        HandOfGodFx.resistance(sl, player, RESIST_TICKS, tag);
        HandOfGodFx.status(player, Status.RESISTANCE, RESIST_TICKS);
        Later.run(sl, RESIST_TICKS, () -> {
            if (player.getPersistentData().getLongOr(RESIST_UNTIL, 0L) != until) return;      // cast again since
            player.getPersistentData().remove(RESIST_UNTIL);
        });
    }

    // =========================
    // Ability 3 - Grandmaster's Edge
    // Normal: for EDGE_TICKS whatever you hold is used like a grandmaster's weapon: EDGE_ATTACK_SPEED faster swings,
    //         EDGE_REACH more reach, and every swing that lands also cuts every enemy in front of you (EDGE_ARC_SHARE of
    //         its damage). The third swing in a row within EDGE_COMBO_GAP is a finishing cut: EDGE_FINISHER more.
    // Sneak:  Riposte. A parrying stance for RIPOSTE_TICKS: the next blow or projectile is turned aside, its source
    //         is staggered for RIPOSTE_STAGGER, and your next hit within RIPOSTE_WINDOW is a counter (RIPOSTE_COUNTER more).
    // =========================
    public static void handOfGodGrandmastersEdge(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, EDGE_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, RIPOSTE_COST)) return;
            riposte(player, sl);
            return;
        }
        if (!Spells.paySpirituality(player, EDGE_COST)) return;
        grandmastersEdge(player, sl);
    }

    private static void grandmastersEdge(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        data.putLong(EDGE_UNTIL, sl.getGameTime() + EDGE_TICKS);
        data.putInt(EDGE_COMBO, 0);
        edgeBody(player, true);                                          // at once; bodyTick keeps it and takes it off later
        HandOfGodFx.grandmastersEdge(sl, player, EDGE_TICKS);
        HandOfGodFx.status(player, Status.GRANDMASTER, EDGE_TICKS);
    }

    private static void edgeBody(Player player, boolean on) {
        Physique.keep(player, Attributes.ATTACK_SPEED, EDGE_SPEED_ID, EDGE_ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, on);
        Physique.keep(player, Attributes.ENTITY_INTERACTION_RANGE, EDGE_REACH_ID, EDGE_REACH, AttributeModifier.Operation.ADD_VALUE, on);
    }

    /** A swing of the Grandmaster's Edge landed: the combo, the arc in front, the finisher. Returns the damage. */
    private static float edgeSwing(Player player, ServerLevel sl, LivingEntity target, float amount, float charge) {
        if (charge < EDGE_MIN_CHARGE) return amount;                    // a spam click is no grandmaster's cut
        CompoundTag data = player.getPersistentData();
        long now = sl.getGameTime();
        int combo = now - data.getLongOr(EDGE_LAST, 0L) <= EDGE_COMBO_GAP ? data.getIntOr(EDGE_COMBO, 0) + 1 : 1;
        data.putLong(EDGE_LAST, now);
        boolean finisher = combo >= 3;
        data.putInt(EDGE_COMBO, finisher ? 0 : combo);
        if (finisher) HandOfGodFx.edgeFinisher(sl, player, target);
        else HandOfGodFx.edgeSwing(sl, player, combo - 1);

        // the arc: a tick later, so these strikes are not taken for the swing itself
        float arc = amount * EDGE_ARC_SHARE;
        UUID struck = target.getUUID();
        Later.run(sl, 1, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity enemy : Spells.enemiesInCone(player, sl, player.getEyePosition(), Spells.flatLook(player),
                    EDGE_ARC_RANGE, EDGE_ARC_ANGLE)) {
                if (enemy.getUUID().equals(struck)) continue;
                Spells.strike(player, enemy, arc);
            }
        });
        return finisher ? amount + EDGE_FINISHER : amount;
    }

    private static void riposte(Player player, ServerLevel sl) {
        long until = sl.getGameTime() + RIPOSTE_TICKS;
        player.getPersistentData().putLong(RIPOSTE_UNTIL, until);
        HandOfGodFx.riposte(sl, player);
        HandOfGodFx.status(player, Status.RIPOSTE, RIPOSTE_TICKS);
        Substitute.arm(player, RIPOSTE_TICKS, 1, (saved, source, amount, level) -> parry(player, level, source.getEntity(), source.getDirectEntity()));
    }

    /** The blow was turned aside: its source reels, and the next hit is the counter. */
    private static void parry(Player player, ServerLevel sl, Entity attacker, Entity direct) {
        player.getPersistentData().remove(RIPOSTE_UNTIL);
        HandOfGodFx.riposteParry(sl, player, direct != null ? direct : attacker);
        if (direct instanceof Projectile projectile) projectile.discard();
        if (attacker instanceof LivingEntity living && living != player && living.level() == sl) {
            Root.apply(living, RIPOSTE_STAGGER, RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.ATTACK);
            if (living.distanceTo(player) < 6) Spells.push(living, player.position(), 0.6, 0.2);
        }
        HandOfGodFx.statusEnd(player, Status.RIPOSTE);
        EmpoweredHit.arm(player, RIPOSTE_COUNTER_HIT, RIPOSTE_WINDOW, (striker, target, amount, level) -> {
            HandOfGodFx.riposteCounter(level, striker, target);
            return amount + RIPOSTE_COUNTER;
        });
    }

    // =========================
    // Ability 5 - Protection
    // Normal: drive your sword into the ground: a dome of Dawn light PROTECTION_RADIUS round you for PROTECTION_TICKS
    //         (combat/Dome). Enemies are kept out, projectiles from outside end at the shell, allies inside take
    //         PROTECTION_SHIELD less damage and no harmful effects. You are braced and cannot leave it; each of your
    //         attacks weakens it for PROTECTION_WEAKEN (projectiles go through, the shield is halved).
    //         Glory (2): a Cage instead, a whole sphere that also holds the enemies in it and lasts while you stay
    //         within CAGE_LEASH of it. Hand of God (1): concealed enemies cannot cross it either, and an orange
    //         layer shows outside. The key again while it stands lowers it (free).
    // Sneak:  Damage Transference. For TRANSFER_TICKS half of what each ally within TRANSFER_RADIUS takes lands on
    //         you instead, and you get Resistance for each ally you cover. Hand of God (1), standing in your own
    //         Protection: the sword is driven in again and the dome is whole and new (MEND_COST).
    // =========================
    public static void handOfGodProtection(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, PROTECTION_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (Beyonder.hasSequence(player, WARD_SEQUENCE) && Dome.isInside(player, DOME, player)) {
                if (!Spells.paySpirituality(player, MEND_COST)) return;
                mendDome(player, sl);
                return;
            }
            if (coolingDown(player, sl, TRANSFER_READY, "Damage Transference")) return;
            List<LivingEntity> allies = Spells.alliesAround(player, sl, player.position(), TRANSFER_RADIUS);
            if (allies.isEmpty()) {
                player.sendSystemMessage(Component.literal("There is no one near you to stand in front of."));
                return; // nothing spent
            }
            if (!Spells.paySpirituality(player, TRANSFER_COST)) return;
            player.getPersistentData().putLong(TRANSFER_READY, sl.getGameTime() + TRANSFER_COOLDOWN);
            transference(player, sl, allies);
            return;
        }
        if (Dome.isRaised(player, DOME)) {                               // the key again: it is lowered
            Dome.close(player, DOME);
            return;
        }
        if (coolingDown(player, sl, PROTECTION_READY, "Protection")) return;
        if (!Spells.paySpirituality(player, PROTECTION_COST)) return;
        player.getPersistentData().putLong(PROTECTION_READY, sl.getGameTime() + PROTECTION_COOLDOWN);
        if (Beyonder.hasSequence(player, CAGE_SEQUENCE)) cage(player, sl);
        else protection(player, sl);
    }

    private static void protection(Player player, ServerLevel sl) {
        Vec3 ground = Spells.groundUnder(player, sl, player);
        boolean ward = Beyonder.hasSequence(player, WARD_SEQUENCE);
        Dome dome = Dome.at(ground, PROTECTION_RADIUS).lasts(PROTECTION_TICKS)
                .keepsEnemiesOut().stopsProjectiles().shields(PROTECTION_SHIELD).wardsEffects().holdsOwner()
                .onBlock(at -> HandOfGodFx.domeHit(sl, at))
                .onEnd(() -> {
                    HandOfGodFx.protectionEnd(sl, ground, player.getPersistentData().getStringOr(DOME_TAG, ""));
                    HandOfGodFx.statusEnd(player, Status.PROTECTION);
                    player.getPersistentData().remove(DOME_TAG);
                });
        if (ward) dome = dome.outsiders(entity -> !Spells.isFriend(player, entity));
        dome.raise(player, sl, DOME);
        showProtection(player, sl, ground, PROTECTION_TICKS, false);
    }

    private static void cage(Player player, ServerLevel sl) {
        Vec3 middle = Spells.groundUnder(player, sl, player).add(0, CAGE_RADIUS * 0.35, 0);
        boolean ward = Beyonder.hasSequence(player, WARD_SEQUENCE);
        Dome dome = Dome.sphere(middle, CAGE_RADIUS).lasts(CAGE_TICKS)
                .keepsEnemiesIn().stopsProjectiles().shields(PROTECTION_SHIELD).wardsEffects()
                .keepWhile(() -> player.position().distanceTo(middle) <= CAGE_RADIUS + CAGE_LEASH)
                .onBlock(at -> HandOfGodFx.domeHit(sl, at))
                .onEnd(() -> {
                    HandOfGodFx.cageEnd(sl, middle, player.getPersistentData().getStringOr(DOME_TAG, ""));
                    HandOfGodFx.statusEnd(player, Status.CAGE);
                    player.getPersistentData().remove(DOME_TAG);
                });
        if (ward) dome = dome.outsiders(entity -> !Spells.isFriend(player, entity));
        dome.raise(player, sl, DOME);
        showProtection(player, sl, middle, CAGE_TICKS, true);
    }

    /** The look of the dome (or cage) for 'ticks', under a new tag kept on the player (the mend replaces it). */
    private static void showProtection(Player player, ServerLevel sl, Vec3 center, int ticks, boolean cage) {
        String tag = "hog_dome_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putString(DOME_TAG, tag);
        boolean ward = Beyonder.hasSequence(player, WARD_SEQUENCE);
        if (cage) {
            HandOfGodFx.cage(sl, center, CAGE_RADIUS, ticks, tag);
            if (ward) HandOfGodFx.sphere(sl, center, CAGE_RADIUS + 0.5, 0x70FF7A1A, ticks, tag);
            HandOfGodFx.status(player, Status.CAGE, ticks);
        } else {
            HandOfGodFx.protection(sl, player, center, PROTECTION_RADIUS, ticks, tag);
            if (ward) HandOfGodFx.dome(sl, center, PROTECTION_RADIUS + 0.5, 0x70FF7A1A, ticks, tag);
            HandOfGodFx.status(player, Status.PROTECTION, ticks);
        }
    }

    /** Hand of God: sneak inside your own Protection or Cage, and it stands whole and new. */
    private static void mendDome(Player player, ServerLevel sl) {
        Vec3 center = Dome.center(player, DOME);
        if (center == null) return;
        boolean cage = Beyonder.hasSequence(player, CAGE_SEQUENCE);
        ParticleShapes.clearModels(sl, center, player.getPersistentData().getStringOr(DOME_TAG, ""));
        Dome.renew(player, DOME);
        showProtection(player, sl, center, cage ? CAGE_TICKS : PROTECTION_TICKS, cage);
        HandOfGodFx.domeHit(sl, center.add(0, 1, 0));
    }

    /** One of the guardian's attacks: the dome dims for a few seconds. */
    private static void weakenDome(Player player, ServerLevel sl) {
        Vec3 center = Dome.center(player, DOME);
        if (center == null) return;
        if (!Dome.isWeakened(player, DOME)) {
            HandOfGodFx.domeWeaken(sl, center, Beyonder.hasSequence(player, CAGE_SEQUENCE) ? CAGE_RADIUS : PROTECTION_RADIUS);
        }
        Dome.weaken(player, DOME, PROTECTION_WEAKEN);
    }

    private static void transference(Player player, ServerLevel sl, List<LivingEntity> allies) {
        endCover(player, sl);                                            // an older one first
        long until = sl.getGameTime() + TRANSFER_TICKS;
        String tag = "hog_aegis_" + player.getId() + "_" + sl.getGameTime();
        List<UUID> ids = new ArrayList<>();
        for (LivingEntity ally : allies) {
            DamageLink.redirect(ally, player, TRANSFER_SHARE, TRANSFER_TICKS);
            ids.add(ally.getUUID());
        }
        COVERS.put(player.getUUID(), new Cover(ids, until, tag));
        int resistance = Math.min(allies.size(), TRANSFER_MOST_RESISTANCE + 1) - 1;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, TRANSFER_TICKS, resistance));
        HandOfGodFx.transference(sl, player, allies, TRANSFER_TICKS, tag);
        HandOfGodFx.status(player, Status.TRANSFERENCE, TRANSFER_TICKS);
    }

    /** Once a second: the covered allies who went too far are let go, and it ends when its time is up. */
    private static void coverTick(Player player, ServerLevel sl) {
        Cover cover = COVERS.get(player.getUUID());
        if (cover == null) return;
        if (sl.getGameTime() >= cover.until()) {
            endCover(player, sl);
            return;
        }
        Iterator<UUID> iterator = cover.allies().iterator();
        while (iterator.hasNext()) {
            UUID id = iterator.next();
            Entity found = sl.getEntity(id);
            if (found instanceof LivingEntity ally && ally.isAlive() && ally.distanceTo(player) <= TRANSFER_LEASH) continue;
            if (found instanceof LivingEntity ally) {
                LivingEntity receiver = DamageLink.redirectTarget(ally);
                if (receiver != null && receiver.getUUID().equals(player.getUUID())) DamageLink.clearRedirect(ally);
                ParticleShapes.clearModels(sl, ally, cover.tag());
            }
            iterator.remove();
        }
    }

    /**
     * Damage Transference ends: every ally still covered is let go. sl == null: the guardian is leaving (logout,
     * death): only the links are cut, nothing is shown.
     */
    private static void endCover(Player player, ServerLevel sl) {
        Cover cover = COVERS.remove(player.getUUID());
        if (cover == null) return;
        List<LivingEntity> allies = new ArrayList<>();
        if (player.level() instanceof ServerLevel level) {
            for (UUID id : cover.allies()) {
                if (!(level.getEntity(id) instanceof LivingEntity ally)) continue;
                // the link names the guardian by UUID, so it would find their body again after a respawn
                if (DamageLink.redirectTarget(ally) != null && DamageLink.redirectTarget(ally).getUUID().equals(player.getUUID())) {
                    DamageLink.clearRedirect(ally);
                }
                allies.add(ally);
            }
        }
        if (sl == null) return;
        HandOfGodFx.transferenceEnd(sl, player, allies, cover.tag());
        HandOfGodFx.statusEnd(player, Status.TRANSFERENCE);
    }

    // =========================
    // A real swing landed (fist or weapon, not an ability): Grandmaster's Edge, Divine Touch, Tracking
    // =========================

    /** Returns the damage the swing does. */
    private static float landed(Player player, ServerLevel sl, LivingEntity target, float amount, float charge) {
        if (isActive(player, EDGE_UNTIL)) amount = edgeSwing(player, sl, target, amount, charge);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return amount;
        if (Beyonder.hasSequence(player, DIVINE_TOUCH_SEQUENCE) && player.getMainHandItem().isEmpty()) {
            amount += DIVINE_TOUCH_DAMAGE;
            NoHeal.apply(target, DIVINE_TOUCH_NO_HEAL);
            HandOfGodFx.divineTouch(sl, target);
        }
        if (Beyonder.hasSequence(player, TRACKING_SEQUENCE) && Spells.isEnemy(player, target)) {
            Track track = TRACKS.get(target.getUUID());
            long until = sl.getGameTime() + TRACKING_TICKS;
            if (track != null && track.hunter.equals(player.getUUID())) track.until = until;
            else TRACKS.put(target.getUUID(), new Track(player.getUUID(), until, target.position()));
        }
        return amount;
    }

    /** Every few ticks for a Demon Hunter: a footprint (only they see) where each hunted enemy stepped. */
    private static void trackingTick(Player player, ServerLevel sl) {
        if (TRACKS.isEmpty() || !(player instanceof ServerPlayer hunter)) return;
        long now = sl.getGameTime();
        Iterator<Map.Entry<UUID, Track>> iterator = TRACKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Track> entry = iterator.next();
            Track track = entry.getValue();
            if (!track.hunter.equals(player.getUUID())) continue;
            if (track.until < now || !(sl.getEntity(entry.getKey()) instanceof LivingEntity prey) || !prey.isAlive()) {
                iterator.remove();
                continue;
            }
            if (!prey.onGround() || prey.position().distanceTo(track.lastPrint) < TRACKING_STEP) continue;
            track.lastPrint = prey.position();
            HandOfGodFx.footprint(hunter, prey.position(), prey.getYRot());
        }
    }

    // =========================
    // Passives (the potion effects and the checks once a second are in SequencePassives)
    // =========================

    /**
     * Once a second, for every player: the body. Weapon Mastery (7), Giant's Physique (6) and the Guardian's footing
     * (5) while they are this Pathway at that Sequence, the Grandmaster's Edge while it lasts; off otherwise.
     */
    private static void bodyTick(Player player) {
        boolean mine = SoulCore.getPathway(player).equals(PATHWAY);
        boolean mastery = mine && Beyonder.hasSequence(player, MASTERY_SEQUENCE);
        boolean physique = mine && Beyonder.hasSequence(player, PHYSIQUE_SEQUENCE);
        boolean footing = mine && Beyonder.hasSequence(player, FOOTING_SEQUENCE);
        Physique.keep(player, Attributes.ATTACK_SPEED, MASTERY_SPEED_ID, MASTERY_ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, mastery);
        Physique.keep(player, Attributes.ENTITY_INTERACTION_RANGE, MASTERY_REACH_ID, MASTERY_REACH, AttributeModifier.Operation.ADD_VALUE, mastery);
        Physique.keep(player, Attributes.SCALE, PHYSIQUE_SCALE_ID, PHYSIQUE_SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, physique);
        Physique.keep(player, Attributes.MAX_HEALTH, PHYSIQUE_HEALTH_ID, PHYSIQUE_HEALTH, AttributeModifier.Operation.ADD_VALUE, physique);
        Physique.keep(player, Attributes.KNOCKBACK_RESISTANCE, FOOTING_ID, FOOTING, AttributeModifier.Operation.ADD_VALUE, footing);
        edgeBody(player, isActive(player, EDGE_UNTIL));
    }

    /** Pugilist (8): supernatural damage 25% less, blindness and nausea do not take. Run once a second. */
    public static void innateResistance(Player player) {
        SupernaturalResistance.grant(player, INNATE_GRANT, 40, INNATE_RESIST_CUT, false, null);
    }

    /** Silver Knight (3): evil and undead within DAWN_AURA_RADIUS slowly burn. Run once a second. */
    public static void dawnAura(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        HandOfGodFx.dawnAura(sl, player, DAWN_AURA_RADIUS);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), DAWN_AURA_RADIUS)) {
            if (!Spells.isEvil(enemy)) continue;
            Spells.strike(player, enemy, DAWN_AURA_DAMAGE);
            HandOfGodFx.lightOfDawnBurn(sl, enemy);
        }
    }

    /** Glory (2): nothing stops your healing, wither ends at once and nothing rots you (it cannot be put on either). */
    public static void twilightResistance(Player player) {
        player.removeEffect(MobEffects.WITHER);
        if (NoHeal.isApplied(player)) NoHeal.remove(player);
        Decay.cure(player);
    }

    /** Twilight Giant (0): enemies within TWILIGHT_AURA_RADIUS are slowed and weakened and cannot heal. Run once a second. */
    public static void twilightAura(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), TWILIGHT_AURA_RADIUS)) {
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, 0, 60);
            Spells.keepEffect(enemy, MobEffects.WEAKNESS, 0, 60);
            NoHeal.apply(enemy, 40);
        }
    }

    /** True for this Pathway at this Sequence or stronger. */
    static boolean isHandOfGod(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True while the game time saved under 'key' is still ahead. */
    static boolean isActive(Entity entity, String key) {
        return entity.getPersistentData().getLongOr(key, 0L) > entity.level().getGameTime();
    }

    /** True (and says so) while the cooldown saved under 'key' runs. */
    private static boolean coolingDown(Player player, ServerLevel sl, String key, String name) {
        long left = player.getPersistentData().getLongOr(key, 0L) - sl.getGameTime();
        if (left <= 0) return false;
        player.sendSystemMessage(Component.literal(name + " is not ready yet (" + ((left + 19) / 20) + "s)."));
        return true;
    }

    /** After a relog or a death: nothing of the timed casts is left over (their looks did not survive either). */
    private static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(CHARGING_UNTIL, BLOOD_UNTIL, FLURRY_UNTIL, RESIST_UNTIL, EDGE_UNTIL, EDGE_COMBO,
                EDGE_LAST, RIPOSTE_UNTIL, DOME_TAG)) {
            data.remove(key);
        }
        SWINGS.remove(player.getUUID());
        endCover(player, null);
        edgeBody(player, false);
        HandOfGodDawn.clearState(player);
        HandOfGodDemonHunting.clearState(player);
        HandOfGodTwilight.clearState(player);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) HandOfGodFx.statusEnd(player, status);       // a clock left running before death
    }
}
