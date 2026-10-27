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
public class COpenAbilityMenuPacket {

    /** detail: a small second line under the label ("" = none). locked: greyed out, cannot be picked. */
    public record Option(Identifier icon, String label, int x, int y, String detail, boolean locked, String description) {}

    private final String menuId;
    private final String title;
    private final int iconSize;
    private final List<Option> options;

    public COpenAbilityMenuPacket(String menuId, String title, int iconSize, List<Option> options) {
        this.menuId = menuId;
        this.title = title;
        this.iconSize = iconSize;
        this.options = options;
    }

    public COpenAbilityMenuPacket(FriendlyByteBuf buffer) {
        this.menuId = buffer.readUtf();
        this.title = buffer.readUtf();
        this.iconSize = buffer.readInt();
        int count = buffer.readInt();
        this.options = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            options.add(new Option(buffer.readIdentifier(), buffer.readUtf(), buffer.readInt(), buffer.readInt(),
                    buffer.readUtf(), buffer.readBoolean(), buffer.readUtf()));
        }
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
