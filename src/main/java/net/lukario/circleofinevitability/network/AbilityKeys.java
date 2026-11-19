package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.StolenAbilities;
import net.lukario.circleofinevitability.combat.AbilityCharge;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.HeldActions;
import net.lukario.circleofinevitability.combat.NextCast;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.KeySeal;
import net.lukario.circleofinevitability.status.Parasite;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Routes a player's ability keys and keeps pending charges on the server. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AbilityKeys {
    @FunctionalInterface
    private interface KeyCast {
        void cast(ServerPlayer player, Level level, ServerLevel serverLevel, AbilityCharge charge);
    }

    private record Key(ServerPlayer player, int slot) {}

    private record Hold(ServerLevel level, String pathway, int sequence, boolean sneaking, long startTick, int maxTicks) {}

    private static final List<KeyCast> CASTS = List.of(
            SKeyPressAbilityOneUsed::cast, SKeyPressAbilityTwoUsed::cast, SKeyPressAbilityThreeUsed::cast,
            SKeyPressAbilityFourUsed::cast, SKeyPressAbilityFiveUsed::cast, SKeyPressAbilitySixUsed::cast,
            SKeyPressAbilitySevenUsed::cast);
    private static final Map<Key, Hold> HOLDS = new HashMap<>();

    private AbilityKeys() {}

    static void input(ServerPlayer player, int slot, SAbilityKeyPacket.Action action) {
        if (slot < 1 || slot > CASTS.size()) return;
        Key key = new Key(player, slot);
        switch (action) {
            case PRESS, REPEAT -> press(key, action == SAbilityKeyPacket.Action.REPEAT);
            case RELEASE -> release(key);
            case CANCEL -> HOLDS.remove(key);
        }
    }

    private static void press(Key key, boolean repeat) {
        ServerPlayer player = key.player();
        if (!player.isAlive() || player.hasDisconnected() || HOLDS.containsKey(key)) return;

        String pathway = SoulCore.getPathway(player);
        PathwayAbilities.Ability ability = PathwayAbilities.of(pathway, key.slot());
        boolean sneaking = player.isShiftKeyDown();
        int maxTicks = ability == null ? 0 : ability.chargeTicks(sneaking);
        if (maxTicks <= 0) {
            cast(key, AbilityCharge.NONE);
        } else if (!repeat && Beyonder.hasSequence(player, ability.sequence()) && canCast(player, key.slot())) {
            ServerLevel level = player.level();
            HOLDS.put(key, new Hold(level, pathway, SoulCore.getSequence(player), sneaking, level.getGameTime(), maxTicks));
        }
    }

    private static void release(Key key) {
        Hold hold = HOLDS.remove(key);
        ServerPlayer player = key.player();
        if (hold == null || !player.isAlive() || player.hasDisconnected() || player.level() != hold.level()
                || !hold.pathway().equals(SoulCore.getPathway(player)) || hold.sequence() != SoulCore.getSequence(player)) return;

        int ticks = (int) Math.clamp(hold.level().getGameTime() - hold.startTick(), 0L, hold.maxTicks());
        boolean sneaking = player.isShiftKeyDown();
        player.setShiftKeyDown(hold.sneaking());
        try {
            cast(key, new AbilityCharge(ticks, hold.maxTicks()));
        } finally {
            player.setShiftKeyDown(sneaking);
        }
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

    private static boolean canCast(ServerPlayer player, int slot) {
        if (Decree.blocksKey(player, slot)) return false;
        if (Silence.blocksKey(player, slot)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return false;
        }
        return !StolenAbilities.blocksKey(player, slot) && !Parasite.blocksKey(player, slot) && !KeySeal.blocksKey(player, slot);
    }

    private static void clear(Player player) {
        HOLDS.keySet().removeIf(key -> key.player() == player);
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
