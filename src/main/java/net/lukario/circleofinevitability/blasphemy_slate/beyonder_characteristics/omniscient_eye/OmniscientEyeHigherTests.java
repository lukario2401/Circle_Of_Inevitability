package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

public final class OmniscientEyeHigherTests {
    private static ServerPlayer player(GameTestHelper h){var p=OmniscientEyeGameTests.player(h,0);p.setYRot(0);p.setXRot(0);return p;}
    private static Mob enemy(GameTestHelper h,ServerPlayer p){
        var mob=h.spawn(EntityTypes.HUSK,new BlockPos(1,1,2));BeyonderMobs.make(mob,"Wanderer",8);
        mob.setNoAi(true);mob.setPos(p.getX(),p.getY(),p.getZ()+1);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);mob.setHealth(2000);
        h.addCleanup(ok->mob.discard());return mob;
    }
    private static void cast(GameTestHelper h,ServerPlayer p,AbilityWheel wheel,String key){
        SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);
        h.assertTrue(wheel.cast(p,wheel.entry(key),true)==AbilityWheel.Result.DONE,"Cast succeeds: "+key);
    }
    public static void wheelStudy(GameTestHelper h){
        var observer=OmniscientEyeGameTests.player(h,6);observer.setYRot(0);observer.setXRot(0);
        var caster=OmniscientEyeGameTests.player(h,6);caster.setPos(observer.getX(),observer.getY(),observer.getZ()+1);
        var wheel=AbilityWheel.create("omniscient_observed_wheel","Observed").pathway("Wanderer")
            .entry("entry","ability_menus/key_of_stars/blink","Observed entry",8,1,0,(p,sl)->AbilityWheel.Result.DONE);
        wheel.cast(caster,wheel.entry("entry"));String id="wheel/omniscient_observed_wheel/entry";
        h.assertTrue(Codex.study(observer,id)>0,"Real wheelCast notification gains study");
        var power=AbilityCatalog.all().get(id);
        h.assertTrue(power!=null && power.cast().entry()==wheel.entry("entry") && power.icon().contains("blink"),"Exact wheel entry and icon survive catalog lookup");
        Codex.choice(observer,id,false);h.assertTrue(!Codex.data(observer).getStringOr("selected","").equals(id),"Incomplete wheel entry cannot be selected");
        Codex.learn(observer,power,100);Codex.choice(observer,id,false);Codex.replicate(observer);
        h.assertTrue(Casts.lastPerformed(observer).entry()==power.cast().entry(),"Replication casts selected entry, not a random wheel pick");h.succeed();
    }
    public static void fate(GameTestHelper h){
        var p=player(h);var target=enemy(h,p);var wheel=OmniscientEyeHigher.FATE;
        cast(h,p,wheel,"prophecy");h.assertTrue(Prediction.active(p),"Prediction granted");
        cast(h,p,wheel,"cognize");h.assertTrue(Appraisal.bonus(p,target)>=.25f,"Cognize mark");
        h.assertTrue(AbilityCatalog.of(target).stream().anyMatch(power->Codex.study(p,power.id())>=50),"Cognize studies actual enemy powers");
        cast(h,p,wheel,"leverage");h.assertTrue(Weight.isHeavy(target)&&Vulnerable.extra(target)>=.3f,"Leverage strips and burdens");
        net.lukario.circleofinevitability.root.Root.apply(target,100,net.lukario.circleofinevitability.root.RootRestriction.MOVEMENT);
        cast(h,p,wheel,"decipher");h.assertTrue(!net.lukario.circleofinevitability.root.Root.isRooted(target),"Decipher frees bound body");
        cast(h,p,wheel,"bookshelf_form");h.assertTrue(Form.isIn(p,OmniscientEyeHigher.FORM),"Bookshelf form active");
        float before=SoulCore.getSpirituality(p);
        h.assertTrue(wheel.cast(p,wheel.entry("bookshelf_form"))==AbilityWheel.Result.FREE,"Ending form is free despite cooldown");
        h.assertTrue(!Form.isIn(p,OmniscientEyeHigher.FORM)&&SoulCore.getSpirituality(p)==before,"Form ends without charge");h.succeed();
    }
    public static void authorities(GameTestHelper h){
        var p=player(h);var target=enemy(h,p);var wheel=OmniscientEyeHigher.OMNISCIENCE;
        cast(h,p,wheel,"omniscience");h.assertTrue(Appraisal.bonus(p,target)>=.35f,"Omniscience strongest mark");
        Substitute.arm(target,100,1,(who,source,amount,sl)->{});
        cast(h,p,wheel,"exploit");h.assertTrue(!Substitute.isArmed(target),"Exploit removes ward first");
        cast(h,p,wheel,"redefine");h.assertTrue(AbilityLock.isLocked(target),"Redefine locks Beyonder mob abilities");
        float hp=target.getHealth();cast(h,p,wheel,"calamity_of_knowledge");
        h.assertTrue(target.getHealth()<hp && Madness.get(target)==5,"Calamity hurts marked target and adds Madness");
        cast(h,p,wheel,"evade_disaster");h.assertTrue(DamageWard.blocks(p,p.damageSources().generic(),2),"Evade wards source-less damage");
        var token=Substitute.snapshot(p);h.assertTrue(!Substitute.takeSnapshot(p,token,p.damageSources().mobAttack(target),1),"Small hit preserves conditional escape");
        h.assertTrue(Substitute.takeSnapshot(p,token,p.damageSources().mobAttack(target),p.getMaxHealth()),"Large hit consumes conditional escape");
        cast(h,p,wheel,"wise_counsel");h.assertTrue(Zone.isOpen(p,"omniscient_counsel"),"Counsel zone active");
        h.succeed();
    }
    public static void tower(GameTestHelper h){
        var p=player(h);var target=enemy(h,p);var wheel=OmniscientEyeHigher.OMNISCIENCE;
        cast(h,p,wheel,"tower_of_omniscience");h.assertTrue(OmniscientEyeHigher.kingdom(p),"Kingdom follows owner");
        var nativePower=net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities.of("Wanderer",2);
        Casts.mobCast(target,p.level(),Casts.madeUpFor(target,p.level(),nativePower,false));
        var id="key/Wanderer/2/false";h.assertTrue(Codex.study(p,id)==100,"Tower fully studies a real cast inside");
        var other=player(h);other.setPos(p.getX(),p.getY(),p.getZ()+1);
        var observed=AbilityWheel.create("omniscient_tower_wheel","Tower observation").entry("power","","Tower power",0,1,0,(who,sl)->AbilityWheel.Result.DONE);
        observed.cast(other,observed.entry("power"));
        h.assertTrue(Codex.study(p,"wheel/omniscient_tower_wheel/power")==100,"Tower fully studies an individual wheel entry");
        h.assertTrue(OmniscientEyeHigher.replicationCost(p)==1,"Tower replication costs original price");
        float before=SoulCore.getSpirituality(p);
        h.assertTrue(wheel.cast(p,wheel.entry("tower_of_omniscience"))==AbilityWheel.Result.FREE,"Tower can end through cooldown");
        h.assertTrue(!OmniscientEyeHigher.kingdom(p)&&before==SoulCore.getSpirituality(p),"Ending Tower has no cost");h.succeed();
    }
    public static void loans(GameTestHelper h){
        var p=player(h);var ally=OmniscientEyeGameTests.player(h,6);SoulCore.setPathway(ally,"Wanderer");
        ally.setPos(p.getX(),p.getY(),p.getZ()+1);
        Spells.addAllyCheck((caster,body)->caster.getUUID().equals(p.getUUID()) && body.getUUID().equals(ally.getUUID()));
        int[] count={0};var source=AbilityWheel.create("omniscient_loan_source","Source").entry("power","","Loan",0,20,100,(who,sl)->{count[0]++;return AbilityWheel.Result.DONE;});
        cast(h,p,source,"power");cast(h,p,OmniscientEyeHigher.OMNISCIENCE,"open_source");
        h.assertTrue(AbilityGrants.has(ally),"Open Source grants other pathway a concrete power");
        float before=SoulCore.getSpirituality(ally);
        h.assertTrue(AbilityGrants.use(ally)&&!AbilityGrants.has(ally)&&count[0]==2,"Loan can be used once above recipient Sequence");
        h.assertTrue(Math.abs(before-SoulCore.getSpirituality(ally)-60)<.1,"Borrower pays triple");
        cast(h,p,OmniscientEyeHigher.OMNISCIENCE,"redefine");h.assertTrue(NextCast.active(ally),"Redefine primes allied next cast");
        var nativeSpell=AbilityWheel.create("omniscient_next_test","Next").entry("cast","","Next",9,20,0,(who,sl)->AbilityWheel.Result.DONE);
        before=SoulCore.getSpirituality(ally);nativeSpell.cast(ally,nativeSpell.entry("cast"));
        h.assertTrue(SoulCore.getSpirituality(ally)==before&&!NextCast.active(ally),"Next cast free and consumed");
        nativeSpell.cast(ally,nativeSpell.entry("cast"));h.assertTrue(SoulCore.getSpirituality(ally)<before,"Following cast pays normally");h.succeed();
    }
    public static void sharedKit(GameTestHelper h){
        var p=player(h);var target=enemy(h,p);
        Vec3 predicted=Prediction.lead(Vec3.ZERO,new Vec3(10,0,0),new Vec3(0,0,1),2);
        h.assertTrue(Math.abs(predicted.length()/2-predicted.z)<.001,"Prediction solves interception time");
        Madness.add(p,target,9);h.assertTrue(Madness.get(target)==9,"Madness builds to threshold");
        float hp=target.getHealth();Madness.add(p,target,1);
        h.assertTrue(Madness.get(target)==3 && target.getHealth()<hp,"Madness breaks and resets to three");
        h.assertTrue(Dossier.lines(p,target,3).stream().anyMatch(line->line.getString().contains("Wanderer")),"Dossier reads Beyonder identity");
        NextCast.arm(p,100,0,1.3f);float before=SoulCore.getSpirituality(p);
        Casts.keyPressed(p,1);NextCast.run(p,()->{Spells.paySpirituality(p,8);return true;});Casts.keyDone(p);
        h.assertTrue(!NextCast.active(p)&&SoulCore.getSpirituality(p)==before,"Native zero-cost key still consumes one-shot grant");h.succeed();
    }
}
