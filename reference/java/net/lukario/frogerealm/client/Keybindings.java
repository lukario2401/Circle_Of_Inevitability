package net.lukario.frogerealm.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

public final class Keybindings {
    public static final Keybindings INSTANCE = new Keybindings();

    private Keybindings() {}

    private static final String CATEGORY = "key.categories." + ForgeRealm.MOD_ID;

    public final KeyMapping exampleKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".example_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_P, -1),
            CATEGORY
    );

    public final KeyMapping examplePacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".example_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_V, -1),
            CATEGORY
    );

    public final KeyMapping abilityOnePacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".ability_one_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_G, -1),
            CATEGORY
    );
    public final KeyMapping abilityTwoPacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".ability_two_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_H, -1),
            CATEGORY
    );
    public final KeyMapping abilityThreePacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".ability_three_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_J, -1),
            CATEGORY
    );
    public final KeyMapping abilityFourPacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".ability_four_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_K, -1),
            CATEGORY
    );
    public final KeyMapping abilityFivePacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".ability_five_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_L, -1),
            CATEGORY
    );
    public final KeyMapping abilitySixPacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".ability_six_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_N, -1),
            CATEGORY
    );
    public final KeyMapping abilitySevenPacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".ability_seven_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_M, -1),
            CATEGORY
    );
}