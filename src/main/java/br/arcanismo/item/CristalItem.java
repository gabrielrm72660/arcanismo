package br.arcanismo.item;

import br.arcanismo.aspecto.Aspecto;
import net.minecraft.world.item.Item;

/** Cristal de Essência de um aspecto primordial. Também serve de "foco" para o Cajado. */
public class CristalItem extends Item {
    private final Aspecto aspecto;

    public CristalItem(Aspecto aspecto, Properties propriedades) {
        super(propriedades);
        this.aspecto = aspecto;
    }

    public Aspecto aspecto() {
        return aspecto;
    }
}
