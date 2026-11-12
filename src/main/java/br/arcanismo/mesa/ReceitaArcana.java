package br.arcanismo.mesa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import br.arcanismo.conhecimento.Pesquisa;
import br.arcanismo.item.CristalItem;
import br.arcanismo.registro.ModBlocos;
import br.arcanismo.registro.ModItens;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Receita da Mesa Arcana: um padrão 3x3 (como na bancada comum) mais um custo de Vis,
 * pago pelo cajado colocado no encaixe da mesa. Pode exigir uma pesquisa.
 */
public record ReceitaArcana(String[] padrao, Map<Character, Predicate<ItemStack>> chave,
                            Supplier<ItemStack> resultado, int vis, Pesquisa pesquisa) {

    // ------------------------------------------------------------------ ingredientes
    public static Predicate<ItemStack> item(Item item) {
        return s -> s.is(item);
    }

    public static Predicate<ItemStack> item(Supplier<? extends Item> item) {
        return s -> s.is(item.get());
    }

    public static final Predicate<ItemStack> CRISTAL = s -> s.getItem() instanceof CristalItem;
    public static final Predicate<ItemStack> TIJOLO = s -> s.is(ModBlocos.TIJOLO_ARCANO.get().asItem());

    // ------------------------------------------------------------------ correspondência
    /** Grade 3x3 na ordem linha a linha (índices 0..8). */
    public boolean combina(List<ItemStack> grade) {
        int alt = padrao.length, larg = padrao[0].length();
        for (int dy = 0; dy <= 3 - alt; dy++) {
            for (int dx = 0; dx <= 3 - larg; dx++) {
                if (combinaEm(grade, dx, dy, false) || combinaEm(grade, dx, dy, true)) return true;
            }
        }
        return false;
    }

    private boolean combinaEm(List<ItemStack> grade, int dx, int dy, boolean espelhado) {
        int alt = padrao.length, larg = padrao[0].length();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                ItemStack s = grade.get(y * 3 + x);
                int px = x - dx, py = y - dy;
                char c = ' ';
                if (px >= 0 && py >= 0 && px < larg && py < alt) {
                    c = padrao[py].charAt(espelhado ? larg - 1 - px : px);
                }
                if (c == ' ') {
                    if (!s.isEmpty()) return false;
                } else {
                    Predicate<ItemStack> p = chave.get(c);
                    if (s.isEmpty() || p == null || !p.test(s)) return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ lista
    private static List<ReceitaArcana> todas;

    public static List<ReceitaArcana> todas() {
        if (todas != null) return todas;
        List<ReceitaArcana> l = new ArrayList<>();

        l.add(new ReceitaArcana(new String[]{"  C", " S ", "N  "},
                Map.of('C', CRISTAL, 'S', item(Items.STICK), 'N', item(Items.GOLD_NUGGET)),
                () -> new ItemStack(ModItens.CAJADO_APRENDIZ.get()), 0, null));

        l.add(new ReceitaArcana(new String[]{" G ", "GPG", " C "},
                Map.of('G', item(Items.GOLD_INGOT), 'P', item(Items.GLASS_PANE), 'C', CRISTAL),
                () -> new ItemStack(ModItens.LENTE_ARCANA.get()), 5, null));

        l.add(new ReceitaArcana(new String[]{"C C", "CKC"},
                Map.of('C', CRISTAL, 'K', item(Items.CAULDRON)),
                () -> new ItemStack(ModBlocos.CALDEIRAO_ARCANO.get()), 10, Pesquisa.ALQUIMIA));

        l.add(new ReceitaArcana(new String[]{"BBB", "BAB", "BBB"},
                Map.of('B', item(Items.STONE_BRICKS), 'A', item(Items.AMETHYST_SHARD)),
                () -> new ItemStack(ModBlocos.TIJOLO_ARCANO.get(), 8), 2, Pesquisa.ARTIFICIO));

        l.add(new ReceitaArcana(new String[]{" AC", " BA", "B  "},
                Map.of('C', CRISTAL, 'A', item(ModItens.LINGOTE_ARCANITA), 'B', item(Items.BLAZE_ROD)),
                () -> new ItemStack(ModItens.CAJADO_ARCANITA.get()), 40, Pesquisa.CAJADO_ARCANITA));

        // Infusão
        l.add(new ReceitaArcana(new String[]{" L ", "CTC", "TTT"},
                Map.of('L', item(ModItens.LINGOTE_ARCANITA), 'C', CRISTAL, 'T', TIJOLO),
                () -> new ItemStack(ModBlocos.ALTAR_INFUSAO.get()), 50, Pesquisa.INFUSAO));
        l.add(new ReceitaArcana(new String[]{"TCT", " T ", "TTT"},
                Map.of('C', CRISTAL, 'T', TIJOLO),
                () -> new ItemStack(ModBlocos.PEDESTAL_ARCANO.get()), 10, Pesquisa.INFUSAO));

        // Ferramentas
        l.add(new ReceitaArcana(new String[]{"LLL", " S ", " S "},
                Map.of('L', item(ModItens.LINGOTE_ARCANITA), 'S', item(Items.STICK)),
                () -> new ItemStack(ModItens.PICARETA_ARCANITA.get()), 20, Pesquisa.FERRAMENTAS_ARCANITA));
        l.add(new ReceitaArcana(new String[]{"L", "L", "S"},
                Map.of('L', item(ModItens.LINGOTE_ARCANITA), 'S', item(Items.STICK)),
                () -> new ItemStack(ModItens.ESPADA_ARCANITA.get()), 20, Pesquisa.FERRAMENTAS_ARCANITA));

        // Cultivo de cristais: 8 cristais de um aspecto + ametista = aglomerado
        for (br.arcanismo.aspecto.Aspecto a : br.arcanismo.aspecto.Aspecto.values()) {
            if (!a.primordial()) continue;
            l.add(new ReceitaArcana(new String[]{"CCC", "CAC", "CCC"},
                    Map.of('C', item(ModItens.cristal(a)), 'A', item(Items.AMETHYST_SHARD)),
                    () -> new ItemStack(ModBlocos.aglomerado(a).get()), 15, Pesquisa.CULTIVO_CRISTAL));
        }

        todas = l;
        return todas;
    }

    public static ReceitaArcana encontrar(List<ItemStack> grade) {
        for (ReceitaArcana r : todas()) {
            if (r.combina(grade)) return r;
        }
        return null;
    }
}
