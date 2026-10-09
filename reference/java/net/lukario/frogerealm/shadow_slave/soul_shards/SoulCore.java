package net.lukario.frogerealm.shadow_slave.soul_shards;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SoulCore {

    private static final String MOD_TAG = "forgerealmmod";

    private static CompoundTag getModData(Player player) {
        CompoundTag data = player.getPersistentData();

        if (!data.contains(MOD_TAG)) {
            data.put(MOD_TAG, new CompoundTag());
        }

        return data.getCompound(MOD_TAG);
    }

    // =========================
    // INTERNAL CLAMP LOGIC
    // =========================
    private static int clampSoulShards(Player player, int value) {
        int tier = Math.max(1, getAspectTier(player)); // avoid tier = 0
        int max = 250 * tier * SoulCore.getAscensionStage(player);
        return Math.max(0, Math.min(value, max));
    }

    // =========================
    // Soul Shards
    // =========================
    public static int getSoulShards(Player player) {
        CompoundTag tag = getModData(player);

        if (!tag.contains("soul_shards")) {
            tag.putInt("soul_shards", 0); // default shards
        }

        return tag.getInt("soul_shards");
    }


    public static void setSoulShards(Player player, int value) {
        getModData(player).putInt("soul_shards", clampSoulShards(player, value));
    }

    public static void addSoulShards(Player player, int amount) {
        int current = getSoulShards(player);
        setSoulShards(player, current + amount); // clamp handles everything
    }

    // =========================
    // Aspect Tier
    // =========================
    public static int getAspectTier(Player player) {
        CompoundTag tag = getModData(player);

        if (!tag.contains("aspect_tier")) {
            tag.putInt("aspect_tier", 1); // default tier
        }

        return tag.getInt("aspect_tier");
    }


    public static void setAspectTier(Player player, int value) {
        int clamped = Math.min(7, Math.max(1, value)); // tier must be between 1 and 7
        getModData(player).putInt("aspect_tier", clamped);

        setSoulShards(player, getSoulShards(player));
    }

    // =========================
    // Aspect level
    // =========================
    public static int getAscensionStage(Player player) {
        CompoundTag tag = getModData(player);

        if (!tag.contains("aspect_level")) {
            tag.putInt("aspect_level", 1); // default tier
        }

        return tag.getInt("aspect_level");
    }


    public static void setAscensionStage(Player player, int value) {
        int clamped = Math.min(8, Math.max(1, value)); // tier must be between 1 and 7
        getModData(player).putInt("aspect_level", clamped);

        setSoulShards(player, getSoulShards(player));
    }

    // =========================
    // Corruption
    // =========================

    private static CompoundTag getModDataCorruption(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();

        if (!persistentData.contains(MOD_TAG)) {
            persistentData.put(MOD_TAG, new CompoundTag());
        }

        return persistentData.getCompound(MOD_TAG);
    }

    public static int getCorruption(LivingEntity livingEntity) {
        CompoundTag tag = getModDataCorruption(livingEntity);

        if (!tag.contains("corruption_level")) {
            tag.putInt("corruption_level", 0);
        }

        return tag.getInt("corruption_level");
    }

    public static void setCorruption(LivingEntity livingEntity, int value) {
        int clamped = Math.min(100, Math.max(0, value));

        getModDataCorruption(livingEntity).putInt("corruption_level", clamped);
    }

    public static float getSoulEssence(Player player) {
        CompoundTag tag = getModData(player);

        if (!tag.contains("soul_essence")) {
            tag.putFloat("soul_essence", 0);
        }

        return tag.getFloat("soul_essence");
    }

    public static void setSoulEssence(Player player, float value) {
        float maxValue = (float) ((getSoulShards(player)/100) * (getAscensionStage(player) * 10)) * getAspectTier(player) ;
        float clamped = Math.min(maxValue, Math.max(0, value)); // tier must be between 1 and 7
        getModData(player).putFloat("soul_essence", clamped);
    }

    // aspect
    public static String getAspect(Player player) {
        CompoundTag tag = getModData(player);

        if (!tag.contains("soul_aspect")) {
            tag.putString("soul_aspect", "none"); // default tier
        }

        return tag.getString("soul_aspect");
    }


    public static void setAspect(Player player, String aspectName) {
        getModData(player).putString("soul_aspect", aspectName);
    }

    // =========================
    // Copy data on death
    // =========================
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        CompoundTag oldData = event.getOriginal().getPersistentData();
        event.getEntity().getPersistentData().put(MOD_TAG, oldData.getCompound(MOD_TAG));
    }
}
