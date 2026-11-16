package br.navegantes.registro;

import br.navegantes.Navegantes;
import br.navegantes.navio.NavioEntidade;
import br.navegantes.navio.PecaInterativa;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntidades {
    public static final DeferredRegister<EntityType<?>> ENTIDADES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Navegantes.MODID);

    public static final RegistryObject<EntityType<NavioEntidade>> NAVIO = ENTIDADES.register("navio",
            () -> EntityType.Builder.<NavioEntidade>of(NavioEntidade::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Navegantes.id("navio"))));

    /** Peça pequena e clicável que acompanha o navio (timão, fornalhas). Não é salva: o navio a recria. */
    public static final RegistryObject<EntityType<PecaInterativa>> PECA = ENTIDADES.register("peca_navio",
            () -> EntityType.Builder.<PecaInterativa>of(PecaInterativa::new, MobCategory.MISC)
                    .sized(0.9f, 0.9f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Navegantes.id("peca_navio"))));

    private ModEntidades() {}
}
