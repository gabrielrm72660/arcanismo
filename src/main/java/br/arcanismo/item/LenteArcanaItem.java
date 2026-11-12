package br.arcanismo.item;

import br.arcanismo.aspecto.ListaAspectos;
import br.arcanismo.aspecto.MiraArcana;
import br.arcanismo.aspecto.RegistroAspectos;
import br.arcanismo.conhecimento.Conhecimento;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Lente Arcana.
 *  - Só de segurar, o visor mostra os aspectos do bloco ou criatura que você olha.
 *  - Segure o botão direito para ABSORVER a essência: partículas saem do alvo
 *    e voam até você. Ao completar, o conhecimento é registrado.
 *  - Com um item na outra mão, um clique escaneia esse item na hora.
 */
public class LenteArcanaItem extends Item {
    /** Quantos ticks segurando para absorver (20 ticks = 1 segundo). */
    public static final int TEMPO_ABSORCAO = 30;

    public LenteArcanaItem(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public InteractionResult use(Level nivel, Player jogador, InteractionHand mao) {
        InteractionHand outraMao = mao == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack outra = jogador.getItemInHand(outraMao);

        // Item na outra mão: escaneia na hora
        if (!outra.isEmpty() && !(outra.getItem() instanceof LenteArcanaItem)) {
            if (!nivel.isClientSide()) {
                String chave = "item:" + BuiltInRegistries.ITEM.getKey(outra.getItem());
                escanear(jogador, chave, outra.getHoverName(), RegistroAspectos.deItem(outra));
            }
            return InteractionResult.SUCCESS;
        }

        // Senão, começa a absorver o que estiver na mira
        jogador.startUsingItem(mao);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack pilha, LivingEntity usuario) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack pilha) {
        return ItemUseAnimation.SPYGLASS;
    }

    @Override
    public void onUseTick(Level nivel, LivingEntity usuario, ItemStack pilha, int restante) {
        if (!(usuario instanceof Player jogador)) return;
        int tempo = getUseDuration(pilha, usuario) - restante;
        MiraArcana.Alvo alvo = MiraArcana.mirar(jogador);
        if (alvo == null) return;

        if (nivel.isClientSide()) {
            // Essência sendo sugada: partículas partem do alvo e voam até os olhos
            RandomSource r = nivel.getRandom();
            Vec3 olhos = jogador.getEyePosition().add(0, -0.25, 0);
            for (int i = 0; i < 4; i++) {
                double sx = alvo.centro().x + (r.nextDouble() - 0.5) * 0.9;
                double sy = alvo.centro().y + (r.nextDouble() - 0.5) * 0.9;
                double sz = alvo.centro().z + (r.nextDouble() - 0.5) * 0.9;
                nivel.addParticle(ParticleTypes.ENCHANT, olhos.x, olhos.y, olhos.z,
                        sx - olhos.x, sy - olhos.y, sz - olhos.z);
            }
            if (tempo % 3 == 0) {
                nivel.addParticle(ParticleTypes.WITCH, alvo.centro().x + (r.nextDouble() - 0.5) * 0.6,
                        alvo.centro().y + 0.4, alvo.centro().z + (r.nextDouble() - 0.5) * 0.6, 0.0, 0.03, 0.0);
            }
            return;
        }

        if (tempo == 1) {
            nivel.playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.7f, 0.6f);
        }
        if (tempo >= TEMPO_ABSORCAO) {
            escanear(jogador, alvo.chave(), alvo.nome(), alvo.aspectos());
            jogador.stopUsingItem();
            jogador.getCooldowns().addCooldown(pilha, 10);
        }
    }

    private static void escanear(Player jogador, String chave, Component nome, ListaAspectos aspectos) {
        if (aspectos.vazia()) {
            jogador.sendSystemMessage(Component.literal("Nada de arcano aqui.").withStyle(ChatFormatting.GRAY));
            return;
        }
        boolean novo = Conhecimento.registrarEscaneamento(jogador, chave, aspectos);

        jogador.sendSystemMessage(Component.empty()
                .append(nome.copy().withStyle(ChatFormatting.BOLD))
                .append(Component.literal(": "))
                .append(aspectos.texto()));

        if (novo) {
            jogador.sendSystemMessage(Component.literal("✦ Essência absorvida! Novo conhecimento. ✦")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            jogador.level().playSound(null, jogador.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.4f);
        } else {
            jogador.sendSystemMessage(Component.literal("Você já conhece isto.").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
