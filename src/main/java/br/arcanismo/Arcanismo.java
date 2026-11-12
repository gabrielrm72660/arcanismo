package br.arcanismo;

import br.arcanismo.evento.ModEventos;
import br.arcanismo.registro.ModAbas;
import br.arcanismo.registro.ModBlockEntities;
import br.arcanismo.registro.ModBlocos;
import br.arcanismo.registro.ModEntidades;
import br.arcanismo.registro.ModItens;
import br.arcanismo.registro.ModMenus;
import net.minecraft.resources.Identifier;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

/**
 * Arcanismo — mod de magia arcana inspirado em mods clássicos de "ciência mágica".
 *
 * Ciclo de jogo da versão 0.1:
 *  1. Minere Minério Arcano e obtenha Cristais de Essência.
 *  2. Faça a Lente Arcana e escaneie blocos, itens e criaturas para ganhar conhecimento.
 *  3. Use o Grimório Arcano para gastar conhecimento em pesquisas.
 *  4. Dissolva itens no Caldeirão Arcano e use catalisadores para criar materiais mágicos.
 *  5. Conjure feitiços com o Cajado — mas cuidado com o Fluxo, que corrompe a terra.
 */
@Mod(Arcanismo.MODID)
public final class Arcanismo {
    public static final String MODID = "arcanismo";

    /** Único ponto do mod que cria identificadores. Se a API mudar, ajuste só aqui. */
    public static Identifier id(String caminho) {
        return Identifier.fromNamespaceAndPath(MODID, caminho);
    }

    public Arcanismo(FMLJavaModLoadingContext contexto) {
        BusGroup barramento = contexto.getModBusGroup();

        // A ordem importa: ModBlocos registra também os BlockItems em ModItens.ITENS.
        ModBlocos.BLOCOS.register(barramento);
        ModItens.ITENS.register(barramento);
        ModBlockEntities.TIPOS.register(barramento);
        ModAbas.ABAS.register(barramento);
        ModMenus.MENUS.register(barramento);
        ModEntidades.ENTIDADES.register(barramento);

        ModEventos.registrar();
        if (!FMLLoader.getDist().isDedicatedServer()) {
            br.arcanismo.cliente.ClienteSetup.registrar(barramento);
        }
    }
}
