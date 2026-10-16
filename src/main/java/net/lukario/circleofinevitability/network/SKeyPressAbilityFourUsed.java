package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Parasite;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypseSilkWeaver;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesAirCannon;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorFlameTransformation;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManFleshBomb;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessPotions;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.HandOfOrder.handOfOrderAbility4;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KeyOfStars.keyOfStarsSeal;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorSpearSkull;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionHammer;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.HandOfGodDawn.handOfGodDawn;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KnightOfMisfortune.knightOfMisfortuneRequiem;

public class SKeyPressAbilityFourUsed {

    public SKeyPressAbilityFourUsed() {}
    public SKeyPressAbilityFourUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        // rooted with ABILITIES = can't use any ability
        if (Root.has(player, RootRestriction.ABILITIES)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 4)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 4)) return;

        keyOfStarsSeal(player,level,serverLevel,false);
        attendantOfMysteriesAirCannon(player,level,serverLevel,false);
        princeOfAbolitionHammer(player,level,serverLevel,false);
        handOfOrderAbility4(player,level,serverLevel,false);
        paleEmperorSpearSkull(player,level,serverLevel,false);
        conquerorFlameTransformation(player,level,serverLevel,false);
        apocalypseSilkWeaver(player,level,serverLevel,false);
        hangedManFleshBomb(player,level,serverLevel,false);
        beautyGoddessPotions(player,level,serverLevel,false);
        handOfGodDawn(player,level,serverLevel,false);
        knightOfMisfortuneRequiem(player,level,serverLevel,false);
    }

}
