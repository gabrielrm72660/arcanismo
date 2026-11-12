package br.arcanismo.conhecimento;

import java.util.Map;

import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.aspecto.ListaAspectos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Conhecimento arcano do jogador, guardado nos dados persistentes dele.
 * O evento de clonagem (ModEventos) copia tudo quando o jogador morre.
 */
public final class Conhecimento {
    public static final String RAIZ = "arcanismo";
    private static final String ESCANEADOS = "escaneados";
    private static final String PONTOS = "pontos";
    private static final String PESQUISAS = "pesquisas";

    private Conhecimento() {}

    private static CompoundTag raiz(Player jogador) {
        CompoundTag dados = jogador.getPersistentData();
        CompoundTag raiz = dados.getCompoundOrEmpty(RAIZ);
        dados.put(RAIZ, raiz);
        return raiz;
    }

    private static CompoundTag sub(Player jogador, String nome) {
        CompoundTag raiz = raiz(jogador);
        CompoundTag t = raiz.getCompoundOrEmpty(nome);
        raiz.put(nome, t);
        return t;
    }

    /** Registra um escaneamento. Retorna true se era algo novo (e dá pontos). */
    public static boolean registrarEscaneamento(Player jogador, String chave, ListaAspectos aspectos) {
        CompoundTag escaneados = sub(jogador, ESCANEADOS);
        if (escaneados.getBooleanOr(chave, false)) return false;
        escaneados.putBoolean(chave, true);
        CompoundTag pontos = sub(jogador, PONTOS);
        for (Map.Entry<Aspecto, Integer> e : aspectos.mapa().entrySet()) {
            pontos.putInt(e.getKey().id, pontos.getIntOr(e.getKey().id, 0) + e.getValue());
        }
        return true;
    }

    public static int pontos(Player jogador, Aspecto a) {
        return sub(jogador, PONTOS).getIntOr(a.id, 0);
    }

    public static int totalEscaneados(Player jogador) {
        return sub(jogador, ESCANEADOS).size();
    }

    public static boolean tem(Player jogador, Pesquisa p) {
        return sub(jogador, PESQUISAS).getBooleanOr(p.id, false);
    }

    public static boolean podePagar(Player jogador, ListaAspectos custo) {
        for (Map.Entry<Aspecto, Integer> e : custo.mapa().entrySet()) {
            if (pontos(jogador, e.getKey()) < e.getValue()) return false;
        }
        return true;
    }

    public static boolean requisitosOk(Player jogador, Pesquisa p) {
        for (Pesquisa r : p.requisitos) if (!tem(jogador, r)) return false;
        return true;
    }

    public static boolean disponivel(Player jogador, Pesquisa p) {
        return !tem(jogador, p) && requisitosOk(jogador, p);
    }

    /** Pontos ganhos por outras fontes (ex.: Orbes de Luz). */
    public static void adicionarPontos(Player jogador, Aspecto a, int quantidade) {
        CompoundTag pontos = sub(jogador, PONTOS);
        pontos.putInt(a.id, pontos.getIntOr(a.id, 0) + quantidade);
    }

    /** Desbloqueia a pesquisa pagando o custo. Retorna false se não puder. */
    public static boolean pesquisar(Player jogador, Pesquisa p) {
        if (!disponivel(jogador, p) || !podePagar(jogador, p.custo)) return false;
        CompoundTag pontos = sub(jogador, PONTOS);
        for (Map.Entry<Aspecto, Integer> e : p.custo.mapa().entrySet()) {
            pontos.putInt(e.getKey().id, pontos.getIntOr(e.getKey().id, 0) - e.getValue());
        }
        sub(jogador, PESQUISAS).putBoolean(p.id, true);
        return true;
    }
}
