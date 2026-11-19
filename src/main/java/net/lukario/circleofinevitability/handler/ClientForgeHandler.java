package net.lukario.circleofinevitability.handler;


import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.client.ClientPathwayData;
import net.lukario.circleofinevitability.client.Keybindings;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.lukario.circleofinevitability.network.SAbilityKeyPacket;
import net.lukario.circleofinevitability.network.SAbilityKeyPacket.Action;
import net.lukario.circleofinevitability.network.SKeyPressSpawnEntityPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeHandler {
    private static final Component EXAMPLE_KEY_PRESSED =
            Component.translatable("message." + CircleOfInevitability.MOD_ID + ".example_key_pressed");
    private static final List<KeyMapping> ABILITY_KEYS = Keybindings.INSTANCE.abilityKeys;
    private static final boolean[] ABILITY_KEYS_DOWN = new boolean[ABILITY_KEYS.size()];
    private static final List<SAbilityKeyPacket> PENDING_INPUT = new ArrayList<>();
    private static LocalPlayer lastPlayer;
    private static ClientLevel lastLevel;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if(Keybindings.INSTANCE.exampleKey.consumeClick() && minecraft.player != null) {
            minecraft.player.sendSystemMessage(EXAMPLE_KEY_PRESSED);
        }

        if(Keybindings.INSTANCE.examplePacketKey.consumeClick() && minecraft.player != null) {
            minecraft.player.sendOverlayMessage(EXAMPLE_KEY_PRESSED);
            PacketHandler.sendToServer(new SKeyPressSpawnEntityPacket());
        }
        pollAbilityKeys(minecraft);
    }

    private static void pollAbilityKeys(Minecraft minecraft) {
        if (minecraft.player != lastPlayer || minecraft.level != lastLevel) {
            Arrays.fill(ABILITY_KEYS_DOWN, false);
            lastPlayer = minecraft.player;
            lastLevel = minecraft.level;
        }

        boolean active = lastPlayer != null && lastPlayer.isAlive() && minecraft.isWindowActive()
                && minecraft.gui.screen() == null && minecraft.gui.overlay() == null;
        for (int index = 0; index < ABILITY_KEYS.size(); index++) {
            KeyMapping mapping = ABILITY_KEYS.get(index);
            boolean clicked = mapping.consumeClick();
            boolean wasDown = ABILITY_KEYS_DOWN[index];
            boolean down = active && mapping.isDown();
            int slot = index + 1;

            if (active && clicked) {
                preventConflictingMenus(minecraft, mapping, slot);
                if (!wasDown || down) {
                    PENDING_INPUT.add(new SAbilityKeyPacket(slot, wasDown ? Action.REPEAT : Action.PRESS));
                }
            }
            if (!down && (wasDown || active && clicked)) {
                PENDING_INPUT.add(new SAbilityKeyPacket(slot, active ? Action.RELEASE : Action.CANCEL));
            }
            if (!down) discardClicks(mapping);
            ABILITY_KEYS_DOWN[index] = down && (wasDown || clicked);
        }
    }

    private static void preventConflictingMenus(Minecraft minecraft, KeyMapping mapping, int slot) {
        KeyMapping advancements = minecraft.options.keyAdvancements;
        KeyMapping quickActions = minecraft.options.keyQuickActions;
        boolean opensAdvancements = mapping.same(advancements);
        boolean opensQuickActions = mapping.same(quickActions);
        if (!opensAdvancements && !opensQuickActions) return;

        PathwayAbilities.Ability ability = PathwayAbilities.of(ClientPathwayData.getPathway(), slot);
        if (ability == null || ClientPathwayData.getSequence() > ability.sequence()
                || ability.normalChargeTicks() <= 0 && ability.sneakChargeTicks() <= 0) return;

        if (opensAdvancements) discardClicks(advancements);
        if (opensQuickActions) discardClicks(quickActions);
    }

    private static void discardClicks(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            // Ignore queued repeats from the same physical key.
        }
    }

    @SubscribeEvent
    public static void sendAbilityInput(TickEvent.ClientTickEvent.Post event) {
        // Vanilla sends movement input, including sneaking, between the two tick events.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == lastPlayer && minecraft.level == lastLevel) {
            PENDING_INPUT.forEach(PacketHandler::sendToServer);
        }
        PENDING_INPUT.clear();
    }
}
