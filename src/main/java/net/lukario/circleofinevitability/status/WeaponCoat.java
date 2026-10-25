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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Weapon coat: something on a player's weapon (or fists) that changes their next few hits. A poison, a concoction,
 * holy oil, frost. Like EmpoweredHit, but it lasts for several hits. Works from any class/ability:
 *
 *   WeaponCoat.arm(player, "my_class_poison", 10, 400, (attacker, target, amount, sl) -> {
 *       // a coated hit is landing right now: 'amount' is what it would do. Effects, looks and sounds go here.
 *       target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
 *       return amount + 4f;                         // what it does instead
 *   });
 *
 *   WeaponCoat.isArmed(player, "my_class_poison")    charges(...)    ticksLeft(...)    disarm(...)
 *
 * - 10 = how many hits it lasts, 400 = and for how long at most (ticks); it wears off at whichever comes first.
 * - the name keeps different coats apart (start it with your class). Several coats can be on at once; each hit uses
 *   one charge of every one of them, in the order they were put on. Arming one again replaces it.
 *
 * Only real swings count (not Spells.strike), as for EmpoweredHit: a swing the game cancels does not use a charge.
 * Inside the coat's part, add to 'amount' rather than calling Spells.strike on the same target; to hurt someone else,
 * do it a tick later (Later.run), so it is not taken for a second swing.
 * Not saved: gone after a server restart, on logout and on death.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponCoat {

    /** What a coated hit does. Returns the damage the hit deals instead of 'amount'. */
    @FunctionalInterface
    public interface Strike {
        float run(Player attacker, LivingEntity target, float amount, ServerLevel level);
    }

    /** What a coated hit does, for any attacker (armAny: a mob's own blows count too). */
    @FunctionalInterface
    public interface AnyStrike {
        float run(LivingEntity attacker, LivingEntity target, float amount, ServerLevel level);
    }

    private static final class Coat {
        final long expires;
        final Strike strike;            // a player's (arm)
        final AnyStrike any;            // anyone's (armAny)
        int charges;

        Coat(long expires, int charges, Strike strike, AnyStrike any) {
            this.expires = expires;
            this.charges = charges;
            this.strike = strike;
            this.any = any;
        }

        float run(LivingEntity attacker, LivingEntity target, float amount, ServerLevel level) {
            if (strike != null && attacker instanceof Player player) return strike.run(player, target, amount, level);
            return any != null ? any.run(attacker, target, amount, level) : amount;
        }
    }

    /** player -> (name -> coat), in the order they were put on. */
    private static final Map<UUID, Map<String, Coat>> COATS = new HashMap<>();

    private record Swing(UUID target, long tick) {}
    private static final Map<UUID, Swing> SWINGING = new HashMap<>();

    private WeaponCoat() {}

    // =========================
    // API
    // =========================

    public static void arm(Player player, String name, int charges, int ticks, Strike strike) {
        if (!(player.level() instanceof ServerLevel sl) || charges <= 0 || ticks <= 0 || strike == null) return;
        Map<String, Coat> mine = COATS.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>());
        mine.remove(name);                                       // put on again: it goes to the back of the line
        mine.put(name, new Coat(sl.getGameTime() + ticks, charges, strike, null));
    }

    /**
     * The same coat on anyone: a player's swings, or a mob's own melee blows (not its abilities' strikes). For a
     * Beyonder mob's ability (blasphemy_slate/beyonder_mobs).
     */
    public static void armAny(LivingEntity who, String name, int charges, int ticks, AnyStrike strike) {
        if (!(who.level() instanceof ServerLevel sl) || charges <= 0 || ticks <= 0 || strike == null) return;
        Map<String, Coat> mine = COATS.computeIfAbsent(who.getUUID(), id -> new LinkedHashMap<>());
        mine.remove(name);
        mine.put(name, new Coat(sl.getGameTime() + ticks, charges, null, strike));
    }

    public static boolean isArmed(Entity player, String name) {
        return live(player, name) != null;
    }

    /** Hits it has left (0 = not armed). */
    public static int charges(Entity player, String name) {
        Coat coat = live(player, name);
        return coat == null ? 0 : coat.charges;
    }

    /** Ticks until it wears off (0 = not armed). */
    public static int ticksLeft(Entity player, String name) {
        Coat coat = live(player, name);
        return coat == null ? 0 : (int) (coat.expires - player.level().getGameTime());
    }

    public static void disarm(Entity player, String name) {
        Map<String, Coat> mine = COATS.get(player.getUUID());
        if (mine == null) return;
        mine.remove(name);
        if (mine.isEmpty()) COATS.remove(player.getUUID());
    }

    private static Coat live(Entity player, String name) {
        if (player == null) return null;
        Map<String, Coat> mine = COATS.get(player.getUUID());
        Coat coat = mine == null ? null : mine.get(name);
        if (coat == null) return null;
        if (coat.expires <= player.level().getGameTime() || coat.charges <= 0) {
            disarm(player, name);
            return null;
        }
        return coat;
    }

    private static void clear(Entity player) {
        COATS.remove(player.getUUID());
        SWINGING.remove(player.getUUID());
    }

    // =========================
    // Events
    // =========================

    // LOWEST: whatever cancels the swing (a root, a concealed target) has had its say
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onWeaponCoatSwing(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel sl) || !COATS.containsKey(player.getUUID())) return;
        SWINGING.put(player.getUUID(), new Swing(event.getTarget().getUUID(), sl.getGameTime()));
    }

    @SubscribeEvent
    public static void onWeaponCoatHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel sl)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getSource().getDirectEntity() != attacker) return;
        if (attacker instanceof Player player) {
            Swing swing = SWINGING.remove(player.getUUID());
            if (swing == null || !swing.target().equals(target.getUUID()) || swing.tick() != sl.getGameTime()) return;
        } else if (Spells.isSpellHit(target) || !COATS.containsKey(attacker.getUUID())) {
            return;                                                  // a mob's ability, not its blow
        }
        Map<String, Coat> mine = COATS.get(attacker.getUUID());
        if (mine == null) return;

        long now = sl.getGameTime();
        float amount = event.getAmount();
        for (Map.Entry<String, Coat> entry : new ArrayList<>(mine.entrySet())) {
            Coat coat = entry.getValue();
            if (coat.expires <= now || coat.charges <= 0) continue;
            coat.charges--;
            try {
                amount = coat.run(attacker, target, amount, sl);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A weapon coat failed", e);   // the hit still lands as it was
            }
        }
        List<String> spent = new ArrayList<>();
        for (Map.Entry<String, Coat> entry : mine.entrySet()) {
            if (entry.getValue().charges <= 0 || entry.getValue().expires <= now) spent.add(entry.getKey());
        }
        spent.forEach(mine::remove);
        if (mine.isEmpty()) COATS.remove(attacker.getUUID());
        event.setAmount(Math.max(0f, amount));
    }

    @SubscribeEvent
    public static void onWeaponCoatDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onWeaponCoatLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }
}
