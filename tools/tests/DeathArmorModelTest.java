package net.lukario.circleofinevitability.client.armor;

import com.google.gson.JsonParser;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import java.nio.file.Files;
import java.nio.file.Path;

/** Death rig baking, slot isolation, the texture's UV space, and the cape, jaw, heart, lantern and shroud animations. */
public final class DeathArmorModelTest {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var rig=JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/circle_of_inevitability/armor/death.rig.json"))).getAsJsonObject();
        var size=rig.getAsJsonArray("texture_size");
        int texW=size.get(0).getAsInt(),texH=size.get(1).getAsInt();
        for(var element:rig.getAsJsonArray("bones"))for(var c:element.getAsJsonObject().getAsJsonArray("cubes")){
            var cube=c.getAsJsonObject();var uv=cube.getAsJsonArray("uv");var s=cube.getAsJsonArray("size");
            float w=s.get(0).getAsFloat(),h=s.get(1).getAsFloat(),d=s.get(2).getAsFloat();
            check(uv.get(0).getAsInt()+2*(w+d)<=texW && uv.get(1).getAsInt()+d+h<=texH,"Every cube's UV island fits the atlas");
        }
        var png=Path.of("src/main/resources/assets/circle_of_inevitability/textures/entity/equipment/humanoid/death.png");
        byte[] header=Files.readAllBytes(png);
        int pngW=((header[16]&255)<<24)|((header[17]&255)<<16)|((header[18]&255)<<8)|(header[19]&255);
        check(pngW%texW==0 && pngW>=texW,"The painted texture is a whole multiple of the rig's UV space");

        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
            var model=new DeathArmorModel(DeathArmorModel.createLayer(rig,slot).bakeRoot(),slot);
            check(model.jaw().isEmpty()==(slot!=EquipmentSlot.HEAD),"Only the visage has a jaw");
            check(model.heart().isEmpty()==(slot!=EquipmentSlot.CHEST),"Only the mantle carries the heart");
            check(model.cape(0).isEmpty()==(slot!=EquipmentSlot.CHEST),"Only the mantle has a cape");
            check(model.shroudFront().isEmpty()==(slot!=EquipmentSlot.LEGS),"Only the shroud has its panels");
            check(!model.leftArm.isEmpty()==(slot==EquipmentSlot.CHEST),"Only the mantle has arm cubes");
            check(!model.leftLeg.isEmpty()==(slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET),"Leg/boot slot isolation");
            var state=new HumanoidRenderState();state.scale=1;state.ageScale=1;state.ageInTicks=40;
            model.setupAnim(state);
            for(int tick=0;tick<400;tick++){
                state.ageInTicks=tick;model.setupAnim(state);model.animate(state,tick%40);
                for(int i=0;i<DeathArmorModel.CAPE_STRIPS;i++)
                    check(Float.isFinite(model.cape(i).xRot) && Math.abs(model.cape(i).xRot)<1.6f,"Cape stays finite and behind");
                check(model.heart().xScale>.99f && model.heart().xScale<1.5f,"Heart beats within bounds");
            }
            if(slot==EquipmentSlot.HEAD){
                state.ageInTicks=0;model.setupAnim(state);model.animate(state,1000);float rest=model.jaw().xRot;
                model.setupAnim(state);model.animate(state,2);
                check(model.jaw().xRot>rest+.3f,"The jaw drops when the Reaping pulses");
                model.setupAnim(state);model.animate(state,DeathArmorModel.PULSE_TICKS+1);
                check(Math.abs(model.jaw().xRot-rest)<.02f,"And shuts again");
            }
            if(slot==EquipmentSlot.CHEST){
                state.ageInTicks=0;model.setupAnim(state);model.animate(state,1000);float still=model.cape(0).xRot;
                model.setupAnim(state);model.rightLeg.xRot=.9f;model.leftLeg.xRot=-.9f;model.animate(state,1000);
                check(model.cape(0).xRot>still+.6f && model.cape(4).xRot<still+.3f,"A cape strip is pushed back only by its own leg");
            }
            if(slot==EquipmentSlot.LEGS){
                state.ageInTicks=0;model.setupAnim(state);model.animate(state,1000);float front=model.shroudFront().xRot;
                model.setupAnim(state);model.rightLeg.xRot=-.8f;model.animate(state,1000);
                check(model.shroudFront().xRot<front-.5f,"A leg swinging forward pushes the front panel forward");
            }
        }
        check(DeathArmorModel.pulse(-1)==0 && DeathArmorModel.pulse(2)==1 && DeathArmorModel.pulse(DeathArmorModel.PULSE_TICKS)==0,"Pulse envelope");
        System.out.println("Death armor slot geometry, atlas and animations passed.");
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
}
