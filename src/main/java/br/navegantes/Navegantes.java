package br.navegantes;

import br.navegantes.navio.EventosNavio;
import br.navegantes.registro.ModAbas;
import br.navegantes.registro.ModBlocos;
import br.navegantes.registro.ModEntidades;
import br.navegantes.registro.ModItens;
import net.minecraft.resources.Identifier;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

/**
 * Navegantes — navios de verdade.
 *  1. Construa a estrutura na água e cole os blocos com a Cola Naval.
 *  2. Coloque o Timão: a estrutura colada vira um navio.
 *  3. Alimente a Fornalha a Vapor com carvão ou erga Velas para ganhar velocidade.
 *  4. Clique no Timão para guiar (W/A/S/D). Agachar lança ou recolhe a âncora.
 */
@Mod(Navegantes.MODID)
public final class Navegantes {
    public static final String MODID = "navegantes";

    /** Único ponto do mod que cria identificadores. Se a API mudar, ajuste só aqui. */
    public static Identifier id(String caminho) {
        return Identifier.fromNamespaceAndPath(MODID, caminho);
    }

    public Navegantes(FMLJavaModLoadingContext contexto) {
        BusGroup barramento = contexto.getModBusGroup();

        // A ordem importa: ModBlocos registra também os BlockItems em ModItens.ITENS.
        ModBlocos.BLOCOS.register(barramento);
        ModItens.ITENS.register(barramento);
        ModEntidades.ENTIDADES.register(barramento);
        ModAbas.ABAS.register(barramento);

        EventosNavio.registrar();
        if (!FMLLoader.getDist().isDedicatedServer()) {
            br.navegantes.cliente.ClienteSetup.registrar(barramento);
        }
    }
}
