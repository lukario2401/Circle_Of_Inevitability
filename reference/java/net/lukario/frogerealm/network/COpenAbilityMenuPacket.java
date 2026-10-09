package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.AbilityMenuScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client: open an icon menu. Don't use directly — use AbilityMenu.open(player).
 */
public class COpenAbilityMenuPacket {

    public record Option(ResourceLocation icon, String label, int x, int y) {}

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
            options.add(new Option(buffer.readResourceLocation(), buffer.readUtf(), buffer.readInt(), buffer.readInt()));
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(menuId);
        buffer.writeUtf(title);
        buffer.writeInt(iconSize);
        buffer.writeInt(options.size());
        for (Option option : options) {
            buffer.writeResourceLocation(option.icon());
            buffer.writeUtf(option.label());
            buffer.writeInt(option.x());
            buffer.writeInt(option.y());
        }
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            AbilityMenuScreen.open(menuId, title, iconSize, options);
        }
    }
}
