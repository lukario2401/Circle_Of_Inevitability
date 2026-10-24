package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderFx.Status;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Chosen;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.Dispel;
import net.lukario.circleofinevitability.status.NoHeal;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.List;

/**
 * Hand Of Order, key 7: the Order wheel (Balancer, Sequence 2). docs/HAND_OF_ORDER.md, "Key 7 wheel".
 *
 *   Balance (2)                 the enemy you look at is pulled down to your weight for a while: its damage, armor,
 *                               speed and max health lowered to yours (beyond/Physique); the heavier, the shorter
 *   Divide the Battlefield (2)  a ring of Order round you (combat/Dome): nothing crosses it either way, enemy shots
 *                               end at it, and enemies too strong for the scales are thrown out first
 *   Law: No Return (2)          "Resurrection of the dead is prohibited here!": a status/Decree of RESURRECTION and
 *                               MARIONETTES; it takes one of the Decree places (HandOfOrderDecrees.writeLaw)
 *   Order Proxy (1)             for a few seconds Strength, Resistance, Speed, no Rule, root or Silence holds you and
 *                               every ability is free with no cooldown; then a long weariness and half your spirituality
 *   Quell Disorder (1)          Order restores itself round you: what enemies made with abilities is undone (status/
 *                               Dispel), their zones, Decrees and domes end; allies are cleansed
 *   Court of Judgment (0)       the Ancient Court: a Decree whose clauses are written one every COURT_CLAUSE_EVERY,
 *                               and a sentence for each breach that grows to Death. Again: it adjourns, free
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost). The looks are HandOfOrderFx's;
 * every number is right below. Order Proxy's "free, no cooldown" is read by every cost and cooldown (HandOfOrder's
 * HandOfOrderEvents add the scales).
 */
public final class HandOfOrderOrder {

    private HandOfOrderOrder() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks). Costs from Sequence 2 up are
    // placeholders until the spirituality pool there is known (docs/HAND_OF_ORDER.md).
    // =========================

    static final int ORDER_SEQUENCE = 2;                         // the key
    private static final float QUICK_CAST_COST = 2f;

    // Balance
    private static final int BALANCE_SEQUENCE = 2;
    private static final int BALANCE_COST = 20000;
    private static final int BALANCE_COOLDOWN = 800;
    private static final double BALANCE_RANGE = 24.0;
    private static final int BALANCE_TICKS = 400;               // x your max health / its max health...
    private static final int BALANCE_MIN_TICKS = 60;            // ...never under this (a boss: this)

    // Divide the Battlefield
    private static final int DIVIDE_SEQUENCE = 2;
    private static final int DIVIDE_COST = 20000;
    private static final int DIVIDE_COOLDOWN = 1200;
    private static final double DIVIDE_RADIUS = 10.0;           // 20 blocks wide
    private static final int DIVIDE_TICKS = 500;
    private static final float DIVIDE_TOO_STRONG = 3f;          // enemies with this many times your max health are thrown out
    private static final double DIVIDE_THROWN = 4.0;            // this far past the edge

    // Law: No Return
    private static final int NO_RETURN_SEQUENCE = 2;
    private static final int NO_RETURN_COST = 20000;
    private static final int NO_RETURN_COOLDOWN = 1200;
    private static final double NO_RETURN_RADIUS = 20.0;
    private static final int NO_RETURN_TICKS = 900;

    // Order Proxy
    private static final int PROXY_SEQUENCE = 1;
    private static final int PROXY_COST = 30000;
    private static final int PROXY_COOLDOWN = 2400;
    private static final int PROXY_TICKS = 120;
    private static final int PROXY_JUSTICIAR_TICKS = 200;       // Justiciar (0): longer...
    private static final int SPENT_TICKS = 600;                 // after it: Weakness III and Slowness II this long...
    private static final int SPENT_JUSTICIAR_TICKS = 300;       // ...(Justiciar: half as long)
    private static final float SPENT_SPIRITUALITY = 0.5f;       // ...and this part of your spirituality is gone

    // Quell Disorder
    private static final int QUELL_SEQUENCE = 1;
    private static final int QUELL_COST = 30000;
    private static final int QUELL_COOLDOWN = 900;
    private static final double QUELL_RADIUS = 24.0;

    // Court of Judgment
    private static final int COURT_SEQUENCE = 0;
    private static final int COURT_COST = 60000;
    private static final int COURT_COOLDOWN = 3600;
    private static final double COURT_RADIUS = 40.0;
    private static final double COURT_HEIGHT = 16.0;
    private static final int COURT_TICKS = 1200;
    private static final int COURT_CLAUSE_EVERY = 200;          // one more clause this often (the first at once)
    private static final int SENTENCE_STAGGER = 20;             // 1st breach: a warning, staggered...
    private static final int SENTENCE_MARK = 100;               // ...and marked (glowing) this long
    private static final float SENTENCE_DAMAGE_2 = 60f;         // 2nd: this much...
    private static final int SENTENCE_HOLD_2 = 40;              // ...and held
    private static final float SENTENCE_DAMAGE_3 = 150f;        // 3rd: this much, held, and the Sword of Judgment falls free
    private static final int SENTENCE_HOLD_3 = 80;
    private static final float SENTENCE_DEATH = 200f;           // 4th: Death, at once...
    private static final int SENTENCE_NO_HEAL = 200;            // ...and no healing this long

    /** The Court's clauses, in the order they are written (docs/HAND_OF_ORDER.md). */
    private static final Decree.Rule[] CLAUSES = {
            Decree.Rule.PASSAGE, Decree.Rule.CONCEALMENT, Decree.Rule.RESURRECTION, Decree.Rule.STAND_INS,
            Decree.Rule.HEALING, Decree.Rule.PROJECTILES, Decree.Rule.ABILITIES};

    // =========================
    // Names and what is kept
    // =========================
    static final String BALANCED = "hand_of_order_balanced";                 // status/Chosen
    private static final String DIVIDE = "hand_of_order_divide";             // combat/Dome
    private static final String COURT = "hand_of_order_court";               // status/Decree
    private static final String PROXY_UNTIL = "hand_of_order_proxy_until";   // on the player: game time it ends
    private static final String PROXY_ON = "hand_of_order_proxy_on";         // ...and that its end is still to come

    /** The four things Balance weighs, each with its own modifier. */
    private static final List<Holder<Attribute>> WEIGHED = List.of(
            Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.MOVEMENT_SPEED, Attributes.MAX_HEALTH);
    private static final List<Identifier> WEIGHED_IDS = List.of(
            Physique.id("hand_of_order_balance_attack"), Physique.id("hand_of_order_balance_armor"),
            Physique.id("hand_of_order_balance_speed"), Physique.id("hand_of_order_balance_health"));

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("hand_of_order_order", "Order")
            .radius(78)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .cooldownBy(HandOfOrder::cooldownFor)
            .entry("balance", HandOfOrderFx.WHEEL_BALANCE, "Balance", BALANCE_SEQUENCE,
                    BALANCE_COST, BALANCE_COOLDOWN, HandOfOrderOrder::balance)
            .costBy(HandOfOrder::costFor)
            .entry("divide_battlefield", HandOfOrderFx.WHEEL_DIVIDE_BATTLEFIELD, "Divide the Battlefield", DIVIDE_SEQUENCE,
                    DIVIDE_COST, DIVIDE_COOLDOWN, HandOfOrderOrder::divideBattlefield)
            .costBy(HandOfOrder::costFor)
            .entry("law_no_return", HandOfOrderFx.WHEEL_LAW_NO_RETURN, "Law: No Return", NO_RETURN_SEQUENCE,
                    NO_RETURN_COST, NO_RETURN_COOLDOWN, HandOfOrderOrder::lawNoReturn)
            .costBy(HandOfOrder::decreeCostFor)
            .entry("order_proxy", HandOfOrderFx.WHEEL_ORDER_PROXY, "Order Proxy", PROXY_SEQUENCE,
                    PROXY_COST, PROXY_COOLDOWN, HandOfOrderOrder::orderProxy)
            .costBy(HandOfOrder::costFor)
            .entry("quell_disorder", HandOfOrderFx.WHEEL_QUELL_DISORDER, "Quell Disorder", QUELL_SEQUENCE,
                    QUELL_COST, QUELL_COOLDOWN, HandOfOrderOrder::quellDisorder)
            .costBy(HandOfOrder::costFor)
            .entry("court_of_judgment", HandOfOrderFx.WHEEL_COURT_OF_JUDGMENT, "Court of Judgment", COURT_SEQUENCE,
                    COURT_COST, COURT_COOLDOWN, HandOfOrderOrder::courtOfJudgment)
            .costBy(HandOfOrder::costFor);

    // =========================
    // Ability 7 - Order
    // Normal: the Order wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void handOfOrderOrder(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!HandOfOrder.ready(player, bypassClassCheck, ORDER_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Balance. The enemy you look at (BALANCE_RANGE), heavier than you (more max health, or a boss): for BALANCE_TICKS
    // x your max health / its max health (at least BALANCE_MIN_TICKS; a boss: that), each of WEIGHED that it has more
    // of than you is lowered to yours (status/Chosen; it ends if either dies). Weaker enemies are left alone.
    // =========================
    private static AbilityWheel.Result balance(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, BALANCE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to weigh."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean boss = HandOfOrder.isBoss(target);
        float mine = player.getMaxHealth();
        float its = target.getMaxHealth();
        if (!boss && its <= mine) {
            player.sendSystemMessage(Component.literal("It is no heavier than you: the scales are already level."));
            return AbilityWheel.Result.REFUSED;
        }
        int ticks = boss ? BALANCE_MIN_TICKS : Math.max(BALANCE_MIN_TICKS, Math.round(BALANCE_TICKS * mine / its));
        Chosen.release(player, BALANCED);                    // one at a time: the last one is let go first
        Chosen.choose(player, BALANCED, target, ticks);
        HandOfOrderFx.balance(sl, player, target, ticks, balanceTag(player));
        Later.run(sl, HandOfOrderFx.BALANCE_SETTLED, () -> {
            if (Chosen.is(target, player, BALANCED)) weigh(player, target);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Each thing it has more of than 'player' is lowered to theirs. */
    private static void weigh(Player player, LivingEntity target) {
        for (int i = 0; i < WEIGHED.size(); i++) {
            Holder<Attribute> attribute = WEIGHED.get(i);
            Identifier id = WEIGHED_IDS.get(i);
            Physique.remove(target, attribute, id);
            AttributeInstance its = target.getAttribute(attribute);
            AttributeInstance mine = player.getAttribute(attribute);
            if (its == null || mine == null || its.getValue() <= mine.getValue() || its.getValue() <= 0) continue;
            double down = mine.getValue() / its.getValue() - 1.0;
            Physique.keep(target, attribute, id, down, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
        }
    }

    private static String balanceTag(Player player) {
        return "ho_balance_" + player.getId();
    }

    // =========================
    // Divide the Battlefield. A ring DIVIDE_RADIUS round you for DIVIDE_TICKS (combat/Dome, keepsEnemiesIn and
    // stopsProjectiles: nothing of theirs crosses it either way). First, every enemy inside with DIVIDE_TOO_STRONG times
    // your max health (and every boss) is thrown out past the edge, so it cannot come back while the ring stands.
    // =========================
    private static AbilityWheel.Result divideBattlefield(ServerPlayer player, ServerLevel sl) {
        Vec3 center = player.position();
        float mine = player.getMaxHealth();
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, center, DIVIDE_RADIUS)) {
            if (!HandOfOrder.isBoss(enemy) && enemy.getMaxHealth() <= mine * DIVIDE_TOO_STRONG) continue;
            throwOut(player, sl, center, enemy);
        }
        Dome.at(center, DIVIDE_RADIUS)
                .lasts(DIVIDE_TICKS)
                .keepsEnemiesIn()
                .stopsProjectiles()
                .outsiders(one -> !Spells.isFriend(player, one))        // a hidden enemy is held too
                .onBlock(at -> HandOfOrderFx.divideBlocked(sl, at))
                .onEnd(() -> {
                    HandOfOrderFx.divideEnd(sl, center, DIVIDE_RADIUS, divideTag(player));
                    HandOfOrderFx.statusEnd(player, Status.DIVIDED);
                })
                .raise(player, sl, DIVIDE);
        HandOfOrderFx.divideBattlefield(sl, player, center, DIVIDE_RADIUS, DIVIDE_TICKS, divideTag(player));
        return AbilityWheel.Result.DONE;
    }

    private static void throwOut(Player player, ServerLevel sl, Vec3 center, LivingEntity enemy) {
        Vec3 away = new Vec3(enemy.getX() - center.x, 0, enemy.getZ() - center.z);
        if (away.lengthSqr() < 0.01) away = Spells.flatLook(player);
        Vec3 out = Spells.groundAt(player, sl, center.add(away.normalize().scale(DIVIDE_RADIUS + DIVIDE_THROWN)).add(0, 2, 0));
        HandOfOrderFx.thrownOut(sl, enemy);
        enemy.teleportTo(out.x, out.y, out.z);
        Spells.push(enemy, center, 0.6, 0.3);
    }

    private static String divideTag(Player player) {
        return "ho_divide_" + player.getId();
    }

    // =========================
    // Law: No Return. NO_RETURN_RADIUS, NO_RETURN_TICKS (through decreeTicksFor): a Decree of RESURRECTION and
    // MARIONETTES in one of the Decree places, with the Decrees' Violation ladder.
    // =========================
    private static AbilityWheel.Result lawNoReturn(ServerPlayer player, ServerLevel sl) {
        Vec3 center = HandOfOrderDecrees.placeFor(player, sl);
        if (center == null) {
            player.sendSystemMessage(Component.literal("There is no ground there to write on."));
            return AbilityWheel.Result.REFUSED;
        }
        HandOfOrderDecrees.writeLaw(player, sl, "law_no_return", center, NO_RETURN_RADIUS,
                HandOfOrder.decreeTicksFor(player, NO_RETURN_TICKS), HandOfOrderFx.Rule.RESURRECTION,
                EnumSet.of(Decree.Rule.RESURRECTION, Decree.Rule.MARIONETTES));
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Order Proxy. For PROXY_TICKS (Justiciar: PROXY_JUSTICIAR_TICKS): Strength IV, Resistance III, Speed II; roots and
    // Silence come off every tick, no Rule binds you (HandOfOrder's Decree.addImmunity), and every ability costs
    // nothing and has no cooldown (its cost and cooldown scales). It starts a tick after the cast, so its own cost and
    // cooldown are paid. When it ends: Weakness III and Slowness II for SPENT_TICKS (Justiciar: SPENT_JUSTICIAR_TICKS)
    // and SPENT_SPIRITUALITY of your spirituality is gone.
    // =========================
    private static AbilityWheel.Result orderProxy(ServerPlayer player, ServerLevel sl) {
        if (isProxy(player)) {
            player.sendSystemMessage(Component.literal("You already act as the proxy of Order."));
            return AbilityWheel.Result.REFUSED;
        }
        int ticks = Beyonder.hasSequence(player, COURT_SEQUENCE) ? PROXY_JUSTICIAR_TICKS : PROXY_TICKS;
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 3));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 2));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1));
        HandOfOrderFx.orderProxy(sl, player, ticks, proxyTag(player));
        Later.run(sl, 1, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            CompoundTag data = player.getPersistentData();
            data.putLong(PROXY_UNTIL, sl.getGameTime() + ticks - 1);
            data.putBoolean(PROXY_ON, true);
        });
        return AbilityWheel.Result.DONE;
    }

    /** True while this player acts as the proxy of Order (every cost and cooldown asks). */
    public static boolean isProxy(Player player) {
        return player.getPersistentData().getLongOr(PROXY_UNTIL, 0L) > player.level().getGameTime();
    }

    private static String proxyTag(Player player) {
        return "ho_proxy_" + player.getId();
    }

    /** Every tick for a Hand Of Order: the proxy keeps its freedom, and pays when it ends. */
    static void tick(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBooleanOr(PROXY_ON, false)) return;
        if (isProxy(player)) {
            if (Root.isRooted(player)) Root.remove(player);              // roots and Silence (a root) do not hold it
            if (player.tickCount % 4 == 0) HandOfOrderFx.proxyTick(sl, player);
            return;
        }
        data.remove(PROXY_ON);
        data.remove(PROXY_UNTIL);
        int spent = Beyonder.hasSequence(player, COURT_SEQUENCE) ? SPENT_JUSTICIAR_TICKS : SPENT_TICKS;
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, spent, 2));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, spent, 1));
        SoulCore.setSpiritualityQuietly(player, SoulCore.getSpirituality(player) * (1f - SPENT_SPIRITUALITY));  // not "spent on a cast"
        HandOfOrderFx.proxyEnd(sl, player, proxyTag(player));
        HandOfOrderFx.status(player, Status.SPENT, spent);
    }

    // =========================
    // Quell Disorder. Within QUELL_RADIUS: on each enemy (hidden ones too) what it made with abilities is undone
    // (status/Dispel: Concealment, marionette strings, substitutes, death wards, damage links...); every zone, Decree and
    // dome an enemy has there ends. Allies (and you) are cleansed of harmful effects.
    // =========================
    private static AbilityWheel.Result quellDisorder(ServerPlayer player, ServerLevel sl) {
        Vec3 at = player.position();
        HandOfOrderFx.quellDisorder(sl, player, QUELL_RADIUS);
        int quelled = 0;
        for (LivingEntity one : Spells.livingAround(player, sl, at, QUELL_RADIUS)) {
            if (Spells.isFriend(player, one)) {
                Spells.cleanse(one);
                HandOfOrderFx.cleansed(sl, one);
                continue;
            }
            if (Dispel.statuses(one) > 0) {
                HandOfOrderFx.quelled(sl, one);
                quelled++;
            }
        }
        Spells.cleanse(player);
        HandOfOrderFx.cleansed(sl, player);
        int fields = Zone.closeNear(sl, at, QUELL_RADIUS, owner -> !Spells.isFriend(player, owner));
        for (Dome.Raised dome : Dome.raisedAt(sl, at)) {
            if (!Spells.isFriend(player, dome.owner())) {
                Dome.close(dome.owner(), dome.name());
                fields++;
            }
        }
        player.sendSystemMessage(Component.literal("Order is restored: " + quelled + " quelled, " + fields + " fields ended."));
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Court of Judgment. A status/Decree COURT_RADIUS round you for COURT_TICKS, its clauses (CLAUSES) written one at
    // once and one more every COURT_CLAUSE_EVERY. Each breach is Judged, and the sentence grows: 1st a warning
    // (staggered, marked), 2nd SENTENCE_DAMAGE_2 and held, 3rd SENTENCE_DAMAGE_3, held, and the Sword of Judgment falls
    // on it free (HandOfOrder.judge), 4th and on Death: SENTENCE_DEATH and no healing. Cast again: it adjourns (free).
    // =========================
    private static AbilityWheel.Result courtOfJudgment(ServerPlayer player, ServerLevel sl) {
        if (Decree.isWritten(player, COURT)) {
            Decree.close(player, COURT);
            player.sendSystemMessage(Component.literal("The Court is adjourned."));
            return AbilityWheel.Result.FREE;
        }
        Vec3 center = player.position();
        String tag = courtTag(player);
        int[] written = {0};
        Decree.at(center, COURT_RADIUS)
                .height(COURT_HEIGHT)
                .lasts(COURT_TICKS)
                .onViolation((who, rule, count, level) -> sentence(player, who, count, level))
                .onPulse(COURT_CLAUSE_EVERY, left -> {
                    if (written[0] >= CLAUSES.length) return;
                    Decree.forbid(player, COURT, CLAUSES[written[0]]);
                    written[0]++;
                    HandOfOrderFx.clause(sl, center, COURT_RADIUS, written[0]);
                    HandOfOrderFx.courtClauses(player, written[0], left);
                })
                .onEnd(() -> {
                    HandOfOrderFx.adjourn(sl, center, COURT_RADIUS, tag);
                    HandOfOrderFx.courtClausesHide(player);
                    HandOfOrderFx.statusEnd(player, Status.COURT);
                })
                .write(player, sl, COURT);
        HandOfOrderFx.court(sl, player, center, COURT_RADIUS, COURT_TICKS, tag);
        return AbilityWheel.Result.DONE;
    }

    private static void sentence(Player owner, LivingEntity who, int breach, ServerLevel sl) {
        HandOfOrderFx.judged(sl, who, Math.min(4, breach));
        Later.run(sl, 1, () -> {                                     // out of whatever event the breach was caught in
            if (!who.isAlive() || !Spells.casterStillHere(owner, sl)) return;
            if (breach <= 1) {
                Root.apply(who, SENTENCE_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
                Spells.keepEffect(who, MobEffects.GLOWING, 0, SENTENCE_MARK);
            } else if (breach == 2) {
                Spells.strikeTrue(owner, who, SENTENCE_DAMAGE_2);
                Root.apply(who, SENTENCE_HOLD_2, RootRestriction.EVERYTHING);
            } else if (breach == 3) {
                Spells.strikeTrue(owner, who, SENTENCE_DAMAGE_3);
                if (!who.isAlive()) return;
                Root.apply(who, SENTENCE_HOLD_3, RootRestriction.EVERYTHING);
                HandOfOrder.judge(owner, sl, who, HandOfOrderVerdicts.strideSpot(owner, sl, who));   // free and instant
            } else {
                NoHeal.apply(who, SENTENCE_NO_HEAL);
                Spells.strikeTrue(owner, who, SENTENCE_DEATH);
            }
        });
    }

    private static String courtTag(Player player) {
        return "ho_court_" + player.getId();
    }

    // =========================
    // Endings
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class OrderEvents {

        private OrderEvents() {}

        static {
            // Balance ends however it ends (time, either one dies, another weighed): the weight comes back
            Chosen.addListener(BALANCED, (chooser, target) -> {
                if (target != null) {
                    for (int i = 0; i < WEIGHED.size(); i++) Physique.remove(target, WEIGHED.get(i), WEIGHED_IDS.get(i));
                }
                if (chooser != null && chooser.level() instanceof ServerLevel sl) {
                    HandOfOrderFx.balanceEnd(sl, chooser, target, balanceTag(chooser));
                }
            });
        }
    }

    /** After a relog or a death: no proxy left half-running (and no price for it either). */
    static void clearState(Player player) {
        player.getPersistentData().remove(PROXY_ON);
        player.getPersistentData().remove(PROXY_UNTIL);
    }
}
