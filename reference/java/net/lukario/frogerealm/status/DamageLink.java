package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Damage links: make damage go somewhere else. Two kinds, both usable from any class/ability:
 *
 * 1. REDIRECT - part of what one entity takes is dealt to another instead:
 *
 *   DamageLink.redirect(player, target, 0.5f, 100);    // for 5 seconds, half of every hit on the player goes to the target
 *   DamageLink.redirectTarget(player)                  // who it currently goes to (null = no link)
 *   DamageLink.redirectTicksLeft(player)
 *   DamageLink.clearRedirect(player)
 *
 * 2. SHARE - a group feels each other's pain: when one of them is hurt, each of the others takes part of it too:
 *
 *   DamageLink.share(enemies, 0.5f, 200, player);      // for 10 seconds, each takes half of what any other one takes
 *   DamageLink.isShared(entity)                        // (player = who gets the kill; may be null)
 *   DamageLink.sharedWith(entity)                      // the others in its group that are still alive
 *   DamageLink.unshare(entity)                         // take one out of its group
 *
 * Passed-on damage is never passed on again, so links cannot bounce damage back and forth forever.
 * A link ends when its time is up or when the entity at either end dies. Not saved: gone after a server restart.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DamageLink {

    /** A puff of sparks on whoever receives passed-on damage. Set to false to handle visuals yourself. */
    public static boolean SHOW_PARTICLES = true;

    private record Redirect(UUID receiver, float fraction, long expires) {}

    private static final class Bond {
        final Set<UUID> members = new HashSet<>();
        final float fraction;
        final long expires;
        final UUID credit;                  // the player who gets the kills, or null

        Bond(float fraction, long expires, UUID credit) {
            this.fraction = fraction;
            this.expires = expires;
            this.credit = credit;
        }
    }

    private static final Map<UUID, Redirect> REDIRECTS = new HashMap<>();   // who is protected -> where it goes
    private static final Map<UUID, Bond> BONDS = new HashMap<>();           // each member -> the bond it is in
    private static boolean passing;                                         // true while WE are dealing passed-on damage

    private static final ParticleFx PASSED = ParticleFx.of("fx/spark")
            .color(0xFFFFE9C0).endColor(0x00FF7A5A)
            .size(0.1f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(10, 6).friction(0.88f)
            .glow().spin(18f).randomRotation();

    private DamageLink() {}

    // =========================
    // Redirect
    // =========================

    /** For 'ticks' ticks, 'fraction' (0..1) of every hit 'victim' takes is dealt to 'receiver' instead. */
    public static void redirect(LivingEntity victim, LivingEntity receiver, float fraction, int ticks) {
        if (!(victim.level() instanceof ServerLevel sl) || ticks <= 0 || victim == receiver) return;
        float part = Math.max(0f, Math.min(1f, fraction));
        forgetExpired(sl.getGameTime());
        REDIRECTS.put(victim.getUUID(), new Redirect(receiver.getUUID(), part, sl.getGameTime() + ticks));
    }

    /** Who part of this entity's damage currently goes to, or null. */
    public static LivingEntity redirectTarget(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel sl)) return null;
        Redirect redirect = liveRedirect(victim, sl);
        return redirect == null ? null : receiverOf(redirect, sl);
    }

    public static int redirectTicksLeft(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel sl)) return 0;
        Redirect redirect = liveRedirect(victim, sl);
        return redirect == null ? 0 : (int) (redirect.expires() - sl.getGameTime());
    }

    public static void clearRedirect(Entity victim) {
        REDIRECTS.remove(victim.getUUID());
    }

    /** The victim's redirect if it has one that has not run out and whose receiver is still alive here. */
    private static Redirect liveRedirect(LivingEntity victim, ServerLevel sl) {
        Redirect redirect = REDIRECTS.get(victim.getUUID());
        if (redirect == null) return null;
        if (redirect.expires() < sl.getGameTime() || receiverOf(redirect, sl) == null) {
            REDIRECTS.remove(victim.getUUID());
            return null;
        }
        return redirect;
    }

    private static LivingEntity receiverOf(Redirect redirect, ServerLevel sl) {
        return sl.getEntity(redirect.receiver()) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    // =========================
    // Share
    // =========================

    /**
     * For 'ticks' ticks, whenever one of the group is hurt each of the others takes 'fraction' of that damage.
     * Entities that were in another group leave it. 'credit' gets the kills (null = nobody).
     */
    public static void share(Collection<? extends LivingEntity> group, float fraction, int ticks, Player credit) {
        if (group.size() < 2 || ticks <= 0) return;
        Bond bond = null;
        for (LivingEntity member : group) {
            if (!(member.level() instanceof ServerLevel sl)) continue;
            if (bond == null) {
                forgetExpired(sl.getGameTime());
                bond = new Bond(Math.max(0f, fraction), sl.getGameTime() + ticks, credit == null ? null : credit.getUUID());
            }
            unshare(member);
            bond.members.add(member.getUUID());
            BONDS.put(member.getUUID(), bond);
        }
    }

    public static boolean isShared(LivingEntity entity) {
        return entity.level() instanceof ServerLevel sl && liveBond(entity, sl) != null;
    }

    /** The others in this entity's group that are still alive in its dimension (empty if it is in no group). */
    public static List<LivingEntity> sharedWith(LivingEntity entity) {
        List<LivingEntity> others = new ArrayList<>();
        if (!(entity.level() instanceof ServerLevel sl)) return others;
        Bond bond = liveBond(entity, sl);
        if (bond == null) return others;
        for (UUID id : bond.members) {
            if (id.equals(entity.getUUID())) continue;
            if (sl.getEntity(id) instanceof LivingEntity other && other.isAlive()) others.add(other);
        }
        return others;
    }

    public static int shareTicksLeft(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel sl)) return 0;
        Bond bond = liveBond(entity, sl);
        return bond == null ? 0 : (int) (bond.expires - sl.getGameTime());
    }

    /** Takes this entity out of its group. The rest of the group stays linked. */
    public static void unshare(Entity entity) {
        Bond bond = BONDS.remove(entity.getUUID());
        if (bond != null) bond.members.remove(entity.getUUID());
    }

    private static Bond liveBond(LivingEntity entity, ServerLevel sl) {
        Bond bond = BONDS.get(entity.getUUID());
        if (bond == null) return null;
        if (bond.expires < sl.getGameTime()) {
            for (UUID id : bond.members) BONDS.remove(id);      // the whole group is over
            bond.members.clear();
            return null;
        }
        return bond;
    }

    /** Drops every link whose time is up. Called when a new one is made, so links nobody asks about don't pile up. */
    private static void forgetExpired(long now) {
        REDIRECTS.values().removeIf(redirect -> redirect.expires() < now);
        BONDS.values().removeIf(bond -> bond.expires < now);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onDamageLinkDamage(LivingDamageEvent event) {
        if (passing) return;                                    // damage we passed on ourselves stops here
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel sl)) return;
        float amount = event.getAmount();
        if (amount <= 0f) return;

        Redirect redirect = liveRedirect(victim, sl);
        if (redirect != null) {
            LivingEntity receiver = receiverOf(redirect, sl);
            float moved = amount * redirect.fraction();
            amount -= moved;
            event.setAmount(amount);
            pass(sl, receiver, victim.damageSources().thorns(victim), moved);
        }

        Bond bond = liveBond(victim, sl);
        if (bond != null && amount > 0f) {
            Player credit = bond.credit == null ? null : sl.getPlayerByUUID(bond.credit);
            DamageSource source = credit == null ? sl.damageSources().magic() : sl.damageSources().indirectMagic(credit, credit);
            for (LivingEntity other : sharedWith(victim)) pass(sl, other, source, amount * bond.fraction);
        }
    }

    private static void pass(ServerLevel sl, LivingEntity to, DamageSource source, float amount) {
        if (amount <= 0f) return;
        passing = true;
        try {
            to.invulnerableTime = 0;          // it arrives together with other hits, so it must not be swallowed
            to.hurt(source, amount);
        } finally {
            passing = false;
        }
        if (SHOW_PARTICLES) ParticleShapes.burst(sl, PASSED, to.getBoundingBox().getCenter(), 8, 0.04, 0.18);
    }

    @SubscribeEvent
    public static void onDamageLinkDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        clearRedirect(event.getEntity());
        unshare(event.getEntity());
    }

    @SubscribeEvent
    public static void onDamageLinkPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearRedirect(event.getEntity());
        unshare(event.getEntity());
    }
}
