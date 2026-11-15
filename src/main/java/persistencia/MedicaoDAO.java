package persistencia;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import dominio.Medicao;
import dominio.TipoSensor;

public class MedicaoDAO extends BaseDAO {
    // Caso a tabela exista as informações são inseridas
    public void inserir(Medicao medicao) {
        String sql = "INSERT INTO medicao (id, sensorId, tipo, nivel, valorPrevisto, " +
                "limiteUltrapassado, estrategia, dataHora) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        executarUpdate(sql, stmt -> {
            stmt.setString(2, medicao.sensorId());
            stmt.setString(3, medicao.tipo().name());
            stmt.setDouble(4, medicao.valor());
            stmt.setObject(5, medicao.dataHora());
        });
    }
    public List<Medicao> listarTudo() {
        String sql = "SELECT * FROM medicao";

        return executarQuery(sql, null, rs -> new Medicao(
                rs.getLong("id"),
                rs.getString("sensorId"),
                TipoSensor.valueOf(rs.getString("tipo")),
                rs.getDouble("valor"),
                rs.getObject("dataHora", LocalDateTime.class)
        ));
    }

    public List<Medicao> listarUltimo() {
        String sql = "SELECT * FROM medicao WHERE sensorId = ? ORDER BY momento DESC LIMIT ?";

        return executarQuery(sql, null, rs -> new Medicao(
                rs.getLong("id"),
                rs.getString("sensorId"),
                TipoSensor.valueOf(rs.getString("tipo")),
                rs.getDouble("valor"),
                rs.getObject("dataHora", LocalDateTime.class)
        ));
    }

    public void atualizar(Medicao medicao) {
        String sql = "UPDATE medicao SET sensorId = ?, tipo = ?, nivel = ?, valorPrevisto = ?, " +
                "limiteUltrapassado = ?, estrategia = ?, dataHora = ? WHERE id = ?";

        executarUpdate(sql, stmt -> {
            stmt.setString(2, medicao.sensorId());
            stmt.setString(3, medicao.tipo().name());
            stmt.setDouble(4, medicao.valor());
            stmt.setObject(8, medicao.dataHora());
        });
    }

    public void deletar(Long id) {
        String sql = "DELETE FROM medicao WHERE id = ?";

        executarUpdate(sql, stmt -> stmt.setLong(1, id));
    }
}
