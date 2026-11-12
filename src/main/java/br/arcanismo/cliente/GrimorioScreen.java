package br.arcanismo.cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import br.arcanismo.Arcanismo;
import br.arcanismo.aspecto.Aspecto;
import br.arcanismo.conhecimento.Pesquisa;
import br.arcanismo.grimorio.GrimorioMenu;
import br.arcanismo.registro.ModBlocos;
import br.arcanismo.registro.ModItens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * O Grimório Arcano ilustrado, com duas abas:
 *  - Livro: páginas com aspectos, receitas da Mesa e do Caldeirão, feitiços e lugares.
 *  - Pesquisas: mapa arrastável com a árvore de pesquisas (arraste com o mouse,
 *    clique num nó para ver o custo e pesquisar).
 */
public class GrimorioScreen extends AbstractContainerScreen<GrimorioMenu> {
    private static final Identifier TEXTURA = Arcanismo.id("textures/gui/grimorio.png");
    private static final Identifier ICONES = Arcanismo.id("textures/gui/aspectos.png");
    private static final int LARG = 256, ALT = 180;
    private static final int ESCALA = 26; // pixels por unidade do mapa

    // Cores (ARGB)
    static final int TINTA = 0xFF3A2614;
    static final int TITULO = 0xFF5A1E78;
    static final int SUAVE = 0xFF7A6448;
    static final int DOURADO = 0xFF9A6A14;

    private final List<Pagina> paginas = new ArrayList<>();
    private int espalho = 0; // par de páginas aberto
    private int esq, topo;
    private Button anterior, proxima, indice, abaLivro, abaMapa, botaoPesquisar;

    // Estado do mapa
    private boolean modoMapa = false;
    private double mapaX = 150, mapaY = 40;
    private boolean arrastando = false;
    private double ultimoX, ultimoY, arrasteTotal;
    private Pesquisa selecionada = null;

    public GrimorioScreen(GrimorioMenu menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo);
        montarPaginas();
    }

    // ------------------------------------------------------------------ estrutura

    @Override
    protected void init() {
        super.init();
        esq = (this.width - LARG) / 2;
        topo = (this.height - ALT) / 2 - 8;
        abaLivro = this.addRenderableWidget(Button.builder(Component.literal("Livro"), b -> mudarModo(false))
                .bounds(esq + 20, topo - 18, 60, 16).build());
        abaMapa = this.addRenderableWidget(Button.builder(Component.literal("Pesquisas"), b -> mudarModo(true))
                .bounds(esq + 84, topo - 18, 72, 16).build());
        anterior = this.addRenderableWidget(Button.builder(Component.literal("◀ Anterior"), b -> virar(-1))
                .bounds(esq + 18, topo + ALT + 4, 80, 20).build());
        proxima = this.addRenderableWidget(Button.builder(Component.literal("Próxima ▶"), b -> virar(1))
                .bounds(esq + LARG - 98, topo + ALT + 4, 80, 20).build());
        indice = this.addRenderableWidget(Button.builder(Component.literal("Índice"), b -> { espalho = 0; atualizarBotoes(); })
                .bounds(esq + LARG / 2 - 30, topo + ALT + 4, 60, 20).build());
        botaoPesquisar = this.addRenderableWidget(Button.builder(Component.literal("Pesquisar"), b -> pesquisar())
                .bounds(esq + LARG - 88, topo + ALT - 36, 66, 16).build());
        atualizarBotoes();
    }

    private void mudarModo(boolean mapa) {
        modoMapa = mapa;
        selecionada = null;
        atualizarBotoes();
    }

    private int totalEspalhos() {
        return (paginas.size() + 1) / 2;
    }

    private void virar(int d) {
        espalho = Math.max(0, Math.min(totalEspalhos() - 1, espalho + d));
        atualizarBotoes();
    }

    private void atualizarBotoes() {
        if (anterior == null) return;
        anterior.visible = proxima.visible = indice.visible = !modoMapa;
        anterior.active = espalho > 0;
        proxima.active = espalho < totalEspalhos() - 1;
        abaLivro.active = modoMapa;
        abaMapa.active = !modoMapa;
        boolean disponivel = selecionada != null && !this.menu.tem(selecionada) && this.menu.requisitosOk(selecionada);
        botaoPesquisar.visible = modoMapa && disponivel;
        botaoPesquisar.active = disponivel && this.menu.podePagar(selecionada);
    }

    private void pesquisar() {
        if (selecionada != null && this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selecionada.ordinal());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // Sem rótulos de inventário: o livro desenha tudo.
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float parcial) {
        super.extractBackground(g, mouseX, mouseY, parcial);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURA, esq, topo, 0.0f, 0.0f, LARG, ALT, 256, 256);
        atualizarBotoes();

        Pincel p = new Pincel(g, this.font, mouseX, mouseY);
        if (modoMapa) {
            desenharMapa(p, mouseX, mouseY);
            return;
        }
        int i = espalho * 2;
        if (i < paginas.size()) {
            paginas.get(i).desenhar(p, esq + 18, topo + 16);
            p.centro(String.valueOf(i + 1), esq + 68, topo + 160, SUAVE);
        }
        if (i + 1 < paginas.size()) {
            paginas.get(i + 1).desenhar(p, esq + 137, topo + 16);
            p.centro(String.valueOf(i + 2), esq + 187, topo + 160, SUAVE);
        }
    }

    // ------------------------------------------------------------------ mapa de pesquisas

    private int mx1() { return esq + 14; }
    private int my1() { return topo + 12; }
    private int mx2() { return esq + 242; }
    private int my2() { return topo + 168; }

    private boolean dentroMapa(double x, double y) {
        return x >= mx1() && x < mx2() && y >= my1() && y < my2();
    }

    private int noX(Pesquisa p) { return (int) ((mx1() + mx2()) / 2 + mapaX + p.x * ESCALA); }
    private int noY(Pesquisa p) { return (int) ((my1() + my2()) / 2 + mapaY + p.y * ESCALA); }
    private static int raio(Pesquisa p) { return p == Pesquisa.INFUSAO ? 15 : 11; }

    private Pesquisa noEm(double x, double y) {
        for (Pesquisa p : Pesquisa.values()) {
            int r = raio(p);
            if (Math.abs(x - noX(p)) <= r && Math.abs(y - noY(p)) <= r) return p;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent evento, boolean duplo) {
        if (modoMapa && evento.button() == 0 && dentroMapa(evento.x(), evento.y())
                && !(botaoPesquisar.visible && botaoPesquisar.isMouseOver(evento.x(), evento.y()))) {
            arrastando = true;
            ultimoX = evento.x();
            ultimoY = evento.y();
            arrasteTotal = 0;
            return true;
        }
        return super.mouseClicked(evento, duplo);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent evento) {
        if (arrastando) {
            arrastando = false;
            if (arrasteTotal < 4) {
                selecionada = noEm(evento.x(), evento.y());
                atualizarBotoes();
            }
            return true;
        }
        return super.mouseReleased(evento);
    }

    private void desenharMapa(Pincel p, int mouseX, int mouseY) {
        GuiGraphicsExtractor g = p.g;
        // Arrasto: acompanha o mouse enquanto o botão está pressionado
        if (arrastando) {
            double dx = mouseX - ultimoX, dy = mouseY - ultimoY;
            mapaX += dx;
            mapaY += dy;
            arrasteTotal += Math.abs(dx) + Math.abs(dy);
            ultimoX = mouseX;
            ultimoY = mouseY;
        }

        g.enableScissor(mx1(), my1(), mx2(), my2());
        g.fill(mx1(), my1(), mx2(), my2(), 0x30402030);

        // Ligações
        for (Pesquisa alvo : Pesquisa.values()) {
            for (Pesquisa req : alvo.requisitos) {
                int cor;
                if (this.menu.tem(alvo)) cor = alvo.categoria.cor;
                else if (this.menu.requisitosOk(alvo)) cor = (alvo.categoria.cor & 0x00FFFFFF) | 0xA0000000;
                else cor = 0x70706050;
                linha(g, noX(req), noY(req), noX(alvo), noY(alvo), cor);
            }
        }

        // Nós
        long tempo = System.currentTimeMillis();
        Pesquisa sobMouse = dentroMapa(mouseX, mouseY) ? noEm(mouseX, mouseY) : null;
        for (Pesquisa q : Pesquisa.values()) {
            int x = noX(q), y = noY(q), r = raio(q);
            boolean feita = this.menu.tem(q);
            boolean disp = !feita && this.menu.requisitosOk(q);
            int borda = feita || disp ? q.categoria.cor : 0xFF6A6458;
            if (disp && this.menu.podePagar(q)) {
                int a = (int) (120 + 100 * Math.sin(tempo / 250.0));
                g.fill(x - r - 3, y - r - 3, x + r + 3, y + r + 3, (a << 24) | (q.categoria.cor & 0x00FFFFFF));
            }
            g.fill(x - r - 1, y - r - 1, x + r + 1, y + r + 1, 0xFF201018);
            g.fill(x - r, y - r, x + r, y + r, borda);
            int miolo = feita ? 0xFFF4E8C8 : (disp ? 0xFFD8C8A4 : 0xFF8C8478);
            g.fill(x - r + 2, y - r + 2, x + r - 2, y + r - 2, miolo);
            g.item(q.icone.get(), x - 8, y - 8);
            if (!feita && !disp) g.fill(x - r + 2, y - r + 2, x + r - 2, y + r - 2, 0x99302820);
            if (feita) g.fill(x + r - 5, y - r + 1, x + r - 1, y - r + 5, 0xFFE8B830);
            if (q == selecionada) {
                g.fill(x - r - 2, y - r - 2, x + r + 2, y - r - 1, 0xFFFFFFFF);
                g.fill(x - r - 2, y + r + 1, x + r + 2, y + r + 2, 0xFFFFFFFF);
            }
        }
        g.disableScissor();

        // Dica ao passar o mouse
        if (sobMouse != null && sobMouse != selecionada && !arrastando) {
            g.setTooltipForNextFrame(this.font, List.of(Component.literal(sobMouse.nome),
                    Component.literal(sobMouse.categoria.nome).withColor(sobMouse.categoria.cor & 0x00FFFFFF)),
                    Optional.empty(), mouseX, mouseY);
        }

        if (selecionada != null) desenharDetalhes(p, selecionada);
        else p.texto("Arraste para mover · clique num nó", mx1() + 4, my2() - 11, 0xFF5A4A3A);
    }

    private void desenharDetalhes(Pincel p, Pesquisa q) {
        GuiGraphicsExtractor g = p.g;
        int x1 = mx1() + 4, x2 = mx2() - 4, y2 = my2() - 3, y1 = y2 - 54;
        g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, q.categoria.cor);
        g.fill(x1, y1, x2, y2, 0xF0261A2E);
        g.text(this.font, q.nome, x1 + 4, y1 + 4, 0xFFF2D27A, false);
        String cat = q.categoria.nome;
        g.text(this.font, cat, x2 - 4 - this.font.width(cat), y1 + 4, q.categoria.cor, false);

        List<String> linhas = p.quebrar(q.descricao, x2 - x1 - 8);
        int y = y1 + 15;
        for (int i = 0; i < Math.min(2, linhas.size()); i++) {
            g.text(this.font, linhas.get(i), x1 + 4, y, 0xFFD8CCE8, false);
            y += 10;
        }

        int iy = y2 - 18;
        if (this.menu.tem(q)) {
            g.text(this.font, "✔ Concluída", x1 + 4, iy + 4, 0xFF7CE07C, false);
        } else if (!this.menu.requisitosOk(q)) {
            StringBuilder falta = new StringBuilder("Requer: ");
            for (Pesquisa r : q.requisitos) if (!this.menu.tem(r)) falta.append(r.nome).append(", ");
            String t = falta.substring(0, falta.length() - 2);
            while (this.font.width(t) > x2 - x1 - 8 && t.length() > 4) t = t.substring(0, t.length() - 1);
            g.text(this.font, t, x1 + 4, iy + 4, 0xFFE07070, false);
        } else {
            int ix = x1 + 4;
            for (Map.Entry<Aspecto, Integer> e : q.custo.mapa().entrySet()) {
                g.blit(RenderPipelines.GUI_TEXTURED, ICONES, ix, iy, e.getKey().ordinal() * 16.0f, 0.0f, 16, 16, 160, 16);
                boolean ok = this.menu.pontos(e.getKey()) >= e.getValue();
                String n = this.menu.pontos(e.getKey()) + "/" + e.getValue();
                g.text(this.font, n, ix + 18, iy + 4, ok ? 0xFF7CE07C : 0xFFE07070, false);
                ix += 22 + this.font.width(n);
            }
        }
    }

    /** Linha de 2 pixels de espessura, desenhada em pequenos quadrados. */
    private static void linha(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int cor) {
        int passos = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (passos == 0) return;
        for (int i = 0; i <= passos; i += 2) {
            int x = x0 + (x1 - x0) * i / passos;
            int y = y0 + (y1 - y0) * i / passos;
            g.fill(x - 1, y - 1, x + 1, y + 1, cor);
        }
    }

    // ------------------------------------------------------------------ desenho

    /** Pequeno ajudante de desenho: texto com quebra de linha, slots, itens e setas. */
    static final class Pincel {
        final GuiGraphicsExtractor g;
        final Font fonte;
        final int mx, my;
        static final int LARGURA = 102;

        Pincel(GuiGraphicsExtractor g, Font fonte, int mx, int my) {
            this.g = g;
            this.fonte = fonte;
            this.mx = mx;
            this.my = my;
        }

        /** Desenha o item e mostra a dica (nome) quando o mouse passa por cima. */
        void item(ItemStack item, int x, int y) {
            if (item.isEmpty()) return;
            g.item(item, x, y);
            if (mx >= x && mx < x + 16 && my >= y && my < y + 16) {
                g.setTooltipForNextFrame(fonte, item, mx, my);
            }
        }

        void texto(String s, int x, int y, int cor) {
            g.text(fonte, s, x, y, cor, false);
        }

        void centro(String s, int cx, int y, int cor) {
            g.text(fonte, s, cx - fonte.width(s) / 2, y, cor, false);
        }

        /** Título centralizado com divisor ornamental. Retorna o y seguinte. */
        int titulo(String s, int x, int y) {
            centro(s, x + LARGURA / 2, y, TITULO);
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURA, x + 1, y + 11, 0.0f, 222.0f, 100, 5, 256, 256);
            return y + 20;
        }

        /** Parágrafo com quebra automática. Retorna o y seguinte. */
        int paragrafo(String s, int x, int y, int cor) {
            for (String linha : quebrar(s, LARGURA)) {
                texto(linha, x, y, cor);
                y += 10;
            }
            return y + 3;
        }

        List<String> quebrar(String s, int largura) {
            List<String> linhas = new ArrayList<>();
            for (String bloco : s.split("\n")) {
                StringBuilder atual = new StringBuilder();
                for (String palavra : bloco.split(" ")) {
                    String teste = atual.length() == 0 ? palavra : atual + " " + palavra;
                    if (fonte.width(teste) > largura && atual.length() > 0) {
                        linhas.add(atual.toString());
                        atual = new StringBuilder(palavra);
                    } else {
                        atual = new StringBuilder(teste);
                    }
                }
                linhas.add(atual.toString());
            }
            return linhas;
        }

        void slot(int x, int y, ItemStack item) {
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURA, x, y, 0.0f, 192.0f, 18, 18, 256, 256);
            item(item, x + 1, y + 1);
        }

        void slotResultado(int x, int y, ItemStack item) {
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURA, x, y, 20.0f, 192.0f, 26, 26, 256, 256);
            item(item, x + 5, y + 5);
        }

        void seta(int x, int y) {
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURA, x, y, 48.0f, 192.0f, 22, 15, 256, 256);
        }

        void quadradoCor(int x, int y, int cor) {
            g.fill(x, y, x + 7, y + 7, 0xFF2A1A0A);
            g.fill(x + 1, y + 1, x + 6, y + 6, cor);
        }
    }

    static int corAspecto(Aspecto a) {
        return switch (a) {
            case AR -> 0xFFB8960A;
            case TERRA -> 0xFF2E7D1E;
            case FOGO -> 0xFFC4380E;
            case AGUA -> 0xFF1C6FB8;
            case ORDEM -> 0xFF6A7088;
            case ENTROPIA -> 0xFF3A2A48;
            case VIDA -> 0xFF3C9A2A;
            case METAL -> 0xFF5A5A5A;
            case ENERGIA -> 0xFFC07A08;
            case MAGIA -> 0xFF8A2AB8;
        };
    }

    // ------------------------------------------------------------------ tipos de página

    interface Pagina {
        void desenhar(Pincel p, int x, int y);
    }

    record PaginaTexto(String titulo, String texto) implements Pagina {
        public void desenhar(Pincel p, int x, int y) {
            y = p.titulo(titulo, x, y);
            p.paragrafo(texto, x, y, TINTA);
        }
    }

    /** Receita de bancada 3x3. Ingredientes nulos = espaço vazio. */
    record PaginaReceita(String titulo, List<Supplier<ItemStack>> grade, Supplier<ItemStack> resultado,
                         String texto) implements Pagina {
        public void desenhar(Pincel p, int x, int y) {
            y = p.titulo(titulo, x, y);
            for (int i = 0; i < 9; i++) {
                Supplier<ItemStack> s = i < grade.size() ? grade.get(i) : null;
                p.slot(x + (i % 3) * 18, y + (i / 3) * 18, s == null ? ItemStack.EMPTY : s.get());
            }
            p.seta(x + 57, y + 19);
            p.slotResultado(x + 78, y + 14, resultado.get());
            p.paragrafo(texto, x, y + 60, TINTA);
        }
    }

    /** Receita do Caldeirão Arcano. */
    record PaginaCaldeirao(String titulo, Supplier<ItemStack> catalisador, Object[] custo,
                           Supplier<ItemStack> resultado, String pesquisa, String texto) implements Pagina {
        public void desenhar(Pincel p, int x, int y) {
            y = p.titulo(titulo, x, y);
            p.slot(x + 4, y + 4, new ItemStack(ModBlocos.CALDEIRAO_ARCANO.get()));
            p.texto("+", x + 26, y + 9, SUAVE);
            p.slot(x + 34, y + 4, catalisador.get());
            p.seta(x + 56, y + 6);
            p.slotResultado(x + 78, y, resultado.get());
            y += 32;
            p.texto("Essência:", x, y, SUAVE);
            y += 10;
            for (int i = 0; i + 1 < custo.length; i += 2) {
                Aspecto a = (Aspecto) custo[i];
                p.quadradoCor(x + 2, y, corAspecto(a));
                p.texto(a.nome + " " + custo[i + 1], x + 12, y, corAspecto(a));
                y += 10;
            }
            p.texto("Pesquisa: " + pesquisa, x, y + 2, DOURADO);
            p.paragrafo(texto, x, y + 15, TINTA);
        }
    }

    /** Receita do Altar de Infusão: item central cercado pelos ingredientes dos pedestais. */
    record PaginaInfusao(String titulo, Supplier<ItemStack> centro, List<Supplier<ItemStack>> pedestais,
                         Object[] essencia, int vis, Supplier<ItemStack> resultado, String pesquisa) implements Pagina {
        public void desenhar(Pincel p, int x, int y) {
            y = p.titulo(titulo, x, y);
            int cx = x + 30, cy = y + 30;
            int n = pedestais.size();
            for (int k = 0; k < n; k++) {
                double a = -Math.PI / 2 + k * 2 * Math.PI / n;
                p.slot(cx - 9 + (int) Math.round(Math.cos(a) * 25), cy - 9 + (int) Math.round(Math.sin(a) * 25),
                        pedestais.get(k).get());
            }
            p.slot(cx - 9, cy - 9, centro.get());
            p.seta(x + 60, cy - 7);
            p.slotResultado(x + 78, cy - 13, resultado.get());
            y += 62;
            for (int i = 0; i + 1 < essencia.length; i += 2) {
                Aspecto a = (Aspecto) essencia[i];
                p.quadradoCor(x + 2, y, corAspecto(a));
                p.texto(a.nome + " " + essencia[i + 1], x + 12, y, corAspecto(a));
                y += 10;
            }
            p.texto("Vis " + vis + " · " + pesquisa, x, y + 2, DOURADO);
        }
    }

    record PaginaLista(String titulo, List<Supplier<ItemStack>> icones, List<String> nomes,
                       List<String> descricoes) implements Pagina {
        public void desenhar(Pincel p, int x, int y) {
            y = p.titulo(titulo, x, y);
            for (int i = 0; i < nomes.size(); i++) {
                ItemStack icone = icones.get(i).get();
                p.item(icone, x, y);
                p.texto(nomes.get(i), x + 20, y, TITULO);
                p.texto(descricoes.get(i), x + 20, y + 9, TINTA);
                y += 20;
            }
        }
    }

    record PaginaAspectos(String titulo, boolean primordiais) implements Pagina {
        public void desenhar(Pincel p, int x, int y) {
            y = p.titulo(titulo, x, y);
            for (Aspecto a : Aspecto.values()) {
                if (a.primordial() != primordiais) continue;
                p.quadradoCor(x, y + 1, corAspecto(a));
                p.texto(a.nome, x + 11, y, corAspecto(a));
                String detalhe = primordiais ? descricaoPrimordial(a) : a.componenteA.nome + " + " + a.componenteB.nome;
                p.texto(detalhe, x + 11, y + 9, SUAVE);
                y += 21;
            }
            if (!primordiais) {
                p.paragrafo("Escaneie com a Lente Arcana para descobrir de que aspectos cada coisa é feita.", x, y + 4, TINTA);
            }
        }

        static String descricaoPrimordial(Aspecto a) {
            return switch (a) {
                case AR -> "vento, voo, leveza";
                case TERRA -> "pedra, solo, firmeza";
                case FOGO -> "calor, chama, fúria";
                case AGUA -> "fluxo, cura, mar";
                case ORDEM -> "estrutura, cristal";
                case ENTROPIA -> "decadência, caos";
                default -> "";
            };
        }
    }

    record PaginaIndice(GrimorioScreen tela, List<String> secoes, List<Integer> numeros) implements Pagina {
        public void desenhar(Pincel p, int x, int y) {
            y = p.titulo("Índice", x, y);
            for (int i = 0; i < secoes.size(); i++) {
                String num = String.valueOf(numeros.get(i));
                p.texto(secoes.get(i), x + 2, y, TINTA);
                p.texto(num, x + Pincel.LARGURA - p.fonte.width(num), y, SUAVE);
                y += 12;
            }
        }
    }

    record PaginaCapa() implements Pagina {
        public void desenhar(Pincel p, int x, int y) {
            p.centro("✦ Grimório ✦", x + Pincel.LARGURA / 2, y + 6, TITULO);
            p.centro("Arcano", x + Pincel.LARGURA / 2, y + 18, TITULO);
            p.g.blit(RenderPipelines.GUI_TEXTURED, TEXTURA, x + 1, y + 32, 0.0f, 222.0f, 100, 5, 256, 256);
            p.slotResultado(x + 38, y + 38, new ItemStack(ModItens.GRIMORIO.get()));
            p.paragrafo("Tudo o que existe é feito de aspectos. Quem os compreende pode dobrar o mundo.", x, y + 68, TINTA);
            p.paragrafo("Agache e clique com o livro para pesquisar.", x, y + 122, SUAVE);
        }
    }

    // ------------------------------------------------------------------ conteúdo

    private static Supplier<ItemStack> i(Item item) {
        return () -> new ItemStack(item);
    }

    private static Supplier<ItemStack> m(Supplier<? extends ItemLike> item) {
        return () -> new ItemStack(item.get());
    }

    /** Qualquer cristal: alterna entre os seis a cada segundo. */
    private static Supplier<ItemStack> cristalQualquer() {
        Aspecto[] p = {Aspecto.AR, Aspecto.TERRA, Aspecto.FOGO, Aspecto.AGUA, Aspecto.ORDEM, Aspecto.ENTROPIA};
        return () -> new ItemStack(ModItens.cristal(p[(int) ((System.currentTimeMillis() / 1000) % p.length)]).get());
    }

    private static Supplier<ItemStack> cristal(Aspecto a) {
        return () -> new ItemStack(ModItens.cristal(a).get());
    }

    private void montarPaginas() {
        Supplier<ItemStack> C = cristalQualquer();
        List<String> secoes = List.of("Aspectos", "Conhecimento", "Mesa Arcana", "Caldeirão", "Infusão", "Feitiços", "Lugares");
        List<Integer> numeros = new ArrayList<>();

        paginas.add(new PaginaCapa());
        PaginaIndice indice = new PaginaIndice(this, secoes, numeros);
        paginas.add(indice);

        // Aspectos
        numeros.add(paginas.size() + 1);
        paginas.add(new PaginaAspectos("Primordiais", true));
        paginas.add(new PaginaAspectos("Compostos", false));

        // Conhecimento
        numeros.add(paginas.size() + 1);
        paginas.add(new PaginaTexto("A Lente Arcana",
                "Segurando a Lente, o visor mostra os aspectos do que você olha.\n"
                        + "Segure o botão direito para absorver a essência e ganhar conhecimento.\n"
                        + "Item na outra mão: um clique basta."));
        paginas.add(new PaginaTexto("Pesquisas",
                "Abra a aba Pesquisas no topo do livro: um mapa com 26 pesquisas em 7 categorias.\n"
                        + "Arraste o mapa, clique num nó e pague o custo em conhecimento."));

        // Bancada
        numeros.add(paginas.size() + 1);
        paginas.add(new PaginaReceita("Grimório Arcano",
                List.of(i(Items.BOOK), C), m(ModItens.GRIMORIO),
                "Bancada comum: livro + qualquer cristal."));
        paginas.add(new PaginaReceita("Mesa Arcana",
                java.util.Arrays.asList(null, C, null,
                        C, i(Items.CRAFTING_TABLE), C,
                        null, C, null),
                m(ModBlocos.MESA_ARCANA), "Bancada comum. Tudo o mais se faz nela, pagando Vis."));
        paginas.add(new PaginaTexto("Usando a Mesa",
                "Ponha o cajado no encaixe dourado: ele paga o Vis da receita.\n"
                        + "Monte os itens na grade 3x3 como numa bancada comum.\n"
                        + "Recarregue o cajado com cristais ou absorvendo Orbes de Luz."));
        paginas.add(new PaginaReceita("Lente Arcana",
                java.util.Arrays.asList(null, i(Items.GOLD_INGOT), null,
                        i(Items.GOLD_INGOT), i(Items.GLASS_PANE), i(Items.GOLD_INGOT),
                        null, C, null),
                m(ModItens.LENTE_ARCANA), "Mesa Arcana · 5 Vis"));
        paginas.add(new PaginaReceita("Cajado de Aprendiz",
                java.util.Arrays.asList(null, null, C,
                        null, i(Items.STICK), null,
                        i(Items.GOLD_NUGGET), null, null),
                m(ModItens.CAJADO_APRENDIZ), "Mesa Arcana · sem custo. Guarda 100 Vis."));
        paginas.add(new PaginaReceita("Caldeirão Arcano",
                java.util.Arrays.asList(null, null, null,
                        C, null, C,
                        C, i(Items.CAULDRON), C),
                m(ModBlocos.CALDEIRAO_ARCANO), "Mesa Arcana · 10 Vis"));
        paginas.add(new PaginaReceita("Tijolo Arcano",
                List.of(i(Items.STONE_BRICKS), i(Items.STONE_BRICKS), i(Items.STONE_BRICKS),
                        i(Items.STONE_BRICKS), i(Items.AMETHYST_SHARD), i(Items.STONE_BRICKS),
                        i(Items.STONE_BRICKS), i(Items.STONE_BRICKS), i(Items.STONE_BRICKS)),
                () -> new ItemStack(ModBlocos.TIJOLO_ARCANO.get(), 8), "Mesa Arcana · 2 Vis. Rende 8."));
        paginas.add(new PaginaReceita("Cajado de Arcanita",
                java.util.Arrays.asList(null, m(ModItens.LINGOTE_ARCANITA), C,
                        null, i(Items.BLAZE_ROD), m(ModItens.LINGOTE_ARCANITA),
                        i(Items.BLAZE_ROD), null, null),
                m(ModItens.CAJADO_ARCANITA), "Mesa · 40 Vis · Metalurgia. Guarda 300."));

        // Caldeirão
        numeros.add(paginas.size() + 1);
        paginas.add(new PaginaTexto("O Caldeirão",
                "1. Encha com balde de água.\n2. Fogo, lava, magma ou fogueira embaixo.\n"
                        + "3. Jogue itens (Q): viram essência.\n4. Clique com o catalisador.\n"
                        + "Mão vazia mostra a essência. Acima de 100, o excesso vira Fluxo!"));
        paginas.add(new PaginaCaldeirao("Pó de Vis", i(Items.REDSTONE),
                new Object[]{Aspecto.ENERGIA, 6, Aspecto.MAGIA, 2}, () -> new ItemStack(ModItens.PO_DE_VIS.get(), 2),
                "Alquimia", "Recarrega 50 de Vis."));
        paginas.add(new PaginaCaldeirao("Cristal Refinado", i(Items.AMETHYST_SHARD),
                new Object[]{Aspecto.FOGO, 8}, cristal(Aspecto.FOGO),
                "Alquimia", "Vale para qualquer primordial: 8 dele geram o cristal."));
        paginas.add(new PaginaCaldeirao("Arcanita", i(Items.IRON_INGOT),
                new Object[]{Aspecto.METAL, 6, Aspecto.MAGIA, 4}, m(ModItens.LINGOTE_ARCANITA),
                "Metalurgia", "Metal impregnado de magia."));
        paginas.add(new PaginaCaldeirao("Sal Purificador", i(Items.SUGAR),
                new Object[]{Aspecto.ORDEM, 8, Aspecto.VIDA, 4}, () -> new ItemStack(ModItens.SAL_PURIFICADOR.get(), 4),
                "Purificação", "Limpa a Terra Corrompida."));
        paginas.add(new PaginaCaldeirao("Pó Vital", i(Items.BONE_MEAL),
                new Object[]{Aspecto.VIDA, 6, Aspecto.TERRA, 2}, () -> new ItemStack(ModItens.PO_VITAL.get(), 2),
                "Pó Vital", "Plantas crescem de uma vez."));
        paginas.add(new PaginaCaldeirao("Transmutação", i(Items.COPPER_INGOT),
                new Object[]{Aspecto.METAL, 4, Aspecto.MAGIA, 4}, i(Items.GOLD_INGOT),
                "Transmutação", "Bloco de carvão + Terra 12, Ordem 12, Magia 6 = diamante."));
        paginas.add(new PaginaTexto("O Fluxo",
                "Essência desperdiçada vira Fluxo e corrompe a terra ao redor. A corrupção se espalha um pouco e enfraquece quem pisa nela.\n"
                        + "Use o Sal Purificador para curar o solo."));

        // Infusão
        numeros.add(paginas.size() + 1);
        paginas.add(new PaginaTexto("O Altar de Infusão",
                "1. Item central no altar.\n2. Ingredientes em pedestais ao redor (até 5 blocos).\n"
                        + "3. Essência nos caldeirões próximos.\n4. Clique no altar com o cajado.\n"
                        + "Pedestais simétricos deixam a infusão estável."));
        paginas.add(new PaginaReceita("Altar de Infusão",
                List.of(() -> ItemStack.EMPTY, m(ModItens.LINGOTE_ARCANITA), () -> ItemStack.EMPTY,
                        C, m(ModBlocos.TIJOLO_ARCANO), C,
                        m(ModBlocos.TIJOLO_ARCANO), m(ModBlocos.TIJOLO_ARCANO), m(ModBlocos.TIJOLO_ARCANO)),
                m(ModBlocos.ALTAR_INFUSAO), "Mesa · 50 Vis · Infusão"));
        paginas.add(new PaginaReceita("Pedestal Arcano",
                List.of(m(ModBlocos.TIJOLO_ARCANO), C, m(ModBlocos.TIJOLO_ARCANO),
                        () -> ItemStack.EMPTY, m(ModBlocos.TIJOLO_ARCANO), () -> ItemStack.EMPTY,
                        m(ModBlocos.TIJOLO_ARCANO), m(ModBlocos.TIJOLO_ARCANO), m(ModBlocos.TIJOLO_ARCANO)),
                m(ModBlocos.PEDESTAL_ARCANO), "Mesa · 10 Vis · Infusão"));
        paginas.add(new PaginaInfusao("Cajado de Cristal", m(ModItens.CAJADO_ARCANITA),
                List.of(C, i(Items.DIAMOND), C, C, i(Items.DIAMOND), C),
                new Object[]{Aspecto.MAGIA, 12, Aspecto.ENERGIA, 10}, 60, m(ModItens.CAJADO_CRISTAL), "Cajado de Cristal"));
        paginas.add(new PaginaInfusao("Óculos", i(Items.GOLDEN_HELMET),
                List.of(m(ModItens.LENTE_ARCANA), i(Items.GOLD_INGOT), i(Items.AMETHYST_SHARD), i(Items.GOLD_INGOT)),
                new Object[]{Aspecto.ORDEM, 8, Aspecto.MAGIA, 6}, 30, m(ModItens.OCULOS), "Óculos"));
        paginas.add(new PaginaInfusao("Botas do Viajante", i(Items.LEATHER_BOOTS),
                List.of(i(Items.FEATHER), cristal(Aspecto.AR), i(Items.FEATHER), cristal(Aspecto.AR)),
                new Object[]{Aspecto.AR, 12, Aspecto.VIDA, 4}, 25, m(ModItens.BOTAS), "Botas"));
        paginas.add(new PaginaInfusao("Lâmina do Vazio", i(Items.DIAMOND_SWORD),
                List.of(cristal(Aspecto.ENTROPIA), i(Items.OBSIDIAN), cristal(Aspecto.ENTROPIA), i(Items.ENDER_PEARL)),
                new Object[]{Aspecto.ENTROPIA, 16, Aspecto.MAGIA, 8}, 50, m(ModItens.LAMINA_VAZIO), "Lâmina"));
        paginas.add(new PaginaReceita("Picareta Arcana",
                java.util.Arrays.asList(m(ModItens.LINGOTE_ARCANITA), m(ModItens.LINGOTE_ARCANITA), m(ModItens.LINGOTE_ARCANITA),
                        null, i(Items.STICK), null, null, i(Items.STICK), null),
                m(ModItens.PICARETA_ARCANITA), "Mesa · 20 Vis · Ferramentas"));
        paginas.add(new PaginaReceita("Espada Arcana",
                java.util.Arrays.asList(null, m(ModItens.LINGOTE_ARCANITA), null,
                        null, m(ModItens.LINGOTE_ARCANITA), null, null, i(Items.STICK), null),
                m(ModItens.ESPADA_ARCANITA), "Mesa · 20 Vis · Ferramentas"));

        // Feitiços
        numeros.add(paginas.size() + 1);
        paginas.add(new PaginaLista("Feitiços",
                List.of(cristal(Aspecto.FOGO), cristal(Aspecto.AR), cristal(Aspecto.TERRA),
                        cristal(Aspecto.AGUA), cristal(Aspecto.ORDEM), cristal(Aspecto.ENTROPIA)),
                List.of("Fogo · 5 Vis", "Ar · 6 Vis", "Terra · 4 Vis", "Água · 8 Vis", "Ordem · 12 Vis", "Entropia · 10 Vis"),
                List.of("Bola de fogo", "Rajada de vento", "Escava um bloco", "Cura e apaga fogo", "Escudo protetor", "Murcha monstros")));
        paginas.add(new PaginaTexto("Vis",
                "Segure um cristal na outra mão para escolher o elemento. Sem cristal, o cajado lança fogo.\n"
                        + "Agache e clique para recarregar: Pó de Vis dá 50, um cristal dá 15.\n"
                        + "Outros elementos exigem a pesquisa Conjuração."));

        paginas.add(new PaginaTexto("Orbes de Luz",
                "Essência pura que vaga perto dos aglomerados de cristal. Seguem quem segura a Lente.\n"
                        + "Clique com o cajado: +25 Vis.\nClique com a mão: vira um cristal."));

        // Lugares
        numeros.add(paginas.size() + 1);
        paginas.add(new PaginaLista("Lugares Arcanos",
                List.of(m(ModBlocos.TIJOLO_ARCANO), m(ModBlocos.MINERIO_ARCANO_ARDOSIA), m(ModBlocos.TERRA_CORROMPIDA)),
                List.of("Vila dos Magos", "Cripta Arcana", "Torre Corrompida"),
                List.of("planícies, matas", "no subsolo", "pântanos, taigas")));
        paginas.add(new PaginaLista("Cristais",
                List.of(m(ModBlocos.aglomerado(Aspecto.FOGO)), m(ModBlocos.aglomerado(Aspecto.AGUA)),
                        m(ModBlocos.MINERIO_ARCANO), m(ModBlocos.MINERIO_ARCANO_ARDOSIA)),
                List.of("Aglomerados", "Minere-os", "Minério Arcano", "Em ardósia"),
                List.of("nas cavernas", "2 a 4 cristais", "cristais variados", "brilha no escuro")));
    }
}
