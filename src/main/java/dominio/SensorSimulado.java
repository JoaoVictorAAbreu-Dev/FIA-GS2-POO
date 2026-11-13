package dominio;

import excecao.FonteIndisponivelException;

import java.util.Objects;
import java.util.Random;

/**
 * Base dos sensores simulados: é um componente do território (tem id, nome,
 * região e status) e cumpre o contrato {@link Sensor}.
 *
 * <p>Aqui ficam as regras comuns (falha simulada, status). Cada subclasse
 * define apenas como sua grandeza varia, em {@link #simularLeitura(Random)}.</p>
 */
public abstract class SensorSimulado extends ComponenteTerritorial implements Sensor {

    private final TipoSensor tipo;
    private final Random random;
    private boolean funcionando = true;

    /**
     * @param random gerador de números; injete um {@code new Random(semente)} nos
     *               testes para leituras reproduzíveis
     */
    protected SensorSimulado(String id, String nome, String regiao, TipoSensor tipo, Random random) {
        super(id, nome, regiao);
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.random = Objects.requireNonNull(random, "random");
    }

    /** Gera um valor realista para a grandeza, dentro da faixa física do tipo. */
    protected abstract double simularLeitura(Random random);

    @Override
    public final double lerValor() throws FonteIndisponivelException {
        if (!funcionando) {
            throw new FonteIndisponivelException(getId(), "Sensor " + getId() + " (" + tipo.getDescricao()
                    + ", " + getRegiao() + ") não respondeu: está fora de operação.");
        }
        return simularLeitura(random);
    }

    @Override
    public boolean estaFuncionando() {
        return funcionando;
    }

    @Override
    public StatusComponente verificarStatus() {
        atualizarStatus(funcionando ? StatusComponente.OPERACIONAL : StatusComponente.INDISPONIVEL);
        return getStatus();
    }

    /** Simula pane (falta de energia, cabo rompido...). Próximas leituras falham. */
    public void simularFalha() {
        funcionando = false;
        verificarStatus();
    }

    /** Recoloca o sensor em operação após uma falha simulada. */
    public void restabelecer() {
        funcionando = true;
        verificarStatus();
    }

    @Override
    public TipoSensor getTipo() {
        return tipo;
    }
}
