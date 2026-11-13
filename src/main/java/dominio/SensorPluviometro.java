package dominio;

import java.util.Random;

/**
 * Pluviômetro: na maior parte do tempo não chove (60% das leituras = 0); quando
 * chove, a intensidade segue uma distribuição exponencial (média 15 mm/h), o
 * que gera muitas chuvas fracas e algumas fortes.
 */
public class SensorPluviometro extends SensorSimulado {

    private static final double CHANCE_DE_CHUVA = 0.4;
    private static final double INTENSIDADE_MEDIA = 15.0;

    public SensorPluviometro(String id, String nome, String regiao) {
        this(id, nome, regiao, new Random());
    }

    public SensorPluviometro(String id, String nome, String regiao, Random random) {
        super(id, nome, regiao, TipoSensor.PRECIPITACAO, random);
    }

    @Override
    protected double simularLeitura(Random random) {
        if (random.nextDouble() >= CHANCE_DE_CHUVA) {
            return 0.0;
        }
        double intensidade = -Math.log(1.0 - random.nextDouble()) * INTENSIDADE_MEDIA;
        return getTipo().limitarAFaixa(intensidade);
    }
}
