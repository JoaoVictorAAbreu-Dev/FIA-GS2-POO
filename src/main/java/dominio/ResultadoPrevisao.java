package dominio;

import excecao.DadosInvalidosException;

import java.util.OptionalDouble;

/**
 * Saída de uma {@link EstrategiaPrevisao}: o valor previsto (quando há dados
 * suficientes) e um texto que explica o que ele significa.
 *
 * @param estrategia     nome da estratégia que gerou o resultado
 * @param sensorId       sensor do histórico; {@code null} só se o histórico estava vazio
 * @param tipo           grandeza do histórico; {@code null} só se o histórico estava vazio
 * @param valorPrevisto  vazio quando o sistema decidiu não prever (dados insuficientes)
 * @param leiturasUsadas quantas leituras entraram no cálculo (ou estavam disponíveis)
 * @param interpretacao  explicação em linguagem simples para o menu
 */
public record ResultadoPrevisao(String estrategia, String sensorId, TipoSensor tipo,
                                OptionalDouble valorPrevisto, int leiturasUsadas, String interpretacao) {

    public ResultadoPrevisao {
        if (estrategia == null || estrategia.isBlank()) {
            throw new DadosInvalidosException("O resultado precisa informar a estratégia de previsão.");
        }
        if (valorPrevisto == null) {
            valorPrevisto = OptionalDouble.empty();
        }
        if (valorPrevisto.isPresent()) {
            if (tipo == null || sensorId == null) {
                throw new DadosInvalidosException("Uma previsão com valor precisa informar sensor e tipo.");
            }
            double valor = valorPrevisto.getAsDouble();
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                throw new DadosInvalidosException("A previsão de " + sensorId
                        + " gerou um valor não numérico; verifique o histórico.");
            }
        }
        if (leiturasUsadas < 0) {
            throw new DadosInvalidosException("Quantidade de leituras não pode ser negativa: " + leiturasUsadas);
        }
    }

    static ResultadoPrevisao comValor(String estrategia, String sensorId, TipoSensor tipo, double valor,
                                      int leiturasUsadas, String interpretacao) {
        return new ResultadoPrevisao(estrategia, sensorId, tipo, OptionalDouble.of(valor), leiturasUsadas,
                interpretacao);
    }

    static ResultadoPrevisao dadosInsuficientes(String estrategia, String sensorId, TipoSensor tipo,
                                                int disponiveis, int minimo) {
        String origem = sensorId == null ? "no histórico" : "no histórico de " + sensorId;
        String texto = "Dados insuficientes: " + disponiveis + " leitura(s) " + origem + "; a estratégia "
                + estrategia + " exige pelo menos " + minimo + ". Nenhuma previsão nem alerta foi emitido.";
        return new ResultadoPrevisao(estrategia, sensorId, tipo, OptionalDouble.empty(), disponiveis, texto);
    }

    /** {@code true} se há valor previsto (dados suficientes). */
    public boolean temPrevisao() {
        return valorPrevisto.isPresent();
    }
}
