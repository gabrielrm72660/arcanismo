package br.navegantes.navio;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.navegantes.bloco.AssentoBlock;
import br.navegantes.bloco.FornalhaBlock;
import br.navegantes.bloco.TimaoBlock;
import br.navegantes.bloco.VelaBlock;
import br.navegantes.registro.ModEntidades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** Transforma a estrutura colada ao redor de um Timão em um navio (entidade). */
public final class Montador {
    /** Sem avisar vizinhos e sem soltar itens: a estrutura inteira sai de uma vez. */
    private static final int SEM_AVISO = 2 | 16 | 32;

    private Montador() {}

    public static boolean montar(ServerLevel nivel, BlockPos posTimao, Player jogador) {
        BlockState estadoTimao = nivel.getBlockState(posTimao);
        if (!(estadoTimao.getBlock() instanceof TimaoBlock)) return false;

        Cola cola = Cola.de(nivel);
        List<BlockPos> blocos = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();
        ArrayDeque<BlockPos> fila = new ArrayDeque<>();
        blocos.add(posTimao);
        vistos.add(posTimao.asLong());
        fila.add(posTimao);
        int colados = 0, ignorados = 0;

        while (!fila.isEmpty()) {
            BlockPos atual = fila.poll();
            for (Direction d : Direction.values()) {
                BlockPos v = atual.relative(d);
                if (vistos.contains(v.asLong())) continue;
                BlockState e = nivel.getBlockState(v);
                if (e.isAir() || e.getBlock() instanceof LiquidBlock) continue;
                boolean doMod = e.getBlock() instanceof TimaoBlock || e.getBlock() instanceof FornalhaBlock
                        || e.getBlock() instanceof VelaBlock || e.getBlock() instanceof AssentoBlock;
                if (!doMod && !cola.contem(v)) continue;
                vistos.add(v.asLong());
                if (e.hasBlockEntity()) {
                    ignorados++;
                    continue;
                }
                blocos.add(v.immutable());
                if (!doMod) colados++;
                fila.add(v.immutable());
                if (blocos.size() > Estrutura.LIMITE) {
                    Mensagens.chat(jogador, "Estrutura grande demais (limite de " + Estrutura.LIMITE + " blocos).");
                    return false;
                }
            }
        }

        if (blocos.size() < 2) {
            Mensagens.chat(jogador, "Nenhum bloco colado encostado no Timão. Use a Cola Naval nos blocos do casco.");
            return false;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : blocos) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }

        // A quilha precisa tocar a água.
        int quilha = 0, molhados = 0;
        double superficie = Double.NEGATIVE_INFINITY;
        Direction[] lados = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (BlockPos p : blocos) {
            if (p.getY() != minY) continue;
            quilha++;
            boolean molhado = false;
            FluidState proprio = nivel.getFluidState(p);
            if (proprio.is(FluidTags.WATER)) {
                molhado = true;
                superficie = Math.max(superficie, alturaAgua(nivel, p));
            }
            for (Direction d : lados) {
                BlockPos v = p.relative(d);
                if (nivel.getFluidState(v).is(FluidTags.WATER)) {
                    molhado = true;
                    superficie = Math.max(superficie, alturaAgua(nivel, v));
                }
            }
            if (molhado) molhados++;
        }
        if (molhados == 0 || molhados * 4 < quilha) {
            Mensagens.chat(jogador, "O navio precisa estar na água: a parte de baixo do casco deve tocar a água.");
            return false;
        }

        int altura = 0;
        for (BlockPos p : blocos) altura = Math.max(altura, p.getY() - minY + 1);
        double calado = Mth.clamp(superficie - minY, 0.35, Math.max(0.5, altura * 0.8));

        int pivoX = minX + (maxX - minX) / 2;
        int pivoZ = minZ + (maxZ - minZ) / 2;

        Estrutura estrutura = new Estrutura();
        for (BlockPos p : blocos) {
            estrutura.adicionar(p.getX() - pivoX, p.getY() - minY, p.getZ() - pivoZ, nivel.getBlockState(p));
        }
        estrutura.finalizar();

        NavioEntidade navio = ModEntidades.NAVIO.get().create(nivel, EntitySpawnReason.TRIGGERED);
        if (navio == null) return false;
        navio.definir(estrutura, calado);
        navio.snapTo(pivoX + 0.5, minY, pivoZ + 0.5, 0.0f, 0.0f);

        // Tira os blocos do mundo e devolve a água onde o casco estava dentro dela.
        for (BlockPos p : blocos) {
            nivel.setBlock(p, Blocks.AIR.defaultBlockState(), SEM_AVISO);
        }
        encherDeAgua(nivel, blocos, (int) Math.floor(superficie - 0.01));
        cola.remover(blocos);

        nivel.addFreshEntity(navio);

        String extra = ignorados > 0 ? " (" + ignorados + " blocos com inventário ficaram de fora)" : "";
        Mensagens.chat(jogador, "Navio montado com " + blocos.size() + " blocos!" + extra
                + " Ele começa com a âncora lançada. Sente no assento (ou clique no Timão) e aperte Ctrl para soltá-la. "
                + "Motores/fornalhas: " + estrutura.fornalhas + ", velas: " + estrutura.velas + ".");
        if (estrutura.timao() != null && estrutura.fornalhas == 0 && estrutura.velas == 0) {
            Mensagens.chat(jogador, "Dica: sem Motor, Fornalha a Vapor nem Vela o navio não anda.");
        }
        return true;
    }

    /** Altura da superfície da água (em coordenadas do mundo) na coluna que contém {@code agua}. */
    public static double alturaAgua(ServerLevel nivel, BlockPos agua) {
        int y = agua.getY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(agua.getX(), y, agua.getZ());
        for (int i = 0; i < 64; i++) {
            m.setY(y + 1);
            if (!nivel.getFluidState(m).is(FluidTags.WATER)) break;
            y++;
        }
        m.setY(y);
        FluidState f = nivel.getFluidState(m);
        return y + f.getHeight(nivel, m);
    }

    private static void encherDeAgua(ServerLevel nivel, List<BlockPos> removidos, int yAgua) {
        Set<Long> candidatos = new HashSet<>();
        for (BlockPos p : removidos) {
            if (p.getY() <= yAgua) candidatos.add(p.asLong());
        }
        ArrayDeque<BlockPos> fila = new ArrayDeque<>();
        Set<Long> feitos = new HashSet<>();
        for (BlockPos p : removidos) {
            if (!candidatos.contains(p.asLong())) continue;
            for (Direction d : Direction.values()) {
                if (nivel.getFluidState(p.relative(d)).is(FluidTags.WATER) && !candidatos.contains(p.relative(d).asLong())) {
                    fila.add(p);
                    feitos.add(p.asLong());
                    break;
                }
            }
        }
        while (!fila.isEmpty()) {
            BlockPos p = fila.poll();
            nivel.setBlock(p, Blocks.WATER.defaultBlockState(), 3);
            for (Direction d : Direction.values()) {
                BlockPos v = p.relative(d);
                if (candidatos.contains(v.asLong()) && feitos.add(v.asLong())) fila.add(v);
            }
        }
    }
}
