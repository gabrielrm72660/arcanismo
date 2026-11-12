# Arcanismo — v0.1 (Forge, Minecraft 26.1)

Mod de magia arcana: aspectos, pesquisa, alquimia no caldeirão, feitiços, corrupção pelo Fluxo, uma vila de magos e duas dungeons.

## Como montar

1. Baixe o **MDK do Forge 26.1** (versão 62.0.9) e extraia numa pasta vazia.
2. Apague o mod de exemplo: a pasta `src/main/java/com/example` (ou equivalente).
3. Copie a pasta `src` deste pacote por cima da `src` do MDK.
4. Edite o `gradle.properties` do MDK:
   ```
   mod_id=arcanismo
   mod_name=Arcanismo
   mod_version=0.1.0
   mod_group_id=br.arcanismo
   mod_authors=Você
   mod_description=Magia arcana: aspectos, pesquisa, alquimia e masmorras.
   ```
5. Use **Java 25** (exigido pela 26.1) e rode `gradlew runClient` para testar, ou `gradlew build` para gerar o `.jar` em `build/libs`.
6. Coloque o `.jar` na pasta `mods` do perfil "Teste" do CurseForge.

## Como jogar

| Passo | O que fazer |
|---|---|
| 1 | Procure **Aglomerados de Cristal** no chão e no teto das cavernas (2 a 4 cristais cada) ou minere **Minério Arcano** (Y -48 a 64). |
| 2 | Faça o **Grimório** (livro + cristal) e a **Lente Arcana** (ouro, vidro, cristal). Clique com o Grimório para abrir o livro com todas as receitas. |
| 3 | Segurando a Lente, o visor mostra os aspectos do que você olha. **Segure o botão direito** para absorver a essência e ganhar conhecimento. Item na outra mão: um clique basta. |
| 4 | Grimório: clique para ver o progresso. **Agachado + clique** pesquisa a próxima disponível. |
| 5 | **Caldeirão Arcano**: encha com balde de água, ponha fogo/lava/magma/fogueira embaixo e jogue itens (Q). Eles viram essência. |
| 6 | Clique no caldeirão com um **catalisador** para criar algo. Mão vazia mostra a essência; agachado esvazia (e libera Fluxo). |
| 7 | **Cajado**: clique para conjurar; agachado + clique recarrega com Pó de Vis ou cristal. Cristal na outra mão muda o elemento. |

### Receitas do caldeirão

| Catalisador | Essência | Resultado | Pesquisa |
|---|---|---|---|
| Redstone | Energia 6, Magia 2 | 2 Pó de Vis | Alquimia |
| Fragmento de ametista | 8 de um primordial | Cristal desse aspecto | Alquimia |
| Lingote de ferro | Metal 6, Magia 4 | Lingote de Arcanita | Metalurgia |
| Açúcar | Ordem 8, Vida 4 | 4 Sal Purificador | Purificação |

Se a essência passar de 100, o excesso vira **Fluxo** e transforma terra em **Terra Corrompida**, que se espalha um pouco e enfraquece quem pisa. O **Sal Purificador** limpa.

### Feitiços (cristal na outra mão)

Sem cristal ou Fogo: bola de fogo · Ar: rajada de vento · Terra: escava o bloco · Água: cura · Ordem: escudo de absorção · Entropia: murcha monstros próximos.

## Estruturas

- **Vila dos Magos** (planícies, florestas, savanas): torre do arquimago com caldeirão funcionando sobre fogueira, biblioteca, observatório, três casas, fonte arcana, jardim e cinco magos.
- **Cripta Arcana** (subterrânea, qualquer bioma): câmara central com caldeirão sobre magma e quatro salas com geradores (zumbi, esqueleto, aranha, bruxa) e baús.
- **Torre Corrompida** (pântanos, florestas, taigas): torre em ruínas cercada de terra corrompida, com geradores e baú no topo.

Para achar no modo criativo: `/locate structure arcanismo:vila_magos` (ou `cripta_arcana`, `torre_corrompida`).

## Pontos que podem precisar de ajuste ao compilar

O código segue a API do Forge mais recente que conheço, mas não foi compilado contra a 26.1. Se der erro, os suspeitos mais prováveis são:

- `Arcanismo.java`: `context.getModBusGroup()` / `BusGroup` e `Identifier` (antes `ResourceLocation`).
- `ModBlockEntities`: construtor `new BlockEntityType<>(fábrica, Set.of(bloco))`.
- `CaldeiraoBlockEntity`: `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)`.
- `ModEventos`: `PlayerEvent.Clone.BUS.addListener(...)`.
- `level.isClientSide()` (método) e `getIntOr`/`getCompoundOrEmpty` em `CompoundTag`.
- Pasta do biome modifier: `data/arcanismo/forge/biome_modifier/`.

Compare com o mod de exemplo do MDK: ele mostra o jeito certo para a sua versão.

## Próxima versão (ideias)

Altar de infusão, nós de Vis no mundo, golens, interface gráfica do Grimório, mais focos de feitiço, armaduras de mago e trocas próprias para os magos da vila.

## Novidades da v0.3

- **Mapa de pesquisas** no Grimório (aba "Pesquisas"): 26 pesquisas em 7 categorias. Arraste o mapa com o mouse, clique num nó para ver custo e requisitos, e use o botão "Pesquisar".
- **Mesa Arcana**: todos os itens do mod são feitos nela, pagando Vis com o cajado.
- **Altar de Infusão** + **Pedestais**: item central no altar, ingredientes nos pedestais, essência nos caldeirões próximos, clique com o cajado. Pedestais simétricos = infusão estável.
- **Itens novos**: Cajado de Cristal (600 Vis), Óculos da Revelação, Botas do Viajante, Lâmina do Vazio, Picareta e Espada de Arcanita, Pó Vital.
- **Orbes de Luz**: com a mão viram cristal e dão +3 de conhecimento; com o cajado dão +25 Vis.
