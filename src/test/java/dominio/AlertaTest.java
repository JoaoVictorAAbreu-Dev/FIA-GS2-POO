package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 13, 12, 0);

    @ParameterizedTest(name = "chuva prevista {0} mm/h com limiar 30 → {1}")
    @CsvSource({
            "29.9, NENHUM",
            "30.0, ATENCAO",
            "44.9, ATENCAO",
            "45.0, ALERTA",
            "60.0, CRITICO",
            "200.0, CRITICO"
    })
    void classificaNivelPelosLimitesDoTipo(double previsto, String esperado) {
        // Act
        Optional<NivelAlerta> nivel = NivelAlerta.classificar(previsto, 30.0, TipoSensor.PRECIPITACAO);

        // Assert
        assertEquals(esperado, nivel.map(Enum::name).orElse("NENHUM"));
    }

    @Test
    void avaliaPrevisaoECriaAlertaExplicado() {
        // Arrange: média das últimas 5 = 48 mm/h; limiar 30 → ALERTA a partir de 45
        ResultadoPrevisao previsao = new PrevisaoMediaMovel(5)
                .prever(HistoricoDeTeste.chuva(40, 45, 50, 50, 55));

        // Act
        Alerta alerta = Alerta.avaliar(previsao, 30.0, AGORA).orElseThrow();

        // Assert
        assertEquals(NivelAlerta.ALERTA, alerta.nivel());
        assertEquals(48.0, alerta.valorPrevisto(), 1e-9);
        assertEquals(45.0, alerta.limiteUltrapassado(), 1e-9);
        assertEquals("[ALERTA] Sensor PLU-01 (chuva): a previsão de 48,0 mm/h (Média móvel) atingiu o limite "
                        + "de 45,0 mm/h do nível ALERTA. O que fazer: " + NivelAlerta.ALERTA.getOrientacao(),
                alerta.explicacao());
    }

    @Test
    void naoEmiteAlertaSemPrevisaoOuAbaixoDoLimiar() {
        // Arrange
        ResultadoPrevisao insuficiente = new PrevisaoMediaMovel(5).prever(HistoricoDeTeste.chuva(90, 90));
        ResultadoPrevisao tranquila = new PrevisaoMediaMovel(5).prever(HistoricoDeTeste.chuva(1, 2, 3, 4, 5));

        // Act / Assert
        assertTrue(Alerta.avaliar(insuficiente, 30.0, AGORA).isEmpty());
        assertTrue(Alerta.avaliar(tranquila, 30.0, AGORA).isEmpty());
    }

    @Test
    void rejeitaLimiarInvalidoNaAvaliacao() {
        // Arrange
        ResultadoPrevisao previsao = new PrevisaoMediaMovel(5).prever(HistoricoDeTeste.chuva(1, 2, 3, 4, 5));

        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> Alerta.avaliar(previsao, 0.0, AGORA));
    }

    @Test
    void rejeitaAlertaIncoerente() {
        // Act / Assert: previsão abaixo do limite não pode virar alerta
        assertThrows(DadosInvalidosException.class, () -> new Alerta(null, "PLU-01", TipoSensor.PRECIPITACAO,
                NivelAlerta.CRITICO, 40.0, 60.0, PrevisaoMediaMovel.NOME, AGORA));
    }
}
