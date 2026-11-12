package br.arcanismo.evento;

import br.arcanismo.conhecimento.Conhecimento;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Eventos do jogo (barramento do Forge, não do mod). */
public final class ModEventos {
    private ModEventos() {}

    public static void registrar() {
        PlayerEvent.Clone.BUS.addListener(ModEventos::aoClonarJogador);
    }

    /** Mantém o conhecimento arcano quando o jogador morre ou volta do End. */
    private static void aoClonarJogador(PlayerEvent.Clone evento) {
        CompoundTag antigo = evento.getOriginal().getPersistentData().getCompoundOrEmpty(Conhecimento.RAIZ);
        evento.getEntity().getPersistentData().put(Conhecimento.RAIZ, antigo.copy());
    }
}
