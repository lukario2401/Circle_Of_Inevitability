package net.lukario.circleofinevitability.handler;


import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.client.CameraChordInput;
import net.lukario.circleofinevitability.client.ClientDetachedCamera;
import net.lukario.circleofinevitability.client.ClientPathwayData;
import net.lukario.circleofinevitability.client.Keybindings;
import net.lukario.circleofinevitability.combat.AbilityCombos;
import net.lukario.circleofinevitability.item.custom.abilitys.DashKeybindClient;
import net.lukario.circleofinevitability.client.MonocleKeybindClient;
import net.lukario.circleofinevitability.item.custom.abilitys.star_burst.StarBurstKeybindClient;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.lukario.circleofinevitability.network.SLegacyAbilityPacket;
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
    private static boolean lastInputActive;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean detached = ClientDetachedCamera.viewpoint() != null;
        boolean gameplay = CameraChordInput.updateContext(minecraft);
        var keys = Keybindings.INSTANCE;
        if (CameraChordInput.click(keys.exampleKey) && gameplay && !detached) {
            minecraft.player.sendSystemMessage(EXAMPLE_KEY_PRESSED);
        }
        if (CameraChordInput.click(keys.examplePacketKey) && gameplay && !detached) {
            minecraft.player.sendOverlayMessage(EXAMPLE_KEY_PRESSED);
            PacketHandler.sendToServer(new SKeyPressSpawnEntityPacket());
        }
        var dash = DashKeybindClient.DASH_KEY;
        boolean dashPressed = CameraChordInput.click(dash) || CameraChordInput.down(dash);
        boolean burstPressed = CameraChordInput.click(StarBurstKeybindClient.STAR_BURST_KEY);
        if (gameplay && !detached && minecraft.hasSingleplayerServer() && (dashPressed || burstPressed)) {
            PacketHandler.sendToServer(new SLegacyAbilityPacket(dashPressed, burstPressed));
        }
        if (CameraChordInput.click(MonocleKeybindClient.GAZE_KEY) && gameplay && !detached) {
            PacketHandler.sendToServer(new net.lukario.circleofinevitability.network.SMonocleGazePacket());
        }
        pollAbilityKeys(minecraft);
        CameraChordInput.finishTick();
    }

    private static void pollAbilityKeys(Minecraft minecraft) {
        if (minecraft.player != lastPlayer || minecraft.level != lastLevel) {
            Arrays.fill(ABILITY_KEYS_DOWN, false);
            lastInputActive = false;
            lastPlayer = minecraft.player;
            lastLevel = minecraft.level;
        }

        boolean active = ClientDetachedCamera.viewpoint() == null && lastPlayer != null && lastPlayer.isAlive() && minecraft.isWindowActive()
                && minecraft.gui.screen() == null && minecraft.gui.overlay() == null;
        if (!active && lastInputActive) PENDING_INPUT.add(new SAbilityKeyPacket(0, Action.CANCEL));
        lastInputActive = active;
        int releases = 0;
        for (int index = 0; index < ABILITY_KEYS.size(); index++) {
            KeyMapping mapping = ABILITY_KEYS.get(index);
            boolean clicked = CameraChordInput.click(mapping);
            boolean wasDown = ABILITY_KEYS_DOWN[index];
            boolean down = active && CameraChordInput.down(mapping);
            int slot = index + 1;

            if (active && clicked) {
                preventConflictingMenus(minecraft, mapping, slot);
                if (!wasDown || down) {
                    PENDING_INPUT.add(new SAbilityKeyPacket(slot, wasDown ? Action.REPEAT : Action.PRESS));
                }
            }
            if (active && !down) {
                // Send releases of previously held keys before new presses observed in the same tick.
                if (wasDown) PENDING_INPUT.add(releases++, new SAbilityKeyPacket(slot, Action.RELEASE));
                else if (clicked) PENDING_INPUT.add(new SAbilityKeyPacket(slot, Action.RELEASE));
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

        String pathway = ClientPathwayData.getPathway();
        int sequence = ClientPathwayData.getSequence();
        PathwayAbilities.Ability ability = PathwayAbilities.of(pathway, slot);
        boolean canCharge = ability != null && sequence <= ability.sequence()
                && (ability.normalChargeTicks() > 0 || ability.sneakChargeTicks() > 0);
        if (!canCharge && !AbilityCombos.usesSlot(pathway, sequence, slot)) return;

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
