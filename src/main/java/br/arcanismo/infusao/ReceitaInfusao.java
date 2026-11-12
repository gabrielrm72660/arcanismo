package br.arcanismo.infusao;

import static br.arcanismo.aspecto.Aspecto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import br.arcanismo.aspecto.ListaAspectos;
import br.arcanismo.conhecimento.Pesquisa;
import br.arcanismo.item.CristalItem;
import br.arcanismo.registro.ModItens;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Receita do Altar de Infusão: item central + itens nos pedestais (em qualquer ordem)
 * + essência retirada dos caldeirões próximos + Vis do cajado.
 */
public record ReceitaInfusao(Predicate<ItemStack> centro, List<Predicate<ItemStack>> pedestais,
                             ListaAspectos essencia, int vis, Supplier<ItemStack> resultado,
                             Pesquisa pesquisa, int instabilidade) {

    private static Predicate<ItemStack> i(Item item) { return s -> s.is(item); }
    private static Predicate<ItemStack> i(Supplier<? extends Item> item) { return s -> s.is(item.get()); }
    private static final Predicate<ItemStack> CRISTAL = s -> s.getItem() instanceof CristalItem;

    public boolean combina(ItemStack central, List<ItemStack> itens) {
        if (!centro.test(central) || itens.size() != pedestais.size()) return false;
        boolean[] usado = new boolean[itens.size()];
        for (Predicate<ItemStack> p : pedestais) {
            boolean achou = false;
            for (int k = 0; k < itens.size(); k++) {
                if (!usado[k] && p.test(itens.get(k))) {
                    usado[k] = true;
                    achou = true;
                    break;
                }
            }
            if (!achou) return false;
        }
        return true;
    }

    private static List<ReceitaInfusao> todas;

    public static List<ReceitaInfusao> todas() {
        if (todas != null) return todas;
        List<ReceitaInfusao> l = new ArrayList<>();

        l.add(new ReceitaInfusao(i(ModItens.CAJADO_ARCANITA),
                List.of(CRISTAL, CRISTAL, CRISTAL, CRISTAL, i(Items.DIAMOND), i(Items.DIAMOND)),
                ListaAspectos.de(MAGIA, 12, ENERGIA, 10), 60,
                () -> new ItemStack(ModItens.CAJADO_CRISTAL.get()), Pesquisa.CAJADO_CRISTAL, 3));

        l.add(new ReceitaInfusao(i(Items.GOLDEN_HELMET),
                List.of(i(ModItens.LENTE_ARCANA), i(Items.GOLD_INGOT), i(Items.GOLD_INGOT), i(Items.AMETHYST_SHARD)),
                ListaAspectos.de(ORDEM, 8, MAGIA, 6), 30,
                () -> new ItemStack(ModItens.OCULOS.get()), Pesquisa.OCULOS, 1));

        l.add(new ReceitaInfusao(i(Items.LEATHER_BOOTS),
                List.of(i(Items.FEATHER), i(Items.FEATHER), i(ModItens.cristal(AR)), i(ModItens.cristal(AR))),
                ListaAspectos.de(AR, 12, VIDA, 4), 25,
                () -> new ItemStack(ModItens.BOTAS.get()), Pesquisa.BOTAS, 1));

        l.add(new ReceitaInfusao(i(Items.DIAMOND_SWORD),
                List.of(i(ModItens.cristal(ENTROPIA)), i(ModItens.cristal(ENTROPIA)), i(Items.OBSIDIAN), i(Items.ENDER_PEARL)),
                ListaAspectos.de(ENTROPIA, 16, MAGIA, 8), 50,
                () -> new ItemStack(ModItens.LAMINA_VAZIO.get()), Pesquisa.LAMINA_VAZIO, 5));

        todas = l;
        return todas;
    }

    public static ReceitaInfusao encontrar(ItemStack central, List<ItemStack> itens) {
        for (ReceitaInfusao r : todas()) if (r.combina(central, itens)) return r;
        return null;
    }
}
