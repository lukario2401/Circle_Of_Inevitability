package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

/** Only stable catalog identifiers are persisted; no expiring Cast or entity references. */
public final class Codex {
    private Codex() {}
    public record Power(String id, String name, String icon, int sequence, Casts.Cast cast) {}
    public record Row(String id, String name, String icon, int sequence, double study, boolean favorite, boolean selected) {}
    public static boolean enabled(Player p) {
        return SoulCore.getPathway(p).equals("Omniscient Eye") && SoulCore.getSequence(p) <= 6;
    }
    static CompoundTag data(Player p) {
        CompoundTag persistent=p.getPersistentData();
        if (!persistent.contains("circle_of_inevitability")) persistent.put("circle_of_inevitability", new CompoundTag());
        CompoundTag mod=persistent.getCompoundOrEmpty("circle_of_inevitability");
        if (!mod.contains("omniscient_codex")) mod.put("omniscient_codex",new CompoundTag());
        return mod.getCompoundOrEmpty("omniscient_codex");
    }
    public static String id(Casts.Cast cast) {
        return cast.entry()!=null ? "wheel/"+cast.wheel().id()+"/"+cast.entry().key()
                : "key/"+cast.pathway()+"/"+cast.key()+"/"+cast.sneak();
    }
    public static double study(Player p,String id) { return Math.clamp(data(p).getDoubleOr(id,0),0,100); }
    public static boolean learn(ServerPlayer p,Power power,double amount) {
        if (!enabled(p) || power==null || power.sequence()<SoulCore.getSequence(p) || amount<=0) return false;
        data(p).putDouble(power.id(),Math.min(100,study(p,power.id())+amount));
        return true;
    }
    public static Map<String,Power> catalog() {
        CodexCatalog.initialize();
        Map<String,Power> result=new LinkedHashMap<>();
        UUID nobody=new UUID(0,0);
        for (var a:PathwayAbilities.ALL) if (a.stealable()) {
            for (boolean sneak:new boolean[]{false,true}) {
                if (sneak && !a.sneakForm()) continue;
                var cast=new Casts.Cast(nobody,"",a.pathway(),a.key(),sneak,null,null,a,"",Vec3.ZERO,0);
                String slug=a.pathway().toLowerCase(Locale.ROOT).replace(' ','_');
                String icon="ability_menus/"+slug+"/"+cast.name().replace(" (sneak)","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_");
                result.put(id(cast),new Power(id(cast),a.pathway()+": "+cast.name(),icon,a.sequence(),cast));
            }
        }
        for (var wheel:AbilityWheel.registered()) for (var e:wheel.entries()) if (e.copyable()) {
            var cast=new Casts.Cast(nobody,"","",0,false,wheel,e,null,"",Vec3.ZERO,0);
            result.put(id(cast),new Power(id(cast),e.name(),e.icon(),e.sequence(),cast));
        }
        return result;
    }
    public static void observe(LivingEntity caster,ServerLevel sl,Casts.Cast cast) {
        if (!cast.copyable()) return;
        for (ServerPlayer p:sl.players()) {
            if (!enabled(p) || p==caster || p.distanceToSqr(caster)>32*32 || !p.hasLineOfSight(caster)) continue;
            Vec3 towards=caster.getEyePosition().subtract(p.getEyePosition());
            if (towards.lengthSqr()>0 && p.getLookAngle().dot(towards.normalize())<0.5) continue;
            int seq=SoulCore.getSequence(p);
            if (cast.sequence()<seq) continue;
            String key=id(cast);
            data(p).putDouble(key,Math.min(100,study(p,key)+StudyRules.gain(seq,cast.sequence())));
        }
    }
    public static List<Row> rows(ServerPlayer p) {
        List<Row> rows=new ArrayList<>();
        for (Power power:catalog().values()) {
            double study=study(p,power.id());
            if (study<=0) continue;
            rows.add(new Row(power.id(),power.name(),power.icon(),power.sequence(),study,
                    data(p).getBooleanOr("favorite/"+power.id(),false),data(p).getStringOr("selected","").equals(power.id())));
        }
        rows.sort((a,b)->StudyRules.compare(a.favorite(),a.study(),a.sequence(),a.name(),b.favorite(),b.study(),b.sequence(),b.name()));
        return rows;
    }
    public static void open(ServerPlayer p) {
        if (enabled(p)) PacketHandler.sendToPlayer(new COpenCodexPacket(rows(p)),p);
    }
    public static void choice(ServerPlayer p,String id,boolean favorite) {
        if (!enabled(p)) return;
        Power power=catalog().get(id);
        if (power==null || study(p,id)<=0) return;
        if (favorite) {
            data(p).putBoolean("favorite/"+id,!data(p).getBooleanOr("favorite/"+id,false));
            open(p);
        } else if (power.sequence()>=SoulCore.getSequence(p)) data(p).putString("selected",id);
    }
    public static void replicate(ServerPlayer p) {
        if (!enabled(p)) return;
        String id=data(p).getStringOr("selected","");
        Power power=catalog().get(id);
        if (power==null || study(p,id)<=0 || power.sequence()<SoulCore.getSequence(p)) {
            p.sendSystemMessage(Component.literal("Sneak + key 4 to select a studied ability at your Sequence or weaker.")); return;
        }
        net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder.atOwnSequence(p,
                ()->Echo.weaker(p,(float)(0.4+0.6*study(p,id)/100),()->Casts.recastPaid(p,power.cast(),1.5f)));
    }
}
