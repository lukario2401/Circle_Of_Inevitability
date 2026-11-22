package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.StolenAbilities;
import net.lukario.circleofinevitability.camera.DetachedCamera;
import net.lukario.circleofinevitability.combat.AbilityCharge;
import net.lukario.circleofinevitability.combat.AbilityCombos;
import net.lukario.circleofinevitability.combat.AbilityCombos.Combo;
import net.lukario.circleofinevitability.combat.CastContext;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.HeldActions;
import net.lukario.circleofinevitability.combat.NextCast;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.KeySeal;
import net.lukario.circleofinevitability.status.Parasite;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Routes a player's ability keys and keeps pending charges on the server. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AbilityKeys {
    @FunctionalInterface
    private interface KeyCast {
        void cast(ServerPlayer player, Level level, ServerLevel serverLevel, AbilityCharge charge);
    }

    private record Key(ServerPlayer player, int slot) {}

    private record Hold(ServerLevel level, String pathway, int sequence, boolean sneaking, long startTick,
                        AbilityCharge.Rule rule, int paidIntervals, Combo combo) {
        Hold paidInterval() {
            return new Hold(level, pathway, sequence, sneaking, startTick, rule, paidIntervals + 1, combo);
        }
    }

    private record Pending(Key key, ServerLevel level, String pathway, int sequence, long pressedAt, long releasedAt,
                           List<Combo> options, long expiresAt) {
        boolean released() {
            return releasedAt >= 0;
        }

        Pending release(long tick) {
            return new Pending(key, level, pathway, sequence, pressedAt, tick, options, expiresAt);
        }
    }

    private static final List<KeyCast> CASTS = List.of(
            SKeyPressAbilityOneUsed::cast, SKeyPressAbilityTwoUsed::cast, SKeyPressAbilityThreeUsed::cast,
            SKeyPressAbilityFourUsed::cast, SKeyPressAbilityFiveUsed::cast, SKeyPressAbilitySixUsed::cast,
            SKeyPressAbilitySevenUsed::cast);
    private static final Map<Key, Hold> HOLDS = new HashMap<>();
    private static final Map<ServerPlayer, Pending> PENDING_COMBOS = new HashMap<>();
    private static final Set<Key> CONSUMED_KEYS = new HashSet<>();

    private AbilityKeys() {}

    static void input(ServerPlayer player, int slot, SAbilityKeyPacket.Action action) {
        if (DetachedCamera.active(player) || (slot == 0 && action == SAbilityKeyPacket.Action.CANCEL)) {
            clear(player);
            return;
        }
        if (slot < 1 || slot > CASTS.size()) return;
        Key key = new Key(player, slot);
        if (CONSUMED_KEYS.contains(key)) {
            if (action == SAbilityKeyPacket.Action.RELEASE || action == SAbilityKeyPacket.Action.CANCEL) {
                if (action == SAbilityKeyPacket.Action.RELEASE) release(key, player.level().getGameTime());
                else removeHold(key);
                CONSUMED_KEYS.remove(key);
            }
            return;
        }
        long now = player.level().getGameTime();
        Pending pending = PENDING_COMBOS.get(player);
        if (action == SAbilityKeyPacket.Action.CANCEL) {
            HOLDS.remove(key);
            if (pending != null && pending.key().equals(key)) PENDING_COMBOS.remove(player);
            return;
        }
        if (pending != null && expirePending(player, pending, now)) pending = null;

        switch (action) {
            case PRESS -> beginPress(key, pending, now);
            case REPEAT -> {
                if (pending == null || !pending.key().equals(key)) press(key, true, now);
            }
            case RELEASE -> {
                if (pending != null && pending.key().equals(key) && !pending.released()) {
                    Pending released = pending.release(now);
                    if (pending.options().stream().anyMatch(combo -> combo.input() == AbilityCombos.Input.SEQUENCE)) {
                        PENDING_COMBOS.put(player, released);
                    } else {
                        playSolo(released);
                    }
                } else {
                    release(key, now);
                }
            }
            case CANCEL -> throw new IllegalStateException("Cancel was already handled");
        }
    }

    private static void beginPress(Key key, Pending pending, long now) {
        ServerPlayer player = key.player();
        if (!player.isAlive() || player.hasDisconnected() || HOLDS.containsKey(key)) return;
        boolean sneaking = player.isShiftKeyDown();
        if (pending != null) {
            if (pending.key().equals(key) && !pending.released()) return;
            if (!sneaking) {
                for (Combo combo : pending.options()) {
                    if (combo.matches(pending.key().slot(), key.slot(), pending.released(), now - pending.pressedAt())) {
                        PENDING_COMBOS.remove(player);
                        if (!pending.released()) CONSUMED_KEYS.add(pending.key());
                        CONSUMED_KEYS.add(key);
                        if (combo.rule().metered()) beginChord(player, combo, now);
                        else castCombo(player, combo, AbilityCharge.NONE);
                        return;
                    }
                }
            }
            playSolo(pending);
        }

        String pathway = SoulCore.getPathway(player);
        int sequence = SoulCore.getSequence(player);
        List<Combo> options = sneaking ? List.of() : AbilityCombos.startingWith(pathway, sequence, key.slot());
        if (options.isEmpty()) {
            press(key, false, now);
            return;
        }

        int window = options.stream().mapToInt(combo -> combo.input().window()).max().orElseThrow();
        PENDING_COMBOS.put(player, new Pending(key, player.level(), pathway, sequence, now, -1, options, now + window));
    }

    private static boolean expirePending(ServerPlayer player, Pending pending, long now) {
        if (!player.isAlive() || player.hasDisconnected() || player.level() != pending.level()
                || !pending.pathway().equals(SoulCore.getPathway(player)) || pending.sequence() != SoulCore.getSequence(player)) {
            PENDING_COMBOS.remove(player);
            return true;
        }
        if (now < pending.expiresAt()) return false;
        playSolo(pending);
        return true;
    }

    private static void playSolo(Pending pending) {
        ServerPlayer player = pending.key().player();
        PENDING_COMBOS.remove(player);
        inForm(player, false, () -> press(pending.key(), false, pending.pressedAt()));
        if (pending.released()) release(pending.key(), pending.releasedAt());
    }

    private static void press(Key key, boolean repeat, long pressedAt) {
        ServerPlayer player = key.player();
        if (!player.isAlive() || player.hasDisconnected() || HOLDS.containsKey(key)) return;

        String pathway = SoulCore.getPathway(player);
        PathwayAbilities.Ability ability = PathwayAbilities.of(pathway, key.slot());
        boolean sneaking = player.isShiftKeyDown();
        AbilityCharge.Rule rule = ability == null ? AbilityCharge.Rule.INSTANT : ability.chargeRule(sneaking);
        if (rule.maxTicks() == 0) {
            cast(key, AbilityCharge.NONE);
        } else if (!repeat && Beyonder.hasSequence(player, ability.sequence()) && canCast(player, key.slot())) {
            if (rule.metered() && !Spells.paySpirituality(player, rule.installmentCost())) return;
            ServerLevel level = player.level();
            HOLDS.put(key, new Hold(level, pathway, SoulCore.getSequence(player), sneaking, pressedAt, rule, 0, null));
        }
    }

    private static void beginChord(ServerPlayer player, Combo combo, long now) {
        if (!canCast(player, combo) || !Cooldowns.ready(player, combo.key(), combo.name())
                || !Spells.paySpirituality(player, combo.cost())) return;
        Hold hold = new Hold(player.level(), combo.pathway(), SoulCore.getSequence(player), false, now, combo.rule(), 0, combo);
        storeHold(new Key(player, combo.first()), hold);
        combo.chargePulse().accept(player);
    }

    private static void storeHold(Key key, Hold hold) {
        if (hold.combo() == null) {
            HOLDS.put(key, hold);
        } else {
            HOLDS.put(new Key(key.player(), hold.combo().first()), hold);
            HOLDS.put(new Key(key.player(), hold.combo().second()), hold);
        }
    }

    private static void removeHold(Key key) {
        Hold hold = HOLDS.remove(key);
        if (hold != null && hold.combo() != null) {
            HOLDS.remove(new Key(key.player(), hold.combo().first()));
            HOLDS.remove(new Key(key.player(), hold.combo().second()));
        }
    }

    private static void release(Key key, long releasedAt) {
        advance(key, releasedAt, true);
    }

    /** Both a release packet and a server tick can land on the payment boundary. Settle it exactly once. */
    private static void advance(Key key, long now, boolean released) {
        Hold hold = HOLDS.get(key);
        if (hold == null || (!released && hold.combo() != null && key.slot() != hold.combo().first())) return;
        ServerPlayer player = key.player();
        if (!player.isAlive() || player.hasDisconnected() || player.level() != hold.level()
                || !hold.pathway().equals(SoulCore.getPathway(player)) || hold.sequence() != SoulCore.getSequence(player)) {
            removeHold(key);
            return;
        }

        AbilityCharge.Rule rule = hold.rule();
        int ticks = (int) Math.clamp(now - hold.startTick(), 0L, rule.maxTicks());
        if (rule.metered()) {
            while (hold.paidIntervals() < ticks / rule.intervalTicks()) {
                boolean allowed = hold.combo() == null ? canCast(player, key.slot()) : canCast(player, hold.combo());
                if (!allowed) {
                    removeHold(key);
                    return;
                }
                if (!Spells.paySpirituality(player, rule.installmentCost())) {
                    finish(key, hold, hold.paidIntervals() * rule.intervalTicks(), released);
                    return;
                }
                hold = hold.paidInterval();
                storeHold(key, hold);
                if (hold.combo() != null) hold.combo().chargePulse().accept(player);
            }
        }
        if (released || (rule.metered() && ticks == rule.maxTicks())) finish(key, hold, ticks, released);
    }

    private static void finish(Key key, Hold hold, int ticks, boolean released) {
        removeHold(key);
        if (!released) CONSUMED_KEYS.add(key);
        AbilityCharge charge = new AbilityCharge(ticks, hold.rule().maxTicks(), hold.rule().metered());
        if (hold.combo() == null) inForm(key.player(), hold.sneaking(), () -> cast(key, charge));
        else inForm(key.player(), false, () -> castCombo(key.player(), hold.combo(), charge));
    }

    private static void inForm(ServerPlayer player, boolean wasSneaking, Runnable action) {
        boolean sneaking = player.isShiftKeyDown();
        player.setShiftKeyDown(wasSneaking);
        try {
            action.run();
        } finally {
            player.setShiftKeyDown(sneaking);
        }
    }

    private static void castCombo(ServerPlayer player, Combo combo, AbilityCharge charge) {
        if (!player.isAlive() || player.hasDisconnected() || !combo.pathway().equals(SoulCore.getPathway(player))
                || !Beyonder.hasSequence(player, combo.sequence())) return;
        if (!canCast(player, combo) || !Cooldowns.ready(player, combo.key(), combo.name())
                || HeldActions.defer(player, () -> castCombo(player, combo, charge))) return;

        KeySeal.pressed(player, combo.first());
        KeySeal.pressed(player, combo.second());
        boolean cast = NextCast.run(player, () -> CastContext.run(player, combo.id(), () -> {
            Runnable effect = combo.prepare().prepare(player, player.level(), charge);
            if (effect == null || !charge.payBase(player, combo.cost())) return false;
            effect.run();
            return true;
        }));
        if (!cast) return;
        Cooldowns.start(player, combo.key(), combo.cooldown());
        Casts.comboCast(player, combo);
        player.sendOverlayMessage(Component.literal(combo.name()));
    }

    private static void cast(Key key, AbilityCharge charge) {
        ServerPlayer player = key.player();
        int slot = key.slot();
        if (!canCast(player, slot) || HeldActions.defer(player, () -> cast(key, charge))) return;

        ServerLevel level = player.level();
        KeySeal.pressed(player, slot);
        Casts.keyPressed(player, slot);
        try {
            NextCast.run(player, () -> {
                CASTS.get(slot - 1).cast(player, level, level, charge);
                return null;
            });
        } finally {
            Casts.keyDone(player);
        }
    }

    private static boolean canCast(ServerPlayer player, Combo combo) {
        return canCast(player, combo.first()) && canCast(player, combo.second());
    }

    private static boolean canCast(ServerPlayer player, int slot) {
        if (DetachedCamera.active(player) || Decree.blocksKey(player, slot)) return false;
        if (Silence.blocksKey(player, slot)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return false;
        }
        return !StolenAbilities.blocksKey(player, slot) && !Parasite.blocksKey(player, slot) && !KeySeal.blocksKey(player, slot);
    }

    private static void clear(Player player) {
        HOLDS.keySet().removeIf(key -> key.player() == player);
        PENDING_COMBOS.remove(player);
        CONSUMED_KEYS.removeIf(key -> key.player() == player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if (DetachedCamera.active(player)) {
            clear(player);
            return;
        }
        Pending pending = PENDING_COMBOS.get(player);
        long now = player.level().getGameTime();
        if (pending != null) expirePending(player, pending, now);
        if (!HOLDS.isEmpty()) {
            for (int slot = 1; slot <= CASTS.size(); slot++) advance(new Key(player, slot), now, false);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        clear(event.getOriginal());
    }
}
