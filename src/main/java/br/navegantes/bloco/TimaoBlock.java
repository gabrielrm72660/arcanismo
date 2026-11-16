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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Timão (roda de leme). Ao ser colocado — ou ao receber um clique — tenta transformar a
 * estrutura colada ao redor dele num navio. A roda fica virada para quem o colocou, então a proa
 * aponta para o lado oposto: coloque-o olhando para a frente do barco.
 */
public class TimaoBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape FORMA = Block.box(3, 0, 3, 13, 15, 13);

    public TimaoBlock(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        return defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }

    @Override
    public void setPlacedBy(Level nivel, BlockPos pos, BlockState estado, LivingEntity quem, ItemStack pilha) {
        super.setPlacedBy(nivel, pos, estado, quem, pilha);
        if (nivel instanceof ServerLevel servidor && quem instanceof Player jogador) {
            Montador.montar(servidor, pos, jogador);
        }
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
