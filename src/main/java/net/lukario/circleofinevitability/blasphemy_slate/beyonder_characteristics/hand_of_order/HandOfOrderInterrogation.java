package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order;

import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.WeaponCoat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand Of Order, key 3: the Interrogation wheel (Interrogator, Sequence 7). docs/HAND_OF_ORDER.md, "Key 3 wheel".
 *
 *   Psychic Lashing    the weapon in hand (or the fist) is an Illusory Torture Device for a while: every hit adds
 *                      lightning that no armor stops, and staggers (status/WeaponCoat)
 *   Psychic Piercing   lightning leaps from the eyes in a thin beam: the first few enemies on it are struck through
 *                      armor, spirits and marionettes twice as hard
 *   Whip of Pain       a barbed whip of lightning lashes the mind of the enemy you look at: it trembles, cannot hide,
 *                      and every effect on it is told to you
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost), as for Hand Of God. Every entry's
 * cost goes through HandOfOrder.costFor, every cooldown through cooldownFor. The looks are HandOfOrderFx's; every
 * number is right below.
 */
public final class HandOfOrderInterrogation {

    private HandOfOrderInterrogation() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int INTERROGATION_SEQUENCE = 7;                 // the key
    static final float QUICK_CAST_COST = HandOfOrder.QUICK_CAST_COST;            // a sneak recast: no cooldown, this many times the cost

    // Psychic Lashing
    static final int LASHING_SEQUENCE = 7;
    static final int LASHING_COST = 1250;
    static final int LASHING_COOLDOWN = 400;
    static final int LASHING_TICKS = 400;               // 20 seconds...
    static final int LASHING_HITS = 12;                 // ...or this many hits, whichever ends first
    static final float LASHING_DAMAGE = 6f;             // added to each hit, through armor
    static final int LASHING_STAGGER = 10;

    // Psychic Piercing
    static final int PIERCING_SEQUENCE = 7;
    static final int PIERCING_COST = 1250;
    static final int PIERCING_COOLDOWN = 200;
    static final double PIERCING_RANGE = 12.0;
    static final int PIERCING_MAX = 3;                  // enemies on the line it strikes
    static final float PIERCING_DAMAGE = 30f;           // through armor...
    static final float PIERCING_SPIRIT = 2f;            // ...and this many times that on spirits and marionettes
    static final double PIERCING_WIDTH = 0.3;

    // Whip of Pain
    static final int WHIP_SEQUENCE = 7;
    static final int WHIP_COST = 1500;
    static final int WHIP_COOLDOWN = 500;
    static final double WHIP_RANGE = 16.0;
    static final float WHIP_DAMAGE = 20f;               // through armor
    static final int WHIP_TICKS = 80;                   // it trembles this long: Slowness III, Weakness...
    static final int WHIP_SHAKE_TICKS = 60;             // ...and a player's screen shakes (nausea) this long
    static final int WHIP_EXPOSED = 40;                 // and it cannot hide again for this long

    // =========================
    // Names and what is kept
    // =========================
    static final String LASHING = "hand_of_order_lashing";                 // status/WeaponCoat
    static final String LASHING_SHOWN = "hand_of_order_lashing_shown";     // on the player: the iron is shown

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("hand_of_order_interrogation", "Interrogation")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .cooldownBy(HandOfOrder::cooldownFor)
            .entry("psychic_lashing", HandOfOrderFx.WHEEL_PSYCHIC_LASHING, "Psychic Lashing", LASHING_SEQUENCE,
                    LASHING_COST, LASHING_COOLDOWN, HandOfOrderInterrogation::psychicLashing)
            .costBy(HandOfOrder::costFor)
            .entry("psychic_piercing", HandOfOrderFx.WHEEL_PSYCHIC_PIERCING, "Psychic Piercing", PIERCING_SEQUENCE,
                    PIERCING_COST, PIERCING_COOLDOWN, HandOfOrderInterrogation::psychicPiercing)
            .costBy(HandOfOrder::costFor)
            .entry("whip_of_pain", HandOfOrderFx.WHEEL_WHIP_OF_PAIN, "Whip of Pain", WHIP_SEQUENCE,
                    WHIP_COST, WHIP_COOLDOWN, HandOfOrderInterrogation::whipOfPain)
            .costBy(HandOfOrder::costFor);

    // =========================
    // Ability 3 - Interrogation
    // Normal: the Interrogation wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void handOfOrderInterrogation(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!HandOfOrder.ready(player, bypassClassCheck, INTERROGATION_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Psychic Lashing: for LASHING_TICKS or LASHING_HITS hits, every real swing adds LASHING_DAMAGE that no armor
    // stops (a tick later, Spells.strikeTrue) and staggers for LASHING_STAGGER. (Spirits that weapons pass through do
    // not exist in the mod yet; when they do, the coat's hit is what reaches them.)
    // =========================
    private static AbilityWheel.Result psychicLashing(ServerPlayer player, ServerLevel sl) {
        WeaponCoat.arm(player, LASHING, LASHING_HITS, LASHING_TICKS, (attacker, target, amount, level) -> {
            HandOfOrderFx.lashHit(level, attacker, target);
            Later.run(level, 1, () -> {
                if (!target.isAlive() || target.level() != level) return;
                Spells.strikeTrue(attacker, target, LASHING_DAMAGE);
                Root.apply(target, LASHING_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
            });
            return amount;
        });
        String tag = lashingTag(player);
        HandOfOrderFx.lashingEnd(sl, player, tag);                                   // cast again: one iron, not two
        HandOfOrderFx.psychicLashing(sl, player, LASHING_TICKS, tag);
        player.getPersistentData().putBoolean(LASHING_SHOWN, true);
        return AbilityWheel.Result.DONE;
    }

    private static String lashingTag(Player player) {
        return "ho_lashing_" + player.getId();
    }

    /** Every tick for a Hand Of Order: sparks while the iron is on the hand, and its look taken off when it is spent. */
    static void tick(Player player, ServerLevel sl) {
        boolean armed = WeaponCoat.isArmed(player, LASHING);
        if (armed) {
            if (player.tickCount % 4 == 0) HandOfOrderFx.lashingTick(sl, player);
            return;
        }
        if (player.getPersistentData().getBooleanOr(LASHING_SHOWN, false)) {
            player.getPersistentData().remove(LASHING_SHOWN);
            HandOfOrderFx.lashingEnd(sl, player, lashingTag(player));
        }
    }

    // =========================
    // Psychic Piercing: a beam from the eyes, PIERCING_RANGE long (or to the first block). The first PIERCING_MAX
    // enemies on it take PIERCING_DAMAGE through armor; marionettes and summons PIERCING_SPIRIT times that. It fires
    // even at nothing.
    // =========================
    private static AbilityWheel.Result psychicPiercing(ServerPlayer player, ServerLevel sl) {
        Vec3 eyes = player.getEyePosition();
        Vec3 end = Spells.aimPoint(player, sl, PIERCING_RANGE);
        List<LivingEntity> onLine = Spells.enemiesOnLine(player, sl, eyes, end, PIERCING_WIDTH);
        List<LivingEntity> hits = new ArrayList<>(onLine.subList(0, Math.min(PIERCING_MAX, onLine.size())));
        HandOfOrderFx.psychicPiercing(sl, player, end, hits);
        for (LivingEntity one : hits) {
            boolean spirit = Marionette.isMarionette(one) || Marionette.isSummoned(one);
            Spells.strikeTrue(player, one, spirit ? PIERCING_DAMAGE * PIERCING_SPIRIT : PIERCING_DAMAGE);
            HandOfOrderFx.piercingHit(sl, one, spirit);
        }
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Whip of Pain: the enemy you look at (WHIP_RANGE), at HandOfOrderFx.WHIP_LANDS: WHIP_DAMAGE through armor, then for
    // WHIP_TICKS it trembles (Slowness III, Weakness, a player cannot sprint and their screen shakes). It cannot lie:
    // a Concealment on it ends and it cannot hide for WHIP_EXPOSED (status/Exposed), and every effect on it is told
    // to you.
    // =========================
    private static AbilityWheel.Result whipOfPain(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, WHIP_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to lash."));
            return AbilityWheel.Result.REFUSED;
        }
        HandOfOrderFx.whipOfPain(sl, player, target);
        Later.run(sl, HandOfOrderFx.WHIP_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            Spells.strikeTrue(player, target, WHIP_DAMAGE);
            if (!target.isAlive()) return;
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, WHIP_TICKS, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WHIP_TICKS, 0));
            if (target instanceof Player victim) {
                victim.setSprinting(false);
                victim.addEffect(new MobEffectInstance(MobEffects.NAUSEA, WHIP_SHAKE_TICKS, 0));
            }
            boolean wasHidden = Concealment.isHidden(target);
            if (wasHidden) Concealment.reveal(target);
            Exposed.apply(target, WHIP_EXPOSED);
            if (wasHidden) HandOfOrderFx.truthOut(sl, target);
            HandOfOrderFx.trembling(sl, target, WHIP_TICKS, "ho_whip_" + target.getId());
            player.sendSystemMessage(confession(target));
        });
        return AbilityWheel.Result.DONE;
    }

    /** It cannot lie: what is on it, said out. */
    private static Component confession(LivingEntity target) {
        List<String> effects = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            effects.add(effect.getEffect().value().getDisplayName().getString() + " " + (effect.getAmplifier() + 1)
                    + " (" + Math.max(1, effect.getDuration() / 20) + "s)");
        }
        String name = target.getDisplayName().getString();
        return Component.literal(effects.isEmpty()
                ? name + " confesses: nothing is on it."
                : name + " confesses: " + String.join(", ", effects) + ".");
    }

    /** After a relog or a death: the iron is gone. */
    static void clearState(Player player) {
        WeaponCoat.disarm(player, LASHING);
        player.getPersistentData().remove(LASHING_SHOWN);
        HandOfOrderFx.statusEnd(player, HandOfOrderFx.Status.LASHING);
    }
}
