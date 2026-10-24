package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.KeySeal;
import net.lukario.circleofinevitability.status.Parasite;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationZombie.abominationZombie;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypseSilkWeaver;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesAirCannon;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorFlameTransformation;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManFleshBomb;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessPotions;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderVerdicts.handOfOrderVerdict;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KeyOfStars.keyOfStarsSeal;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorSpearSkull;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.FrenziedMage.frenziedMageHammer;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodDawn.handOfGodDawn;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortune.knightOfMisfortuneRequiem;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition.princeOfAbolitionDistort;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor.knowledgeEmperorScrolls;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorWorkshop.illuminatorWorkshop;

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

        // a Decree that prohibits abilities here (status/Decree, ABILITIES): refused, and a Violation
        if (Decree.blocksKey(player, 4)) return;
        // rooted with ABILITIES = can't use any ability (status/Silence lets a power that needs no voice through)
        if (Silence.blocksKey(player, 4)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 4)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 4)) return;
        // one power taken away for a while (status/KeySeal, the Hand Of Order's Deprivation)
        if (KeySeal.blocksKey(player, 4)) return;
        KeySeal.pressed(player, 4);

        keyOfStarsSeal(player,level,serverLevel,false);
        attendantOfMysteriesAirCannon(player,level,serverLevel,false);
        frenziedMageHammer(player,level,serverLevel,false);
        handOfOrderVerdict(player,level,serverLevel,false);
        paleEmperorSpearSkull(player,level,serverLevel,false);
        conquerorFlameTransformation(player,level,serverLevel,false);
        apocalypseSilkWeaver(player,level,serverLevel,false);
        hangedManFleshBomb(player,level,serverLevel,false);
        beautyGoddessPotions(player,level,serverLevel,false);
        handOfGodDawn(player,level,serverLevel,false);
        knightOfMisfortuneRequiem(player,level,serverLevel,false);
        abominationZombie(player,level,serverLevel,false);
        princeOfAbolitionDistort(player,level,serverLevel,false);
        knowledgeEmperorScrolls(player,level,serverLevel,false);
        illuminatorWorkshop(player,level,serverLevel,false);
    }

}
