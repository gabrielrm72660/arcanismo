package br.arcanismo.bloco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Terra Corrompida: o resultado do Fluxo liberado por magia descuidada.
 * Espalha-se para terra e grama vizinhas, mas perde "força" a cada salto,
 * então a corrupção é sempre limitada. Blocos com força 0 não se espalham.
 * Quem pisa nela fica fraco.
 */
public class TerraCorrompidaBlock extends Block {
    public static final IntegerProperty FORCA = IntegerProperty.create("forca", 0, 3);

    public TerraCorrompidaBlock(Properties propriedades) {
        super(propriedades);
        registerDefaultState(stateDefinition.any().setValue(FORCA, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(FORCA);
    }

    @Override
    protected void randomTick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource aleatorio) {
        int forca = estado.getValue(FORCA);
        if (forca <= 0 || aleatorio.nextInt(3) != 0) return;

        BlockPos alvo = pos.offset(aleatorio.nextInt(3) - 1, aleatorio.nextInt(3) - 1, aleatorio.nextInt(3) - 1);
        BlockState estadoAlvo = nivel.getBlockState(alvo);
        if (estadoAlvo.is(BlockTags.DIRT) && !estadoAlvo.is(this)) {
            nivel.setBlockAndUpdate(alvo, defaultBlockState().setValue(FORCA, forca - 1));
        }
    }

    @Override
    public void stepOn(Level nivel, BlockPos pos, BlockState estado, Entity entidade) {
        if (!nivel.isClientSide() && entidade instanceof LivingEntity vivo && nivel.getGameTime() % 20 == 0) {
            vivo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
        super.stepOn(nivel, pos, estado, entidade);
    }

    @Override
    public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource aleatorio) {
        if (aleatorio.nextInt(4) == 0) {
            nivel.addParticle(ParticleTypes.WITCH, pos.getX() + aleatorio.nextDouble(), pos.getY() + 1.05,
                    pos.getZ() + aleatorio.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
