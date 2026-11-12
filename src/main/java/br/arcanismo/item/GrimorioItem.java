package br.arcanismo.item;

import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.conhecimento.Conhecimento;
import br.arcanismo.conhecimento.Pesquisa;
import br.arcanismo.grimorio.GrimorioMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Grimório Arcano.
 *  - Clique direito: abre o livro (receitas e o mapa de pesquisas).
 *  - Agachado + clique direito: resumo do seu conhecimento no chat.
 */
public class GrimorioItem extends Item {
    private static final Component TITULO = Component.literal("Grimório Arcano");

    public GrimorioItem(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public InteractionResult use(Level nivel, Player jogador, InteractionHand mao) {
        if (nivel.isClientSide()) return InteractionResult.SUCCESS;

        if (!Conhecimento.tem(jogador, Pesquisa.FUNDAMENTOS)) {
            Conhecimento.pesquisar(jogador, Pesquisa.FUNDAMENTOS);
        }

        if (jogador.isShiftKeyDown()) {
            mostrarResumo(jogador);
        } else {
            jogador.openMenu(new SimpleMenuProvider((id, inventario, quem) -> new GrimorioMenu(id, inventario), TITULO));
        }
        return InteractionResult.SUCCESS;
    }

    private static void mostrarResumo(Player jogador) {
        MutableComponent pontos = Component.literal("Conhecimento: ").withStyle(ChatFormatting.GRAY);
        for (Aspecto a : Aspecto.values()) {
            int v = Conhecimento.pontos(jogador, a);
            if (v > 0) pontos.append(a.texto().append(Component.literal(" " + v + "  ")));
        }
        jogador.sendSystemMessage(pontos);
        int feitas = 0;
        for (Pesquisa p : Pesquisa.values()) if (Conhecimento.tem(jogador, p)) feitas++;
        jogador.sendSystemMessage(Component.literal("Pesquisas: " + feitas + "/" + Pesquisa.values().length
                + " · escaneados: " + Conhecimento.totalEscaneados(jogador)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
