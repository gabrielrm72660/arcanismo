package br.arcanismo.aspecto;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Descobre para onde o jogador está olhando (bloco ou criatura) e quais aspectos o alvo tem.
 * Funciona igual no cliente (para o visor) e no servidor (para registrar o conhecimento).
 */
public final class MiraArcana {
    public static final double ALCANCE = 6.0;

    public record Alvo(String chave, Component nome, ListaAspectos aspectos, Vec3 centro, LivingEntity entidade) {}

    private MiraArcana() {}

    public static Alvo mirar(Player jogador) {
        Level nivel = jogador.level();
        Vec3 olhos = jogador.getEyePosition();
        Vec3 direcao = jogador.getLookAngle();
        Vec3 fim = olhos.add(direcao.scale(ALCANCE));

        // Criatura mais próxima na linha de visão
        LivingEntity melhor = null;
        double melhorDist = ALCANCE * ALCANCE;
        AABB area = jogador.getBoundingBox().expandTowards(direcao.scale(ALCANCE)).inflate(1.0);
        for (LivingEntity e : nivel.getEntitiesOfClass(LivingEntity.class, area, e -> e != jogador && e.isAlive())) {
            Optional<Vec3> acerto = e.getBoundingBox().inflate(0.2).clip(olhos, fim);
            if (acerto.isPresent()) {
                double d = olhos.distanceToSqr(acerto.get());
                if (d < melhorDist) {
                    melhorDist = d;
                    melhor = e;
                }
            }
        }

        // Bloco na linha de visão
        HitResult mira = jogador.pick(ALCANCE, 0.0f, false);
        if (mira instanceof BlockHitResult blocoHit && mira.getType() == HitResult.Type.BLOCK
                && olhos.distanceToSqr(mira.getLocation()) < melhorDist) {
            BlockPos pos = blocoHit.getBlockPos();
            BlockState estado = nivel.getBlockState(pos);
            Block bloco = estado.getBlock();
            ListaAspectos aspectos = RegistroAspectos.deItem(new ItemStack(bloco));
            if (aspectos.vazia()) return null;
            return new Alvo("bloco:" + BuiltInRegistries.BLOCK.getKey(bloco), bloco.getName(), aspectos,
                    Vec3.atCenterOf(pos), null);
        }

        if (melhor != null) {
            return new Alvo("entidade:" + BuiltInRegistries.ENTITY_TYPE.getKey(melhor.getType()), melhor.getName(),
                    RegistroAspectos.deEntidade(melhor),
                    melhor.position().add(0, melhor.getBbHeight() / 2, 0), melhor);
        }
        return null;
    }
}
