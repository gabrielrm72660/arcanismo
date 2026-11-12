package br.arcanismo.conhecimento;

/** Categorias da árvore de pesquisas, cada uma com sua cor no mapa do Grimório. */
public enum Categoria {
    FUNDAMENTOS("Fundamentos", 0xFFD8D4E8),
    TAUMATURGIA("Taumaturgia", 0xFF6F7CF0),
    ALQUIMIA("Alquimia", 0xFFE8C83A),
    ARTIFICIO("Artifício", 0xFFE87A6A),
    NATUREZA("Natureza", 0xFF5CCB4E),
    PROIBIDO("Proibido", 0xFF9A3AB8),
    INFUSAO("Infusão", 0xFFF2A0B8);

    public final String nome;
    public final int cor;

    Categoria(String nome, int cor) {
        this.nome = nome;
        this.cor = cor;
    }
}
