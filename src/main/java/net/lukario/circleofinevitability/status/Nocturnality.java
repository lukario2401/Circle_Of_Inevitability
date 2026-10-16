package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Nocturnality: how dark it is for an entity, as one number, and night forced on a place or a body. For every
 * ability or passive that is stronger (or weaker) in the dark. Works from any class/ability:
 *
 *   float dark = Nocturnality.darkness(entity);        // 0 = full light ... 1 = pitch black
 *   if (Nocturnality.isDark(entity)) ...               // at least DARK: "full dark"
 *   Nocturnality.forceNight(sl, center, 20, 800);      // 40 seconds of night 20 blocks round a spot (a Night Domain)
 *   Nocturnality.forceNight(entity, 200);              // 10 seconds of night that goes with the entity
 *   Nocturnality.isForcedNight(entity)
 *   Nocturnality.inDaySun(entity)                      // bright day, open sky, no forced night: the dark's enemy
 *
 * The darkness is the light where it stands (block light, and sky light, which counts for a quarter by night),
 * or 1 inside forced night. Spells.inDaylight is the other half of this.
 *
 * Forced night on a place is not saved (it ends with a restart); on an entity it is saved with it. It changes only
 * what darkness() says: the sky players see is not touched.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Nocturnality {

    /** This dark (0..1) or darker counts as "full dark" (isDark). */
    public static float DARK = 0.75f;

    private static final String TAG = "circle_of_inevitability_forced_night";       // game time it ends

    private record Night(ServerLevel level, Vec3 center, double radius, long until) {}

    private static final List<Night> NIGHTS = new ArrayList<>();

    private Nocturnality() {}

    // =========================
    // API
    // =========================

    /** 0 (full light) to 1 (pitch black), where the entity stands; 1 inside forced night. */
    public static float darkness(Entity entity) {
        if (isForcedNight(entity)) return 1f;
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos);
        if (level.isDarkOutside()) sky /= 4;
        return 1f - Math.max(block, sky) / 15f;
    }

    /** At least DARK: full dark. */
    public static boolean isDark(Entity entity) {
        return darkness(entity) >= DARK;
    }

    /** Night on everyone within 'radius' of 'center' for 'ticks'. */
    public static void forceNight(ServerLevel level, Vec3 center, double radius, int ticks) {
        if (ticks <= 0) return;
        NIGHTS.add(new Night(level, center, radius, level.getGameTime() + ticks));
    }

    /** Night that goes with this entity for 'ticks' (never shortens one it has). */
    public static void forceNight(Entity entity, int ticks) {
        if (entity.level().isClientSide() || ticks <= 0) return;
        long until = entity.level().getGameTime() + ticks;
        if (entity.getPersistentData().getLongOr(TAG, 0L) < until) entity.getPersistentData().putLong(TAG, until);
    }

    /** Ends the night on this entity (not the places it stands in). */
    public static void endNight(Entity entity) {
        entity.getPersistentData().remove(TAG);
    }

    public static boolean isForcedNight(Entity entity) {
        long now = entity.level().getGameTime();
        if (entity.getPersistentData().getLongOr(TAG, 0L) > now) return true;
        if (NIGHTS.isEmpty()) return false;
        Vec3 at = entity.position();
        for (Night night : NIGHTS) {
            if (night.level() != entity.level() || night.until() <= now) continue;
            if (at.distanceToSqr(night.center()) <= night.radius() * night.radius()) return true;
        }
        return false;
    }

    /** The bright sun of day on it under the open sky, and no forced night. */
    public static boolean inDaySun(Entity entity) {
        Level level = entity.level();
        BlockPos above = entity.blockPosition().above();
        if (level.isDarkOutside() || !level.canSeeSky(above)) return false;
        return level.getBrightness(LightLayer.SKY, above) >= 15 && !isForcedNight(entity);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onNocturnalityLevelUnload(LevelEvent.Unload event) {
        NIGHTS.removeIf(night -> night.level() == event.getLevel());
    }

    /** Forgets the nights that are over, every 5 seconds. */
    @SubscribeEvent
    public static void onNocturnalityLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (NIGHTS.isEmpty() || !(event.level() instanceof ServerLevel sl) || sl.getGameTime() % 100 != 0) return;
        long now = sl.getGameTime();
        NIGHTS.removeIf(night -> night.level() == sl && night.until() <= now);
    }
}
