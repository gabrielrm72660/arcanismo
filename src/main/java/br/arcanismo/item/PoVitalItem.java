package br.arcanismo.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Pó Vital: faz uma planta crescer de uma só vez (como várias farinhas de osso). */
public class PoVitalItem extends Item {
    public PoVitalItem(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level nivel = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!(nivel.getBlockState(pos).getBlock() instanceof BonemealableBlock)) return InteractionResult.PASS;
        if (nivel.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel servidor = (ServerLevel) nivel;

        boolean cresceu = false;
        for (int i = 0; i < 10; i++) {
            BlockState estado = servidor.getBlockState(pos);
            if (!(estado.getBlock() instanceof BonemealableBlock planta)
                    || !planta.isValidBonemealTarget(servidor, pos, estado)) break;
            planta.performBonemeal(servidor, servidor.getRandom(), pos, estado);
            cresceu = true;
        }
        if (!cresceu) return InteractionResult.PASS;
        servidor.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                20, 0.4, 0.4, 0.4, 0.0);
        servidor.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.6f);
        if (ctx.getPlayer() != null) ctx.getItemInHand().consume(1, ctx.getPlayer());
        return InteractionResult.SUCCESS;
    }
}
