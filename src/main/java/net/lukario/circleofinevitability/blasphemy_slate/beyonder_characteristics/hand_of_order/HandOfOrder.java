package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderFx.Status;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Disorder;
import net.lukario.circleofinevitability.combat.HudCounter;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.MeleeCombo;
import net.lukario.circleofinevitability.combat.Residue;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Chosen;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.EmpoweredHit;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.Fear;
import net.lukario.circleofinevitability.status.Forgotten;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
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
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Hand Of Order: the Justiciar Pathway of Lord of the Mysteries, the Judgment card (docs/HAND_OF_ORDER.md). Seven keys
 * in the Attendant Of Mysteries pattern, from Arbiter (Sequence 9) up to Justiciar (Sequence 0); every ability has a
 * normal and a sneak cast:
 *
 *   1 Authority       (Arbiter)              Halt: one word, the enemy you look at is held and hunters forget you
 *                                                                              | sneak: Presence (from 4: Intimidation)
 *   2 Jurisdiction    (Sheriff)              claim a circle of ground: you sense enemies in it and Order costs less
 *                                                                              | sneak: Recognition
 *   3 Interrogation   (Interrogator)         the wheel: Psychic Lashing, Psychic Piercing, Whip of Pain
 *                                            (HandOfOrderInterrogation)        | sneak: the last pick again
 *   4 Verdict         (Judge)                the wheel: Imprison (Confinement from 4), Exile, Death, Maintain
 *                                            Secrecy (6), Deprivation, Execution (4) (HandOfOrderVerdicts) | sneak: the last pick
 *   5 Decree          (Judge)                the wheel: Prohibition: Passage, Prohibition: Concealment, Imperative:
 *                                            Reality, Contract (4) (HandOfOrderDecrees)          | sneak: the last pick
 *   6 Judgment        (Disciplinary Paladin) Punishment: name the Convict   | sneak: Sword of Judgment (Chaos Hunter)
 *   7 Order           (Balancer)             the wheel: Balance, Divide the Battlefield, Law: No Return (2), Order
 *                                            Proxy, Quell Disorder (1), Court of Judgment (0) (HandOfOrderOrder) | sneak: the last pick
 *
 * The parts of the kit it leans on, any Pathway can use too: status/Chosen (the Convict, the one Recognized),
 * combat/Disorder (what the Sword of Judgment strikes hardest), status/Forgotten (Halt, Maintain Secrecy),
 * status/KeySeal (Deprivation), combat/Zone (Jurisdiction), status/WeaponCoat (Psychic Lashing), status/Exile (the void),
 * status/Decree (every Rule, Violation and Law), combat/Dome (Divide the Battlefield).
 * Every cast's cost goes through costFor / pay (a Jurisdiction makes Order cheaper; a Decree's through decreeCostFor)
 * and every wheel's cooldown through cooldownFor. Order Proxy's "free, no cooldown" is a Spells.addCostScale and a
 * Cooldowns.addScale (below, in HandOfOrderEvents), so it reaches every key and every wheel at once.
 *
 * Every look and sound is in HandOfOrderFx (see /orderfx); this class only decides who is held, hurt and judged, and
 * when. Nothing here breaks blocks or lights real fire. The numbers for every ability are below.
 */
public class HandOfOrder {

    public static final String PATHWAY = "Hand Of Order";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Halt (Sequence 9)
    static final int HALT_SEQUENCE = 9;
    static final int HALT_COST = 250;
    static final int HALT_COOLDOWN = 1;
    static final double HALT_RANGE = 16.0;
    static final int HALT_TICKS = 40;                    // held this long (a player: HALT_PLAYER_TICKS)
    static final int HALT_PLAYER_TICKS = 20;
    static final int HALT_JUDGE_TICKS = 60;              // from Judge (6)
    static final int HALT_JUDGE_PLAYER_TICKS = 30;
    static final double HALT_FORGET_RADIUS = 32.0;       // mobs this close that hunt you...
    static final int HALT_FORGET_TICKS = 100;            // ...cannot pick you again for this long
    // sneak: Presence
    static final int PRESENCE_COST = 400;
    static final int PRESENCE_COOLDOWN = 1;
    static final double PRESENCE_RADIUS = 8.0;
    static final double PRESENCE_JUDGE_RADIUS = 12.0;    // from Judge (6)
    static final int PRESENCE_TICKS = 120;               // Slowness II and Weakness I this long
    static final float COWED_MAX_HEALTH = 60f;           // mobs under this max health...
    static final int COWED_TICKS = 60;                   // ...cannot attack for this long
    static final int INTIMIDATION_SEQUENCE = 4;          // Imperative Mage: everyone in the wave is also rooted...
    static final int INTIMIDATION_TICKS = 30;            // ...this long

    // Ability 2 - Jurisdiction (Sequence 8)
    static final int JUDGE_SEQUENCE = 6;                         // many things grow at Judge
    static final int JURISDICTION_SEQUENCE = 8;
    static final int JURISDICTION_COST = 1250;
    static final int JURISDICTION_COOLDOWN = 1;
    static final int JURISDICTION_TICKS = 2400;
    static final double JURISDICTION_RADIUS = 24.0;
    static final double JURISDICTION_JUDGE_RADIUS = 48.0;        // from Judge (6)
    static final int JURISDICTION_CITY_SEQUENCE = 3;             // Chaos Hunter: a whole city
    static final double JURISDICTION_CITY_RADIUS = 96.0;
    static final int TWO_JURISDICTIONS_SEQUENCE = 1;             // Hand of Order: two at once
    static final float JURISDICTION_DISCOUNT = 0.8f;             // inside: your Order costs 20% less...
    static final float JURISDICTION_DECREES_LONGER = 1.25f;      // ...and your Decrees last 25% longer
    // sneak: Recognition
    static final int RECOGNITION_COST = 600;
    static final int RECOGNITION_COOLDOWN = 1;
    static final double RECOGNITION_RANGE = 48.0;
    static final int RECOGNITION_TICKS = 1200;
    static final double RECOGNITION_SHOWN = 64.0;                // its distance and its trail are shown within this
    static final int RECOGNITION_DETAILS_SEQUENCE = 4;           // Imperative Mage: its Pathway, Sequence, effects;
    static final double RECOGNITION_CONCEALED = 6.0;             // a concealed one still seen this close
    static final int RECOGNITION_DISORDER_SEQUENCE = 3;          // Chaos Hunter: its Disorder

    // Ability 6 - Punishment (Sequence 5)
    static final int PUNISHMENT_SEQUENCE = 5;
    static final int PUNISHMENT_COST = 3000;
    static final int PUNISHMENT_COOLDOWN = 1;
    static final double PUNISHMENT_RANGE = 24.0;
    static final int PUNISHMENT_TICKS = 500;
    static final float CONVICT_MORE_DAMAGE = 1.3f;               // you deal 30% more to the Convict...
    static final double CONVICT_SLOW = -0.15;                    // ...it is 15% slower and glows through walls
    static final int PUNISHMENT_WAIT = 160;                      // cast at nothing: the next enemy to hit you within this
    // sneak: Sword of Judgment
    static final int SWORD_SEQUENCE = 3;
    static final int SWORD_COST = 12000;
    static final int SWORD_COOLDOWN = 1;
    static final double SWORD_RANGE = 32.0;                      // doubled from Balancer (2); doubled again inside
    static final int SWORD_RANGE_SEQUENCE = 2;                   // your Jurisdiction (from Chaos Hunter)
    static final float SWORD_DAMAGE = 80f;
    static final float SWORD_PER_DISORDER = 0.15f;               // +15% for each point of Disorder...
    static final float SWORD_MAX = 2.5f;                         // ...up to this many times

    // Passives (docs/HAND_OF_ORDER.md, "Passives"); the once-a-second ones run from SequencePassives
    static final double ORDER_RADIUS = 5.0;                      // Arbiter: weak mobs this close keep their distance
    static final float ORDER_WEAK_HEALTH = 30f;
    static final int PROFICIENCY_SEQUENCE = 8;                   // Weapon Proficiency: faster attacks, more reach
    static final double PROFICIENCY_ATTACK_SPEED = 0.1;
    static final double PROFICIENCY_REACH = 0.5;
    static final int INTUITION_SEQUENCE = 8;                     // Sheriff's Intuition: a chime when hunted unseen
    static final int INTUITION_REST = 60;
    static final int DEMOLITION_SEQUENCE = 7;                    // Demolition Proficiency: explosions hurt this much less
    static final float DEMOLITION_CUT = 0.5f;
    static final double AURA_RADIUS = 6.0;                       // Judge's Aura: enemies this close deal 10% less
    static final float AURA_CUT = 0.1f;
    static final int RESTRAINT_SEQUENCE = 5;                     // Self-Restraint: mind effects last half as long
    static final float RESTRAINT_SHORTER = 0.5f;
    static final int BEYONDER_SENSE_EVERY = 40;                  // Beyonder Sense (4): Beyonders this close are marked
    static final double BEYONDER_SENSE_RADIUS = 24.0;
    static final double DISORDER_SENSE_RADIUS = 32.0;            // Disorder Sense (3): enemies this disorderly glow
    static final int DISORDER_SENSE_MIN = 2;
    static final double IMBALANCE_RADIUS = 24.0;                 // Sense Imbalance (2): concealed enemies cannot hide
    static final int IMBALANCE_TICKS = 30;
    static final double SENSE_ORDER_RADIUS = 48.0;               // Sense of Order (1): casts this close chime

    // What grows on the Decrees and Punishment (keys 5 and 7 are HandOfOrderDecrees and HandOfOrderOrder)
    static final int SENSE_OF_ORDER_SEQUENCE = 1;                // Sense of Order (1): Rules written by an enemy of a
                                                                         // lower Sequence cannot bind you
    static final int SURE_HIT_SEQUENCE = 3;                      // Chaos Hunter: the Convict breaking your Rule...
    static final int SURE_HIT_TICKS = 200;                       // ...makes your next hit on it within this...
    static final float SURE_HIT_MORE = 1.5f;                     // ...sure (no shield) and this much harder
    static final int MANY_DECREES_SEQUENCE = 1;                  // Hand of Order: Decrees last twice as long
    static final float MANY_DECREES_LONGER = 2f;
    static final int LAW_OF_ORDER_SEQUENCE = 0;                  // Justiciar, Law of Order: Decrees cost 25% less...
    static final float LAW_OF_ORDER_CHEAPER = 0.75f;
    static final float LAW_OF_ORDER_LONGER = 1.25f;              // ...and last 25% longer

    static final float BOSS_HEALTH = 150f;

    // =========================
    // Names and what is saved on the player (game times are "until" this tick)
    // =========================
    static final String CONVICT = "hand_of_order_convict";               // status/Chosen
    static final String RECOGNIZED = "hand_of_order_recognized";         // status/Chosen
    static final String JURISDICTION = "hand_of_order_jurisdiction";       // combat/Zone (the second: + "_2")
    static final String JURISDICTION_2 = "hand_of_order_jurisdiction_2";
    static final String PUNISHMENT_PENDING = "hand_of_order_punishment_pending";
    static final String INTUITION_READY = "hand_of_order_intuition_ready";
    static final String RECOGNITION_COUNTER = "hand_of_order_recognition";  // combat/HudCounter
    static final String SURE_HIT = "hand_of_order_sure_hit";                         // status/EmpoweredHit

    /** false: this Pathway's single keys have no cooldown (combat/Cooldowns.addFree): spirituality is their only limit. */
    public static final boolean KEY_COOLDOWNS = false;
    /** A sneak recast from any of this Pathway's wheels: no cooldown, this many times the cost. */
    public static final float QUICK_CAST_COST = 3f;

    static final String HALT_KEY = "hand_of_order_halt";          // combat/Cooldowns
    static final String PRESENCE_KEY = "hand_of_order_presence";
    static final String JURISDICTION_KEY = "hand_of_order_jurisdiction";
    static final String RECOGNITION_KEY = "hand_of_order_recognition";
    static final String PUNISHMENT_KEY = "hand_of_order_punishment";
    static final String SWORD_KEY = "hand_of_order_sword";

    private static final Identifier PROFICIENCY_SPEED_ID = Physique.id("hand_of_order_proficiency_speed");
    private static final Identifier PROFICIENCY_REACH_ID = Physique.id("hand_of_order_proficiency_reach");
    private static final Identifier CONVICT_SLOW_ID = Physique.id("hand_of_order_convict_slow");

    /** Mind effects that Self-Restraint shortens. */
    private static final Set<Holder<MobEffect>> MIND_EFFECTS = Set.of(MobEffects.NAUSEA, MobEffects.BLINDNESS, MobEffects.DARKNESS);

    /** Players whose own shortened effect is being put back on (Self-Restraint must let that one through). */
    private static final Set<UUID> SHORTENING = new HashSet<>();

    // =========================
    // The melee combo (combat/MeleeCombos): three hits that repeat; HandOfOrderFx.combo is the look of each
    // =========================
    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway(PATHWAY)
            .step((player, target, sl) -> HandOfOrderFx.combo(sl, player, target, 0))
            .step((player, target, sl) -> HandOfOrderFx.combo(sl, player, target, 1))
            .step((player, target, sl) -> HandOfOrderFx.combo(sl, player, target, 2))
            .resetAfter(40)
            .minCharge(0.8f);

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class HandOfOrderEvents {

        static {
            // a Convict let go (its time is up, it died, another was named): its marks come off
            Chosen.addListener(CONVICT, (chooser, target) -> {
                if (target != null) Physique.remove(target, Attributes.MOVEMENT_SPEED, CONVICT_SLOW_ID);
                if (chooser != null && chooser.level() instanceof ServerLevel sl) {
                    HandOfOrderFx.punishmentEnd(sl, chooser, target, convictTag(chooser));
                }
            });
            // the one Recognized is let go: its distance is no longer shown
            Chosen.addListener(RECOGNIZED, (chooser, target) -> {
                if (chooser == null) return;
                HudCounter.hide(chooser, RECOGNITION_COUNTER);
                HandOfOrderFx.statusEnd(chooser, Status.RECOGNITION);
            });
            // Order Proxy: every ability costs nothing and has no cooldown while it lasts (every key and wheel asks these)
            Spells.addCostScale(player -> HandOfOrderOrder.isProxy(player) ? 0f : 1f);
            Cooldowns.addScale(player -> HandOfOrderOrder.isProxy(player) ? 0f : 1f);
            // the single keys have no cooldown (KEY_COOLDOWNS)
            if (!KEY_COOLDOWNS) Cooldowns.addFree(Cooldowns.keysStartingWith("hand_of_order_"));
            // no Rule binds an Order Proxy; from Hand of Order (1) no Rule of a lower Sequence binds you (Sense of Order)
            Decree.addImmunity((owner, entity, rule) -> entity instanceof Player player
                    && (HandOfOrderOrder.isProxy(player) || (isHandOfOrder(player, SENSE_OF_ORDER_SEQUENCE)
                    && SoulCore.getSequence(owner) > SoulCore.getSequence(player))));
            // Balance's end listener lives with the rest of the Order wheel
            HandOfOrderOrder.registerListeners();
        }

        @SubscribeEvent
        public static void onHandOfOrderTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (player.tickCount % 20 == 0) bodyTick(player);
            if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
            HandOfOrderInterrogation.tick(player, sl);
            HandOfOrderOrder.tick(player, sl);
            if (player.tickCount % 20 == 0) {
                recognitionTick(player, sl);
                convictTick(player);
            }
        }

        /** Punishment's 30% on the Convict, a Convict named by its blow, Demolition Proficiency and Judge's Aura. */
        @SubscribeEvent
        public static void onHandOfOrderHurt(LivingHurtEvent event) {
            LivingEntity target = event.getEntity();
            if (!(target.level() instanceof ServerLevel sl)) return;
            if (event.getSource().getEntity() instanceof Player caster && Chosen.is(target, caster, CONVICT)) {
                event.setAmount(event.getAmount() * CONVICT_MORE_DAMAGE);
                HandOfOrderFx.convictHit(sl, target);
            }
            if (!(target instanceof Player victim) || !SoulCore.getPathway(victim).equals(PATHWAY)) return;
            if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != victim) {
                if (isActive(victim, PUNISHMENT_PENDING) && Spells.isEnemy(victim, attacker)) {
                    victim.getPersistentData().remove(PUNISHMENT_PENDING);
                    convict(victim, sl, attacker, PUNISHMENT_TICKS);
                }
                if (Beyonder.hasSequence(victim, JUDGE_SEQUENCE) && attacker.distanceTo(victim) <= AURA_RADIUS
                        && Spells.isEnemy(victim, attacker)) {
                    event.setAmount(event.getAmount() * (1f - AURA_CUT));
                }
            }
            if (Beyonder.hasSequence(victim, DEMOLITION_SEQUENCE) && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                event.setAmount(event.getAmount() * (1f - DEMOLITION_CUT));
            }
        }

        /** Chaos Hunter's sure hit: the Convict's raised shield drops before the blow lands. */
        @SubscribeEvent
        public static void onHandOfOrderSwing(AttackEntityEvent event) {
            Player player = event.getEntity();
            if (player.level().isClientSide() || !EmpoweredHit.isArmed(player, SURE_HIT)) return;
            if (event.getTarget() instanceof LivingEntity target && Chosen.is(target, player, CONVICT)) target.stopUsingItem();
        }

        /** What really got through to a Hand Of Order: a blow breaks an Execution being wound up. */
        @SubscribeEvent
        public static void onHandOfOrderDamage(LivingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel sl)) return;
            if (event.getAmount() <= 0f || !SoulCore.getPathway(player).equals(PATHWAY)) return;
            HandOfOrderVerdicts.casterHurt(player, sl);
        }

        /** Sheriff's Intuition: a mob starts hunting the Sheriff from where they cannot see it. */
        @SubscribeEvent
        public static void onHandOfOrderTarget(LivingChangeTargetEvent event) {
            if (!(event.getNewTarget() instanceof ServerPlayer hunted) || event.getEntity().level().isClientSide()) return;
            if (!isHandOfOrder(hunted, INTUITION_SEQUENCE)) return;
            LivingEntity mob = event.getEntity();
            long now = hunted.level().getGameTime();
            if (hunted.getPersistentData().getLongOr(INTUITION_READY, 0L) > now) return;
            Vec3 toMob = mob.position().subtract(hunted.position());
            boolean behind = toMob.horizontalDistanceSqr() > 0.01
                    && Spells.flatLook(hunted).dot(new Vec3(toMob.x, 0, toMob.z).normalize()) < 0.2;
            if (!behind && hunted.hasLineOfSight(mob)) return;                     // seen coming: no warning needed
            hunted.getPersistentData().putLong(INTUITION_READY, now + INTUITION_REST);
            HandOfOrderFx.intuition(hunted);
        }

        /**
         * Self-Restraint: a mind effect (nausea, blindness, darkness) put on a Disciplinary Paladin lasts half as long.
         * The effect is turned away and put on again a tick later, shorter (SHORTENING lets that one through).
         */
        @SubscribeEvent
        public static void onHandOfOrderEffect(MobEffectEvent.Applicable event) {
            if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel sl)) return;
            MobEffectInstance effect = event.getEffectInstance();
            if (!MIND_EFFECTS.contains(effect.getEffect()) || effect.isInfiniteDuration() || effect.getDuration() < 20) return;
            if (SHORTENING.contains(player.getUUID()) || !isHandOfOrder(player, RESTRAINT_SEQUENCE)) return;
            event.setResult(Result.DENY);
            Holder<MobEffect> kind = effect.getEffect();
            int ticks = Math.max(1, Math.round(effect.getDuration() * (1f - RESTRAINT_SHORTER)));
            int amplifier = effect.getAmplifier();
            UUID id = player.getUUID();
            Later.run(sl, 1, () -> {
                if (!player.isAlive()) return;
                SHORTENING.add(id);
                try {
                    player.addEffect(new MobEffectInstance(kind, ticks, amplifier));
                } finally {
                    SHORTENING.remove(id);
                }
                HandOfOrderFx.selfRestraint(sl, player);
            });
        }

        // what was on the screen does not survive relogging or dying: start clean
        @SubscribeEvent
        public static void onHandOfOrderLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onHandOfOrderRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onHandOfOrderLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            Player player = event.getEntity();
            SHORTENING.remove(player.getUUID());
            HandOfOrderVerdicts.forget(player);
        }
    }

    // =========================
    // Ability 1 - Authority
    // Normal: Halt. One word at the enemy you look at (HALT_RANGE): it is held for HALT_TICKS (a player
    //         HALT_PLAYER_TICKS; from Judge HALT_JUDGE_...), and every mob within HALT_FORGET_RADIUS that was hunting
    //         you forgets you for HALT_FORGET_TICKS (status/Forgotten).
    // Sneak:  Presence. A wave of Authority PRESENCE_RADIUS round you (Judge: PRESENCE_JUDGE_RADIUS): Slowness II
    //         for PRESENCE_TICKS; mobs also Weakness I, and those under COWED_MAX_HEALTH cannot attack for COWED_TICKS.
    //         Imperative Mage (4): Intimidation, everyone in the wave is also rooted for INTIMIDATION_TICKS.
    // =========================
    public static void handOfOrderAuthority(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, HALT_SEQUENCE)) return;
        if (player.isShiftKeyDown()) presence(player, sl);
        else halt(player, sl);
    }

    private static void halt(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, HALT_KEY, "Halt")) return;
        LivingEntity target = Spells.aimEnemy(player, sl, HALT_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to stop."));
            return; // nothing spent
        }
        if (!pay(player, HALT_COST)) return;
        Cooldowns.start(player, HALT_KEY, HALT_COOLDOWN);
        boolean judge = Beyonder.hasSequence(player, JUDGE_SEQUENCE);
        int ticks = target instanceof Player
                ? (judge ? HALT_JUDGE_PLAYER_TICKS : HALT_PLAYER_TICKS)
                : (judge ? HALT_JUDGE_TICKS : HALT_TICKS);
        HandOfOrderFx.halt(sl, player, target);
        Later.run(sl, HandOfOrderFx.HALT_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            Root.apply(target, ticks, RootRestriction.EVERYTHING);
            HandOfOrderFx.halted(sl, target, ticks, "ho_halt_" + target.getId());
        });
        for (Mob hunter : Forgotten.huntersOf(sl, player, HALT_FORGET_RADIUS)) {
            Forgotten.apply(hunter, player, HALT_FORGET_TICKS);
            HandOfOrderFx.forgets(sl, hunter);
        }
    }

    private static void presence(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, PRESENCE_KEY, "Presence")) return;
        if (!pay(player, PRESENCE_COST)) return;
        Cooldowns.start(player, PRESENCE_KEY, PRESENCE_COOLDOWN);
        double radius = Beyonder.hasSequence(player, JUDGE_SEQUENCE) ? PRESENCE_JUDGE_RADIUS : PRESENCE_RADIUS;
        boolean intimidation = Beyonder.hasSequence(player, INTIMIDATION_SEQUENCE);
        HandOfOrderFx.presence(sl, player, radius, intimidation);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), radius)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PRESENCE_TICKS, 1));
            if (!(enemy instanceof Player)) {                                       // players are only slowed
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PRESENCE_TICKS, 0));
                if (enemy.getMaxHealth() < COWED_MAX_HEALTH) {
                    Root.apply(enemy, COWED_TICKS, RootRestriction.ATTACK);
                    HandOfOrderFx.cowed(sl, enemy, COWED_TICKS, "ho_cowed_" + enemy.getId());
                }
            }
            if (intimidation) Root.apply(enemy, INTIMIDATION_TICKS, RootRestriction.MOVEMENT);
        }
    }

    // =========================
    // Ability 2 - Jurisdiction
    // Normal: claim a circle of ground round you (JURISDICTION_RADIUS; Judge 48, Chaos Hunter 96) for
    //         JURISDICTION_TICKS (combat/Zone; cast again to move it). Every enemy that steps in chimes for you alone
    //         and is seen through walls by you while inside. While you stand in it your Order costs
    //         JURISDICTION_DISCOUNT (costFor) and your Decrees last JURISDICTION_DECREES_LONGER (decreeTicksFor).
    //         Hand of Order (1): two at once; a third cast moves the one with less time left.
    // Sneak:  Recognition. Memorize the enemy you look at (RECOGNITION_RANGE) for RECOGNITION_TICKS (status/Chosen;
    //         one at a time): while it is within RECOGNITION_SHOWN you alone see it through walls and its footprints,
    //         and its distance is shown beside the hotbar. Imperative Mage (4): its Pathway, Sequence and effects,
    //         and a concealed one is still seen within RECOGNITION_CONCEALED. Chaos Hunter (3): its Disorder.
    // =========================
    public static void handOfOrderJurisdiction(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, JURISDICTION_SEQUENCE)) return;
        if (player.isShiftKeyDown()) recognition(player, sl);
        else jurisdiction(player, sl);
    }

    private static void jurisdiction(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, JURISDICTION_KEY, "Jurisdiction")) return;
        if (!pay(player, JURISDICTION_COST)) return;
        Cooldowns.start(player, JURISDICTION_KEY, JURISDICTION_COOLDOWN);
        double radius = jurisdictionRadius(player);
        Vec3 center = player.position();
        String name = jurisdictionToClaim(player);
        String tag = "ho_jurisdiction_" + player.getId() + "_" + sl.getGameTime();
        ServerPlayer owner = player instanceof ServerPlayer sp ? sp : null;
        // opened first: a Jurisdiction of the same name closes (its onEnd runs) before the new one is shown
        Zone.at(center, radius)
                .height(Math.max(8.0, radius * 0.5))
                .lasts(JURISDICTION_TICKS)
                .every(20)
                .onEnter(enemy -> {
                    if (owner != null) HandOfOrderFx.jurisdictionEnter(owner, enemy);
                })
                .onInside((enemy, ticksInside) -> {
                    if (owner != null) HandOfOrderFx.jurisdictionSense(owner, enemy);
                })
                .onPulse(ticksLeft -> {
                    if (ticksLeft % 100 == 0) HandOfOrderFx.jurisdictionTick(sl, center, radius);
                })
                .onEnd(() -> {
                    HandOfOrderFx.jurisdictionEnd(sl, center, radius, tag);
                    if (!hasJurisdiction(player)) HandOfOrderFx.statusEnd(player, Status.JURISDICTION);
                })
                .open(player, sl, name);
        HandOfOrderFx.jurisdiction(sl, player, center, radius, JURISDICTION_TICKS, tag);
    }

    private static double jurisdictionRadius(Player player) {
        if (Beyonder.hasSequence(player, JURISDICTION_CITY_SEQUENCE)) return JURISDICTION_CITY_RADIUS;
        if (Beyonder.hasSequence(player, JUDGE_SEQUENCE)) return JURISDICTION_JUDGE_RADIUS;
        return JURISDICTION_RADIUS;
    }

    /** Which Jurisdiction a new cast claims: the only one, or (Hand of Order) a free one, else the one ending sooner. */
    private static String jurisdictionToClaim(Player player) {
        if (!Beyonder.hasSequence(player, TWO_JURISDICTIONS_SEQUENCE)) return JURISDICTION;
        if (!Zone.isOpen(player, JURISDICTION)) return JURISDICTION;
        if (!Zone.isOpen(player, JURISDICTION_2)) return JURISDICTION_2;
        return Zone.ticksLeft(player, JURISDICTION) <= Zone.ticksLeft(player, JURISDICTION_2) ? JURISDICTION : JURISDICTION_2;
    }

    private static boolean hasJurisdiction(Player player) {
        return Zone.isOpen(player, JURISDICTION) || Zone.isOpen(player, JURISDICTION_2);
    }

    /** True while this player stands in one of their own Jurisdictions. */
    public static boolean inJurisdiction(Player player) {
        Vec3 at = player.position();
        return Zone.covers(player, JURISDICTION, at) || Zone.covers(player, JURISDICTION_2, at);
    }

    private static void recognition(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, RECOGNITION_KEY, "Recognition")) return;
        LivingEntity target = Spells.aimEnemy(player, sl, RECOGNITION_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to memorize."));
            return; // nothing spent
        }
        if (!pay(player, RECOGNITION_COST)) return;
        Cooldowns.start(player, RECOGNITION_KEY, RECOGNITION_COOLDOWN);
        Chosen.choose(player, RECOGNIZED, target, RECOGNITION_TICKS);
        if (player instanceof ServerPlayer owner) HandOfOrderFx.recognition(owner, target, RECOGNITION_TICKS, "ho_recognition");
        player.sendSystemMessage(recognized(player, target));
        recognitionTick(player, sl);
    }

    /** What Recognition tells: who it is; from Imperative Mage its Pathway, Sequence and effects; from Chaos Hunter its Disorder. */
    private static Component recognized(Player player, LivingEntity one) {
        StringBuilder text = new StringBuilder("Memorized: ").append(one.getDisplayName().getString());
        if (Beyonder.hasSequence(player, RECOGNITION_DETAILS_SEQUENCE)) {
            if (isBeyonder(one)) {
                text.append(", a Beyonder of ").append(Beyonders.pathwayOf(one)).append(" at Sequence ").append(Beyonders.sequenceOf(one));
            }
            List<String> effects = new ArrayList<>();
            for (MobEffectInstance effect : one.getActiveEffects()) {
                effects.add(effect.getEffect().value().getDisplayName().getString() + " " + (effect.getAmplifier() + 1));
            }
            if (!effects.isEmpty()) text.append(", under ").append(String.join(", ", effects));
        }
        if (Beyonder.hasSequence(player, RECOGNITION_DISORDER_SEQUENCE)) text.append(". Disorder ").append(Disorder.of(one));
        return Component.literal(text.append(".").toString());
    }

    /** Once a second: the one Recognized, while near, is seen through walls by its hunter and its distance shown. */
    private static void recognitionTick(Player player, ServerLevel sl) {
        LivingEntity target = Chosen.of(player, RECOGNIZED);
        if (target == null) return;                                            // the Chosen listener cleans up
        double distance = target.level() == sl ? target.distanceTo(player) : Double.MAX_VALUE;
        if (distance > RECOGNITION_SHOWN) {
            HudCounter.hide(player, RECOGNITION_COUNTER);
            return;
        }
        HudCounter.show(player, RECOGNITION_COUNTER, "hand_of_order/recognition", "Recognized", (int) Math.round(distance), 0, 0xF0D68C);
        boolean hidden = Concealment.isHidden(target) || Unnoticed.isUnnoticed(target);
        boolean stillSeen = !hidden || (Beyonder.hasSequence(player, RECOGNITION_DETAILS_SEQUENCE) && distance <= RECOGNITION_CONCEALED);
        if (stillSeen && player instanceof ServerPlayer owner) HandOfOrderFx.recognitionTrack(owner, target);
    }

    // =========================
    // Ability 6 - Judgment
    // Normal: Punishment. Name the enemy you look at (PUNISHMENT_RANGE) the Convict for PUNISHMENT_TICKS
    //         (status/Chosen; one at a time): you deal CONVICT_MORE_DAMAGE to it, it is CONVICT_SLOW slower and glows
    //         through walls, and a dawn-like halo stands behind you. Cast at nothing: the next enemy to hit you within
    //         PUNISHMENT_WAIT becomes the Convict.
    // Sneak:  Sword of Judgment (Chaos Hunter, 3). One stride to the most Disorderly enemy in view (the Convict first,
    //         if it is in range and in view), SWORD_RANGE at most (doubled from Balancer, and again in your
    //         Jurisdiction), and a cut that cannot be dodged: SWORD_DAMAGE, plus SWORD_PER_DISORDER for each point of
    //         its Disorder (combat/Disorder), up to SWORD_MAX times. It strips harmful effects off you.
    // =========================
    public static void handOfOrderJudgment(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, PUNISHMENT_SEQUENCE)) return;
        if (player.isShiftKeyDown()) swordOfJudgment(player, sl);
        else punishment(player, sl);
    }

    private static void punishment(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, PUNISHMENT_KEY, "Punishment")) return;
        LivingEntity target = Spells.aimEnemy(player, sl, PUNISHMENT_RANGE);
        if (!pay(player, PUNISHMENT_COST)) return;
        Cooldowns.start(player, PUNISHMENT_KEY, PUNISHMENT_COOLDOWN);
        if (target != null) {
            convict(player, sl, target, PUNISHMENT_TICKS);
            return;
        }
        Chosen.release(player, CONVICT);
        player.getPersistentData().putLong(PUNISHMENT_PENDING, sl.getGameTime() + PUNISHMENT_WAIT);
        HandOfOrderFx.punishment(sl, player, null, PUNISHMENT_WAIT, convictTag(player));
        player.sendSystemMessage(Component.literal("The next enemy to strike you will be named the Convict."));
    }

    /** 'target' is this player's Convict for 'ticks' (one before it is let go: the Chosen listener cleans it up). */
    static void convict(Player player, ServerLevel sl, LivingEntity target, int ticks) {
        Chosen.choose(player, CONVICT, target, ticks);
        player.getPersistentData().remove(PUNISHMENT_PENDING);
        String tag = convictTag(player);
        HandOfOrderFx.punishmentEnd(sl, player, target, tag);                  // named again: one halo, one brand
        HandOfOrderFx.punishment(sl, player, target, ticks, tag);
        markConvict(target);
    }

    /** Once a second: the Convict keeps glowing and slowed. */
    private static void convictTick(Player player) {
        LivingEntity convict = Chosen.of(player, CONVICT);
        if (convict != null) markConvict(convict);
    }

    private static void markConvict(LivingEntity convict) {
        Spells.keepEffect(convict, MobEffects.GLOWING, 0, 40);
        Physique.keep(convict, Attributes.MOVEMENT_SPEED, CONVICT_SLOW_ID, CONVICT_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
    }

    static String convictTag(Player player) {
        return "ho_convict_" + player.getId();
    }

    /** The Convict of this player right now, or null. */
    public static LivingEntity convictOf(Player player) {
        return Chosen.of(player, CONVICT);
    }

    /**
     * Chaos Hunter (3), "Violators will be punished!": 'who' broke a Rule of 'owner''s Decree. If it is the owner's
     * Convict, the owner's next real hit on it within SURE_HIT_TICKS cannot be blocked and is SURE_HIT_MORE harder
     * (status/EmpoweredHit; a hit on anything else leaves it armed).
     */
    static void violatorPunished(Player owner, LivingEntity who, ServerLevel sl) {
        if (!isHandOfOrder(owner, SURE_HIT_SEQUENCE) || convictOf(owner) != who) return;
        armSureHit(owner, SURE_HIT_TICKS);
        HandOfOrderFx.convictPunished(sl, who);
        owner.sendSystemMessage(Component.literal("Violators will be punished! Your next blow on the Convict is sure."));
    }

    private static void armSureHit(Player owner, int ticks) {
        long until = owner.level().getGameTime() + ticks;
        EmpoweredHit.arm(owner, SURE_HIT, ticks, (attacker, target, amount, level) -> {
            if (Chosen.is(target, attacker, CONVICT)) {
                HandOfOrderFx.convictHit(level, target);
                return amount * SURE_HIT_MORE;
            }
            int left = (int) (until - level.getGameTime());          // another was struck: it waits on for the Convict
            if (left > 1) Later.run(level, 1, () -> armSureHit(attacker, left - 1));
            return amount;
        });
    }

    private static void swordOfJudgment(Player player, ServerLevel sl) {
        if (!Beyonder.hasSequence(player, SWORD_SEQUENCE)) {
            player.sendSystemMessage(Component.literal("The Sword of Judgment answers only a Chaos Hunter (Sequence 3)."));
            return;
        }
        if (!Cooldowns.ready(player, SWORD_KEY, "Sword of Judgment")) return;
        if (Root.has(player, RootRestriction.TELEPORT) || Root.has(player, RootRestriction.MOVE)) {
            player.sendSystemMessage(Component.literal("You cannot leave this place."));
            return; // nothing spent
        }
        LivingEntity target = judgmentTarget(player, sl, swordRange(player));
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one in view to judge."));
            return; // nothing spent
        }
        Vec3 stand = HandOfOrderVerdicts.strideSpot(player, sl, target);
        if (stand == null) {
            player.sendSystemMessage(Component.literal("There is no room to stand before it."));
            return; // nothing spent
        }
        if (!pay(player, SWORD_COST)) return;
        Cooldowns.start(player, SWORD_KEY, SWORD_COOLDOWN);
        judge(player, sl, target, stand);
    }

    /**
     * The Judgment itself, paid for or not: one stride to 'stand' (null: it falls from where you are), and the cut at
     * HandOfOrderFx.SWORD_LANDS. The Court of Judgment gives it free on a third breach (HandOfOrderOrder).
     */
    static void judge(Player player, ServerLevel sl, LivingEntity target, Vec3 stand) {
        int disorder = Disorder.of(target);
        Vec3 from = player.position();
        if (stand != null) {
            player.teleportTo(stand.x, stand.y, stand.z);
            player.fallDistance = 0;
        }
        HandOfOrderFx.swordOfJudgment(sl, player, from, target);
        Spells.cleanse(player);
        HandOfOrderFx.judgmentCleanse(sl, player);
        Later.run(sl, HandOfOrderFx.SWORD_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            float times = Math.min(SWORD_MAX, 1f + SWORD_PER_DISORDER * disorder);
            Spells.strikeTrue(player, target, SWORD_DAMAGE * times);
        });
    }

    private static double swordRange(Player player) {
        double range = SWORD_RANGE;
        if (Beyonder.hasSequence(player, SWORD_RANGE_SEQUENCE)) range *= 2;
        if (inJurisdiction(player)) range *= 2;
        return range;
    }

    /** The Convict if it is in range and in view; otherwise the most Disorderly enemy in view (the nearer of equals). */
    private static LivingEntity judgmentTarget(Player player, ServerLevel sl, double range) {
        LivingEntity convict = Chosen.of(player, CONVICT);
        if (convict != null && convict.level() == sl && convict.distanceTo(player) <= range
                && player.hasLineOfSight(convict) && Spells.canBeChosen(player, convict)) {
            return convict;
        }
        LivingEntity best = null;
        int bestDisorder = -1;
        for (LivingEntity one : Spells.enemiesInSight(player, sl, range, 45)) {      // nearest first
            int disorder = Disorder.of(one);
            if (disorder > bestDisorder) {
                best = one;
                bestDisorder = disorder;
            }
        }
        return best;
    }

    // Keys 5 and 7 are wheels of their own: HandOfOrderDecrees.handOfOrderDecree and HandOfOrderOrder.handOfOrderOrder.

    // =========================
    // What a cast costs, how long a cooldown is, how long a Decree lasts (every key and wheel entry goes through these)
    // =========================

    /**
     * What a cast of 'cost' really costs this player now: JURISDICTION_DISCOUNT while they stand in their own
     * Jurisdiction. The one place for the rules that change only a Hand Of Order's own costs (Order Proxy's "costs
     * nothing" is a Spells.addCostScale in HandOfOrderEvents, so it reaches everything). Wheel entries call it
     * through .costBy(...).
     */
    public static int costFor(Player player, int cost) {
        if (cost <= 0 || Beyonder.isCastingUnbound(player) || !SoulCore.getPathway(player).equals(PATHWAY)) return cost;
        if (inJurisdiction(player)) return Math.round(cost * JURISDICTION_DISCOUNT);
        return cost;
    }

    /** What a Decree of 'cost' costs: Law of Order (0) makes it LAW_OF_ORDER_CHEAPER, then costFor. */
    public static int decreeCostFor(Player player, int cost) {
        if (isHandOfOrder(player, LAW_OF_ORDER_SEQUENCE)) cost = Math.round(cost * LAW_OF_ORDER_CHEAPER);
        return costFor(player, cost);
    }

    /**
     * What a wheel cooldown of 'ticks' is for this player now. (Order Proxy's "no cooldown" is a Cooldowns.addScale,
     * which the wheels apply after this.) The place for a rule that shortens only this Pathway's wheels.
     */
    public static int cooldownFor(Player player, int ticks) {
        return ticks;
    }

    /**
     * How long a Decree of 'ticks' lasts for this player: JURISDICTION_DECREES_LONGER in their Jurisdiction,
     * MANY_DECREES_LONGER from Hand of Order (1), and LAW_OF_ORDER_LONGER from Justiciar (0).
     */
    public static int decreeTicksFor(Player player, int ticks) {
        float scale = 1f;
        if (inJurisdiction(player)) scale *= JURISDICTION_DECREES_LONGER;
        if (isHandOfOrder(player, MANY_DECREES_SEQUENCE)) scale *= MANY_DECREES_LONGER;
        if (isHandOfOrder(player, LAW_OF_ORDER_SEQUENCE)) scale *= LAW_OF_ORDER_LONGER;
        return Math.round(ticks * scale);
    }

    /** Pays for a cast by key (after costFor). False, with the usual message, if it cannot. */
    static boolean pay(Player player, int cost) {
        int due = costFor(player, cost);
        return due <= 0 || Spells.paySpirituality(player, due);
    }

    // =========================
    // Passives (the once-a-second ones are lines in SequencePassives)
    // =========================

    /** Arbiter (9), once a second: weak hostile mobs within ORDER_RADIUS keep their distance (status/Fear). */
    public static void order(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity one : Spells.enemiesAround(player, sl, player.position(), ORDER_RADIUS)) {
            if (one instanceof Mob mob && one instanceof Enemy && mob.getMaxHealth() < ORDER_WEAK_HEALTH) Fear.apply(mob, player, 40);
        }
        if (player.tickCount % 60 == 0) HandOfOrderFx.orderPresence(sl, player);
    }

    /** Imperative Mage (4), now and then: every Beyonder within BEYONDER_SENSE_RADIUS is marked, for you alone. */
    public static void beyonderSense(Player player) {
        if (!(player instanceof ServerPlayer owner) || !(player.level() instanceof ServerLevel sl)) return;
        if (player.tickCount % BEYONDER_SENSE_EVERY >= 20) return;
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), BEYONDER_SENSE_RADIUS)) {
            if (isBeyonder(one)) HandOfOrderFx.beyonderSense(owner, one);
        }
    }

    /** Chaos Hunter (3), once a second: enemies of high Disorder within DISORDER_SENSE_RADIUS smoulder, for you alone. */
    public static void disorderSense(Player player) {
        if (!(player instanceof ServerPlayer owner) || !(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), DISORDER_SENSE_RADIUS)) {
            if (Disorder.of(enemy) >= DISORDER_SENSE_MIN) HandOfOrderFx.disorderSense(owner, enemy);
        }
    }

    /** Balancer (2), once a second: concealed enemies within IMBALANCE_RADIUS cannot hide (status/Exposed). */
    public static void senseImbalance(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), IMBALANCE_RADIUS)) {
            if (Spells.isFriend(player, one)) continue;
            if (Concealment.isHidden(one) || Unnoticed.isUnnoticed(one) || one.hasEffect(MobEffects.INVISIBILITY)) {
                Exposed.apply(one, IMBALANCE_TICKS);
            }
        }
    }

    /** Hand of Order (1), once a second: an ability cast by someone else within SENSE_ORDER_RADIUS chimes for you. */
    public static void senseOfOrder(Player player) {
        if (!(player instanceof ServerPlayer owner) || !(player.level() instanceof ServerLevel sl)) return;
        for (Residue.Cast cast : Residue.near(sl, player.position(), SENSE_ORDER_RADIUS, 20)) {
            if (!cast.caster().equals(player.getUUID())) HandOfOrderFx.senseOrder(owner, cast.at());
        }
    }

    /** Once a second, for every player: Weapon Proficiency (8) while they are this Pathway at that Sequence; off otherwise. */
    private static void bodyTick(Player player) {
        boolean proficient = isHandOfOrder(player, PROFICIENCY_SEQUENCE);
        Physique.keep(player, Attributes.ATTACK_SPEED, PROFICIENCY_SPEED_ID, PROFICIENCY_ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, proficient);
        Physique.keep(player, Attributes.ENTITY_INTERACTION_RANGE, PROFICIENCY_REACH_ID, PROFICIENCY_REACH, AttributeModifier.Operation.ADD_VALUE, proficient);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** True for this Pathway at this Sequence or stronger. */
    public static boolean isHandOfOrder(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    /** The same for anyone: a player as above, a Beyonder mob of this Pathway by its own Sequence. */
    public static boolean isHandOfOrder(LivingEntity entity, int sequence) {
        if (entity instanceof Player player) return isHandOfOrder(player, sequence);
        return PATHWAY.equals(Beyonders.pathwayOf(entity)) && Beyonder.hasSequence(entity, sequence);
    }

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True while the game time saved under 'key' is still ahead. */
    static boolean isActive(Player player, String key) {
        return player.getPersistentData().getLongOr(key, 0L) > player.level().getGameTime();
    }

    /** True for a boss: the dragon, the wither, the warden, an elder guardian, or anything with more than BOSS_HEALTH. */
    static boolean isBoss(LivingEntity one) {
        return one.getType() == EntityTypes.ENDER_DRAGON || one.getType() == EntityTypes.WITHER
                || one.getType() == EntityTypes.WARDEN || one.getType() == EntityTypes.ELDER_GUARDIAN
                || one.getMaxHealth() > BOSS_HEALTH;
    }

    /** True for a Beyonder: a player who follows a Pathway, or a Beyonder mob (blasphemy_slate/beyonder_mobs). */
    static boolean isBeyonder(LivingEntity one) {
        return Beyonders.isBeyonder(one);
    }

    /** After a relog or a death: nothing of the timed casts is left over on the screen. */
    private static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(PUNISHMENT_PENDING);
        HudCounter.hide(player, RECOGNITION_COUNTER);
        HandOfOrderInterrogation.clearState(player);
        HandOfOrderVerdicts.forget(player);
        HandOfOrderOrder.clearState(player);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) HandOfOrderFx.statusEnd(player, status);   // a clock left running before death
    }
}
