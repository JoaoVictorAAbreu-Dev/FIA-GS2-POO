package dominio;

import excecao.DadosInvalidosException;

/**
 * Grandezas medidas no território. Cada tipo concentra sua unidade, a faixa
 * física aceita e o degrau entre níveis de alerta, para que a validação de uma
 * medição exista em um único lugar.
 *
 * <p>As faixas são limites de plausibilidade (não de risco): um valor fora
 * delas indica sensor defeituoso ou dado corrompido e é rejeitado com
 * {@link DadosInvalidosException}. Os limites de risco ficam em
 * {@link DadosTerritorio} (limiar) e {@link NivelAlerta} (níveis).</p>
 */
public enum TipoSensor {

    /** Temperatura do ar. Degrau de 3 °C: limiar 35 → ATENÇÃO 35, ALERTA 38, CRÍTICO 41. */
    TEMPERATURA("temperatura", "°C", -20.0, 60.0, 3.0),

    /** Intensidade de chuva. Degrau de 15 mm/h: limiar 30 → 30, 45, 60. */
    PRECIPITACAO("chuva", "mm/h", 0.0, 300.0, 15.0),

    /** Vazão consumida por um setor de abastecimento. Degrau de 30 m³/h: limiar 120 → 120, 150, 180. */
    CONSUMO_AGUA("consumo de água", "m³/h", 0.0, 1000.0, 30.0);

    private final String descricao;
    private final String unidade;
    private final double minimoFisico;
    private final double maximoFisico;
    private final double degrauEntreNiveis;

    TipoSensor(String descricao, String unidade, double minimoFisico, double maximoFisico,
               double degrauEntreNiveis) {
        this.descricao = descricao;
        this.unidade = unidade;
        this.minimoFisico = minimoFisico;
        this.maximoFisico = maximoFisico;
        this.degrauEntreNiveis = degrauEntreNiveis;
    }

    /**
     * Garante que o valor é um número finito dentro da faixa física do tipo.
     *
     * @throws DadosInvalidosException com mensagem que cita a grandeza, o valor e a faixa
     */
    public void validarValor(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new DadosInvalidosException(
                    "Medição de " + descricao + " inválida: o valor precisa ser um número finito; recebido: "
                            + Formatos.numero(valor) + ".");
        }
        if (valor < 0 && minimoFisico == 0) {
            throw new DadosInvalidosException(
                    "Medição de " + descricao + " negativa: " + formatar(valor) + ".");
        }
        if (valor < minimoFisico || valor > maximoFisico) {
            throw new DadosInvalidosException(
                    "Medição de " + descricao + " fora da faixa física (" + Formatos.numero(minimoFisico)
                            + " a " + formatar(maximoFisico) + "); recebido: " + formatar(valor) + ".");
        }
    }

    /**
     * Regra única para limiares de risco deste tipo: número finito, positivo e
     * dentro da faixa física (um limiar inalcançável desligaria os alertas).
     *
     * @throws DadosInvalidosException ex.: "O limiar de chuva deve ser positivo; valor recebido: -5,0"
     */
    public void validarLimiar(double limiar) {
        if (Double.isNaN(limiar) || Double.isInfinite(limiar)) {
            throw new DadosInvalidosException("O limiar de " + descricao
                    + " precisa ser um número finito; valor recebido: " + Formatos.numero(limiar));
        }
        if (limiar <= 0) {
            throw new DadosInvalidosException("O limiar de " + descricao
                    + " deve ser positivo; valor recebido: " + Formatos.numero(limiar));
        }
        if (limiar > maximoFisico) {
            throw new DadosInvalidosException("O limiar de " + descricao + " não pode passar de "
                    + formatar(maximoFisico) + "; valor recebido: " + Formatos.numero(limiar));
        }
    }

    /** Traz um valor calculado (ex.: uma extrapolação) para dentro da faixa física. */
    public double limitarAFaixa(double valor) {
        return Math.max(minimoFisico, Math.min(maximoFisico, valor));
    }

    /** Valor com unidade, em pt-BR: {@code "12,5 mm/h"}. */
    public String formatar(double valor) {
        return Formatos.numero(valor) + " " + unidade;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUnidade() {
        return unidade;
    }

    public double getMinimoFisico() {
        return minimoFisico;
    }

    public double getMaximoFisico() {
        return maximoFisico;
    }

    public double getDegrauEntreNiveis() {
        return degrauEntreNiveis;
    }
}
