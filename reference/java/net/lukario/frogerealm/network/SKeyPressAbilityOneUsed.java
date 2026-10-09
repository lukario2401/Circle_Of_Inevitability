package net.lukario.frogerealm.network;

import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalMonarch.abyssalMonarchAspectAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalPenitent.abyssalPenitentProfaneCut;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.astralBolt;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesPaperDagger;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.BloodBoundAscetic.bloodSlash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoDuelist.chronoDuelistAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.temporalSlash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.DeathDescendant.shadowCut;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.phaseShift;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.fleshDevourerRend;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.GravityArchitect.placeGravityWell;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.HangedAscetic.hangedAsceticShadowCurse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.flameCut;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderBuff;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsStarBurst;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.LightBringerAspect.lightBringerAspectAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PhantomSequence.phantomSequencePhantomStrike;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PlagueSovereign.plagueTouch;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.ruinSlash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RunicSequencer.runicSequencerSigilMark;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.Shepard.shepardShadowLurk;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.sigilCarve;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.arcSpike;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VectorArbiter.vectorSlash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidAscendant.voidAscendantAbilityOne;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidResonator.voidPulse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorRoot;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionSpear;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.WormOfTime.wormOfTimeAbility1;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.expeditioners.Verso.crescendoBladeQuickStrike;

public class SKeyPressAbilityOneUsed {

    public SKeyPressAbilityOneUsed() {}
    public SKeyPressAbilityOneUsed(FriendlyByteBuf buffer) {}
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

        shadowSlaveAspectAbilityOneUsed(player,level,serverLevel);
        lightBringerAspectAbilityOneUsed(player,level,serverLevel);
        abyssalMonarchAspectAbilityOneUsed(player,level,serverLevel);
        chronoDuelistAbilityOneUsed(player,level,serverLevel);
        stormHeraldAbilityOneUsed(player,serverLevel);
        voidWalkerAbilityOneUsed(player,serverLevel);
        aetherWardenAbilityOneUsed(player,serverLevel);
        ruinSlash(player,level,serverLevel);
        sigilCarve(player,level,serverLevel);
        astralBolt(player,level,serverLevel);
        flameCut(player,level,serverLevel);
        phaseShift(player,serverLevel);
        temporalSlash(player,level,serverLevel);
        arcSpike(player,level,serverLevel);
        voidAscendantAbilityOne(player,level,serverLevel);
        fleshDevourerRend(player,level,serverLevel);
        bloodSlash(player,level,serverLevel);
        vectorSlash(player,level,serverLevel);
        voidPulse(player,level,serverLevel);
        plagueTouch(player,level,serverLevel);
        placeGravityWell(player,level,serverLevel);
        shadowCut(player,level,serverLevel);
        phantomSequencePhantomStrike(player,level,serverLevel);
        runicSequencerSigilMark(player,level,serverLevel);
        abyssalPenitentProfaneCut(player,level,serverLevel);
        hangedAsceticShadowCurse(player,level,serverLevel,false);
        shepardShadowLurk(player,level,serverLevel,false);
        keyOfStarsStarBurst(player,level,serverLevel,false);
        attendantOfMysteriesPaperDagger(player,level,serverLevel,false);
        crescendoBladeQuickStrike(player,level,serverLevel);
        princeOfAbolitionSpear(player,level,serverLevel,false);
        handOfOrderBuff(player,level,serverLevel,false);
        paleEmperorRoot(player,serverLevel,false);
        wormOfTimeAbility1(player,level,serverLevel,false);
    }
}
