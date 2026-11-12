package br.arcanismo.infusao;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Base comum do Altar e do Pedestal: guarda um item e o exibe flutuando. */
public abstract class SuporteBlockEntity extends BlockEntity {
    protected ItemStack item = ItemStack.EMPTY;

    protected SuporteBlockEntity(BlockEntityType<?> tipo, BlockPos pos, BlockState estado) {
        super(tipo, pos, estado);
    }

    protected abstract double alturaVitrine();

    public ItemStack item() {
        return item;
    }

    public void definirItem(ItemStack novo) {
        this.item = novo;
        setChanged();
        if (level instanceof ServerLevel s) Vitrine.atualizar(s, worldPosition, item, alturaVitrine());
    }

    protected void tickVitrine() {
        if (level instanceof ServerLevel s && s.getGameTime() % 20 == 0) {
            Vitrine.atualizar(s, worldPosition, item, alturaVitrine());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput saida) {
        super.saveAdditional(saida);
        if (!item.isEmpty()) saida.store("item", ItemStack.CODEC, item);
    }

    @Override
    protected void loadAdditional(ValueInput entrada) {
        super.loadAdditional(entrada);
        item = entrada.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
}
