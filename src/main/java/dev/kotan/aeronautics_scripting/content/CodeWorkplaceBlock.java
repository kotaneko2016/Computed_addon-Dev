package dev.kotan.aeronautics_scripting.content;

import com.mojang.serialization.MapCodec;
import dev.kotan.aeronautics_scripting.network.CodeWorkplaceNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CodeWorkplaceBlock extends BaseEntityBlock {
    public static final MapCodec<CodeWorkplaceBlock> CODEC = simpleCodec(CodeWorkplaceBlock::new);
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0;

    public CodeWorkplaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CodeWorkplaceBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
            || !serverPlayer.mayBuild()
            || !level.mayInteract(serverPlayer, pos)
            || serverPlayer.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_INTERACTION_DISTANCE_SQUARED
            || !(level.getBlockEntity(pos) instanceof CodeWorkplaceBlockEntity workplace)) {
            return InteractionResult.FAIL;
        }

        CodeWorkplaceNetworking.openEditor(serverPlayer, pos, workplace.getProgram(), workplace.getRevision());
        return InteractionResult.SUCCESS;
    }
}
