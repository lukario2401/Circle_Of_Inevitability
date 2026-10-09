package net.lukario.frogerealm.network;

import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoDuelist;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalPenitent.abyssalPenitentSacrificialOffering;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilityTwoUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.orbReclaim;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesFlamingJump;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderDecree;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.BloodBoundAscetic.crimsonOffering;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.timeSnare;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.DeathDescendant.umbralBind;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.phantomStrike;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.devour;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.GravityArchitect.detonateWellAbility;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.HangedAscetic.hangedAsceticFleshBomb;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.stanceShift;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsDash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PhantomSequence.mirageStep;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PlagueSovereign.miasmaCloud;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.rend;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RunicSequencer.runicSequencerSigilBind;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilityTwoUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.Shepard.shepardShadowShaping;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.detonateSigilsAbility;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.discharge;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilityThreeUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VectorArbiter.angleBreak;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidAscendant.voidAscendantAbilityTwo;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidResonator.resonanceWave;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilityTwoUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorDoor;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionFlame;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.expeditioners.Verso.crescendoBladeMarkingShot;

public class SKeyPressAbilityTwoUsed {

    public SKeyPressAbilityTwoUsed() {}
    public SKeyPressAbilityTwoUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        // rooted with ABILITIES = can't use any ability
        if (Root.has(player, RootRestriction.ABILITIES)) {
            player.displayClientMessage(Component.literal("You are bound by Order."), true);
            return;
        }

        shadowSlaveAspectAbilityTwoUsed(player,level,serverLevel);
        ChronoDuelist.chronoDuelistAbilityThreeUsed(player,level,serverLevel);
        stormHeraldAbilityThreeUsed(player,serverLevel);
        voidWalkerAbilityTwoUsed(player,serverLevel);
        aetherWardenAbilityTwoUsed(player,serverLevel);
        rend(player,level,serverLevel);
        detonateSigilsAbility(player,level,serverLevel);
        orbReclaim(player,level,serverLevel);
        stanceShift(player,serverLevel);
        phantomStrike(player,level,serverLevel);
        timeSnare(player,level,serverLevel);
        discharge(player,level,serverLevel);
        voidAscendantAbilityTwo(player,level,serverLevel);
        devour(player,level,serverLevel);
        crimsonOffering(player,serverLevel);
        angleBreak(player,level,serverLevel);
        resonanceWave(player,level,serverLevel);
        miasmaCloud(player,level,serverLevel);
        detonateWellAbility(player,level,serverLevel);
        umbralBind(player,level,serverLevel);
        mirageStep(player,level,serverLevel);
        runicSequencerSigilBind(player,level,serverLevel);
        abyssalPenitentSacrificialOffering(player,serverLevel);
        hangedAsceticFleshBomb(player,level,serverLevel, false);
        shepardShadowShaping(player,level,serverLevel, false);
        keyOfStarsDash(player,level,serverLevel, false);
        attendantOfMysteriesFlamingJump(player,level,serverLevel, false);
        handOfOrderDecree(player,level,serverLevel, false);
        crescendoBladeMarkingShot(player,level,serverLevel);
        princeOfAbolitionFlame(player,level,serverLevel,false);
        paleEmperorDoor(player,serverLevel,false);

    }
}
