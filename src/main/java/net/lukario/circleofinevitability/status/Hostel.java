package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hostel: a player carries creatures in a place of their own (a Dimension in the body, a menagerie, a bottle). A
 * creature lodged there is gone from the world and kept by its type, health and name; let out, it fights for its
 * keeper as a summon (status/Marionette) and, if it lives when its time ends, it goes back in. Works from any
 * class/ability:
 *
 *   Hostel.lodge(player, mob, 3)                  // false when the 3 places are taken (the mob is not touched then)
 *   Mob guest = Hostel.release(player, sl, feet, 800)   // the oldest guest, out for 40 seconds; null when empty
 *   Hostel.places(player)   Hostel.count(player)   Hostel.empty(player)   Hostel.isGuest(entity)
 *
 * Saved with the player (the guests stay through logging out). Dying empties it. A guest let out whose keeper is
 * gone is not taken back in: it collapses as any summon does.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Hostel {

    /** One lodged creature. */
    public record Guest(String type, float health, String name) {}

    private static final String TAG = "circle_of_inevitability_hostel";
    private static final String KEEPER = "circle_of_inevitability_hostel_keeper";     // on a guest let out
    /** A guest this close to its summon's end goes back in. */
    private static final int BACK_IN = 10;

    private Hostel() {}

    // =========================
    // API
    // =========================

    /** Takes 'mob' in (it leaves the world). False, and nothing happens, when 'max' places are taken. */
    public static boolean lodge(Player player, Mob mob, int max) {
        if (!(mob.level() instanceof ServerLevel) || !mob.isAlive() || count(player) >= max) return false;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        CompoundTag guest = new CompoundTag();
        guest.putString("Type", id.toString());
        guest.putFloat("Health", Math.max(1f, mob.getHealth()));
        if (mob.hasCustomName() && mob.getCustomName() != null) guest.putString("Name", mob.getCustomName().getString());
        ListTag list = list(player);
        list.add(guest);
        player.getPersistentData().put(TAG, list);
        mob.getPersistentData().remove(KEEPER);
        mob.discard();
        return true;
    }

    /** The guests, oldest first. */
    public static List<Guest> places(Player player) {
        List<Guest> guests = new ArrayList<>();
        for (Tag tag : list(player)) {
            if (!(tag instanceof CompoundTag guest)) continue;
            guests.add(new Guest(guest.getStringOr("Type", ""), guest.getFloatOr("Health", 1f), guest.getStringOr("Name", "")));
        }
        return guests;
    }

    public static int count(Player player) {
        return list(player).size();
    }

    public static void empty(Player player) {
        player.getPersistentData().remove(TAG);
    }

    /** Lets the oldest guest out at 'feet' for 'ticks' as a summon of the player's. Null when there is none (or no room). */
    public static Mob release(Player player, ServerLevel sl, Vec3 feet, int ticks) {
        ListTag list = list(player);
        if (list.isEmpty() || !(list.get(0) instanceof CompoundTag guest)) return null;
        Identifier id = Identifier.tryParse(guest.getStringOr("Type", ""));
        EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(id);
        Entity made = type == null ? null : type.create(sl, EntitySpawnReason.MOB_SUMMONED);
        list.remove(0);
        player.getPersistentData().put(TAG, list);
        if (!(made instanceof Mob mob)) {
            if (made != null) made.discard();
            return null;                                        // a type that no longer exists: the place is freed
        }
        mob.snapTo(feet.x, feet.y, feet.z, player.getYRot(), 0f);
        mob.setYHeadRot(player.getYRot());
        String name = guest.getStringOr("Name", "");
        if (!name.isEmpty()) mob.setCustomName(Component.literal(name));
        if (!sl.addFreshEntity(mob) || !Marionette.summon(mob, player, ticks)) {
            mob.discard();
            list.add(0, guest);                                 // it stays in
            player.getPersistentData().put(TAG, list);
            return null;
        }
        mob.setHealth(Math.min(mob.getMaxHealth(), guest.getFloatOr("Health", mob.getMaxHealth())));
        mob.getPersistentData().putString(KEEPER, player.getUUID().toString());
        return mob;
    }

    /** True for a guest that is out of its Hostel now. */
    public static boolean isGuest(Entity entity) {
        return entity != null && entity.getPersistentData().contains(KEEPER);
    }

    private static ListTag list(Player player) {
        return player.getPersistentData().getListOrEmpty(TAG).copy();
    }

    // =========================
    // Events
    // =========================

    /** A guest whose time is nearly up goes back in, at the health it has now. */
    @SubscribeEvent
    public static void onHostelLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob mob) || entity.tickCount % 5 != 0 || !(entity.level() instanceof ServerLevel sl)) return;
        if (!mob.getPersistentData().contains(KEEPER)) return;
        if (Marionette.ticksLeft(mob) > BACK_IN) return;
        try {
            Entity keeper = sl.getEntity(UUID.fromString(mob.getPersistentData().getStringOr(KEEPER, "")));
            mob.getPersistentData().remove(KEEPER);
            if (keeper instanceof Player player && player.isAlive() && mob.isAlive()) lodge(player, mob, Integer.MAX_VALUE);
        } catch (IllegalArgumentException e) {
            mob.getPersistentData().remove(KEEPER);
        }
    }

    @SubscribeEvent
    public static void onHostelDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) empty(player);
    }
}
