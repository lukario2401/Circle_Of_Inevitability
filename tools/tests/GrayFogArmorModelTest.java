package net.lukario.circleofinevitability.client.armor;

import com.google.gson.JsonParser;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import java.nio.file.Files;
import java.nio.file.Path;

/** Gray Fog rig baking, slot isolation, coat tails following the legs, and the figurine's idle and recharge states. */
public final class GrayFogArmorModelTest {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var rig=JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/circle_of_inevitability/armor/gray_fog.rig.json"))).getAsJsonObject();
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
            var model=new GrayFogArmorModel(GrayFogArmorModel.createLayer(rig,slot).bakeRoot(),slot);
            check(!model.head.isEmpty()==(slot==EquipmentSlot.HEAD),"Only the hat has head cubes");
            check(!model.leftArm.isEmpty()==(slot==EquipmentSlot.CHEST),"Only the coat has arm cubes");
            check(!model.leftLeg.isEmpty()==(slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET),"Leg/shoe slot isolation");
            check(model.body.getChild("coat_tail_left").isEmpty()==(slot!=EquipmentSlot.CHEST),"Coat tails belong to the coat");
            check(model.body.hasChild("figurine")==(slot==EquipmentSlot.CHEST),"Only the coat carries the figurine");
            var state=new HumanoidRenderState();state.scale=1;state.ageScale=1;state.ageInTicks=40;
            model.setupAnim(state);
            if(slot!=EquipmentSlot.CHEST)continue;
            var right=model.body.getChild("coat_tail_right");var left=model.body.getChild("coat_tail_left");
            model.rightLeg.xRot=.9f;model.leftLeg.xRot=-.9f;model.swayTails(state);
            check(right.xRot>.7f && left.xRot<.2f && left.xRot>.05f,"A tail is pushed back only by its own leg swinging back");
            var figurine=model.body.getChild("figurine");
            for(int tick=0;tick<200;tick++) {
                state.ageInTicks=tick;model.animateFigurine(state,true);
                check(figurine.visible && Float.isFinite(figurine.yRot),"Ready figurine is drawn");
                check(Math.abs(figurine.y-GrayFogArmorModel.FIGURINE_Y)<=.81f && figurine.x==GrayFogArmorModel.FIGURINE_X,"Figurine bobs at the shoulder");
            }
            model.animateFigurine(state,false);
            check(!figurine.visible,"Recharging figurine is hidden");
        }
        System.out.println("Gray Fog armor slot geometry, coat tails and figurine passed.");
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
}
