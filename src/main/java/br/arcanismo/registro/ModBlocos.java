package br.arcanismo.registro;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import br.arcanismo.Arcanismo;
import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.bloco.AglomeradoBlock;
import br.arcanismo.bloco.CaldeiraoBlock;
import br.arcanismo.bloco.TerraCorrompidaBlock;
import br.arcanismo.infusao.AltarBlock;
import br.arcanismo.infusao.PedestalBlock;
import br.arcanismo.mesa.MesaArcanaBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocos {
    public static final DeferredRegister<Block> BLOCOS = DeferredRegister.create(Registries.BLOCK, Arcanismo.MODID);

    public static final RegistryObject<Block> MINERIO_ARCANO = registrar("minerio_arcano", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(s -> 4));

    public static final RegistryObject<Block> MINERIO_ARCANO_ARDOSIA = registrar("minerio_arcano_ardosia", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).lightLevel(s -> 4));

    public static final RegistryObject<Block> CALDEIRAO_ARCANO = registrar("caldeirao_arcano", CaldeiraoBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).noOcclusion());

    public static final RegistryObject<Block> TERRA_CORROMPIDA = registrar("terra_corrompida", TerraCorrompidaBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6f)
                    .sound(SoundType.GRAVEL).randomTicks());

    public static final RegistryObject<Block> TIJOLO_ARCANO = registrar("tijolo_arcano", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_PURPLE));

    public static final RegistryObject<Block> MESA_ARCANA = registrar("mesa_arcana", MesaArcanaBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).mapColor(MapColor.COLOR_PURPLE).lightLevel(s -> 3));

    public static final RegistryObject<Block> ALTAR_INFUSAO = registrar("altar_infusao", AltarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_PURPLE)
                    .noOcclusion().lightLevel(s -> 7));

    public static final RegistryObject<Block> PEDESTAL_ARCANO = registrar("pedestal_arcano", PedestalBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_PURPLE)
                    .noOcclusion().lightLevel(s -> 3));

    /** Aglomerados de cristal, um por aspecto primordial. */
    private static final Map<Aspecto, RegistryObject<Block>> AGLOMERADOS = new EnumMap<>(Aspecto.class);

    static {
        for (Aspecto a : Aspecto.values()) {
            if (!a.primordial()) continue;
            AGLOMERADOS.put(a, registrar("aglomerado_" + a.id, p -> new AglomeradoBlock(a, p),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noOcclusion()
                            .sound(SoundType.AMETHYST_CLUSTER).strength(1.5f).lightLevel(s -> 6).randomTicks()
                            .pushReaction(PushReaction.DESTROY)));
        }
    }

    public static RegistryObject<Block> aglomerado(Aspecto a) {
        return AGLOMERADOS.get(a);
    }

    private ModBlocos() {}

    private static RegistryObject<Block> registrar(String nome, Function<BlockBehaviour.Properties, Block> fabrica,
                                                   Supplier<BlockBehaviour.Properties> propriedades) {
        RegistryObject<Block> bloco = BLOCOS.register(nome, () -> fabrica.apply(
                propriedades.get().setId(ResourceKey.create(Registries.BLOCK, Arcanismo.id(nome)))));
        ModItens.ITENS.register(nome, () -> new BlockItem(bloco.get(),
                ModItens.propriedades(nome).useBlockDescriptionPrefix()));
        return bloco;
    }
}
