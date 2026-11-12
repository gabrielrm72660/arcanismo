package br.arcanismo.registro;

import br.arcanismo.Arcanismo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModAbas {
    public static final DeferredRegister<CreativeModeTab> ABAS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Arcanismo.MODID);

    public static final RegistryObject<CreativeModeTab> ARCANISMO = ABAS.register("arcanismo",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.arcanismo"))
                    .icon(() -> new ItemStack(ModItens.GRIMORIO.get()))
                    .displayItems((parametros, saida) -> {
                        for (RegistryObject<Item> item : ModItens.ITENS.getEntries()) {
                            saida.accept(item.get());
                        }
                    })
                    .build());

    private ModAbas() {}
}
