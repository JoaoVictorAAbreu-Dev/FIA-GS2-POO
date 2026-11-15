package servico;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.EstrategiaPrevisao;
import dominio.Medicao;
import dominio.ResultadoPrevisao;
import dominio.Sensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import persistencia.AlertaDAO;
import persistencia.MedicaoDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Fluxo principal do sistema, entre o menu e o banco:
 * ler sensor → gravar medição → buscar histórico → prever → avaliar → gravar alerta.
 *
 * <p>Os DAOs são recebidos pelo construtor, então os testes usam versões
 * falsas (Mockito) e verificam a lógica de alerta sem banco.</p>
 */
public class ServicoMonitoramento {

    private final MedicaoDAO medicaoDAO;
    private final AlertaDAO alertaDAO;
    private final FabricaPrevisao fabricaPrevisao;
    private final DadosTerritorio territorio;
    private final Supplier<LocalDateTime> relogio;

    public ServicoMonitoramento(MedicaoDAO medicaoDAO, AlertaDAO alertaDAO, FabricaPrevisao fabricaPrevisao,
                                DadosTerritorio territorio) {
        this(medicaoDAO, alertaDAO, fabricaPrevisao, territorio, LocalDateTime::now);
    }

    /** @param relogio fonte da data/hora; os testes passam um horário fixo */
    public ServicoMonitoramento(MedicaoDAO medicaoDAO, AlertaDAO alertaDAO, FabricaPrevisao fabricaPrevisao,
                                DadosTerritorio territorio, Supplier<LocalDateTime> relogio) {
        if (medicaoDAO == null || alertaDAO == null || fabricaPrevisao == null || territorio == null
                || relogio == null) {
            throw new DadosInvalidosException(
                    "O serviço de monitoramento precisa dos DAOs, da fábrica de previsões, do território e do relógio.");
        }
        this.medicaoDAO = medicaoDAO;
        this.alertaDAO = alertaDAO;
        this.fabricaPrevisao = fabricaPrevisao;
        this.territorio = territorio;
        this.relogio = relogio;
    }

    /**
     * Lê o sensor e grava a medição. Se a leitura falhar ou for inválida, nada
     * é gravado.
     *
     * @return a medição gravada, com id
     * @throws FonteIndisponivelException se o sensor não responder ou o banco falhar
     * @throws DadosInvalidosException    se o valor lido for impossível (ex.: sensor descalibrado)
     */
    public Medicao registrarLeitura(Sensor sensor) throws FonteIndisponivelException {
        if (sensor == null) {
            throw new DadosInvalidosException("Informe o sensor a ser lido.");
        }
        Medicao medicao = sensor.lerMedicao(relogio.get());
        return medicaoDAO.inserir(medicao);
    }

    /**
     * Roda a estratégia sobre o histórico do banco e grava o alerta, se houver.
     *
     * <p>Busca só as leituras que a estratégia usa (as N mais recentes). Com
     * menos que isso, a estratégia "se cala": devolve resultado sem valor e
     * nenhum alerta é gravado.</p>
     *
     * @throws FonteIndisponivelException se o banco falhar
     */
    public AvaliacaoPrevisao preverEAvaliar(String sensorId, TipoPrevisao tipoPrevisao)
            throws FonteIndisponivelException {
        EstrategiaPrevisao estrategia = fabricaPrevisao.criar(tipoPrevisao);
        List<Medicao> historico = medicaoDAO.buscarUltimasPorSensor(sensorId, estrategia.getMinimoLeituras());
        ResultadoPrevisao previsao = estrategia.prever(historico);

        Optional<Alerta> alerta = Alerta.avaliar(previsao, territorio, relogio.get());
        if (alerta.isEmpty()) {
            return new AvaliacaoPrevisao(previsao, Optional.empty());
        }
        return new AvaliacaoPrevisao(previsao, Optional.of(alertaDAO.inserir(alerta.get())));
    }

    /** Todas as medições gravadas do sensor, da mais antiga para a mais recente. */
    public List<Medicao> historicoDoSensor(String sensorId) throws FonteIndisponivelException {
        return medicaoDAO.buscarHistoricoPorSensor(sensorId);
    }

    /** Alertas já emitidos, do mais recente para o mais antigo. */
    public List<Alerta> alertasEmitidos() throws FonteIndisponivelException {
        return alertaDAO.listarTodos();
    }

    /** Os {@code limite} alertas mais recentes, do mais recente para o mais antigo. */
    public List<Alerta> ultimosAlertas(int limite) throws FonteIndisponivelException {
        return alertaDAO.listarUltimosAlertas(limite);
    }
}
