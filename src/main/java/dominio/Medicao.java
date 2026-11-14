package dominio;

import excecao.DadosInvalidosException;

import java.time.LocalDateTime;

/**
 * Uma leitura de sensor. Imutável: depois de criada e validada, não muda.
 *
 * <p>Toda criação passa pelo construtor compacto, então não existe
 * {@code Medicao} inválida em memória, venha ela do sensor, do banco ou de um
 * teste.</p>
 *
 * <p>Para a persistência: use {@link #nova} antes de gravar (id nulo) e
 * {@link #comId} ou o construtor canônico ao ler do banco.</p>
 *
 * @param id       chave no banco; {@code null} enquanto não foi gravada
 * @param sensorId identificador do sensor que gerou a leitura (ex.: "PLU-01")
 * @param tipo     grandeza medida; define unidade e faixa válida
 * @param valor    valor lido, na unidade de {@code tipo}
 * @param dataHora momento da leitura
 */
public record Medicao(Long id, String sensorId, TipoSensor tipo, double valor, LocalDateTime dataHora) {

    // Construtor compacto: valida os campos antes de o record ser criado.
    public Medicao {
        if (id != null && id <= 0) {
            throw new DadosInvalidosException("O id da medição deve ser positivo; recebido: " + id + ".");
        }
        if (sensorId == null || sensorId.isBlank()) {
            throw new DadosInvalidosException("A medição precisa identificar o sensor de origem.");
        }
        if (tipo == null) {
            throw new DadosInvalidosException("A medição do sensor " + sensorId + " precisa informar o tipo.");
        }
        if (dataHora == null) {
            throw new DadosInvalidosException("A medição do sensor " + sensorId + " precisa informar data e hora.");
        }
        // A faixa física válida depende da grandeza, por isso a regra fica em TipoSensor.
        tipo.validarValor(valor);
        // Normaliza o id (" PLU-01 " vira "PLU-01") antes de atribuir ao campo.
        sensorId = sensorId.strip();
    }

    /** Medição ainda não persistida (id nulo). */
    public static Medicao nova(String sensorId, TipoSensor tipo, double valor, LocalDateTime dataHora) {
        return new Medicao(null, sensorId, tipo, valor, dataHora);
    }

    /** Cópia com o id gerado pelo banco. */
    public Medicao comId(Long novoId) {
        return new Medicao(novoId, sensorId, tipo, valor, dataHora);
    }

    /** Valor com unidade, ex.: {@code "12,5 mm/h"}. */
    public String valorFormatado() {
        return tipo.formatar(valor);
    }
}
