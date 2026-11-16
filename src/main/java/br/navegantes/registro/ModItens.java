package br.navegantes.registro;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import br.navegantes.Navegantes;
import br.navegantes.item.ColaItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModItens {
    public static final DeferredRegister<Item> ITENS = DeferredRegister.create(Registries.ITEM, Navegantes.MODID);

    public static final RegistryObject<Item> COLA_NAVAL =
            registrar("cola_naval", ColaItem::new, p -> p.stacksTo(16));

    private ModItens() {}

    /** Cria as propriedades com o ID já definido (obrigatório desde a 1.21.2). */
    public static Item.Properties propriedades(String nome) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Navegantes.id(nome)));
    }

    private static RegistryObject<Item> registrar(String nome, Function<Item.Properties, Item> fabrica,
                                                  UnaryOperator<Item.Properties> ajustes) {
        return ITENS.register(nome, () -> fabrica.apply(ajustes.apply(propriedades(nome))));
    }
}
