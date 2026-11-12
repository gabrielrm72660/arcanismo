package br.arcanismo.infusao;

import br.arcanismo.item.CajadoItem;
import br.arcanismo.registro.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Pedestal Arcano: segura um ingrediente para a infusão. Clique com um item para pôr, mão vazia para tirar. */
public class PedestalBlock extends Block implements EntityBlock {
    private static final VoxelShape FORMA = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14), Block.box(5, 3, 5, 11, 12, 11), Block.box(3, 12, 3, 13, 15, 13));

    public PedestalBlock(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }

    static InteractionResult colocar(SuporteBlockEntity be, ItemStack pilha, Player jogador, Level nivel) {
        if (!be.item().isEmpty() || pilha.isEmpty() || pilha.getItem() instanceof CajadoItem) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!nivel.isClientSide()) {
            be.definirItem(pilha.copyWithCount(1));
            pilha.consume(1, jogador);
        }
        return InteractionResult.SUCCESS;
    }

    static InteractionResult retirar(SuporteBlockEntity be, Player jogador, Level nivel) {
        if (be.item().isEmpty()) return InteractionResult.PASS;
        if (!nivel.isClientSide()) {
            ItemStack devolver = be.item().copy();
            be.definirItem(ItemStack.EMPTY);
            if (!jogador.getInventory().add(devolver)) jogador.drop(devolver, false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack pilha, BlockState estado, Level nivel, BlockPos pos,
                                          Player jogador, InteractionHand mao, BlockHitResult acerto) {
        if (nivel.getBlockEntity(pos) instanceof SuporteBlockEntity be) return colocar(be, pilha, jogador, nivel);
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jogador, BlockHitResult acerto) {
        if (nivel.getBlockEntity(pos) instanceof SuporteBlockEntity be) return retirar(be, jogador, nivel);
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level nivel, BlockPos pos, BlockState estado, Player jogador) {
        if (nivel instanceof ServerLevel s && nivel.getBlockEntity(pos) instanceof SuporteBlockEntity be) {
            if (!be.item().isEmpty()) Block.popResource(nivel, pos, be.item().copy());
            Vitrine.limpar(s, pos);
        }
        return super.playerWillDestroy(nivel, pos, estado, jogador);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return new PedestalBlockEntity(pos, estado);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level nivel, BlockState estado, BlockEntityType<T> tipo) {
        if (nivel.isClientSide() || tipo != ModBlockEntities.PEDESTAL.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<PedestalBlockEntity>) PedestalBlockEntity::tick;
    }
}
