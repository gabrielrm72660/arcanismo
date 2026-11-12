package br.arcanismo.infusao;

import br.arcanismo.registro.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PedestalBlockEntity extends SuporteBlockEntity {
    public PedestalBlockEntity(BlockPos pos, BlockState estado) {
        super(ModBlockEntities.PEDESTAL.get(), pos, estado);
    }

    @Override
    protected double alturaVitrine() {
        return 1.0;
    }

    public static void tick(Level nivel, BlockPos pos, BlockState estado, PedestalBlockEntity be) {
        be.tickVitrine();
    }
}
