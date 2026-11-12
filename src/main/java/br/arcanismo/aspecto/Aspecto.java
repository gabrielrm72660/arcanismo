package br.arcanismo.aspecto;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * As essências que compõem todas as coisas do mundo.
 * Seis são primordiais; as demais são combinações de duas outras.
 */
public enum Aspecto {
    // Primordiais
    AR("ar", "Ar", ChatFormatting.YELLOW, null, null),
    TERRA("terra", "Terra", ChatFormatting.DARK_GREEN, null, null),
    FOGO("fogo", "Fogo", ChatFormatting.RED, null, null),
    AGUA("agua", "Água", ChatFormatting.AQUA, null, null),
    ORDEM("ordem", "Ordem", ChatFormatting.WHITE, null, null),
    ENTROPIA("entropia", "Entropia", ChatFormatting.DARK_GRAY, null, null),
    // Compostos
    VIDA("vida", "Vida", ChatFormatting.GREEN, TERRA, AGUA),
    METAL("metal", "Metal", ChatFormatting.GRAY, TERRA, ORDEM),
    ENERGIA("energia", "Energia", ChatFormatting.GOLD, FOGO, ORDEM),
    MAGIA("magia", "Magia", ChatFormatting.LIGHT_PURPLE, AR, ENERGIA);

    public final String id;
    public final String nome;
    public final ChatFormatting cor;
    public final Aspecto componenteA;
    public final Aspecto componenteB;

    Aspecto(String id, String nome, ChatFormatting cor, Aspecto a, Aspecto b) {
        this.id = id;
        this.nome = nome;
        this.cor = cor;
        this.componenteA = a;
        this.componenteB = b;
    }

    public boolean primordial() {
        return componenteA == null;
    }

    public MutableComponent texto() {
        return Component.literal(nome).withStyle(cor);
    }

    public static Aspecto porId(String id) {
        for (Aspecto a : values()) {
            if (a.id.equals(id)) return a;
        }
        return null;
    }
}
