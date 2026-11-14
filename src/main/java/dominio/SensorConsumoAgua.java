package dominio;

import java.util.Random;

/**
 * Macromedidor de vazão de um setor de abastecimento: consumo base entre 70 e
 * 130 m³/h e, em 10% das leituras, um acréscimo de 40 a 100 m³/h que simula
 * vazamento ou pico de uso.
 */
public class SensorConsumoAgua extends SensorSimulado {

    private static final double CONSUMO_MINIMO = 70.0;
    private static final double VARIACAO_CONSUMO = 60.0;
    private static final double CHANCE_DE_PICO = 0.1;
    private static final double PICO_MINIMO = 40.0;
    private static final double VARIACAO_PICO = 60.0;

    /** Uso normal: leituras aleatórias a cada execução. */
    public SensorConsumoAgua(String id, String nome, String regiao) {
        this(id, nome, regiao, new Random());
    }

    /** Para testes: com {@code new Random(semente)} as leituras se repetem. */
    public SensorConsumoAgua(String id, String nome, String regiao, Random random) {
        super(id, nome, regiao, TipoSensor.CONSUMO_AGUA, random);
    }

    @Override
    protected double simularLeitura(Random random) {
        // nextDouble() está em [0, 1), então o consumo base fica entre 70 e 130 m³/h.
        double consumo = CONSUMO_MINIMO + random.nextDouble() * VARIACAO_CONSUMO;
        // Sorteio de 10%: soma um pico de 40 a 100 m³/h (vazamento ou uso fora do normal).
        if (random.nextDouble() < CHANCE_DE_PICO) {
            consumo += PICO_MINIMO + random.nextDouble() * VARIACAO_PICO;
        }
        return getTipo().limitarAFaixa(consumo);
    }
}
