package br.arcanismo.registro;

import br.arcanismo.Arcanismo;
import br.arcanismo.entidade.OrbeDeLuz;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntidades {
    public static final DeferredRegister<EntityType<?>> ENTIDADES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Arcanismo.MODID);

    public static final RegistryObject<EntityType<OrbeDeLuz>> ORBE_DE_LUZ = ENTIDADES.register("orbe_de_luz",
            () -> EntityType.Builder.<OrbeDeLuz>of(OrbeDeLuz::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .fireImmune()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Arcanismo.id("orbe_de_luz"))));

    private ModEntidades() {}
}
