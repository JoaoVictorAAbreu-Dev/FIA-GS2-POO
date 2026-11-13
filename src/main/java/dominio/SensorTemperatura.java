package dominio;

import java.util.Random;

/** Termômetro de rua: valores em torno de 27 °C com variação normal (desvio de 6 °C). */
public class SensorTemperatura extends SensorSimulado {

    private static final double MEDIA = 27.0;
    private static final double DESVIO = 6.0;

    public SensorTemperatura(String id, String nome, String regiao) {
        this(id, nome, regiao, new Random());
    }

    public SensorTemperatura(String id, String nome, String regiao, Random random) {
        super(id, nome, regiao, TipoSensor.TEMPERATURA, random);
    }

    @Override
    protected double simularLeitura(Random random) {
        return getTipo().limitarAFaixa(MEDIA + random.nextGaussian() * DESVIO);
    }
}
