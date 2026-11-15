package persistencia;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Abre conexões JDBC com o banco H2 e cria as tabelas na primeira execução.
 *
 * <p>O banco padrão fica em <b>arquivo</b> ({@code ./dados/territorio.mv.db}),
 * então medições e alertas sobrevivem ao reiniciar o programa. Os testes passam
 * outra URL (ex.: um arquivo em pasta temporária) pelo construtor.</p>
 */
public class FabricaConexao {

    /** Banco em arquivo, relativo à pasta onde o programa é executado. */
    public static final String URL_PADRAO = "jdbc:h2:./dados/territorio";

    /** Identificador usado em {@link FonteIndisponivelException} quando o banco falha. */
    public static final String ID_FONTE = "banco de dados";

    private static final String USUARIO = "sa";
    private static final String SENHA = "";
    private static final String ARQUIVO_SCHEMA = "/schema.sql";

    private final String url;

    public FabricaConexao() {
        this(URL_PADRAO);
    }

    public FabricaConexao(String url) {
        if (url == null || url.isBlank()) {
            throw new DadosInvalidosException("Informe a URL de conexão com o banco de dados.");
        }
        this.url = url.strip();
    }

    /**
     * Abre uma nova conexão. Quem chama deve fechá-la (try-with-resources).
     *
     * @throws FonteIndisponivelException se o banco não puder ser aberto
     */
    public Connection abrirConexao() throws FonteIndisponivelException {
        try {
            return DriverManager.getConnection(url, USUARIO, SENHA);
        } catch (SQLException e) {
            throw new FonteIndisponivelException(ID_FONTE,
                    "Não foi possível conectar ao banco de dados (" + url + "): " + e.getMessage(), e);
        }
    }

    /**
     * Executa o {@code schema.sql}. Os comandos usam {@code IF NOT EXISTS}, então
     * rodar de novo não apaga nem duplica nada.
     *
     * @throws FonteIndisponivelException se o script não for encontrado ou falhar
     */
    public void criarTabelasSeNecessario() throws FonteIndisponivelException {
        String script = lerSchema();
        try (Connection conn = abrirConexao(); Statement stmt = conn.createStatement()) {
            // O script não tem dados vindos do usuário, então executar o texto direto é seguro.
            for (String comando : script.split(";")) {
                if (!comando.isBlank()) {
                    stmt.execute(comando);
                }
            }
        } catch (SQLException e) {
            throw new FonteIndisponivelException(ID_FONTE,
                    "Não foi possível criar as tabelas do banco: " + e.getMessage(), e);
        }
    }

    private static String lerSchema() throws FonteIndisponivelException {
        try (InputStream entrada = FabricaConexao.class.getResourceAsStream(ARQUIVO_SCHEMA)) {
            if (entrada == null) {
                throw new FonteIndisponivelException(ID_FONTE,
                        "Arquivo " + ARQUIVO_SCHEMA + " não encontrado no classpath.");
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FonteIndisponivelException(ID_FONTE,
                    "Não foi possível ler " + ARQUIVO_SCHEMA + ": " + e.getMessage(), e);
        }
    }

    public String getUrl() {
        return url;
    }
}
