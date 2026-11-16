package br.navegantes.navio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import br.navegantes.bloco.AssentoBlock;
import br.navegantes.bloco.FornalhaBlock;
import br.navegantes.bloco.MotorBlock;
import br.navegantes.bloco.TimaoBlock;
import br.navegantes.registro.ModEntidades;
import br.navegantes.registro.ModItens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * O navio: uma estrutura de blocos que se move como uma coisa só.
 *
 * <p>A entidade fica no pivô (centro do casco, na altura da quilha). Cada bloco tem uma posição
 * local (dx, dy, dz) e o conjunto gira em torno do eixo Y pelo "rumo". O servidor decide o
 * movimento; o cliente só recebe posição, rumo e a lista de blocos. Quem anda no convés é
 * levado junto e colide com os blocos do navio (feito à mão, pois o jogo não conhece esses blocos).
 */
public class NavioEntidade extends Entity {
    private static final EntityDataAccessor<String> DADOS_ESTRUTURA =
            SynchedEntityData.defineId(NavioEntidade.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DADOS_ANCORA =
            SynchedEntityData.defineId(NavioEntidade.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DADOS_ACESAS =
            SynchedEntityData.defineId(NavioEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DADOS_RUMO =
            SynchedEntityData.defineId(NavioEntidade.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DADOS_LUGARES =
            SynchedEntityData.defineId(NavioEntidade.class, EntityDataSerializers.STRING);

    private static final double DEGRAU = 0.6;
    private static final int COMBUSTIVEL_MAXIMO = 72000;

    /** Quem está sobre o convés: posição no referencial do navio e onde o deixamos no mundo. */
    private static final class Carga {
        double lx, ly, lz;
        double mundoX, mundoY, mundoZ;
        float rumo;
        int semApoio;
        int visto;
    }

    private Estrutura estrutura = new Estrutura();
    private double calado = 0.6;
    private float rumo;
    private float rumoAnterior;
    private float velocidade;
    private float giro;
    private double velVertical;
    private int[] combustivel = new int[0];
    private double[] potencias = new double[0];
    private int[] consumos = new int[0];
    private boolean ctrlAnterior;
    private boolean bloqueado;
    private int semAgua;
    private double[][] amostras = new double[0][];

    /** Quem está em qual assento: id da entidade -> índice do assento (-1 = em pé, atrás do timão). */
    private final Map<Integer, Integer> lugares = new HashMap<>();

    /** Pose de cada tick no cliente, só para desenhar: o desenho interpola entre o tick anterior e o atual. */
    private boolean iniciado;
    private double sx, sy, sz, sxAnt, syAnt, szAnt;

    private final List<PecaInterativa> pecas = new ArrayList<>();
    private final List<Estrutura.Bloco> blocosDasPecas = new ArrayList<>();
    private List<Estrutura.Bloco> alvosPecas;
    private final Map<Integer, Carga> cargas = new HashMap<>();

    public NavioEntidade(EntityType<?> tipo, Level nivel) {
        super(tipo, nivel);
    }

    // ------------------------------------------------------------------ dados e estado

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        construtor.define(DADOS_ESTRUTURA, "");
        construtor.define(DADOS_ANCORA, false);
        construtor.define(DADOS_ACESAS, 0);
        construtor.define(DADOS_RUMO, 0.0f);
        construtor.define(DADOS_LUGARES, "");
    }

    /** Chamado pelo Montador, no servidor, logo depois de criar a entidade. */
    public void definir(Estrutura nova, double caladoInicial) {
        this.estrutura = nova;
        this.calado = caladoInicial;
        this.combustivel = new int[nova.fornalhas];
        this.alvosPecas = null;
        prepararMotores();
        this.rumo = 0.0f;
        this.rumoAnterior = 0.0f;
        calcularAmostras();
        getEntityData().set(DADOS_ESTRUTURA, nova.codificar());
        getEntityData().set(DADOS_RUMO, 0.0f);
        getEntityData().set(DADOS_ANCORA, true); // o navio nasce com a âncora lançada
        atualizarCaixa();
    }

    /** Guarda a força e o consumo de cada motor/fornalha (cada tipo de bloco tem os seus). */
    private void prepararMotores() {
        int n = estrutura.fornalhasLista().size();
        potencias = new double[n];
        consumos = new int[n];
        for (Estrutura.Bloco b : estrutura.fornalhasLista()) {
            int i = b.indiceFornalha;
            if (i < 0 || i >= n) continue;
            if (b.estado.getBlock() instanceof FornalhaBlock f) {
                potencias[i] = f.potencia();
                consumos[i] = f.consumo();
            } else {
                potencias[i] = 0.10;
                consumos[i] = 1;
            }
        }
    }

    public Estrutura estrutura() {
        return estrutura;
    }

    public boolean ancora() {
        return getEntityData().get(DADOS_ANCORA);
    }

    public int acesasBits() {
        return getEntityData().get(DADOS_ACESAS);
    }

    public float rumo() {
        return rumo;
    }

    public float rumoRender(float parcial) {
        return Mth.rotLerp(parcial, rumoAnterior, rumo);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> acessor) {
        super.onSyncedDataUpdated(acessor);
        if (!level().isClientSide()) return;
        if (DADOS_ESTRUTURA.equals(acessor)) {
            estrutura = Estrutura.decodificar(getEntityData().get(DADOS_ESTRUTURA));
            calcularAmostras();
            atualizarCaixa();
        } else if (DADOS_LUGARES.equals(acessor)) {
            decodificarLugares(getEntityData().get(DADOS_LUGARES));
        } else if (DADOS_RUMO.equals(acessor) && tickCount <= 1) {
            rumo = getEntityData().get(DADOS_RUMO);
            rumoAnterior = rumo;
        }
    }

    // ------------------------------------------------------------------ lugares (quem senta onde)

    private void publicarLugares() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : lugares.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        getEntityData().set(DADOS_LUGARES, sb.toString());
    }

    private void decodificarLugares(String texto) {
        lugares.clear();
        if (texto.isEmpty()) return;
        for (String par : texto.split(",")) {
            int i = par.indexOf(':');
            if (i < 0) continue;
            try {
                lugares.put(Integer.parseInt(par.substring(0, i)), Integer.parseInt(par.substring(i + 1)));
            } catch (NumberFormatException ignorado) {
                // ignora um par malformado
            }
        }
    }

    /** Índice do assento de um passageiro (-1 = em pé). Sem registro, vale a ordem de embarque. */
    private int lugarDe(Entity passageiro) {
        Integer l = lugares.get(passageiro.getId());
        if (l != null) return l;
        int ordem = getPassengers().indexOf(passageiro);
        return ordem >= 0 && ordem < estrutura.assentos().size() ? ordem : -1;
    }

    /** O assento do piloto é o mais perto do timão; sem assentos, o piloto fica em pé atrás do timão. */
    private int lugarDoPiloto() {
        return estrutura.assentos().isEmpty() ? -1 : 0;
    }

    private int capacidade() {
        return Math.max(1, estrutura.assentos().size());
    }

    private void limparLugares() {
        boolean mudou = lugares.keySet().removeIf(id -> {
            for (Entity p : getPassengers()) {
                if (p.getId() == id) return false;
            }
            return true;
        });
        if (mudou) publicarLugares();
    }

    private void atualizarCaixa() {
        setPos(getX(), getY(), getZ());
    }

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        if (estrutura == null || estrutura.blocos().isEmpty()) {
            return super.makeBoundingBox(pos);
        }
        double r = estrutura.raio;
        return new AABB(pos.x - r, pos.y, pos.z - r, pos.x + r, pos.y + estrutura.altura, pos.z + r);
    }

    private void calcularAmostras() {
        List<double[]> lista = new ArrayList<>();
        lista.add(new double[]{0.0, 0.0});
        Estrutura.Bloco maxX = null, minX = null, maxZ = null, minZ = null;
        for (Estrutura.Bloco b : estrutura.fundo()) {
            if (maxX == null || b.dx > maxX.dx) maxX = b;
            if (minX == null || b.dx < minX.dx) minX = b;
            if (maxZ == null || b.dz > maxZ.dz) maxZ = b;
            if (minZ == null || b.dz < minZ.dz) minZ = b;
        }
        for (Estrutura.Bloco b : new Estrutura.Bloco[]{maxX, minX, maxZ, minZ}) {
            if (b != null) lista.add(new double[]{b.dx, b.dz});
        }
        amostras = lista.toArray(new double[0][]);
    }

    // ------------------------------------------------------------------ geometria

    /** Posição no mundo de um ponto local (x, y, z em blocos a partir do pivô e da quilha). */
    public Vec3 paraMundo(double lx, double ly, double lz) {
        double r = Math.toRadians(rumo);
        double c = Math.cos(r), s = Math.sin(r);
        return new Vec3(getX() + lx * c + lz * s, getY() + ly, getZ() - lx * s + lz * c);
    }

    private double[] vetorParaMundo(double lx, double lz) {
        double r = Math.toRadians(rumo);
        double c = Math.cos(r), s = Math.sin(r);
        return new double[]{lx * c + lz * s, -lx * s + lz * c};
    }

    private double[] vetorParaLocal(double wx, double wz) {
        double r = Math.toRadians(rumo);
        double c = Math.cos(r), s = Math.sin(r);
        return new double[]{wx * c - wz * s, wx * s + wz * c};
    }

    private double[] frenteMundo(float yaw) {
        Direction d = estrutura.frenteDoTimao();
        double r = Math.toRadians(yaw);
        double c = Math.cos(r), s = Math.sin(r);
        double lx = d.getStepX(), lz = d.getStepZ();
        return new double[]{lx * c + lz * s, -lx * s + lz * c};
    }

    /** Onde o piloto fica de pé: atrás do timão, se o espaço estiver livre. */
    private Vec3 localPiloto() {
        Estrutura.Bloco t = estrutura.timao();
        if (t == null) return new Vec3(0.0, estrutura.altura, 0.0);
        Direction atras = estrutura.frenteDoTimao().getOpposite();
        int ax = t.dx + atras.getStepX();
        int az = t.dz + atras.getStepZ();
        if (estrutura.em(ax, t.dy, az) == null) return new Vec3(ax, t.dy, az);
        return new Vec3(t.dx, t.dy, t.dz);
    }

    /** Onde (no referencial do navio) os pés de um passageiro ficam: no assento ou atrás do timão. */
    private Vec3 localDe(Entity passageiro) {
        int lugar = lugarDe(passageiro);
        List<Estrutura.Bloco> assentos = estrutura.assentos();
        if (lugar >= 0 && lugar < assentos.size()) {
            Estrutura.Bloco b = assentos.get(lugar);
            return new Vec3(b.dx, b.dy + 0.05, b.dz);
        }
        return localPiloto();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passageiro, EntityDimensions dimensoes, float escala) {
        Vec3 l = localDe(passageiro);
        double[] w = vetorParaMundo(l.x, l.z);
        Vec3 ponto = passageiro.getVehicleAttachmentPoint(this);
        return new Vec3(w[0] + ponto.x, l.y + ponto.y, w[1] + ponto.z);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity quem) {
        Vec3 l = localDe(quem);
        int lugar = lugarDe(quem);
        double sobe = lugar >= 0 && lugar < estrutura.assentos().size() ? 0.6 : 0.0;
        return paraMundo(l.x, l.y + sobe, l.z).add(0.0, 0.1, 0.0);
    }

    @Override
    protected boolean canAddPassenger(Entity passageiro) {
        return getPassengers().size() < capacidade();
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (estrutura.blocos().isEmpty()) {
            if (!level().isClientSide()) discard();
            return;
        }
        if (level() instanceof ServerLevel nivel) {
            tickServidor(nivel);
        } else {
            tickCliente();
        }
        Iterator<Carga> it = cargas.values().iterator();
        while (it.hasNext()) {
            if (it.next().visto < tickCount - 5) it.remove();
        }
    }

    private ServerPlayer pilotoAtual() {
        int lugarPiloto = lugarDoPiloto();
        for (Entity p : getPassengers()) {
            if (p instanceof ServerPlayer jogador && lugarDe(p) == lugarPiloto) return jogador;
        }
        return null;
    }

    /** Força total agora: velas + motores/fornalhas que ainda têm combustível. */
    private double potencia() {
        double total = Math.min(estrutura.velas, 6) * 0.02;
        for (int i = 0; i < combustivel.length && i < potencias.length; i++) {
            if (combustivel[i] > 0) total += potencias[i];
        }
        return total;
    }

    /** Quanto tempo (em ticks) o combustível mais cheio ainda dura, já descontado o consumo. */
    private int autonomia() {
        int melhor = 0;
        for (int i = 0; i < combustivel.length; i++) {
            int consumo = i < consumos.length && consumos[i] > 0 ? consumos[i] : 1;
            melhor = Math.max(melhor, combustivel[i] / consumo);
        }
        return melhor;
    }

    private void tickServidor(ServerLevel nivel) {
        manterPecas();
        limparLugares();

        ServerPlayer piloto = pilotoAtual();
        Input entrada = piloto != null ? piloto.getLastClientInput() : Input.EMPTY;
        // Ctrl lança/recolhe a âncora; Shift levanta do assento (o próprio jogo cuida disso).
        if (piloto != null && entrada.sprint() && !ctrlAnterior) {
            alternarAncora(piloto);
        }
        ctrlAnterior = entrada.sprint();

        boolean ancorado = ancora();
        boolean freia = piloto != null && entrada.backward() && !entrada.forward();

        double potencia = potencia();
        double massa = 1.0 / (1.0 + Math.max(0, estrutura.blocos().size() - 40) / 240.0);
        double vmax = Math.min(0.6, potencia) * massa;

        // Com a âncora solta o navio anda sozinho; motores e fornalhas queimam combustível enquanto
        // empurram (e param de gastar se o navio estiver encalhado).
        int bits = 0;
        if (!ancorado && !bloqueado) {
            for (int i = 0; i < combustivel.length; i++) {
                if (combustivel[i] > 0) {
                    if (i < 31) bits |= 1 << i;
                    int consumo = i < consumos.length && consumos[i] > 0 ? consumos[i] : 1;
                    combustivel[i] = Math.max(0, combustivel[i] - consumo);
                }
            }
        }

        double alvo = ancorado ? 0.0 : (freia ? -vmax * 0.35 : vmax);
        if (ancorado) {
            velocidade *= 0.85f;
        } else {
            double k = (freia && velocidade > 0.0f) ? 0.12 : 0.04;
            velocidade += (float) ((alvo - velocidade) * k);
        }
        if (alvo == 0.0 && Math.abs(velocidade) < 0.002f) velocidade = 0.0f;

        float alvoGiro = 0.0f;
        if (piloto != null && !ancorado) alvoGiro = (entrada.left() ? 1 : 0) - (entrada.right() ? 1 : 0);
        if (velocidade < -0.01f) alvoGiro = -alvoGiro;
        double taxa = 2.4 / (1.0 + Math.max(0.0, estrutura.raio - 3.0) / 6.0);
        double rapidez = 0.25 + 0.75 * Math.min(1.0, Math.abs(velocidade) / 0.1);
        giro += (float) ((alvoGiro * taxa * rapidez - giro) * 0.2);
        if (alvoGiro == 0.0f && Math.abs(giro) < 0.02f) giro = 0.0f;
        if (ancorado) giro = 0.0f;

        if (velocidade != 0.0f || giro != 0.0f) mover();
        flutuar(nivel);

        getEntityData().set(DADOS_ACESAS, bits);
        getEntityData().set(DADOS_RUMO, rumo);

        posicionarPecas();
        levarEntidades(nivel);

        if (piloto != null && tickCount % 10 == 0) {
            String aviso = ancorado ? "   [ÂNCORA LANÇADA - Ctrl solta]"
                    : (potencia <= 0.0 ? "   [SEM FORÇA - abasteça o Motor ou erga Velas]" : "");
            Mensagens.barra(piloto, String.format("Velocidade %.1f b/s   Combustível %s%s",
                    Math.abs(velocidade) * 20.0f, tempo(autonomia()), aviso));
        }
    }

    private void tickCliente() {
        atualizarCliente();

        int bits = acesasBits();
        if (bits != 0 && tickCount % 4 == 0) {
            for (Estrutura.Bloco f : estrutura.fornalhasLista()) {
                int i = f.indiceFornalha;
                if (i < 31 && ((bits >> i) & 1) != 0) {
                    Vec3 w = paraMundo(f.dx, f.dy + 1.1, f.dz);
                    level().addParticle(ParticleTypes.LARGE_SMOKE, w.x, w.y, w.z, 0.0, 0.05, 0.0);
                }
            }
        }
    }

    /**
     * Uma vez por tick no cliente: guarda a pose do tick. O jogo não interpola esta entidade por
     * conta própria (a posição "salta" a cada pacote), então o desenho interpola entre estas poses.
     */
    private void atualizarCliente() {
        rumoAnterior = rumo;
        rumo = getEntityData().get(DADOS_RUMO);
        if (!iniciado) {
            sx = sxAnt = getX();
            sy = syAnt = getY();
            sz = szAnt = getZ();
            rumoAnterior = rumo;
            iniciado = true;
        } else {
            sxAnt = sx;
            syAnt = sy;
            szAnt = sz;
            sx = getX();
            sy = getY();
            sz = getZ();
        }
    }

    /** Posição suave para desenhar (cliente). */
    public Vec3 posRender(float parcial) {
        if (!iniciado) return new Vec3(getX(), getY(), getZ());
        return new Vec3(Mth.lerp(parcial, sxAnt, sx), Mth.lerp(parcial, syAnt, sy), Mth.lerp(parcial, szAnt, sz));
    }

    // ------------------------------------------------------------------ movimento no servidor

    private void mover() {
        bloqueado = false;
        float rumoNovo = rumo + giro;
        double[] f = frenteMundo(rumoNovo);
        double nx = getX() + f[0] * velocidade;
        double nz = getZ() + f[1] * velocidade;
        double y = getY();

        if (!colide(nx, y, nz, rumoNovo)) {
            aplicar(nx, nz, rumoNovo);
        } else if (!colide(nx, y, nz, rumo)) {
            giro = 0.0f;
            aplicar(nx, nz, rumo);
        } else if (!colide(nx, y, getZ(), rumo)) {
            giro = 0.0f;
            aplicar(nx, getZ(), rumo);
        } else if (!colide(getX(), y, nz, rumo)) {
            giro = 0.0f;
            aplicar(getX(), nz, rumo);
        } else if (!colide(getX(), y, getZ(), rumoNovo)) {
            velocidade *= 0.5f;
            aplicar(getX(), getZ(), rumoNovo);
        } else {
            velocidade = 0.0f;
            giro = 0.0f;
            bloqueado = true;
        }
    }

    private void aplicar(double x, double z, float yaw) {
        rumo = yaw;
        setYRot(yaw);
        setPos(x, getY(), z);
    }

    /** Algum bloco do casco, nessa posição e rumo, bateria em algo sólido do mundo? */
    private boolean colide(double px, double py, double pz, float yaw) {
        double r = Math.toRadians(yaw);
        double c = Math.cos(r), s = Math.sin(r);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        Level nivel = level();
        for (Estrutura.Bloco b : estrutura.casco()) {
            double wx = px + b.dx * c + b.dz * s;
            double wz = pz - b.dx * s + b.dz * c;
            double wy = py + b.dy + 0.5;
            m.set(Mth.floor(wx), Mth.floor(wy), Mth.floor(wz));
            BlockState e = nivel.getBlockState(m);
            if (e.isAir() || e.is(Blocks.LILY_PAD)) continue;
            if (!e.getCollisionShape(nivel, m).isEmpty()) return true;
        }
        return false;
    }

    private double superficieEm(ServerLevel nivel, double wx, double wz) {
        int topo = Mth.floor(getY() + estrutura.altura + 1.0);
        int base = Mth.floor(getY() - 3.0);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = topo; y >= base; y--) {
            m.set(Mth.floor(wx), y, Mth.floor(wz));
            FluidState f = nivel.getFluidState(m);
            if (f.is(FluidTags.WATER)) return y + f.getHeight(nivel, m);
        }
        return Double.NaN;
    }

    private void flutuar(ServerLevel nivel) {
        double soma = 0.0;
        int n = 0;
        for (double[] a : amostras) {
            double[] w = vetorParaMundo(a[0], a[1]);
            double s = superficieEm(nivel, getX() + w[0], getZ() + w[1]);
            if (!Double.isNaN(s)) {
                soma += s;
                n++;
            }
        }
        if (n == 0) {
            semAgua++;
            velVertical = Math.max(velVertical - 0.04, -0.5);
            velocidade *= 0.9f;
            double ny = getY() + velVertical;
            if (!colide(getX(), ny, getZ(), rumo)) {
                setPos(getX(), ny, getZ());
            } else {
                velVertical = 0.0;
            }
            return;
        }
        semAgua = 0;
        velVertical = 0.0;
        double alvo = soma / n - calado;
        double dy = Mth.clamp((alvo - getY()) * 0.2, -0.12, 0.12);
        if (Math.abs(dy) > 1.0E-4) {
            double ny = getY() + dy;
            if (!colide(getX(), ny, getZ(), rumo)) setPos(getX(), ny, getZ());
        }
    }

    // ------------------------------------------------------------------ peças clicáveis

    private static int tipoPeca(Estrutura.Bloco b) {
        Block bloco = b.estado.getBlock();
        if (bloco instanceof TimaoBlock) return PecaInterativa.TIMAO;
        if (bloco instanceof AssentoBlock) return PecaInterativa.ASSENTO;
        if (bloco instanceof FornalhaBlock) return PecaInterativa.FORNALHA;
        return -1;
    }

    private static int indicePeca(Estrutura.Bloco b, int tipo) {
        return tipo == PecaInterativa.ASSENTO ? b.indiceAssento : (tipo == PecaInterativa.FORNALHA ? b.indiceFornalha : 0);
    }

    /** Blocos que ganham uma peça clicável: as duas metades do timão, cada assento, cada motor/fornalha. */
    private List<Estrutura.Bloco> alvosDasPecas() {
        if (alvosPecas == null) {
            List<Estrutura.Bloco> lista = new ArrayList<>();
            for (Estrutura.Bloco b : estrutura.blocos()) {
                if (tipoPeca(b) >= 0) lista.add(b);
            }
            alvosPecas = lista;
        }
        return alvosPecas;
    }

    private void manterPecas() {
        List<Estrutura.Bloco> alvos = alvosDasPecas();
        boolean ok = pecas.size() == alvos.size();
        if (ok) {
            for (PecaInterativa p : pecas) {
                if (p.isRemoved()) {
                    ok = false;
                    break;
                }
            }
        }
        if (ok) return;
        for (PecaInterativa p : pecas) p.discard();
        pecas.clear();
        blocosDasPecas.clear();
        for (Estrutura.Bloco b : alvos) {
            int tipo = tipoPeca(b);
            criarPeca(tipo, indicePeca(b, tipo), b);
        }
    }

    private void criarPeca(int tipo, int indice, Estrutura.Bloco bloco) {
        PecaInterativa p = ModEntidades.PECA.get().create(level(), EntitySpawnReason.TRIGGERED);
        if (p == null) return;
        p.configurar(this, tipo, indice);
        Vec3 w = paraMundo(bloco.dx, bloco.dy + 0.05, bloco.dz);
        p.setPos(w.x, w.y, w.z);
        level().addFreshEntity(p);
        pecas.add(p);
        blocosDasPecas.add(bloco);
    }

    private void posicionarPecas() {
        for (int i = 0; i < pecas.size(); i++) {
            Estrutura.Bloco b = blocosDasPecas.get(i);
            Vec3 w = paraMundo(b.dx, b.dy + 0.05, b.dz);
            pecas.get(i).setPos(w.x, w.y, w.z);
        }
    }

    /** Clique numa peça (só no servidor). */
    public void aoClicarPeca(PecaInterativa peca, Player jogador, InteractionHand mao) {
        ItemStack naMao = jogador.getItemInHand(mao);
        if (naMao.is(ModItens.CHAVE_INGLESA.get())) {
            desmontar(jogador);
            return;
        }
        if (peca.tipo() == PecaInterativa.TIMAO) {
            if (jogador.isShiftKeyDown() && naMao.is(ModItens.COLA_NAVAL.get())) {
                desmontar(jogador);
                return;
            }
            sentar(jogador, lugarDoPiloto());
            return;
        }
        if (peca.tipo() == PecaInterativa.ASSENTO) {
            sentar(jogador, peca.indice());
            return;
        }

        int i = peca.indice();
        if (i < 0 || i >= combustivel.length) return;
        boolean motor = i < estrutura.fornalhasLista().size()
                && estrutura.fornalhasLista().get(i).estado.getBlock() instanceof MotorBlock;
        String nome = motor ? "Motor" : "Fornalha";
        int consumo = i < consumos.length && consumos[i] > 0 ? consumos[i] : 1;
        if (!naMao.isEmpty() && level().fuelValues().isFuel(naMao) && naMao.getMaxStackSize() > 1) {
            int queima = level().fuelValues().burnDuration(naMao);
            if (queima <= 0 || combustivel[i] + queima > COMBUSTIVEL_MAXIMO) {
                Mensagens.barra(jogador, nome + " cheio.");
                return;
            }
            combustivel[i] += queima;
            if (!jogador.getAbilities().instabuild) naMao.shrink(1);
            Mensagens.barra(jogador, nome + " abastecido: " + tempo(combustivel[i] / consumo) + " de combustível.");
            level().playSound(null, blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8f, 1.0f);
        } else {
            Mensagens.barra(jogador, nome + ": " + tempo(combustivel[i] / consumo) + " de combustível. Clique com carvão na mão.");
        }
    }

    /** Senta o jogador (lugar = índice do assento; -1 = em pé atrás do timão, quando não há assentos). */
    private void sentar(Player jogador, int lugar) {
        boolean abordo = getPassengers().contains(jogador);
        for (Entity p : getPassengers()) {
            if (p != jogador && lugarDe(p) == lugar) {
                Mensagens.barra(jogador, lugar < 0 ? "Já tem alguém ao timão."
                        : (lugar == lugarDoPiloto() ? "O assento do piloto está ocupado." : "Assento ocupado."));
                return;
            }
        }
        if (!abordo && getPassengers().size() >= capacidade()) {
            Mensagens.barra(jogador, "O navio está lotado.");
            return;
        }
        lugares.put(jogador.getId(), lugar);
        publicarLugares();
        if (!abordo && !jogador.startRiding(this)) {
            lugares.remove(jogador.getId());
            publicarLugares();
            return;
        }
        ctrlAnterior = true; // se o Ctrl já estava apertado, não conta como "lançar a âncora"
        if (lugar == lugarDoPiloto()) {
            Mensagens.chat(jogador, "Ao leme! Ctrl solta/lança a âncora (com ela solta o navio anda sozinho), "
                    + "A/D vira, S freia e dá ré, Shift levanta.");
            if (ancora()) Mensagens.barra(jogador, "Âncora lançada. Aperte Ctrl para soltá-la e partir.");
        } else {
            Mensagens.barra(jogador, "Você está sentado como passageiro. Shift levanta.");
        }
    }

    private void alternarAncora(ServerPlayer jogador) {
        boolean nova = !ancora();
        getEntityData().set(DADOS_ANCORA, nova);
        if (nova) {
            Mensagens.barra(jogador, "Âncora lançada! O navio vai parando.");
        } else if (potencia() > 0.0) {
            Mensagens.barra(jogador, "Âncora solta! O navio segue sozinho. A/D vira, S freia.");
        } else {
            Mensagens.barra(jogador, "Âncora solta, mas sem força: abasteça o Motor ou a Fornalha, ou erga Velas.");
        }
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.NEUTRAL, 0.6f, nova ? 0.8f : 1.4f);
    }

    // ------------------------------------------------------------------ achar o navio com o olhar

    /** O navio que o jogador está mirando (qualquer bloco dele), a no máximo "alcance" blocos. */
    public static NavioEntidade acharNoRaio(Level nivel, Player jogador, double alcance) {
        Vec3 olho = jogador.getEyePosition();
        Vec3 olhar = jogador.getLookAngle();
        AABB area = new AABB(olho, olho).inflate(alcance + 1.0);
        NavioEntidade melhor = null;
        double menor = alcance;
        for (NavioEntidade navio : nivel.getEntitiesOfClass(NavioEntidade.class, area)) {
            double d = navio.distanciaDoRaio(olho, olhar, menor);
            if (d >= 0.0 && d <= menor) {
                menor = d;
                melhor = navio;
            }
        }
        return melhor;
    }

    private double distanciaDoRaio(Vec3 olho, Vec3 dir, double maximo) {
        double[] o = vetorParaLocal(olho.x - getX(), olho.z - getZ());
        double[] d = vetorParaLocal(dir.x, dir.z);
        double[] origem = {o[0], olho.y - getY(), o[1]};
        double[] direcao = {d[0], dir.y, d[1]};
        double melhor = -1.0;
        for (Estrutura.Bloco b : estrutura.blocos()) {
            double t = tocaCelula(origem, direcao, b.dx - 0.5, b.dy, b.dz - 0.5, b.dx + 0.5, b.dy + 1.0, b.dz + 0.5, maximo);
            if (t >= 0.0 && (melhor < 0.0 || t < melhor)) melhor = t;
        }
        return melhor;
    }

    /** Raio contra uma caixa (método das lajes). Devolve a distância de entrada, ou -1 se não bate. */
    private static double tocaCelula(double[] o, double[] d, double x0, double y0, double z0,
                                     double x1, double y1, double z1, double maximo) {
        double[] lo = {x0, y0, z0};
        double[] hi = {x1, y1, z1};
        double entra = 0.0, sai = maximo;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0E-9) {
                if (o[i] < lo[i] || o[i] > hi[i]) return -1.0;
            } else {
                double a = (lo[i] - o[i]) / d[i];
                double b = (hi[i] - o[i]) / d[i];
                if (a > b) {
                    double t = a;
                    a = b;
                    b = t;
                }
                entra = Math.max(entra, a);
                sai = Math.min(sai, b);
                if (entra > sai) return -1.0;
            }
        }
        return entra;
    }

    private static String tempo(int ticks) {
        int s = ticks / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    // ------------------------------------------------------------------ convés: levar e colidir

    private void levarEntidades(ServerLevel nivel) {
        AABB area = getBoundingBox().inflate(1.0, 3.0, 1.0);
        List<Entity> perto = nivel.getEntities(this, area,
                e -> (e instanceof LivingEntity && !(e instanceof Player)) || e instanceof ItemEntity);
        for (Entity e : perto) tratarEntidade(e);
    }

    /**
     * Mantém uma entidade "presa" ao navio e a impede de atravessar os blocos dele.
     * No servidor vale para mobs e itens; no cliente, para o jogador local (jogadores mandam
     * a própria posição, então cada um calcula a sua no seu computador).
     */
    public void tratarEntidade(Entity e) {
        if (e.isRemoved() || e.isSpectator() || e.isPassenger() || e == this || estrutura.blocos().isEmpty()) return;
        int id = e.getId();
        Carga c = cargas.get(id);
        Vec3 pos = e.position();
        double[] p = new double[3];

        if (c != null) {
            double dx = pos.x - c.mundoX, dy = pos.y - c.mundoY, dz = pos.z - c.mundoZ;
            if (dx * dx + dy * dy + dz * dz > 36.0) {
                cargas.remove(id);
                c = null;
            } else {
                double[] d = vetorParaLocal(dx, dz);
                p[0] = c.lx + d[0];
                p[1] = c.ly + dy;
                p[2] = c.lz + d[1];
                float dRumo = Mth.wrapDegrees(rumo - c.rumo);
                if (dRumo != 0.0f && e instanceof Player) e.setYRot(e.getYRot() - dRumo);
            }
        }
        if (c == null) {
            double[] d = vetorParaLocal(pos.x - getX(), pos.z - getZ());
            p[0] = d[0];
            p[1] = pos.y - getY();
            p[2] = d[1];
            if (!dentro(p)) return;
        }

        int resultado = resolver(e, p);
        boolean apoiado = (resultado & 1) != 0;
        boolean empurrado = (resultado & 2) != 0;

        if (apoiado && c == null) {
            c = new Carga();
            cargas.put(id, c);
        }
        if (apoiado) {
            c.semApoio = 0;
        } else if (c != null) {
            c.semApoio++;
            if (c.semApoio > 12 || !dentro(p)) {
                cargas.remove(id);
                c = null;
            }
        }

        if (c != null || apoiado || empurrado) {
            Vec3 w = paraMundo(p[0], p[1], p[2]);
            e.setPos(w.x, w.y, w.z);
        }
        if (apoiado) {
            e.setOnGround(true);
            e.resetFallDistance();
            Vec3 dm = e.getDeltaMovement();
            if (dm.y < 0.0) e.setDeltaMovement(dm.x, 0.0, dm.z);
        }
        if (c != null) {
            c.lx = p[0];
            c.ly = p[1];
            c.lz = p[2];
            Vec3 agora = e.position();
            c.mundoX = agora.x;
            c.mundoY = agora.y;
            c.mundoZ = agora.z;
            c.rumo = rumo;
            c.visto = tickCount;
        }
    }

    private boolean dentro(double[] p) {
        double limite = estrutura.raio + 1.0;
        return Math.abs(p[0]) <= limite && Math.abs(p[2]) <= limite && p[1] >= -1.0 && p[1] <= estrutura.altura + 4.0;
    }

    /**
     * Resolve colisões da entidade (um cilindro) com os blocos do navio, em coordenadas locais.
     * Devolve bits: 1 = pisando no convés, 2 = foi empurrada para fora de uma parede.
     */
    private int resolver(Entity e, double[] p) {
        double raio = e.getBbWidth() / 2.0;
        double altura = e.getBbHeight();
        double vy = e.getDeltaMovement().y;
        double lx = p[0], ly = p[1], lz = p[2];
        boolean apoiado = false;
        boolean empurrado = false;

        for (int passada = 0; passada < 2; passada++) {
            double piso = Double.NEGATIVE_INFINITY;
            int x0 = Mth.floor(lx - raio + 0.5), x1 = Mth.floor(lx + raio + 0.5);
            int z0 = Mth.floor(lz - raio + 0.5), z1 = Mth.floor(lz + raio + 0.5);
            int y0 = Mth.floor(ly - 1.0), y1 = Mth.floor(ly + altura);
            for (int ix = x0; ix <= x1; ix++) {
                for (int iz = z0; iz <= z1; iz++) {
                    for (int iy = y0; iy <= y1; iy++) {
                        Estrutura.Bloco b = estrutura.em(ix, iy, iz);
                        if (b == null) continue;
                        for (AABB caixa : b.caixas()) {
                            double cx = Mth.clamp(lx, caixa.minX, caixa.maxX);
                            double cz = Mth.clamp(lz, caixa.minZ, caixa.maxZ);
                            double ddx = lx - cx, ddz = lz - cz;
                            double d2 = ddx * ddx + ddz * ddz;
                            if (d2 >= raio * raio) continue;

                            boolean acima = ly >= caixa.maxY - 1.0E-4;
                            if (acima) {
                                if (ly <= caixa.maxY + 0.25 && vy <= 0.0) piso = Math.max(piso, caixa.maxY);
                                continue;
                            }
                            if (ly + altura <= caixa.minY + 1.0E-4) continue;
                            if (caixa.maxY - ly <= DEGRAU) {
                                piso = Math.max(piso, caixa.maxY);
                                continue;
                            }
                            // parede: empurra para fora
                            if (d2 > 1.0E-9) {
                                double dist = Math.sqrt(d2);
                                double empurra = raio - dist;
                                lx += ddx / dist * empurra;
                                lz += ddz / dist * empurra;
                            } else {
                                double esquerda = lx - caixa.minX, direita = caixa.maxX - lx;
                                double tras = lz - caixa.minZ, frente = caixa.maxZ - lz;
                                double menor = Math.min(Math.min(esquerda, direita), Math.min(tras, frente));
                                if (menor == esquerda) lx = caixa.minX - raio;
                                else if (menor == direita) lx = caixa.maxX + raio;
                                else if (menor == tras) lz = caixa.minZ - raio;
                                else lz = caixa.maxZ + raio;
                            }
                            empurrado = true;
                        }
                    }
                }
            }
            if (piso > Double.NEGATIVE_INFINITY && (ly < piso || (ly <= piso + 0.25 && vy <= 0.0))) {
                ly = piso;
                apoiado = true;
            }
        }
        p[0] = lx;
        p[1] = ly;
        p[2] = lz;
        return (apoiado ? 1 : 0) | (empurrado ? 2 : 0);
    }

    // ------------------------------------------------------------------ desmontar

    /** Devolve os blocos ao mundo (alinhados à grade) e remove o navio. */
    public void desmontar(Player jogador) {
        if (!(level() instanceof ServerLevel nivel) || isRemoved()) return;
        int k = Math.floorMod(Math.round(rumo / 90.0f), 4);
        Rotation rotacao = switch (k) {
            case 1 -> Rotation.COUNTERCLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
        int bx = Mth.floor(getX());
        int bz = Mth.floor(getZ());
        int by = (int) Math.round(getY());

        ejectPassengers();

        List<BlockPos> postos = new ArrayList<>();
        int perdidos = 0;
        for (Estrutura.Bloco b : estrutura.blocos()) {
            int rx, rz;
            switch (k) {
                case 1 -> { rx = b.dz; rz = -b.dx; }
                case 2 -> { rx = -b.dx; rz = -b.dz; }
                case 3 -> { rx = -b.dz; rz = b.dx; }
                default -> { rx = b.dx; rz = b.dz; }
            }
            BlockPos pos = new BlockPos(bx + rx, by + b.dy, bz + rz);
            BlockState estado = b.estado.rotate(rotacao);
            if (estado.hasProperty(FornalhaBlock.LIT)) estado = estado.setValue(FornalhaBlock.LIT, false);
            BlockState atual = nivel.getBlockState(pos);
            if (atual.isAir() || atual.canBeReplaced()) {
                nivel.setBlock(pos, estado, 3);
                postos.add(pos);
            } else {
                Block.popResource(nivel, pos, new ItemStack(estado.getBlock()));
                perdidos++;
            }
        }
        Cola.de(nivel).adicionar(postos);

        for (PecaInterativa p : pecas) p.discard();
        pecas.clear();
        blocosDasPecas.clear();
        discard();
        if (jogador != null) {
            Mensagens.chat(jogador, "Navio desmontado: edite à vontade. Para montar de novo, cole o que for novo com a "
                    + "Cola Naval e clique no Timão." + (perdidos > 0 ? " " + perdidos + " blocos viraram itens no chão." : ""));
        }
    }

    @Override
    public void kill(ServerLevel nivel) {
        desmontar(null);
    }

    // ------------------------------------------------------------------ configuração da entidade

    @Override
    public boolean hurtServer(ServerLevel nivel, DamageSource fonte, float dano) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity outra) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    // ------------------------------------------------------------------ disco

    @Override
    protected void addAdditionalSaveData(ValueOutput saida) {
        estrutura.salvar(saida.child("estrutura"));
        saida.putFloat("rumo", rumo);
        saida.putDouble("calado", calado);
        saida.putBoolean("ancora", ancora());
        saida.putIntArray("combustivel", combustivel);
        saida.putFloat("velocidade", velocidade);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput entrada) {
        estrutura = Estrutura.carregar(entrada.childOrEmpty("estrutura"));
        rumo = entrada.getFloatOr("rumo", 0.0f);
        rumoAnterior = rumo;
        calado = entrada.getDoubleOr("calado", 0.6);
        velocidade = entrada.getFloatOr("velocidade", 0.0f);
        int[] lido = entrada.getIntArray("combustivel").orElse(new int[0]);
        combustivel = new int[estrutura.fornalhas];
        System.arraycopy(lido, 0, combustivel, 0, Math.min(lido.length, combustivel.length));
        alvosPecas = null;
        prepararMotores();
        calcularAmostras();
        setYRot(rumo);
        getEntityData().set(DADOS_ANCORA, entrada.getBooleanOr("ancora", true));
        getEntityData().set(DADOS_RUMO, rumo);
        getEntityData().set(DADOS_ESTRUTURA, estrutura.codificar());
        atualizarCaixa();
    }
}
