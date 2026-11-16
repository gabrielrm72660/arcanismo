package br.navegantes.registro;

import java.util.function.Function;
import java.util.function.Supplier;

import br.navegantes.Navegantes;
import br.navegantes.bloco.FornalhaBlock;
import br.navegantes.bloco.TimaoBlock;
import br.navegantes.bloco.VelaBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocos {
    public static final DeferredRegister<Block> BLOCOS = DeferredRegister.create(Registries.BLOCK, Navegantes.MODID);

    public static final RegistryObject<Block> TIMAO = registrar("timao", TimaoBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f)
                    .sound(SoundType.WOOD).noOcclusion());

    public static final RegistryObject<Block> FORNALHA_VAPOR = registrar("fornalha_vapor", FornalhaBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 13 : 0));

    public static final RegistryObject<Block> VELA = registrar("vela", VelaBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8f)
                    .sound(SoundType.WOOL).noOcclusion());

    private ModBlocos() {}

    private static RegistryObject<Block> registrar(String nome, Function<BlockBehaviour.Properties, Block> fabrica,
                                                   Supplier<BlockBehaviour.Properties> propriedades) {
        RegistryObject<Block> bloco = BLOCOS.register(nome, () -> fabrica.apply(
                propriedades.get().setId(ResourceKey.create(Registries.BLOCK, Navegantes.id(nome)))));
        ModItens.ITENS.register(nome, () -> new BlockItem(bloco.get(),
                ModItens.propriedades(nome).useBlockDescriptionPrefix()));
        return bloco;
    }
}
