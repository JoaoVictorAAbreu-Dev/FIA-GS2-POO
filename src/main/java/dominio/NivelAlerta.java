package dominio;

import java.util.Optional;

/**
 * Níveis de alerta, do mais leve ao mais grave.
 *
 * <p>O limite de cada nível é {@code limiar + ordem × degrau do tipo}. Com
 * chuva (limiar 30 mm/h, degrau 15): ATENÇÃO a partir de 30, ALERTA a partir
 * de 45 e CRÍTICO a partir de 60 mm/h. O limiar vem de {@link DadosTerritorio};
 * o degrau, de {@link TipoSensor}.</p>
 */
public enum NivelAlerta {

    ATENCAO("ATENÇÃO", 0,
            "Acompanhar as próximas leituras e revisar o plano de contingência."),
    ALERTA("ALERTA", 1,
            "Avisar a coordenação local e deixar equipes e recursos de resposta de prontidão."),
    CRITICO("CRÍTICO", 2,
            "Acionar o plano de contingência e avisar a população da região e o órgão responsável.");

    private final String rotulo;
    private final int ordem;
    private final String orientacao;

    NivelAlerta(String rotulo, int ordem, String orientacao) {
        this.rotulo = rotulo;
        this.ordem = ordem;
        this.orientacao = orientacao;
    }

    /** Valor a partir do qual este nível é disparado. */
    public double limite(double limiar, TipoSensor tipo) {
        return limiar + ordem * tipo.getDegrauEntreNiveis();
    }

    /**
     * Nível mais grave cujo limite foi atingido pelo valor previsto, ou vazio se
     * a previsão está abaixo do limiar.
     *
     * @throws excecao.DadosInvalidosException se o limiar for inválido para o tipo
     */
    public static Optional<NivelAlerta> classificar(double valorPrevisto, double limiar, TipoSensor tipo) {
        tipo.validarLimiar(limiar);
        NivelAlerta[] niveis = values();
        for (int i = niveis.length - 1; i >= 0; i--) {
            if (valorPrevisto >= niveis[i].limite(limiar, tipo)) {
                return Optional.of(niveis[i]);
            }
        }
        return Optional.empty();
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getOrientacao() {
        return orientacao;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
