package br.navegantes.bloco;

import br.navegantes.navio.Montador;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Timão (roda de leme), com 2 blocos de altura: pedestal embaixo e a roda em cima. Ao ser colocado
 * — ou ao receber um clique — tenta transformar a estrutura colada ao redor dele num navio. A roda
 * fica virada para quem o colocou, então a proa aponta para o lado oposto.
 */
public class TimaoBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape BAIXO = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape ALTO = Block.box(3, 0, 3, 13, 14, 13);

    public TimaoBlock(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(FACING, HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        BlockPos pos = contexto.getClickedPos();
        if (!contexto.getLevel().getBlockState(pos.above()).canBeReplaced(contexto)) return null;
        return defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
        return estado.getValue(HALF) == DoubleBlockHalf.LOWER ? BAIXO : ALTO;
    }

    @Override
    public void setPlacedBy(Level nivel, BlockPos pos, BlockState estado, LivingEntity quem, ItemStack pilha) {
        super.setPlacedBy(nivel, pos, estado, quem, pilha);
        nivel.setBlock(pos.above(), estado.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        if (nivel instanceof ServerLevel servidor && quem instanceof Player jogador) {
            Montador.montar(servidor, pos, jogador);
        }
    }

    /** Quebrar uma metade leva a outra junto (e o item cai uma vez só). */
    @Override
    public BlockState playerWillDestroy(Level nivel, BlockPos pos, BlockState estado, Player jogador) {
        if (!nivel.isClientSide()) {
            boolean baixo = estado.getValue(HALF) == DoubleBlockHalf.LOWER;
            BlockPos outra = baixo ? pos.above() : pos.below();
            BlockState outroEstado = nivel.getBlockState(outra);
            if (outroEstado.is(this) && outroEstado.getValue(HALF) != estado.getValue(HALF)) {
                nivel.setBlock(outra, Blocks.AIR.defaultBlockState(), 35);
                if (!baixo && !jogador.getAbilities().instabuild) {
                    Block.popResource(nivel, pos, new ItemStack(this));
                }
            }
        }
        return super.playerWillDestroy(nivel, pos, estado, jogador);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos,
                                               Player jogador, BlockHitResult acerto) {
        if (nivel instanceof ServerLevel servidor) {
            Montador.montar(servidor, pos, jogador);
        }
        return InteractionResult.SUCCESS;
    }
}
