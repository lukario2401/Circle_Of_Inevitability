package net.lukario.frogerealm.shadow_slave.beyonder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public class SoulCore {

    private static final String MOD_TAG = "forgerealmmod";

    private static CompoundTag getModData(Player player) {
        CompoundTag data = player.getPersistentData();

        if (!data.contains(MOD_TAG)) {
            data.put(MOD_TAG, new CompoundTag());
        }

        return data.getCompoundOrEmpty(MOD_TAG);
    }

    // =========================
    // Sequence: 9 (weakest) .. 0 (strongest). The old ascension level was 9 - Sequence.
    // Ability gates go through Beyonder.hasSequence, scaling through Beyonder.powerLevel / powerScale.
    // =========================
    public static int getSequence(Player player) {
        CompoundTag tag = getModData(player);

        if (!tag.contains("sequence")) {
            tag.putInt("sequence", Beyonder.WEAKEST_SEQUENCE); // everyone starts at Sequence 9
        }

        return tag.getIntOr("sequence", Beyonder.WEAKEST_SEQUENCE);
    }


    public static void setSequence(Player player, int value) {
        int clamped = Math.min(Beyonder.WEAKEST_SEQUENCE, Math.max(Beyonder.STRONGEST_SEQUENCE, value));
        getModData(player).putInt("sequence", clamped);

        setSpiritualityQuietly(player, getSpirituality(player)); // the maximum may have changed
    }

    // =========================
    // Corruption
    // =========================

    private static CompoundTag getModDataCorruption(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();

        if (!persistentData.contains(MOD_TAG)) {
            persistentData.put(MOD_TAG, new CompoundTag());
        }

        return persistentData.getCompoundOrEmpty(MOD_TAG);
    }

    public static int getCorruption(LivingEntity livingEntity) {
        CompoundTag tag = getModDataCorruption(livingEntity);

        if (!tag.contains("corruption_level")) {
            tag.putInt("corruption_level", 0);
        }

        return tag.getIntOr("corruption_level", 0);
    }

    public static void setCorruption(LivingEntity livingEntity, int value) {
        int clamped = Math.min(100, Math.max(0, value));

        getModDataCorruption(livingEntity).putInt("corruption_level", clamped);
    }

    // =========================
    // Spirituality (the resource abilities spend)
    // =========================
    public static float getSpirituality(Player player) {
        CompoundTag tag = getModData(player);

        if (!tag.contains("spirituality")) {
            tag.putFloat("spirituality", 0);
        }

        return tag.getFloatOr("spirituality", 0f);
    }

    /** Sets spirituality (clamped to 0..max). A decrease counts as spirituality spent on an ability and feeds digestion. */
    public static void setSpirituality(Player player, float value) {
        float before = getSpirituality(player);
        float after = setSpiritualityQuietly(player, value);
        if (after < before) Digestion.onSpiritualitySpent(player, before - after);
    }

    /** Sets spirituality without feeding digestion (commands, a lower maximum after a Sequence change). Returns the stored value. */
    public static float setSpiritualityQuietly(Player player, float value) {
        float clamped = Math.min(Beyonder.maxSpirituality(player), Math.max(0, value));
        getModData(player).putFloat("spirituality", clamped);
        return clamped;
    }

    // =========================
    // Digestion of the current Sequence: 0..100 percent (Digestion, DigestionRates)
    // =========================
    public static float getDigestion(Player player) {
        return getModData(player).getFloatOr("digestion", 0f);
    }

    public static void setDigestion(Player player, float percent) {
        getModData(player).putFloat("digestion", Math.min(100f, Math.max(0f, percent)));
    }

    // =========================
    // Pathway ("none" until one is chosen)
    // =========================
    public static String getPathway(Player player) {
        CompoundTag tag = getModData(player);

        if (!tag.contains("pathway")) {
            tag.putString("pathway", "none");
        }

        return tag.getStringOr("pathway", "");
    }


    public static void setPathway(Player player, String pathwayName) {
        getModData(player).putString("pathway", pathwayName);
    }

    // =========================
    // Copy data on death
    // =========================
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        CompoundTag oldData = event.getOriginal().getPersistentData();
        event.getEntity().getPersistentData().put(MOD_TAG, oldData.getCompoundOrEmpty(MOD_TAG));
    }
}
