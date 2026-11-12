package br.arcanismo.bloco;

import br.arcanismo.aspecto.Aspecto;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import br.arcanismo.entidade.OrbeDeLuz;
import br.arcanismo.registro.ModEntidades;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Aglomerado de Cristal: lascas de essência que crescem incrustadas nas rochas das cavernas
 * (chão, teto e paredes). Ao ser minerado, solta Cristais do seu aspecto.
 */
public class AglomeradoBlock extends AmethystClusterBlock {
    private final Aspecto aspecto;

    public AglomeradoBlock(Aspecto aspecto, Properties propriedades) {
        super(7.0f, 3.0f, propriedades);
        this.aspecto = aspecto;
    }

    public Aspecto aspecto() {
        return aspecto;
    }

    /** De vez em quando o aglomerado liberta um Orbe de Luz do seu aspecto. */
    @Override
    protected void randomTick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource aleatorio) {
        if (aleatorio.nextInt(30) != 0) return;
        AABB area = new AABB(pos).inflate(16);
        if (!nivel.getEntitiesOfClass(OrbeDeLuz.class, area).isEmpty()) return;
        OrbeDeLuz orbe = new OrbeDeLuz(ModEntidades.ORBE_DE_LUZ.get(), nivel);
        orbe.definirAspecto(aspecto);
        orbe.setPos(pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);
        nivel.addFreshEntity(orbe);
    }

    @Override
    public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource aleatorio) {
        if (aleatorio.nextInt(10) == 0) {
            nivel.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.3 + aleatorio.nextDouble() * 0.4,
                    pos.getY() + 0.3 + aleatorio.nextDouble() * 0.4,
                    pos.getZ() + 0.3 + aleatorio.nextDouble() * 0.4,
                    0.0, 0.01, 0.0);
        }
    }
}
