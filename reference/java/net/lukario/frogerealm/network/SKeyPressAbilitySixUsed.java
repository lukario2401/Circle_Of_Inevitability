package net.lukario.frogerealm.network;

import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalMonarch.abyssalMonarchAbilitySix;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalPenitent.abyssalPenitentAbyssalRelease;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilitySixUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.cosmicOverdraw;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.BloodBoundAscetic.bloodBoundAsceticBloodFrenzy;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.delayedExecution;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.DeathDescendant.darkFeast;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.DeathDescendant.shiftMode;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.phaseLock;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.bloodFrenzy;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.GravityArchitect.singularityBeam;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.HangedAscetic.hangedAsceticShadowSweep;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.cataclysmForm;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsOrbital;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorWings;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PhantomSequence.veilOfMirrors;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PlagueSovereign.necroticBurst;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.cataclysmState;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RunicSequencer.runicSequencerQuickInscription;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilitySixUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.Shepard.shepardCullOfSpiritualFlesh;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.apocalypseScript;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.thunderCollapse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilitySevenUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VectorArbiter.fracturePlane;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidAscendant.voidAscendantAbilitySix;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidResonator.resonantChain;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilitySixUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.expeditioners.Verso.crescendoBladeFromFire;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderWords;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionEntropy;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesHistoricalVoid;

public class SKeyPressAbilitySixUsed {

    public SKeyPressAbilitySixUsed() {}
    public SKeyPressAbilitySixUsed(FriendlyByteBuf buffer) {}
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

        shadowSlaveAspectAbilitySixUsed(player,level,serverLevel);
        abyssalMonarchAbilitySix(player,serverLevel);
        stormHeraldAbilitySevenUsed(player,serverLevel);
        voidWalkerAbilitySixUsed(player,serverLevel);
        aetherWardenAbilitySixUsed(player,serverLevel);
        cataclysmState(player,serverLevel);
        apocalypseScript(player,serverLevel);
        cosmicOverdraw(player,serverLevel);
        cataclysmForm(player,serverLevel);
        phaseLock(player,level,serverLevel);
        delayedExecution(player,level,serverLevel);
        thunderCollapse(player,level,serverLevel);
        voidAscendantAbilitySix(player,level,serverLevel);
        bloodFrenzy(player,serverLevel);
        bloodBoundAsceticBloodFrenzy(player,serverLevel);
        fracturePlane(player,level,serverLevel);
        resonantChain(player,level,serverLevel);
        necroticBurst(player,level,serverLevel);
        singularityBeam(player,level,serverLevel);
        darkFeast(player,serverLevel);
        shiftMode(player,serverLevel);
        veilOfMirrors(player,serverLevel);
        runicSequencerQuickInscription(player,serverLevel);
        abyssalPenitentAbyssalRelease(player,level,serverLevel);
        hangedAsceticShadowSweep(player,level,serverLevel,false);
        shepardCullOfSpiritualFlesh(player,level,serverLevel,false);
        keyOfStarsOrbital(player,level,serverLevel,false);
        crescendoBladeFromFire(player,level,serverLevel);
        handOfOrderWords(player,level,serverLevel,false);
        princeOfAbolitionEntropy(player,level,serverLevel,false);
        attendantOfMysteriesHistoricalVoid(player,level,serverLevel,false);
        paleEmperorWings(player,level,serverLevel,false);

    }
}
