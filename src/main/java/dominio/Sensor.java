package dominio;

import excecao.FonteIndisponivelException;

import java.time.LocalDateTime;

/** Contrato de qualquer fonte de leituras do território. */
public interface Sensor {

    String getId();

    /** Grandeza medida; define unidade e faixa válida. */
    TipoSensor getTipo();

    /**
     * Lê o valor atual, na unidade de {@link #getTipo()}.
     *
     * @throws FonteIndisponivelException se o sensor não responder
     */
    double lerValor() throws FonteIndisponivelException;

    /** Indica se o sensor responde (entrega algum valor, mesmo que errado). */
    boolean estaFuncionando();

    /**
     * Indica se as leituras do sensor são confiáveis. Por padrão, todo sensor
     * que responde é confiável; um sensor com defeito pode responder com
     * valores impossíveis e sobrescrever este método.
     */
    default boolean temLeiturasConfiaveis() {
        return estaFuncionando();
    }

    /**
     * Lê o sensor e devolve uma {@link Medicao} já validada (id nulo, pronta
     * para gravar).
     *
     * @throws FonteIndisponivelException             se o sensor não responder
     * @throws excecao.DadosInvalidosException se o valor lido for inválido
     */
    default Medicao lerMedicao(LocalDateTime dataHora) throws FonteIndisponivelException {
        return Medicao.nova(getId(), getTipo(), lerValor(), dataHora);
    }
}
