package net.lukario.circleofinevitability.combat;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
/** Intercept moving targets without hard-coding a pathway or projectile. Guides expire in entity game time. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class Prediction {
    public static void grant(LivingEntity e,int ticks){e.getPersistentData().putLong("spellkit_prediction",e.level().getGameTime()+ticks);}
    public static boolean active(Entity e){return e.getPersistentData().getLongOr("spellkit_prediction",0)>e.level().getGameTime();}
    public static Vec3 lead(Vec3 from,Vec3 target,Vec3 velocity,double speed){
        if(speed<=0)return target;
        Vec3 d=target.subtract(from);double a=velocity.lengthSqr()-speed*speed,b=2*d.dot(velocity),c=d.lengthSqr(),t=0;
        if(Math.abs(a)<1e-8){if(Math.abs(b)>1e-8)t=-c/b;}
        else {double disc=b*b-4*a*c;if(disc>=0){double t1=(-b-Math.sqrt(disc))/(2*a),t2=(-b+Math.sqrt(disc))/(2*a);t=t1>0 && t2>0?Math.min(t1,t2):Math.max(t1,t2);}}
        return target.add(velocity.scale(Math.clamp(t,0,100)));
    }
    public static Vec3 aim(LivingEntity caster,LivingEntity target,Vec3 from,double speed){
        Vec3 at=target.getEyePosition();return (active(caster)?lead(from,at,target.getDeltaMovement(),speed):at).subtract(from).normalize();
    }
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent public static void projectile(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel sl)
                || !(event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile shot)
                || !(shot.getOwner() instanceof LivingEntity caster) || !active(caster))return;
        Later.run(sl,1,()->{
            if(shot.isRemoved() || !caster.isAlive() || caster.level()!=sl)return;
            double speed=shot.getDeltaMovement().length();if(speed<.05)return;
            var target=Spells.aimEnemy(caster,sl,64);
            if(target!=null)shot.setDeltaMovement(aim(caster,target,shot.position(),speed).scale(speed));
        });
    }
}
