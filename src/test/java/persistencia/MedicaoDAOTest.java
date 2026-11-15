package persistencia;

import dominio.Medicao;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de integração com um H2 de verdade, em arquivo numa pasta temporária. */
class MedicaoDAOTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 14, 0, 0);

    @TempDir
    Path pasta;

    private String url;
    private FabricaConexao fabrica;
    private MedicaoDAO dao;

    @BeforeEach
    void prepararBanco() throws FonteIndisponivelException {
        url = "jdbc:h2:" + pasta.resolve("teste").toAbsolutePath();
        fabrica = new FabricaConexao(url);
        fabrica.criarTabelasSeNecessario();
        dao = new MedicaoDAO(fabrica);
    }

    @Test
    void inserirGeraIdEHistoricoVoltaEmOrdemCronologica() throws FonteIndisponivelException {
        // Arrange: gravadas fora de ordem
        dao.inserir(chuva(20.0, INICIO.plusHours(2)));
        dao.inserir(chuva(10.0, INICIO));
        dao.inserir(chuva(15.0, INICIO.plusHours(1)));

        // Act
        List<Medicao> historico = dao.buscarHistoricoPorSensor("PLU-01");

        // Assert
        assertEquals(List.of(10.0, 15.0, 20.0), historico.stream().map(Medicao::valor).toList());
        historico.forEach(m -> assertNotNull(m.id()));
    }

    @Test
    void ultimasTrazAsMaisRecentesEmOrdemCronologica() throws FonteIndisponivelException {
        // Arrange: 6 leituras de hora em hora (valores 1..6) e uma de outro sensor
        for (int i = 1; i <= 6; i++) {
            dao.inserir(chuva(i, INICIO.plusHours(i)));
        }
        dao.inserir(Medicao.nova("PLU-02", TipoSensor.PRECIPITACAO, 99.0, INICIO.plusHours(10)));

        // Act
        List<Medicao> ultimas = dao.buscarUltimasPorSensor("PLU-01", 3);

        // Assert: as 3 mais recentes do PLU-01, da mais antiga para a mais nova
        assertEquals(List.of(4.0, 5.0, 6.0), ultimas.stream().map(Medicao::valor).toList());
    }

    @Test
    void dadosSobrevivemAoReabrirOBanco() throws FonteIndisponivelException {
        // Arrange
        Medicao gravada = dao.inserir(chuva(12.5, INICIO));

        // Act: nova fábrica e novo DAO apontando para o mesmo arquivo, como num reinício
        MedicaoDAO reaberto = new MedicaoDAO(new FabricaConexao(url));
        List<Medicao> historico = reaberto.buscarHistoricoPorSensor("PLU-01");

        // Assert
        assertEquals(List.of(gravada), historico);
    }

    @Test
    void registroCorrompidoNoBancoLancaDadosInvalidos() throws Exception {
        // Arrange: linha com chuva negativa gravada direto no banco, sem passar pelo domínio
        try (Connection conn = fabrica.abrirConexao(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO medicao (sensorId, tipo, valor, dataHora) "
                    + "VALUES ('PLU-01', 'PRECIPITACAO', -5, TIMESTAMP '2026-11-14 00:00:00')");
        }

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> dao.buscarHistoricoPorSensor("PLU-01"));

        // Assert
        assertTrue(erro.getMessage().startsWith("Registro inválido no banco (id 1)"), erro.getMessage());
    }

    @Test
    void medicaoJaGravadaNaoEInseridaDeNovo() throws FonteIndisponivelException {
        // Arrange
        Medicao gravada = dao.inserir(chuva(12.5, INICIO));

        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> dao.inserir(gravada));
    }

    @Test
    void bancoInacessivelLancaFonteIndisponivel() {
        // Arrange: URL de um driver que não existe
        MedicaoDAO semBanco = new MedicaoDAO(new FabricaConexao("jdbc:inexistente:banco"));

        // Act
        FonteIndisponivelException erro = assertThrows(FonteIndisponivelException.class,
                () -> semBanco.buscarHistoricoPorSensor("PLU-01"));

        // Assert
        assertEquals(FabricaConexao.ID_FONTE, erro.getIdFonte());
    }

    private static Medicao chuva(double valor, LocalDateTime dataHora) {
        return Medicao.nova("PLU-01", TipoSensor.PRECIPITACAO, valor, dataHora);
    }
}
