package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;
import java.util.function.Consumer;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.DeferredRegister;

public final class OmniscientEyeGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS=DeferredRegister.create(Registries.TEST_FUNCTION,"circle_of_inevitability");
    static {
        FUNCTIONS.register("omniscient_rules",()->OmniscientEyeGameTests::rules);
        FUNCTIONS.register("omniscient_storage",()->OmniscientEyeGameTests::storage);
        FUNCTIONS.register("omniscient_replication",()->OmniscientEyeGameTests::replication);
        FUNCTIONS.register("omniscient_abilities",()->OmniscientEyeGameTests::abilities);
        FUNCTIONS.register("omniscient_observation",()->OmniscientEyeGameTests::observation);
    }
    private static ServerPlayer player(GameTestHelper h,int seq) {
        ServerPlayer p=h.makeMockServerPlayerInLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        var at=h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,1,1.5));p.setPos(at.x,at.y,at.z);
        SoulCore.setPathway(p,"Omniscient Eye");SoulCore.setSequence(p,seq);SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);
        h.addCleanup(ok->p.discard());return p;
    }
    private static void rules(GameTestHelper h) {
        for(int seq=0;seq<=9;seq++)for(int ability=0;ability<=9;ability++) {
            double gain=StudyRules.gain(seq,ability);
            h.assertTrue((gain>0)==(ability>=seq),"Only same or weaker Sequence can be studied");
            if(seq>0 && ability>=seq)h.assertTrue(StudyRules.gain(seq-1,ability)>=gain,"Stronger observer studies faster");
            if(ability<9 && ability>=seq)h.assertTrue(StudyRules.gain(seq,ability+1)>=gain,"Weaker powers study faster");
        }
        h.assertTrue(StudyRules.gain(0,9)==100,"Sequence zero learns Sequence nine immediately");
        h.assertTrue(StudyRules.compare(true,1,9,"A",false,100,0,"B")<0,"Favorites first");
        h.assertTrue(StudyRules.compare(false,90,9,"A",false,80,0,"B")<0,"Study descending");
        h.assertTrue(StudyRules.compare(false,90,0,"A",false,90,9,"B")<0,"Stronger Sequence wins ties");h.succeed();
    }
    private static void storage(GameTestHelper h) {
        ServerPlayer p=player(h,6);var catalog=Codex.catalog();
        var weak=catalog.values().stream().filter(a->a.sequence()==9).findFirst().orElseThrow();
        var strong=catalog.values().stream().filter(a->a.sequence()<6).findFirst().orElseThrow();
        h.assertTrue(!Codex.learn(p,strong,100),"OP learning still respects Sequence");
        Codex.learn(p,weak,20);Codex.learn(p,weak,30);
        h.assertTrue(Codex.study(p,weak.id())==50,"Repeated observations accumulate");
        Codex.choice(p,weak.id(),false);Codex.data(p).putBoolean("favorite/"+weak.id(),true);
        var saved=p.getPersistentData().getCompoundOrEmpty("circle_of_inevitability").copy();
        p.getPersistentData().put("circle_of_inevitability",saved);
        var row=Codex.rows(p).getFirst();h.assertTrue(row.favorite() && row.selected() && row.study()==50,"NBT round trip retains progress, favorite and selection");
        ServerPlayer clone=player(h,6);
        SoulCore.onPlayerClone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,p,true));
        h.assertTrue(Codex.study(clone,weak.id())==50 && Codex.rows(clone).getFirst().favorite(),"Death clone retains library");
        SoulCore.setSequence(p,9);Codex.choice(p,strong.id(),false);
        h.assertTrue(Codex.data(p).getStringOr("selected","").equals(weak.id()),"Forged selection refused");h.succeed();
    }
    private static void replication(GameTestHelper h) {
        ServerPlayer p=player(h,6);int[] count={0};
        AbilityWheel wheel=AbilityWheel.create("omniscient_test_copy","Test")
            .entry("copy","","Copy",9,20,1200,(who,sl)->{
                h.assertTrue(!net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder.hasSequence(who,0),"Copy cannot unlock higher Sequence upgrades");
                count[0]++;return AbilityWheel.Result.DONE;});
        var power=Codex.catalog().get("wheel/omniscient_test_copy/copy");
        Codex.learn(p,power,100);Codex.choice(p,power.id(),false);
        float before=SoulCore.getSpirituality(p);Codex.replicate(p);Codex.replicate(p);
        h.assertTrue(count[0]==2,"Selected power repeats immediately");
        h.assertTrue(Math.abs(before-SoulCore.getSpirituality(p)-60)<.1,"Each copy pays 1.5 times original price");
        h.assertTrue(wheel.cooldownLeft(p,wheel.entry("copy"))==0,"No wheel cooldown starts");
        h.assertTrue(Casts.last(p)==null,"Copies are not broadcast for recursive study");
        SoulCore.setSpiritualityQuietly(p,0);Codex.replicate(p);h.assertTrue(count[0]==2,"Unfunded copy refused");h.succeed();
    }
    private static void abilities(GameTestHelper h) {
        ServerPlayer p=player(h,4);var wheel=OmniscientEye.ARTS;
        h.assertTrue(wheel.entries().size()==6,"Analyze plus five Mystic Arts");
        var armor=wheel.entry("golden_armor_art");
        h.assertTrue(wheel.cast(p,armor)==AbilityWheel.Result.DONE,"Golden Armor Art succeeds");
        h.assertTrue(net.lukario.circleofinevitability.status.ConjuredArmor.isWorn(p,"omniscient_gold"),"Armor worn");
        h.assertTrue(wheel.cast(p,armor)==AbilityWheel.Result.REFUSED,"Own Mystic Arts retain their cooldown");
        float before=SoulCore.getSpirituality(p);
        h.assertTrue(wheel.cast(p,wheel.entry("analyze"))==AbilityWheel.Result.REFUSED,"Analyze needs a target");
        h.assertTrue(SoulCore.getSpirituality(p)==before,"Refused ability costs nothing");h.succeed();
    }
    private static void observation(GameTestHelper h) {
        ServerPlayer p=player(h,6);p.setYRot(0);p.setXRot(0);
        var mob=h.spawn(net.minecraft.world.entity.EntityTypes.HUSK,new net.minecraft.core.BlockPos(1,1,2));
        mob.setNoAi(true);mob.setPos(p.getX(),p.getY(),p.getZ()+1);
        h.addCleanup(ok->mob.discard());
        var ability=net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities.of("Wanderer",2);
        var cast=Casts.madeUpFor(mob,p.level(),ability,false);String id=Codex.id(cast);
        h.assertTrue(p.level().players().contains(p),"Observer registered in level");
        h.assertTrue(cast.copyable(),"Test ability is copyable");
        h.assertTrue(p.hasLineOfSight(mob),"Test caster has clear line of sight");
        h.assertTrue(p.getLookAngle().dot(mob.getEyePosition().subtract(p.getEyePosition()).normalize())>.5,"Test caster in front of observer");
        Codex.observe(mob,p.level(),cast);double learned=Codex.study(p,id);
        h.assertTrue(learned>0,"Visible mob cast is studied");
        Codex.observe(mob,p.level(),cast);h.assertTrue(Codex.study(p,id)==learned*2,"Repeated casts add progress");
        p.setYRot(180);Codex.observe(mob,p.level(),cast);
        h.assertTrue(Codex.study(p,id)==learned*2,"Casts behind observer are not studied");
        p.setYRot(0);h.setBlock(1,2,2,net.minecraft.world.level.block.Blocks.STONE);
        Codex.observe(mob,p.level(),cast);h.assertTrue(Codex.study(p,id)==learned*2,"Wall blocks observation");
        h.succeed();
    }
}
