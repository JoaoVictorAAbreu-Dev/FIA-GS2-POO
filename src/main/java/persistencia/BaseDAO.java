package persistencia;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseDAO {

    /** Preenche os {@code ?} do comando (índices começam em 1). */
    @FunctionalInterface
    protected interface StatementPreparer {
        void prepare(PreparedStatement stmt) throws SQLException;
    }

    /** Converte a linha atual do {@link ResultSet} em um objeto do domínio. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private final FabricaConexao fabricaConexao;

    protected BaseDAO(FabricaConexao fabricaConexao) {
        if (fabricaConexao == null) {
            throw new DadosInvalidosException("O DAO precisa da fábrica de conexões.");
        }
        this.fabricaConexao = fabricaConexao;
    }

    /** Preenche os {@code ?} do comando (índices começam em 1). */
    protected long executarUpdate(String sql, StatementPreparer preparer) throws FonteIndisponivelException {
        try (Connection conn = fabricaConexao.abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparer.prepare(stmt);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } throw new FonteIndisponivelException(FabricaConexao.ID_FONTE, "O banco gravou o registro, mas não devolveu o id gerado.");
            }
        } catch (SQLException e) {
            throw falhaNoBanco("Gravar ", e);
        }
    }

    /**
     * Executa um SELECT e converte cada linha com o {@code mapper}.
     *
     * @throws FonteIndisponivelException se o banco falhar
     * @throws DadosInvalidosException    se alguma linha tiver dados inválidos
     */
    protected <T> List<T> consultar(String sql, StatementPreparer preparer, RowMapper<T> mapper)
            throws FonteIndisponivelException {
        List<T> resultados = new ArrayList<>();
        try (Connection conn = fabricaConexao.abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            preparer.prepare(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultados.add(mapearLinha(rs, mapper));
                }
            }
            return resultados;
        } catch (SQLException e) {
            throw falhaNoBanco("consultar", e);
        }
    }

    /** Para SELECT sem parâmetros. */
    protected static void semParametros(PreparedStatement stmt) {
        // Nada a preencher: o SQL não tem "?".
    }

    /**
     * Uma linha com tipo desconhecido ou valor fora da faixa não entra no
     * cálculo: o erro diz qual registro está corrompido.
     */
    private static <T> T mapearLinha(ResultSet rs, RowMapper<T> mapper) throws SQLException {
        try {
            return mapper.map(rs);
        } catch (IllegalArgumentException | DadosInvalidosException e) {
            throw new DadosInvalidosException("Registro inválido no banco (id " + rs.getLong("id") + "): "
                    + e.getMessage(), e);
        }
    }

    private static FonteIndisponivelException falhaNoBanco(String operacao, SQLException e) {
        return new FonteIndisponivelException(FabricaConexao.ID_FONTE,
                "Erro ao " + operacao + " no banco de dados: " + e.getMessage(), e);
    }
}
