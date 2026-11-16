package servico;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.NivelAlerta;
import dominio.PontoMonitoramento;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioBuilderTest {

    private DadosTerritorio territorio;

    @BeforeEach
    void criarTerritorio() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
        territorio = new DadosTerritorio("Bacia do Córrego", "Zona Leste", 45_000, "1234",
                ConfiguracaoSistema.getInstancia().getLimiaresPadrao());
    }

    @AfterEach
    void restaurarConfiguracao() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
    }

    @Test
    void montaSoAsSecoesPedidasNaOrdemDosPassos() {
        // Act
        String relatorio = new RelatorioBuilder("Status")
                .comLimiares(territorio)
                .comTerritorio(territorio)
                .construir();

        // Assert: limiar 2,0 m e degrau 0,5 m → 2,0 / 2,5 / 3,0
        assertTrue(relatorio.startsWith("===== Status =====\nLimiares por nível:"), relatorio);
        assertTrue(relatorio.contains("nível do córrego: ATENÇÃO ≥ 2,0 m ALERTA ≥ 2,5 m CRÍTICO ≥ 3,0 m"), relatorio);
        assertTrue(relatorio.indexOf("Limiares") < relatorio.indexOf("Território: Bacia do Córrego"), relatorio);
        assertFalse(relatorio.contains("Alertas recentes"), relatorio);
    }

    @Test
    void componentesEAlertasAparecemComStatusENivel() {
        // Arrange
        PontoMonitoramento ponto = new PontoMonitoramento("PT-01", "Estação", "Zona Leste");
        Alerta alerta = new Alerta(1L, "NIV-01", TipoSensor.NIVEL_CORREGO, NivelAlerta.CRITICO, 3.2, 3.0,
                "Média móvel", LocalDateTime.of(2026, 11, 15, 18, 0));

        // Act
        String relatorio = new RelatorioBuilder("Status")
                .comComponentes(List.of(ponto))
                .comAlertasRecentes(List.of(alerta))
                .construir();

        // Assert: ponto sem sensores fica INDISPONIVEL
        assertTrue(relatorio.contains("PT-01 - Estação (Zona Leste) [INDISPONIVEL] - sem resposta"), relatorio);
        assertTrue(relatorio.contains("15/11/2026 18:00  [CRÍTICO] NIV-01: 3,2 m previsto, limite 3,0 m"), relatorio);
    }

    @Test
    void semAlertasAvisaEmVezDeDeixarASecaoVazia() {
        // Act
        String relatorio = new RelatorioBuilder("Status").comAlertasRecentes(List.of()).construir();

        // Assert
        assertEquals("===== Status =====\nAlertas recentes:\n  Nenhum alerta emitido.", relatorio);
    }

    @Test
    void relatorioSemSecaoOuComDadoNuloLancaDadosInvalidos() {
        // Arrange
        RelatorioBuilder vazio = new RelatorioBuilder("Status");

        // Act / Assert
        assertThrows(DadosInvalidosException.class, vazio::construir);
        assertThrows(DadosInvalidosException.class, () -> vazio.comTerritorio(null));
        assertThrows(DadosInvalidosException.class, () -> new RelatorioBuilder(" "));
    }
}
