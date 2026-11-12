package br.arcanismo.grimorio;

import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.conhecimento.Conhecimento;
import br.arcanismo.conhecimento.Pesquisa;
import br.arcanismo.registro.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * "Menu" do Grimório. Não tem espaços de item: serve para sincronizar com o cliente
 * os pontos de conhecimento e as pesquisas concluídas (dados do servidor), e para
 * receber o pedido de pesquisa quando o jogador clica no botão do mapa.
 *
 * Dados: 0..9 pontos de cada aspecto; 10..12 pesquisas concluídas (12 bits cada).
 */
public class GrimorioMenu extends AbstractContainerMenu {
    private static final int BITS = 12;
    private static final int N_ASPECTOS = Aspecto.values().length;
    private static final int N_BLOCOS = (Pesquisa.values().length + BITS - 1) / BITS;

    private final Player jogador;
    private final SimpleContainerData dados = new SimpleContainerData(N_ASPECTOS + N_BLOCOS);

    public GrimorioMenu(int id, Inventory inventario) {
        super(ModMenus.GRIMORIO.get(), id);
        this.jogador = inventario.player;
        addDataSlots(dados);
        if (!jogador.level().isClientSide()) atualizar();
    }

    private void atualizar() {
        for (Aspecto a : Aspecto.values()) {
            dados.set(a.ordinal(), Math.min(30000, Conhecimento.pontos(jogador, a)));
        }
        for (int b = 0; b < N_BLOCOS; b++) {
            int mascara = 0;
            for (int i = 0; i < BITS; i++) {
                int idx = b * BITS + i;
                if (idx < Pesquisa.values().length && Conhecimento.tem(jogador, Pesquisa.values()[idx])) {
                    mascara |= 1 << i;
                }
            }
            dados.set(N_ASPECTOS + b, mascara);
        }
    }

    // ------------------------------------------------------------------ leitura (cliente)
    public int pontos(Aspecto a) {
        return dados.get(a.ordinal());
    }

    public boolean tem(Pesquisa p) {
        int idx = p.ordinal();
        return (dados.get(N_ASPECTOS + idx / BITS) & (1 << (idx % BITS))) != 0;
    }

    public boolean requisitosOk(Pesquisa p) {
        for (Pesquisa r : p.requisitos) if (!tem(r)) return false;
        return true;
    }

    public boolean podePagar(Pesquisa p) {
        for (var e : p.custo.mapa().entrySet()) if (pontos(e.getKey()) < e.getValue()) return false;
        return true;
    }

    // ------------------------------------------------------------------ pedido de pesquisa (servidor)
    @Override
    public boolean clickMenuButton(Player quem, int botao) {
        if (botao < 0 || botao >= Pesquisa.values().length) return false;
        Pesquisa p = Pesquisa.values()[botao];
        if (Conhecimento.pesquisar(quem, p)) {
            quem.sendSystemMessage(Component.literal("✦ Pesquisa concluída: " + p.nome)
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            quem.level().playSound(null, quem.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            atualizar();
            broadcastChanges();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player quem, int indice) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player quem) {
        return true;
    }
}
