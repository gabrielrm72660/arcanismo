package br.arcanismo.cliente;

import java.util.Map;

import br.arcanismo.Arcanismo;
import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.aspecto.MiraArcana;
import br.arcanismo.item.LenteArcanaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Visor da Lente Arcana: enquanto você segura a Lente, um painel abaixo da mira
 * mostra o nome do alvo e os ícones dos seus aspectos. Ao segurar o botão direito,
 * uma barra mostra o progresso da absorção.
 */
public final class LenteHud {
    private static final Identifier PAINEL = Arcanismo.id("textures/gui/lente_painel.png");
    private static final Identifier BARRA = Arcanismo.id("textures/gui/lente_barra.png");
    private static final Identifier ICONES = Arcanismo.id("textures/gui/aspectos.png");

    private LenteHud() {}

    public static void desenhar(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        Player jogador = mc.player;
        if (jogador == null || mc.options.hideGui || mc.screen != null) return;
        boolean segurando = jogador.getMainHandItem().getItem() instanceof LenteArcanaItem
                || jogador.getOffhandItem().getItem() instanceof LenteArcanaItem;
        boolean oculos = jogador.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)
                .is(br.arcanismo.registro.ModItens.OCULOS.get());
        if (!segurando && !oculos) return;

        MiraArcana.Alvo alvo = MiraArcana.mirar(jogador);
        if (alvo == null) return;

        Font fonte = mc.font;
        int cx = g.guiWidth() / 2;
        int x = cx - 80, y = g.guiHeight() / 2 + 14;
        g.blit(RenderPipelines.GUI_TEXTURED, PAINEL, x, y, 0.0f, 0.0f, 160, 64, 160, 64);

        String nome = alvo.nome().getString();
        while (fonte.width(nome) > 130 && nome.length() > 3) nome = nome.substring(0, nome.length() - 1);
        g.text(fonte, nome, cx - fonte.width(nome) / 2, y + 13, 0xFFF2D27A, true);

        // Ícones dos aspectos
        Map<Aspecto, Integer> mapa = alvo.aspectos().mapa();
        int n = Math.min(mapa.size(), 6);
        int passo = 22;
        int ix = cx - (n * passo - 6) / 2;
        int iy = y + 26;
        int i = 0;
        for (Map.Entry<Aspecto, Integer> e : mapa.entrySet()) {
            if (i++ >= 6) break;
            g.blit(RenderPipelines.GUI_TEXTURED, ICONES, ix, iy, e.getKey().ordinal() * 16.0f, 0.0f, 16, 16, 160, 16);
            String qtd = String.valueOf(e.getValue());
            g.text(fonte, qtd, ix + 17 - fonte.width(qtd), iy + 10, 0xFFFFFFFF, true);
            ix += passo;
        }

        // Progresso da absorção
        if (jogador.isUsingItem() && jogador.getUseItem().getItem() instanceof LenteArcanaItem) {
            float prog = Math.min(1.0f, jogador.getTicksUsingItem() / (float) LenteArcanaItem.TEMPO_ABSORCAO);
            int largura = Math.round(110 * prog);
            if (largura > 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, BARRA, x + 25, y + 53, 0.0f, 0.0f, largura, 2, 128, 2);
            }
        } else if (segurando) {
            String dica = "Segure o botão direito para absorver";
            g.text(fonte, dica, cx - fonte.width(dica) / 2, y + 66, 0xFFC9A8F0, true);
        }
    }
}
