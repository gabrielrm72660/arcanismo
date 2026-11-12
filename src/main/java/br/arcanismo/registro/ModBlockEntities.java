package br.arcanismo.registro;

import java.util.Set;

import br.arcanismo.Arcanismo;
import br.arcanismo.bloco.CaldeiraoBlockEntity;
import br.arcanismo.infusao.AltarBlockEntity;
import br.arcanismo.infusao.PedestalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TIPOS =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Arcanismo.MODID);

    public static final RegistryObject<BlockEntityType<CaldeiraoBlockEntity>> CALDEIRAO =
            TIPOS.register("caldeirao_arcano", () -> new BlockEntityType<>(
                    CaldeiraoBlockEntity::new, Set.of(ModBlocos.CALDEIRAO_ARCANO.get())));

    public static final RegistryObject<BlockEntityType<PedestalBlockEntity>> PEDESTAL =
            TIPOS.register("pedestal_arcano", () -> new BlockEntityType<>(
                    PedestalBlockEntity::new, Set.of(ModBlocos.PEDESTAL_ARCANO.get())));

    public static final RegistryObject<BlockEntityType<AltarBlockEntity>> ALTAR =
            TIPOS.register("altar_infusao", () -> new BlockEntityType<>(
                    AltarBlockEntity::new, Set.of(ModBlocos.ALTAR_INFUSAO.get())));

    private ModBlockEntities() {}
}
