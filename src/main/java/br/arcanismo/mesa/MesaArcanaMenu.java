package br.arcanismo.mesa;

import java.util.ArrayList;
import java.util.List;

import br.arcanismo.conhecimento.Conhecimento;
import br.arcanismo.item.CajadoItem;
import br.arcanismo.registro.ModBlocos;
import br.arcanismo.registro.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu da Mesa Arcana: grade 3x3, encaixe do cajado (fonte de Vis) e resultado.
 * Índices: 0..8 grade, 9 cajado, 10 resultado, 11..46 inventário do jogador.
 */
public class MesaArcanaMenu extends AbstractContainerMenu {
    public static final int CAJADO = 9, RESULTADO = 10, INV_INICIO = 11, INV_FIM = 47;

    private final ContainerLevelAccess acesso;
    private final Player jogador;
    final SimpleContainer entrada = new SimpleContainer(10) {
        @Override
        public void setChanged() {
            super.setChanged();
            MesaArcanaMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer saida = new ResultContainer();

    /** Construtor do cliente. */
    public MesaArcanaMenu(int id, Inventory inventario) {
        this(id, inventario, ContainerLevelAccess.NULL);
    }

    public MesaArcanaMenu(int id, Inventory inventario, ContainerLevelAccess acesso) {
        super(ModMenus.MESA_ARCANA.get(), id);
        this.acesso = acesso;
        this.jogador = inventario.player;

        for (int l = 0; l < 3; l++) {
            for (int c = 0; c < 3; c++) {
                addSlot(new Slot(entrada, l * 3 + c, 48 + c * 18, 18 + l * 18));
            }
        }
        addSlot(new Slot(entrada, CAJADO, 16, 36) {
            @Override
            public boolean mayPlace(ItemStack pilha) {
                return pilha.getItem() instanceof CajadoItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(saida, 0, 134, 36) {
            @Override
            public boolean mayPlace(ItemStack pilha) {
                return false;
            }

            @Override
            public void onTake(Player quem, ItemStack pilha) {
                consumir();
                super.onTake(quem, pilha);
            }
        });

        for (int l = 0; l < 3; l++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inventario, c + l * 9 + 9, 8 + c * 18, 84 + l * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inventario, c, 8 + c * 18, 142));
        }
    }

    public List<ItemStack> grade() {
        List<ItemStack> g = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) g.add(entrada.getItem(i));
        return g;
    }

    public ItemStack cajado() {
        return entrada.getItem(CAJADO);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        atualizarResultado();
    }

    private void atualizarResultado() {
        if (jogador.level().isClientSide()) return;
        ReceitaArcana r = ReceitaArcana.encontrar(grade());
        ItemStack res = ItemStack.EMPTY;
        if (r != null && (r.pesquisa() == null || Conhecimento.tem(jogador, r.pesquisa()))) {
            ItemStack cajado = cajado();
            boolean temVis = r.vis() == 0
                    || (cajado.getItem() instanceof CajadoItem && CajadoItem.vis(cajado) >= r.vis());
            if (temVis) res = r.resultado().get();
        }
        saida.setItem(0, res);
        broadcastChanges();
    }

    /** Gasta os ingredientes e o Vis quando o jogador pega o resultado. */
    private void consumir() {
        ReceitaArcana r = ReceitaArcana.encontrar(grade());
        if (r != null && r.vis() > 0) {
            ItemStack cajado = cajado();
            if (cajado.getItem() instanceof CajadoItem c) c.adicionarVis(cajado, -r.vis());
        }
        for (int i = 0; i < 9; i++) {
            ItemStack s = entrada.getItem(i);
            if (!s.isEmpty()) s.shrink(1);
        }
        entrada.setChanged();
        atualizarResultado();
    }

    @Override
    public ItemStack quickMoveStack(Player quem, int indice) {
        Slot slot = slots.get(indice);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack pilha = slot.getItem();
        ItemStack copia = pilha.copy();

        if (indice == RESULTADO) {
            if (!moveItemStackTo(pilha, INV_INICIO, INV_FIM, true)) return ItemStack.EMPTY;
            slot.onTake(quem, copia);
            return ItemStack.EMPTY;
        } else if (indice < INV_INICIO) {
            if (!moveItemStackTo(pilha, INV_INICIO, INV_FIM, false)) return ItemStack.EMPTY;
        } else if (pilha.getItem() instanceof CajadoItem && !slots.get(CAJADO).hasItem()) {
            if (!moveItemStackTo(pilha, CAJADO, CAJADO + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(pilha, 0, 9, false)) {
            return ItemStack.EMPTY;
        }

        if (pilha.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copia;
    }

    @Override
    public boolean stillValid(Player quem) {
        return stillValid(acesso, quem, ModBlocos.MESA_ARCANA.get());
    }

    @Override
    public void removed(Player quem) {
        super.removed(quem);
        acesso.execute((nivel, pos) -> clearContainer(quem, entrada));
    }
}
