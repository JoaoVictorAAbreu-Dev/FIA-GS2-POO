package persistencia;

import dominio.Alerta;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testes de integração com um H2 de verdade, em arquivo numa pasta temporária. */
class AlertaDAOTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 14, 0, 0);

    @TempDir
    Path pasta;

    private AlertaDAO dao;

    @BeforeEach
    void prepararBanco() throws FonteIndisponivelException {
        FabricaConexao fabrica = new FabricaConexao("jdbc:h2:" + pasta.resolve("teste").toAbsolutePath());
        fabrica.criarTabelasSeNecessario();
        dao = new AlertaDAO(fabrica);
    }

    @Test
    void gravaTodosOsCamposELerDevolveOMesmoAlerta() throws FonteIndisponivelException {
        // Arrange
        Alerta alerta = new Alerta(null, "PLU-01", TipoSensor.PRECIPITACAO, NivelAlerta.ALERTA, 48.0, 45.0,
                "Média móvel", INICIO);

        // Act
        Alerta gravado = dao.inserir(alerta);
        List<Alerta> alertas = dao.listarTodos();

        // Assert
        assertNotNull(gravado.id());
        assertEquals(List.of(gravado), alertas);
    }

    @Test
    void listaDoMaisRecenteParaOMaisAntigo() throws FonteIndisponivelException {
        // Arrange
        dao.inserir(atencao(INICIO));
        dao.inserir(atencao(INICIO.plusHours(2)));
        dao.inserir(atencao(INICIO.plusHours(1)));

        // Act
        List<Alerta> alertas = dao.listarTodos();

        // Assert
        assertEquals(List.of(INICIO.plusHours(2), INICIO.plusHours(1), INICIO),
                alertas.stream().map(Alerta::dataHora).toList());
    }

    @Test
    void ultimosAlertasTrazSoOsMaisRecentes() throws FonteIndisponivelException {
        // Arrange
        dao.inserir(atencao(INICIO));
        dao.inserir(atencao(INICIO.plusHours(1)));
        dao.inserir(atencao(INICIO.plusHours(2)));

        // Act
        List<Alerta> ultimos = dao.listarUltimosAlertas(2);

        // Assert: os 2 mais recentes, na mesma ordem de listarTodos
        assertEquals(List.of(INICIO.plusHours(2), INICIO.plusHours(1)),
                ultimos.stream().map(Alerta::dataHora).toList());
    }

    @Test
    void limiteDeAlertasPrecisaSerPositivo() {
        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> dao.listarUltimosAlertas(0));
    }

    private static Alerta atencao(LocalDateTime dataHora) {
        return new Alerta(null, "NIV-01", TipoSensor.NIVEL_CORREGO, NivelAlerta.ATENCAO, 2.1, 2.0,
                "Média móvel", dataHora);
    }
}
