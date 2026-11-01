package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGodFx;
import net.lukario.circleofinevitability.combat.DamageFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Saved 0..10 counter, one point decaying per 100 idle ticks. A Cast deduplicates DoT/area hits. */
public final class Rage {
    private static final String MARK = "thunder_god_rage", TIPSY = "thunder_god_tipsy";
    private static Cast current;
    private Rage() {}
    public static int count(LivingEntity owner) { return Marks.count(owner, owner, MARK); }
    public static void add(LivingEntity owner, int amount) {
        if (!ThunderGod.is(owner, 8) || amount <= 0) return;
        Marks.add(owner, owner, MARK, amount * (tipsy(owner) ? 2 : 1), 10, 100);
        if (owner instanceof Player p) ThunderGodFx.rage(p, count(owner));
    }
    public static int spend(LivingEntity owner) { return Marks.take(owner, owner, MARK); }
    public static boolean tipsy(LivingEntity owner) { return Marks.count(owner, TIPSY) > 0; }
    public static void drink(LivingEntity owner) { Marks.add(owner, TIPSY, 1, 600); }
    public static void clear(LivingEntity owner) {
        Marks.clear(owner, owner, MARK); Marks.clear(owner, TIPSY);
        owner.getPersistentData().remove("thunder_god_hit_ready");
        if (owner instanceof Player p) ThunderGodFx.rageEnd(p);
    }
    /** Called only after a damage event confirms a hit; blocked/missed attempts never add Rage. */
    public static void landed(LivingEntity owner, LivingEntity target, boolean spell, boolean melee) {
        if (!ThunderGod.is(owner, 8) || !Spells.isEnemy(owner, target)) return;
        if (spell) {
            if (current == null || current.owner != owner || !current.hits.add(target.getUUID())) return;
            melee = current.blow;
        }
        add(owner, 1 + (melee && owner instanceof Player p && Form.isIn(p, ThunderGod.WRATH) ? 1 : 0));
    }
    public static boolean stormHit(LivingEntity owner){return current!=null&&current.owner==owner&&(current.origin.contains("_sky/")||current.origin.contains("_thunder/")||current.origin.contains("_storm/"));}
    public static Cast cast(LivingEntity owner) { return new Cast(owner, false); }
    public static Cast blow(LivingEntity owner) { return new Cast(owner, true); }
    public static final class Cast {
        private final LivingEntity owner;
        private final boolean blow;
        private final String origin;
        private final Set<UUID> hits = new HashSet<>();
        private Cast(LivingEntity owner, boolean blow) { this.owner = owner; this.blow = blow; this.origin=owner instanceof Player p?net.lukario.circleofinevitability.combat.CastContext.id(p):""; }
        public void strikeTrue(LivingEntity target, float damage) {
            Cast before = current; current = this;
            try { DamageFx.credit(owner,target); Spells.strikeTrue(owner,target,damage); } finally { current=before; }
        }
        public void strike(LivingEntity target, float damage) {
            Cast before = current; current = this;
            try { DamageFx.credit(owner, target); Spells.strike(owner, target, damage); }
            finally { current = before; }
        }
    }
}
