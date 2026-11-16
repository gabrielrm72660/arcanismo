package br.navegantes.bloco;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Fornalha a Vapor: o motor a combustão do navio. Parece a fornalha de minérios.
 * Montada num navio, queima carvão (clique com o carvão na mão) para empurrar o casco.
 */
public class FornalhaBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FornalhaBlock(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        return defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite());
    }
}
