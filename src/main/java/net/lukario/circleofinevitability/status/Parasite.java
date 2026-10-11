package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Parasite: a player leaves their own body and lives inside another living thing (a mob or a player), which
 * carries them wherever it goes and keeps acting on its own. Works from any Pathway:
 *
 *   Parasite.enter(serverPlayer, host, 2400, HOOKS);    // inside for two minutes at most (0 = no limit). False if refused
 *   Parasite.isInside(player)      Parasite.hostOf(player)      Parasite.ticksInside(player)
 *   Parasite.isHost(entity)        Parasite.parasiteIn(entity)
 *   Parasite.leave(player, Parasite.End.LEFT);          // back out, standing behind the host
 *
 * While inside:
 *   - the parasite has no body: nobody sees it, nothing can hurt it (except the void and /kill), nothing
 *     bumps into it, mobs forget it and spells that use Spells.isEnemy pass over it
 *   - it sees through the host's eyes and goes where the host goes, also through a portal the host takes
 *     (a host that is a player; a mob that changes dimension is a new entity and the parasite is put out)
 *   - holding sneak steps the view back out of the host for as long as it is held (the game does that by itself)
 *   - the ability keys do nothing, except the ones its Hooks allow (allowsKey)
 *   - one host carries one parasite
 *
 * It ends when leave(...) is called, when the time is up, when the host dies or is gone (unloaded, logged
 * out, became a spectator), or when the parasite dies or logs out. Whatever ends it, the parasite gets its
 * body back and Hooks.left(...) is told why (End).
 *
 * WHAT THE PARASITE DOES TO ITS HOST is not in this file: it is the Hooks you pass to enter(...)
 * (entered / tick / left). This file only moves the parasite in, carries it and puts it back out.
 *
 * How it is done: the parasite is put in spectator mode with its view set on the host, which is how the
 * game itself lets someone look through another entity's eyes. The game mode it had before is kept in the
 * player's saved data and given back when it ends; if the server stops without warning while someone is
 * inside, they get it back when they log in again. Everything else (who is in whom, the Hooks) is not
 * saved: a restart ends every parasitism.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Parasite {

    /** Why a parasitism ended. */
    public enum End {
        /** The parasite chose to leave (or something else gave it a body again, e.g. /gamemode). */
        LEFT,
        /** The time given to enter(...) is over. */
        TIME_UP,
        /** The ability that started it could not be kept up (it calls leave with this itself). */
        OUT_OF_POWER,
        /** The host died with the parasite inside. */
        HOST_DIED,
        /** The host is no longer there: unloaded, despawned, logged out, in another dimension, a spectator. */
        HOST_GONE,
        /** The parasite died or logged out. */
        PARASITE_GONE
    }

    /**
     * What a parasitism does. Every method is optional: override the ones you need.
     *
     *   private static final Parasite.Hooks HOOKS = new Parasite.Hooks() {
     *       public void tick(ServerPlayer parasite, LivingEntity host, ServerLevel sl, int ticksInside) {
     *           if (ticksInside % 20 == 0) host.setHealth(host.getHealth() - 1f);
     *       }
     *   };
     */
    public interface Hooks {
        /** Once, right after the parasite went in. */
        default void entered(ServerPlayer parasite, LivingEntity host, ServerLevel sl) {}

        /** Every tick while inside. ticksInside starts at 1. Calling Parasite.leave(...) from here is fine. */
        default void tick(ServerPlayer parasite, LivingEntity host, ServerLevel sl, int ticksInside) {}

        /**
         * Once, when it is over. The parasite has its body back already and stands where it was put out.
         * 'host' is null if it is no longer in this world, and it is dead when 'why' is HOST_DIED.
         */
        default void left(ServerPlayer parasite, LivingEntity host, ServerLevel sl, End why) {}

        /** True for the ability keys (1..7 = G H J K L N M) that still work from inside a host. */
        default boolean allowsKey(int key) { return false; }
    }

    /** Hooks that do nothing: a parasite that only rides along. */
    public static final Hooks NOTHING = new Hooks() {};

    /** How far from the host the parasite is put back out (blocks). It comes out behind the host if there is room. */
    public static double OUT_DISTANCE = 1.5;
    /** No fall damage for this long after coming out (the host may have been in the air). */
    public static int LANDING_TICKS = 100;

    private static final String TAG = "circle_of_inevitability_parasite";   // the game mode to give back
    private static final double LEASH = 3.0;                                // further from the host than this: pulled back

    /** One parasite in one host. */
    private static final class Link {
        final UUID host;
        final int maxTicks;
        final Hooks hooks;
        int ticks;
        Vec3 where;          // where the host was last seen: where the parasite comes out if the host is gone

        Link(UUID host, int maxTicks, Hooks hooks, Vec3 where) {
            this.host = host;
            this.maxTicks = maxTicks;
            this.hooks = hooks;
            this.where = where;
        }
    }

    /** parasite -> what it is in. */
    private static final Map<UUID, Link> LINKS = new HashMap<>();

    private Parasite() {}

    // =========================
    // API
    // =========================

    /**
     * True if this player could go into this host right now: both alive, in the same dimension, the player has a
     * body of its own (is not a spectator, is in no host), and the host is no spectator and carries no parasite.
     * It does not look at distance, line of sight or who is stronger: that is for the ability to decide.
     */
    public static boolean canEnter(Player parasite, LivingEntity host) {
        if (parasite == null || host == null || host == parasite) return false;
        if (parasite.level().isClientSide() || parasite.level() != host.level()) return false;
        if (!parasite.isAlive() || parasite.isSpectator() || isInside(parasite)) return false;
        return host.isAlive() && !host.isSpectator() && !isHost(host);
    }

    /**
     * Puts the parasite into the host for 'maxTicks' ticks at most (0 or less = until something else ends it).
     * 'hooks' is what the parasitism does (null = nothing). False if it could not be done (see canEnter).
     */
    public static boolean enter(ServerPlayer parasite, LivingEntity host, int maxTicks, Hooks hooks) {
        if (!canEnter(parasite, host)) return false;
        ServerLevel sl = parasite.level();

        parasite.getPersistentData().putString(TAG, bodyMode(parasite).name());
        parasite.setGameMode(GameType.SPECTATOR);
        if (!parasite.isSpectator()) {                    // something refused the change
            parasite.getPersistentData().remove(TAG);
            return false;
        }
        parasite.setCamera(host);

        Link link = new Link(host.getUUID(), maxTicks, hooks == null ? NOTHING : hooks, host.position());
        LINKS.put(parasite.getUUID(), link);
        try {
            link.hooks.entered(parasite, host, sl);
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A parasite's entered hook failed", e);   // it is inside all the same
        }
        return true;
    }

    /** Puts the player back out of its host and tells its Hooks why. Does nothing if it is in no host. */
    public static void leave(Player player, End why) {
        Link link = LINKS.get(player.getUUID());
        if (link != null && player instanceof ServerPlayer parasite) end(parasite, link, why);
    }

    public static boolean isInside(Player player) {
        return player != null && LINKS.containsKey(player.getUUID());
    }

    /** The host this player is in, or null (in no host, or the host is not in this world right now). */
    public static LivingEntity hostOf(Player player) {
        Link link = player == null ? null : LINKS.get(player.getUUID());
        if (link == null || !(player.level() instanceof ServerLevel sl)) return null;
        return sl.getEntity(link.host) instanceof LivingEntity host ? host : null;
    }

    /** How long this player has been inside its host (0 if it is in none). */
    public static int ticksInside(Player player) {
        Link link = player == null ? null : LINKS.get(player.getUUID());
        return link == null ? 0 : link.ticks;
    }

    /** True if someone lives in this entity. */
    public static boolean isHost(Entity entity) {
        if (entity == null || LINKS.isEmpty()) return false;
        UUID id = entity.getUUID();
        for (Link link : LINKS.values()) {
            if (link.host.equals(id)) return true;
        }
        return false;
    }

    /** The player who lives in this entity, or null. */
    public static ServerPlayer parasiteIn(Entity host) {
        if (host == null || LINKS.isEmpty() || !(host.level() instanceof ServerLevel sl)) return null;
        UUID id = host.getUUID();
        for (Map.Entry<UUID, Link> entry : LINKS.entrySet()) {
            if (!entry.getValue().host.equals(id)) continue;
            for (ServerPlayer online : sl.getServer().getPlayerList().getPlayers()) {
                if (online.getUUID().equals(entry.getKey())) return online;
            }
        }
        return null;
    }

    /**
     * For the ability keys: true if this player is inside a host and that key (1..7) does not work from there.
     * Tells them so. A key press that gets true does nothing.
     */
    public static boolean blocksKey(Player player, int key) {
        Link link = LINKS.get(player.getUUID());
        if (link == null || link.hooks.allowsKey(key)) return false;
        player.sendSystemMessage(Component.literal("You have no body of your own to do that with."));
        return true;
    }

    // =========================
    // Inner workings
    // =========================

    /** The game mode that gives the player the body it has now. */
    private static GameType bodyMode(ServerPlayer player) {
        if (player.isCreative()) return GameType.CREATIVE;
        return player.getAbilities().mayBuild ? GameType.SURVIVAL : GameType.ADVENTURE;
    }

    /** Back to the game mode from before, and the view back on themselves. Does nothing if nothing was kept. */
    private static void giveBodyBack(ServerPlayer parasite) {
        CompoundTag data = parasite.getPersistentData();
        if (!data.contains(TAG)) return;
        String kept = data.getStringOr(TAG, "");
        data.remove(TAG);
        if (!parasite.isSpectator()) return;              // something else gave them a body already: leave that alone

        GameType mode;
        try {
            mode = GameType.valueOf(kept);
        } catch (IllegalArgumentException e) {
            mode = GameType.SURVIVAL;
        }
        if (mode == GameType.SPECTATOR) mode = GameType.SURVIVAL;
        parasite.setCamera(parasite);
        parasite.setGameMode(mode);
    }

    /** Keeps the parasite's view and place on the host. */
    private static void carry(ServerPlayer parasite, LivingEntity host) {
        // sneaking takes the view off the host (the game does that); it goes back on when they stop sneaking
        if (!parasite.isShiftKeyDown() && parasite.getCamera() != host) parasite.setCamera(host);
        if (parasite.distanceToSqr(host) > LEASH * LEASH) parasite.teleportTo(host.getX(), host.getY(), host.getZ());
    }

    /** Stands the parasite (which has its body back) behind the host, or where the host was if it is gone. */
    private static void putOut(ServerPlayer parasite, ServerLevel sl, LivingEntity host, Vec3 where) {
        Vec3 spot;
        if (host != null && host.isAlive()) {
            Vec3 behind = Vec3.directionFromRotation(0f, host.getYRot()).scale(-1);
            spot = Spells.freeSpotNear(parasite, sl, where, behind, OUT_DISTANCE);
        } else {
            spot = Spells.fits(parasite, sl, where) ? where : Spells.freeSpotNear(parasite, sl, where, Vec3.ZERO, OUT_DISTANCE);
        }
        if (spot == null) spot = where;                   // walled in on every side: where the host is, then
        parasite.teleportTo(spot.x, spot.y, spot.z);
        FallGuard.protect(parasite, LANDING_TICKS);
    }

    private static void end(ServerPlayer parasite, Link link, End why) {
        if (!LINKS.remove(parasite.getUUID(), link)) return;          // ended already
        ServerLevel sl = parasite.level();
        LivingEntity host = sl.getEntity(link.host) instanceof LivingEntity found ? found : null;
        if (host != null) link.where = host.position();

        giveBodyBack(parasite);
        if (parasite.isAlive()) putOut(parasite, sl, host, link.where);
        try {
            link.hooks.left(parasite, host, sl, why);
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A parasite's left hook failed", e);       // it is out all the same
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onParasitePlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (LINKS.isEmpty() || !(event.player() instanceof ServerPlayer parasite)) return;
        Link link = LINKS.get(parasite.getUUID());
        if (link == null) return;
        ServerLevel sl = parasite.level();

        if (!parasite.isAlive()) {
            end(parasite, link, End.PARASITE_GONE);
            return;
        }
        if (!parasite.isSpectator()) {                    // a command gave them a body again
            end(parasite, link, End.LEFT);
            return;
        }
        Entity found = sl.getEntity(link.host);
        if (!(found instanceof LivingEntity host) || !host.isAlive() || host.isSpectator()) {
            end(parasite, link, found instanceof LivingEntity dead && dead.isDeadOrDying() ? End.HOST_DIED : End.HOST_GONE);
            return;
        }

        link.where = host.position();
        link.ticks++;
        if (link.maxTicks > 0 && link.ticks >= link.maxTicks) {
            end(parasite, link, End.TIME_UP);
            return;
        }
        carry(parasite, host);
        try {
            link.hooks.tick(parasite, host, sl, link.ticks);
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A parasite's tick hook failed", e);
        }
    }

    // LOWEST: after DeathWard and anything else that cancels a death, so a host that is saved keeps its parasite
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onParasiteDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || LINKS.isEmpty()) return;

        if (dead instanceof ServerPlayer parasite) {
            Link own = LINKS.get(parasite.getUUID());
            if (own != null) end(parasite, own, End.PARASITE_GONE);
        }
        ServerPlayer inside = parasiteIn(dead);
        if (inside != null) leave(inside, End.HOST_DIED);
    }

    @SubscribeEvent
    public static void onParasiteLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        leave(player, End.PARASITE_GONE);                 // they are saved with their own body, standing by the host
        ServerPlayer inside = parasiteIn(player);
        if (inside != null) leave(inside, End.HOST_GONE);
    }

    @SubscribeEvent
    public static void onParasiteLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (LINKS.containsKey(player.getUUID()) || !player.getPersistentData().contains(TAG)) return;
        // the server stopped without warning while they were inside something
        giveBodyBack(player);
        FallGuard.protect(player, LANDING_TICKS);
    }
}
