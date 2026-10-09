package net.lukario.frogerealm.network;

import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalMonarch;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalPenitent.abyssalPenitentChainsOfRepentance;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilityFourUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.astralPulse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesAirCannon;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.BloodBoundAscetic.sanguineDrain;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoDuelist.chronoDuelistAbilityFive;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.futureStep;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.DeathDescendant.veilStep;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.voidDrift;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.grotesqueGrowth;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.GravityArchitect.gravityTether;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.HangedAscetic.hangedAsceticGraze;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.flareDash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderAbility4;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsSeal;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PhantomSequence.echoShade;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PlagueSovereign.harvestSoul;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.overload;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RunicSequencer.runicSequencerSigilConverge;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilityFourUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.Shepard.shepardGrazing;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.spreadMark;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.overclock;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilityFiveUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VectorArbiter.pivotStep;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidAscendant.voidAscendantAbilityFour;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidResonator.frequencyShift;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilityFourUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorSpearSkull;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.expeditioners.Verso.crescendoBladeSteelStrike;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionHammer;

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
            player.displayClientMessage(Component.literal("You are bound by Order."), true);
            return;
        }

        shadowSlaveAspectAbilityFourUsed(player,level,serverLevel);
        AbyssalMonarch.abyssalMonarchAbilityFour(player,serverLevel);
        chronoDuelistAbilityFive(player,serverLevel);
        stormHeraldAbilityFiveUsed(player,serverLevel);
        voidWalkerAbilityFourUsed(player,serverLevel);
        aetherWardenAbilityFourUsed(player,serverLevel);
        overload(player);
        spreadMark(player,level,serverLevel);
        astralPulse(player,level,serverLevel);
        flareDash(player,level,serverLevel);
        voidDrift(player,level,serverLevel);
        futureStep(player,level,serverLevel);
        overclock(player,serverLevel);
        voidAscendantAbilityFour(player,level,serverLevel);
        grotesqueGrowth(player,serverLevel);
        sanguineDrain(player,level,serverLevel);
        pivotStep(player,level,serverLevel);
        frequencyShift(player,serverLevel);
        harvestSoul(player,level,serverLevel);
        gravityTether(player,level,serverLevel);
        veilStep(player,level,serverLevel);
        echoShade(player,level,serverLevel);
        runicSequencerSigilConverge(player,level,serverLevel);
        abyssalPenitentChainsOfRepentance(player,level,serverLevel);
        hangedAsceticGraze(player,level,serverLevel,false);
        shepardGrazing(player,level,serverLevel,false);
        keyOfStarsSeal(player,level,serverLevel,false);
        attendantOfMysteriesAirCannon(player,level,serverLevel,false);
        crescendoBladeSteelStrike(player,level,serverLevel);
        princeOfAbolitionHammer(player,level,serverLevel,false);
        handOfOrderAbility4(player,level,serverLevel,false);
        paleEmperorSpearSkull(player,level,serverLevel,false);
    }

}
