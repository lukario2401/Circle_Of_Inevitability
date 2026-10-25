package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Empowered hit: the next time this player hits something with a normal attack (their fist or weapon, not an
 * ability), that hit is changed. A backstab, a charged blow, a poisoned blade. Works from any class/ability:
 *
 *   EmpoweredHit.arm(player, "my_class_backstab", 40, (attacker, target, amount, sl) -> {
 *       // the hit is landing right now: 'amount' is what it would do. Look, sound, effects go here.
 *       return amount * 1.5f + 10f;               // what it does instead
 *   });
 *
 *   EmpoweredHit.isArmed(player, "my_class_backstab")    EmpoweredHit.ticksLeft(...)    EmpoweredHit.disarm(...)
 *
 * - 40 = how long it waits for the hit (ticks). Unused, it just fades.
 * - the name keeps different empowered hits of one player apart (start it with your class). A player can have
 *   several at once; one hit uses all of them, in the order they were armed. Arming one again replaces it.
 *
 * Only real swings count: Spells.strike (which abilities use) also looks like a player attack to the game, so
 * this listens to the swing itself (AttackEntityEvent) and changes only the damage that swing then does.
 * A swing the game cancels (a rooted player, a concealed target) does not use it up.
 * Not saved: gone after a server restart, on logout and on death.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EmpoweredHit {

    /** What the empowered hit does. Returns the damage the hit deals instead of 'amount'. */
    @FunctionalInterface
    public interface Strike {
        float run(Player attacker, LivingEntity target, float amount, ServerLevel level);
    }

    /** What an empowered hit does, for any attacker (armAny: a mob's own blow counts too). */
    @FunctionalInterface
    public interface AnyStrike {
        float run(LivingEntity attacker, LivingEntity target, float amount, ServerLevel level);
    }

    private record Armed(long expires, Strike strike, AnyStrike any) {
        float run(LivingEntity attacker, LivingEntity target, float amount, ServerLevel level) {
            if (strike != null && attacker instanceof Player player) return strike.run(player, target, amount, level);
            return any != null ? any.run(attacker, target, amount, level) : amount;
        }
    }

    /** player -> (name -> what is armed). LinkedHashMap keeps the order they were armed in. */
    private static final Map<UUID, Map<String, Armed>> ARMED = new HashMap<>();

    /** A swing that is on its way: who swung, at whom, on which tick. The damage it does follows in the same tick. */
    private record Swing(UUID target, long tick) {}

    private static final Map<UUID, Swing> SWINGING = new HashMap<>();

    private EmpoweredHit() {}

    // =========================
    // API
    // =========================

    public static void arm(Player player, String name, int ticks, Strike strike) {
        if (!(player.level() instanceof ServerLevel sl) || ticks <= 0 || strike == null) return;
        long now = sl.getGameTime();
        Map<String, Armed> mine = ARMED.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>());
        mine.values().removeIf(old -> old.expires() < now);
        mine.remove(name);                                       // armed again: it goes to the back of the line
        mine.put(name, new Armed(now + ticks, strike, null));
    }

    /**
     * The same for anyone: a player's next swing, or a mob's next melee blow (not its abilities' strikes). For a
     * Beyonder mob's ability (blasphemy_slate/beyonder_mobs).
     */
    public static void armAny(LivingEntity who, String name, int ticks, AnyStrike strike) {
        if (!(who.level() instanceof ServerLevel sl) || ticks <= 0 || strike == null) return;
        long now = sl.getGameTime();
        Map<String, Armed> mine = ARMED.computeIfAbsent(who.getUUID(), id -> new LinkedHashMap<>());
        mine.values().removeIf(old -> old.expires() < now);
        mine.remove(name);
        mine.put(name, new Armed(now + ticks, null, strike));
    }

    public static boolean isArmed(Entity player, String name) {
        return ticksLeft(player, name) > 0;
    }

    /** Ticks until it fades (0 = not armed). */
    public static int ticksLeft(Entity player, String name) {
        if (player == null) return 0;
        Map<String, Armed> mine = ARMED.get(player.getUUID());
        Armed armed = mine == null ? null : mine.get(name);
        if (armed == null) return 0;
        long left = armed.expires() - player.level().getGameTime();
        if (left <= 0) {
            mine.remove(name);
            return 0;
        }
        return (int) left;
    }

    public static void disarm(Entity player, String name) {
        Map<String, Armed> mine = ARMED.get(player.getUUID());
        if (mine != null) mine.remove(name);
    }

    private static void disarmAll(Entity player) {
        ARMED.remove(player.getUUID());
        SWINGING.remove(player.getUUID());
    }

    // =========================
    // Events
    // =========================

    // LOWEST: whatever cancels the swing (a root, a concealed target) has had its say. A cancelled event never
    // reaches here, so it does not count as a swing.
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onEmpoweredHitSwing(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel sl)) return;
        if (!ARMED.containsKey(player.getUUID())) return;
        SWINGING.put(player.getUUID(), new Swing(event.getTarget().getUUID(), sl.getGameTime()));
    }

    @SubscribeEvent
    public static void onEmpoweredHitHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel sl)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return;        // an arrow, a summon: not their swing

        if (attacker instanceof Player player) {
            Swing swing = SWINGING.remove(player.getUUID());
            if (swing == null || !swing.target().equals(target.getUUID()) || swing.tick() != sl.getGameTime()) return;
        } else if (Spells.isSpellHit(target) || !ARMED.containsKey(attacker.getUUID())) {
            return;                                                          // a mob's ability, not its blow
        }

        Map<String, Armed> mine = ARMED.remove(attacker.getUUID());       // one swing uses everything that is armed
        if (mine == null) return;
        long now = sl.getGameTime();
        float amount = event.getAmount();
        for (Armed armed : mine.values()) {
            if (armed.expires() < now) continue;
            try {
                amount = armed.run(attacker, target, amount, sl);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("An empowered hit failed", e);   // the hit still lands as it was
            }
        }
        event.setAmount(Math.max(0f, amount));
    }

    @SubscribeEvent
    public static void onEmpoweredHitDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) disarmAll(event.getEntity());
    }

    @SubscribeEvent
    public static void onEmpoweredHitLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        disarmAll(event.getEntity());
    }
}
