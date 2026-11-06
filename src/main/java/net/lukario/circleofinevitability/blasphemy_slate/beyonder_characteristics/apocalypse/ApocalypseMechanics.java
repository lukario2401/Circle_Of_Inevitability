package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.lukario.circleofinevitability.status.EmpoweredHit;
import net.lukario.circleofinevitability.status.NoHeal;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Combat-earned interactions; no cast counter or automatic catastrophe. State belongs to an owner and a world. */
final class ApocalypseMechanics {
    enum Affliction { FLAME, SILK, PLAGUE }

    static final int WEAVE_TICKS = 160;
    static final int DESPAIR_TICKS = 120;
    static final int DESPAIR_COOLDOWN = 200;
    static final float HARVEST_DAMAGE = 24f;
    static final float HARVEST_HEAL = 4f;
    static final float HARVEST_CHARGE = 0.25f;
    static final double MIRROR_REACH = 12;
    static final float CHARGE_POWER = 0.15f;
    static final int AMBUSH_WAIT = 40;
    static final int AMBUSH_TICKS = 60;
    static final int AMBUSH_COOLDOWN = 200;
    static final float AMBUSH_DAMAGE = 12f;
    static final String AMBUSH = "apocalypse_ambush";

    private record Pair(UUID owner, UUID victim) {}
    private static final class Weave {
        final ServerLevel level;
        final long[] seals = new long[Affliction.values().length];
        long despairUntil;
        long readyAt;
        Weave(ServerLevel level) { this.level = level; }
        long expires() {
            long until = Math.max(despairUntil, readyAt);
            for (long seal : seals) until = Math.max(until, seal);
            return until;
        }
    }
    private record Preparation(ServerLevel level, long readyAt) {}
    private static final Map<Pair, Weave> WEAVES = new HashMap<>();
    private static final Map<UUID, Preparation> PREPARATIONS = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_SWEEP = new HashMap<>();

    private ApocalypseMechanics() {}

    /** Only actual enemy contact contributes. Duplicate families refresh their own seal, never create another. */
    static void afflict(LivingEntity owner, ServerLevel sl, LivingEntity victim, Affliction kind) {
        if (!Spells.casterStillHere(owner, sl) || victim.level() != sl || !Spells.isEnemy(owner, victim)) return;
        Pair key = new Pair(owner.getUUID(), victim.getUUID());
        Weave weave = WEAVES.get(key);
        if (weave == null || weave.level != sl) {
            weave = new Weave(sl);
            WEAVES.put(key, weave);
        }
        long now = sl.getGameTime();
        if (weave.readyAt > now) return;
        weave.seals[kind.ordinal()] = now + WEAVE_TICKS;
        SpellFx.runeCircle(sl, victim.position(), 0.55 + kind.ordinal() * 0.15,
                switch (kind) { case FLAME -> 0xC09040D0; case SILK -> 0xC0D0B8FF; case PLAGUE -> 0xC070B080; }, 10, 2f);
        for (long seal : weave.seals) if (seal <= now) return;
        java.util.Arrays.fill(weave.seals, 0);
        weave.despairUntil = now + DESPAIR_TICKS;
        weave.readyAt = now + DESPAIR_COOLDOWN;
        NoHeal.apply(victim, DESPAIR_TICKS);
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DESPAIR_TICKS, 1));
        DemonessFx.curseMark(sl, victim, DESPAIR_TICKS, "apocalypse_despair_" + owner.getUUID());
        Spells.sound(sl, victim.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9f, 0.5f);
        if (owner instanceof Player player) player.sendOverlayMessage(
                Component.literal("Despair woven — Fatal Step can harvest it"));
    }

    static boolean hasDespair(LivingEntity owner, LivingEntity victim) {
        Weave weave = WEAVES.get(new Pair(owner.getUUID(), victim.getUUID()));
        return weave != null && weave.level == owner.level() && victim.level() == weave.level
                && weave.despairUntil > weave.level.getGameTime();
    }

    /** Invoked by Fatal Step's actual melee hit. A second backstab cannot harvest the same window. */
    static float harvest(LivingEntity owner, ServerLevel sl, LivingEntity victim) {
        if (!Spells.casterStillHere(owner, sl) || !Spells.isEnemy(owner, victim) || !hasDespair(owner, victim)) return 0f;
        WEAVES.get(new Pair(owner.getUUID(), victim.getUUID())).despairUntil = 0;
        owner.heal(HARVEST_HEAL);
        for (Apocalypse.Shard shard : Apocalypse.shardsOf(owner, sl)) {
            if (shard.ground.distanceToSqr(victim.position()) > MIRROR_REACH * MIRROR_REACH) continue;
            shard.charge = Math.min(1f, shard.charge + HARVEST_CHARGE);
            DemonessFx.shardCharge(sl, shard.ground, shard.yaw, shard.charge, shard.tag);
        }
        DemonessFx.curseRelease(sl, victim, "apocalypse_despair_" + owner.getUUID());
        SpellFx.shockRing(sl, victim.position(), 2, 0xC0D27BFF, 12, 0);
        return HARVEST_DAMAGE;
    }

    /** Wait inside the mirror, then land a melee hit during a short opening. Spell damage cannot spend it. */
    static void prepareAmbush(LivingEntity owner, ServerLevel sl, int timeInside) {
        if (timeInside < AMBUSH_WAIT || !Spells.casterStillHere(owner, sl)) return;
        long now = sl.getGameTime();
        Preparation old = PREPARATIONS.get(owner.getUUID());
        if (old != null && old.level() == sl && old.readyAt() > now) return;
        PREPARATIONS.put(owner.getUUID(), new Preparation(sl, now + AMBUSH_COOLDOWN));
        EmpoweredHit.armAny(owner, AMBUSH, AMBUSH_TICKS, (attacker, victim, amount, level) -> {
            hideAmbush(attacker);
            if (level != sl || !Spells.casterStillHere(attacker, sl) || !Spells.isEnemy(attacker, victim)) return amount;
            afflict(attacker, sl, victim, Affliction.SILK);
            DemonessFx.blackFlameHit(sl, victim.getBoundingBox().getCenter());
            return amount + AMBUSH_DAMAGE;
        });
        if (owner instanceof Player player) HudTimer.show(player, AMBUSH, "Mirror ambush", AMBUSH_TICKS, 0xD27BFF);
    }

    /** Consume charge only when a paid catastrophe executes. Distance is measured from its aimed impact. */
    static float feedCatastrophe(LivingEntity owner, ServerLevel sl, Vec3 ground) {
        float charge = 0;
        for (Apocalypse.Shard shard : Apocalypse.shardsOf(owner, sl)) {
            if (shard.ground.distanceToSqr(ground) > MIRROR_REACH * MIRROR_REACH || shard.charge <= 0) continue;
            charge += shard.charge;
            shard.charge = 0;
            DemonessFx.shardCharge(sl, shard.ground, shard.yaw, 0, shard.tag);
        }
        float power = 1 + Math.min(Apocalypse.SHARD_MAX, charge) * CHARGE_POWER;
        if (power > 1) {
            SpellFx.shockRing(sl, ground, 3, 0xC0D27BFF, 12, 0);
            Spells.sound(sl, ground, SoundEvents.GLASS_BREAK, 0.8f, 1.4f);
            if (owner instanceof Player player) player.sendOverlayMessage(
                    Component.literal("Mirror-fed catastrophe +" + Math.round((power - 1) * 100) + "%"));
        }
        return power;
    }

    static void tick(LivingEntity owner, ServerLevel sl) {
        if (owner.tickCount % 20 != 0) return;
        if (!Apocalypse.PATHWAY.equals(Beyonders.pathwayOf(owner))) clearOwner(owner);
        long now = sl.getGameTime();
        Long previous = LAST_SWEEP.get(sl);
        if (previous != null && now - previous < 20) return;
        LAST_SWEEP.put(sl, now);
        WEAVES.entrySet().removeIf(e -> e.getValue().level == sl && e.getValue().expires() <= now);
        PREPARATIONS.entrySet().removeIf(e -> e.getValue().level() == sl && e.getValue().readyAt() <= now);
    }

    private static void clearOwner(LivingEntity entity) {
        UUID id = entity.getUUID();
        WEAVES.keySet().removeIf(key -> key.owner().equals(id));
        boolean prepared = PREPARATIONS.remove(id) != null || EmpoweredHit.isArmed(entity, AMBUSH);
        EmpoweredHit.disarm(entity, AMBUSH);
        if (prepared) hideAmbush(entity);
    }

    static void clear(LivingEntity entity) {
        clearOwner(entity);
        WEAVES.keySet().removeIf(key -> key.victim().equals(entity.getUUID()));
    }

    private static void hideAmbush(LivingEntity entity) {
        if (entity instanceof Player player) HudTimer.hide(player, AMBUSH);
    }

    static void clearLevel(ServerLevel level) {
        LAST_SWEEP.remove(level);
        WEAVES.values().removeIf(weave -> weave.level == level);
        PREPARATIONS.values().removeIf(preparation -> preparation.level() == level);
    }

    /** Existing saves lose their old counter and orb row; combat preparations deliberately start empty. */
    static void removeLegacyStacks(Player player) {
        player.getPersistentData().remove("apocalypse_stacks");
        for (int i = 0; i < 20; i++) player.getPersistentData().remove("apocalypse_stack_image_" + i);
        ScreenImages.hide(player, "apo_stack_*");
    }
}
