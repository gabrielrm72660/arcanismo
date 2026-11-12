package br.arcanismo.registro;

import br.arcanismo.Arcanismo;
import br.arcanismo.grimorio.GrimorioMenu;
import br.arcanismo.mesa.MesaArcanaMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Arcanismo.MODID);

    public static final RegistryObject<MenuType<MesaArcanaMenu>> MESA_ARCANA =
            MENUS.register("mesa_arcana", () -> new MenuType<>(MesaArcanaMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<GrimorioMenu>> GRIMORIO =
            MENUS.register("grimorio", () -> new MenuType<>(GrimorioMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
