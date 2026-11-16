package br.navegantes.navio;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Mensagens curtas para o jogador (na barra acima do inventário). */
public final class Mensagens {
    private Mensagens() {}

    public static void barra(Player jogador, String texto) {
        if (jogador instanceof ServerPlayer servidor) {
            servidor.sendSystemMessage(Component.literal(texto), true);
        }
    }

    public static void chat(Player jogador, String texto) {
        if (jogador != null) jogador.sendSystemMessage(Component.literal(texto));
    }
}
