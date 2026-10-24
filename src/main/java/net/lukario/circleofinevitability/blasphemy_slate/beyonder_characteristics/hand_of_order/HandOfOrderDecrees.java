package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Chosen;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hand Of Order, key 5: the Decree wheel (Judge, Sequence 6). docs/HAND_OF_ORDER.md, "Key 5 wheel". A Decree is a
 * status/Decree written where you stand (from Hand of Order (1): anywhere in view up to 64 blocks); allies are never
 * bound; every Rule broken inside is a Violation and each is punished harder than the last (the ladder below).
 *
 *   Prohibition: Passage       no teleporting, flying, gliding or levitating inside
 *   Prohibition: Concealment   no hiding inside; the hidden are pulled out. From Imperative Mage (4): no stand-ins either
 *   Imperative: Reality (4)    "Weaken Mysticism, enhance Reality": ability damage halved and abilities dearer for
 *                              everyone inside, plain blows harder; your own Order is spared
 *   Contract (4)               the enemy you look at signs: it cannot harm you, each try is a Violation of the Contract
 *
 * Grows: casting the same Prohibition again within LAYER_WINDOW adds a layer (Disciplinary Paladin, up to LAYERS_MAX):
 * longer, and stronger enemies, who shrug off the first layer, are bound by the second (and punished at
 * STRONG_OBEY). Chaos Hunter (3): both Prohibitions also freeze marionettes. Balancer (2): two Decrees at once ("Laws");
 * Hand of Order (1): three, twice as long, placed anywhere in view. Justiciar (0), Law of Order: 25% cheaper and 25%
 * longer (HandOfOrder.decreeCostFor and decreeTicksFor). Law: No Return (key 7) is a Decree too and takes a place here.
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost). The looks are HandOfOrderFx's;
 * every number is right below.
 */
public final class HandOfOrderDecrees {

    private HandOfOrderDecrees() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int DECREE_SEQUENCE = 6;                        // the key
    private static final float QUICK_CAST_COST = 2f;

    // Prohibition: Passage / Concealment
    private static final int PROHIBITION_SEQUENCE = 6;
    private static final int PROHIBITION_COST = 2450;
    private static final int PROHIBITION_COOLDOWN = 500;
    private static final double PROHIBITION_RADIUS = 14.0;
    private static final int PROHIBITION_TICKS = 240;
    private static final double DECREE_HEIGHT = 8.0;            // how far up a Decree still holds
    private static final int LAYER_SEQUENCE = 5;                // Disciplinary Paladin: the same one again...
    private static final int LAYER_WINDOW = 200;                // ...within this...
    private static final int LAYERS_MAX = 3;                    // ...adds a layer, up to this many...
    private static final int LAYER_TICKS = 120;                 // ...each this much longer
    private static final float STRONG_OBEY = 0.67f;             // stronger enemies bound by a 2nd layer are punished this hard
    private static final int STAND_INS_SEQUENCE = 4;            // Imperative Mage: Concealment forbids stand-ins too
    private static final int MARIONETTES_SEQUENCE = 3;          // Chaos Hunter: both freeze marionettes too

    // Imperative: Reality
    private static final int REALITY_SEQUENCE = 4;
    private static final int REALITY_COST = 6000;
    private static final int REALITY_COOLDOWN = 900;
    private static final double REALITY_RADIUS = 18.0;
    private static final int REALITY_TICKS = 600;

    // Contract
    private static final int CONTRACT_SEQUENCE = 4;
    private static final int CONTRACT_COST = 6000;
    private static final int CONTRACT_COOLDOWN = 800;
    private static final double CONTRACT_RANGE = 16.0;
    private static final int CONTRACT_TICKS = 600;
    private static final int CONTRACT_STAGGER = 20;             // its 1st Violation: staggered this long
    private static final float CONTRACT_DAMAGE = 40f;           // its 2nd: this much, through everything
    private static final int CONTRACT_HOLD = 60;                // its 3rd and on: held this long
    private static final int CONTRACT_GAP = 10;                 // tries closer together than this are one

    // How many at once, and where
    private static final int LAWS_SEQUENCE = 2;                 // Balancer: two Decrees at once
    private static final int MANY_SEQUENCE = 1;                 // Hand of Order: three, anywhere in view...
    private static final double MANY_RANGE = 64.0;              // ...this far

    // The Violation ladder of a Decree (by count: 1st, 2nd, 3rd, 4th and on)
    private static final int LADDER_STAGGER = 20;               // 1st: staggered (cannot move or attack) this long
    private static final float LADDER_DAMAGE_2 = 30f;           // 2nd: this much, through everything
    private static final float LADDER_DAMAGE_3 = 60f;           // 3rd: this much...
    private static final int LADDER_HOLD_3 = 40;                // ...and held this long
    private static final float LADDER_DAMAGE_4 = 100f;          // 4th and on: this much...
    private static final int LADDER_HOLD_4 = 60;                // ...and held this long
    private static final float RECOIL = 0.2f;                   // punishing a stronger enemy recoils on you: this part
    private static final int RECOIL_HALVED_SEQUENCE = 4;        // Beyonder Sense (4): half the recoil

    // =========================
    // Names and what is kept (not saved: a restart lifts every Decree)
    // =========================
    static final String CONTRACT = "hand_of_order_contract";                 // status/Chosen
    private static final String[] SLOTS = {"hand_of_order_decree_1", "hand_of_order_decree_2", "hand_of_order_decree_3"};

    /** What each written Decree of a player is: the wheel entry it came from, when it was (re)cast, its layers. */
    private static final class Written {
        final String entry;
        long at;
        int layers = 1;

        Written(String entry, long at) {
            this.entry = entry;
            this.at = at;
        }
    }

    private static final Map<UUID, Map<String, Written>> WRITTEN = new HashMap<>();

    /** A signer's Violations of the Contract, and when the last was. */
    private static final Map<UUID, int[]> CONTRACT_COUNT = new HashMap<>();
    private static final Map<UUID, Long> CONTRACT_LAST = new HashMap<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("hand_of_order_decree", "Decree")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .cooldownBy(HandOfOrder::cooldownFor)
            .entry("prohibition_passage", HandOfOrderFx.WHEEL_PROHIBITION_PASSAGE, "Prohibition: Passage", PROHIBITION_SEQUENCE,
                    PROHIBITION_COST, PROHIBITION_COOLDOWN, HandOfOrderDecrees::prohibitPassage)
            .costBy(HandOfOrder::decreeCostFor)
            .entry("prohibition_concealment", HandOfOrderFx.WHEEL_PROHIBITION_CONCEALMENT, "Prohibition: Concealment", PROHIBITION_SEQUENCE,
                    PROHIBITION_COST, PROHIBITION_COOLDOWN, HandOfOrderDecrees::prohibitConcealment)
            .costBy(HandOfOrder::decreeCostFor)
            .entry("imperative_reality", HandOfOrderFx.WHEEL_IMPERATIVE_REALITY, "Imperative: Reality", REALITY_SEQUENCE,
                    REALITY_COST, REALITY_COOLDOWN, HandOfOrderDecrees::imperativeReality)
            .costBy(HandOfOrder::decreeCostFor)
            .entry("contract", HandOfOrderFx.WHEEL_CONTRACT, "Contract", CONTRACT_SEQUENCE,
                    CONTRACT_COST, CONTRACT_COOLDOWN, HandOfOrderDecrees::contract)
            .costBy(HandOfOrder::costFor);

    // =========================
    // Ability 5 - Decree
    // Normal: the Decree wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void handOfOrderDecree(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!HandOfOrder.ready(player, bypassClassCheck, DECREE_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Prohibition: Passage / Concealment. PROHIBITION_RADIUS round where you stand, PROHIBITION_TICKS (through
    // HandOfOrder.decreeTicksFor). Again within LAYER_WINDOW (from 5): a layer. Concealment forbids STAND_INS from 4;
    // both forbid MARIONETTES from 3.
    // =========================
    private static AbilityWheel.Result prohibitPassage(ServerPlayer player, ServerLevel sl) {
        return prohibit(player, sl, "prohibition_passage", HandOfOrderFx.Rule.PASSAGE, Decree.Rule.PASSAGE);
    }

    private static AbilityWheel.Result prohibitConcealment(ServerPlayer player, ServerLevel sl) {
        return prohibit(player, sl, "prohibition_concealment", HandOfOrderFx.Rule.CONCEALMENT, Decree.Rule.CONCEALMENT);
    }

    private static AbilityWheel.Result prohibit(ServerPlayer player, ServerLevel sl, String entry, HandOfOrderFx.Rule look, Decree.Rule rule) {
        if (Beyonder.hasSequence(player, LAYER_SEQUENCE) && addLayer(player, sl, entry)) return AbilityWheel.Result.DONE;
        Set<Decree.Rule> rules = EnumSet.of(rule);
        if (rule == Decree.Rule.CONCEALMENT && Beyonder.hasSequence(player, STAND_INS_SEQUENCE)) rules.add(Decree.Rule.STAND_INS);
        if (Beyonder.hasSequence(player, MARIONETTES_SEQUENCE)) rules.add(Decree.Rule.MARIONETTES);
        Vec3 center = placeFor(player, sl);
        if (center == null) {
            player.sendSystemMessage(Component.literal("There is no ground there to write on."));
            return AbilityWheel.Result.REFUSED;
        }
        writeLaw(player, sl, entry, center, PROHIBITION_RADIUS, HandOfOrder.decreeTicksFor(player, PROHIBITION_TICKS), look, rules);
        return AbilityWheel.Result.DONE;
    }

    /** The same Prohibition again within LAYER_WINDOW of the last cast: one more layer on it. False if there is none. */
    private static boolean addLayer(Player player, ServerLevel sl, String entry) {
        Map<String, Written> mine = WRITTEN.get(player.getUUID());
        if (mine == null) return false;
        long now = sl.getGameTime();
        for (Map.Entry<String, Written> one : mine.entrySet()) {
            Written written = one.getValue();
            String slot = one.getKey();
            if (!written.entry.equals(entry) || now - written.at > LAYER_WINDOW || written.layers >= LAYERS_MAX) continue;
            if (!Decree.isWritten(player, slot)) continue;
            written.layers++;
            written.at = now;
            Decree.extend(player, slot, HandOfOrder.decreeTicksFor(player, LAYER_TICKS));
            HandOfOrderFx.decreeLayer(sl, Decree.centerOf(player, slot), Decree.radiusOf(player, slot), written.layers, tagOf(player, slot));
            HandOfOrderFx.status(player, Status.DECREE, Decree.ticksLeft(player, slot));
            player.sendSystemMessage(Component.literal("The Prohibition is bound in " + written.layers + " layers."));
            return true;
        }
        return false;
    }

    // =========================
    // Imperative: Reality. REALITY_RADIUS, REALITY_TICKS (through decreeTicksFor): status/Decree's REALITY weighs on
    // everyone inside, you excepted.
    // =========================
    private static AbilityWheel.Result imperativeReality(ServerPlayer player, ServerLevel sl) {
        Vec3 center = placeFor(player, sl);
        if (center == null) {
            player.sendSystemMessage(Component.literal("There is no ground there to write on."));
            return AbilityWheel.Result.REFUSED;
        }
        writeLaw(player, sl, "imperative_reality", center, REALITY_RADIUS, HandOfOrder.decreeTicksFor(player, REALITY_TICKS),
                HandOfOrderFx.Rule.REALITY, EnumSet.of(Decree.Rule.REALITY));
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Contract. The enemy you look at (CONTRACT_RANGE) signs for CONTRACT_TICKS (status/Chosen; one at a time): every
    // attack it makes on you is cancelled and is a Violation of the Contract (1st staggered, 2nd CONTRACT_DAMAGE, 3rd
    // and on held). If you strike it, the paper tears and the Contract ends.
    // =========================
    private static AbilityWheel.Result contract(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, CONTRACT_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to sign the Contract."));
            return AbilityWheel.Result.REFUSED;
        }
        Chosen.release(player, CONTRACT);                    // one Contract at a time: the old paper tears
        CONTRACT_COUNT.remove(target.getUUID());
        CONTRACT_LAST.remove(target.getUUID());
        Chosen.choose(player, CONTRACT, target, CONTRACT_TICKS);
        HandOfOrderFx.contract(sl, player, target, CONTRACT_TICKS, contractTag(player));
        return AbilityWheel.Result.DONE;
    }

    static String contractTag(Player player) {
        return "ho_contract_" + player.getId();
    }

    private static void contractBroken(Player owner, LivingEntity signer, ServerLevel sl) {
        long now = sl.getGameTime();
        Long last = CONTRACT_LAST.get(signer.getUUID());
        if (last != null && now - last < CONTRACT_GAP) return;
        CONTRACT_LAST.put(signer.getUUID(), now);
        int step = ++CONTRACT_COUNT.computeIfAbsent(signer.getUUID(), id -> new int[1])[0];
        HandOfOrderFx.contractViolation(sl, signer, Math.min(3, step));
        Later.run(sl, 1, () -> {                              // not inside the cancelled attack
            if (!signer.isAlive() || !Spells.casterStillHere(owner, sl)) return;
            if (step == 1) {
                Root.apply(signer, CONTRACT_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
            } else if (step == 2) {
                Spells.strikeTrue(owner, signer, CONTRACT_DAMAGE);
            } else {
                Root.apply(signer, CONTRACT_HOLD, RootRestriction.EVERYTHING);
            }
        });
    }

    // =========================
    // Writing a Decree into one of the player's places (also Law: No Return, key 7)
    // =========================

    /**
     * Writes a Decree of 'rules' round 'center' for 'ticks' into a free place of this player (or over the one ending
     * soonest), with its look ('look' picks the color and the word), the Violation ladder, the layers and its end.
     * 'entry' is the wheel entry it came from (a recast of the same one within LAYER_WINDOW layers it).
     */
    static void writeLaw(Player player, ServerLevel sl, String entry, Vec3 center, double radius, int ticks,
                         HandOfOrderFx.Rule look, Set<Decree.Rule> rules) {
        String slot = slotFor(player);
        String tag = tagOf(player, slot);
        Decree.close(player, slot);                              // the one written there before ends first (its look too)
        UUID id = player.getUUID();
        WRITTEN.computeIfAbsent(id, key -> new HashMap<>()).put(slot, new Written(entry, sl.getGameTime()));
        Decree.at(center, radius)
                .forbid(rules.toArray(new Decree.Rule[0]))
                .height(DECREE_HEIGHT)
                .lasts(ticks)
                .binds(enemy -> layersOf(id, slot) >= 2 || !strongerThan(player, enemy))
                .onViolation((who, rule, count, level) -> punish(player, who, rule, count, level, layersOf(id, slot)))
                .onEnter(who -> {
                    if (rules.contains(Decree.Rule.MARIONETTES) && Marionette.isMarionette(who)) HandOfOrderFx.puppetFrozen(sl, who);
                })
                .onPulse(40, left -> HandOfOrderFx.decreeTick(sl, center, radius, look))
                .onEnd(() -> ended(player, sl, slot, center, radius, tag, look))
                .write(player, sl, slot);
        if (look == HandOfOrderFx.Rule.REALITY) HandOfOrderFx.reality(sl, player, center, radius, ticks, tag);
        else if (look == HandOfOrderFx.Rule.RESURRECTION) HandOfOrderFx.lawNoReturn(sl, player, center, radius, ticks, tag);
        else HandOfOrderFx.decree(sl, player, center, radius, ticks, look, tag);
    }

    private static void ended(Player player, ServerLevel sl, String slot, Vec3 center, double radius, String tag, HandOfOrderFx.Rule look) {
        Map<String, Written> mine = WRITTEN.get(player.getUUID());
        if (mine != null) {
            mine.remove(slot);
            if (mine.isEmpty()) WRITTEN.remove(player.getUUID());
        }
        if (look == HandOfOrderFx.Rule.REALITY) {
            HandOfOrderFx.realityEnd(sl, center, radius, tag);
            HandOfOrderFx.statusEnd(player, Status.REALITY);
        } else {
            HandOfOrderFx.decreeEnd(sl, center, radius, tag);
        }
        // the clock by the hotbar shows the Decree that is left longest, or goes
        int longest = 0;
        for (String other : SLOTS) longest = Math.max(longest, Decree.ticksLeft(player, other));
        if (longest > 0) HandOfOrderFx.status(player, Status.DECREE, longest);
        else HandOfOrderFx.statusEnd(player, Status.DECREE);
    }

    /** A free place for one more Decree, or (all taken) the one ending soonest. */
    private static String slotFor(Player player) {
        int places = Beyonder.hasSequence(player, MANY_SEQUENCE) ? 3 : Beyonder.hasSequence(player, LAWS_SEQUENCE) ? 2 : 1;
        String soonest = SLOTS[0];
        int least = Integer.MAX_VALUE;
        for (int i = 0; i < places; i++) {
            int left = Decree.ticksLeft(player, SLOTS[i]);
            if (left <= 0) return SLOTS[i];
            if (left < least) {
                least = left;
                soonest = SLOTS[i];
            }
        }
        return soonest;
    }

    /** Where a Decree goes: under you, or from Hand of Order (1) the ground you look at up to MANY_RANGE. */
    static Vec3 placeFor(Player player, ServerLevel sl) {
        if (Beyonder.hasSequence(player, MANY_SEQUENCE)) return Spells.aimGround(player, sl, MANY_RANGE);
        return player.onGround() ? player.position() : Spells.groundAt(player, sl, player.position());
    }

    private static int layersOf(UUID player, String slot) {
        Map<String, Written> mine = WRITTEN.get(player);
        Written written = mine == null ? null : mine.get(slot);
        return written == null ? 1 : written.layers;
    }

    static String tagOf(Player player, String slot) {
        return "ho_" + slot + "_" + player.getId();
    }

    /** All of this player's Decrees end (Quell Disorder of an enemy, a relog). */
    static void closeAll(Player player) {
        for (String slot : SLOTS) Decree.close(player, slot);
        WRITTEN.remove(player.getUUID());
    }

    // =========================
    // The Violation ladder
    // =========================

    /**
     * 'who' broke 'rule' in 'owner''s Decree, its 'count'th Violation there: the look at once, then (a tick later, out of
     * whatever event it was caught in) 1st staggered, 2nd LADDER_DAMAGE_2, 3rd LADDER_DAMAGE_3 and held, 4th and on
     * LADDER_DAMAGE_4 and held. A stronger enemy bound only by layers is punished at STRONG_OBEY, and punishing one
     * recoils on the writer (RECOIL; halved from Beyonder Sense, 4). The Convict breaking it arms Chaos Hunter's sure hit.
     */
    private static void punish(Player owner, LivingEntity who, Decree.Rule rule, int count, ServerLevel sl, int layers) {
        HandOfOrderFx.violation(sl, who, count);
        switch (rule) {
            case CONCEALMENT -> HandOfOrderFx.revealed(sl, who);
            case RESURRECTION -> HandOfOrderFx.revivalVoided(sl, who);
            case MARIONETTES -> HandOfOrderFx.puppetFrozen(sl, who);
            default -> HandOfOrderFx.refused(sl, who);
        }
        boolean strong = strongerThan(owner, who);
        float power = strong ? STRONG_OBEY : 1f;
        HandOfOrder.violatorPunished(owner, who, sl);
        Later.run(sl, 1, () -> {
            if (!who.isAlive() || !Spells.casterStillHere(owner, sl)) return;
            float damage = 0f;
            if (count <= 1) {
                Root.apply(who, LADDER_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
            } else if (count == 2) {
                damage = LADDER_DAMAGE_2 * power;
            } else if (count == 3) {
                damage = LADDER_DAMAGE_3 * power;
                Root.apply(who, Math.round(LADDER_HOLD_3 * power), RootRestriction.EVERYTHING);
            } else {
                damage = LADDER_DAMAGE_4 * power;
                Root.apply(who, Math.round(LADDER_HOLD_4 * power), RootRestriction.EVERYTHING);
            }
            if (damage > 0f) Spells.strikeTrue(owner, who, damage);
            if (strong && damage > 0f) recoil(owner, damage);
        });
    }

    /** The Rule recoils on the one who wrote it, for holding one stronger than them. */
    private static void recoil(Player owner, float damage) {
        float part = Beyonder.hasSequence(owner, RECOIL_HALVED_SEQUENCE) ? RECOIL * 0.5f : RECOIL;
        owner.hurt(owner.damageSources().magic(), damage * part);
    }

    /** Stronger than the writer: a boss, or a Beyonder of a higher Sequence (a lower number). */
    static boolean strongerThan(Player owner, LivingEntity enemy) {
        if (HandOfOrder.isBoss(enemy)) return true;
        return enemy instanceof Player other && HandOfOrder.isBeyonder(other)
                && SoulCore.getSequence(other) < SoulCore.getSequence(owner);
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class DecreeEvents {

        private DecreeEvents() {}

        static {
            // the Contract ends however it ends (time, death, torn, another signed): the paper goes
            Chosen.addListener(CONTRACT, (chooser, target) -> {
                if (target != null) {
                    CONTRACT_COUNT.remove(target.getUUID());
                    CONTRACT_LAST.remove(target.getUUID());
                }
                if (chooser != null && chooser.level() instanceof ServerLevel sl && target != null) {
                    HandOfOrderFx.contractTear(sl, target, contractTag(chooser));
                }
            });
        }

        // HIGH: before Substitute and the like, so a hit the Contract forbids does not use those up
        @SubscribeEvent(priority = Priority.HIGH)
        public static boolean onDecreeContractAttack(LivingAttackEvent event) {
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel sl)) return false;
            if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) return false;
            // the signer tries to harm the one it signed for: refused, a Violation
            if (victim instanceof Player owner && Chosen.is(attacker, owner, CONTRACT)) {
                contractBroken(owner, attacker, sl);
                return true;
            }
            // the Mage strikes the signer: the paper tears
            if (attacker instanceof Player owner && Chosen.is(victim, owner, CONTRACT)) Chosen.release(owner, CONTRACT);
            return false;
        }

        @SubscribeEvent
        public static void onDecreeLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            WRITTEN.remove(event.getEntity().getUUID());     // its Decrees end with it (combat/Zone)
        }
    }
}
