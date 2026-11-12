package net.lukario.circleofinevitability.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Shared natural-ground classification; inspecting terrain never loads chunks or edits blocks. */
public final class Terrain {
    public enum Kind { SOIL, STONE, WOOD, WATER, SAND, SNOW, OTHER, NONE }
    private Terrain() {}
    public static Kind at(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return Kind.NONE;
        var state = level.getBlockState(pos);
        if (state.getFluidState().is(FluidTags.WATER)) return Kind.WATER;
        if (state.isAir()) return Kind.NONE;
        if (state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND) || state.is(Blocks.MUD)
                || state.is(Blocks.MOSS_BLOCK)) return Kind.SOIL;
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(Blocks.COBBLESTONE)) return Kind.STONE;
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) return Kind.WOOD;
        if (state.is(BlockTags.SAND)) return Kind.SAND;
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) return Kind.SNOW;
        return Kind.OTHER;
    }
    public static Kind under(LivingEntity entity) { return at(entity.level(), BlockPos.containing(entity.position().add(0,-.1,0))); }
    public static boolean supported(Level level, Vec3 feet) {
        BlockPos below = BlockPos.containing(feet.add(0,-.1,0));
        return level.hasChunkAt(below) && level.hasChunkAt(BlockPos.containing(feet))
                && level.getWorldBorder().isWithinBounds(below)
                && !level.getBlockState(below).getCollisionShape(level,below).isEmpty();
    }
}
