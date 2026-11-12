package br.arcanismo.aspecto;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Conjunto de aspectos com quantidades (ex.: Terra 2, Ordem 1). */
public final class ListaAspectos {
    public static final ListaAspectos VAZIA = new ListaAspectos();

    private final EnumMap<Aspecto, Integer> valores = new EnumMap<>(Aspecto.class);

    /** Cria uma lista a partir de pares (Aspecto, quantidade). */
    public static ListaAspectos de(Object... pares) {
        ListaAspectos l = new ListaAspectos();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            l.adicionar((Aspecto) pares[i], (Integer) pares[i + 1]);
        }
        return l;
    }

    public ListaAspectos adicionar(Aspecto a, int quantidade) {
        if (quantidade <= 0) return this;
        valores.merge(a, quantidade, Integer::sum);
        return this;
    }

    public int get(Aspecto a) {
        return valores.getOrDefault(a, 0);
    }

    public boolean vazia() {
        return valores.isEmpty();
    }

    public int total() {
        int t = 0;
        for (int v : valores.values()) t += v;
        return t;
    }

    public ListaAspectos multiplicar(int fator) {
        ListaAspectos l = new ListaAspectos();
        for (Map.Entry<Aspecto, Integer> e : valores.entrySet()) {
            l.adicionar(e.getKey(), e.getValue() * fator);
        }
        return l;
    }

    public Map<Aspecto, Integer> mapa() {
        return valores;
    }

    /** Texto colorido: "Terra 2, Ordem 1". */
    public MutableComponent texto() {
        MutableComponent c = Component.empty();
        boolean primeiro = true;
        for (Map.Entry<Aspecto, Integer> e : valores.entrySet()) {
            if (!primeiro) c.append(Component.literal(", "));
            c.append(e.getKey().texto().append(Component.literal(" " + e.getValue())));
            primeiro = false;
        }
        return c;
    }
}
