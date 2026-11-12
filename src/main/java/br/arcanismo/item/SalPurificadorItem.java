package br.arcanismo.item;

import br.arcanismo.registro.ModBlocos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Sal Purificador: transforma Terra Corrompida em grama num raio de 3 blocos. */
public class SalPurificadorItem extends Item {
    private static final int RAIO = 3;

    public SalPurificadorItem(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level nivel = ctx.getLevel();
        if (nivel.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel servidor = (ServerLevel) nivel;
        Player jogador = ctx.getPlayer();
        BlockPos centro = ctx.getClickedPos();

        int purificados = 0;
        for (BlockPos p : BlockPos.betweenClosed(centro.offset(-RAIO, -RAIO, -RAIO), centro.offset(RAIO, RAIO, RAIO))) {
            if (servidor.getBlockState(p).is(ModBlocos.TERRA_CORROMPIDA.get())) {
                servidor.setBlockAndUpdate(p, Blocks.GRASS_BLOCK.defaultBlockState());
                servidor.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5,
                        4, 0.3, 0.1, 0.3, 0.0);
                purificados++;
            }
        }
        if (purificados == 0) return InteractionResult.PASS;

        servidor.playSound(null, centro, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.7f);
        if (jogador != null) ctx.getItemInHand().consume(1, jogador);
        return InteractionResult.SUCCESS;
    }
}
