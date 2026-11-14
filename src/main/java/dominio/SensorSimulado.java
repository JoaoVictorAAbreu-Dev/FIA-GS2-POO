package dominio;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;

import java.util.Random;

/**
 * Base dos sensores simulados: é um componente do território (tem id, nome,
 * região e status) e cumpre o contrato {@link Sensor}.
 *
 * <p>Aqui ficam as regras comuns (falha e defeito simulados, status). Cada
 * subclasse define apenas como sua grandeza varia, em
 * {@link #simularLeitura(Random)}.</p>
 *
 * <p>Dois problemas podem ser simulados para demonstrar as exceções:</p>
 * <ul>
 *   <li>{@link #simularFalha()}: o sensor não responde e {@link #lerValor()}
 *       lança {@link FonteIndisponivelException};</li>
 *   <li>{@link #simularDefeitoDeCalibracao()}: o sensor responde, mas com um
 *       valor fisicamente impossível; a validação de {@link Medicao} o rejeita
 *       com {@link DadosInvalidosException} em {@link #lerMedicao}.</li>
 * </ul>
 */
public abstract class SensorSimulado extends ComponenteTerritorial implements Sensor {

    private final TipoSensor tipo;
    private final Random random;
    private boolean funcionando = true;
    private boolean descalibrado = false;

    /**
     * @param random gerador de números; injete um {@code new Random(semente)} nos
     *               testes para leituras reproduzíveis
     * @throws DadosInvalidosException se tipo ou random forem nulos
     */
    protected SensorSimulado(String id, String nome, String regiao, TipoSensor tipo, Random random) {
        super(id, nome, regiao);
        if (tipo == null) {
            throw new DadosInvalidosException("Informe o tipo do sensor " + id + ".");
        }
        if (random == null) {
            throw new DadosInvalidosException("Informe o gerador de números do sensor " + id + ".");
        }
        this.tipo = tipo;
        this.random = random;
    }

    /** Gera um valor realista para a grandeza, dentro da faixa física do tipo. */
    protected abstract double simularLeitura(Random random);

    /**
     * Lê o valor bruto do sensor, sem validar. Com defeito de calibração, o valor
     * fica abaixo do mínimo físico do tipo (ex.: chuva de -15 mm/h).
     */
    @Override
    public final double lerValor() throws FonteIndisponivelException {
        if (!funcionando) {
            throw new FonteIndisponivelException(getId(), "Sensor " + getId() + " (" + tipo.getDescricao()
                    + ", " + getRegiao() + ") não respondeu: está fora de operação.");
        }
        if (descalibrado) {
            return tipo.getMinimoFisico() - tipo.getDegrauEntreNiveis();
        }
        return simularLeitura(random);
    }

    @Override
    public boolean estaFuncionando() {
        return funcionando;
    }

    /**
     * INDISPONIVEL se não responde; DEGRADADO se responde com defeito de
     * calibração; OPERACIONAL caso contrário.
     */
    @Override
    public StatusComponente verificarStatus() {
        StatusComponente novo;
        if (!funcionando) {
            novo = StatusComponente.INDISPONIVEL;
        } else if (descalibrado) {
            novo = StatusComponente.DEGRADADO;
        } else {
            novo = StatusComponente.OPERACIONAL;
        }
        atualizarStatus(novo);
        return novo;
    }

    /** Simula pane (falta de energia, cabo rompido...). Próximas leituras falham. */
    public void simularFalha() {
        funcionando = false;
        verificarStatus();
    }

    /**
     * Simula sensor descalibrado: continua respondendo, mas com leituras
     * impossíveis, que a validação de {@link Medicao} rejeita.
     */
    public void simularDefeitoDeCalibracao() {
        descalibrado = true;
        verificarStatus();
    }

    /** Recoloca o sensor em operação normal, desfazendo falha e defeito simulados. */
    public void restabelecer() {
        funcionando = true;
        descalibrado = false;
        verificarStatus();
    }

    /** {@code true} se o sensor está com defeito de calibração simulado. */
    public boolean estaDescalibrado() {
        return descalibrado;
    }

    @Override
    public TipoSensor getTipo() {
        return tipo;
    }
}
