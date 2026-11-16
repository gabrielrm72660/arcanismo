package br.navegantes.navio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.navegantes.bloco.AssentoBlock;
import br.navegantes.bloco.FornalhaBlock;
import br.navegantes.bloco.TimaoBlock;
import br.navegantes.bloco.VelaBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Os blocos de um navio, em coordenadas locais: (dx, dz) em blocos a partir da coluna do pivô e
 * dy a partir da quilha. Antes de qualquer giro a estrutura está alinhada com o mundo, como foi montada.
 */
public final class Estrutura {
    public static final int LIMITE = 1200;

    public static final class Bloco {
        public final int dx, dy, dz;
        public final BlockState estado;
        public boolean visivel = true;
        public int indiceFornalha = -1;
        public int indiceAssento = -1;
        /** Posição local como BlockPos, só para servir de semente de sorteio no desenho. */
        public final BlockPos posLocal;
        private List<AABB> caixas;

        public Bloco(int dx, int dy, int dz, BlockState estado) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.estado = estado;
            this.posLocal = new BlockPos(dx, dy, dz);
        }

        /** Caixas de colisão em coordenadas locais (célula: x e z de d-0.5 a d+0.5, y de dy a dy+1). */
        public List<AABB> caixas() {
            if (caixas == null) {
                List<AABB> lista = new ArrayList<>();
                VoxelShape forma = estado.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                for (AABB b : forma.toAabbs()) lista.add(b.move(dx - 0.5, dy, dz - 0.5));
                caixas = lista;
            }
            return caixas;
        }
    }

    private final List<Bloco> blocos = new ArrayList<>();
    private final Map<Long, Bloco> mapa = new HashMap<>();
    private final List<Bloco> fornalhasLista = new ArrayList<>();
    private final List<Bloco> casco = new ArrayList<>();
    private final List<Bloco> fundo = new ArrayList<>();
    private final List<Bloco> assentos = new ArrayList<>();
    private Bloco timao;

    public int fornalhas, velas, altura = 1;
    public double raio = 1.0;

    public static long chave(int dx, int dy, int dz) {
        return ((long) (dx + 1024) & 0x1FFFFF) | (((long) (dy + 1024) & 0x1FFFFF) << 21) | (((long) (dz + 1024) & 0x1FFFFF) << 42);
    }

    public void adicionar(int dx, int dy, int dz, BlockState estado) {
        Bloco b = new Bloco(dx, dy, dz, estado);
        blocos.add(b);
        mapa.put(chave(dx, dy, dz), b);
    }

    public Bloco em(int dx, int dy, int dz) {
        return mapa.get(chave(dx, dy, dz));
    }

    public List<Bloco> blocos() {
        return blocos;
    }

    public List<Bloco> fornalhasLista() {
        return fornalhasLista;
    }

    /** Blocos da camada externa (usados para bater em terreno). */
    public List<Bloco> casco() {
        return casco;
    }

    /** Blocos da camada mais baixa (usados para achar a água). */
    public List<Bloco> fundo() {
        return fundo;
    }

    /** O timão (a metade de baixo, se for duplo). */
    public Bloco timao() {
        return timao;
    }

    /** Assentos, do mais perto do timão para o mais longe; o primeiro é o do piloto. */
    public List<Bloco> assentos() {
        return assentos;
    }

    public Direction frenteDoTimao() {
        return timao == null ? Direction.NORTH : timao.estado.getValue(TimaoBlock.FACING).getOpposite();
    }

    /** Recalcula contagens, limites e quais blocos ficam escondidos. Chame depois de adicionar tudo. */
    public void finalizar() {
        fornalhasLista.clear();
        assentos.clear();
        casco.clear();
        fundo.clear();
        fornalhas = 0;
        velas = 0;
        timao = null;
        int maxY = 0, minY = Integer.MAX_VALUE;
        double r2 = 1.0;
        for (Bloco b : blocos) {
            maxY = Math.max(maxY, b.dy);
            minY = Math.min(minY, b.dy);
            r2 = Math.max(r2, (Math.abs(b.dx) + 0.5) * (Math.abs(b.dx) + 0.5) + (Math.abs(b.dz) + 0.5) * (Math.abs(b.dz) + 0.5));
            Block bloco = b.estado.getBlock();
            if (bloco instanceof FornalhaBlock) {
                b.indiceFornalha = fornalhasLista.size();
                fornalhas++;
                fornalhasLista.add(b);
            } else if (bloco instanceof VelaBlock) {
                velas++;
            } else if (bloco instanceof AssentoBlock) {
                assentos.add(b);
            } else if (bloco instanceof TimaoBlock) {
                boolean baixo = b.estado.getValue(TimaoBlock.HALF) == DoubleBlockHalf.LOWER;
                if (timao == null || (baixo && timao.estado.getValue(TimaoBlock.HALF) != DoubleBlockHalf.LOWER)) {
                    timao = b;
                }
            }
        }
        final Bloco ref = timao;
        assentos.sort((a, c) -> Double.compare(distancia2(a, ref), distancia2(c, ref)));
        for (int i = 0; i < assentos.size(); i++) assentos.get(i).indiceAssento = i;
        altura = maxY + 1;
        raio = Math.sqrt(r2);
        for (Bloco b : blocos) {
            if (b.dy == minY) fundo.add(b);
            boolean aberto = false;
            boolean escondido = true;
            for (Direction d : Direction.values()) {
                Bloco v = em(b.dx + d.getStepX(), b.dy + d.getStepY(), b.dz + d.getStepZ());
                if (v == null || !v.estado.isSolidRender()) escondido = false;
                if (v == null && d.getAxis().isHorizontal()) aberto = true;
            }
            b.visivel = !escondido;
            if (aberto) casco.add(b);
        }
        if (casco.isEmpty()) casco.addAll(blocos);
    }

    private static double distancia2(Bloco a, Bloco ref) {
        int rx = ref == null ? 0 : ref.dx, ry = ref == null ? 0 : ref.dy, rz = ref == null ? 0 : ref.dz;
        double dx = a.dx - rx, dy = a.dy - ry, dz = a.dz - rz;
        return dx * dx + dy * dy + dz * dz;
    }

    // ---------- rede (usa os ids de estado da sessão; só vale enquanto o jogo está aberto)
    public String codificar() {
        StringBuilder sb = new StringBuilder(blocos.size() * 14);
        for (Bloco b : blocos) {
            sb.append(b.dx).append(',').append(b.dy).append(',').append(b.dz).append(',')
                    .append(Block.getId(b.estado)).append(';');
        }
        return sb.toString();
    }

    public static Estrutura decodificar(String texto) {
        Estrutura e = new Estrutura();
        if (texto != null && !texto.isEmpty()) {
            for (String parte : texto.split(";")) {
                if (parte.isEmpty()) continue;
                String[] c = parte.split(",");
                if (c.length < 4) continue;
                try {
                    BlockState estado = Block.stateById(Integer.parseInt(c[3]));
                    if (estado == null) estado = Blocks.AIR.defaultBlockState();
                    e.adicionar(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]), estado);
                } catch (NumberFormatException ignorado) {
                    // pedaço corrompido: pula
                }
            }
        }
        e.finalizar();
        return e;
    }

    // ---------- disco (usa os nomes dos blocos, estável entre sessões)
    public void salvar(ValueOutput saida) {
        List<BlockState> paleta = new ArrayList<>();
        Map<BlockState, Integer> indice = new HashMap<>();
        int[] dados = new int[blocos.size() * 4];
        int i = 0;
        for (Bloco b : blocos) {
            Integer idx = indice.get(b.estado);
            if (idx == null) {
                idx = paleta.size();
                paleta.add(b.estado);
                indice.put(b.estado, idx);
            }
            dados[i++] = b.dx;
            dados[i++] = b.dy;
            dados[i++] = b.dz;
            dados[i++] = idx;
        }
        saida.store("paleta", BlockState.CODEC.listOf(), paleta);
        saida.putIntArray("dados", dados);
    }

    public static Estrutura carregar(ValueInput entrada) {
        Estrutura e = new Estrutura();
        List<BlockState> paleta = entrada.read("paleta", BlockState.CODEC.listOf()).orElse(List.of());
        int[] dados = entrada.getIntArray("dados").orElse(new int[0]);
        for (int i = 0; i + 3 < dados.length; i += 4) {
            int idx = dados[i + 3];
            if (idx < 0 || idx >= paleta.size()) continue;
            e.adicionar(dados[i], dados[i + 1], dados[i + 2], paleta.get(idx));
        }
        e.finalizar();
        return e;
    }
}
