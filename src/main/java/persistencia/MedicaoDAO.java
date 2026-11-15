package persistencia;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import dominio.Medicao;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import org.h2.store.fs.retry.FilePathRetryOnInterrupt;

public class MedicaoDAO extends BaseDAO {

    private static final String SQL_INSERIR = "INSERT INTO medicao(sensorId, tipo, valor, dataHora VALUES (?, ?, ?, ?)";
    private static final String SQL_HISTORICO = "SELECT id, sensorId, tipo, valor, dataHora WHERE sensorId = ? ORDER BY dataHora, id";
    private static final String SQL_ULTIMAS = "SELECT id, sensorId, tipo, valor, dataHora WHERE sensorId = ? ORDER BY dataHora DESC, id DESC LIMIT ?";

    public MedicaoDAO(FabricaConexao fabricaConexao) {
        super(fabricaConexao);
    }

    /**
     * Grava uma medição nova (id nulo) e devolve a cópia com o id gerado.
     *
     * @throws DadosInvalidosException    se a medição for nula ou já tiver id
     * @throws FonteIndisponivelException se o banco falhar
     */
    public Medicao inserir(Medicao medicao) throws FonteIndisponivelException {
        if (medicao == null) {
            throw new DadosInvalidosException("Informe a medicao a ser gravado.");
        }
        if (medicao.id() != null) {
            throw new DadosInvalidosException("A medicao " + medicao.id() + " já está gravado no banco.");
        }

        Long idGerado = executarUpdate(SQL_INSERIR, stmt -> {
            stmt.setString(1, medicao.sensorId());
            stmt.setString(2, medicao.tipo().name());
            stmt.setDouble(3, medicao.valor());
            stmt.setObject(4, medicao.dataHora());
        });

        return medicao.comId(idGerado);
    }

    /** Todas as medições do sensor **/
    public List<Medicao> buscarHistoricoPorSensor(String sensorId) throws FonteIndisponivelException{
        String id = exigirSensor(sensorId);
        return consultar(SQL_HISTORICO, stmt -> stmt.setString(1, id), MedicaoDAO::mapear);
    }

    /**
     * As {@code limite} medições mais recentes do sensor, em ordem cronológica
     * (da mais antiga para a mais recente). É o histórico usado nas previsões.
     */
    public List<Medicao> buscarUltimasPorSensor(String sensorId, int limite) throws FonteIndisponivelException {
        String id = exigirSensor(sensorId);
        if (limite < 1) {
            throw new DadosInvalidosException("O limite deve ser positivo: " + limite);
        }

        List<Medicao> maisRecentesPrimeiro = consultar(SQL_ULTIMAS, stmt -> { stmt.setString(1, id); stmt.setInt(2, limite); }, MedicaoDAO::mapear );
        Collections.reverse(maisRecentesPrimeiro);
        return maisRecentesPrimeiro;
    }

    private static String exigirSensor(String sensorId) {
        if (sensorId == null || sensorId.isBlank()) {
            throw new DadosInvalidosException("Informe o sensor para consultar o histórico.");
        }
        return sensorId.strip();
    }

    // O construtor do record valida a linha: dado corrompido não entra no cálculo.
    private static Medicao mapear(ResultSet rs) throws SQLException {
        return new Medicao(
                rs.getLong("id"),
                rs.getString("sensorId"),
                TipoSensor.valueOf(rs.getString("tipo")),
                rs.getDouble("valor"),
                rs.getObject("dataHora", LocalDateTime.class));
    }
}
