package br.arcanismo.entidade;

import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.item.CajadoItem;
import br.arcanismo.item.LenteArcanaItem;
import br.arcanismo.registro.ModItens;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Orbe de Luz: essência pura que vaga pelo mundo, feita só de partículas.
 *  - Flutua sem rumo e se aproxima, curiosa, de quem segura a Lente Arcana.
 *  - Clique nela com o Cajado para absorver Vis (+25), ou com a mão para
 *    condensá-la num Cristal do seu aspecto.
 *  - Some sozinha depois de alguns minutos.
 */
public class OrbeDeLuz extends Entity {
    private static final EntityDataAccessor<Integer> ASPECTO =
            SynchedEntityData.defineId(OrbeDeLuz.class, EntityDataSerializers.INT);
    private static final int VIDA_MAXIMA = 20 * 60 * 5;

    private Vec3 rumo = Vec3.ZERO;
    private int idade = 0;

    public OrbeDeLuz(EntityType<? extends OrbeDeLuz> tipo, Level nivel) {
        super(tipo, nivel);
        this.setNoGravity(true);
    }

    public static final Aspecto[] PRIMORDIAIS = {Aspecto.AR, Aspecto.TERRA, Aspecto.FOGO, Aspecto.AGUA, Aspecto.ORDEM, Aspecto.ENTROPIA};
    private static final int[] CORES = {0xFAE45F, 0x4EB03E, 0xF05C28, 0x3EA8F0, 0xF4F6FF, 0x9A5CD0};

    public Aspecto aspecto() {
        int i = this.entityData.get(ASPECTO);
        return PRIMORDIAIS[Math.floorMod(i, PRIMORDIAIS.length)];
    }

    public void definirAspecto(Aspecto a) {
        for (int i = 0; i < PRIMORDIAIS.length; i++) {
            if (PRIMORDIAIS[i] == a) this.entityData.set(ASPECTO, i);
        }
    }

    private int cor() {
        return CORES[Math.floorMod(this.entityData.get(ASPECTO), CORES.length)];
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        construtor.define(ASPECTO, 0);
    }

    @Override
    public void tick() {
        super.tick();
        RandomSource r = this.random;
        Level nivel = this.level();

        if (nivel.isClientSide()) {
            // O "corpo" da orbe: um núcleo branco e um halo colorido que deixa rastro
            double x = getX(), y = getY() + 0.2, z = getZ();
            nivel.addParticle(new DustParticleOptions(0xFFFFFF, 0.9f), x, y, z, 0, 0, 0);
            for (int i = 0; i < 2; i++) {
                nivel.addParticle(new DustParticleOptions(cor(), 1.3f),
                        x + (r.nextDouble() - 0.5) * 0.35, y + (r.nextDouble() - 0.5) * 0.35,
                        z + (r.nextDouble() - 0.5) * 0.35, 0, 0, 0);
            }
            if (this.tickCount % 5 == 0) {
                nivel.addParticle(ParticleTypes.END_ROD, x, y, z,
                        (r.nextDouble() - 0.5) * 0.04, (r.nextDouble() - 0.5) * 0.04, (r.nextDouble() - 0.5) * 0.04);
            }
            return;
        }

        if (++idade > VIDA_MAXIMA) {
            discard();
            return;
        }

        // Novo rumo de tempos em tempos (ou em direção a quem segura a Lente)
        if (this.tickCount % 40 == 0 || rumo == Vec3.ZERO) {
            Player curioso = nivel.getNearestPlayer(this, 10.0);
            if (curioso != null && (curioso.getMainHandItem().getItem() instanceof LenteArcanaItem
                    || curioso.getOffhandItem().getItem() instanceof LenteArcanaItem)
                    && curioso.distanceToSqr(this) > 6.0) {
                rumo = curioso.getEyePosition().subtract(position()).normalize().scale(0.05);
            } else {
                rumo = new Vec3(r.nextDouble() - 0.5, (r.nextDouble() - 0.5) * 0.6, r.nextDouble() - 0.5)
                        .normalize().scale(0.035);
            }
        }
        double flutuar = Math.sin(this.tickCount * 0.12) * 0.012;
        setDeltaMovement(rumo.add(0, flutuar, 0));
        Vec3 antes = position();
        move(MoverType.SELF, getDeltaMovement());
        if (position().distanceToSqr(antes) < 1.0E-5) rumo = Vec3.ZERO; // bateu: muda de rumo
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player jogador, InteractionHand mao, Vec3 local) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel nivel = (ServerLevel) level();
        ItemStack naMao = jogador.getItemInHand(mao);

        if (naMao.getItem() instanceof CajadoItem cajado) {
            cajado.adicionarVis(naMao, 25);
            jogador.sendSystemMessage(Component.literal("O cajado absorve a orbe: +25 Vis ("
                    + CajadoItem.vis(naMao) + "/" + cajado.capacidade() + ")").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            ItemStack cristal = new ItemStack(ModItens.cristal(aspecto()).get());
            if (!jogador.getInventory().add(cristal)) jogador.drop(cristal, false);
            br.arcanismo.conhecimento.Conhecimento.adicionarPontos(jogador, aspecto(), 3);
            jogador.sendSystemMessage(Component.literal("A luz se condensa num cristal de ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE).append(aspecto().texto())
                    .append(Component.literal(" (+3 conhecimento)").withStyle(ChatFormatting.GRAY)));
        }
        estourar(nivel);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel nivel, DamageSource fonte, float dano) {
        estourar(nivel);
        return true;
    }

    private void estourar(ServerLevel nivel) {
        nivel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 25, 0.2, 0.2, 0.2, 0.08);
        nivel.sendParticles(new DustParticleOptions(cor(), 1.5f), getX(), getY() + 0.2, getZ(), 20, 0.3, 0.3, 0.3, 0.0);
        nivel.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.8f);
        discard();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput entrada) {
        this.entityData.set(ASPECTO, entrada.getIntOr("aspecto", 0));
        this.idade = entrada.getIntOr("idade", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput saida) {
        saida.putInt("aspecto", this.entityData.get(ASPECTO));
        saida.putInt("idade", this.idade);
    }
}
