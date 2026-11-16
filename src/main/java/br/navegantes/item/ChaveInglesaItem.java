package br.navegantes.item;

import br.navegantes.navio.NavioEntidade;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Chave Inglesa. Clique com o botão direito no navio (em qualquer bloco dele) para devolvê-lo ao
 * estado de blocos: assim dá para editar a estrutura. Depois é só clicar no Timão (ou colocá-lo
 * de novo) para montar o navio outra vez. Blocos novos precisam ser colados com a Cola Naval.
 */
public class ChaveInglesaItem extends Item {
    public static final double ALCANCE = 6.0;

    public ChaveInglesaItem(Properties propriedades) {
        super(propriedades);
    }

    private static InteractionResult tentar(Level nivel, Player jogador) {
        NavioEntidade navio = NavioEntidade.acharNoRaio(nivel, jogador, ALCANCE);
        if (navio == null) return InteractionResult.PASS;
        if (!nivel.isClientSide()) navio.desmontar(jogador);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level nivel, Player jogador, InteractionHand mao) {
        return tentar(nivel, jogador);
    }

    @Override
    public InteractionResult useOn(UseOnContext contexto) {
        Player jogador = contexto.getPlayer();
        if (jogador == null) return InteractionResult.PASS;
        return tentar(contexto.getLevel(), jogador);
    }
}
