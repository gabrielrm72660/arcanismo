package br.arcanismo.infusao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.bloco.CaldeiraoBlockEntity;
import br.arcanismo.conhecimento.Conhecimento;
import br.arcanismo.item.CajadoItem;
import br.arcanismo.registro.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Altar de Infusão: o coração da magia avançada.
 *  1. Ponha o item central no altar e os ingredientes em pedestais ao redor (até 5 blocos).
 *  2. Deixe os caldeirões próximos (até 8 blocos) com a essência da receita.
 *  3. Clique no altar com o cajado: ele paga o Vis e a infusão começa.
 *  Cada pedestal sem um par simétrico do outro lado aumenta a instabilidade:
 *  a infusão pode soltar Fluxo ou dar choques em quem estiver perto.
 */
public class AltarBlockEntity extends SuporteBlockEntity {
    private static final int RAIO_PEDESTAIS = 5;
    private static final int RAIO_CALDEIROES = 8;
    private static final int INTERVALO = 25;

    private boolean ativo = false;
    private int progresso = 0;
    private int instabilidade = 0;
    private ReceitaInfusao receita;
    private final List<BlockPos> fila = new ArrayList<>();

    public AltarBlockEntity(BlockPos pos, BlockState estado) {
        super(ModBlockEntities.ALTAR.get(), pos, estado);
    }

    @Override
    protected double alturaVitrine() {
        return 1.15;
    }

    public boolean ativo() {
        return ativo;
    }

    // ------------------------------------------------------------------ início

    private List<PedestalBlockEntity> pedestais() {
        List<PedestalBlockEntity> lista = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-RAIO_PEDESTAIS, -1, -RAIO_PEDESTAIS),
                worldPosition.offset(RAIO_PEDESTAIS, 1, RAIO_PEDESTAIS))) {
            if (level.getBlockEntity(p) instanceof PedestalBlockEntity ped && !ped.item().isEmpty()) lista.add(ped);
        }
        return lista;
    }

    private List<CaldeiraoBlockEntity> caldeiroes() {
        List<CaldeiraoBlockEntity> lista = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-RAIO_CALDEIROES, -2, -RAIO_CALDEIROES),
                worldPosition.offset(RAIO_CALDEIROES, 2, RAIO_CALDEIROES))) {
            if (level.getBlockEntity(p) instanceof CaldeiraoBlockEntity c) lista.add(c);
        }
        return lista;
    }

    private int assimetria(List<PedestalBlockEntity> peds) {
        int sem_par = 0;
        for (PedestalBlockEntity p : peds) {
            BlockPos d = p.getBlockPos().subtract(worldPosition);
            BlockPos espelho = worldPosition.offset(-d.getX(), d.getY(), -d.getZ());
            if (!(level.getBlockEntity(espelho) instanceof PedestalBlockEntity e) || e.item().isEmpty()) sem_par++;
        }
        return sem_par;
    }

    public void iniciar(Player jogador, ItemStack cajado) {
        if (ativo) {
            aviso(jogador, "A infusão já está em andamento.", ChatFormatting.GRAY);
            return;
        }
        if (item.isEmpty()) {
            aviso(jogador, "Coloque o item central no altar primeiro.", ChatFormatting.GRAY);
            return;
        }
        List<PedestalBlockEntity> peds = pedestais();
        List<ItemStack> itens = new ArrayList<>();
        for (PedestalBlockEntity p : peds) itens.add(p.item());
        ReceitaInfusao r = ReceitaInfusao.encontrar(item, itens);
        if (r == null) {
            aviso(jogador, "Nada acontece. Confira a receita no Grimório.", ChatFormatting.GRAY);
            return;
        }
        if (r.pesquisa() != null && !Conhecimento.tem(jogador, r.pesquisa())) {
            aviso(jogador, "Você ainda não domina esta infusão (pesquisa: " + r.pesquisa().nome + ").", ChatFormatting.RED);
            return;
        }
        if (!(cajado.getItem() instanceof CajadoItem c) || CajadoItem.vis(cajado) < r.vis()) {
            aviso(jogador, "O cajado precisa de " + r.vis() + " Vis.", ChatFormatting.RED);
            return;
        }

        // Essência disponível nos caldeirões
        List<CaldeiraoBlockEntity> cals = caldeiroes();
        for (Map.Entry<Aspecto, Integer> e : r.essencia().mapa().entrySet()) {
            int soma = 0;
            for (CaldeiraoBlockEntity cal : cals) soma += cal.quantidade(e.getKey());
            if (soma < e.getValue()) {
                MutableComponent msg = Component.literal("Falta essência nos caldeirões próximos: ")
                        .withStyle(ChatFormatting.RED).append(r.essencia().texto());
                jogador.sendSystemMessage(msg);
                return;
            }
        }
        // Pagar
        c.adicionarVis(cajado, -r.vis());
        for (Map.Entry<Aspecto, Integer> e : r.essencia().mapa().entrySet()) {
            int falta = e.getValue();
            for (CaldeiraoBlockEntity cal : cals) {
                if (falta <= 0) break;
                falta -= cal.retirar(e.getKey(), falta);
            }
        }

        receita = r;
        fila.clear();
        for (PedestalBlockEntity p : peds) fila.add(p.getBlockPos().immutable());
        instabilidade = r.instabilidade() + assimetria(peds) * 2;
        progresso = 0;
        ativo = true;
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.2f);
        aviso(jogador, "A infusão começa... (instabilidade " + instabilidade + ")", ChatFormatting.LIGHT_PURPLE);
    }

    private static void aviso(Player jogador, String texto, ChatFormatting cor) {
        jogador.sendSystemMessage(Component.literal(texto).withStyle(cor));
    }

    // ------------------------------------------------------------------ andamento

    public static void tick(Level nivel, BlockPos pos, BlockState estado, AltarBlockEntity be) {
        be.tickVitrine();
        if (!be.ativo || !(nivel instanceof ServerLevel servidor)) return;
        be.progresso++;

        double ax = pos.getX() + 0.5, ay = pos.getY() + 1.3, az = pos.getZ() + 0.5;
        // Essência fluindo dos pedestais para o altar
        if (be.progresso % 2 == 0) {
            for (BlockPos p : be.fila) {
                servidor.sendParticles(ParticleTypes.ENCHANT, ax, ay, az, 0,
                        p.getX() + 0.5 - ax, p.getY() + 1.2 - ay, p.getZ() + 0.5 - az, 1.0);
            }
            servidor.sendParticles(ParticleTypes.WITCH, ax, ay, az, 2, 0.2, 0.2, 0.2, 0.02);
        }
        if (be.progresso % 40 == 0) {
            servidor.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8f, 1.4f);
        }

        if (be.progresso % INTERVALO != 0) return;

        // Instabilidade
        if (servidor.getRandom().nextInt(100) < be.instabilidade * 3) be.evento(servidor);

        if (be.fila.isEmpty()) {
            be.concluir(servidor);
            return;
        }
        BlockPos proximo = be.fila.remove(0);
        if (servidor.getBlockEntity(proximo) instanceof PedestalBlockEntity ped && !ped.item().isEmpty()) {
            ped.definirItem(ItemStack.EMPTY);
            servidor.sendParticles(ParticleTypes.END_ROD, proximo.getX() + 0.5, proximo.getY() + 1.1,
                    proximo.getZ() + 0.5, 12, 0.15, 0.15, 0.15, 0.05);
            servidor.playSound(null, proximo, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.0f);
        } else {
            be.falhar(servidor);
        }
    }

    private void evento(ServerLevel nivel) {
        if (nivel.getRandom().nextBoolean()) {
            CaldeiraoBlockEntity.liberarFluxo(nivel, worldPosition, 20);
            avisarProximos(nivel, "A infusão vacila e libera Fluxo!");
        } else {
            for (Player p : nivel.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(6))) {
                p.hurtServer(nivel, nivel.damageSources().magic(), 3.0f);
            }
            nivel.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5, 30, 1.5, 0.8, 1.5, 0.2);
            avisarProximos(nivel, "Uma descarga arcana escapa do altar!");
        }
    }

    private void falhar(ServerLevel nivel) {
        ativo = false;
        fila.clear();
        CaldeiraoBlockEntity.liberarFluxo(nivel, worldPosition, 40);
        avisarProximos(nivel, "Um ingrediente sumiu: a infusão falhou!");
        setChanged();
    }

    private void concluir(ServerLevel nivel) {
        ativo = false;
        definirItem(receita.resultado().get());
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.3, z = worldPosition.getZ() + 0.5;
        nivel.sendParticles(ParticleTypes.END_ROD, x, y, z, 50, 0.3, 0.3, 0.3, 0.15);
        nivel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 30, 0.3, 0.5, 0.3, 0.3);
        nivel.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 0.8f);
        avisarProximos(nivel, "✦ Infusão concluída: " + receita.resultado().get().getHoverName().getString() + " ✦");
    }

    private void avisarProximos(ServerLevel nivel, String texto) {
        for (Player p : nivel.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(12))) {
            p.sendSystemMessage(Component.literal(texto).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
