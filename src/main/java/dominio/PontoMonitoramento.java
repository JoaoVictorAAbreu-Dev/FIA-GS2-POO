package dominio;

import excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Local físico do território (ex.: "Estação Córrego Aricanduva") que agrupa
 * sensores. Seu status é derivado dos sensores: nenhum respondendo (ou nenhum
 * instalado) = INDISPONIVEL; algum sem resposta ou com leituras impossíveis =
 * DEGRADADO; todos confiáveis = OPERACIONAL.
 */
public class PontoMonitoramento extends ComponenteTerritorial {

    private final List<Sensor> sensores = new ArrayList<>();

    public PontoMonitoramento(String id, String nome, String regiao) {
        super(id, nome, regiao);
    }

    /** @throws DadosInvalidosException se já houver sensor com o mesmo id */
    public void adicionarSensor(Sensor sensor) {
        if (sensor == null) {
            throw new DadosInvalidosException("Informe o sensor a ser instalado no ponto " + getId() + ".");
        }
        // Ids repetidos no mesmo ponto tornariam ambíguas as leituras e o histórico.
        boolean idRepetido = sensores.stream().anyMatch(s -> s.getId().equals(sensor.getId()));
        if (idRepetido) {
            throw new DadosInvalidosException("O ponto " + getId() + " já possui um sensor com id "
                    + sensor.getId() + ".");
        }
        sensores.add(sensor);
    }

    @Override
    public StatusComponente verificarStatus() {
        long respondendo = sensores.stream().filter(Sensor::estaFuncionando).count();
        long confiaveis = sensores.stream().filter(Sensor::temLeiturasConfiaveis).count();
        StatusComponente novo;
        // respondendo == 0 cobre também o ponto sem nenhum sensor instalado.
        if (respondendo == 0) {
            novo = StatusComponente.INDISPONIVEL;
        } else if (confiaveis < sensores.size()) {
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

    /** Visão somente leitura: sensores só entram pelo {@link #adicionarSensor}, que valida. */
    public List<Sensor> getSensores() {
        return Collections.unmodifiableList(sensores);
    }
}
