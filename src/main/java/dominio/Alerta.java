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

    /** Margem para erros de arredondamento de ponto flutuante ao recalcular limites. */
    private static final double TOLERANCIA = 1e-9;

    // Construtor compacto: toda criação de Alerta (avaliar, comId, leitura do banco) passa por aqui,
    // então não existe alerta incoerente em memória.
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
        // Um alerta só faz sentido se a previsão de fato chegou ao limite registrado.
        if (valorPrevisto < limiteUltrapassado) {
            throw new DadosInvalidosException("Alerta incoerente para " + sensorId + ": a previsão ("
                    + tipo.formatar(valorPrevisto) + ") não atinge o limite (" + tipo.formatar(limiteUltrapassado)
                    + ").");
        }
        // O limite do nível é limiar + ordem × degrau; o limiar de ATENÇÃO implícito precisa ser positivo.
        double limiarImplicito = limiteUltrapassado - nivel.getOrdem() * tipo.getDegrauEntreNiveis();
        if (limiarImplicito <= 0) {
            throw new DadosInvalidosException("Alerta incoerente para " + sensorId + ": o limite de "
                    + tipo.formatar(limiteUltrapassado) + " é baixo demais para o nível " + nivel.getRotulo() + ".");
        }
        // O nível registrado precisa ser o mais grave atingido: se a previsão já passa do limite
        // do próximo nível, o alerta foi classificado abaixo do que deveria.
        Optional<NivelAlerta> proximo = nivel.proximoNivel();
        double limiteDoProximo = limiteUltrapassado + tipo.getDegrauEntreNiveis();
        if (proximo.isPresent() && valorPrevisto >= limiteDoProximo + TOLERANCIA) {
            throw new DadosInvalidosException("Alerta incoerente para " + sensorId + ": a previsão de "
                    + tipo.formatar(valorPrevisto) + " já atinge o nível " + proximo.get().getRotulo() + " ("
                    + tipo.formatar(limiteDoProximo) + "), não " + nivel.getRotulo() + ".");
        }
        sensorId = sensorId.strip();
    }

    /**
     * Compara a previsão com o limiar do território <b>para o tipo da própria
     * previsão</b> e cria o alerta do nível mais grave atingido. Buscar o limiar
     * aqui dentro impede usar, por engano, o limiar de outra grandeza.
     *
     * @return vazio se não há previsão (dados insuficientes) ou se ela fica abaixo do limiar
     * @throws DadosInvalidosException se o território não for informado
     */
    public static Optional<Alerta> avaliar(ResultadoPrevisao previsao, DadosTerritorio territorio,
                                           LocalDateTime dataHora) {
        if (territorio == null) {
            throw new DadosInvalidosException("Informe o território para avaliar a previsão.");
        }
        if (previsao == null || !previsao.temPrevisao()) {
            return Optional.empty();
        }
        double valor = previsao.valorPrevisto().getAsDouble();
        TipoSensor tipo = previsao.tipo();
        double limiar = territorio.getLimiar(tipo);
        // Abaixo do limiar, classificar devolve vazio e nenhum alerta é criado.
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
