package br.arcanismo.registro;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import br.arcanismo.Arcanismo;
import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.item.CajadoItem;
import br.arcanismo.item.CristalItem;
import br.arcanismo.item.GrimorioItem;
import br.arcanismo.item.LenteArcanaItem;
import br.arcanismo.item.SalPurificadorItem;
import br.arcanismo.item.PoVitalItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModItens {
    public static final DeferredRegister<Item> ITENS = DeferredRegister.create(Registries.ITEM, Arcanismo.MODID);

    private static final Map<Aspecto, RegistryObject<Item>> CRISTAIS = new EnumMap<>(Aspecto.class);

    static {
        for (Aspecto a : Aspecto.values()) {
            if (a.primordial()) {
                CRISTAIS.put(a, registrar("cristal_" + a.id, p -> new CristalItem(a, p), p -> p));
            }
        }
    }

    public static final RegistryObject<Item> PO_DE_VIS =
            registrar("po_de_vis", Item::new, p -> p);
    public static final RegistryObject<Item> LINGOTE_ARCANITA =
            registrar("lingote_arcanita", Item::new, p -> p.rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> SAL_PURIFICADOR =
            registrar("sal_purificador", SalPurificadorItem::new, p -> p);
    public static final RegistryObject<Item> LENTE_ARCANA =
            registrar("lente_arcana", LenteArcanaItem::new, p -> p.stacksTo(1));
    public static final RegistryObject<Item> GRIMORIO =
            registrar("grimorio", GrimorioItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> CAJADO_APRENDIZ =
            registrar("cajado_aprendiz", p -> new CajadoItem(100, p), p -> p.stacksTo(1));
    public static final RegistryObject<Item> CAJADO_ARCANITA =
            registrar("cajado_arcanita", p -> new CajadoItem(300, p), p -> p.stacksTo(1).rarity(Rarity.RARE));

    /** Material das ferramentas de Arcanita: entre o diamante e a netherite. */
    public static final ToolMaterial ARCANITA = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1800, 10.0f, 3.5f, 18, TagKey.create(Registries.ITEM, Arcanismo.id("reparo_arcanita")));

    public static final RegistryObject<Item> PICARETA_ARCANITA =
            registrar("picareta_arcanita", Item::new, p -> p.pickaxe(ARCANITA, 1.0f, -2.8f).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> ESPADA_ARCANITA =
            registrar("espada_arcanita", Item::new, p -> p.sword(ARCANITA, 3.0f, -2.4f).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> LAMINA_VAZIO =
            registrar("lamina_vazio", Item::new, p -> p.sword(ToolMaterial.NETHERITE, 6.0f, -2.2f)
                    .fireResistant().rarity(Rarity.EPIC));
    public static final RegistryObject<Item> CAJADO_CRISTAL =
            registrar("cajado_cristal", p -> new CajadoItem(600, p), p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final RegistryObject<Item> OCULOS =
            registrar("oculos_revelacao", Item::new, p -> p.stacksTo(1).equippable(EquipmentSlot.HEAD).rarity(Rarity.RARE));
    public static final RegistryObject<Item> BOTAS =
            registrar("botas_viajante", Item::new, p -> p.stacksTo(1).equippable(EquipmentSlot.FEET).rarity(Rarity.RARE)
                    .attributes(ItemAttributeModifiers.builder()
                            .add(Attributes.MOVEMENT_SPEED,
                                    new AttributeModifier(Arcanismo.id("botas_viajante"), 0.3,
                                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                                    EquipmentSlotGroup.FEET)
                            .build()));
    public static final RegistryObject<Item> PO_VITAL =
            registrar("po_vital", PoVitalItem::new, p -> p);

    private ModItens() {}

    public static RegistryObject<Item> cristal(Aspecto a) {
        return CRISTAIS.get(a);
    }

    public static Iterable<RegistryObject<Item>> cristais() {
        return CRISTAIS.values();
    }

    /** Cria as propriedades com o ID já definido (obrigatório desde a 1.21.2). */
    public static Item.Properties propriedades(String nome) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Arcanismo.id(nome)));
    }

    private static RegistryObject<Item> registrar(String nome, Function<Item.Properties, Item> fabrica,
                                                  UnaryOperator<Item.Properties> ajustes) {
        return ITENS.register(nome, () -> fabrica.apply(ajustes.apply(propriedades(nome))));
    }
}
