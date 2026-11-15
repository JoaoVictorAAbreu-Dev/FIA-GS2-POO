package servico;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;

public class FabricaConexao {
    private static final String URL = "jdbc:h2:mem:meubanco;DB_CLOSE_DELAY=-1";
    private static final String USER ="";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar ao banco de dados. ERROR: ", e);
        }
    }
}
