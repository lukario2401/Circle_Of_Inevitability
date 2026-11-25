package net.lukario.circleofinevitability.client.armor;

import com.google.gson.JsonParser;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Vector3f;
import java.nio.file.Files;
import java.nio.file.Path;

/** Unshadowed rig baking, slot isolation, halo ownership, finite orbit and exact spear-tip strike endpoints. */
public final class UnshadowedArmorModelTest {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var rig=JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/circle_of_inevitability/armor/unshadowed.rig.json"))).getAsJsonObject();
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
            var model=new UnshadowedArmorModel(UnshadowedArmorModel.createLayer(rig,slot).bakeRoot(),slot);
            check(!model.head.isEmpty()==(slot==EquipmentSlot.HEAD),"Only helmet has head and halo cubes");
            check(!model.leftArm.isEmpty()==(slot==EquipmentSlot.CHEST),"Only chestplate has arm cubes");
            check(!model.leftLeg.isEmpty()==(slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET),"Leg/boot slot isolation");
            var state=new HumanoidRenderState();state.scale=1;state.ageScale=1;state.ageInTicks=40;
            model.setupAnim(state);
            check(Math.abs(model.head.getChild("halo").zRot-.8f)<1e-4f,"Halo turns with age");
            if(slot!=EquipmentSlot.CHEST)continue;
            for(int tick=0;tick<200;tick++) {
                state.ageInTicks=tick;
                model.animateSpears(state,new CompoundTag(),tick,"minecraft:overworld");
                for(int i=0;i<UnshadowedArmorModel.SPEARS;i++) {
                    var part=model.body.getChild("spear_"+i);
                    check(Float.isFinite(part.xRot)&&Float.isFinite(part.yRot)&&Math.hypot(part.x,part.z)<16,"Finite idle orbit");
                    check(tip(part).y<part.y,"Idle spears stand tip-up");
                }
            }
            var strike=new CompoundTag();strike.putString("dimension","minecraft:overworld");
            strike.putLong("started",100);strike.putInt("duration",16);strike.putInt("windup",8);
            strike.putDouble("x",0);strike.putDouble("y",1);strike.putDouble("z",3);
            state.ageInTicks=108;
            model.animateSpears(state,strike,108,"minecraft:overworld");
            for(int i=0;i<UnshadowedArmorModel.SPEARS;i++)
                check(tip(model.body.getChild("spear_"+i)).distance(new Vector3f(0,8,-48))<.01f,"Every spear tip reaches the strike point at impact");
            model.animateSpears(state,strike,117,"minecraft:overworld");
            check(Math.abs(model.body.getChild("spear_0").z)<16,"Expired strike returns to orbit");
            model.animateSpears(state,strike,108,"minecraft:the_nether");
            check(Math.abs(model.body.getChild("spear_0").z)<16,"Other-dimension snapshot is ignored");
        }
        System.out.println("Unshadowed armor slot geometry, halo, spear orbit and strike endpoints passed.");
    }
    private static Vector3f tip(net.minecraft.client.model.geom.ModelPart part) {
        return new Vector3f(0,UnshadowedArmorModel.SPEAR_LENGTH,0).rotateX(part.xRot).rotateY(part.yRot).add(part.x,part.y,part.z);
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
}
