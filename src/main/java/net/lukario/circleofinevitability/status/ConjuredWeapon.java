package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpirituality;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A conjured weapon: a weapon that is not an item. A sword of light, a blade of shadow, claws of flame. Nothing is put
 * in the hand, so nothing can be dropped, stolen or duplicated: while it is drawn, the player's normal hits (fist or
 * whatever they hold) ARE the weapon's hits (a Beyonder mob's own blows too, when its ability draws one). Works from any class/ability:
 *
 *   ConjuredWeapon.of("my_class_flame_blade")
 *           .lasts(800)                        // ticks; 0 = until put away                    (default 0)
 *           .damage(6f)                        // more damage on every hit while drawn          (optional)
 *           .reach(1.0)                        // blocks more reach while drawn                 (optional)
 *           .upkeep(20, 25)                    // 25 spirituality every 20 ticks; put away when it cannot be paid (optional)
 *           .onHit((attacker, target, amount, charge, sl) -> amount)   // every hit; return the damage (optional)
 *           .onEnd((wielder, why) -> ...)      // it was put away, and why                      (optional)
 *           .draw(player);
 *
 *   ConjuredWeapon.isDrawn(player, "my_class_flame_blade")    ticksLeft(...)    putAway(player, "my_class_flame_blade")
 *
 * 'charge' in onHit is how full the attack bar was when they swung (1 = a full swing): for "a full swing also...".
 * Only real swings count (not Spells.strike), as for EmpoweredHit; inside onHit add to 'amount' rather than striking
 * the same target, and hurt others a tick later (Later.run).
 * The look is yours: a model held beside the body (ParticleShapes.modelOnTurning), cleared in onEnd.
 * One weapon per name (start it with your class); drawing it again replaces it (the old onEnd runs with REPLACED).
 * Not saved: put away on death, on logout and after a server restart.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConjuredWeapon {

    public enum Ending { TIME, PUT_AWAY, NO_SPIRITUALITY, REPLACED, DIED, LEFT }

    @FunctionalInterface
    public interface Hit {
        float run(LivingEntity attacker, LivingEntity target, float amount, float charge, ServerLevel level);
    }

    @FunctionalInterface
    public interface End {
        void run(LivingEntity wielder, Ending why);
    }

    private final String name;
    private int lasts;
    private float damage;
    private double reach;
    private int upkeepEvery;
    private float upkeepCost;
    private Hit onHit;
    private End onEnd;

    private record Drawn(ConjuredWeapon weapon, long started) {}

    /** player -> (name -> weapon). */
    private static final Map<UUID, Map<String, Drawn>> DRAWN = new HashMap<>();

    private record Swing(UUID target, long tick, float charge) {}
    private static final Map<UUID, Swing> SWINGING = new HashMap<>();

    private ConjuredWeapon(String name) {
        this.name = name;
    }

    public static ConjuredWeapon of(String name) {
        return new ConjuredWeapon(name);
    }

    public ConjuredWeapon lasts(int ticks) { this.lasts = Math.max(0, ticks); return this; }
    public ConjuredWeapon damage(float extra) { this.damage = extra; return this; }
    public ConjuredWeapon reach(double blocks) { this.reach = blocks; return this; }
    public ConjuredWeapon upkeep(int everyTicks, float cost) { this.upkeepEvery = Math.max(0, everyTicks); this.upkeepCost = Math.max(0f, cost); return this; }
    public ConjuredWeapon onHit(Hit action) { this.onHit = action; return this; }
    public ConjuredWeapon onEnd(End action) { this.onEnd = action; return this; }

    /** Draws it now. */
    public void draw(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel sl) || !player.isAlive()) return;
        if (isDrawn(player, name)) finish(player, name, Ending.REPLACED);
        DRAWN.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(name, new Drawn(this, sl.getGameTime()));
        reachOf(player);
    }

    // =========================
    // API
    // =========================

    public static boolean isDrawn(Entity player, String name) {
        Map<String, Drawn> mine = player == null ? null : DRAWN.get(player.getUUID());
        return mine != null && mine.containsKey(name);
    }

    /** Ticks until it is put away by itself (0 = not drawn; Integer.MAX_VALUE = until put away). */
    public static int ticksLeft(Entity player, String name) {
        Map<String, Drawn> mine = player == null ? null : DRAWN.get(player.getUUID());
        Drawn drawn = mine == null ? null : mine.get(name);
        if (drawn == null) return 0;
        if (drawn.weapon().lasts <= 0) return Integer.MAX_VALUE;
        return (int) Math.max(0, drawn.started() + drawn.weapon().lasts - player.level().getGameTime());
    }

    /** Puts it away now (its onEnd runs with PUT_AWAY). */
    public static void putAway(LivingEntity player, String name) {
        if (isDrawn(player, name)) finish(player, name, Ending.PUT_AWAY);
    }

    // =========================
    // Internals
    // =========================

    private static Identifier reachId(String name) {
        return Physique.id("conjured_weapon_reach_" + name.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
    }

    private static void reachOf(LivingEntity player) {
        Map<String, Drawn> mine = DRAWN.get(player.getUUID());
        if (mine == null) return;
        for (Drawn drawn : mine.values()) {
            double reach = drawn.weapon().reach;
            Physique.keep(player, Attributes.ENTITY_INTERACTION_RANGE, reachId(drawn.weapon().name), reach, AttributeModifier.Operation.ADD_VALUE, reach != 0);
        }
    }

    private static void finish(LivingEntity player, String name, Ending why) {
        Map<String, Drawn> mine = DRAWN.get(player.getUUID());
        Drawn drawn = mine == null ? null : mine.remove(name);
        if (mine != null && mine.isEmpty()) DRAWN.remove(player.getUUID());
        if (drawn == null) return;
        Physique.remove(player, Attributes.ENTITY_INTERACTION_RANGE, reachId(name));
        if (drawn.weapon().onEnd == null) return;
        try {
            drawn.weapon().onEnd.run(player, why);
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("A conjured weapon's end failed", e);
        }
    }

    private static void finishAll(LivingEntity player, Ending why) {
        Map<String, Drawn> mine = DRAWN.get(player.getUUID());
        if (mine == null) return;
        for (String name : mine.keySet().toArray(new String[0])) finish(player, name, why);
        SWINGING.remove(player.getUUID());
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onConjuredWeaponTick(TickEvent.PlayerTickEvent.Post event) {
        tick(event.player());
    }

    /** A Beyonder mob's conjured weapon (a mob's ability) wears off and is paid for the same way. */
    @SubscribeEvent
    public static void onConjuredWeaponMobTick(LivingEvent.LivingTickEvent event) {
        if (DRAWN.isEmpty() || event.getEntity() instanceof Player) return;
        tick(event.getEntity());
    }

    private static void tick(LivingEntity player) {
        if (DRAWN.isEmpty() || !(player.level() instanceof ServerLevel sl)) return;
        Map<String, Drawn> mine = DRAWN.get(player.getUUID());
        if (mine == null) return;
        long now = sl.getGameTime();
        for (Drawn drawn : mine.values().toArray(new Drawn[0])) {
            ConjuredWeapon weapon = drawn.weapon();
            long age = now - drawn.started();
            if (weapon.lasts > 0 && age >= weapon.lasts) {
                finish(player, weapon.name, Ending.TIME);
                continue;
            }
            if (weapon.upkeepEvery > 0 && weapon.upkeepCost > 0f && age > 0 && age % weapon.upkeepEvery == 0) {
                if (!(player instanceof Player payer)) {                              // a mob pays from its own bar
                    if (!MobSpirituality.spend(player, weapon.upkeepCost)) finish(player, weapon.name, Ending.NO_SPIRITUALITY);
                    continue;
                }
                if (SoulCore.getSpirituality(payer) < weapon.upkeepCost) {
                    player.sendSystemMessage(Component.literal("Your spirituality runs dry and the weapon fades."));
                    finish(player, weapon.name, Ending.NO_SPIRITUALITY);
                    continue;
                }
                SoulCore.setSpirituality(payer, SoulCore.getSpirituality(payer) - weapon.upkeepCost);
            }
        }
        if (now % 20 == 0) reachOf(player);                                         // the modifiers are not saved
    }

    // LOWEST: whatever cancels the swing (a root, a concealed target) has had its say
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onConjuredWeaponSwing(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel sl) || !DRAWN.containsKey(player.getUUID())) return;
        SWINGING.put(player.getUUID(), new Swing(event.getTarget().getUUID(), sl.getGameTime(), player.getAttackStrengthScale(0.5f)));
    }

    @SubscribeEvent
    public static void onConjuredWeaponHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel sl)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity player) || event.getSource().getDirectEntity() != player) return;
        Swing swing;
        if (player instanceof Player) {
            swing = SWINGING.remove(player.getUUID());
            if (swing == null || !swing.target().equals(target.getUUID()) || swing.tick() != sl.getGameTime()) return;
        } else {                                                     // a mob's own blow (not its ability's strike)
            if (Spells.isSpellHit(target) || !DRAWN.containsKey(player.getUUID())) return;
            swing = new Swing(target.getUUID(), sl.getGameTime(), 1f);
        }
        Map<String, Drawn> mine = DRAWN.get(player.getUUID());
        if (mine == null) return;
        float amount = event.getAmount();
        for (Drawn drawn : mine.values().toArray(new Drawn[0])) {
            ConjuredWeapon weapon = drawn.weapon();
            amount += weapon.damage * Math.max(0.2f, swing.charge());      // a spam click does little more
            if (weapon.onHit == null) continue;
            try {
                amount = weapon.onHit.run(player, target, amount, swing.charge(), sl);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A conjured weapon's hit failed", e);
            }
        }
        event.setAmount(Math.max(0f, amount));
    }

    @SubscribeEvent
    public static void onConjuredWeaponDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && DRAWN.containsKey(event.getEntity().getUUID())) finishAll(event.getEntity(), Ending.DIED);
    }

    @SubscribeEvent
    public static void onConjuredWeaponLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        finishAll(event.getEntity(), Ending.LEFT);
    }
}
