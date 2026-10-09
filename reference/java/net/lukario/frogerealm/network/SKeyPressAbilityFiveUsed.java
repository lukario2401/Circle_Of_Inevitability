package net.lukario.frogerealm.network;

import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalPenitent.abyssalPenitentForbiddenInsight;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilityFiveUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.orbSplit;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesCountdown;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.BloodBoundAscetic.lastStand;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoDuelist.chronoDuelistAbilitySix;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.timeCollapse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.DeathDescendant.soulFracture;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.realityFracture;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.fleshHook;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.GravityArchitect.eventHorizon;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.HangedAscetic.hangedAsceticCullOfSpiritualFlesh;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.overheat;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderAbility5;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsTransport;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorSpikes;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PhantomSequence.specterBreak;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PlagueSovereign.epidemic;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.executionDrive;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RunicSequencer.runicSequencerSigilExecute;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.Shepard.shepardCommandeerShadow;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.delayedCollapse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.stormChannel;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilitySixUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VectorArbiter.vectorArbiterBacklinePierce;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidAscendant.voidAscendantAbilityFive;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidResonator.frequencySurge;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilityFiveUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.expeditioners.Verso.crescendoBladeOverload;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionMeteor;

public class SKeyPressAbilityFiveUsed {

    public SKeyPressAbilityFiveUsed() {}
    public SKeyPressAbilityFiveUsed(FriendlyByteBuf buffer) {}
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

        chronoDuelistAbilitySix(player,serverLevel);
        stormHeraldAbilitySixUsed(player,serverLevel);
        voidWalkerAbilityFiveUsed(player,serverLevel);
        aetherWardenAbilityFiveUsed(player,serverLevel);
        executionDrive(player,level,serverLevel);
        delayedCollapse(player,level,serverLevel);
        orbSplit(player,serverLevel);
        overheat(player,serverLevel);
        realityFracture(player,level,serverLevel);
        timeCollapse(player,serverLevel);
        stormChannel(player,serverLevel);
        voidAscendantAbilityFive(player,level,serverLevel);
        fleshHook(player,level,serverLevel);
        lastStand(player,serverLevel);
        vectorArbiterBacklinePierce(player,level,serverLevel);
        frequencySurge(player,serverLevel);
        epidemic(player,level,serverLevel);
        eventHorizon(player,serverLevel);
        soulFracture(player,level,serverLevel);
        specterBreak(player,level,serverLevel);
        runicSequencerSigilExecute(player,level,serverLevel);
        abyssalPenitentForbiddenInsight(player,serverLevel);
        hangedAsceticCullOfSpiritualFlesh(player,level,serverLevel, false);
        shepardCommandeerShadow(player,level,serverLevel, false);
        keyOfStarsTransport(player,level,serverLevel, false);
        attendantOfMysteriesCountdown(player,level,serverLevel, false);
        handOfOrderAbility5(player,level,serverLevel, false);
        crescendoBladeOverload(player,serverLevel);
        princeOfAbolitionMeteor(player,level,serverLevel,false);
        paleEmperorSpikes(player,level,serverLevel,false);


    }
}
