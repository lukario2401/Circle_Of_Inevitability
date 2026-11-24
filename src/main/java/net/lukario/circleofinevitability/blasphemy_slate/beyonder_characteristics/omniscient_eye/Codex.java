package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;

import java.util.*;
import net.lukario.circleofinevitability.combat.AbilityStudy.Row;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsReplication;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
    /** Fully learns every currently eligible catalog entry; retains the ordinary Sequence gates. */
    public static int learnAll(ServerPlayer p){
        if(!enabled(p))return 0;
        int count=0;for(var power:catalog().values())if(learn(p,power,100))count++;
        return count;
    }
    private static AbilityStudy activeLibrary(Player player) {
        if (enabled(player)) return LIBRARY;
        return KeyOfStarsReplication.enabled(player) ? KeyOfStarsReplication.MEMORY : null;
    }

    public static void observe(LivingEntity caster,ServerLevel sl,Casts.Cast cast) {
        if (!cast.copyable()) return;
        for (ServerPlayer p:sl.players()) {
            AbilityStudy library = activeLibrary(p);
            if (library == null || !StudyRules.witnesses(p, caster)) continue;
            int seq=SoulCore.getSequence(p);
            if (cast.sequence()<seq) continue;
            String key=id(cast);
            if (library.complete(p, key)) continue;
            double progress = library.add(p, key, StudyRules.gain(seq, cast.sequence()));
            if (library == KeyOfStarsReplication.MEMORY && progress >= 100) {
                p.sendSystemMessage(Component.literal("Memorized " + cast.name() + " for Replication."));
            }
        }
    }
    public static List<Row> rows(ServerPlayer p) { return LIBRARY.rows(p,catalog().values()); }
    public static void open(ServerPlayer p) {
        AbilityStudy library = activeLibrary(p);
        if (library != null) PacketHandler.sendToPlayer(new COpenCodexPacket(library.rows(p, catalog().values())), p);
    }
    public static void choice(ServerPlayer p,String id,boolean favorite) {
        AbilityStudy library = activeLibrary(p);
        if (library == null) return;
        AbilityCatalog.Power power = catalog().get(id);
        if (power == null || library.progress(p, id) <= 0) return;
        if (favorite) {
            library.toggleFavorite(p, id);
            open(p);
        } else if (power.sequence() >= SoulCore.getSequence(p)) {
            library.select(p, id);
        }
    }
    public static void replicate(ServerPlayer p) {
        if (!enabled(p)) return;
        AbilityCatalog.Power power = LIBRARY.selectedPower(p);
        if (power == null) {
            p.sendSystemMessage(Component.literal("Replication requires 100% study at your Sequence or weaker. Sneak + key 4 to choose.")); return;
        }
        boolean success=net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder.atOwnSequence(p,
                ()->Echo.weaker(p,1f,()->Casts.recastPaid(p,power.cast(),OmniscientEyeHigher.replicationCost(p)*NextCast.cost(p))));
        if(success){NextCast.used(p);OmniscientEyeHigher.replicated(p);}
    }
}
