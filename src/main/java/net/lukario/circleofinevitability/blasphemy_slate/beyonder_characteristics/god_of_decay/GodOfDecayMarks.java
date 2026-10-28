package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Echo;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.*;

/** Owner-scoped persistent marks. LATENT is invisible to has/count/FX and resists cleansing. */
public final class GodOfDecayMarks {
    public enum Kind {
        INFECTED(true), LAZY(true), OBESE(true), DEGENERATE(true), LATENT(false), ROTTING(true),
        CORRUPTED(true), HOLLOWED(false), DOOMED(false), AGED(true), SEGMENTED(false), FATED(false), RED_TAPE(false);
        final boolean sickness;
        Kind(boolean sickness) { this.sickness=sickness; }
    }
    static final String DATA = "god_of_decay_marks";
    public record Mark(String key, UUID owner, Kind kind, CompoundTag data) {}
    private GodOfDecayMarks() {}
    static String key(LivingEntity owner, Kind kind) { return owner.getUUID()+"/"+kind.name(); }
    static boolean alive(LivingEntity target, CompoundTag data) { return data.getLongOr("until",0L)>target.level().getGameTime(); }
    static List<Mark> all(LivingEntity target) {
        List<Mark> marks = new ArrayList<>();
        CompoundTag root = target.getPersistentData().getCompoundOrEmpty(DATA);
        for (String key : root.keySet()) {
            String[] pieces=key.split("/",2);
            if (pieces.length!=2) continue;
            try { marks.add(new Mark(key,UUID.fromString(pieces[0]),Kind.valueOf(pieces[1]),root.getCompoundOrEmpty(key))); }
            catch (IllegalArgumentException ignored) { }
        }
        return marks;
    }
    public static int put(LivingEntity owner, LivingEntity target, Kind kind, int ticks) { return put(owner,target,kind,ticks,1); }
    static int put(LivingEntity owner, LivingEntity target, Kind kind, int ticks, float potency) {
        return put(owner,target,kind,ticks,potency,false);
    }
    /** Decay's Sequence 1 upgrade counts Aged without also casting Senescence. */
    static void countedAge(LivingEntity owner,LivingEntity target,int ticks) {
        CompoundTag prior=target.getPersistentData().getCompoundOrEmpty(DATA).getCompoundOrEmpty(key(owner,Kind.AGED));
        if(alive(target,prior)&&!prior.getBooleanOr("count_only",false))return;
        put(owner,target,Kind.AGED,ticks,1,true);
    }
    private static int put(LivingEntity owner, LivingEntity target, Kind kind, int ticks, float potency,boolean countOnly) {
        if (!(target.level() instanceof ServerLevel sl) || owner.level()!=sl || !target.isAlive() || ticks<=0) return 0;
        if (owner!=target && Spells.isFriend(owner,target)) return 0;
        String key=key(owner,kind);
        CompoundTag root=target.getPersistentData().getCompoundOrEmpty(DATA);
        CompoundTag previous=root.getCompoundOrEmpty(key);
        long now=sl.getGameTime();
        CompoundTag data=new CompoundTag();
        data.putLong("until",Math.max(now+ticks,previous.getLongOr("until",0L)));
        data.putLong("start",alive(target,previous)?previous.getLongOr("start",now):now);
        data.putLong("pulse",alive(target,previous)?previous.getLongOr("pulse",now+20):now+20);
        data.putLong("hop",alive(target,previous)?previous.getLongOr("hop",now+80):now+80);
        data.putFloat("power",Math.max(potency*Echo.strength(owner),previous.getFloatOr("power",0f)));
        data.putFloat("health",alive(target,previous)?previous.getFloatOr("health",target.getMaxHealth()):target.getMaxHealth());
        data.putBoolean("count_only",countOnly);
        data.putBoolean("irreversible",GodOfDecay.is(owner,0));
        data.putBoolean("contagious",GodOfDecay.is(owner,6));
        data.putBoolean("advanced",GodOfDecay.is(owner,3));
        if (kind==Kind.INFECTED && alive(target,previous)) data.putLong("until",Math.min(now+400,previous.getLongOr("until",now)+ticks));
        if (kind==Kind.DOOMED) root.remove(key(owner,Kind.FATED));
        if (kind==Kind.FATED) { root.remove(key(owner,Kind.DOOMED)); GodOfDecayPowers.forgetDoom(owner.getUUID(),target); }
        root.put(key,data); target.getPersistentData().put(DATA,root);
        if (kind!=Kind.LATENT && kind!=Kind.FATED && kind!=Kind.RED_TAPE) {
            GodOfDecayFx.end(sl,target,target.position(),tag(target,key));
            GodOfDecayFx.disease(sl,target,GodOfDecayFx.Disease.valueOf(kind.name()),ticks,tag(target,key));
        }
        refreshBody(target);
        return ticks;
    }
    public static boolean has(LivingEntity target, Kind kind) {
        if (kind==Kind.LATENT) return false;
        return all(target).stream().anyMatch(m -> m.kind==kind && alive(target,m.data));
    }
    public static boolean from(LivingEntity target, Kind kind, LivingEntity owner) {
        return kind!=Kind.LATENT && alive(target,target.getPersistentData().getCompoundOrEmpty(DATA).getCompoundOrEmpty(key(owner,kind)));
    }
    public static int count(LivingEntity owner, LivingEntity target) {
        int count=(int)all(target).stream().filter(m -> m.owner.equals(owner.getUUID()) && m.kind.sickness && alive(target,m.data)).count();
        if (GodOfDecayPowers.inEnemyDomain(target,GodOfDecayFx.Ability.ENTROPY)) count*=2;
        return Math.min(6,count);
    }
    static float strength(LivingEntity target, Kind kind) {
        return (float)all(target).stream().filter(m -> m.kind==kind && alive(target,m.data)).mapToDouble(m->m.data.getFloatOr("power",1f)).max().orElse(0);
    }
    static LivingEntity owner(ServerLevel sl, Mark mark) { return sl.getEntity(mark.owner) instanceof LivingEntity e && e.isAlive()?e:null; }
    static String tag(LivingEntity target,String key) { return "decay_mark_"+target.getUUID()+"_"+key; }
    static void remove(LivingEntity target, Mark mark) {
        if(mark.kind==Kind.DOOMED) GodOfDecayPowers.forgetDoom(mark.owner,target);
        CompoundTag root=target.getPersistentData().getCompoundOrEmpty(DATA); root.remove(mark.key);
        if (root.isEmpty()) target.getPersistentData().remove(DATA); else target.getPersistentData().put(DATA,root);
        if (target.level() instanceof ServerLevel sl) GodOfDecayFx.end(sl,target,target.position(),tag(target,mark.key));
    }
    static void end(LivingEntity owner,LivingEntity target,Kind kind) {
        for (Mark m:all(target)) if (m.kind==kind && m.owner.equals(owner.getUUID())) remove(target,m);
        refreshBody(target);
    }
    public static boolean cleanse(LivingEntity target) {
        boolean changed=false;
        for(Mark m:all(target)) if(m.kind!=Kind.LATENT && !m.data.getBooleanOr("irreversible",false)) { remove(target,m); changed=true; }
        refreshBody(target); return changed;
    }
    static void clear(LivingEntity target) { for(Mark m:all(target)) remove(target,m); refreshBody(target); }
    static void age(LivingEntity target,int extraTicks) {
        for(Mark m:all(target)) if(m.kind!=Kind.LATENT && !m.data.getBooleanOr("irreversible",false)) m.data.putLong("until",m.data.getLongOr("until",0L)-extraTicks);
    }
    static void refreshBody(LivingEntity target) {
        float depth=0;
        long now=target.level().getGameTime();
        for(Mark m:all(target)) if(alive(target,m.data)) {
            float seconds=(now-m.data.getLongOr("start",now))/20f;
            if(m.kind==Kind.DEGENERATE) depth=Math.max(depth,Math.min(.75f,seconds*.02f*m.data.getFloatOr("power",1f)));
            if(m.kind==Kind.AGED && !m.data.getBooleanOr("count_only",false)) depth=Math.max(depth,Math.min(.75f,seconds*.05f*m.data.getFloatOr("power",1f)));
        }
        Physique.keep(target,Attributes.MAX_HEALTH,Physique.id("god_decay_health"),-depth,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,depth>0);
        float slow=Math.max(strength(target,Kind.LAZY)*.5f,has(target,Kind.AGED)?.4f:0);
        Physique.keep(target,Attributes.ATTACK_SPEED,Physique.id("god_decay_attack_speed"),-Math.min(.9f,slow),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,slow>0);
        Physique.keep(target,Attributes.ARMOR,Physique.id("god_decay_hollow"),-.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,has(target,Kind.HOLLOWED));
        if(target.getHealth()>target.getMaxHealth()) target.setHealth(target.getMaxHealth());
    }
    static void tick(LivingEntity target,ServerLevel sl) {
        long now=sl.getGameTime();
        for(Mark m:all(target)) {
            LivingEntity caster=owner(sl,m);
            if(!alive(target,m.data)) {
                if(caster!=null && target.isAlive() && m.kind==Kind.DOOMED) {
                    GodOfDecayFx.collapse(sl,target);
                    GodOfDecayPowers.doomHit(caster,target,m.data.getFloatOr("health",target.getMaxHealth())*(.30f+.03f*count(caster,target))*m.data.getFloatOr("power",1f));
                }
                remove(target,m);
                continue;
            }
            if(m.kind==Kind.LATENT) {
                if(now-m.data.getLongOr("start",now)>=160 && caster!=null) {
                    remove(target,m);
                    for(Kind disease:List.of(Kind.LAZY,Kind.OBESE,Kind.DEGENERATE)) put(caster,target,disease,200,2);
                    GodOfDecayFx.outbreak(sl,target,200,tag(target,m.key));
                }
                continue;
            }
            if(m.kind==Kind.LAZY || m.kind==Kind.OBESE) {
                target.setSprinting(false);
                if(m.kind==Kind.OBESE) Root.apply(target,2,RootRestriction.JUMP);
                if(target instanceof Mob mob && m.kind==Kind.LAZY) mob.setTarget(null);
            }
            if(m.kind==Kind.RED_TAPE) { Root.apply(target,2,RootRestriction.USE_ITEMS); target.stopUsingItem(); }
            if(m.kind==Kind.SEGMENTED && target instanceof Mob mob) mob.setTarget(null);
            if(now<m.data.getLongOr("pulse",0L)) continue;
            m.data.putLong("pulse",now+20);
            float power=m.data.getFloatOr("power",1f), health=m.data.getFloatOr("health",target.getMaxHealth());
            if(caster!=null) switch(m.kind) {
                case INFECTED -> Spells.strike(caster,target,2*power);
                case ROTTING -> { Spells.strike(caster,target,health*.02f*power); GodOfDecayPowers.wear(target,1,false,true); }
                case DOOMED -> GodOfDecayPowers.doomHit(caster,target,health*.03f*power);
                case AGED -> {
                    if(m.data.getBooleanOr("count_only",false))break;
                    GodOfDecayPowers.wear(target,.1f,true);
                    if(!(target instanceof net.minecraft.world.entity.player.Player) && health<=150 && !Spells.isNaturalBoss(target) && now-m.data.getLongOr("start",now)>=120) {
                        GodOfDecayFx.collapse(sl,target); net.lukario.circleofinevitability.combat.CertainDamage.strike(caster,target,target.getHealth()+target.getMaxHealth()*4,null); remove(target,m);
                    }
                }
                default -> { }
            }
            if(m.kind==Kind.LAZY || m.kind==Kind.OBESE) Spells.keepEffect(target,MobEffects.SLOWNESS,Math.min(8,(int)(2*power)-1),25);
            if(m.kind==Kind.LAZY) Spells.keepEffect(target,MobEffects.MINING_FATIGUE,Math.min(8,(int)(2*power)-1),25);
            if(m.kind!=Kind.FATED && m.kind!=Kind.RED_TAPE) GodOfDecayFx.diseaseTick(sl,target,GodOfDecayFx.Disease.valueOf(m.kind.name()));
            if(caster!=null && m.kind.sickness && m.data.getBooleanOr("contagious",false)
                    && (GodOfDecay.is(caster,0)||count(caster,target)>=2) && now>=m.data.getLongOr("hop",0L)) {
                int interval=GodOfDecayPowers.active(caster,GodOfDecayFx.Ability.VIRUS_INCARNATION)?20:GodOfDecay.is(caster,0)?40:80;
                // One disease per carrier/owner/interval, not one hop per mark.
                String hopKey="decay_hop_"+caster.getUUID();
                if(target.getPersistentData().getLongOr(hopKey,0L)<=now) {
                    for(LivingEntity next:Spells.enemiesAround(caster,sl,target.position(),5).stream().sorted(Comparator.comparingDouble(target::distanceToSqr)).toList()) {
                        if(next==target || Spells.isBoss(next) || from(next,m.kind,caster)) continue;
                        put(caster,next,m.kind,(int)(m.data.getLongOr("until",now)-now),power);
                        GodOfDecayFx.contagion(sl,target,next); break;
                    }
                    target.getPersistentData().putLong(hopKey,now+interval);
                }
                m.data.putLong("hop",now+interval);
            }
        }
        refreshBody(target);
    }
}
