package servico;

import dominio.Alerta;
import dominio.ResultadoPrevisao;
import excecao.DadosInvalidosException;

import java.util.Optional;

/**
 * Resultado de rodar uma previsão para um sensor: a previsão em si e, se ela
 * atingiu o limiar, o alerta já gravado no banco (com id).
 */
public record AvaliacaoPrevisao(ResultadoPrevisao previsao, Optional<Alerta> alerta) {

    public AvaliacaoPrevisao {
        if (previsao == null) {
            throw new DadosInvalidosException("A avaliação precisa do resultado da previsão.");
        }
        if (alerta == null) {
            alerta = Optional.empty();
        }
    }

    public boolean gerouAlerta() {
        return alerta.isPresent();
    }
}
