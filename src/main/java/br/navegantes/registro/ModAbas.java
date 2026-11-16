package br.navegantes.registro;

import br.navegantes.Navegantes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModAbas {
    public static final DeferredRegister<CreativeModeTab> ABAS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Navegantes.MODID);

    public static final RegistryObject<CreativeModeTab> NAVEGANTES = ABAS.register("navegantes",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.navegantes"))
                    .icon(() -> new ItemStack(ModBlocos.TIMAO.get()))
                    .displayItems((parametros, saida) -> {
                        for (RegistryObject<Item> item : ModItens.ITENS.getEntries()) {
                            saida.accept(item.get());
                        }
                    })
                    .build());

    private ModAbas() {}
}
