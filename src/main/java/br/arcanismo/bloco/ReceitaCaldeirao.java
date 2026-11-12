package br.arcanismo.bloco;

import static br.arcanismo.aspecto.Aspecto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.aspecto.ListaAspectos;
import br.arcanismo.conhecimento.Pesquisa;
import br.arcanismo.registro.ModItens;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Receita do Caldeirão Arcano: com essência suficiente dissolvida,
 * clicar com o catalisador transforma-o no resultado.
 */
public record ReceitaCaldeirao(Supplier<Item> catalisador, ListaAspectos custo,
                               Supplier<ItemStack> resultado, Pesquisa pesquisa) {

    private static List<ReceitaCaldeirao> todas;

    public static List<ReceitaCaldeirao> todas() {
        if (todas != null) return todas;
        List<ReceitaCaldeirao> l = new ArrayList<>();

        // Pó de Vis: recarrega o cajado
        l.add(new ReceitaCaldeirao(() -> Items.REDSTONE, ListaAspectos.de(ENERGIA, 6, MAGIA, 2),
                () -> new ItemStack(ModItens.PO_DE_VIS.get(), 2), Pesquisa.ALQUIMIA));

        // Cristais refinados: fragmento de ametista + 8 de um aspecto primordial
        Aspecto[] primordiais = {AR, TERRA, FOGO, AGUA, ORDEM, ENTROPIA};
        for (Aspecto a : primordiais) {
            l.add(new ReceitaCaldeirao(() -> Items.AMETHYST_SHARD, ListaAspectos.de(a, 8),
                    () -> new ItemStack(ModItens.cristal(a).get()), Pesquisa.CRISTAIS_REFINADOS));
        }

        // Lingote de Arcanita
        l.add(new ReceitaCaldeirao(() -> Items.IRON_INGOT, ListaAspectos.de(METAL, 6, MAGIA, 4),
                () -> new ItemStack(ModItens.LINGOTE_ARCANITA.get()), Pesquisa.METALURGIA));

        // Sal Purificador
        l.add(new ReceitaCaldeirao(() -> Items.SUGAR, ListaAspectos.de(ORDEM, 8, VIDA, 4),
                () -> new ItemStack(ModItens.SAL_PURIFICADOR.get(), 4), Pesquisa.PURIFICACAO));

        // Pó Vital: crescimento instantâneo
        l.add(new ReceitaCaldeirao(() -> Items.BONE_MEAL, ListaAspectos.de(VIDA, 6, TERRA, 2),
                () -> new ItemStack(ModItens.PO_VITAL.get(), 2), Pesquisa.PO_VITAL));

        // Transmutação
        l.add(new ReceitaCaldeirao(() -> Items.COPPER_INGOT, ListaAspectos.de(METAL, 4, MAGIA, 4),
                () -> new ItemStack(Items.GOLD_INGOT), Pesquisa.TRANSMUTACAO));
        l.add(new ReceitaCaldeirao(() -> Items.COAL_BLOCK, ListaAspectos.de(TERRA, 12, ORDEM, 12, MAGIA, 6),
                () -> new ItemStack(Items.DIAMOND), Pesquisa.TRANSMUTACAO));

        todas = l;
        return todas;
    }
}
