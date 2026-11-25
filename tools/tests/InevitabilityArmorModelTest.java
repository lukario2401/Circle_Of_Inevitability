package net.lukario.circleofinevitability.client.armor;

import com.google.gson.JsonParser;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Vector3f;
import java.nio.file.Files;
import java.nio.file.Path;

/** Native model baking, slot isolation, finite idle animation and precise strike endpoint regression. */
public final class InevitabilityArmorModelTest {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var rig=JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/circle_of_inevitability/armor/inevitability.rig.json"))).getAsJsonObject();
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
            var root=InevitabilityArmorModel.createLayer(rig,slot).bakeRoot();
            var model=new InevitabilityArmorModel(root,slot);
            check(!model.head.isEmpty()==(slot==EquipmentSlot.HEAD),"Only helmet has head cubes");
            check(!model.leftArm.isEmpty()==(slot==EquipmentSlot.CHEST),"Only chestplate has arm cubes");
            check(!model.leftLeg.isEmpty()==(slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET),"Leg/boot slot isolation");
            if(slot!=EquipmentSlot.CHEST)continue;
            var state=new HumanoidRenderState();state.scale=1;state.ageScale=1;
            var strike=new CompoundTag();strike.putString("dimension","minecraft:overworld");
            strike.putLong("started",100);strike.putInt("duration",12);strike.putInt("windup",6);
            strike.putDouble("x",0);strike.putDouble("y",1);strike.putDouble("z",3);
            for(int tick=0;tick<100;tick++) {
                state.ageInTicks=tick;
                model.animateTentacles(state,new CompoundTag(),tick,"minecraft:overworld");
                for(int i=0;i<4;i++)for(int j=0;j<12;j++) {
                    var part=model.body.getChild("tendril_"+i+"_"+j);
                    check(Float.isFinite(part.xRot)&&Float.isFinite(part.yRot)&&part.yScale>0,"Finite idle segment transforms");
                }
            }
            state.ageInTicks=106;
            model.animateTentacles(state,strike,106,"minecraft:overworld");
            for(int i=0;i<4;i++) {
                var tip=model.body.getChild("tendril_"+i+"_11");
                var end=new Vector3f(0,(tip.yScale-.035f)*3,0).rotateX(tip.xRot).rotateY(tip.yRot).add(tip.x,tip.y,tip.z);
                check(end.distance(new Vector3f(0,8,-48))<.01f,"All tentacles reach authoritative target at impact");
            }
            model.animateTentacles(state,strike,113,"minecraft:overworld");
            check(Math.abs(model.body.getChild("tendril_0_11").z)<25,"Expired strike returns to idle");
            model.animateTentacles(state,strike,106,"minecraft:the_nether");
            check(Math.abs(model.body.getChild("tendril_0_11").z)<25,"Other-dimension snapshot is ignored");
        }
        System.out.println("Armor slot geometry, native model baking, idle animation and strike endpoints passed.");
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
}
