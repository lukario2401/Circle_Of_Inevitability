package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpirituality;
import net.lukario.circleofinevitability.combat.Disorder;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.AbilityLock;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.Dispel;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.Forgotten;
import net.lukario.circleofinevitability.status.KeySeal;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.WeaponCoat;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder.*;

/**
 * Hand Of Order abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 * The numbers are the player abilities' own (HandOfOrder and its wheel classes), so the two never disagree; only the
 * cooldowns, and when a mob thinks a cast worth it, are a mob's. A mob has no wheels: every wheel entry is an ability
 * of its own. To port another ability: one more MobAbilities.register(...) in register(), and its cast method below.
 *
 *   1  Halt (9), Presence (9, sneak)                as a player's
 *   2  Jurisdiction (8)      a circle round it: every enemy that stands in it glows (it senses them)
 *      Recognition (8, sneak) its target glows for RECOGNITION_TICKS
 *   3  Psychic Lashing (7)   its own blows add LASHING_DAMAGE through armor and stagger (status/WeaponCoat.armAny)
 *      Psychic Piercing (7)  the beam from its eyes; Whip of Pain (7) as a player's (no confession told)
 *   4  Imprison (6, Confinement 4), Exile (6, the void 4, hurricane 3), Death (6, at the wound 5), Maintain Secrecy (6),
 *      Deprivation (4), Execution (4: broken when it is hurt while its fist is raised)
 *   5  Prohibition: Passage (6), Prohibition: Concealment (6), Imperative: Reality (4), Contract (4): its Decrees, one
 *      at a time, round where it stands, with the Violation ladder (no layers, no recoil)
 *   6  Punishment (5)        its target is its Convict: it glows, is slowed, and the mob's hits on it deal 30% more
 *      Sword of Judgment (3, sneak)
 *   7  Balance (2), Divide the Battlefield (2), Law: No Return (2), Order Proxy (1), Quell Disorder (1),
 *      Court of Judgment (0)
 *
 * Left out: nothing.
 */
public final class HandOfOrderMobs {

    // ---------- mob cooldowns (ticks; a player's: docs/HAND_OF_ORDER.md)
    private static final int HALT_MOB_COOLDOWN = 160;
    private static final int PRESENCE_MOB_COOLDOWN = 280;
    private static final int RECOGNITION_MOB_COOLDOWN = 600;
    private static final int LASHING_MOB_COOLDOWN = 500;
    private static final int PIERCING_MOB_COOLDOWN = 140;
    private static final int WHIP_MOB_COOLDOWN = 260;
    private static final int VERDICT_MOB_COOLDOWN = 300;
    private static final int DECREE_MOB_COOLDOWN = 600;
    private static final int PUNISHMENT_MOB_COOLDOWN = 600;
    private static final int SWORD_MOB_COOLDOWN = 400;
    private static final int ORDER_MOB_COOLDOWN = 900;

    // ---------- names (mob versions keep their own, so they never touch a player's)
    private static final String JURISDICTION_ZONE = "hand_of_order_mob_jurisdiction";
    private static final String LASHING_COAT = "hand_of_order_mob_lashing";
    private static final String DECREE_SLOT = "hand_of_order_mob_decree";
    private static final String DIVIDE_DOME = "hand_of_order_mob_divide";
    private static final String COURT_DECREE = "hand_of_order_mob_court";
    private static final Identifier CONVICT_SLOW_MOB_ID = Physique.id("hand_of_order_mob_convict_slow");

    /** A mob's Convict (Punishment) and the one who signed its Contract: whose, and until when (not saved). */
    private record Bond(UUID mob, long until) {}
    private static final Map<UUID, Bond> CONVICTS = new HashMap<>();
    private static final Map<UUID, Bond> SIGNERS = new HashMap<>();
    private static final Map<UUID, int[]> CONTRACT_BREACHES = new HashMap<>();
    private static final Map<UUID, Long> CONTRACT_LAST_BREACH = new HashMap<>();

    private HandOfOrderMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        // ---- key 1: Authority
        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.NORMAL)          // Halt
                .cost(HALT_COST).cooldown(HALT_MOB_COOLDOWN)
                .range(HALT_RANGE).sight()
                .when((mob, target, sl) -> !Root.has(target, RootRestriction.MOVE))  // not one already held
                .cast(HandOfOrderMobs::halt));
        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.SNEAK).named("Presence")
                .cost(PRESENCE_COST).cooldown(PRESENCE_MOB_COOLDOWN)
                .range(PRESENCE_RADIUS)                                               // its target must be in the wave
                .cast(HandOfOrderMobs::presence));

        // ---- key 2: Jurisdiction
        MobAbilities.register(MobAbility.of(PATHWAY, 2, MobAbility.NORMAL)
                .cost(JURISDICTION_COST).cooldown(JURISDICTION_TICKS).weight(6)
                .range(JURISDICTION_RADIUS)
                .when((mob, target, sl) -> !Zone.isOpen(mob, JURISDICTION_ZONE))
                .cast(HandOfOrderMobs::jurisdiction));
        MobAbilities.register(MobAbility.of(PATHWAY, 2, MobAbility.SNEAK).named("Recognition")
                .cost(RECOGNITION_COST).cooldown(RECOGNITION_MOB_COOLDOWN).weight(6)
                .range(RECOGNITION_RANGE).sight()
                .when((mob, target, sl) -> !target.hasEffect(MobEffects.GLOWING))
                .cast(HandOfOrderMobs::recognition));

        // ---- key 3: the Interrogation wheel
        MobAbilities.register(MobAbility.of(PATHWAY, 3, MobAbility.NORMAL).entry("psychic_lashing").named("Psychic Lashing")
                .sequence(HandOfOrderInterrogation.LASHING_SEQUENCE)
                .cost(HandOfOrderInterrogation.LASHING_COST).cooldown(LASHING_MOB_COOLDOWN)
                .range(4.0)
                .when((mob, target, sl) -> MobSpells.inMelee(mob, target) && !WeaponCoat.isArmed(mob, LASHING_COAT))
                .cast(HandOfOrderMobs::psychicLashing));
        MobAbilities.register(MobAbility.of(PATHWAY, 3, MobAbility.NORMAL).entry("psychic_piercing").named("Psychic Piercing")
                .sequence(HandOfOrderInterrogation.PIERCING_SEQUENCE)
                .cost(HandOfOrderInterrogation.PIERCING_COST).cooldown(PIERCING_MOB_COOLDOWN)
                .range(HandOfOrderInterrogation.PIERCING_RANGE).sight()
                .cast(HandOfOrderMobs::psychicPiercing));
        MobAbilities.register(MobAbility.of(PATHWAY, 3, MobAbility.NORMAL).entry("whip_of_pain").named("Whip of Pain")
                .sequence(HandOfOrderInterrogation.WHIP_SEQUENCE)
                .cost(HandOfOrderInterrogation.WHIP_COST).cooldown(WHIP_MOB_COOLDOWN)
                .range(HandOfOrderInterrogation.WHIP_RANGE).sight()
                .cast(HandOfOrderMobs::whipOfPain));

        // ---- key 4: the Verdict wheel
        MobAbilities.register(MobAbility.of(PATHWAY, 4, MobAbility.NORMAL).entry("imprison").named("Imprison")
                .sequence(HandOfOrderVerdicts.IMPRISON_SEQUENCE)
                .cost(HandOfOrderVerdicts.IMPRISON_COST).cooldown(VERDICT_MOB_COOLDOWN)
                .range(HandOfOrderVerdicts.IMPRISON_RANGE).sight()
                .when((mob, target, sl) -> !Root.has(target, RootRestriction.MOVE))
                .cast(HandOfOrderMobs::imprison));
        MobAbilities.register(MobAbility.of(PATHWAY, 4, MobAbility.NORMAL).entry("exile").named("Exile")
                .sequence(HandOfOrderVerdicts.EXILE_SEQUENCE)
                .cost(HandOfOrderVerdicts.EXILE_COST).cooldown(VERDICT_MOB_COOLDOWN)
                .range(HandOfOrderVerdicts.EXILE_RANGE).sight()
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.6f) || mob.distanceTo(target) < 6.0)   // it wants room
                .cast(HandOfOrderMobs::exile));
        MobAbilities.register(MobAbility.of(PATHWAY, 4, MobAbility.NORMAL).entry("death").named("Death")
                .sequence(HandOfOrderVerdicts.DEATH_SEQUENCE)
                .cost(HandOfOrderVerdicts.DEATH_COST).cooldown(VERDICT_MOB_COOLDOWN)
                .range(HandOfOrderVerdicts.DEATH_RANGE).sight()
                .when((mob, target, sl) -> !Root.has(mob, RootRestriction.MOVE) && !Root.has(mob, RootRestriction.TELEPORT))
                .cast(HandOfOrderMobs::death));
        MobAbilities.register(MobAbility.of(PATHWAY, 4, MobAbility.NORMAL).entry("maintain_secrecy").named("Maintain Secrecy")
                .sequence(HandOfOrderVerdicts.SECRECY_SEQUENCE)
                .cost(HandOfOrderVerdicts.SECRECY_COST).cooldown(VERDICT_MOB_COOLDOWN * 2).weight(6)
                .range(HandOfOrderVerdicts.SECRECY_RANGE).sight()
                .when((mob, target, sl) -> !Silence.isSilenced(target) && !AbilityLock.isLocked(target))
                .cast(HandOfOrderMobs::maintainSecrecy));
        MobAbilities.register(MobAbility.of(PATHWAY, 4, MobAbility.NORMAL).entry("deprivation").named("Deprivation")
                .sequence(HandOfOrderVerdicts.DEPRIVATION_SEQUENCE)
                .cost(HandOfOrderVerdicts.DEPRIVATION_COST).cooldown(HandOfOrderVerdicts.DEPRIVATION_COOLDOWN)
                .range(HandOfOrderVerdicts.DEPRIVATION_RANGE).sight()
                .when((mob, target, sl) -> !AbilityLock.isLocked(target))
                .cast(HandOfOrderMobs::deprivation));
        MobAbilities.register(MobAbility.of(PATHWAY, 4, MobAbility.NORMAL).entry("execution").named("Execution")
                .sequence(HandOfOrderVerdicts.EXECUTION_SEQUENCE)
                .cost(HandOfOrderVerdicts.EXECUTION_COST).cooldown(HandOfOrderVerdicts.EXECUTION_COOLDOWN)
                .range(HandOfOrderVerdicts.EXECUTION_RANGE).sight()
                .cast(HandOfOrderMobs::execution));

        // ---- key 5: the Decree wheel (one Decree of a mob at a time)
        MobAbilities.register(MobAbility.of(PATHWAY, 5, MobAbility.NORMAL).entry("prohibition_passage").named("Prohibition: Passage")
                .sequence(HandOfOrderDecrees.PROHIBITION_SEQUENCE)
                .cost(HandOfOrderDecrees.PROHIBITION_COST).cooldown(DECREE_MOB_COOLDOWN)
                .range(HandOfOrderDecrees.PROHIBITION_RADIUS)
                .when((mob, target, sl) -> !Decree.isWritten(mob, DECREE_SLOT) && target instanceof Player)
                .cast((mob, target, sl) -> prohibit(mob, sl, HandOfOrderFx.Rule.PASSAGE, Decree.Rule.PASSAGE)));
        MobAbilities.register(MobAbility.of(PATHWAY, 5, MobAbility.NORMAL).entry("prohibition_concealment").named("Prohibition: Concealment")
                .sequence(HandOfOrderDecrees.PROHIBITION_SEQUENCE)
                .cost(HandOfOrderDecrees.PROHIBITION_COST).cooldown(DECREE_MOB_COOLDOWN).weight(4)
                .range(HandOfOrderDecrees.PROHIBITION_RADIUS)
                .when((mob, target, sl) -> !Decree.isWritten(mob, DECREE_SLOT))
                .cast((mob, target, sl) -> prohibit(mob, sl, HandOfOrderFx.Rule.CONCEALMENT, Decree.Rule.CONCEALMENT)));
        MobAbilities.register(MobAbility.of(PATHWAY, 5, MobAbility.NORMAL).entry("imperative_reality").named("Imperative: Reality")
                .sequence(HandOfOrderDecrees.REALITY_SEQUENCE)
                .cost(HandOfOrderDecrees.REALITY_COST).cooldown(HandOfOrderDecrees.REALITY_COOLDOWN)
                .range(HandOfOrderDecrees.REALITY_RADIUS)
                .when((mob, target, sl) -> !Decree.isWritten(mob, DECREE_SLOT) && Beyonders.isBeyonder(target))
                .cast(HandOfOrderMobs::imperativeReality));
        MobAbilities.register(MobAbility.of(PATHWAY, 5, MobAbility.NORMAL).entry("contract").named("Contract")
                .sequence(HandOfOrderDecrees.CONTRACT_SEQUENCE)
                .cost(HandOfOrderDecrees.CONTRACT_COST).cooldown(HandOfOrderDecrees.CONTRACT_COOLDOWN)
                .range(HandOfOrderDecrees.CONTRACT_RANGE).sight()
                .when((mob, target, sl) -> !SIGNERS.containsKey(target.getUUID()))
                .cast(HandOfOrderMobs::contract));

        // ---- key 6: Judgment
        MobAbilities.register(MobAbility.of(PATHWAY, 6, MobAbility.NORMAL)          // Punishment
                .cost(PUNISHMENT_COST).cooldown(PUNISHMENT_MOB_COOLDOWN)
                .range(PUNISHMENT_RANGE).sight()
                .when((mob, target, sl) -> !isConvictOf(target, mob))
                .cast(HandOfOrderMobs::punishment));
        MobAbilities.register(MobAbility.of(PATHWAY, 6, MobAbility.SNEAK).named("Sword of Judgment")
                .sequence(SWORD_SEQUENCE)
                .cost(SWORD_COST).cooldown(SWORD_MOB_COOLDOWN)
                .range(SWORD_RANGE).sight()
                .when((mob, target, sl) -> !Root.has(mob, RootRestriction.MOVE) && !Root.has(mob, RootRestriction.TELEPORT))
                .cast(HandOfOrderMobs::swordOfJudgment));

        // ---- key 7: the Order wheel
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("balance").named("Balance")
                .sequence(HandOfOrderOrder.BALANCE_SEQUENCE)
                .cost(HandOfOrderOrder.BALANCE_COST).cooldown(HandOfOrderOrder.BALANCE_COOLDOWN)
                .range(HandOfOrderOrder.BALANCE_RANGE).sight()
                .when((mob, target, sl) -> HandOfOrder.isBoss(target) || target.getMaxHealth() > mob.getMaxHealth())
                .cast(HandOfOrderMobs::balance));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("divide_battlefield").named("Divide the Battlefield")
                .sequence(HandOfOrderOrder.DIVIDE_SEQUENCE)
                .cost(HandOfOrderOrder.DIVIDE_COST).cooldown(HandOfOrderOrder.DIVIDE_COOLDOWN)
                .range(HandOfOrderOrder.DIVIDE_RADIUS)
                .when((mob, target, sl) -> !Dome.isRaised(mob, DIVIDE_DOME))
                .cast(HandOfOrderMobs::divideBattlefield));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("law_no_return").named("Law: No Return")
                .sequence(HandOfOrderOrder.NO_RETURN_SEQUENCE)
                .cost(HandOfOrderOrder.NO_RETURN_COST).cooldown(HandOfOrderOrder.NO_RETURN_COOLDOWN).weight(4)
                .range(HandOfOrderOrder.NO_RETURN_RADIUS)
                .when((mob, target, sl) -> !Decree.isWritten(mob, DECREE_SLOT))
                .cast(HandOfOrderMobs::lawNoReturn));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("order_proxy").named("Order Proxy")
                .sequence(HandOfOrderOrder.PROXY_SEQUENCE)
                .cost(HandOfOrderOrder.PROXY_COST).cooldown(HandOfOrderOrder.PROXY_COOLDOWN)
                .anyRange()
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.5f))
                .cast(HandOfOrderMobs::orderProxy));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("quell_disorder").named("Quell Disorder")
                .sequence(HandOfOrderOrder.QUELL_SEQUENCE)
                .cost(HandOfOrderOrder.QUELL_COST).cooldown(HandOfOrderOrder.QUELL_COOLDOWN)
                .range(HandOfOrderOrder.QUELL_RADIUS)
                .when((mob, target, sl) -> Dispel.boons(target) > 0 || Spells.harmfulEffects(mob).size() >= 2)
                .cast(HandOfOrderMobs::quellDisorder));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("court_of_judgment").named("Court of Judgment")
                .sequence(HandOfOrderOrder.COURT_SEQUENCE)
                .cost(HandOfOrderOrder.COURT_COST).cooldown(HandOfOrderOrder.COURT_COOLDOWN)
                .range(HandOfOrderOrder.COURT_RADIUS)
                .when((mob, target, sl) -> !Decree.isWritten(mob, COURT_DECREE))
                .cast(HandOfOrderMobs::courtOfJudgment));
    }

    // =========================
    // Key 1: Halt, Presence
    // =========================

    private static boolean halt(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean judge = MobSpells.hasSequence(mob, JUDGE_SEQUENCE);
        int ticks = target instanceof Player
                ? (judge ? HALT_JUDGE_PLAYER_TICKS : HALT_PLAYER_TICKS)
                : (judge ? HALT_JUDGE_TICKS : HALT_TICKS);
        MobSpells.face(mob, target);
        HandOfOrderFx.halt(sl, mob, target);
        Later.run(sl, HandOfOrderFx.HALT_LANDS, () -> {
            if (!MobSpells.casterStillHere(mob, sl) || !target.isAlive() || target.level() != sl) return;
            Root.apply(target, ticks, RootRestriction.EVERYTHING);
            HandOfOrderFx.halted(sl, target, ticks, "ho_halt_" + target.getId());
        });
        return true;
    }

    private static boolean presence(Mob mob, LivingEntity target, ServerLevel sl) {
        double radius = MobSpells.hasSequence(mob, JUDGE_SEQUENCE) ? PRESENCE_JUDGE_RADIUS : PRESENCE_RADIUS;
        boolean intimidation = MobSpells.hasSequence(mob, INTIMIDATION_SEQUENCE);
        HandOfOrderFx.presence(sl, mob, radius, intimidation);
        for (LivingEntity enemy : MobSpells.enemiesAround(mob, sl, mob.position(), radius)) {
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
        return true;
    }

    // =========================
    // Key 2: Jurisdiction, Recognition
    // =========================

    private static boolean jurisdiction(Mob mob, LivingEntity target, ServerLevel sl) {
        double radius = MobSpells.hasSequence(mob, JURISDICTION_CITY_SEQUENCE) ? JURISDICTION_CITY_RADIUS
                : MobSpells.hasSequence(mob, JUDGE_SEQUENCE) ? JURISDICTION_JUDGE_RADIUS : JURISDICTION_RADIUS;
        Vec3 center = mob.position();
        String tag = "ho_mob_jurisdiction_" + mob.getId() + "_" + sl.getGameTime();
        Zone.at(center, radius)
                .height(Math.max(8.0, radius * 0.5))
                .lasts(JURISDICTION_TICKS)
                .every(20)
                .onInside((enemy, ticksInside) -> Spells.keepEffect(enemy, MobEffects.GLOWING, 0, 40))   // it senses them
                .onPulse(ticksLeft -> {
                    if (ticksLeft % 100 == 0) HandOfOrderFx.jurisdictionTick(sl, center, radius);
                })
                .onEnd(() -> HandOfOrderFx.jurisdictionEnd(sl, center, radius, tag))
                .open(mob, sl, JURISDICTION_ZONE);
        HandOfOrderFx.jurisdiction(sl, mob, center, radius, JURISDICTION_TICKS, tag);
        return true;
    }

    private static boolean recognition(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, RECOGNITION_TICKS, 0));
        return true;
    }

    // =========================
    // Key 3: the Interrogation wheel
    // =========================

    private static boolean psychicLashing(Mob mob, LivingEntity target, ServerLevel sl) {
        WeaponCoat.armAny(mob, LASHING_COAT, HandOfOrderInterrogation.LASHING_HITS, HandOfOrderInterrogation.LASHING_TICKS,
                (attacker, struck, amount, level) -> {
                    HandOfOrderFx.lashHit(level, attacker, struck);
                    Later.run(level, 1, () -> {
                        if (!struck.isAlive() || struck.level() != level) return;
                        Spells.strikeTrue(attacker, struck, HandOfOrderInterrogation.LASHING_DAMAGE);
                        Root.apply(struck, HandOfOrderInterrogation.LASHING_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
                    });
                    return amount;
                });
        String tag = "ho_mob_lashing_" + mob.getId();
        HandOfOrderFx.lashingEnd(sl, mob, tag);
        HandOfOrderFx.psychicLashing(sl, mob, HandOfOrderInterrogation.LASHING_TICKS, tag);
        Later.run(sl, HandOfOrderInterrogation.LASHING_TICKS, () -> HandOfOrderFx.lashingEnd(sl, mob, tag));
        return true;
    }

    private static boolean psychicPiercing(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        Vec3 eyes = mob.getEyePosition();
        Vec3 end = MobSpells.lineEnd(mob, sl, eyes, MobSpells.toward(mob, target), HandOfOrderInterrogation.PIERCING_RANGE);
        List<LivingEntity> onLine = MobSpells.enemiesOnLine(mob, sl, eyes, end, HandOfOrderInterrogation.PIERCING_WIDTH);
        List<LivingEntity> hits = new ArrayList<>(onLine.subList(0, Math.min(HandOfOrderInterrogation.PIERCING_MAX, onLine.size())));
        HandOfOrderFx.psychicPiercing(sl, mob, end, hits);
        for (LivingEntity one : hits) {
            boolean spirit = Marionette.isMarionette(one) || Marionette.isSummoned(one);
            float damage = HandOfOrderInterrogation.PIERCING_DAMAGE;
            Spells.strikeTrue(mob, one, spirit ? damage * HandOfOrderInterrogation.PIERCING_SPIRIT : damage);
            HandOfOrderFx.piercingHit(sl, one, spirit);
        }
        return true;
    }

    private static boolean whipOfPain(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        HandOfOrderFx.whipOfPain(sl, mob, target);
        Later.run(sl, HandOfOrderFx.WHIP_LANDS, () -> {
            if (!MobSpells.casterStillHere(mob, sl) || !target.isAlive() || target.level() != sl) return;
            Spells.strikeTrue(mob, target, HandOfOrderInterrogation.WHIP_DAMAGE);
            if (!target.isAlive()) return;
            int ticks = HandOfOrderInterrogation.WHIP_TICKS;
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
            if (target instanceof Player victim) {
                victim.setSprinting(false);
                victim.addEffect(new MobEffectInstance(MobEffects.NAUSEA, HandOfOrderInterrogation.WHIP_SHAKE_TICKS, 0));
            }
            boolean wasHidden = Concealment.isHidden(target);
            if (wasHidden) Concealment.reveal(target);
            Exposed.apply(target, HandOfOrderInterrogation.WHIP_EXPOSED);
            if (wasHidden) HandOfOrderFx.truthOut(sl, target);
            HandOfOrderFx.trembling(sl, target, ticks, "ho_whip_" + target.getId());
        });
        return true;
    }

    // =========================
    // Key 4: the Verdict wheel
    // =========================

    private static boolean imprison(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean confinement = MobSpells.hasSequence(mob, HandOfOrderVerdicts.CONFINEMENT_SEQUENCE);
        int ticks = confinement ? HandOfOrderVerdicts.CONFINEMENT_TICKS : HandOfOrderVerdicts.IMPRISON_TICKS;
        String tag = "ho_mob_imprison_" + target.getId() + "_" + sl.getGameTime();
        MobSpells.face(mob, target);
        if (confinement) HandOfOrderFx.confinement(sl, mob, target, ticks, tag);
        else HandOfOrderFx.imprison(sl, mob, target, ticks, tag);
        Later.run(sl, HandOfOrderFx.IMPRISON_LANDS, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            Root.apply(target, ticks, RootRestriction.EVERYTHING);
            if (confinement) Substitute.disarm(target);
        });
        Later.run(sl, HandOfOrderFx.IMPRISON_LANDS + ticks, () -> HandOfOrderFx.imprisonEnd(sl, target, tag));
        return true;
    }

    private static boolean exile(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean intoVoid = MobSpells.hasSequence(mob, HandOfOrderVerdicts.VOID_SEQUENCE);
        boolean hurricane = MobSpells.hasSequence(mob, HandOfOrderVerdicts.HURRICANE_SEQUENCE);
        Vec3 away = target.position().subtract(mob.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        Vec3 direction = flat.lengthSqr() < 1.0E-4 ? Spells.flatLook(mob) : flat.normalize();
        MobSpells.face(mob, target);
        HandOfOrderFx.exile(sl, mob, target, target.position().add(direction.scale(HandOfOrderVerdicts.EXILE_SHOWN)));
        Later.run(sl, HandOfOrderFx.EXILE_LANDS, () -> {
            if (!MobSpells.casterStillHere(mob, sl) || !target.isAlive() || target.level() != sl) return;
            Vec3 at = target.position();
            if (hurricane) {
                HandOfOrderFx.exileHurricane(sl, at, HandOfOrderVerdicts.HURRICANE_RADIUS);
                for (LivingEntity near : MobSpells.enemiesAround(mob, sl, at, HandOfOrderVerdicts.HURRICANE_RADIUS)) {
                    if (near != target) Spells.push(near, at, HandOfOrderVerdicts.HURRICANE_PUSH, HandOfOrderVerdicts.EXILE_LIFT * 0.6);
                }
            }
            if (intoVoid) {
                String tag = "ho_mob_void_" + target.getId() + "_" + sl.getGameTime();
                Vec3 chest = target.getBoundingBox().getCenter();
                HandOfOrderFx.exileVoid(sl, target, HandOfOrderVerdicts.VOID_TICKS, tag);
                Exile.banish(target, HandOfOrderVerdicts.VOID_TICKS, at, (who, level) -> HandOfOrderFx.exileReturn(level, chest, tag));
                return;
            }
            Spells.push(target, mob.position(), HandOfOrderVerdicts.EXILE_PUSH, HandOfOrderVerdicts.EXILE_LIFT);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, HandOfOrderVerdicts.EXILE_SLOW_TICKS, 1));
        });
        return true;
    }

    private static boolean death(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 stand = MobSpells.spotBefore(mob, sl, target);
        if (stand == null) return false;                                          // no room: nothing spent
        boolean wound = MobSpells.hasSequence(mob, HandOfOrderVerdicts.WOUND_SEQUENCE);
        Vec3 from = mob.position();
        if (!MobSpells.stepTo(mob, sl, stand)) return false;
        MobSpells.face(mob, target);
        HandOfOrderFx.death(sl, mob, from, target, wound);
        Later.run(sl, HandOfOrderFx.DEATH_LANDS, () -> {
            if (!MobSpells.casterStillHere(mob, sl) || !target.isAlive() || target.level() != sl) return;
            float damage = HandOfOrderVerdicts.DEATH_DAMAGE;
            Spells.strikeTrue(mob, target, wound ? damage * HandOfOrderVerdicts.WOUND_MORE : damage);
            if (!target.isAlive()) return;
            Root.apply(target, HandOfOrderVerdicts.DEATH_STUN, RootRestriction.EVERYTHING);
            if (target instanceof Player stunned) HandOfOrderFx.status(stunned, HandOfOrderFx.Status.STUNNED, HandOfOrderVerdicts.DEATH_STUN);
            if (wound) target.addEffect(new MobEffectInstance(MobEffects.WITHER, HandOfOrderVerdicts.WOUND_TICKS, 1));
        });
        return true;
    }

    private static boolean maintainSecrecy(Mob mob, LivingEntity target, ServerLevel sl) {
        int ticks = HandOfOrderVerdicts.SECRECY_TICKS;
        String tag = "ho_mob_secrecy_" + target.getId();
        MobSpells.face(mob, target);
        HandOfOrderFx.maintainSecrecy(sl, mob, target, ticks, tag);
        if (target instanceof Player) {
            Silence.apply(target, ticks);
        } else {
            AbilityLock.apply(target, ticks);
            if (target instanceof Mob other) Forgotten.apply(other, mob, HandOfOrderVerdicts.SECRECY_FORGET_TICKS);
        }
        Later.run(sl, ticks, () -> HandOfOrderFx.secrecyEnd(sl, target, tag));
        return true;
    }

    private static boolean deprivation(Mob mob, LivingEntity target, ServerLevel sl) {
        int ticks;
        int count;
        if (target instanceof Player victim) {
            int many = MobSpells.hasSequence(mob, HandOfOrderVerdicts.DEPRIVE_MORE_SEQUENCE) ? HandOfOrderVerdicts.DEPRIVE_MORE : 1;
            List<Integer> keys = KeySeal.lastUsed(victim, many);
            if (keys.isEmpty()) return false;                                     // nothing to take: nothing spent
            ticks = HandOfOrderVerdicts.DEPRIVATION_TICKS;
            count = keys.size();
            for (int key : keys) KeySeal.seal(victim, key, ticks);
        } else {
            ticks = HandOfOrder.isBoss(target) ? HandOfOrderVerdicts.DEPRIVATION_BOSS_TICKS : HandOfOrderVerdicts.DEPRIVATION_TICKS;
            if (!AbilityLock.apply(target, ticks)) return false;
            count = 1;
        }
        String tag = "ho_mob_deprived_" + target.getId();
        MobSpells.face(mob, target);
        HandOfOrderFx.deprivation(sl, mob, target, count);
        Later.run(sl, HandOfOrderFx.DEPRIVED, () -> {
            if (target.isAlive()) HandOfOrderFx.deprived(sl, target, ticks, tag);
        });
        Later.run(sl, HandOfOrderFx.DEPRIVED + ticks, () -> HandOfOrderFx.deprivedEnd(sl, target, tag));
        return true;
    }

    private static boolean execution(Mob mob, LivingEntity target, ServerLevel sl) {
        String tag = "ho_mob_execution_" + mob.getId() + "_" + sl.getGameTime();
        float healthBefore = mob.getHealth();
        MobSpells.face(mob, target);
        HandOfOrderFx.execution(sl, mob, target, tag);
        Later.run(sl, HandOfOrderFx.EXECUTION_LANDS, () -> {
            if (!MobSpells.casterStillHere(mob, sl)) return;
            boolean broken = mob.getHealth() < healthBefore;                      // a blow it took breaks it
            if (broken || !target.isAlive() || target.level() != sl
                    || target.distanceTo(mob) > HandOfOrderVerdicts.EXECUTION_RANGE || !mob.hasLineOfSight(target)) {
                HandOfOrderFx.executionBroken(sl, mob, target, tag);
                return;
            }
            boolean low = target.getHealth() < target.getMaxHealth() * HandOfOrderVerdicts.EXECUTION_THRESHOLD;
            boolean strong = target instanceof Player || HandOfOrder.isBoss(target);
            boolean slain = low && !strong;
            HandOfOrderFx.executionStrike(sl, mob, target, slain);
            float damage = HandOfOrderVerdicts.EXECUTION_DAMAGE;
            Spells.strikeTrue(mob, target, slain ? target.getHealth() + target.getAbsorptionAmount() + damage
                    : low ? damage * HandOfOrderVerdicts.EXECUTION_STRONG : damage);
        });
        return true;
    }

    // =========================
    // Key 5: the Decree wheel
    // =========================

    private static boolean prohibit(Mob mob, ServerLevel sl, HandOfOrderFx.Rule look, Decree.Rule rule) {
        List<Decree.Rule> rules = new ArrayList<>(List.of(rule));
        if (rule == Decree.Rule.CONCEALMENT && MobSpells.hasSequence(mob, HandOfOrderDecrees.STAND_INS_SEQUENCE)) rules.add(Decree.Rule.STAND_INS);
        if (MobSpells.hasSequence(mob, HandOfOrderDecrees.MARIONETTES_SEQUENCE)) rules.add(Decree.Rule.MARIONETTES);
        return writeLaw(mob, sl, DECREE_SLOT, groundOf(mob, sl), HandOfOrderDecrees.PROHIBITION_RADIUS,
                HandOfOrderDecrees.PROHIBITION_TICKS, look, rules);
    }

    private static boolean imperativeReality(Mob mob, LivingEntity target, ServerLevel sl) {
        return writeLaw(mob, sl, DECREE_SLOT, groundOf(mob, sl), HandOfOrderDecrees.REALITY_RADIUS,
                HandOfOrderDecrees.REALITY_TICKS, HandOfOrderFx.Rule.REALITY, List.of(Decree.Rule.REALITY));
    }

    private static boolean contract(Mob mob, LivingEntity target, ServerLevel sl) {
        int ticks = HandOfOrderDecrees.CONTRACT_TICKS;
        SIGNERS.put(target.getUUID(), new Bond(mob.getUUID(), sl.getGameTime() + ticks));
        CONTRACT_BREACHES.remove(target.getUUID());
        CONTRACT_LAST_BREACH.remove(target.getUUID());
        String tag = "ho_mob_contract_" + mob.getId();
        MobSpells.face(mob, target);
        HandOfOrderFx.contract(sl, mob, target, ticks, tag);
        Later.run(sl, ticks, () -> {
            Bond bond = SIGNERS.get(target.getUUID());
            if (bond != null && bond.mob().equals(mob.getUUID()) && bond.until() <= sl.getGameTime()) endContract(sl, target, tag);
        });
        return true;
    }

    private static void endContract(ServerLevel sl, LivingEntity signer, String tag) {
        SIGNERS.remove(signer.getUUID());
        CONTRACT_BREACHES.remove(signer.getUUID());
        CONTRACT_LAST_BREACH.remove(signer.getUUID());
        HandOfOrderFx.contractTear(sl, signer, tag);
    }

    /** The signer tried to harm the mob it signed for: refused, and punished like a player's Contract. */
    private static void contractBroken(Mob mob, LivingEntity signer, ServerLevel sl) {
        long now = sl.getGameTime();
        Long last = CONTRACT_LAST_BREACH.get(signer.getUUID());
        if (last != null && now - last < HandOfOrderDecrees.CONTRACT_GAP) return;
        CONTRACT_LAST_BREACH.put(signer.getUUID(), now);
        int step = ++CONTRACT_BREACHES.computeIfAbsent(signer.getUUID(), id -> new int[1])[0];
        HandOfOrderFx.contractViolation(sl, signer, Math.min(3, step));
        Later.run(sl, 1, () -> {
            if (!signer.isAlive() || !MobSpells.casterStillHere(mob, sl)) return;
            if (step == 1) Root.apply(signer, HandOfOrderDecrees.CONTRACT_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
            else if (step == 2) Spells.strikeTrue(mob, signer, HandOfOrderDecrees.CONTRACT_DAMAGE);
            else Root.apply(signer, HandOfOrderDecrees.CONTRACT_HOLD, RootRestriction.EVERYTHING);
        });
    }

    /**
     * A Decree of 'rules' round 'center' for 'ticks', written by the mob into 'slot' (it replaces the one there), with
     * its look and the Violation ladder of a player's Decree (no layers, no recoil).
     */
    private static boolean writeLaw(Mob mob, ServerLevel sl, String slot, Vec3 center, double radius, int ticks,
                                    HandOfOrderFx.Rule look, List<Decree.Rule> rules) {
        String tag = "ho_mob_" + slot + "_" + mob.getId();
        Decree.close(mob, slot);
        Decree.at(center, radius)
                .forbid(rules.toArray(new Decree.Rule[0]))
                .height(HandOfOrderDecrees.DECREE_HEIGHT)
                .lasts(ticks)
                .binds(enemy -> !strongerThan(mob, enemy))
                .onViolation((who, rule, count, level) -> punish(mob, who, rule, count, level))
                .onPulse(40, left -> HandOfOrderFx.decreeTick(sl, center, radius, look))
                .onEnd(() -> {
                    if (look == HandOfOrderFx.Rule.REALITY) HandOfOrderFx.realityEnd(sl, center, radius, tag);
                    else HandOfOrderFx.decreeEnd(sl, center, radius, tag);
                })
                .write(mob, sl, slot);
        if (look == HandOfOrderFx.Rule.REALITY) HandOfOrderFx.reality(sl, mob, center, radius, ticks, tag);
        else if (look == HandOfOrderFx.Rule.RESURRECTION) HandOfOrderFx.lawNoReturn(sl, mob, center, radius, ticks, tag);
        else HandOfOrderFx.decree(sl, mob, center, radius, ticks, look, tag);
        return true;
    }

    /** The Violation ladder (HandOfOrderDecrees.punish): 1st staggered, 2nd, 3rd and on hurt harder and held. */
    private static void punish(Mob mob, LivingEntity who, Decree.Rule rule, int count, ServerLevel sl) {
        HandOfOrderFx.violation(sl, who, count);
        switch (rule) {
            case CONCEALMENT -> HandOfOrderFx.revealed(sl, who);
            case RESURRECTION -> HandOfOrderFx.revivalVoided(sl, who);
            case MARIONETTES -> HandOfOrderFx.puppetFrozen(sl, who);
            default -> HandOfOrderFx.refused(sl, who);
        }
        Later.run(sl, 1, () -> {
            if (!who.isAlive() || !MobSpells.casterStillHere(mob, sl)) return;
            if (count <= 1) {
                Root.apply(who, HandOfOrderDecrees.LADDER_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
            } else if (count == 2) {
                Spells.strikeTrue(mob, who, HandOfOrderDecrees.LADDER_DAMAGE_2);
            } else if (count == 3) {
                Spells.strikeTrue(mob, who, HandOfOrderDecrees.LADDER_DAMAGE_3);
                Root.apply(who, HandOfOrderDecrees.LADDER_HOLD_3, RootRestriction.EVERYTHING);
            } else {
                Spells.strikeTrue(mob, who, HandOfOrderDecrees.LADDER_DAMAGE_4);
                Root.apply(who, HandOfOrderDecrees.LADDER_HOLD_4, RootRestriction.EVERYTHING);
            }
        });
    }

    /** Stronger than the mob: a boss, or a Beyonder of a higher Sequence (a lower number). Not bound by its Decrees. */
    private static boolean strongerThan(Mob mob, LivingEntity enemy) {
        if (HandOfOrder.isBoss(enemy)) return true;
        return Beyonders.isBeyonder(enemy) && Beyonders.sequenceOf(enemy) < BeyonderMobs.sequence(mob);
    }

    private static Vec3 groundOf(Mob mob, ServerLevel sl) {
        return mob.onGround() ? mob.position() : Spells.groundAt(mob, sl, mob.position());
    }

    // =========================
    // Key 6: Punishment, Sword of Judgment
    // =========================

    private static boolean punishment(Mob mob, LivingEntity target, ServerLevel sl) {
        CONVICTS.values().removeIf(bond -> bond.mob().equals(mob.getUUID()));     // one Convict at a time
        int ticks = PUNISHMENT_TICKS;
        Bond bond = new Bond(mob.getUUID(), sl.getGameTime() + ticks);
        CONVICTS.put(target.getUUID(), bond);
        String tag = "ho_mob_convict_" + mob.getId();
        MobSpells.face(mob, target);
        HandOfOrderFx.punishmentEnd(sl, mob, target, tag);
        HandOfOrderFx.punishment(sl, mob, target, ticks, tag);
        markConvict(sl, mob, target, bond, tag);
        return true;
    }

    /** Once a second while this naming lasts: the Convict glows and is slowed; then its marks come off. */
    private static void markConvict(ServerLevel sl, Mob mob, LivingEntity convict, Bond bond, String tag) {
        boolean still = CONVICTS.get(convict.getUUID()) == bond && bond.until() > sl.getGameTime();
        if (!still || !convict.isAlive() || !MobSpells.casterStillHere(mob, sl)) {
            CONVICTS.remove(convict.getUUID(), bond);
            if (!CONVICTS.containsKey(convict.getUUID())) Physique.remove(convict, Attributes.MOVEMENT_SPEED, CONVICT_SLOW_MOB_ID);
            HandOfOrderFx.punishmentEnd(sl, mob, convict, tag);
            return;
        }
        Spells.keepEffect(convict, MobEffects.GLOWING, 0, 40);
        Physique.keep(convict, Attributes.MOVEMENT_SPEED, CONVICT_SLOW_MOB_ID, CONVICT_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
        Later.run(sl, 20, () -> markConvict(sl, mob, convict, bond, tag));
    }

    private static boolean isConvictOf(LivingEntity target, Mob mob) {
        Bond bond = CONVICTS.get(target.getUUID());
        return bond != null && bond.mob().equals(mob.getUUID()) && bond.until() > target.level().getGameTime();
    }

    private static boolean swordOfJudgment(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 stand = MobSpells.spotBefore(mob, sl, target);
        if (stand == null) return false;
        judge(mob, sl, target, stand);
        return true;
    }

    /** The Judgment: one stride before it, its harmful effects stripped, and the cut that nothing softens. */
    private static void judge(Mob mob, ServerLevel sl, LivingEntity target, Vec3 stand) {
        int disorder = Disorder.of(target);
        Vec3 from = mob.position();
        if (stand != null) MobSpells.stepTo(mob, sl, stand);
        MobSpells.face(mob, target);
        HandOfOrderFx.swordOfJudgment(sl, mob, from, target);
        Spells.cleanse(mob);
        HandOfOrderFx.judgmentCleanse(sl, mob);
        Later.run(sl, HandOfOrderFx.SWORD_LANDS, () -> {
            if (!MobSpells.casterStillHere(mob, sl) || !target.isAlive() || target.level() != sl) return;
            float times = Math.min(SWORD_MAX, 1f + SWORD_PER_DISORDER * disorder);
            Spells.strikeTrue(mob, target, SWORD_DAMAGE * times);
        });
    }

    // =========================
    // Key 7: the Order wheel
    // =========================

    private static boolean balance(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean boss = HandOfOrder.isBoss(target);
        float mine = mob.getMaxHealth();
        float its = target.getMaxHealth();
        if (!boss && its <= mine) return false;
        int ticks = boss ? HandOfOrderOrder.BALANCE_MIN_TICKS
                : Math.max(HandOfOrderOrder.BALANCE_MIN_TICKS, Math.round(HandOfOrderOrder.BALANCE_TICKS * mine / its));
        String tag = "ho_mob_balance_" + mob.getId();
        MobSpells.face(mob, target);
        HandOfOrderFx.balance(sl, mob, target, ticks, tag);
        Later.run(sl, HandOfOrderFx.BALANCE_SETTLED, () -> {
            if (!target.isAlive() || !MobSpells.casterStillHere(mob, sl)) return;
            weigh(mob, target);
        });
        Later.run(sl, HandOfOrderFx.BALANCE_SETTLED + ticks, () -> {
            for (int i = 0; i < HandOfOrderOrder.WEIGHED.size(); i++) {
                Physique.remove(target, HandOfOrderOrder.WEIGHED.get(i), HandOfOrderOrder.WEIGHED_IDS.get(i));
            }
            HandOfOrderFx.balanceEnd(sl, mob, target, tag);
        });
        return true;
    }

    /** Each thing it has more of than the mob is lowered to the mob's (HandOfOrderOrder.weigh). */
    private static void weigh(Mob mob, LivingEntity target) {
        for (int i = 0; i < HandOfOrderOrder.WEIGHED.size(); i++) {
            Holder<Attribute> attribute = HandOfOrderOrder.WEIGHED.get(i);
            Identifier id = HandOfOrderOrder.WEIGHED_IDS.get(i);
            Physique.remove(target, attribute, id);
            AttributeInstance its = target.getAttribute(attribute);
            AttributeInstance mine = mob.getAttribute(attribute);
            if (its == null || mine == null || its.getValue() <= mine.getValue() || its.getValue() <= 0) continue;
            Physique.keep(target, attribute, id, mine.getValue() / its.getValue() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
        }
    }

    private static boolean divideBattlefield(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 center = mob.position();
        double radius = HandOfOrderOrder.DIVIDE_RADIUS;
        float mine = mob.getMaxHealth();
        for (LivingEntity enemy : MobSpells.enemiesAround(mob, sl, center, radius)) {
            if (!HandOfOrder.isBoss(enemy) && enemy.getMaxHealth() <= mine * HandOfOrderOrder.DIVIDE_TOO_STRONG) continue;
            Vec3 away = new Vec3(enemy.getX() - center.x, 0, enemy.getZ() - center.z);
            if (away.lengthSqr() < 0.01) away = Spells.flatLook(mob);
            Vec3 out = Spells.groundAt(mob, sl, center.add(away.normalize().scale(radius + HandOfOrderOrder.DIVIDE_THROWN)).add(0, 2, 0));
            HandOfOrderFx.thrownOut(sl, enemy);
            enemy.teleportTo(out.x, out.y, out.z);
            Spells.push(enemy, center, 0.6, 0.3);
        }
        String tag = "ho_mob_divide_" + mob.getId();
        Dome.at(center, radius)
                .lasts(HandOfOrderOrder.DIVIDE_TICKS)
                .keepsEnemiesIn()
                .stopsProjectiles()
                .onBlock(at -> HandOfOrderFx.divideBlocked(sl, at))
                .onEnd(() -> HandOfOrderFx.divideEnd(sl, center, radius, tag))
                .raise(mob, sl, DIVIDE_DOME);
        HandOfOrderFx.divideBattlefield(sl, mob, center, radius, HandOfOrderOrder.DIVIDE_TICKS, tag);
        return true;
    }

    private static boolean lawNoReturn(Mob mob, LivingEntity target, ServerLevel sl) {
        return writeLaw(mob, sl, DECREE_SLOT, groundOf(mob, sl), HandOfOrderOrder.NO_RETURN_RADIUS,
                HandOfOrderOrder.NO_RETURN_TICKS, HandOfOrderFx.Rule.RESURRECTION,
                List.of(Decree.Rule.RESURRECTION, Decree.Rule.MARIONETTES));
    }

    private static boolean orderProxy(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean justiciar = MobSpells.hasSequence(mob, HandOfOrderOrder.COURT_SEQUENCE);
        int ticks = justiciar ? HandOfOrderOrder.PROXY_JUSTICIAR_TICKS : HandOfOrderOrder.PROXY_TICKS;
        mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 3));
        mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 2));
        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1));
        String tag = "ho_mob_proxy_" + mob.getId();
        HandOfOrderFx.orderProxy(sl, mob, ticks, tag);
        for (int t = 4; t < ticks; t += 4) {                                      // no root or Silence holds it
            Later.run(sl, t, () -> {
                if (!MobSpells.casterStillHere(mob, sl)) return;
                if (Root.isRooted(mob)) Root.remove(mob);
                HandOfOrderFx.proxyTick(sl, mob);
            });
        }
        Later.run(sl, ticks, () -> {                                              // then it pays
            if (!MobSpells.casterStillHere(mob, sl)) return;
            int spent = justiciar ? HandOfOrderOrder.SPENT_JUSTICIAR_TICKS : HandOfOrderOrder.SPENT_TICKS;
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, spent, 2));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, spent, 1));
            MobSpirituality.set(mob, MobSpirituality.get(mob) * (1f - HandOfOrderOrder.SPENT_SPIRITUALITY));
            HandOfOrderFx.proxyEnd(sl, mob, tag);
        });
        return true;
    }

    private static boolean quellDisorder(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 at = mob.position();
        double radius = HandOfOrderOrder.QUELL_RADIUS;
        HandOfOrderFx.quellDisorder(sl, mob, radius);
        for (LivingEntity one : Spells.livingAround(mob, sl, at, radius)) {
            if (Spells.isFriend(mob, one)) {
                Spells.cleanse(one);
                HandOfOrderFx.cleansed(sl, one);
            } else if (Spells.isEnemy(mob, one) && Dispel.statuses(one) > 0) {
                HandOfOrderFx.quelled(sl, one);
            }
        }
        Spells.cleanse(mob);
        HandOfOrderFx.cleansed(sl, mob);
        Zone.closeNear(sl, at, radius, owner -> owner != mob && Spells.isEnemy(mob, owner));
        for (Dome.Raised dome : Dome.raisedAt(sl, at)) {
            if (dome.owner() != mob && Spells.isEnemy(mob, dome.owner())) Dome.close(dome.owner(), dome.name());
        }
        return true;
    }

    private static boolean courtOfJudgment(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 center = mob.position();
        double radius = HandOfOrderOrder.COURT_RADIUS;
        String tag = "ho_mob_court_" + mob.getId();
        int[] written = {0};
        Decree.at(center, radius)
                .height(HandOfOrderOrder.COURT_HEIGHT)
                .lasts(HandOfOrderOrder.COURT_TICKS)
                .binds(enemy -> !strongerThan(mob, enemy))
                .onViolation((who, rule, count, level) -> sentence(mob, who, count, level))
                .onPulse(HandOfOrderOrder.COURT_CLAUSE_EVERY, left -> {
                    if (written[0] >= HandOfOrderOrder.CLAUSES.length) return;
                    Decree.forbid(mob, COURT_DECREE, HandOfOrderOrder.CLAUSES[written[0]]);
                    written[0]++;
                    HandOfOrderFx.clause(sl, center, radius, written[0]);
                })
                .onEnd(() -> HandOfOrderFx.adjourn(sl, center, radius, tag))
                .write(mob, sl, COURT_DECREE);
        HandOfOrderFx.court(sl, mob, center, radius, HandOfOrderOrder.COURT_TICKS, tag);
        return true;
    }

    /** The Court's sentence (HandOfOrderOrder.sentence): a warning, then harder each breach, up to Death. */
    private static void sentence(Mob mob, LivingEntity who, int breach, ServerLevel sl) {
        HandOfOrderFx.judged(sl, who, Math.min(4, breach));
        Later.run(sl, 1, () -> {
            if (!who.isAlive() || !MobSpells.casterStillHere(mob, sl)) return;
            if (breach <= 1) {
                Root.apply(who, HandOfOrderOrder.SENTENCE_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
                Spells.keepEffect(who, MobEffects.GLOWING, 0, HandOfOrderOrder.SENTENCE_MARK);
            } else if (breach == 2) {
                Spells.strikeTrue(mob, who, HandOfOrderOrder.SENTENCE_DAMAGE_2);
                Root.apply(who, HandOfOrderOrder.SENTENCE_HOLD_2, RootRestriction.EVERYTHING);
            } else if (breach == 3) {
                Spells.strikeTrue(mob, who, HandOfOrderOrder.SENTENCE_DAMAGE_3);
                if (!who.isAlive()) return;
                Root.apply(who, HandOfOrderOrder.SENTENCE_HOLD_3, RootRestriction.EVERYTHING);
                judge(mob, sl, who, MobSpells.spotBefore(mob, sl, who));          // free and instant
            } else {
                NoHeal.apply(who, HandOfOrderOrder.SENTENCE_NO_HEAL);
                Spells.strikeTrue(mob, who, HandOfOrderOrder.SENTENCE_DEATH);
            }
        });
    }

    // =========================
    // Events: the Convict's 30%, and the Contract
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class HandOfOrderMobEvents {

        private HandOfOrderMobEvents() {}

        /** A mob's hits on its Convict deal CONVICT_MORE_DAMAGE. */
        @SubscribeEvent
        public static void onHandOfOrderMobHurt(LivingHurtEvent event) {
            LivingEntity target = event.getEntity();
            if (!(target.level() instanceof ServerLevel sl) || CONVICTS.isEmpty()) return;
            if (event.getSource().getEntity() instanceof Mob mob && isConvictOf(target, mob)) {
                event.setAmount(event.getAmount() * CONVICT_MORE_DAMAGE);
                HandOfOrderFx.convictHit(sl, target);
            }
        }

        /** The signer of a mob's Contract cannot harm it (each try is a Violation); the mob striking the signer tears it. */
        @SubscribeEvent(priority = Priority.HIGH)
        public static boolean onHandOfOrderMobContract(LivingAttackEvent event) {
            if (SIGNERS.isEmpty()) return false;
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel sl)) return false;
            if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) return false;
            Bond bond = SIGNERS.get(attacker.getUUID());
            if (bond != null && victim instanceof Mob mob && bond.mob().equals(mob.getUUID())) {
                if (bond.until() <= sl.getGameTime()) {
                    SIGNERS.remove(attacker.getUUID());
                    return false;
                }
                contractBroken(mob, attacker, sl);
                return true;
            }
            Bond signed = SIGNERS.get(victim.getUUID());
            if (signed != null && signed.mob().equals(attacker.getUUID())) endContract(sl, victim, "ho_mob_contract_" + attacker.getId());
            return false;
        }
    }
}
