package dominio;

import java.util.Random;

/**
 * Sensor de nível do córrego (régua ou sensor ultrassônico na margem): em dias
 * normais a água fica perto de 1,2 m, com variação normal (desvio de 0,3 m).
 * Em 10% das leituras, uma cheia acrescenta de 0,8 a 2,0 m.
 */
public class SensorNivelCorrego extends SensorSimulado {

    private static final double NIVEL_MEDIO = 1.2;
    private static final double DESVIO = 0.3;
    private static final double CHANCE_DE_CHEIA = 0.1;
    private static final double CHEIA_MINIMA = 0.8;
    private static final double VARIACAO_CHEIA = 1.2;

    /** Uso normal: leituras aleatórias a cada execução. */
    public SensorNivelCorrego(String id, String nome, String regiao) {
        this(id, nome, regiao, new Random());
    }

    /** Para testes: com {@code new Random(semente)} as leituras se repetem. */
    public SensorNivelCorrego(String id, String nome, String regiao, Random random) {
        super(id, nome, regiao, TipoSensor.NIVEL_CORREGO, random);
    }

    @Override
    protected double simularLeitura(Random random) {
        // nextGaussian() tem média 0 e desvio 1; escala para média 1,2 m e desvio 0,3 m.
        double nivel = NIVEL_MEDIO + random.nextGaussian() * DESVIO;
        // Sorteio de 10%: a água sobe de 0,8 a 2,0 m (cheia).
        if (random.nextDouble() < CHANCE_DE_CHEIA) {
            nivel += CHEIA_MINIMA + random.nextDouble() * VARIACAO_CHEIA;
        }
        // Corta valores impossíveis (ex.: nível negativo numa cauda da distribuição normal).
        return getTipo().limitarAFaixa(nivel);
    }
}
