package persistencia;
import java.sql.*;
import dominio.Alerta;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


public class AlertaDAO extends BaseDAO {

    private static final String SQL_INSERIR =
            "INSERT INTO alerta (sensorId, tipo, nivel, valorPrevisto, limiteUltrapassado, estrategia, dataHora) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_LISTAR =
            "SELECT id, sensorId, tipo, nivel, valorPrevisto, limiteUltrapassado, estrategia, dataHora "
                    + "FROM alerta ORDER BY dataHora DESC, id DESC";

    private static final String SQL_ULTIMAS =
            "SELECT id, sensorId, tipo, nivel, valorPrevisto, limiteUltrapassado, estrategia, dataHora "
                + "FROM alerta ORDER BY dataHora DESC, id DESC LIMIT ?";

    public AlertaDAO(FabricaConexao fabricaConexao) {
        super(fabricaConexao);
    }

    /**
     * Grava um alerta novo (id nulo) e devolve a cópia com o id gerado.
     *
     * @throws DadosInvalidosException    se o alerta for nulo ou já tiver id
     * @throws FonteIndisponivelException se o banco falhar
     */
    public Alerta inserir(Alerta alerta) throws FonteIndisponivelException {

        if (alerta == null) {
            throw new DadosInvalidosException("Informe o alerta a ser gravado.");
        }
        if (alerta.id() != null) {
            throw new DadosInvalidosException("O alerta " + alerta.id() + " já está gravado no banco.");
        }

        Long idGerado = executarUpdate(SQL_INSERIR, stmt -> {
            stmt.setString(1, alerta.sensorId());
            stmt.setString(2, alerta.tipo().name());
            stmt.setString(3, alerta.nivel().name());
            stmt.setDouble(4, alerta.valorPrevisto());
            stmt.setDouble(5, alerta.limiteUltrapassado());
            stmt.setString(6, alerta.estrategia());
            stmt.setObject(7, alerta.dataHora());
        });

        return alerta.comId(idGerado);
    }

    /** Todas as medições do sensor **/
    public List<Alerta> listarTodos() throws FonteIndisponivelException {
        return consultar(SQL_LISTAR, BaseDAO::semParametros, AlertaDAO::mapear);
    }

    private static Alerta mapear(ResultSet rs) throws SQLException {
        return new Alerta(
                rs.getLong("id"),
                rs.getString("sensorId"),
                TipoSensor.valueOf(rs.getString("tipo")),
                NivelAlerta.valueOf(rs.getString("nivel")),
                rs.getDouble("valorPrevisto"),
                rs.getDouble("limiteUltrapassado"),
                rs.getString("estrategia"),
                rs.getObject("dataHora", LocalDateTime.class));
    }

    public List<Alerta> listarUltimosAlertas(int limite) throws FonteIndisponivelException {
        if (limite < 1) {
            throw new DadosInvalidosException("O limite deve ser positivo: " + limite);
        }
        List<Alerta> maisRecentesPrimeiro = consultar(SQL_ULTIMAS, stmt -> stmt.setInt(1, limite), AlertaDAO::mapear);

        Collections.reverse(maisRecentesPrimeiro);
        return maisRecentesPrimeiro;
    }
}
