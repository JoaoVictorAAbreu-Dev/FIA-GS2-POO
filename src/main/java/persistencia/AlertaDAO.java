package persistencia;
import java.sql.*;
import dominio.Alerta;
import dominio.NivelAlerta;
import dominio.TipoSensor;

import java.time.LocalDateTime;
import java.util.List;

public class AlertaDAO extends BaseDAO {

    // Caso a tabela exista as informações são inseridas
    public void inserir(Alerta alerta) {
        String sql = "INSERT INTO alerta (id, sensorId, tipo, nivel, valorPrevisto, " +
                "limiteUltrapassado, estrategia, dataHora) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        executarUpdate(sql, stmt -> {
            stmt.setString(2, alerta.sensorId());
            stmt.setString(3, alerta.tipo().name());
            stmt.setString(4, alerta.nivel().name());
            stmt.setDouble(5, alerta.valorPrevisto());
            stmt.setDouble(6, alerta.limiteUltrapassado());
            stmt.setString(7, alerta.estrategia());
            stmt.setObject(8, alerta.dataHora());
        });
    }
    public List<Alerta> listarTudo() {
        String sql = "SELECT * FROM alerta";

        return executarQuery(sql, null, rs -> new Alerta(
                rs.getLong("id"),
                rs.getString("sensorId"),
                TipoSensor.valueOf(rs.getString("tipo")),
                NivelAlerta.valueOf(rs.getString("nivel")),
                rs.getDouble("valorPrevisto"),
                rs.getDouble("limiteUltrapassado"),
                rs.getString("estrategia"),
                rs.getObject("dataHora", LocalDateTime.class)
        ));
    }

    public List<Alerta> listarUltimo() {
        String sql = "SELECT * FROM alerta WHERE sensorId = ? ORDER BY momento DESC LIMIT ?";

        return executarQuery(sql, null, rs -> new Alerta(
                rs.getLong("id"),
                rs.getString("sensorId"),
                TipoSensor.valueOf(rs.getString("tipo")),
                NivelAlerta.valueOf(rs.getString("nivel")),
                rs.getDouble("valorPrevisto"),
                rs.getDouble("limiteUltrapassado"),
                rs.getString("estrategia"),
                rs.getObject("dataHora", LocalDateTime.class)
        ));
    }

    public void atualizar(Alerta alerta) {
        String sql = "UPDATE alerta SET sensorId = ?, tipo = ?, nivel = ?, valorPrevisto = ?, " +
                "limiteUltrapassado = ?, estrategia = ?, dataHora = ? WHERE id = ?";

        executarUpdate(sql, stmt -> {
            stmt.setString(2, alerta.sensorId());
            stmt.setString(3, alerta.tipo().name());
            stmt.setString(4, alerta.nivel().name());
            stmt.setDouble(5, alerta.valorPrevisto());
            stmt.setDouble(6, alerta.limiteUltrapassado());
            stmt.setString(7, alerta.estrategia());
            stmt.setObject(8, alerta.dataHora());
        });
    }

    public void deletar(Long id) {
        String sql = "DELETE FROM alerta WHERE id = ?";

        executarUpdate(sql, stmt -> stmt.setLong(1, id));
    }
}
