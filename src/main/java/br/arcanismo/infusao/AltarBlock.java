package br.arcanismo.infusao;

import br.arcanismo.item.CajadoItem;
import br.arcanismo.registro.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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

/** Altar de Infusão. Item na mão: põe no altar. Cajado: inicia a infusão. Mão vazia: retira. */
public class AltarBlock extends Block implements EntityBlock {
    private static final VoxelShape FORMA = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 12, 14), Block.box(0, 12, 0, 16, 16, 16));

    public AltarBlock(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack pilha, BlockState estado, Level nivel, BlockPos pos,
                                          Player jogador, InteractionHand mao, BlockHitResult acerto) {
        if (!(nivel.getBlockEntity(pos) instanceof AltarBlockEntity be)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (pilha.getItem() instanceof CajadoItem) {
            if (!nivel.isClientSide()) be.iniciar(jogador, pilha);
            return InteractionResult.SUCCESS;
        }
        if (be.ativo()) return InteractionResult.FAIL;
        return PedestalBlock.colocar(be, pilha, jogador, nivel);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jogador, BlockHitResult acerto) {
        if (nivel.getBlockEntity(pos) instanceof AltarBlockEntity be && !be.ativo()) {
            return PedestalBlock.retirar(be, jogador, nivel);
        }
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
    public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource r) {
        if (r.nextInt(4) == 0) {
            nivel.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5,
                    (r.nextDouble() - 0.5) * 2, -0.5, (r.nextDouble() - 0.5) * 2);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return new AltarBlockEntity(pos, estado);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level nivel, BlockState estado, BlockEntityType<T> tipo) {
        if (nivel.isClientSide() || tipo != ModBlockEntities.ALTAR.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<AltarBlockEntity>) AltarBlockEntity::tick;
    }
}
