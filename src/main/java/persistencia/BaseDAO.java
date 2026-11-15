package persistencia;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Código JDBC comum aos DAOs: abrir conexão, preparar o comando, executar e
 * fechar tudo com try-with-resources.
 *
 * <p>Os valores sempre entram por {@link PreparedStatement} ({@code ?}),
 * nunca por concatenação de String no SQL. Falha do banco vira
 * {@link FonteIndisponivelException}; linha corrompida vira
 * {@link DadosInvalidosException}.</p>
 */
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

    /**
     * Executa um INSERT e devolve o id gerado pelo banco ({@code AUTO_INCREMENT}).
     *
     * @throws FonteIndisponivelException se o banco falhar
     */
    protected long inserirRetornandoId(String sql, StatementPreparer preparer) throws FonteIndisponivelException {
        try (Connection conn = fabricaConexao.abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparer.prepare(stmt);
            stmt.executeUpdate();
            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new FonteIndisponivelException(FabricaConexao.ID_FONTE,
                            "O banco gravou o registro, mas não devolveu o id gerado.");
                }
                return chaves.getLong(1);
            }
        } catch (SQLException e) {
            throw falhaNoBanco("gravar", e);
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
