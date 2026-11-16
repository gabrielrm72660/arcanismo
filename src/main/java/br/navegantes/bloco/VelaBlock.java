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
 * Vela: painel de lã. Fica de lado para a proa (perpendicular ao sentido em que o jogador olha ao colocar).
 * Cada vela montada num navio ajuda a empurrá-lo.
 */
public class VelaBlock extends Block {
    /** Eixo da "frente" do painel (a direção para onde o pano aponta). */
    public static final EnumProperty<Direction.Axis> EIXO = BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape FORMA_Z = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape FORMA_X = Block.box(7, 0, 0, 9, 16, 16);

    public VelaBlock(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(EIXO, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(EIXO);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        return defaultBlockState().setValue(EIXO, contexto.getHorizontalDirection().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
        return estado.getValue(EIXO) == Direction.Axis.X ? FORMA_X : FORMA_Z;
    }
}
