package br.navegantes.navio;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;

/** Ligações com os eventos do Forge. */
public final class EventosNavio {
    private EventosNavio() {}

    public static void registrar() {
        // Agachar já é "lançar a âncora": impede o jogo de tirar o piloto do timão.
        EntityMountEvent.BUS.addListener(evento -> {
            if (evento.isDismounting() && evento.getEntityBeingMounted() instanceof NavioEntidade navio) {
                return !navio.podeDesmontar(evento.getEntityMounting());
            }
            return false;
        });

        // Depois que o jogador local anda, o navio o leva junto e o faz colidir com o convés.
        TickEvent.PlayerTickEvent.Post.BUS.addListener(evento -> {
            Player jogador = evento.player();
            if (!jogador.level().isClientSide() || !jogador.isLocalPlayer()) return;
            if (jogador.isPassenger() || jogador.isSpectator()) return;
            for (NavioEntidade navio : jogador.level().getEntitiesOfClass(NavioEntidade.class,
                    jogador.getBoundingBox().inflate(0.5, 3.0, 0.5))) {
                navio.tratarEntidade(jogador);
            }
        });
    }
}
