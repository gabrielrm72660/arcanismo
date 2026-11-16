package br.navegantes.bloco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Assento. O de ponta (o mais perto do Timão) é o do piloto; os outros são lugares de passageiro.
 * O assento olha para onde quem o colocou estava olhando.
 */
public class AssentoBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape FORMA = Block.box(2, 0, 2, 14, 9, 14);

    public AssentoBlock(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        return defaultBlockState().setValue(FACING, contexto.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }
}
