package net.lukario.circleofinevitability.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/** Source-aware aid. Healing, buffs and shields can share restrictions without changing team membership. */
public final class Support {
    private static final List<BiPredicate<LivingEntity,LivingEntity>> BLOCKERS=new ArrayList<>();
    private Support(){}
    public static void addBlocker(BiPredicate<LivingEntity,LivingEntity> blocker){BLOCKERS.add(blocker);}
    public static boolean blocked(LivingEntity provider,LivingEntity receiver){
        if(provider==null||receiver==null||provider==receiver)return false;
        for(var rule:BLOCKERS)if(rule.test(provider,receiver))return true;
        return false;
    }
    public static void heal(LivingEntity provider,LivingEntity receiver,float amount){if(!blocked(provider,receiver))receiver.heal(amount);}
    public static boolean effect(LivingEntity provider,LivingEntity receiver,MobEffectInstance effect){
        return !blocked(provider,receiver)&&receiver.addEffect(effect,provider);
    }
}
