package dominio;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;

import java.util.Arrays;

/**
 * Sensor de cenário: devolve valores definidos de antemão, um por leitura, na
 * ordem dada. Usado na simulação de 24 horas para que o dia seja sempre o mesmo
 * (reproduzível na apresentação e nos testes).
 *
 * <p>Os valores não são validados aqui: cada leitura passa pela validação de
 * {@link Medicao}, como a de qualquer outro sensor.</p>
 */
public class SensorRoteirizado implements Sensor {

    private final String id;
    private final TipoSensor tipo;
    private final double[] roteiro;
    private int proximaLeitura;

    public SensorRoteirizado(String id, TipoSensor tipo, double... roteiro) {
        this.id = Validacoes.exigirTexto(id, "id");
        if (tipo == null) {
            throw new DadosInvalidosException("Informe o tipo do sensor " + id + ".");
        }
        if (roteiro == null || roteiro.length == 0) {
            throw new DadosInvalidosException("O roteiro do sensor " + id + " precisa de pelo menos uma leitura.");
        }
        this.tipo = tipo;
        // Cópia: alterar o array original depois não muda o roteiro.
        this.roteiro = Arrays.copyOf(roteiro, roteiro.length);
    }

    /** @throws FonteIndisponivelException quando o roteiro já foi todo lido */
    @Override
    public double lerValor() throws FonteIndisponivelException {
        if (!estaFuncionando()) {
            throw new FonteIndisponivelException(id, "O roteiro do sensor " + id + " terminou após "
                    + roteiro.length + " leituras.");
        }
        return roteiro[proximaLeitura++];
    }

    /** Funciona enquanto houver leituras no roteiro. */
    @Override
    public boolean estaFuncionando() {
        return proximaLeitura < roteiro.length;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public TipoSensor getTipo() {
        return tipo;
    }
}
