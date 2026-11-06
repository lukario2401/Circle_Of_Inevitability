package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.screen.*;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Short-lived visuals: no model or HUD can outlive its server refresh by more than ten ticks. */
final class AuthorFx {
    private AuthorFx() {}

    static ModelFx model(String name, int ticks) {
        return ModelFx.of("author/" + name).pivot(8, 8, 8).glow().unshaded()
                .lifetime(ticks).fade(2, 3);
    }

    static void above(ServerLevel sl, LivingEntity target, String name) {
        ParticleShapes.modelOn(sl, model(name, 10).scale(.55f), target,
                new Vec3(0, target.getBbHeight() + .35, 0), 0, 0, 0);
    }

    static void cast(ServerPlayer p, LivingEntity target, String sound, String model) {
        sound(p, sound);
        above(p.level(), target == null ? p : target, model);
        ParticleShapes.ring(p.level(), ParticleFx.of("fx/author/thought").color(0xFFE3BA5F)
                .size(.12f).lifetime(12).glow(), (target == null ? p : target).position().add(0, 1, 0), .8, 10, 0);
    }

    static void sound(ServerPlayer p, String name) {
        Spells.sound(p.level(), p.position(), ModSounds.AUTHOR.get(name).get(), .65f, 1f);
    }

    static void maze(ServerLevel sl, Vec3 at, double radius, boolean warning) {
        ParticleShapes.model(sl, model("mind_ripple", 10).scale((float)(radius * 16 / 10))
                .color(warning ? 0xFFE3BA5F : 0xFF977DD4), at.add(0, .1, 0), 0, 0, 0);
        if (!warning) for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2;
            ParticleShapes.model(sl, model("dream_gate", 10).pivot(8, 0, 8).scale(1.4f),
                    at.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), i * 90, 0, 0);
        }
    }

    static void hud(ServerPlayer p, LivingEntity target, int insight) {
        String strain = net.lukario.circleofinevitability.status.Form.isIn(p, AuthorAdvanced.DRAGON)
                ? " · Strain " + Math.round(p.getPersistentData().getFloatOr(AuthorAdvanced.STRAIN, 0f)) + "/100" : "";
        if (target == null) {
            ScreenImages.hide(p, "author_insight");
            if (!strain.isEmpty()) p.sendOverlayMessage(Component.literal(strain.substring(3)));
            return;
        }
        ScreenImages.show(p, "author_insight", "author/insight_" + insight,
                ScreenAnchor.BOTTOM, 0, -62, 64, 16, 10);
        String status = target instanceof Mob m && m.getTarget() != null ? " · Aggressive" : "";
        if (AuthorMind.sleeping(target)) status += " · Asleep";
        if (AuthorMind.resolved(target)) status += " · Resolve";
        p.sendOverlayMessage(Component.literal(target.getName().getString() + " "
                + Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth())
                + " · Insight " + insight + "/5" + (insight >= 3 ? " · Exposed" : "") + status + strain));
    }
}
