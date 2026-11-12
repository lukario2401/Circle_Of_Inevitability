package net.lukario.circleofinevitability.status;

import java.util.*;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Source-owned, three-level mutations. No pathway, model or sound dependency.
 * hit accumulates mutagen; apply adds levels directly. Call clearSource on owner teardown.
 * Listener callbacks let a pathway supply presentation without copying the status logic. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class Mutation {
    public enum Kind { ORGANS, FUNGUS, WHEAT, VINES }
    public interface Listener {
        default void changed(LivingEntity source,LivingEntity target,Kind kind,int level,int ticks) {}
        default void ended(LivingEntity source,LivingEntity target) {}
    }
    private record Key(UUID source,UUID target) {}
    private static final class Change {
        final LivingEntity source,target;Kind kind;int level,mutagen;long until;
        Change(LivingEntity source,LivingEntity target,Kind kind) { this.source=source;this.target=target;this.kind=kind; }
        String tag() { return "mutation_"+source.getUUID(); }
    }
    private static final Map<Key,Change> ACTIVE=new HashMap<>();
    private static final List<Listener> LISTENERS=new ArrayList<>();
    static { Cleansing.addListener(Mutation::clear); }
    private Mutation() {}
    public static void addListener(Listener listener) { LISTENERS.add(Objects.requireNonNull(listener)); }
    private static Key key(LivingEntity s,LivingEntity t) { return new Key(s.getUUID(),t.getUUID()); }
    private static boolean allowed(LivingEntity s,LivingEntity t) {
        return s.level() instanceof ServerLevel&&s.level()==t.level()&&s.isAlive()&&t.isAlive()
                &&Spells.isEnemy(s,t)&&Spells.canBeChosen(s,t)
                &&(!(t instanceof Player p)||!p.isCreative()&&!p.isSpectator())
                &&(!(s instanceof Player p&&t instanceof Player q)||p.canHarmPlayer(q));
    }
    public static int level(LivingEntity source,LivingEntity target) {
        Change c=ACTIVE.get(key(source,target));return c==null||c.until<=target.level().getGameTime()?0:c.level;
    }
    public static Kind kind(LivingEntity source,LivingEntity target) {
        Change c=ACTIVE.get(key(source,target));return level(source,target)==0?null:c.kind;
    }
    public static int hit(LivingEntity source,LivingEntity target,Kind kind,int amount) {
        if(amount<=0||!allowed(source,target))return level(source,target);
        Change c=ACTIVE.computeIfAbsent(key(source,target),k->new Change(source,target,kind));
        if(c.until<=target.level().getGameTime()) { c.level=0;c.mutagen=0; }
        c.until=target.level().getGameTime()+240;c.mutagen+=amount;
        int threshold=Spells.isBoss(target)?6:3;
        if(c.mutagen>=threshold) { c.mutagen%=threshold;return apply(source,target,kind,1,240); }
        return c.level;
    }
    public static int apply(LivingEntity source,LivingEntity target,Kind kind,int levels,int ticks) {
        if(levels<=0||ticks<=0||!allowed(source,target))return level(source,target);
        Change c=ACTIVE.computeIfAbsent(key(source,target),k->new Change(source,target,kind));
        if(c.until<=target.level().getGameTime())c.level=0;
        if(c.kind!=kind)Falter.remove(target,c.tag());
        c.kind=kind;c.level=Math.min(3,c.level+levels);c.until=target.level().getGameTime()+ticks;
        pulse(c);for(var listener:List.copyOf(LISTENERS))listener.changed(source,target,kind,c.level,ticks);
        return c.level;
    }
    /** Only a fully mutated enemy blooms; normal damage still respects combat defenses. */
    public static boolean bloom(LivingEntity source,LivingEntity target,float cap) {
        if(level(source,target)<3||!allowed(source,target))return false;
        Spells.strike(source,target,Math.min(Math.max(0,cap),target.getMaxHealth()*.15f));
        for(var e:Spells.enemiesAround(source,(ServerLevel)source.level(),target.position(),4))
            if(allowed(source,e))Root.applyOwned(e,"mutation_bloom_"+source.getUUID(),40,EnumSet.of(RootRestriction.MOVE,RootRestriction.JUMP));
        return true;
    }
    public static void clear(LivingEntity target) {
        for(var c:new ArrayList<>(ACTIVE.values()))if(c.target==target)remove(c);
    }
    public static void clearSource(LivingEntity source) {
        for(var c:new ArrayList<>(ACTIVE.values()))if(c.source==source)remove(c);
    }
    private static void remove(Change c) {
        ACTIVE.remove(key(c.source,c.target));Falter.remove(c.target,c.tag());Root.removeOwned(c.target,c.tag());
        for(var listener:List.copyOf(LISTENERS))listener.ended(c.source,c.target);
    }
    private static void pulse(Change c) {
        if(c.level==0)return;
        switch(c.kind) {
            case ORGANS -> {} // Hurt handler applies a single strongest modifier, never multiplicative owners.
            case FUNGUS -> Spells.keepEffect(c.target,MobEffects.WEAKNESS,0,25);
            case WHEAT -> Spells.keepEffect(c.target,MobEffects.SLOWNESS,c.level-1,25);
            case VINES -> {
                Falter.apply(c.target,c.tag(),.25f,25);
                if(c.target.level().getGameTime()%60==0)Root.applyOwned(c.target,c.tag(),10,EnumSet.of(RootRestriction.MOVE));
            }
        }
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent.Post event) {
        if(!(event.level() instanceof ServerLevel sl))return;
        for(var c:new ArrayList<>(ACTIVE.values()))if(c.target.level()==sl) {
            if(c.until<=sl.getGameTime()||c.target.isRemoved()||c.source.isRemoved()||!allowed(c.source,c.target))remove(c);
            else if(sl.getGameTime()%20==0)pulse(c);
        }
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        int strongest=0;
        for(var c:ACTIVE.values())if(c.target==event.getEntity()&&c.kind==Kind.ORGANS&&c.until>c.target.level().getGameTime())strongest=Math.max(strongest,c.level);
        if(strongest>0)event.setAmount(event.getAmount()*(1+.15f*strongest));
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        for(var c:new ArrayList<>(ACTIVE.values()))if(c.target==event.getEntity()) {
            if(c.kind==Kind.FUNGUS&&c.level>0&&c.source.isAlive()&&c.source.level()==c.target.level())
                for(var e:Spells.enemiesAround(c.source,(ServerLevel)c.source.level(),c.target.position(),4))if(allowed(c.source,e))Madness.add(c.source,e,1);
            remove(c);
        }
        clearSource(event.getEntity());
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        for(var c:new ArrayList<>(ACTIVE.values()))if(c.target.level()==event.getLevel())remove(c);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { ACTIVE.clear(); }
}
