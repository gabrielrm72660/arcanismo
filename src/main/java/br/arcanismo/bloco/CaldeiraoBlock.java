package br.arcanismo.bloco;

import br.arcanismo.registro.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Caldeirão Arcano.
 *  1. Clique com balde de água para encher.
 *  2. Coloque fogo, lava, magma ou fogueira acesa embaixo.
 *  3. Jogue itens dentro (tecla Q): eles se dissolvem em essência.
 *  4. Clique com um catalisador para criar algo (veja ReceitaCaldeirao).
 *  Mão vazia: mostra a essência. Agachado + mão vazia: esvazia (libera Fluxo!).
 */
public class CaldeiraoBlock extends Block implements EntityBlock {
    public static final BooleanProperty CHEIO = BooleanProperty.create("cheio");

    private static final VoxelShape FORMA = Shapes.join(
            Shapes.block(), Block.box(2, 4, 2, 14, 16, 14), BooleanOp.ONLY_FIRST);

    public CaldeiraoBlock(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(CHEIO, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(CHEIO);
    }

    @Override
    protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack pilha, BlockState estado, Level nivel, BlockPos pos,
                                          Player jogador, InteractionHand mao, BlockHitResult acerto) {
        if (pilha.is(Items.WATER_BUCKET) && !estado.getValue(CHEIO)) {
            if (!nivel.isClientSide()) {
                nivel.setBlockAndUpdate(pos, estado.setValue(CHEIO, true));
                jogador.setItemInHand(mao, ItemUtils.createFilledResult(pilha, jogador, new ItemStack(Items.BUCKET)));
                nivel.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }

        if (estado.getValue(CHEIO) && !pilha.isEmpty()
                && nivel.getBlockEntity(pos) instanceof CaldeiraoBlockEntity caldeirao) {
            if (nivel.isClientSide()) return InteractionResult.SUCCESS;
            if (caldeirao.tentarCatalisar(jogador, pilha)) return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos,
                                               Player jogador, BlockHitResult acerto) {
        if (nivel.isClientSide()) return InteractionResult.SUCCESS;
        if (nivel.getBlockEntity(pos) instanceof CaldeiraoBlockEntity caldeirao) {
            if (jogador.isShiftKeyDown()) {
                caldeirao.esvaziar((ServerLevel) nivel, jogador);
            } else {
                caldeirao.mostrarConteudo(jogador);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return new CaldeiraoBlockEntity(pos, estado);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level nivel, BlockState estado, BlockEntityType<T> tipo) {
        if (nivel.isClientSide() || tipo != ModBlockEntities.CALDEIRAO.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<CaldeiraoBlockEntity>) CaldeiraoBlockEntity::tick;
    }

    @Override
    public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource aleatorio) {
        if (!estado.getValue(CHEIO) || !CaldeiraoBlockEntity.aquecido(nivel, pos)) return;
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.25 + aleatorio.nextDouble() * 0.5;
            double z = pos.getZ() + 0.25 + aleatorio.nextDouble() * 0.5;
            nivel.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.95, z, 0.0, 0.03, 0.0);
        }
        if (aleatorio.nextInt(3) == 0) {
            nivel.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0, 0.05, 0.0);
        }
        if (aleatorio.nextInt(20) == 0) {
            nivel.playLocalSound(pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.4f, 0.8f, false);
        }
    }
}
