package br.arcanismo.aspecto;

import static br.arcanismo.aspecto.Aspecto.*;

import java.util.HashMap;
import java.util.Map;

import br.arcanismo.bloco.AglomeradoBlock;
import br.arcanismo.item.CristalItem;
import br.arcanismo.registro.ModItens;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Tabela que diz de quais aspectos cada coisa é feita.
 * Itens fora da tabela recebem um valor aproximado (veja {@link #deItem}).
 */
public final class RegistroAspectos {
    private static Map<Item, ListaAspectos> itens;
    private static Map<EntityType<?>, ListaAspectos> entidades;

    private RegistroAspectos() {}

    private static void item(Item item, Object... pares) {
        itens.put(item, ListaAspectos.de(pares));
    }

    private static void entidade(EntityType<?> tipo, Object... pares) {
        entidades.put(tipo, ListaAspectos.de(pares));
    }

    private static synchronized void iniciar() {
        if (itens != null) return;
        itens = new HashMap<>();
        entidades = new HashMap<>();

        // Terra e pedra
        item(Items.DIRT, TERRA, 2);
        item(Items.GRASS_BLOCK, TERRA, 2, VIDA, 1);
        item(Items.STONE, TERRA, 2);
        item(Items.COBBLESTONE, TERRA, 1, ENTROPIA, 1);
        item(Items.DEEPSLATE, TERRA, 3);
        item(Items.COBBLED_DEEPSLATE, TERRA, 2, ENTROPIA, 1);
        item(Items.GRAVEL, TERRA, 1, ENTROPIA, 2);
        item(Items.SAND, TERRA, 1, ENTROPIA, 1);
        item(Items.CLAY_BALL, TERRA, 1, AGUA, 1);
        item(Items.OBSIDIAN, TERRA, 3, FOGO, 2, ORDEM, 2);
        item(Items.NETHERRACK, TERRA, 1, FOGO, 2);
        item(Items.GLASS, TERRA, 1, ORDEM, 2);

        // Madeira e plantas
        item(Items.OAK_LOG, TERRA, 2, VIDA, 2);
        item(Items.OAK_PLANKS, TERRA, 1, VIDA, 1);
        item(Items.STICK, VIDA, 1);
        item(Items.OAK_SAPLING, VIDA, 3, TERRA, 1);
        item(Items.OAK_LEAVES, VIDA, 2, AR, 1);
        item(Items.WHEAT_SEEDS, VIDA, 1);
        item(Items.WHEAT, VIDA, 2, TERRA, 1);
        item(Items.SUGAR_CANE, VIDA, 1, AGUA, 2);
        item(Items.SUGAR, VIDA, 1, ENERGIA, 1);
        item(Items.CACTUS, VIDA, 2, AGUA, 1, ENTROPIA, 1);
        item(Items.KELP, VIDA, 1, AGUA, 2);

        // Comida e criaturas
        item(Items.APPLE, VIDA, 3);
        item(Items.BREAD, VIDA, 3, ORDEM, 1);
        item(Items.BEEF, VIDA, 4);
        item(Items.ROTTEN_FLESH, VIDA, 1, ENTROPIA, 3);
        item(Items.BONE, VIDA, 1, ENTROPIA, 2, ORDEM, 1);
        item(Items.BONE_MEAL, VIDA, 2, ENTROPIA, 1);
        item(Items.STRING, AR, 1, ORDEM, 1);
        item(Items.FEATHER, AR, 3);
        item(Items.LEATHER, VIDA, 2, ORDEM, 1);
        item(Items.WHITE_WOOL, VIDA, 1, AR, 2);
        item(Items.EGG, VIDA, 3);
        item(Items.SPIDER_EYE, VIDA, 1, ENTROPIA, 2);
        item(Items.GUNPOWDER, FOGO, 2, ENTROPIA, 3);
        item(Items.SLIME_BALL, VIDA, 2, AGUA, 2);
        item(Items.ENDER_PEARL, MAGIA, 4, AR, 2);
        item(Items.BLAZE_ROD, FOGO, 4, ENERGIA, 2);
        item(Items.GHAST_TEAR, AGUA, 2, MAGIA, 2, VIDA, 1);

        // Minerais e metais
        item(Items.COAL, FOGO, 2, ENERGIA, 2);
        item(Items.CHARCOAL, FOGO, 2, ENERGIA, 1);
        item(Items.RAW_IRON, METAL, 3, TERRA, 1);
        item(Items.IRON_INGOT, METAL, 4);
        item(Items.IRON_NUGGET, METAL, 1);
        item(Items.RAW_COPPER, METAL, 2, TERRA, 1);
        item(Items.COPPER_INGOT, METAL, 3);
        item(Items.RAW_GOLD, METAL, 2, MAGIA, 1, TERRA, 1);
        item(Items.GOLD_INGOT, METAL, 3, MAGIA, 1);
        item(Items.GOLD_NUGGET, METAL, 1);
        item(Items.DIAMOND, ORDEM, 5, TERRA, 2, MAGIA, 1);
        item(Items.EMERALD, ORDEM, 4, VIDA, 2);
        item(Items.LAPIS_LAZULI, MAGIA, 2, ORDEM, 1);
        item(Items.REDSTONE, ENERGIA, 3);
        item(Items.GLOWSTONE_DUST, ENERGIA, 2, FOGO, 1);
        item(Items.QUARTZ, ORDEM, 2, TERRA, 1);
        item(Items.AMETHYST_SHARD, MAGIA, 2, ORDEM, 1);
        item(Items.FLINT, TERRA, 1, FOGO, 1);

        // Água e gelo
        item(Items.WATER_BUCKET, AGUA, 6, METAL, 3);
        item(Items.ICE, AGUA, 3, ORDEM, 1);
        item(Items.SNOWBALL, AGUA, 1, AR, 1);
        item(Items.PRISMARINE_SHARD, AGUA, 2, ORDEM, 2);

        // Objetos mágicos
        item(Items.BOOK, ORDEM, 2, VIDA, 1, MAGIA, 1);
        item(Items.ENCHANTED_BOOK, MAGIA, 6, ORDEM, 2);
        item(Items.EXPERIENCE_BOTTLE, MAGIA, 4, ENERGIA, 2);
        item(Items.PAPER, ORDEM, 1, AR, 1);

        // Criaturas (para a Lente Arcana)
        entidade(EntityType.COW, VIDA, 4, TERRA, 1);
        entidade(EntityType.PIG, VIDA, 4, TERRA, 1);
        entidade(EntityType.SHEEP, VIDA, 3, AR, 1);
        entidade(EntityType.CHICKEN, VIDA, 2, AR, 2);
        entidade(EntityType.VILLAGER, VIDA, 3, ORDEM, 3);
        entidade(EntityType.ZOMBIE, VIDA, 1, ENTROPIA, 4);
        entidade(EntityType.SKELETON, ENTROPIA, 3, ORDEM, 1);
        entidade(EntityType.CREEPER, FOGO, 3, ENTROPIA, 3);
        entidade(EntityType.SPIDER, VIDA, 2, ENTROPIA, 2);
        entidade(EntityType.ENDERMAN, MAGIA, 5, AR, 2);
        entidade(EntityType.WITCH, MAGIA, 4, ENTROPIA, 2);
        entidade(EntityType.BLAZE, FOGO, 5, ENERGIA, 3);
        entidade(EntityType.SQUID, AGUA, 4, VIDA, 2);
        entidade(EntityType.BAT, AR, 3, ENTROPIA, 1);
    }

    /** Aspectos de UMA unidade do item. Nunca retorna nulo. */
    public static ListaAspectos deItem(ItemStack pilha) {
        iniciar();
        if (pilha.isEmpty()) return ListaAspectos.VAZIA;
        Item item = pilha.getItem();

        ListaAspectos conhecida = itens.get(item);
        if (conhecida != null) return conhecida;

        // Itens do próprio mod
        if (item instanceof CristalItem cristal) {
            return ListaAspectos.de(cristal.aspecto(), 3, MAGIA, 1);
        }
        if (item == ModItens.PO_DE_VIS.get()) return ListaAspectos.de(ENERGIA, 3, MAGIA, 3);
        if (item == ModItens.LINGOTE_ARCANITA.get()) return ListaAspectos.de(METAL, 4, MAGIA, 3);
        if (item == ModItens.SAL_PURIFICADOR.get()) return ListaAspectos.de(ORDEM, 3, VIDA, 1);

        if (item instanceof BlockItem bi && bi.getBlock() instanceof AglomeradoBlock ag) {
            return ListaAspectos.de(ag.aspecto(), 4, MAGIA, 1);
        }

        // Aproximações para o resto do jogo
        if (pilha.has(DataComponents.FOOD)) return ListaAspectos.de(VIDA, 2);
        if (item instanceof BlockItem) return ListaAspectos.de(TERRA, 1, ORDEM, 1);
        return ListaAspectos.de(ORDEM, 1, ENTROPIA, 1);
    }

    public static ListaAspectos deEntidade(LivingEntity alvo) {
        iniciar();
        ListaAspectos conhecida = entidades.get(alvo.getType());
        if (conhecida != null) return conhecida;
        if (alvo instanceof Enemy) return ListaAspectos.de(ENTROPIA, 3, VIDA, 1);
        return ListaAspectos.de(VIDA, 3);
    }
}
