package br.navegantes.navio;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Peça pequena e clicável que acompanha o navio (o Timão, cada Assento e cada Motor/Fornalha). O navio inteiro não
 * é "clicável": senão a caixa gigante dele roubaria todos os cliques. Não é salva; o navio recria.
 */
public class PecaInterativa extends Entity {
    public static final int TIMAO = 0;
    public static final int FORNALHA = 1;
    public static final int ASSENTO = 2;

    private NavioEntidade navio;
    private int tipo;
    private int indice;

    public PecaInterativa(EntityType<?> tipo, Level nivel) {
        super(tipo, nivel);
    }

    public void configurar(NavioEntidade navio, int tipo, int indice) {
        this.navio = navio;
        this.tipo = tipo;
        this.indice = indice;
    }

    public int tipo() {
        return tipo;
    }

    public int indice() {
        return indice;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (navio == null || navio.isRemoved())) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput entrada) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput saida) {
    }

    @Override
    public boolean hurtServer(ServerLevel nivel, DamageSource fonte, float dano) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
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
    public InteractionResult interact(Player jogador, InteractionHand mao, Vec3 ponto) {
        if (!level().isClientSide() && navio != null && !navio.isRemoved()) {
            navio.aoClicarPeca(this, jogador, mao);
        }
        return InteractionResult.SUCCESS;
    }
}
