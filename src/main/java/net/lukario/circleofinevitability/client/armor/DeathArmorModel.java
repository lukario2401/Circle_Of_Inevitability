package net.lukario.circleofinevitability.client.armor;

import com.google.gson.JsonObject;
import net.lukario.circleofinevitability.item.armor.DeathArmor;
import net.lukario.circleofinevitability.item.armor.InevitabilityArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.CustomData;

/**
 * Vestments of Death: resource-pack editable rig (armor/death.rig.json, painted at twice the usual texel density).
 * Everything that moves is a named bone of the rig, animated here on top of its baked pose:
 * <ul>
 *   <li>Helmet: the hood's point sways, the soul-fire eyes flicker, and the jaw drops when the Reaping pulses.</li>
 *   <li>Mantle: five cape strips swing with the legs and flutter, the soul lantern swings at the hip, and the heart
 *       in the ribcage beats, and flares on every Reaping pulse.</li>
 *   <li>Shroud: the front and back panels are pushed by the legs, the obol swings on its chain.</li>
 * </ul>
 * The pulse is read from the worn mantle's {@link DeathArmor#REAP_KEY} (game time), so every viewer sees it.
 */
public final class DeathArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final int CAPE_STRIPS = 5;
    /** Ticks a Reaping pulse takes to show: the jaw drops, the eyes and the heart flare, then settle. */
    public static final float PULSE_TICKS = 14;
    private final EquipmentSlot slot;
    private final ModelPart hoodTip, jaw, eyeLeft, eyeRight, heart, lantern, shroudFront, shroudBack, obol;
    private final ModelPart[] cape = new ModelPart[CAPE_STRIPS];

    public DeathArmorModel(ModelPart root, EquipmentSlot slot) {
        super(root);
        this.slot = slot;
        hoodTip = head.getChild("hood_tip");
        jaw = head.getChild("jaw");
        eyeLeft = head.getChild("eye_left");
        eyeRight = head.getChild("eye_right");
        heart = body.getChild("heart");
        lantern = body.getChild("lantern");
        shroudFront = body.getChild("shroud_front");
        shroudBack = body.getChild("shroud_back");
        obol = body.getChild("obol");
        for (int i = 0; i < CAPE_STRIPS; i++) cape[i] = body.getChild("cape_" + i);
    }

    public static LayerDefinition createLayer(EquipmentSlot slot) {
        return createLayer(ArmorRig.load(InevitabilityArmor.id("armor/" + DeathArmor.SET + ".rig.json")), slot);
    }

    /** Pure rig baking entry point, also used by the standalone geometry regression test. */
    public static LayerDefinition createLayer(JsonObject rig, EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        ArmorRig.bake(rig, slot, mesh);
        // The rig's UV space; the PNG itself is painted at a higher density, which the renderer scales for free.
        var size = rig.getAsJsonArray("texture_size");
        return LayerDefinition.create(mesh, size.get(0).getAsInt(), size.get(1).getAsInt());
    }

    @Override public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft == null ? null : minecraft.level;
        long reapAt = state.chestEquipment.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getLongOr(DeathArmor.REAP_KEY, Long.MIN_VALUE / 2);
        float sincePulse = level == null ? Float.MAX_VALUE
                : level.getGameTime() - reapAt + (state.ageInTicks - (float)Math.floor(state.ageInTicks));
        animate(state, sincePulse);
    }

    /** 0..1: how strongly a Reaping pulse shows 'ticks' after it, snapping open and easing shut. */
    public static float pulse(float ticks) {
        if (ticks < 0 || ticks >= PULSE_TICKS) return 0;
        if (ticks < 2) return ticks / 2;
        float t = 1 - (ticks - 2) / (PULSE_TICKS - 2);
        return t * t * (3 - 2 * t);
    }

    /** Every moving bone, from the render state and the ticks since the last Reaping pulse. */
    public void animate(HumanoidRenderState state, float sincePulse) {
        float age = state.ageInTicks;
        float flare = pulse(sincePulse);
        float right = rightLeg.xRot, left = leftLeg.xRot;
        // Hanging things keep hanging while the body leans forward (crouching).
        float hang = -body.xRot;

        if (slot == EquipmentSlot.HEAD) {
            hoodTip.xRot += Mth.sin(age * .07f) * .05f - Math.max(0, head.xRot) * .4f;
            hoodTip.zRot += Mth.sin(age * .05f + 1) * .04f;
            jaw.xRot += flare * .42f + Math.max(0, Mth.sin(age * .9f)) * .015f;
            flicker(eyeLeft, age, 0, flare);
            flicker(eyeRight, age, 2.3f, flare);
        }
        if (slot == EquipmentSlot.CHEST) {
            // A double heartbeat every second and a half, and a flare when the Reaping pulses.
            float beat = age % 30;
            float thump = beat < 3 ? beat / 3 : beat < 6 ? (6 - beat) / 3 : beat < 8 ? (beat - 6) / 2 * .6f
                    : beat < 11 ? (11 - beat) / 3 * .6f : 0;
            float scale = 1 + thump * .12f + flare * .35f;
            heart.xScale = heart.yScale = scale;
            heart.zScale = 1 + flare * .5f;
            for (int i = 0; i < CAPE_STRIPS; i++) {
                // The outer strips follow their own leg, the middle one whichever leg swings further back.
                float leg = i < 2 ? right : i > 2 ? left : Math.max(right, left);
                float walk = Math.max(0, leg) * .85f + Math.max(0, -leg) * .15f;
                float flutter = Mth.sin(age * .11f + i * 1.7f) * .045f + Mth.sin(age * .23f + i * .9f) * .02f;
                cape[i].xRot += walk + flutter + flare * .12f + hang * .8f;
                cape[i].zRot += Mth.sin(age * .07f + i * 2.1f) * .03f;
            }
            swing(lantern, age, left, hang, .5f, 0);
        }
        if (slot == EquipmentSlot.LEGS) {
            float forward = Math.max(0, -Math.min(right, left)), back = Math.max(0, Math.max(right, left));
            shroudFront.xRot += -forward * .9f + Mth.sin(age * .09f) * .025f;
            shroudBack.xRot += back * .9f + Mth.sin(age * .09f + 1.4f) * .025f;
            swing(obol, age, right, hang, .4f, 1.9f);
        }
    }

    /** A soul-fire eye: it shivers, its flame licks taller now and then, and it flares when the Reaping pulses. */
    private static void flicker(ModelPart eye, float age, float phase, float flare) {
        float shiver = Mth.sin(age * 1.3f + phase) * .05f + Mth.sin(age * 2.9f + phase * 2) * .04f;
        eye.yScale = 1 + shiver + Math.max(0, Mth.sin(age * .21f + phase)) * .12f + flare * .45f;
        eye.xScale = 1 - shiver * .5f + flare * .25f;
    }

    /** Something hanging from a chain: pushed forward by its leg, a slow pendulum, and kept upright on a lean. */
    private static void swing(ModelPart part, float age, float leg, float hang, float push, float phase) {
        part.xRot += -leg * push + Mth.sin(age * .12f + phase) * .08f + hang;
        part.zRot += Mth.sin(age * .09f + phase) * .07f;
    }

    public ModelPart cape(int i) { return cape[i]; }
    public ModelPart hoodTip() { return hoodTip; }
    public ModelPart jaw() { return jaw; }
    public ModelPart heart() { return heart; }
    public ModelPart lantern() { return lantern; }
    public ModelPart shroudFront() { return shroudFront; }
    public ModelPart shroudBack() { return shroudBack; }
}
