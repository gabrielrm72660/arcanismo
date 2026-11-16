package br.navegantes.cliente;

import java.util.ArrayList;
import java.util.List;

import br.navegantes.navio.Estrutura;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Dados do navio já prontos para desenhar: rumo e um "bloco em movimento" para cada bloco visível. */
public class NavioRenderState extends EntityRenderState {
    public static final class Desenho {
        public int dx, dy, dz;
        public final MovingBlockRenderState estado = new MovingBlockRenderState();
    }

    public Estrutura estrutura;
    public float rumo;
    public int acesas;
    public int quantos;
    public final List<Desenho> desenhos = new ArrayList<>();
}
