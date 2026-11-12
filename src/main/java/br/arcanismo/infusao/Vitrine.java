package br.arcanismo.infusao;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Mostra o item guardado num altar ou pedestal como um item flutuando e girando
 * acima do bloco. É só decoração: não pode ser pego nem se junta a outros itens.
 */
public final class Vitrine {
    public static final String TAG = "arcanismo_vitrine";

    private Vitrine() {}

    private static AABB area(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY() + 0.5, pos.getZ(), pos.getX() + 1, pos.getY() + 2.2, pos.getZ() + 1);
    }

    public static void atualizar(ServerLevel nivel, BlockPos pos, ItemStack item, double altura) {
        List<ItemEntity> existentes = nivel.getEntitiesOfClass(ItemEntity.class, area(pos),
                e -> e.getPersistentData().getBooleanOr(TAG, false));
        ItemEntity manter = null;
        for (ItemEntity e : existentes) {
            if (manter == null && !item.isEmpty() && ItemStack.isSameItemSameComponents(e.getItem(), item)) {
                manter = e;
            } else {
                e.discard();
            }
        }
        if (item.isEmpty()) return;

        double x = pos.getX() + 0.5, y = pos.getY() + altura, z = pos.getZ() + 0.5;
        if (manter == null) {
            manter = new ItemEntity(nivel, x, y, z, item.copyWithCount(1));
            manter.setNeverPickUp();
            manter.setUnlimitedLifetime();
            manter.setNoGravity(true);
            manter.setInvulnerable(true);
            manter.getPersistentData().putBoolean(TAG, true);
            manter.setDeltaMovement(Vec3.ZERO);
            nivel.addFreshEntity(manter);
        } else {
            manter.setPos(x, y, z);
            manter.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static void limpar(ServerLevel nivel, BlockPos pos) {
        atualizar(nivel, pos, ItemStack.EMPTY, 0);
    }
}
