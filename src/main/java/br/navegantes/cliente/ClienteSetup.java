package br.navegantes.cliente;

import br.navegantes.registro.ModEntidades;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;

/** Registros que só existem no cliente. */
public final class ClienteSetup {
    private ClienteSetup() {}

    public static void registrar(BusGroup barramento) {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(evento -> {
            evento.registerEntityRenderer(ModEntidades.NAVIO.get(), NavioRenderer::new);
            evento.registerEntityRenderer(ModEntidades.PECA.get(), NoopRenderer::new);
        });
    }
}
