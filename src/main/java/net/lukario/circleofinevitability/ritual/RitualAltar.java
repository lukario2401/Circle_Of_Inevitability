package net.lukario.circleofinevitability.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** Measured from the table each time a rite is performed. Nothing is owned by the builder. */
record RitualAltar(int rank, int limit, String pattern, List<Missing> missing) {
    record Missing(BlockPos pos, Block block) {
        String description() { return block.getName().getString() + " at " + pos.toShortString(); }

        void mark(ServerPlayer player) {
            player.level().sendParticles(player, ParticleTypes.END_ROD, false, true, pos.getX() + 0.5,
                    pos.getY() + 0.65, pos.getZ() + 0.5, 6, 0.2, 0.15, 0.2, 0);
        }
    }

    static RitualAltar inspect(Level level, BlockPos pos) {
        List<Missing> missing = new ArrayList<>();
        if (!level.getBlockState(pos).is(RitualTableBlock.TABLE.get())) {
            return new RitualAltar(0, 0, "Place a Ritual Table.", List.of());
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                require(level, pos.offset(x, -1, z), Blocks.POLISHED_DEEPSLATE, missing);
            }
        }
        if (!missing.isEmpty()) return new RitualAltar(1, 25,
                "Center a solid 3×3 polished deepslate floor directly beneath the table.", missing);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos pedestal = pos.relative(side, 2);
            require(level, pedestal, Blocks.CHISELED_DEEPSLATE, missing);
            require(level, pedestal.above(), Blocks.SOUL_LANTERN, missing);
        }
        if (!missing.isEmpty()) return new RitualAltar(2, 60,
                "Two blocks north, east, south and west of the table, place chiseled deepslate at table height with a soul lantern on top.", missing);
        return new RitualAltar(3, 100, "The four lanterns surround a complete sanctuary.", List.of());
    }

    private static void require(Level level, BlockPos pos, Block block, List<Missing> missing) {
        if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(block)) {
            missing.add(new Missing(pos, block));
        }
    }
}
