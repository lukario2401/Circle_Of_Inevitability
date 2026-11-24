package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.client.AbilityMenuScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client: open an icon menu. Don't use directly — use AbilityMenu.open(player).
 */
public record COpenAbilityMenuPacket(String menuId, String title, int iconSize, List<Option> options) {

    /** detail: a small second line under the label ("" = none). locked: greyed out, cannot be picked. */
    public record Option(Identifier icon, String label, int x, int y, String detail, boolean locked, String description) {}

    public COpenAbilityMenuPacket {
        options = List.copyOf(options);
    }

    public COpenAbilityMenuPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(), buffer.readUtf(), buffer.readInt(), readOptions(buffer));
    }

    private static List<Option> readOptions(FriendlyByteBuf buffer) {
        int count = buffer.readInt();
        List<Option> options = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            options.add(new Option(buffer.readIdentifier(), buffer.readUtf(), buffer.readInt(), buffer.readInt(),
                    buffer.readUtf(), buffer.readBoolean(), buffer.readUtf()));
        }
        return options;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(menuId);
        buffer.writeUtf(title);
        buffer.writeInt(iconSize);
        buffer.writeInt(options.size());
        for (Option option : options) {
            buffer.writeIdentifier(option.icon());
            buffer.writeUtf(option.label());
            buffer.writeInt(option.x());
            buffer.writeInt(option.y());
            buffer.writeUtf(option.detail());
            buffer.writeBoolean(option.locked());
            buffer.writeUtf(option.description());
        }
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            AbilityMenuScreen.open(menuId, title, iconSize, options);
        }
    }
}
