package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Shot;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Dispel;
import net.lukario.circleofinevitability.status.EmpoweredHit;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Invulnerable;
import net.lukario.circleofinevitability.status.Scrolls;
import net.lukario.circleofinevitability.status.Study;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Knowledge Emperor, key 7: the Knowledge wheel (Sage, Sequence 2; Knowledge Emperor, 1; Hermit, 0).
 * docs/KNOWLEDGE_EMPEROR.md, "Key 7 wheel".
 *
 *   Informatization          you become a stream of information: unhurt, unchosen, unheld and fast, while a false copy
 *                            stands where you were and bursts when enemies reach it; what hit you comes back out
 *                            round you. From Knowledge Emperor (1) longer, and cast again to flow through any gap
 *   Erase                    deletion: every boon and every ward off the enemy looked at, or a summon gone outright
 *   Knowledge Overload       a flood of useless knowledge stops a mind, then hurts it; at 100% Study it detonates.
 *                            From Hermit (0) every enemy near at once
 *   Lance of Knowledge       a white spear that bursts where it lands, its knowledge forced into minds (nausea).
 *                            From Hermit (0) three in a fan
 *   Condense Knowledge       all your Study near gathers into light and becomes spirituality and strength; with none,
 *                            your next hit counts as full Study
 *   Embodiment of Knowledge  you hide as your own information: unseen, unread, unstudied until it runs out or you cast
 *   Indoctrination           allied Hermits are blessed (spirituality, shorter cooldowns); enemies stunned with
 *                            knowledge and Understood
 *   Divine Kingdom           the Knowledge Moor: nothing hides in it, enemy Study grows, your casts cost half, your
 *                            pouch stays full, spirituality flows three times as fast. Cast again to end it
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost). Every cost goes through
 * KnowledgeEmperor.costFor, every cooldown through cooldownFor. The looks are KnowledgeEmperorFx's; every number is
 * right below.
 */
public final class KnowledgeEmperorKnowledge {

    private KnowledgeEmperorKnowledge() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int KNOWLEDGE_SEQUENCE = 2;                      // the key
    static final float QUICK_CAST_COST = 2f;

    // Informatization
    static final int INFO_SEQUENCE = 2;
    static final int INFO_COST = 20000;                   // from Sequence 2 the bar holds 78400 and refills at once
    static final int INFO_COOLDOWN = 600;
    static final int INFO_TICKS = 120;
    static final int INFO_SPEED = 2;                      // Speed III
    static final double DECOY_LURE = 16.0;                // enemies this close that hunted you go for the copy...
    static final double DECOY_TRIGGER = 1.6;              // ...which bursts when one comes this close
    static final double DECOY_RADIUS = 3.0;
    static final float DECOY_DAMAGE = 40f;
    static final float INFO_RELEASED = 0.3f;              // of what hit you, let out round you at the end...
    static final double INFO_RELEASE_RADIUS = 4.0;        // ...this far
    static final int EMPEROR_INFO_SEQUENCE = 1;           // Knowledge Emperor: longer, and cast again to flow
    static final int EMPEROR_INFO_TICKS = 200;
    static final double INFO_FLOW_RANGE = 16.0;

    // Erase
    static final int ERASE_SEQUENCE = 2;
    static final int ERASE_COST = 15000;
    static final int ERASE_COOLDOWN = 400;
    static final double ERASE_RANGE = 24.0;

    // Knowledge Overload
    static final int OVERLOAD_SEQUENCE = 1;
    static final int OVERLOAD_COST = 32000;
    static final int OVERLOAD_COOLDOWN = 800;
    static final double OVERLOAD_RANGE = 32.0;
    static final int OVERLOAD_HOLD = 40;
    static final int OVERLOAD_STRONG_HOLD = 20;           // bosses and players
    static final float OVERLOAD_DAMAGE = 120f;
    static final double MIND_BURST_RADIUS = 4.0;
    static final float MIND_BURST_DAMAGE = 60f;
    static final int HERMIT_OVERLOAD_SEQUENCE = 0;        // Hermit: every enemy within...
    static final double HERMIT_OVERLOAD_RADIUS = 16.0;

    // Lance of Knowledge
    static final int LANCE_SEQUENCE = 1;
    static final int LANCE_COST = 26000;
    static final int LANCE_COOLDOWN = 500;
    static final double LANCE_RANGE = 40.0;
    static final double LANCE_SPEED = 2.5;
    static final float LANCE_SCALE = 0.7f;                // as KnowledgeEmperorFx.lanceForm shows it
    static final double LANCE_RADIUS = 5.0;
    static final float LANCE_DAMAGE = 200f;               // at the middle; half at the edge
    static final int LANCE_STUN = 20;
    static final int LANCE_NAUSEA = 600;
    static final int HERMIT_LANCE_SEQUENCE = 0;           // Hermit: three, this far apart
    static final double LANCE_FAN = 12.0;

    // Condense Knowledge
    static final int CONDENSE_SEQUENCE = 1;
    static final int CONDENSE_COST = 10000;
    static final int CONDENSE_COOLDOWN = 1200;
    static final double CONDENSE_RADIUS = 32.0;
    static final float CONDENSE_SPIRITUALITY = 0.1f;      // of your bar back for every 100% gathered
    static final int CONDENSE_STRENGTH_TICKS = 200;       // Strength II this long
    static final int CONDENSE_INVERSE_TICKS = 200;        // with nothing gathered: the next hit within this

    // Embodiment of Knowledge
    static final int EMBODIMENT_SEQUENCE = 0;
    static final int EMBODIMENT_COST = 45000;
    static final int EMBODIMENT_COOLDOWN = 1200;
    static final int EMBODIMENT_TICKS = 300;

    // Indoctrination
    static final int INDOCTRINATION_SEQUENCE = 0;
    static final int INDOCTRINATION_COST = 40000;
    static final int INDOCTRINATION_COOLDOWN = 1000;
    static final double INDOCTRINATION_RADIUS = 24.0;
    static final int ENLIGHTENED_TICKS = 600;                     // allied Hermits: twice the spirituality back,
    static final float ENLIGHTENED_COOLDOWN = 0.7f;               // cooldowns 30% shorter
    static final int INDOCTRINATED_TICKS = 160;           // enemies: nausea, blindness, weakness

    // Divine Kingdom
    static final int KINGDOM_SEQUENCE = 0;
    static final int KINGDOM_COST = 60000;
    static final int KINGDOM_COOLDOWN = 3600;
    static final double KINGDOM_RADIUS = 40.0;
    static final double KINGDOM_HEIGHT = 24.0;
    static final int KINGDOM_TICKS = 1200;
    static final int KINGDOM_STUDY = 5;                   // each second, on each enemy inside
    static final float KINGDOM_TIDE = 2f;                 // spirituality comes back this much extra (three times in all)

    // =========================
    // Names and what is kept
    // =========================
    static final String INFO_UNTIL = "knowledge_emperor_info_until";
    static final String EMBODIED_UNTIL = "knowledge_emperor_embodied_until";
    static final String EMBODIED_SINCE = "knowledge_emperor_embodied_since";
    static final String ENLIGHTENED_UNTIL = "knowledge_emperor_enlightened_until";
    static final String KINGDOM = "knowledge_emperor_divine_kingdom";
    static final String CONDENSED_HIT = "knowledge_emperor_condensed";

    /** The false copy each informatized player left behind. Not saved. */
    static final class Decoy {
        final Vec3 at;
        final String tag;
        boolean burst;

        Decoy(Vec3 at, String tag) {
            this.at = at;
            this.tag = tag;
        }
    }
    static final Map<UUID, Decoy> DECOYS = new HashMap<>();

    static {
        // an Embodiment cannot be studied, read or chosen
        Study.addImmunity(KnowledgeEmperorKnowledge::isEmbodied);
        Spells.addUnchoosableCheck((chooser, target) -> isEmbodied(target));
    }

    // =========================
    // The key
    // =========================

    static final AbilityWheel WHEEL = AbilityWheel.create("knowledge_emperor_knowledge", "Knowledge")
            .radius(92)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .cooldownBy(KnowledgeEmperor::cooldownFor)
            .entry("informatization", KnowledgeEmperorFx.WHEEL_INFORMATIZATION, "Informatization", INFO_SEQUENCE,
                    INFO_COST, INFO_COOLDOWN, KnowledgeEmperorKnowledge::informatization)
            .costBy(KnowledgeEmperor::costFor)
            .free(player -> isInformatized(player) && Beyonder.hasSequence(player, EMPEROR_INFO_SEQUENCE))   // the flow
            .entry("erase", KnowledgeEmperorFx.WHEEL_ERASE, "Erase", ERASE_SEQUENCE,
                    ERASE_COST, ERASE_COOLDOWN, KnowledgeEmperorKnowledge::erase)
            .costBy(KnowledgeEmperor::costFor)
            .entry("knowledge_overload", KnowledgeEmperorFx.WHEEL_KNOWLEDGE_OVERLOAD, "Knowledge Overload", OVERLOAD_SEQUENCE,
                    OVERLOAD_COST, OVERLOAD_COOLDOWN, KnowledgeEmperorKnowledge::knowledgeOverload)
            .costBy(KnowledgeEmperor::costFor)
            .entry("lance_of_knowledge", KnowledgeEmperorFx.WHEEL_LANCE_OF_KNOWLEDGE, "Lance of Knowledge", LANCE_SEQUENCE,
                    LANCE_COST, LANCE_COOLDOWN, KnowledgeEmperorKnowledge::lanceOfKnowledge)
            .costBy(KnowledgeEmperor::costFor)
            .entry("condense_knowledge", KnowledgeEmperorFx.WHEEL_CONDENSE_KNOWLEDGE, "Condense Knowledge", CONDENSE_SEQUENCE,
                    CONDENSE_COST, CONDENSE_COOLDOWN, KnowledgeEmperorKnowledge::condenseKnowledge)
            .costBy(KnowledgeEmperor::costFor)
            .entry("embodiment_of_knowledge", KnowledgeEmperorFx.WHEEL_EMBODIMENT, "Embodiment of Knowledge", EMBODIMENT_SEQUENCE,
                    EMBODIMENT_COST, EMBODIMENT_COOLDOWN, KnowledgeEmperorKnowledge::embodiment)
            .costBy(KnowledgeEmperor::costFor)
            .entry("indoctrination", KnowledgeEmperorFx.WHEEL_INDOCTRINATION, "Indoctrination", INDOCTRINATION_SEQUENCE,
                    INDOCTRINATION_COST, INDOCTRINATION_COOLDOWN, KnowledgeEmperorKnowledge::indoctrination)
            .costBy(KnowledgeEmperor::costFor)
            .entry("divine_kingdom", KnowledgeEmperorFx.WHEEL_DIVINE_KINGDOM, "Divine Kingdom", KINGDOM_SEQUENCE,
                    KINGDOM_COST, KINGDOM_COOLDOWN, KnowledgeEmperorKnowledge::divineKingdom)
            .costBy(KnowledgeEmperor::costFor)
            .free(player -> Zone.isOpen(player, KINGDOM))                                                   // ending it
            .onCast((player, sl, entry) -> KnowledgeEmperor.afterCast(player, entry.cost()));

    // =========================
    // Ability 7 - Knowledge
    // Normal: the wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void knowledgeEmperorKnowledge(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KnowledgeEmperor.ready(player, bypassClassCheck, KNOWLEDGE_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Informatization: for INFO_TICKS you are status/Invulnerable (unhurt, unchosen, unheld) with Speed III, and a
    // false copy stands where you were: enemies within DECOY_LURE that hunted you go for it, and it bursts for
    // DECOY_DAMAGE within DECOY_RADIUS when one comes within DECOY_TRIGGER (or at the end). When you re-form,
    // INFO_RELEASED of what hit you meanwhile is dealt to every enemy within INFO_RELEASE_RADIUS.
    // Knowledge Emperor (1): EMPEROR_INFO_TICKS, and casting it again while it lasts (free) flows you to where you aim
    // through any gap, as Stellar Self does.
    // =========================
    static AbilityWheel.Result informatization(LivingEntity player, ServerLevel sl) {
        if (isInformatized(player)) {
            if (!Beyonder.hasSequence(player, EMPEROR_INFO_SEQUENCE)) {
                Spells.tell(player, Component.literal("You are already a stream of information."));
                return AbilityWheel.Result.REFUSED;
            }
            return flow(player, sl);
        }
        int ticks = Beyonder.hasSequence(player, EMPEROR_INFO_SEQUENCE) ? EMPEROR_INFO_TICKS : INFO_TICKS;
        long until = sl.getGameTime() + ticks;
        String tag = "ke_info_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putLong(INFO_UNTIL, until);
        Decoy decoy = new Decoy(player.position(), tag + "_decoy");
        DECOYS.put(player.getUUID(), decoy);
        KnowledgeEmperorFx.decoy(sl, decoy.at, player.getYRot(), ticks, decoy.tag);
        lure(player, sl, decoy);                                   // first: Invulnerable makes them forget you
        Invulnerable.grant(player, ticks);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, INFO_SPEED));
        KnowledgeEmperorFx.informatization(sl, player, ticks, tag);
        KnowledgeEmperorFx.status(player, Status.INFORMATIZATION, ticks);
        Later.run(sl, ticks, () -> {
            if (player.getPersistentData().getLongOr(INFO_UNTIL, 0L) != until) return;    // ended already
            endInformatization(player, sl, tag);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Knowledge Emperor: the stream flows to where you aim, through every gap starlight would pass. */
    private static AbilityWheel.Result flow(LivingEntity player, ServerLevel sl) {
        Vec3 to = KnowledgeEmperorStars.stellarSpot(player, sl, INFO_FLOW_RANGE);
        if (to == null || to.distanceToSqr(player.position()) < 1.0) {
            Spells.tell(player, Component.literal("There is no way for the stream to go there."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 from = player.position();
        player.teleportTo(to.x, to.y, to.z);
        player.fallDistance = 0;
        FallGuard.protect(player, 100);
        KnowledgeEmperorFx.informatizationTick(sl, player);
        KnowledgeEmperorFx.stellarCarry(sl, player, from, to);
        return AbilityWheel.Result.FREE;
    }

    /** The mobs that were after you go for the copy instead. */
    private static void lure(LivingEntity player, ServerLevel sl, Decoy decoy) {
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(DECOY_LURE),
                mob -> mob.isAlive() && mob.getTarget() == player)) {
            if (Spells.isFriend(player, mob)) continue;
            mob.setTarget(null);
            mob.getNavigation().moveTo(decoy.at.x, decoy.at.y, decoy.at.z, 1.2);
        }
    }

    /** Every tick while informatized: text trails off you, and the copy bursts when an enemy reaches it. */
    private static void informatizationTick(LivingEntity player, ServerLevel sl) {
        if (!isInformatized(player)) return;
        if (player.tickCount % 4 == 0) KnowledgeEmperorFx.informatizationTick(sl, player);
        Decoy decoy = DECOYS.get(player.getUUID());
        if (decoy == null || decoy.burst) return;
        if (player.tickCount % 5 == 0 && !Spells.enemiesAround(player, sl, decoy.at.add(0, 1, 0), DECOY_TRIGGER).isEmpty()) {
            burstDecoy(player, sl, decoy);
        }
    }

    private static void burstDecoy(LivingEntity player, ServerLevel sl, Decoy decoy) {
        if (decoy.burst) return;
        decoy.burst = true;
        KnowledgeEmperorFx.decoyBurst(sl, decoy.at, decoy.tag);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, decoy.at.add(0, 1, 0), DECOY_RADIUS)) {
            Study.strike(player, enemy, DECOY_DAMAGE, KnowledgeEmperor.ABILITY_STUDY);
        }
    }

    private static void endInformatization(LivingEntity player, ServerLevel sl, String tag) {
        player.getPersistentData().remove(INFO_UNTIL);
        Decoy decoy = DECOYS.remove(player.getUUID());
        if (decoy != null) burstDecoy(player, sl, decoy);
        float absorbed = Invulnerable.end(player);
        float released = absorbed * INFO_RELEASED;
        if (released > 0f) {
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), INFO_RELEASE_RADIUS)) {
                Spells.strike(player, enemy, released);
            }
        }
        KnowledgeEmperorFx.informatizationEnd(sl, player, tag, released > 0f);
        KnowledgeEmperorFx.statusEnd(player, Status.INFORMATIZATION);
    }

    static boolean isInformatized(LivingEntity player) {
        return KnowledgeEmperor.isActive(player, INFO_UNTIL);
    }

    // =========================
    // Erase: the enemy you look at (ERASE_RANGE). Something conjured (a summon, a decoy a Dispel rule names) is
    // deleted outright; anything else loses every beneficial effect and every ward and hiding (status/Dispel), at
    // KnowledgeEmperorFx.ERASE_LANDS.
    // =========================
    static AbilityWheel.Result erase(LivingEntity player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, ERASE_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("There is nothing before you to erase."));
            return AbilityWheel.Result.REFUSED;
        }
        KnowledgeEmperorFx.erase(sl, player, target);
        Later.run(sl, KnowledgeEmperorFx.ERASE_LANDS, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            if (Dispel.isConjured(target)) {
                KnowledgeEmperorFx.deleted(sl, target);
                Dispel.unmake(target);
                return;
            }
            int boons = Dispel.boons(target);
            int wards = Dispel.statuses(target);
            Study.add(player, target, KnowledgeEmperor.ABILITY_STUDY);
            Spells.tell(player, Component.literal("Erased from " + target.getDisplayName().getString() + ": "
                    + boons + (boons == 1 ? " blessing" : " blessings") + " and " + wards + (wards == 1 ? " ward." : " wards.")));
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Knowledge Overload: the enemy you look at (OVERLOAD_RANGE) is flooded: from KnowledgeEmperorFx.OVERLOAD_REACHES
    // its thoughts stop (held completely, OVERLOAD_HOLD; OVERLOAD_STRONG_HOLD for bosses and players), then it takes
    // OVERLOAD_DAMAGE. At 100% Study a mind that is neither a boss nor a player detonates: it dies, and every enemy
    // within MIND_BURST_RADIUS takes MIND_BURST_DAMAGE. Hermit (0): every enemy within HERMIT_OVERLOAD_RADIUS at once.
    // =========================
    static AbilityWheel.Result knowledgeOverload(LivingEntity player, ServerLevel sl) {
        String tag = "ke_overload_" + player.getId() + "_" + sl.getGameTime();
        if (Beyonder.hasSequence(player, HERMIT_OVERLOAD_SEQUENCE)) {
            List<LivingEntity> all = Spells.enemiesAround(player, sl, player.position(), HERMIT_OVERLOAD_RADIUS);
            all.removeIf(enemy -> !Spells.canBeChosen(player, enemy));
            if (all.isEmpty()) {
                Spells.tell(player, Component.literal("There is no mind near you to flood."));
                return AbilityWheel.Result.REFUSED;
            }
            KnowledgeEmperorFx.overloadAll(sl, player, all, OVERLOAD_HOLD, tag);
            for (LivingEntity target : all) overload(player, sl, target, tag);
            return AbilityWheel.Result.DONE;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, OVERLOAD_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("There is no mind before you to flood."));
            return AbilityWheel.Result.REFUSED;
        }
        KnowledgeEmperorFx.knowledgeOverload(sl, player, target, holdFor(target), tag);
        overload(player, sl, target, tag);
        return AbilityWheel.Result.DONE;
    }

    private static int holdFor(LivingEntity target) {
        return KnowledgeEmperor.isBoss(target) || target instanceof Player ? OVERLOAD_STRONG_HOLD : OVERLOAD_HOLD;
    }

    private static void overload(LivingEntity player, ServerLevel sl, LivingEntity target, String tag) {
        int hold = holdFor(target);
        Later.run(sl, KnowledgeEmperorFx.OVERLOAD_REACHES, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            Root.apply(target, hold, RootRestriction.EVERYTHING);
            if (target instanceof Player flooded) KnowledgeEmperorFx.status(flooded, Status.OVERLOADED, hold);
        });
        Later.run(sl, KnowledgeEmperorFx.OVERLOAD_REACHES + hold, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            KnowledgeEmperorFx.overloadHit(sl, target, tag);
            boolean detonates = Study.isUnderstood(player, target) && !KnowledgeEmperor.isBoss(target) && !(target instanceof Player);
            if (!detonates) {
                Study.strike(player, target, OVERLOAD_DAMAGE, KnowledgeEmperor.ABILITY_STUDY);
                return;
            }
            Vec3 at = target.getBoundingBox().getCenter();
            KnowledgeEmperorFx.mindBurst(sl, target, MIND_BURST_RADIUS);
            Spells.strikeTrue(player, target, target.getHealth() + target.getAbsorptionAmount() + 1000f);
            for (LivingEntity near : Spells.enemiesAround(player, sl, at, MIND_BURST_RADIUS)) {
                if (near != target) Study.strike(player, near, MIND_BURST_DAMAGE, KnowledgeEmperor.ABILITY_STUDY);
            }
        });
    }

    // =========================
    // Lance of Knowledge: the spear forms over your shoulder and is thrown at KnowledgeEmperorFx.LANCE_THROWN where
    // you aim (LANCE_RANGE). Where it stops it bursts: every enemy within LANCE_RADIUS takes LANCE_DAMAGE at the middle
    // down to half at the edge (and the Study bonus, which is "grows with Study"), is stunned LANCE_STUN and sick with
    // nausea for LANCE_NAUSEA. Hermit (0): three lances, LANCE_FAN degrees apart.
    // =========================
    static AbilityWheel.Result lanceOfKnowledge(LivingEntity player, ServerLevel sl) {
        int count = Beyonder.hasSequence(player, HERMIT_LANCE_SEQUENCE) ? 3 : 1;
        KnowledgeEmperorFx.lanceForm(sl, player, count);
        Later.run(sl, KnowledgeEmperorFx.LANCE_THROWN, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Vec3 hand = Spells.clearStart(player, sl, player.getEyePosition().add(Spells.rightOf(player).scale(0.5)).add(0, 0.2, 0));
            Vec3 aim = Spells.aimFrom(player, sl, hand, LANCE_RANGE);
            for (int i = 0; i < count; i++) {
                double turn = count <= 1 ? 0 : (i - (count - 1) / 2.0) * LANCE_FAN;
                Shot.of(KnowledgeEmperorFx.LANCE.scale(LANCE_SCALE))
                        .speed(LANCE_SPEED).range(LANCE_RANGE).width(0.5).tip(1.4 * LANCE_SCALE)
                        .onEnd((at, hitBlock) -> lanceBursts(player, sl, at))
                        .fire(player, sl, hand, Spells.turned(aim, turn));
            }
        });
        return AbilityWheel.Result.DONE;
    }

    private static void lanceBursts(LivingEntity player, ServerLevel sl, Vec3 at) {
        if (!Spells.casterStillHere(player, sl)) return;
        KnowledgeEmperorFx.lanceImpact(sl, at, LANCE_RADIUS);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, at, LANCE_RADIUS)) {
            double far = Math.min(1.0, Spells.distanceToBody(enemy, at) / LANCE_RADIUS);
            Study.strike(player, enemy, (float) (LANCE_DAMAGE * (1.0 - 0.5 * far)), KnowledgeEmperor.ABILITY_STUDY);
            if (!enemy.isAlive()) continue;
            Root.apply(enemy, LANCE_STUN, RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.ATTACK);
            enemy.addEffect(new MobEffectInstance(MobEffects.NAUSEA, LANCE_NAUSEA, 0));
        }
    }

    // =========================
    // Condense Knowledge: all your Study on everything within CONDENSE_RADIUS is gathered (Study.takeAround) and taken
    // in at KnowledgeEmperorFx.CONDENSED: CONDENSE_SPIRITUALITY of your bar back for every 100%, and Strength II for
    // CONDENSE_STRENGTH_TICKS. With nothing to gather, your next hit within CONDENSE_INVERSE_TICKS counts as full
    // Study (status/EmpoweredHit).
    // =========================
    static AbilityWheel.Result condenseKnowledge(LivingEntity player, ServerLevel sl) {
        List<Study.Known> taken = Study.takeAround(player, sl, player.position(), CONDENSE_RADIUS);
        int total = 0;
        List<LivingEntity> sources = new ArrayList<>();
        for (Study.Known known : taken) {
            total += known.percent();
            sources.add(known.target());
        }
        if (total <= 0) {
            String tag = "ke_condensed_" + player.getId();
            KnowledgeEmperorFx.condenseInverse(sl, player, CONDENSE_INVERSE_TICKS, tag);
            KnowledgeEmperorFx.status(player, Status.CONDENSED, CONDENSE_INVERSE_TICKS);
            EmpoweredHit.armAny(player, CONDENSED_HIT, CONDENSE_INVERSE_TICKS, (striker, target, amount, level) -> {
                KnowledgeEmperorFx.statusEnd(striker, Status.CONDENSED);
                if (striker instanceof ServerPlayer viewer) KnowledgeEmperorFx.understood(viewer, target);
                float known = Study.bonus(striker, target);                         // what Study gives it already
                return amount * (1f + Study.MOST_BONUS) / Math.max(1f, known);
            });
            return AbilityWheel.Result.DONE;
        }
        int gathered = total;
        KnowledgeEmperorFx.condenseKnowledge(sl, player, sources);
        Later.run(sl, KnowledgeEmperorFx.CONDENSED, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Spells.giveSpirituality(player, Spells.maxSpiritualityOf(player) * CONDENSE_SPIRITUALITY * gathered / 100f);
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, CONDENSE_STRENGTH_TICKS, 1));
            KnowledgeEmperorFx.status(player, Status.CONDENSED, CONDENSE_STRENGTH_TICKS);
            Spells.tell(player, Component.literal("You condense " + gathered + "% of knowledge."));
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Embodiment of Knowledge: for EMBODIMENT_TICKS you are concealed (status/Concealment) as your own information:
    // nothing can study you, choose you, read you with Prying or find you by Dowsing. Casting any other ability ends
    // it (KnowledgeEmperor.afterCast), and so does stepping out of the concealment (a blow you strike).
    // =========================
    static AbilityWheel.Result embodiment(LivingEntity player, ServerLevel sl) {
        long now = sl.getGameTime();
        CompoundTag data = player.getPersistentData();
        data.putLong(EMBODIED_UNTIL, now + EMBODIMENT_TICKS);
        data.putLong(EMBODIED_SINCE, now);
        Concealment.hide(player, EMBODIMENT_TICKS);
        KnowledgeEmperorFx.embodiment(sl, player);
        KnowledgeEmperorFx.status(player, Status.EMBODIMENT, EMBODIMENT_TICKS);
        long until = now + EMBODIMENT_TICKS;
        Later.run(sl, EMBODIMENT_TICKS, () -> {
            if (player.getPersistentData().getLongOr(EMBODIED_UNTIL, 0L) == until) endEmbodiment(player, sl);
        });
        return AbilityWheel.Result.DONE;
    }

    static boolean isEmbodied(Entity entity) {
        return entity instanceof LivingEntity one && KnowledgeEmperor.isActive(one, EMBODIED_UNTIL);      // a player or a Beyonder mob
    }

    private static void endEmbodiment(LivingEntity player, ServerLevel sl) {
        if (!player.getPersistentData().contains(EMBODIED_UNTIL)) return;
        player.getPersistentData().remove(EMBODIED_UNTIL);
        player.getPersistentData().remove(EMBODIED_SINCE);
        Concealment.reveal(player);
        KnowledgeEmperorFx.embodimentEnd(sl, player);
        KnowledgeEmperorFx.statusEnd(player, Status.EMBODIMENT);
    }

    /** Any cast went off (KnowledgeEmperor.afterCast): an Embodiment cast before this tick ends. */
    static void castSomething(LivingEntity player) {
        if (!isEmbodied(player) || !(player.level() instanceof ServerLevel sl)) return;
        if (player.getPersistentData().getLongOr(EMBODIED_SINCE, 0L) == sl.getGameTime()) return;   // the Embodiment itself
        endEmbodiment(player, sl);
    }

    // =========================
    // Indoctrination: a pulse of knowledge over INDOCTRINATION_RADIUS. Allied Hermits (Knowledge Emperors) are
    // enlightened for ENLIGHTENED_TICKS: spirituality comes back twice as fast and their cooldowns are 30% shorter
    // (KnowledgeEmperor.cooldownFor). Enemies get nausea, blindness and weakness for INDOCTRINATED_TICKS, and are
    // Understood (Study.set to 100).
    // =========================
    static AbilityWheel.Result indoctrination(LivingEntity player, ServerLevel sl) {
        KnowledgeEmperorFx.indoctrination(sl, player, INDOCTRINATION_RADIUS);
        long until = sl.getGameTime() + ENLIGHTENED_TICKS;
        for (LivingEntity ally : Spells.alliesAround(player, sl, player.position(), INDOCTRINATION_RADIUS)) {
            if (!(ally instanceof Player hermit) || !SoulCore.getPathway(hermit).equals(KnowledgeEmperor.PATHWAY)) continue;
            hermit.getPersistentData().putLong(ENLIGHTENED_UNTIL, until);
            KnowledgeEmperorFx.enlightened(sl, hermit, ENLIGHTENED_TICKS, "ke_enlightened_" + hermit.getId());
            KnowledgeEmperorFx.status(hermit, Status.ENLIGHTENED, ENLIGHTENED_TICKS);
        }
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), INDOCTRINATION_RADIUS)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.NAUSEA, INDOCTRINATED_TICKS, 0));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, INDOCTRINATED_TICKS, 0));
            Blind.apply(enemy, INDOCTRINATED_TICKS);
            Study.set(player, enemy, Study.FULL);
            KnowledgeEmperorFx.indoctrinated(sl, enemy, INDOCTRINATED_TICKS, "ke_indoctrinated_" + enemy.getId());
            if (enemy instanceof Player taught) KnowledgeEmperorFx.status(taught, Status.INDOCTRINATED, INDOCTRINATED_TICKS);
        }
        return AbilityWheel.Result.DONE;
    }

    static boolean isEnlightened(LivingEntity player) {
        return KnowledgeEmperor.isActive(player, ENLIGHTENED_UNTIL);
    }

    // =========================
    // Divine Kingdom: the Knowledge Moor opens KINGDOM_RADIUS round where you stand, for KINGDOM_TICKS once risen
    // (cast again to end it, free). Each second inside it: hidden enemies are forced out (Concealment, Unnoticed) and
    // every enemy gains KINGDOM_STUDY Study; a mob readying a blow at you or an ally shows its ghost; you and allied
    // Hermits get spirituality back three times as fast; your pouch is kept full. While you stand in it your casts cost
    // half (KnowledgeEmperor.costFor).
    // =========================
    static AbilityWheel.Result divineKingdom(LivingEntity player, ServerLevel sl) {
        if (Zone.isOpen(player, KINGDOM)) {
            Zone.close(player, KINGDOM);
            return AbilityWheel.Result.FREE;
        }
        Vec3 ground = Spells.groundUnder(player, sl, player);
        String tag = "ke_kingdom_" + player.getId() + "_" + sl.getGameTime();
        int risen = KnowledgeEmperorFx.KINGDOM_RISEN;
        KnowledgeEmperorFx.divineKingdom(sl, player, KINGDOM_RADIUS, KINGDOM_TICKS + risen, tag);
        KnowledgeEmperorFx.status(player, Status.KINGDOM, KINGDOM_TICKS + risen);
        Later.run(sl, risen, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                KnowledgeEmperorFx.kingdomEnd(sl, ground, KINGDOM_RADIUS, tag);
                return;
            }
            Zone.at(ground, KINGDOM_RADIUS).height(KINGDOM_HEIGHT).lasts(KINGDOM_TICKS).every(20)
                    .affects(one -> true)
                    .onInside((one, inside) -> kingdomTouches(player, sl, one))
                    .onPulse(left -> {
                        KnowledgeEmperorFx.kingdomTick(sl, ground, KINGDOM_RADIUS);
                        if (inKingdom(player)) {
                            tide(player, KINGDOM_TIDE);
                            if (player instanceof Player reader) {                    // a pouch is a player's
                                while (Scrolls.size(reader) < KnowledgeEmperor.pouchSize(reader)) KnowledgeEmperor.giveFreeScroll(reader, sl);
                            }
                        }
                    })
                    .onEnd(() -> {
                        KnowledgeEmperorFx.kingdomEnd(sl, ground, KINGDOM_RADIUS, tag);
                        KnowledgeEmperorFx.statusEnd(player, Status.KINGDOM);
                    })
                    .open(player, sl, KINGDOM);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Once a second, for each one inside the Moor. */
    private static void kingdomTouches(LivingEntity player, ServerLevel sl, LivingEntity one) {
        if (Spells.isFriend(player, one)) {
            if (one instanceof Player hermit && SoulCore.getPathway(hermit).equals(KnowledgeEmperor.PATHWAY)) tide(hermit, KINGDOM_TIDE);
            return;
        }
        if (Concealment.isHidden(one) && !isEmbodied(one)) {
            Concealment.reveal(one);
            KnowledgeEmperorFx.kingdomForcedOut(sl, one);
        }
        if (Unnoticed.isUnnoticed(one)) {
            Unnoticed.end(one);
            KnowledgeEmperorFx.kingdomForcedOut(sl, one);
        }
        Study.add(player, one, KINGDOM_STUDY);
        if (one instanceof Mob mob && mob.getTarget() != null && Spells.isFriend(player, mob.getTarget())) {
            LivingEntity aimed = mob.getTarget();
            Vec3 at = aimed.getBoundingBox().getCenter().add(mob.position().subtract(aimed.position()).normalize().scale(0.6));
            KnowledgeEmperorFx.kingdomGhost(sl, mob, at);
        }
    }

    /** True while the player stands in their own Divine Kingdom. */
    static boolean inKingdom(LivingEntity player) {
        return Zone.covers(player, KINGDOM, player.position());
    }

    /** 'times' more of a second's spirituality regeneration, now. */
    static void tide(LivingEntity player, float times) {
        Spells.giveSpirituality(player, Spells.spiritualityRegenOf(player) * 20f * times);
    }

    // =========================
    // Hooks from KnowledgeEmperor
    // =========================

    /** Every tick, for every player. */
    static void tick(LivingEntity player, ServerLevel sl) {
        informatizationTick(player, sl);
        // the Embodiment ends with the concealment (a blow struck steps out of it)
        if (isEmbodied(player) && !Concealment.isHidden(player)) endEmbodiment(player, sl);
    }

    /** After a relog or a death. */
    static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(INFO_UNTIL, EMBODIED_UNTIL, EMBODIED_SINCE)) data.remove(key);
        DECOYS.remove(player.getUUID());
        Invulnerable.end(player);
        Zone.close(player, KINGDOM);
    }

    static void forget(LivingEntity player) {
        DECOYS.remove(player.getUUID());
    }
}
