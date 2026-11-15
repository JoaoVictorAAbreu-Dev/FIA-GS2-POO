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

    /** Intensidade de chuva. Degrau de 15 mm/h: limiar 30 → ATENÇÃO 30, ALERTA 45, CRÍTICO 60. */
    PRECIPITACAO("chuva", "mm/h", 0.0, 300.0, 15.0),

    /**
     * Altura da água no córrego, medida a partir do fundo. Degrau de 0,5 m:
     * limiar 2,0 → 2,0, 2,5, 3,0 m (no exemplo, 3,0 m é onde a água passa da margem).
     */
    NIVEL_CORREGO("nível do córrego", "m", 0.0, 8.0, 0.5),

    /**
     * Umidade do solo de uma encosta ou área de várzea. Solo saturado não absorve
     * mais chuva, e a água escorre para o córrego. Degrau de 7 pontos: limiar 80 → 80, 87, 94 %.
     */
    UMIDADE_SOLO("umidade do solo", "%", 0.0, 100.0, 7.0);

    private final String descricao;          // nome usado nas mensagens ("chuva")
    private final String unidade;            // unidade exibida após o valor ("mm/h")
    private final double minimoFisico;       // menor valor plausível; abaixo disso é defeito
    private final double maximoFisico;       // maior valor plausível; acima disso é defeito
    private final double degrauEntreNiveis;  // distância entre ATENÇÃO, ALERTA e CRÍTICO

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
        // Mensagem mais clara para grandezas que não podem ser negativas (chuva, nível, umidade).
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
     * baixo o bastante para que o limite do nível mais grave
     * ({@code limiar + 2 × degrau}) ainda caiba na faixa física. Como as
     * previsões são limitadas à faixa física, um limiar maior tornaria os níveis
     * mais graves impossíveis de atingir.
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
        double maiorLimiar = getMaiorLimiarPermitido();
        if (limiar > maiorLimiar) {
            NivelAlerta maisGrave = NivelAlerta.maisGrave();
            throw new DadosInvalidosException("O limiar de " + descricao + " não pode passar de "
                    + formatar(maiorLimiar) + " (o nível " + maisGrave.getRotulo() + " precisa caber na faixa física, até "
                    + formatar(maximoFisico) + "); valor recebido: " + Formatos.numero(limiar));
        }
    }

    /** Maior limiar aceito: aquele cujo nível mais grave coincide com o máximo físico. */
    public double getMaiorLimiarPermitido() {
        return maximoFisico - NivelAlerta.maisGrave().getOrdem() * degrauEntreNiveis;
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
