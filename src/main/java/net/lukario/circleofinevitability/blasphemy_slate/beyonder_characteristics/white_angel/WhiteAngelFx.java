package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
/** Cosmetic adapter for the authored White Angel resources. */
public final class WhiteAngelFx {
    private static ModelFx model(String name,int ticks){return ModelFx.of("white_angel/"+name).pivot(8,8,8).glow().unshaded().lifetime(ticks).fade(2,4);}
    public static void at(ServerLevel sl,Vec3 at,String name,int ticks){ParticleShapes.model(sl,model(name,ticks),at,0,0,0);}
    public static void above(ServerLevel sl,LivingEntity e,String name,int ticks){ParticleShapes.modelOn(sl,model(name,ticks),e,new Vec3(0,e.getBbHeight()+.4,0),0,0,0);}
    public static void ring(ServerLevel sl,Vec3 at,String name,double radius,int ticks){ParticleShapes.model(sl,model(name,ticks).scale((float)(radius*16/(name.equals("warning_sigil")?11:name.equals("domain_boundary")?14:7))),at,0,0,0);}
    public static void sound(ServerPlayer p,String name){Spells.sound(p.level(),p.position(),ModSounds.WHITE_ANGEL.get(name).get(),.7f,1);}
}
