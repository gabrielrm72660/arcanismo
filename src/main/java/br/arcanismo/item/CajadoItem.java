package br.arcanismo.item;

import java.util.List;

import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.conhecimento.Conhecimento;
import br.arcanismo.conhecimento.Pesquisa;
import br.arcanismo.registro.ModItens;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cajado Arcano: guarda Vis e conjura feitiços.
 *  - Clique direito: conjura. O feitiço depende do cristal na outra mão
 *    (sem cristal = Faísca de fogo). Outros elementos exigem a pesquisa Conjuração Elemental.
 *  - Agachado + clique direito: recarrega consumindo Pó de Vis (+50) ou um Cristal (+15).
 */
public class CajadoItem extends Item {
    private static final String CHAVE_VIS = "vis";
    private final int capacidade;

    public CajadoItem(int capacidade, Properties propriedades) {
        super(propriedades);
        this.capacidade = capacidade;
    }

    // ---------- Vis guardado no item ----------

    public static int vis(ItemStack pilha) {
        return pilha.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(CHAVE_VIS, 0);
    }

    /** Soma (ou subtrai, com valor negativo) Vis ao cajado, respeitando a capacidade. */
    public void adicionarVis(ItemStack pilha, int valor) {
        definirVis(pilha, vis(pilha) + valor);
    }

    public int capacidade() {
        return capacidade;
    }

    private void definirVis(ItemStack pilha, int valor) {
        int v = Math.max(0, Math.min(capacidade, valor));
        CustomData.update(DataComponents.CUSTOM_DATA, pilha, tag -> tag.putInt(CHAVE_VIS, v));
    }

    @Override
    public boolean isBarVisible(ItemStack pilha) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack pilha) {
        return Math.round(13.0f * vis(pilha) / capacidade);
    }

    @Override
    public int getBarColor(ItemStack pilha) {
        return 0xB36BFF;
    }

    // ---------- Uso ----------

    @Override
    public InteractionResult use(Level nivel, Player jogador, InteractionHand mao) {
        ItemStack cajado = jogador.getItemInHand(mao);
        if (nivel.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel servidor = (ServerLevel) nivel;

        if (jogador.isShiftKeyDown()) {
            recarregar(servidor, jogador, cajado);
            return InteractionResult.SUCCESS;
        }

        InteractionHand outra = mao == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack foco = jogador.getItemInHand(outra);
        Aspecto elemento = foco.getItem() instanceof CristalItem c ? c.aspecto() : Aspecto.FOGO;

        Pesquisa necessaria = Pesquisa.paraFeitico(elemento);
        if (necessaria != null && !Conhecimento.tem(jogador, necessaria)) {
            jogador.sendSystemMessage(Component.literal(
                    "Você ainda não sabe canalizar esse elemento (pesquisa: " + necessaria.nome + ").")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        int custo = custo(elemento);
        int atual = vis(cajado);
        if (atual < custo) {
            jogador.sendSystemMessage(Component.literal("Vis insuficiente (" + atual + "/" + custo
                    + "). Agache e clique para recarregar.").withStyle(ChatFormatting.RED));
            nivel.playSound(null, jogador.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.8f);
            return InteractionResult.FAIL;
        }

        if (conjurar(servidor, jogador, elemento)) {
            definirVis(cajado, atual - custo);
            jogador.getCooldowns().addCooldown(cajado, 10);
        }
        return InteractionResult.SUCCESS;
    }

    private static int custo(Aspecto elemento) {
        return switch (elemento) {
            case FOGO -> 5;
            case AR -> 6;
            case TERRA -> 4;
            case AGUA -> 8;
            case ORDEM -> 12;
            case ENTROPIA -> 10;
            default -> 5;
        };
    }

    private void recarregar(ServerLevel nivel, Player jogador, ItemStack cajado) {
        if (vis(cajado) >= capacidade) {
            jogador.sendSystemMessage(Component.literal("O cajado já está cheio (" + capacidade + " Vis).")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        var inventario = jogador.getInventory();
        // Prioridade: Pó de Vis, depois qualquer cristal
        for (int passo = 0; passo < 2; passo++) {
            for (int i = 0; i < inventario.getContainerSize(); i++) {
                ItemStack s = inventario.getItem(i);
                boolean serve = passo == 0 ? s.is(ModItens.PO_DE_VIS.get()) : s.getItem() instanceof CristalItem;
                if (serve) {
                    int ganho = passo == 0 ? 50 : 15;
                    s.shrink(1);
                    definirVis(cajado, vis(cajado) + ganho);
                    nivel.playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                            SoundSource.PLAYERS, 1.0f, 1.0f);
                    nivel.sendParticles(ParticleTypes.ENCHANT, jogador.getX(), jogador.getY() + 1.0, jogador.getZ(),
                            30, 0.5, 0.8, 0.5, 0.5);
                    jogador.sendSystemMessage(Component.literal("Vis: " + vis(cajado) + "/" + capacidade)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    return;
                }
            }
        }
        jogador.sendSystemMessage(Component.literal("Você precisa de Pó de Vis ou de um Cristal de Essência para recarregar.")
                .withStyle(ChatFormatting.RED));
    }

    private static boolean conjurar(ServerLevel nivel, Player jogador, Aspecto elemento) {
        Vec3 olhar = jogador.getLookAngle();
        Vec3 olhos = jogador.getEyePosition();

        switch (elemento) {
            case FOGO -> {
                SmallFireball bola = new SmallFireball(nivel, jogador, olhar);
                bola.setPos(olhos.x + olhar.x, olhos.y + olhar.y - 0.1, olhos.z + olhar.z);
                nivel.addFreshEntity(bola);
                nivel.playSound(null, jogador.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
            case AR -> {
                // Rajada de vento: empurra criaturas à frente
                List<LivingEntity> alvos = nivel.getEntitiesOfClass(LivingEntity.class,
                        jogador.getBoundingBox().inflate(6.0), e -> e != jogador);
                for (LivingEntity e : alvos) {
                    Vec3 direcao = e.position().subtract(jogador.position()).normalize();
                    if (direcao.dot(olhar) > 0.5) {
                        e.push(olhar.x * 2.0, 0.6, olhar.z * 2.0);
                        e.hurtMarked = true;
                    }
                }
                nivel.sendParticles(ParticleTypes.CLOUD, olhos.x + olhar.x * 2, olhos.y, olhos.z + olhar.z * 2,
                        25, 0.6, 0.4, 0.6, 0.15);
                nivel.playSound(null, jogador.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
            case TERRA -> {
                // Escavação: quebra o bloco mirado (até a dureza de pedra comum)
                HitResult mira = jogador.pick(6.0, 0.0f, false);
                if (!(mira instanceof BlockHitResult bloco) || mira.getType() != HitResult.Type.BLOCK) return false;
                BlockPos pos = bloco.getBlockPos();
                BlockState estado = nivel.getBlockState(pos);
                float dureza = estado.getDestroySpeed(nivel, pos);
                if (dureza < 0 || dureza > 3.0f) {
                    jogador.sendSystemMessage(Component.literal("Esse bloco é duro demais.").withStyle(ChatFormatting.GRAY));
                    return false;
                }
                nivel.destroyBlock(pos, true, jogador);
            }
            case AGUA -> {
                // Cura e apaga chamas
                jogador.heal(6.0f);
                jogador.clearFire();
                nivel.sendParticles(ParticleTypes.SPLASH, jogador.getX(), jogador.getY() + 1.0, jogador.getZ(),
                        40, 0.5, 0.6, 0.5, 0.1);
                nivel.playSound(null, jogador.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            case ORDEM -> {
                // Escudo de Ordem: absorção
                jogador.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 1));
                nivel.sendParticles(ParticleTypes.END_ROD, jogador.getX(), jogador.getY() + 1.0, jogador.getZ(),
                        30, 0.6, 0.8, 0.6, 0.02);
                nivel.playSound(null, jogador.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.6f);
            }
            case ENTROPIA -> {
                // Murchar: enfraquece monstros próximos
                List<LivingEntity> alvos = nivel.getEntitiesOfClass(LivingEntity.class,
                        jogador.getBoundingBox().inflate(8.0), e -> e instanceof Enemy);
                for (LivingEntity e : alvos) {
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 5, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 10, 0));
                }
                nivel.sendParticles(ParticleTypes.SMOKE, jogador.getX(), jogador.getY() + 1.0, jogador.getZ(),
                        60, 3.0, 1.0, 3.0, 0.02);
                nivel.playSound(null, jogador.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.4f, 1.5f);
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
