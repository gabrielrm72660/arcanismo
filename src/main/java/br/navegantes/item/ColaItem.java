package br.navegantes.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import br.navegantes.navio.Cola;
import br.navegantes.navio.Mensagens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Cola Naval. Clique em dois cantos opostos: tudo o que houver dentro da caixa fica colado.
 * Só blocos colados (e o próprio Timão) entram no navio. Agachado, o clique cancela a marcação.
 */
public class ColaItem extends Item {
    private record Marca(ResourceKey<Level> dimensao, BlockPos pos) {}

    private static final Map<UUID, Marca> MARCAS = new HashMap<>();
    public static final int LADO_MAXIMO = 24;

    public ColaItem(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public InteractionResult useOn(UseOnContext contexto) {
        Player jogador = contexto.getPlayer();
        if (jogador == null) return InteractionResult.PASS;
        if (!(contexto.getLevel() instanceof ServerLevel nivel)) return InteractionResult.SUCCESS;

        BlockPos clicado = contexto.getClickedPos();
        Marca primeira = MARCAS.get(jogador.getUUID());

        if (jogador.isShiftKeyDown()) {
            if (primeira != null) {
                MARCAS.remove(jogador.getUUID());
                Mensagens.barra(jogador, "Marcação da cola cancelada.");
            }
            return InteractionResult.SUCCESS;
        }

        if (primeira == null || !primeira.dimensao().equals(nivel.dimension())) {
            MARCAS.put(jogador.getUUID(), new Marca(nivel.dimension(), clicado.immutable()));
            Mensagens.barra(jogador, "Primeiro canto marcado. Clique no canto oposto.");
            return InteractionResult.SUCCESS;
        }

        BlockPos a = primeira.pos();
        int x0 = Math.min(a.getX(), clicado.getX()), x1 = Math.max(a.getX(), clicado.getX());
        int y0 = Math.min(a.getY(), clicado.getY()), y1 = Math.max(a.getY(), clicado.getY());
        int z0 = Math.min(a.getZ(), clicado.getZ()), z1 = Math.max(a.getZ(), clicado.getZ());
        if (x1 - x0 >= LADO_MAXIMO || y1 - y0 >= LADO_MAXIMO || z1 - z0 >= LADO_MAXIMO) {
            Mensagens.barra(jogador, "Caixa grande demais (máximo " + LADO_MAXIMO + " blocos de lado).");
            MARCAS.remove(jogador.getUUID());
            return InteractionResult.SUCCESS;
        }

        List<BlockPos> novos = new ArrayList<>();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState estado = nivel.getBlockState(p);
                    if (!estado.isAir() && !(estado.getBlock() instanceof LiquidBlock)) novos.add(p);
                }
            }
        }
        MARCAS.remove(jogador.getUUID());
        if (novos.isEmpty()) {
            Mensagens.barra(jogador, "Não há blocos dentro da caixa.");
            return InteractionResult.SUCCESS;
        }
        Cola.de(nivel).adicionar(novos);

        int mostrar = Math.min(novos.size(), 80);
        for (int i = 0; i < mostrar; i++) {
            BlockPos p = novos.get(i * novos.size() / mostrar);
            nivel.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    1, 0.3, 0.3, 0.3, 0.0);
        }
        Mensagens.barra(jogador, novos.size() + " blocos colados. Coloque o Timão para montar o navio.");
        if (!jogador.getAbilities().instabuild) {
            contexto.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
