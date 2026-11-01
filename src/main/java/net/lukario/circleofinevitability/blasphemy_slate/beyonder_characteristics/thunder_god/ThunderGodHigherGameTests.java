package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Consumer;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGodGameTests.*;

/** Real-server tests of the higher-sequence authorities and their shared engine hooks. */
public final class ThunderGodHigherGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS=DeferredRegister.create(Registries.TEST_FUNCTION,CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("thunder_full_catalog",()->ThunderGodHigherGameTests::catalog);
        FUNCTIONS.register("thunder_source_gate",()->ThunderGodHigherGameTests::sourceGate);
        FUNCTIONS.register("thunder_source_price",()->ThunderGodHigherGameTests::sourcePrice);
        FUNCTIONS.register("thunder_source_friendly_fire",()->ThunderGodHigherGameTests::friendlyFire);
        FUNCTIONS.register("thunder_kingdom_prices",()->ThunderGodHigherGameTests::kingdomPrices);
        FUNCTIONS.register("thunder_kingdom_seal",()->ThunderGodHigherGameTests::kingdomSeal);
        FUNCTIONS.register("thunder_information",()->ThunderGodHigherGameTests::information);
        FUNCTIONS.register("thunder_dance",()->ThunderGodHigherGameTests::dance);
        FUNCTIONS.register("thunder_maelstrom",()->ThunderGodHigherGameTests::maelstrom);
        FUNCTIONS.register("thunder_punishment",()->ThunderGodHigherGameTests::punishment);
        FUNCTIONS.register("thunder_forest",()->ThunderGodHigherGameTests::forest);
        FUNCTIONS.register("thunder_banish_enemy",()->ThunderGodHigherGameTests::banishEnemy);
        FUNCTIONS.register("thunder_banish_ally",()->ThunderGodHigherGameTests::banishAlly);
        FUNCTIONS.register("thunder_sea_master",()->ThunderGodHigherGameTests::seaMaster);
        FUNCTIONS.register("thunder_artifact_lock",()->ThunderGodHigherGameTests::artifactLock);
        FUNCTIONS.register("thunder_authority",()->ThunderGodHigherGameTests::authority);
        FUNCTIONS.register("thunder_starfall",()->ThunderGodHigherGameTests::starfall);
        FUNCTIONS.register("thunder_presence",()->ThunderGodHigherGameTests::presence);
        FUNCTIONS.register("thunder_high_cleanup",()->ThunderGodHigherGameTests::cleanup);
        FUNCTIONS.register("thunder_light_form",()->ThunderGodHigherGameTests::lightForm);
        FUNCTIONS.register("thunder_chain_openers",()->ThunderGodHigherGameTests::chainOpeners);
        FUNCTIONS.register("thunder_cloud_guard",()->ThunderGodHigherGameTests::cloudGuard);
        FUNCTIONS.register("thunder_magnetism",()->ThunderGodHigherGameTests::magnetism);
        FUNCTIONS.register("thunder_high_recasts",()->ThunderGodHigherGameTests::recasts);
    }
    private ThunderGodHigherGameTests(){}
    private static AbilityWheel.Result cast(ServerPlayer p,GameTestHelper h,Spell s){return ThunderGodPowers.cast(p,h.getLevel(),s);}
    private static void fill(ServerPlayer p){SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);}
    private static void catalog(GameTestHelper h){
        ServerPlayer p=player(h,0);AbilityWheel[] wheels={TIDES,SKY,THUNDER,VOICE,STORM};int[] counts={9,9,9,9,8};
        for(int i=0;i<wheels.length;i++)same(h,wheels[i].entries().size(),counts[i],"Complete wheel size");
        same(h,PathwayAbilities.of(PATHWAY).size(),7,"All seven keys registered");
        SoulCore.setSequence(p,4);h.assertTrue(!STORM.unlocked(p,STORM.entry("storm_hell")),"High entries remain locked");
        h.assertTrue(STORM.unlocked(p,STORM.entry("hurricane")),"Cataclysmic Interrer unlocks the Storm wheel");h.succeed();
    }
    private static void sourceGate(GameTestHelper h){
        ServerPlayer p=player(h,2);ThunderGodEvents.body(p);float health=p.getMaxHealth(),pool=Beyonder.maxSpirituality(p),before=SoulCore.getSpirituality(p);
        Rage.add(p,10);p.setShiftKeyDown(true);key(p,p.level(),h.getLevel(),2,false);
        h.assertTrue(ThunderGodHigher.source(p),"Full Rage starts Source of Wrath");same(h,SoulCore.getSequence(p),2,"Stored Sequence unchanged");same(h,sequence(p),1,"Casting Sequence boosted");
        same(h,p.getMaxHealth(),health,"No health boost");same(h,Beyonder.maxSpirituality(p),pool,"No spirituality pool boost");same(h,before-SoulCore.getSpirituality(p),25000,"Source price paid once");
        h.assertTrue(THUNDER.unlocked(p,THUNDER.entry("heavenly_punishment"))&&!THUNDER.unlocked(p,THUNDER.entry("lightning_forest")),"Only the next Sequence is unlocked");
        fill(p);before=SoulCore.getSpirituality(p);h.assertTrue(TIDES.cast(p,TIDES.entry("tideheal"))==AbilityWheel.Result.REFUSED,"Calm entry refused");same(h,SoulCore.getSpirituality(p),before,"Calm refusal is free");
        Form.end(p);h.assertTrue(!THUNDER.unlocked(p,THUNDER.entry("heavenly_punishment")),"Boost ends with form");h.succeed();
    }
    private static void sourcePrice(GameTestHelper h){
        ServerPlayer p=player(h,1);Rage.add(p,10);cast(p,h,Spell.SOURCE_OF_WRATH);
        h.assertTrue(THUNDER.unlocked(p,THUNDER.entry("lightning_forest")),"Thunder God can reach Tyrant entries");
        h.runAfterDelay(405,()->{
            same(h,sequence(p),1,"Boost expires");same(h,Rage.count(p),0,"Aftermath empties Rage");
            h.assertTrue(p.hasEffect(MobEffects.WEAKNESS)&&p.getEffect(MobEffects.WEAKNESS).getAmplifier()==1,"Spent applies Weakness II; form="+Form.current(p)+", tickCount="+p.tickCount+", remaining="+Form.ticksLeft(p)+", spent="+Marks.count(p,PREFIX+"spent"));
            h.assertTrue(p.hasEffect(MobEffects.SLOWNESS),"Spent applies Slowness");h.succeed();
        });
    }
    private static void friendlyFire(GameTestHelper h){
        ServerPlayer p=player(h,2);Mob ally=target(h,p,0,3);friend(h,p,ally);
        Rage.add(p,10);cast(p,h,Spell.SOURCE_OF_WRATH);float before=ally.getHealth();
        cast(p,h,Spell.GALE_BURST);h.assertTrue(ally.getHealth()<before,"Uncontrolled area attacks hit allies");
        Form.end(p);float after=ally.getHealth();cast(p,h,Spell.GALE_BURST);same(h,ally.getHealth(),after,"Normal ally safety returns after Wrath");h.succeed();
    }
    private static void kingdomPrices(GameTestHelper h){
        ServerPlayer p=player(h,0);h.assertTrue(STORM.cast(p,STORM.entry("chasm_of_storms"))==AbilityWheel.Result.DONE,"Chasm starts");
        h.assertTrue(ThunderGodHigher.inChasm(p),"Owner stands inside Chasm");same(h,Rage.count(p),10,"Chasm fills Rage");
        fill(p);float before=SoulCore.getSpirituality(p);SKY.cast(p,SKY.entry("gale_burst"));same(h,before-SoulCore.getSpirituality(p),1225,"Chasm halves Sky cost");same(h,SKY.cooldownLeft(p,SKY.entry("gale_burst")),120,"Chasm halves cooldown");
        fill(p);STORM.cast(p,STORM.entry("dreadful_heaven"));h.assertTrue(!ThunderGodHigher.inChasm(p)&&ThunderGodHigher.inDreadful(p),"Kingdoms are mutually exclusive");
        fill(p);before=SoulCore.getSpirituality(p);VOICE.cast(p,VOICE.entry("siren_song"));same(h,SoulCore.getSpirituality(p),before,"Voice costs zero in Dreadful Heaven");h.assertTrue(VOICE.cooldownLeft(p,VOICE.entry("siren_song"))>0,"Free Voice keeps cooldown");
        ServerPlayer enemy=player(h,5);enemy.setPos(p.getX()+3,p.getY(),p.getZ());same(h,Spells.costFor(enemy,100),200,"Enemy costs double");h.succeed();
    }
    private static void kingdomSeal(GameTestHelper h){
        ServerPlayer p=player(h,0);cast(p,h,Spell.CHASM_OF_STORMS);Mob outside=target(h,p,0,25);float health=outside.getHealth();
        h.assertTrue(ThunderGodHigher.separated(p,outside),"Kingdom separates bodies");Rage.cast(p).strike(outside,10);same(h,outside.getHealth(),health,"Attacks cannot cross the shell");
        h.assertTrue(Root.has(p,RootRestriction.TELEPORT),"Kingdom cannot be left with a teleport");
        ThunderGodPowers.release(p);h.assertTrue(!ThunderGodHigher.separated(p,outside)&&!Root.has(p,RootRestriction.TELEPORT),"Closing releases the boundary");h.succeed();
    }
    private static void information(GameTestHelper h){
        ServerPlayer p=player(h,2),reader=player(h,5);reader.setPos(p.getX()+3,p.getY(),p.getZ());
        Connection.touch(reader,p);Study.set(reader,p,70);Prediction.grant(reader,300);
        h.assertTrue(Connection.has(reader,p,100)&&Study.get(reader,p)>0&&!Dossier.lines(reader,p,3).isEmpty(),"Information works before disruption");
        cast(p,h,Spell.ELECTROMAGNETIC_DISRUPTION);Vec3 center=Zone.centerOf(p,PREFIX+"electromagnetic_disruption");reader.setPos(center.x,center.y,center.z);
        h.runAfterDelay(8,()->{
            h.assertTrue(Information.blocked(reader)&&!Connection.has(reader,p,100)&&Study.get(reader,p)==0&&!Prediction.active(reader)&&Dossier.lines(reader,p,3).isEmpty(),"All shared information tools respect the field");
            reader.setPos(center.x+100,center.y,center.z);h.assertTrue(!Information.blocked(reader)&&Study.get(reader,p)>0&&!Connection.has(reader,p,100),"Study recovers outside; broken Connections stay ended");h.succeed();
        });
    }
    private static void dance(GameTestHelper h){
        ServerPlayer p=player(h,1),enemy=player(h,5);Vec3 at=p.position().add(0,0,3);enemy.setPos(at.x,at.y,at.z);
        Concealment.hide(enemy,100);Unnoticed.hide(enemy,100);Marks.add(p,enemy,"dance_test",1,100);Connection.touch(enemy,p);
        cast(p,h,Spell.ELECTROMAGNETIC_DISRUPTION);
        h.assertTrue(!Concealment.isHidden(enemy)&&!Unnoticed.isUnnoticed(enemy),"Dance strips enemy hiding");same(h,Marks.count(p,enemy,"dance_test"),0,"Dance removes enemy-owned marks on caster");h.succeed();
    }
    private static void maelstrom(GameTestHelper h){
        ServerPlayer p=player(h,3);p.setXRot(90);Vec3 at=Spells.aimGround(p,h.getLevel(),24);Mob t=target(h,p,0,3);t.setPos(at);
        boolean[] escaped={false};Substitute.armWhen(t,200,1,(source,damage)->true,(who,source,damage,level)->escaped[0]=true);
        cast(p,h,Spell.MAELSTROM);
        h.assertTrue(Root.has(t,RootRestriction.TELEPORT)&&!Substitute.isArmed(t),"Maelstrom holds teleport and suppresses substitutes; target="+t.position()+", center="+Zone.centerOf(p,PREFIX+"maelstrom"));
        Rage.cast(p).strike(t,10);h.assertTrue(!escaped[0]&&t.getHealth()<1000,"Substitute cannot cancel the hit");
        ThunderGodPowers.release(p);h.assertTrue(!Root.has(t,RootRestriction.TELEPORT)&&Substitute.isArmed(t),"Field cleanup releases its own hold without consuming substitute");h.succeed();
    }
    private static void punishment(GameTestHelper h){
        ServerPlayer p=player(h,1);Mob t=target(h,p,0,3);Concealment.hide(t,100);Swarm.start(t,100,8,1,null);Exile.banish(t,100);
        h.assertTrue(cast(p,h,Spell.HEAVENLY_PUNISHMENT)==AbilityWheel.Result.DONE,"Punishment can acquire a concealed/exiled target");same(h,t.getHealth(),1000,"Dark pillar is a warning, not immediate damage");
        h.runAfterDelay(27,()->{h.assertTrue(!Exile.isExiled(t)&&!Concealment.isHidden(t)&&!Swarm.isActive(t),"Punishment strips escape forms");h.assertTrue(t.getHealth()<700,"True strike lands through the veil");h.succeed();});
    }
    private static void forest(GameTestHelper h){
        ServerPlayer p=player(h,0);p.setXRot(90);Vec3 at=Spells.aimGround(p,h.getLevel(),60);Mob enemy=target(h,p,0,3);enemy.setPos(at);Concealment.hide(enemy,200);
        Dome.at(at,4).lasts(200).raise(enemy,h.getLevel(),"forest_enemy");Wall.at(at,0,3,4).lasts(200).raise(enemy,h.getLevel(),"forest_enemy");Zone.at(at,4).lasts(200).open(enemy,h.getLevel(),"forest_enemy");
        Dome.at(at,4).lasts(200).raise(p,h.getLevel(),"forest_ally");
        h.addCleanup(ok->{Dome.close(enemy,"forest_enemy");Wall.close(enemy,"forest_enemy");Zone.close(enemy,"forest_enemy");Dome.close(p,"forest_ally");});
        cast(p,h,Spell.LIGHTNING_FOREST);
        h.runAfterDelay(3,()->{h.assertTrue(!Dome.isRaised(enemy,"forest_enemy")&&!Wall.isRaised(enemy,"forest_enemy")&&!Zone.isOpen(enemy,"forest_enemy"),"Forest destroys enemy barriers");h.assertTrue(Dome.isRaised(p,"forest_ally"),"Allied barrier survives; caster alive="+p.isAlive()+", removed="+p.isRemoved()+", tickCount="+p.tickCount);h.assertTrue(!Concealment.isHidden(enemy),"Enemy veil ends; target="+enemy.position()+", forest="+Zone.centerOf(p,PREFIX+"lightning_forest"));h.succeed();});
    }
    private static void banishEnemy(GameTestHelper h){
        ServerPlayer p=player(h,0);Mob t=target(h,p,0,3);cast(p,h,Spell.BANISH_INTO_THE_STORM);h.assertTrue(Exile.isExiled(t),"Enemy banished");
        h.runAfterDelay(85,()->{h.assertTrue(!Exile.isExiled(t)&&Soaked.isSoaked(t)&&Fear.isAfraid(t)&&t.getHealth()<1000,"Boss returns after four seconds, hurt, wet and afraid");h.succeed();});
    }
    private static void banishAlly(GameTestHelper h){
        ServerPlayer p=player(h,0);Mob t=target(h,p,0,3);friend(h,p,t);cast(p,h,Spell.BANISH_INTO_THE_STORM);h.assertTrue(Exile.isExiled(t),"Ally sheltered");
        h.runAfterDelay(85,()->{h.assertTrue(!Exile.isExiled(t)&&t.getAbsorptionAmount()>=20,"Ally returns with absorption");same(h,t.getHealth(),1000,"Shelter never harms ally");h.succeed();});
    }
    private static void seaMaster(GameTestHelper h){
        ServerPlayer p=player(h,3);cast(p,h,Spell.SEA_MASTER);same(h,Marionette.of(p).size(),3,"Three real sea-creature summons");
        var shot=h.spawn(EntityTypes.ARROW,new net.minecraft.core.BlockPos(1,2,1));Mob enemy=target(h,p,0,3);shot.setOwner(enemy);shot.setPos(p.position().add(0,3,0));h.addCleanup(ok->shot.discard());
        same(h,ThunderGodHigher.projectileFactor(shot),.4,"Sea Master slows hostile projectile travel by 60 percent");
        ThunderGodPowers.release(p);same(h,Marionette.of(p).size(),0,"Summons vanish on release");h.succeed();
    }
    private static void artifactLock(GameTestHelper h){
        ServerPlayer p=player(h,4),enemy=player(h,5);enemy.setPos(p.getX(),p.getY(),p.getZ()+3);cast(p,h,Spell.DEATH_DIRGE);
        h.assertTrue(ArtifactLock.has(enemy),"Death Dirge locks Sealed Artifacts");h.assertTrue(!Root.has(enemy,RootRestriction.USE_ITEMS)&&!Root.has(enemy,RootRestriction.ABILITIES),"Ordinary items and ability keys are not blanket-locked");h.succeed();
    }
    private static void authority(GameTestHelper h){
        ServerPlayer p=player(h,1);Mob enemy=target(h,p,0,3);float before=p.getHealth();
        Shock.strike(enemy,p,100);same(h,p.getHealth(),before,"Authority is immune to kit lightning");
        Shock.boltLanded(h.getLevel(),p.position());Shock.boltLanded(h.getLevel(),p.position());same(h,Rage.count(p),1,"Nearby bolts grant Rage only once per second");h.succeed();
    }
    private static void starfall(GameTestHelper h){
        ServerPlayer p=player(h,0);p.setXRot(90);Vec3 at=Spells.aimGround(p,h.getLevel(),160);Mob t=target(h,p,0,2);t.setPos(at.add(0,0,2));
        h.assertTrue(cast(p,h,Spell.STARFALL)==AbilityWheel.Result.DONE,"Starfall commits");same(h,t.getHealth(),1000,"Impact waits for warning");
        h.runAfterDelay(20,()->same(h,t.getHealth(),1000,"Warning remains actionable before impact"));
        h.runAfterDelay(44,()->{h.assertTrue(p.position().distanceTo(at)<1&&t.getHealth()<1000,"Caster lands unhurt and impact lands");h.succeed();});
    }
    private static void presence(GameTestHelper h){
        ServerPlayer p=player(h,0);Mob t=target(h,p,0,3);cast(p,h,Spell.TYRANTS_PRESENCE);
        h.assertTrue(Root.has(t,RootRestriction.ABILITIES),"Presence starts with a real stiffening");
        ServerPlayer parasite=player(h,5);parasite.setPos(p.getX()+4,p.getY(),p.getZ());h.assertTrue(!Parasite.canEnter(parasite,t),"Nearby bodies cannot be possessed");h.succeed();
    }
    private static void cleanup(GameTestHelper h){
        ServerPlayer p=player(h,0);cast(p,h,Spell.SEA_MASTER);cast(p,h,Spell.STORM_INCARNATION);cast(p,h,Spell.STORM_HELL);cast(p,h,Spell.CHASM_OF_STORMS);
        ThunderGodPowers.release(p);for(Spell s:new Spell[]{Spell.SEA_MASTER,Spell.STORM_INCARNATION,Spell.STORM_HELL,Spell.CHASM_OF_STORMS})h.assertTrue(!ThunderGodHigher.hasField(p,s),"Cleanup closes "+s);
        same(h,Marionette.of(p).size(),0,"Cleanup removes summons");h.assertTrue(!Dome.isRaised(p,PREFIX+"kingdom"),"Cleanup removes kingdom dome");h.succeed();
    }
    private static void lightForm(GameTestHelper h){
        ServerPlayer p=player(h,1);h.assertTrue(cast(p,h,Spell.INCARNATION_OF_LIGHT)==AbilityWheel.Result.DONE,"Light form starts");float before=p.getHealth();p.hurt(p.damageSources().generic(),10);same(h,p.getHealth(),before,"Energy body is unharmed");
        h.runAfterDelay(3,()->{ThunderGodPowers.release(p);h.assertTrue(!ThunderGodHigher.invulnerable(p)&&!Concealment.isHidden(p),"Cleanup restores a visible, vulnerable body");h.succeed();});
    }
    private static void chainOpeners(GameTestHelper h){
        ServerPlayer p=player(h,4);Mob a=target(h,p,0,3),b=target(h,p,3,3),c=target(h,p,-3,3);
        cast(p,h,Spell.CHAIN_LIGHTNING);
        h.assertTrue(1000-a.getHealth()>=60&&1000-b.getHealth()>=60&&1000-c.getHealth()>=60,"All three opening bolts land at full base damage");h.succeed();
    }
    private static void cloudGuard(GameTestHelper h){
        ServerPlayer p=player(h,3);Mob ally=target(h,p,0,3);friend(h,p,ally);Mob attacker=target(h,p,0,7);
        h.assertTrue(cast(p,h,Spell.CLOUD_SWORD)==AbilityWheel.Result.DONE,"Cloud Sword guards an ally");
        ally.hurt(attacker.damageSources().mobAttack(attacker),10);same(h,ally.getHealth(),1000,"The wall blocks a melee line crossing its front");
        attacker.setPos(p.getX(),p.getY(),p.getZ()+2);ally.invulnerableTime=0;ally.hurt(attacker.damageSources().mobAttack(attacker),10);
        same(h,ally.getHealth(),993,"Attacks from behind get the 30 percent cut, not blanket immunity");h.succeed();
    }
    private static void magnetism(GameTestHelper h){
        ServerPlayer p=player(h,2);Vec3 at=Spells.aimGround(p,h.getLevel(),30);Mob normal=target(h,p,0,3),metal=target(h,p,0,4);
        normal.setPos(at.add(3,0,0));metal.setPos(at.add(-3,0,0));metal.setItemSlot(EquipmentSlot.CHEST,net.minecraft.world.item.Items.IRON_CHESTPLATE.getDefaultInstance());
        cast(p,h,Spell.MAGNETIC_POLE_PULL);same(h,metal.getDeltaMovement().length(),normal.getDeltaMovement().length()*2,"Metal armor doubles the magnetic pull");h.succeed();
    }
    private static void recasts(GameTestHelper h){
        ServerPlayer p=player(h,0);fill(p);var spell=STORM.entry("storm_hell");h.assertTrue(STORM.cast(p,spell)==AbilityWheel.Result.DONE,"Storm Hell starts");long ready=p.getPersistentData().getLongOr(PREFIX+"storm_ready_storm_hell",0L);
        SoulCore.setSpiritualityQuietly(p,0);h.assertTrue(STORM.cast(p,spell)==AbilityWheel.Result.FREE,"Ending a storm is free");same(h,p.getPersistentData().getLongOr(PREFIX+"storm_ready_storm_hell",0L),ready,"Dismissal preserves original cooldown");
        fill(p);h.assertTrue(STORM.cast(p,spell)==AbilityWheel.Result.REFUSED,"Cannot restart through cooldown");h.succeed();
    }
}
