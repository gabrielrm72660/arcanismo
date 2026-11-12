package br.arcanismo.conhecimento;

import static br.arcanismo.aspecto.Aspecto.*;
import static br.arcanismo.conhecimento.Categoria.*;

import java.util.function.Supplier;

import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.aspecto.ListaAspectos;
import br.arcanismo.registro.ModBlocos;
import br.arcanismo.registro.ModItens;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * As 26 pesquisas do Arcanismo. A ordem importa: cada pesquisa só pode exigir
 * pesquisas declaradas antes dela. (x, y) é a posição no mapa do Grimório,
 * com a Infusão no centro (0, 0).
 */
public enum Pesquisa {
    // ---------------------------------------------------------------- Fundamentos
    FUNDAMENTOS("fundamentos", "Fundamentos Arcanos", FUNDAMENTOS_CAT(), -7, -3,
            "Tudo é feito de aspectos. Escaneie o mundo com a Lente Arcana.",
            ListaAspectos.VAZIA, () -> new ItemStack(ModItens.GRIMORIO.get())),
    ASPECTOS("aspectos", "Natureza dos Aspectos", FUNDAMENTOS_CAT(), -7, -1,
            "Aspectos compostos nascem da união de dois outros.",
            ListaAspectos.de(ORDEM, 3, TERRA, 3), () -> new ItemStack(ModItens.LENTE_ARCANA.get()), FUNDAMENTOS),
    MESA_ARCANA("mesa", "A Mesa Arcana", FUNDAMENTOS_CAT(), -5, -3,
            "Toda criação arcana nasce na Mesa, paga com Vis do cajado.",
            ListaAspectos.de(ORDEM, 4), () -> new ItemStack(ModBlocos.MESA_ARCANA.get()), FUNDAMENTOS),
    ORBES("orbes", "Orbes de Luz", FUNDAMENTOS_CAT(), -7, 1,
            "Essência pura vaga perto dos cristais. Tocá-la ensina e recarrega.",
            ListaAspectos.de(AR, 4, MAGIA, 2), () -> new ItemStack(ModItens.cristal(AR).get()), ASPECTOS),

    // ---------------------------------------------------------------- Taumaturgia
    CAJADOS("cajados", "Cajados", TAUMATURGIA, -3, -5,
            "O cajado guarda Vis e lança fogo. Base de toda magia de combate.",
            ListaAspectos.de(FOGO, 4, ORDEM, 2), () -> new ItemStack(ModItens.CAJADO_APRENDIZ.get()), MESA_ARCANA),
    CONJURACAO("conjuracao", "Conjuração Elemental", TAUMATURGIA, -1, -6,
            "Cristal de Ar ou Água na outra mão muda o feitiço.",
            ListaAspectos.de(FOGO, 8, AR, 8, MAGIA, 3), () -> new ItemStack(ModItens.cristal(AR).get()), CAJADOS),
    FOCO_TERRA("foco_terra", "Foco da Terra", TAUMATURGIA, 1, -7,
            "Libera o feitiço de escavação com o Cristal de Terra.",
            ListaAspectos.de(TERRA, 12, ORDEM, 4), () -> new ItemStack(ModItens.cristal(TERRA).get()), CONJURACAO),
    FOCOS_AVANCADOS("focos_avancados", "Focos Avançados", TAUMATURGIA, 3, -6,
            "Ordem protege, Entropia definha. Dois focos perigosos.",
            ListaAspectos.de(ORDEM, 14, ENTROPIA, 14, MAGIA, 6), () -> new ItemStack(ModItens.cristal(ENTROPIA).get()), FOCO_TERRA),

    // ---------------------------------------------------------------- Alquimia
    ALQUIMIA("alquimia", "Alquimia do Caldeirão", ALQUIMIA_CAT(), -5, 1,
            "Dissolva itens no caldeirão aquecido. Libera o Caldeirão e o Pó de Vis.",
            ListaAspectos.de(AGUA, 8, FOGO, 8), () -> new ItemStack(ModBlocos.CALDEIRAO_ARCANO.get()), ASPECTOS),
    CRISTAIS_REFINADOS("cristais", "Cristais Refinados", ALQUIMIA_CAT(), -5, 3,
            "Ametista + 8 de um primordial cria o cristal desse aspecto.",
            ListaAspectos.de(TERRA, 6, ORDEM, 6, MAGIA, 3), () -> new ItemStack(Items.AMETHYST_SHARD), ALQUIMIA),
    METALURGIA("metalurgia", "Metalurgia Arcana", ALQUIMIA_CAT(), -3, 2,
            "Ferro + Metal e Magia vira Arcanita.",
            ListaAspectos.de(TERRA, 12, METAL, 6, ORDEM, 6), () -> new ItemStack(ModItens.LINGOTE_ARCANITA.get()), ALQUIMIA),
    PURIFICACAO("purificacao", "Purificação", ALQUIMIA_CAT(), -3, 4,
            "Açúcar + Ordem e Vida vira Sal Purificador.",
            ListaAspectos.de(ORDEM, 12, VIDA, 8), () -> new ItemStack(ModItens.SAL_PURIFICADOR.get()), ALQUIMIA),
    FLUXO("fluxo", "O Fluxo", ALQUIMIA_CAT(), -1, 5,
            "Essência perdida corrompe a terra. Conhecer o mal é o primeiro passo.",
            ListaAspectos.de(ENTROPIA, 10, MAGIA, 4), () -> new ItemStack(ModBlocos.TERRA_CORROMPIDA.get()), PURIFICACAO),
    TRANSMUTACAO("transmutacao", "Transmutação", ALQUIMIA_CAT(), -6, 5,
            "Cobre vira ouro; bloco de carvão vira diamante.",
            ListaAspectos.de(METAL, 12, MAGIA, 10, ORDEM, 10), () -> new ItemStack(Items.GOLD_INGOT), CRISTAIS_REFINADOS, METALURGIA),

    // ---------------------------------------------------------------- Artifício
    ARTIFICIO("artificio", "Artifício", ARTIFICIO_CAT(), 4, 0,
            "Construção arcana. Libera o Tijolo Arcano.",
            ListaAspectos.de(TERRA, 8, ORDEM, 6), () -> new ItemStack(ModBlocos.TIJOLO_ARCANO.get()), MESA_ARCANA),
    CAJADO_ARCANITA("cajado_arcanita", "Cajado de Arcanita", TAUMATURGIA, 2, -4,
            "Um cajado de metal arcano que guarda 300 Vis.",
            ListaAspectos.de(METAL, 10, ENERGIA, 8, MAGIA, 6), () -> new ItemStack(ModItens.CAJADO_ARCANITA.get()), CAJADOS, METALURGIA),
    FERRAMENTAS_ARCANITA("ferramentas", "Ferramentas de Arcanita", ARTIFICIO_CAT(), 6, 2,
            "Picareta e espada mais fortes que as de diamante.",
            ListaAspectos.de(METAL, 14, TERRA, 10, ORDEM, 6), () -> new ItemStack(ModItens.PICARETA_ARCANITA.get()), ARTIFICIO, METALURGIA),
    INFUSAO("infusao", "Infusão", INFUSAO_CAT(), 0, 0,
            "O Altar de Infusão funde itens com essência dos caldeirões. Pedestais simétricos dão estabilidade.",
            ListaAspectos.de(MAGIA, 16, ENERGIA, 12, ORDEM, 12), () -> new ItemStack(ModBlocos.ALTAR_INFUSAO.get()), METALURGIA, CONJURACAO, ARTIFICIO),
    CAJADO_CRISTAL("cajado_cristal", "Cajado de Cristal", TAUMATURGIA, 4, -3,
            "Infusão do Cajado de Arcanita com cristais e diamante. Guarda 600 Vis.",
            ListaAspectos.de(MAGIA, 20, ENERGIA, 14, ORDEM, 10), () -> new ItemStack(ModItens.CAJADO_CRISTAL.get()), CAJADO_ARCANITA, INFUSAO),
    OCULOS("oculos", "Óculos da Revelação", ARTIFICIO_CAT(), 6, -1,
            "Use na cabeça: o visor de aspectos sem precisar segurar a Lente.",
            ListaAspectos.de(ORDEM, 12, MAGIA, 10, AR, 6), () -> new ItemStack(ModItens.OCULOS.get()), ARTIFICIO, INFUSAO),
    BOTAS("botas", "Botas do Viajante", ARTIFICIO_CAT(), 8, 1,
            "Infundidas com Ar: você corre mais rápido.",
            ListaAspectos.de(AR, 16, VIDA, 6), () -> new ItemStack(ModItens.BOTAS.get()), OCULOS),

    // ---------------------------------------------------------------- Natureza
    NATUREZA("natureza", "Essência Viva", NATUREZA_CAT(), 1, 4,
            "A vida também é feita de aspectos.",
            ListaAspectos.de(VIDA, 10, TERRA, 6), () -> new ItemStack(Items.OAK_SAPLING), ALQUIMIA),
    PO_VITAL("po_vital", "Pó Vital", NATUREZA_CAT(), 3, 3,
            "Farinha de osso + Vida no caldeirão. Faz plantas crescerem de uma vez.",
            ListaAspectos.de(VIDA, 14, AGUA, 6), () -> new ItemStack(ModItens.PO_VITAL.get()), NATUREZA),
    CULTIVO_CRISTAL("cultivo", "Cultivo de Cristais", NATUREZA_CAT(), 1, 6,
            "Na Mesa, 8 cristais e uma ametista formam um Aglomerado.",
            ListaAspectos.de(TERRA, 12, MAGIA, 8, ORDEM, 6), () -> new ItemStack(ModBlocos.aglomerado(FOGO).get()), NATUREZA, CRISTAIS_REFINADOS),

    // ---------------------------------------------------------------- Proibido
    PROIBIDO("proibido", "Conhecimento Proibido", PROIBIDO_CAT(), 5, 5,
            "Há poder no Fluxo, para quem aceitar o preço.",
            ListaAspectos.de(ENTROPIA, 20, MAGIA, 10), () -> new ItemStack(Items.ENDER_EYE), FLUXO, FOCOS_AVANCADOS),
    LAMINA_VAZIO("lamina", "Lâmina do Vazio", PROIBIDO_CAT(), 7, 6,
            "Infusão de uma espada de diamante com Entropia pura.",
            ListaAspectos.de(ENTROPIA, 24, METAL, 10, MAGIA, 10), () -> new ItemStack(ModItens.LAMINA_VAZIO.get()), PROIBIDO, INFUSAO);

    public final String id;
    public final String nome;
    public final Categoria categoria;
    public final int x, y;
    public final String descricao;
    public final ListaAspectos custo;
    public final Supplier<ItemStack> icone;
    public final Pesquisa[] requisitos;

    Pesquisa(String id, String nome, Categoria categoria, int x, int y, String descricao,
             ListaAspectos custo, Supplier<ItemStack> icone, Pesquisa... requisitos) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
        this.x = x;
        this.y = y;
        this.descricao = descricao;
        this.custo = custo;
        this.icone = icone;
        this.requisitos = requisitos;
    }

    // Atalhos (os nomes das categorias colidem com os das pesquisas)
    private static Categoria FUNDAMENTOS_CAT() { return Categoria.FUNDAMENTOS; }
    private static Categoria ALQUIMIA_CAT() { return Categoria.ALQUIMIA; }
    private static Categoria ARTIFICIO_CAT() { return Categoria.ARTIFICIO; }
    private static Categoria NATUREZA_CAT() { return Categoria.NATUREZA; }
    private static Categoria PROIBIDO_CAT() { return Categoria.PROIBIDO; }
    private static Categoria INFUSAO_CAT() { return Categoria.INFUSAO; }

    /** Feitiço do cajado → pesquisa necessária. */
    public static Pesquisa paraFeitico(Aspecto elemento) {
        return switch (elemento) {
            case FOGO -> null;
            case AR, AGUA -> CONJURACAO;
            case TERRA -> FOCO_TERRA;
            default -> FOCOS_AVANCADOS;
        };
    }

    public static Pesquisa porId(String id) {
        for (Pesquisa p : values()) if (p.id.equals(id)) return p;
        return null;
    }
}
