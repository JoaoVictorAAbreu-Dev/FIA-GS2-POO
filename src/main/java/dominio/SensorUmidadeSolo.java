package dominio;

import java.util.Random;

/**
 * Sonda de umidade do solo numa encosta ou área de várzea: normalmente entre
 * 35% e 75%. Em 15% das leituras o solo está encharcado e a umidade sobe mais
 * 15 a 30 pontos (limitada a 100%).
 */
public class SensorUmidadeSolo extends SensorSimulado {

    private static final double UMIDADE_MINIMA = 35.0;
    private static final double VARIACAO_UMIDADE = 40.0;
    private static final double CHANCE_DE_ENCHARCAR = 0.15;
    private static final double ACRESCIMO_MINIMO = 15.0;
    private static final double VARIACAO_ACRESCIMO = 15.0;

    /** Uso normal: leituras aleatórias a cada execução. */
    public SensorUmidadeSolo(String id, String nome, String regiao) {
        this(id, nome, regiao, new Random());
    }

    /** Para testes: com {@code new Random(semente)} as leituras se repetem. */
    public SensorUmidadeSolo(String id, String nome, String regiao, Random random) {
        super(id, nome, regiao, TipoSensor.UMIDADE_SOLO, random);
    }

    @Override
    protected double simularLeitura(Random random) {
        // nextDouble() está em [0, 1), então a umidade base fica entre 35% e 75%.
        double umidade = UMIDADE_MINIMA + random.nextDouble() * VARIACAO_UMIDADE;
        // Sorteio de 15%: solo encharcado, mais 15 a 30 pontos.
        if (random.nextDouble() < CHANCE_DE_ENCHARCAR) {
            umidade += ACRESCIMO_MINIMO + random.nextDouble() * VARIACAO_ACRESCIMO;
        }
        // Umidade não passa de 100%.
        return getTipo().limitarAFaixa(umidade);
    }
}
