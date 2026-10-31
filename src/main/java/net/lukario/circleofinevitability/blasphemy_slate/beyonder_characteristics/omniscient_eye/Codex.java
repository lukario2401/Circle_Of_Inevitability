package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;

import java.util.*;
import net.lukario.circleofinevitability.combat.AbilityStudy.Row;
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
    public static boolean enabled(Player p) {
        return SoulCore.getPathway(p).equals("Omniscient Eye") && SoulCore.getSequence(p) <= 6;
    }
    public static final AbilityStudy LIBRARY=new AbilityStudy("omniscient_codex");
    static CompoundTag data(Player p) { return LIBRARY.data(p); }
    public static String id(Casts.Cast cast) { return AbilityCatalog.id(cast); }
    public static double study(Player p,String id) { return LIBRARY.progress(p,id); }
    public static boolean learn(ServerPlayer p,AbilityCatalog.Power power,double amount) {
        if (!enabled(p) || power==null || power.sequence()<SoulCore.getSequence(p) || amount<=0) return false;
        LIBRARY.add(p,power.id(),amount);
        return true;
    }
    public static Map<String,AbilityCatalog.Power> catalog() { return AbilityCatalog.all(); }
    public static void observe(LivingEntity caster,ServerLevel sl,Casts.Cast cast) {
        if (!cast.copyable()) return;
        for (ServerPlayer p:sl.players()) {
            if (!enabled(p) || p==caster || p.distanceToSqr(caster)>32*32 || !p.hasLineOfSight(caster)) continue;
            Vec3 towards=caster.getEyePosition().subtract(p.getEyePosition());
            if (towards.lengthSqr()>0 && p.getLookAngle().dot(towards.normalize())<0.5) continue;
            int seq=SoulCore.getSequence(p);
            if (cast.sequence()<seq) continue;
            String key=id(cast);
            LIBRARY.add(p,key,StudyRules.gain(seq,cast.sequence()));
        }
    }
    public static List<Row> rows(ServerPlayer p) { return LIBRARY.rows(p,catalog().values()); }
    public static void open(ServerPlayer p) {
        if (enabled(p)) PacketHandler.sendToPlayer(new COpenCodexPacket(rows(p)),p);
    }
    public static void choice(ServerPlayer p,String id,boolean favorite) {
        if (!enabled(p)) return;
        AbilityCatalog.Power power=catalog().get(id);
        if (power==null || study(p,id)<=0) return;
        if (favorite) {
            LIBRARY.toggleFavorite(p,id);
            open(p);
        } else if (study(p,id)>=100 && power.sequence()>=SoulCore.getSequence(p)) LIBRARY.select(p,id);
    }
    public static void replicate(ServerPlayer p) {
        if (!enabled(p)) return;
        String id=data(p).getStringOr("selected","");
        AbilityCatalog.Power power=catalog().get(id);
        if (power==null || study(p,id)<100 || power.sequence()<SoulCore.getSequence(p)) {
            p.sendSystemMessage(Component.literal("Replication requires 100% study at your Sequence or weaker. Sneak + key 4 to choose.")); return;
        }
        boolean success=net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder.atOwnSequence(p,
                ()->Echo.weaker(p,1f,()->Casts.recastPaid(p,power.cast(),OmniscientEyeHigher.replicationCost(p)*NextCast.cost(p))));
        if(success){NextCast.used(p);OmniscientEyeHigher.replicated(p);}
    }
}
