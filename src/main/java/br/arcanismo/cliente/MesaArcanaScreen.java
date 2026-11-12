package br.arcanismo.cliente;

import br.arcanismo.Arcanismo;
import br.arcanismo.item.CajadoItem;
import br.arcanismo.mesa.MesaArcanaMenu;
import br.arcanismo.mesa.ReceitaArcana;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Tela da Mesa Arcana: grade 3x3, encaixe do cajado, resultado e custo de Vis. */
public class MesaArcanaScreen extends AbstractContainerScreen<MesaArcanaMenu> {
    private static final Identifier TEXTURA = Arcanismo.id("textures/gui/mesa_arcana.png");

    public MesaArcanaScreen(MesaArcanaMenu menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float parcial) {
        super.extractBackground(g, mouseX, mouseY, parcial);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURA, this.leftPos, this.topPos, 0.0f, 0.0f,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        ReceitaArcana receita = ReceitaArcana.encontrar(this.menu.grade());
        if (receita == null) return;

        ItemStack cajado = this.menu.cajado();
        int disponivel = cajado.getItem() instanceof CajadoItem ? CajadoItem.vis(cajado) : 0;
        String texto = receita.vis() == 0 ? "Sem custo" : "Vis: " + receita.vis();
        int cor = receita.vis() <= disponivel ? 0xFF8A3AC8 : 0xFFC03030;
        g.text(this.font, texto, 143 - this.font.width(texto) / 2, 64, cor, false);
        if (receita.vis() > disponivel) {
            String aviso = cajado.isEmpty() ? "Coloque um cajado" : "Vis insuficiente";
            g.text(this.font, aviso, this.imageWidth - 8 - this.font.width(aviso), 6, 0xFFC03030, false);
        }
    }
}
