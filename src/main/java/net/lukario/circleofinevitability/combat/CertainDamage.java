package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.status.DeathWard;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Unavoidable, owner-credited damage. Only the explicitly captured prior death ward may undo it.
 * Substitutes are checked by the caller before pronouncing the curse; null ward means irreversible damage.
 * Creative/spectator players remain protected. Callers supply their final damage, including their own bonuses.
 */
public final class CertainDamage {
    public static final ResourceKey<DamageType> TYPE=ResourceKey.create(Registries.DAMAGE_TYPE,Identifier.fromNamespaceAndPath("circle_of_inevitability","inevitable"));
    private CertainDamage(){}
    public static void strike(LivingEntity caster,LivingEntity target,float amount,Object priorWard){
        if(!(target.level() instanceof ServerLevel sl)||caster.level()!=sl||!target.isAlive()||amount<=0)return;
        if(target instanceof Player p&&(p.isSpectator()||p.getAbilities().invulnerable))return;
        DamageSource source=new DamageSource(sl.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(TYPE),caster);
        target.stopUsingItem();target.invulnerableTime=0;DamageFx.credit(caster,target);
        DeathWard.withSnapshot(target,priorWard,()->target.hurt(source,amount));
        caster.setLastHurtMob(target);
    }
}
