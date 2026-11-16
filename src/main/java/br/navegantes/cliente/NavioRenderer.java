package br.navegantes.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import br.navegantes.bloco.FornalhaBlock;
import br.navegantes.navio.Estrutura;
import br.navegantes.navio.NavioEntidade;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Desenha cada bloco do navio como um "bloco em movimento" (o mesmo método do bloco em queda). */
public class NavioRenderer extends EntityRenderer<NavioEntidade, NavioRenderState> {
    public NavioRenderer(EntityRendererProvider.Context contexto) {
        super(contexto);
    }

    @Override
    public NavioRenderState createRenderState() {
        return new NavioRenderState();
    }

    @Override
    public void extractRenderState(NavioEntidade navio, NavioRenderState estado, float parcial) {
        super.extractRenderState(navio, estado, parcial);
        Estrutura estrutura = navio.estrutura();
        estado.estrutura = estrutura;
        estado.rumo = navio.rumoRender(parcial);
        estado.acesas = navio.acesasBits();
        estado.quantos = 0;
        if (!(navio.level() instanceof ClientLevel nivel) || estrutura.blocos().isEmpty()) return;

        // Pose suave (o jogo não interpola esta entidade sozinho) e uma única amostra de luz para o
        // navio todo: assim ele não pisca ao andar nem muda de brilho bloco a bloco.
        Vec3 pos = navio.posRender(parcial);
        estado.x = pos.x;
        estado.y = pos.y;
        estado.z = pos.z;
        BlockPos luz = BlockPos.containing(pos.x, pos.y + estrutura.altura + 0.5, pos.z);
        var bioma = nivel.getBiome(luz);

        for (Estrutura.Bloco b : estrutura.blocos()) {
            if (!b.visivel) continue;
            BlockState bloco = b.estado;
            if (bloco.getRenderShape() != RenderShape.MODEL) continue;
            if (b.indiceFornalha >= 0 && bloco.hasProperty(FornalhaBlock.LIT)) {
                boolean acesa = b.indiceFornalha < 31 && ((estado.acesas >> b.indiceFornalha) & 1) != 0;
                bloco = bloco.setValue(FornalhaBlock.LIT, acesa);
            }
            while (estado.desenhos.size() <= estado.quantos) estado.desenhos.add(new NavioRenderState.Desenho());
            NavioRenderState.Desenho d = estado.desenhos.get(estado.quantos++);
            d.dx = b.dx;
            d.dy = b.dy;
            d.dz = b.dz;
            d.estado.randomSeedPos = b.posLocal;
            d.estado.blockPos = luz;
            d.estado.blockState = bloco;
            d.estado.biome = bioma;
            d.estado.cardinalLighting = nivel.cardinalLighting();
            d.estado.lightEngine = nivel.getLightEngine();
        }
    }

    @Override
    public void submit(NavioRenderState estado, PoseStack pose, SubmitNodeCollector coletor, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(estado.rumo));
        for (int i = 0; i < estado.quantos; i++) {
            NavioRenderState.Desenho d = estado.desenhos.get(i);
            pose.pushPose();
            pose.translate(d.dx - 0.5, d.dy, d.dz - 0.5);
            coletor.submitMovingBlock(pose, d.estado);
            pose.popPose();
        }
        pose.popPose();
        super.submit(estado, pose, coletor, camera);
    }
}
