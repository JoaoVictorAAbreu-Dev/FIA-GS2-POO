package dominio;

import excecao.DadosInvalidosException;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Alerta emitido quando uma previsão atinge um limite de risco. Imutável.
 *
 * <p>Guarda tudo o que explica o alerta: qual sensor, valor previsto, limite
 * atingido e estratégia usada. Para persistir: {@link #explicacao()} pode ser
 * gravada como texto, mas é recalculável a partir dos campos.</p>
 *
 * @param id                 chave no banco; {@code null} enquanto não foi gravado
 * @param limiteUltrapassado limite do {@code nivel} que a previsão atingiu
 */
public record Alerta(Long id, String sensorId, TipoSensor tipo, NivelAlerta nivel, double valorPrevisto,
                     double limiteUltrapassado, String estrategia, LocalDateTime dataHora) {

    public Alerta {
        if (id != null && id <= 0) {
            throw new DadosInvalidosException("O id do alerta deve ser positivo; recebido: " + id + ".");
        }
        if (sensorId == null || sensorId.isBlank() || tipo == null || nivel == null || dataHora == null
                || estrategia == null || estrategia.isBlank()) {
            throw new DadosInvalidosException(
                    "O alerta precisa de sensor, tipo, nível, estratégia e data/hora.");
        }
        if (!Double.isFinite(valorPrevisto) || !Double.isFinite(limiteUltrapassado)) {
            throw new DadosInvalidosException("O alerta do sensor " + sensorId
                    + " tem valor previsto ou limite não numérico.");
        }
        if (valorPrevisto < limiteUltrapassado) {
            throw new DadosInvalidosException("Alerta incoerente para " + sensorId + ": a previsão ("
                    + tipo.formatar(valorPrevisto) + ") não atinge o limite (" + tipo.formatar(limiteUltrapassado)
                    + ").");
        }
        sensorId = sensorId.strip();
    }

    /**
     * Compara a previsão com o limiar do território e cria o alerta do nível
     * mais grave atingido.
     *
     * @param limiar limiar de ATENÇÃO do tipo (ex.: {@code dadosTerritorio.getLimiar(tipo)})
     * @return vazio se não há previsão (dados insuficientes) ou se ela fica abaixo do limiar
     */
    public static Optional<Alerta> avaliar(ResultadoPrevisao previsao, double limiar, LocalDateTime dataHora) {
        if (previsao == null || !previsao.temPrevisao()) {
            return Optional.empty();
        }
        double valor = previsao.valorPrevisto().getAsDouble();
        TipoSensor tipo = previsao.tipo();
        return NivelAlerta.classificar(valor, limiar, tipo)
                .map(nivel -> new Alerta(null, previsao.sensorId(), tipo, nivel, valor,
                        nivel.limite(limiar, tipo), previsao.estrategia(), dataHora));
    }

    /** Cópia com o id gerado pelo banco. */
    public Alerta comId(Long novoId) {
        return new Alerta(novoId, sensorId, tipo, nivel, valorPrevisto, limiteUltrapassado, estrategia, dataHora);
    }

    /**
     * Explicação para o usuário, ex.: "[ALERTA] Sensor PLU-01 (chuva): a previsão
     * de 48,0 mm/h (Média móvel) atingiu o limite de 45,0 mm/h do nível ALERTA.
     * O que fazer: ..."
     */
    public String explicacao() {
        return "[" + nivel.getRotulo() + "] Sensor " + sensorId + " (" + tipo.getDescricao() + "): a previsão de "
                + tipo.formatar(valorPrevisto) + " (" + estrategia + ") atingiu o limite de "
                + tipo.formatar(limiteUltrapassado) + " do nível " + nivel.getRotulo() + ". O que fazer: "
                + nivel.getOrientacao();
    }
}
