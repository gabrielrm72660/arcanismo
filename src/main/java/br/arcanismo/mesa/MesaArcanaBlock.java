package br.arcanismo.mesa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Mesa Arcana: onde se fabricam todos os itens do Arcanismo, pagando Vis com o cajado. */
public class MesaArcanaBlock extends Block {
    private static final Component TITULO = Component.translatable("container.arcanismo.mesa_arcana");

    public MesaArcanaBlock(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos,
                                               Player jogador, BlockHitResult acerto) {
        if (!nivel.isClientSide()) {
            jogador.openMenu(new SimpleMenuProvider(
                    (id, inventario, quem) -> new MesaArcanaMenu(id, inventario, ContainerLevelAccess.create(nivel, pos)),
                    TITULO));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource aleatorio) {
        if (aleatorio.nextInt(3) == 0) {
            nivel.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5,
                    aleatorio.nextDouble() - 0.5, -0.6, aleatorio.nextDouble() - 0.5);
        }
    }
}
