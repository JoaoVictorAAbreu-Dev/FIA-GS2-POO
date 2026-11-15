package persistencia;
import servico.FabricaConexao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseDAO {

    @FunctionalInterface
    protected interface StatementPreparer {
        void prepare(PreparedStatement stmt) throws SQLException;
    }

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected int executarUpdate(String sql, StatementPreparer preparer) {
        try (Connection conn = FabricaConexao.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            preparer.prepare(stmt);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excecutar a atualização no banco de dados. ERRO: ", e);
        }
    }

    protected <T> List<T> executarQuery(String sql, StatementPreparer preparer, RowMapper<T> mapper) {
        List<T> resultados = new ArrayList<>();

        try (Connection conn = FabricaConexao.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (preparer != null) {
                preparer.prepare(stmt);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultados.add(mapper.map(rs));
                }
            }
            return resultados;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excecutar consulta no banco. ERRO: ", e);
        }
    }
}
