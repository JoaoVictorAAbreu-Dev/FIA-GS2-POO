package dominio;

import excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Local físico do território (ex.: "Estação Córrego Aricanduva") que agrupa
 * sensores. Seu status é derivado dos sensores: todos respondendo =
 * OPERACIONAL; alguns = DEGRADADO; nenhum (ou nenhum instalado) = INDISPONIVEL.
 */
public class PontoMonitoramento extends ComponenteTerritorial {

    private final List<Sensor> sensores = new ArrayList<>();

    public PontoMonitoramento(String id, String nome, String regiao) {
        super(id, nome, regiao);
    }

    /** @throws DadosInvalidosException se já houver sensor com o mesmo id */
    public void adicionarSensor(Sensor sensor) {
        Objects.requireNonNull(sensor, "sensor");
        boolean idRepetido = sensores.stream().anyMatch(s -> s.getId().equals(sensor.getId()));
        if (idRepetido) {
            throw new DadosInvalidosException("O ponto " + getId() + " já possui um sensor com id "
                    + sensor.getId() + ".");
        }
        sensores.add(sensor);
    }

    @Override
    public StatusComponente verificarStatus() {
        long funcionando = sensores.stream().filter(Sensor::estaFuncionando).count();
        StatusComponente novo;
        if (funcionando == 0) {
            novo = StatusComponente.INDISPONIVEL;
        } else if (funcionando < sensores.size()) {
            novo = StatusComponente.DEGRADADO;
        } else {
            novo = StatusComponente.OPERACIONAL;
        }
        atualizarStatus(novo);
        return novo;
    }

    /**
     * Sempre atual: o status do ponto é derivado dos sensores, então é
     * recalculado a cada consulta. Assim um ponto recém-criado (sem sensores)
     * aparece como INDISPONIVEL, e a falha de um sensor aparece na hora.
     */
    @Override
    public StatusComponente getStatus() {
        return verificarStatus();
    }

    public List<Sensor> getSensores() {
        return Collections.unmodifiableList(sensores);
    }
}
