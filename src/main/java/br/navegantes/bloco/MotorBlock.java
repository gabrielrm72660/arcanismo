package br.navegantes.bloco;

/**
 * Motor: igual à Fornalha a Vapor, mas bem mais forte (e mais faminto por combustível).
 * Parece um alto-forno.
 */
public class MotorBlock extends FornalhaBlock {
    public MotorBlock(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public double potencia() {
        return 0.30;
    }

    @Override
    public int consumo() {
        return 2;
    }
}
