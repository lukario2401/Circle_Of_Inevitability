package net.lukario.circleofinevitability.status;
import java.util.*;
import java.util.function.*;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shared, decaying 0..10 mental strain. Threshold effects and break callbacks work for players and mobs. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class Madness {
    private static final String NAME="spellkit_madness";
    private static final List<Predicate<LivingEntity>> IMMUNE=new ArrayList<>();
    private static final List<BiConsumer<LivingEntity,LivingEntity>> BREAKS=new ArrayList<>();
    public static int get(LivingEntity e){return Marks.count(e,e,NAME);}
    public static void clear(LivingEntity e){Marks.clear(e,e,NAME);Falter.remove(e,NAME);}
    public static void addImmunity(Predicate<LivingEntity> rule){IMMUNE.add(rule);}
    public static void onBreak(BiConsumer<LivingEntity,LivingEntity> rule){BREAKS.add(rule);}
    public static int add(LivingEntity source,LivingEntity target,int amount){
        if(amount<=0 || target.level().isClientSide() || IMMUNE.stream().anyMatch(r->r.test(target)))return get(target);
        int n=Marks.add(target,target,NAME,amount,10,100);
        target.getPersistentData().putString("spellkit_madness_source",source.getUUID().toString());
        if(n>=10){
            Spells.strike(source,target,target.getMaxHealth()*.05f);
            Root.apply(target,40,RootRestriction.MOVE,RootRestriction.ABILITIES,RootRestriction.ATTACK);
            Marks.clear(target,target,NAME);Marks.add(target,target,NAME,3,10,100);
            for(var callback:List.copyOf(BREAKS))callback.accept(source,target);
        }
        effects(target,source);return get(target);
    }
    private static void effects(LivingEntity e,LivingEntity source){
        int n=get(e);
        if(n>=3)Spells.keepEffect(e,MobEffects.NAUSEA,0,60);
        if(n>=6)Falter.apply(e,NAME,.5f,25);else Falter.remove(e,NAME);
        if(n>=9 && source!=e){
            Fear.apply(e,source,25);
            if(e instanceof Mob mob){
                var candidates=e.level().getEntitiesOfClass(LivingEntity.class,e.getBoundingBox().inflate(8),other->other!=e && other.isAlive() && other.isAlliedTo(e));
                if(!candidates.isEmpty())Taunt.apply(mob,candidates.getFirst(),25);
            }
        }
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        var e=event.getEntity();if(!e.level().isClientSide() && e.tickCount%20==0 && get(e)>0){
            LivingEntity source=e;
            try{var found=Connection.find(e.level().getServer(),UUID.fromString(e.getPersistentData().getStringOr("spellkit_madness_source","")));if(found!=null)source=found;}
            catch(IllegalArgumentException ignored){}
            effects(e,source);
        }
    }
}
