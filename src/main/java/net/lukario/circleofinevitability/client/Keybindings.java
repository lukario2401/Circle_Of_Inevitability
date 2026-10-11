package net.lukario.circleofinevitability.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.settings.KeyConflictContext;

public final class Keybindings {
    // 26.2: categories are KeyMapping.Category records, and registering the same id twice throws, so this one
    // is shared with DashKeybindClient and StarBurstKeybindClient (all three used "key.categories.<modid>").
    // Its label is the lang key key.category.circle_of_inevitability.main.
    // It must stay above INSTANCE: static fields are set in order, and INSTANCE's keys read it.
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "main"));

    public static final Keybindings INSTANCE = new Keybindings();

    private Keybindings() {}

    public final KeyMapping exampleKey = new KeyMapping(
            "key." + CircleOfInevitability.MOD_ID + ".example_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_P,
            CATEGORY, 0
    );

    public final KeyMapping examplePacketKey = new KeyMapping(
            "key." + CircleOfInevitability.MOD_ID + ".example_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_V,
            CATEGORY, 0
    );

    public final KeyMapping abilityOnePacketKey = new KeyMapping(
            "key." + CircleOfInevitability.MOD_ID + ".ability_one_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_G,
            CATEGORY, 0
    );
    public final KeyMapping abilityTwoPacketKey = new KeyMapping(
            "key." + CircleOfInevitability.MOD_ID + ".ability_two_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_H,
            CATEGORY, 0
    );
    public final KeyMapping abilityThreePacketKey = new KeyMapping(
            "key." + CircleOfInevitability.MOD_ID + ".ability_three_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_J,
            CATEGORY, 0
    );
    public final KeyMapping abilityFourPacketKey = new KeyMapping(
            "key." + CircleOfInevitability.MOD_ID + ".ability_four_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_K,
            CATEGORY, 0
    );
    public final KeyMapping abilityFivePacketKey = new KeyMapping(
            "key." + CircleOfInevitability.MOD_ID + ".ability_five_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_L,
            CATEGORY, 0
    );
    public final KeyMapping abilitySixPacketKey = new KeyMapping(
            "key." + CircleOfInevitability.MOD_ID + ".ability_six_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_N,
            CATEGORY, 0
    );
    public final KeyMapping abilitySevenPacketKey = new KeyMapping(
            "key." + CircleOfInevitability.MOD_ID + ".ability_seven_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_M,
            CATEGORY, 0
    );
}