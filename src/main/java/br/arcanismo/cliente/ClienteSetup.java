package br.arcanismo.cliente;

import br.arcanismo.Arcanismo;
import br.arcanismo.registro.ModEntidades;
import br.arcanismo.registro.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registros que só existem no cliente (telas, visores e renderizadores). */
public final class ClienteSetup {
    private ClienteSetup() {}

    public static void registrar(BusGroup barramento) {
        // Visor da Lente Arcana
        AddGuiOverlayLayersEvent.BUS.addListener(evento ->
                evento.getLayeredDraw().add(Arcanismo.id("visor_lente"), (g, delta) -> LenteHud.desenhar(g)));

        // Tela da Mesa Arcana
        FMLClientSetupEvent.getBus(barramento).addListener(evento ->
        {
            MenuScreens.register(ModMenus.MESA_ARCANA.get(), MesaArcanaScreen::new);
            MenuScreens.register(ModMenus.GRIMORIO.get(), GrimorioScreen::new);
        });

        // A Orbe de Luz não tem modelo: é desenhada só com partículas
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(evento ->
                evento.registerEntityRenderer(ModEntidades.ORBE_DE_LUZ.get(), NoopRenderer::new));
    }
}
