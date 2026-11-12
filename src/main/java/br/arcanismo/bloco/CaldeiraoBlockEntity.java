package br.arcanismo.bloco;

import java.util.List;
import java.util.Map;

import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.aspecto.ListaAspectos;
import br.arcanismo.aspecto.RegistroAspectos;
import br.arcanismo.conhecimento.Conhecimento;
import br.arcanismo.registro.ModBlocos;
import br.arcanismo.registro.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/** Guarda a essência dissolvida no Caldeirão Arcano. */
public class CaldeiraoBlockEntity extends BlockEntity {
    public static final int CAPACIDADE = 100;

    private final int[] essencia = new int[Aspecto.values().length];

    public CaldeiraoBlockEntity(BlockPos pos, BlockState estado) {
        super(ModBlockEntities.CALDEIRAO.get(), pos, estado);
    }

    // ---------- Lógica por tick (servidor) ----------

    public static void tick(Level nivel, BlockPos pos, BlockState estado, CaldeiraoBlockEntity be) {
        if (nivel.getGameTime() % 10 != 0) return;
        if (!estado.getValue(CaldeiraoBlock.CHEIO) || !aquecido(nivel, pos)) return;

        AABB dentro = new AABB(pos.getX() + 0.1, pos.getY() + 0.2, pos.getZ() + 0.1,
                pos.getX() + 0.9, pos.getY() + 1.3, pos.getZ() + 0.9);
        List<ItemEntity> itens = nivel.getEntitiesOfClass(ItemEntity.class, dentro);
        if (itens.isEmpty()) return;

        ServerLevel servidor = (ServerLevel) nivel;
        for (ItemEntity entidade : itens) {
            ItemStack pilha = entidade.getItem();
            ListaAspectos aspectos = RegistroAspectos.deItem(pilha).multiplicar(pilha.getCount());
            for (Map.Entry<Aspecto, Integer> e : aspectos.mapa().entrySet()) {
                be.essencia[e.getKey().ordinal()] += e.getValue();
            }
            entidade.discard();
            servidor.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    8, 0.2, 0.1, 0.2, 0.05);
            servidor.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.4f, 1.6f);
        }

        int excesso = be.total() - CAPACIDADE;
        if (excesso > 0) {
            be.removerAleatorio(servidor, excesso);
            liberarFluxo(servidor, pos, excesso);
        }
        be.setChanged();
    }

    public static boolean aquecido(Level nivel, BlockPos pos) {
        BlockState abaixo = nivel.getBlockState(pos.below());
        if (abaixo.is(BlockTags.FIRE) || abaixo.is(Blocks.MAGMA_BLOCK)) return true;
        if (abaixo.getFluidState().is(FluidTags.LAVA)) return true;
        return abaixo.is(BlockTags.CAMPFIRES) && abaixo.getValue(CampfireBlock.LIT);
    }

    private int total() {
        int t = 0;
        for (int v : essencia) t += v;
        return t;
    }

    private void removerAleatorio(ServerLevel nivel, int quantidade) {
        for (int i = 0; i < quantidade && total() > 0; i++) {
            int idx;
            do {
                idx = nivel.getRandom().nextInt(essencia.length);
            } while (essencia[idx] <= 0);
            essencia[idx]--;
        }
    }

    /** Fluxo: essência perdida corrompe a terra ao redor. */
    public static void liberarFluxo(ServerLevel nivel, BlockPos centro, int quantidade) {
        int manchas = Math.max(1, quantidade / 10);
        var aleatorio = nivel.getRandom();
        for (int m = 0; m < manchas; m++) {
            for (int tentativa = 0; tentativa < 12; tentativa++) {
                BlockPos p = centro.offset(aleatorio.nextInt(9) - 4, aleatorio.nextInt(4) - 2, aleatorio.nextInt(9) - 4);
                BlockState s = nivel.getBlockState(p);
                if (s.is(BlockTags.DIRT) && !s.is(ModBlocos.TERRA_CORROMPIDA.get())) {
                    nivel.setBlockAndUpdate(p, ModBlocos.TERRA_CORROMPIDA.get().defaultBlockState()
                            .setValue(TerraCorrompidaBlock.FORCA, 2));
                    break;
                }
            }
        }
        nivel.sendParticles(ParticleTypes.WITCH, centro.getX() + 0.5, centro.getY() + 1.2, centro.getZ() + 0.5,
                40, 1.5, 0.5, 1.5, 0.1);
        nivel.playSound(null, centro, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.BLOCKS, 0.5f, 0.6f);
    }

    // ---------- Acesso para o Altar de Infusão ----------

    public int quantidade(Aspecto a) {
        return essencia[a.ordinal()];
    }

    /** Retira até {@code quantidade} do aspecto. Retorna quanto foi retirado. */
    public int retirar(Aspecto a, int quantidade) {
        int tirado = Math.min(quantidade, essencia[a.ordinal()]);
        essencia[a.ordinal()] -= tirado;
        if (tirado > 0) setChanged();
        return tirado;
    }

    // ---------- Interações ----------

    /** Retorna true se o item era um catalisador (mesmo que tenha faltado algo). */
    public boolean tentarCatalisar(Player jogador, ItemStack pilha) {
        boolean ehCatalisador = false;
        ReceitaCaldeirao faltando = null;

        for (ReceitaCaldeirao r : ReceitaCaldeirao.todas()) {
            if (!pilha.is(r.catalisador().get())) continue;
            ehCatalisador = true;
            if (!Conhecimento.tem(jogador, r.pesquisa())) continue;
            if (!temEssencia(r.custo())) {
                if (faltando == null) faltando = r;
                continue;
            }
            // Sucesso
            for (Map.Entry<Aspecto, Integer> e : r.custo().mapa().entrySet()) {
                essencia[e.getKey().ordinal()] -= e.getValue();
            }
            pilha.consume(1, jogador);
            ItemStack resultado = r.resultado().get();
            if (!jogador.getInventory().add(resultado)) jogador.drop(resultado, false);

            ServerLevel nivel = (ServerLevel) level;
            nivel.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                    worldPosition.getZ() + 0.5, 40, 0.3, 0.5, 0.3, 0.6);
            nivel.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            jogador.sendSystemMessage(Component.literal("O caldeirão borbulha e cria: ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE).append(resultado.getHoverName()));
            setChanged();
            return true;
        }

        if (!ehCatalisador) return false;
        if (faltando != null) {
            jogador.sendSystemMessage(Component.literal("Essência insuficiente. Precisa de: ")
                    .withStyle(ChatFormatting.RED).append(faltando.custo().texto()));
        } else {
            jogador.sendSystemMessage(Component.literal("Você ainda não sabe que reação é essa. Pesquise no Grimório.")
                    .withStyle(ChatFormatting.RED));
        }
        return true;
    }

    private boolean temEssencia(ListaAspectos custo) {
        for (Map.Entry<Aspecto, Integer> e : custo.mapa().entrySet()) {
            if (essencia[e.getKey().ordinal()] < e.getValue()) return false;
        }
        return true;
    }

    public void mostrarConteudo(Player jogador) {
        MutableComponent texto = Component.literal("Essência (" + total() + "/" + CAPACIDADE + "): ")
                .withStyle(ChatFormatting.DARK_PURPLE);
        boolean algum = false;
        for (Aspecto a : Aspecto.values()) {
            int v = essencia[a.ordinal()];
            if (v > 0) {
                texto.append(a.texto().append(Component.literal(" " + v + "  ")));
                algum = true;
            }
        }
        if (!algum) texto.append(Component.literal("vazio").withStyle(ChatFormatting.GRAY));
        jogador.sendSystemMessage(texto);
        if (!getBlockState().getValue(CaldeiraoBlock.CHEIO)) {
            jogador.sendSystemMessage(Component.literal("Encha com um balde de água.").withStyle(ChatFormatting.GRAY));
        } else if (!aquecido(level, worldPosition)) {
            jogador.sendSystemMessage(Component.literal("Precisa de calor embaixo (fogo, lava, magma ou fogueira).")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public void esvaziar(ServerLevel nivel, Player jogador) {
        int t = total();
        java.util.Arrays.fill(essencia, 0);
        nivel.setBlockAndUpdate(worldPosition, getBlockState().setValue(CaldeiraoBlock.CHEIO, false));
        if (t > 0) {
            liberarFluxo(nivel, worldPosition, t);
            jogador.sendSystemMessage(Component.literal("A essência escapa como Fluxo e corrompe a terra!")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        setChanged();
    }

    // ---------- Salvar / carregar ----------

    @Override
    protected void saveAdditional(ValueOutput saida) {
        super.saveAdditional(saida);
        for (Aspecto a : Aspecto.values()) {
            saida.putInt("essencia_" + a.id, essencia[a.ordinal()]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput entrada) {
        super.loadAdditional(entrada);
        for (Aspecto a : Aspecto.values()) {
            essencia[a.ordinal()] = entrada.getIntOr("essencia_" + a.id, 0);
        }
    }
}
