package net.lukario.circleofinevitability.ritual;

import com.mojang.serialization.MapCodec;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class RitualTableBlock extends Block {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, CircleOfInevitability.MOD_ID);
    private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "ritual_table"));
    public static final RegistryObject<RitualTableBlock> TABLE = BLOCKS.register("ritual_table", () ->
            new RitualTableBlock(Properties.of().setId(KEY).strength(2.5F).sound(SoundType.DEEPSLATE).noOcclusion()));
    private static final MapCodec<RitualTableBlock> CODEC = simpleCodec(RitualTableBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(column(14, 0, 3), column(8, 3, 11), column(16, 11, 15));

    private RitualTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<RitualTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
            RitualTableMenu.open(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
