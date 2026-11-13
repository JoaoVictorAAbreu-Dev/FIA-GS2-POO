package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 13, 12, 0);

    private DadosTerritorio territorio;

    @BeforeEach
    void prepararTerritorio() {
        Map<TipoSensor, Double> limiares = new EnumMap<>(TipoSensor.class);
        limiares.put(TipoSensor.TEMPERATURA, 35.0);
        limiares.put(TipoSensor.PRECIPITACAO, 30.0);
        limiares.put(TipoSensor.CONSUMO_AGUA, 120.0);
        territorio = new DadosTerritorio("Bacia do Córrego", "Zona Leste", 45_000, "coord-2026", limiares);
    }

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
        // Arrange: média das últimas 5 = 48 mm/h; limiar de chuva 30 → ALERTA a partir de 45
        ResultadoPrevisao previsao = new PrevisaoMediaMovel(5)
                .prever(HistoricoDeTeste.chuva(40, 45, 50, 50, 55));

        // Act
        Alerta alerta = Alerta.avaliar(previsao, territorio, AGORA).orElseThrow();

        // Assert
        assertEquals(NivelAlerta.ALERTA, alerta.nivel());
        assertEquals(48.0, alerta.valorPrevisto(), 1e-9);
        assertEquals(45.0, alerta.limiteUltrapassado(), 1e-9);
        assertEquals("[ALERTA] Sensor PLU-01 (chuva): a previsão de 48,0 mm/h (Média móvel) atingiu o limite "
                        + "de 45,0 mm/h do nível ALERTA. O que fazer: " + NivelAlerta.ALERTA.getOrientacao(),
                alerta.explicacao());
    }

    @Test
    void usaOLimiarDoTipoDaPropriaPrevisao() {
        // Arrange: 100 m³/h é consumo normal (limiar 120). Com o limiar de temperatura (35)
        // por engano, viraria CRÍTICO; avaliar busca o limiar certo sozinho.
        ResultadoPrevisao consumo = new PrevisaoMediaMovel(5)
                .prever(HistoricoDeTeste.de("AGU-01", TipoSensor.CONSUMO_AGUA, 100, 100, 100, 100, 100));

        // Act
        Optional<Alerta> alerta = Alerta.avaliar(consumo, territorio, AGORA);

        // Assert
        assertTrue(alerta.isEmpty());
    }

    @Test
    void naoEmiteAlertaSemPrevisaoOuAbaixoDoLimiar() {
        // Arrange
        ResultadoPrevisao insuficiente = new PrevisaoMediaMovel(5).prever(HistoricoDeTeste.chuva(90, 90));
        ResultadoPrevisao tranquila = new PrevisaoMediaMovel(5).prever(HistoricoDeTeste.chuva(1, 2, 3, 4, 5));

        // Act / Assert
        assertTrue(Alerta.avaliar(insuficiente, territorio, AGORA).isEmpty());
        assertTrue(Alerta.avaliar(tranquila, territorio, AGORA).isEmpty());
    }

    @Test
    void exigeTerritorioNaAvaliacao() {
        // Arrange
        ResultadoPrevisao previsao = new PrevisaoMediaMovel(5).prever(HistoricoDeTeste.chuva(1, 2, 3, 4, 5));

        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> Alerta.avaliar(previsao, null, AGORA));
    }

    @Test
    void rejeitaPrevisaoAbaixoDoLimite() {
        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> new Alerta(null, "PLU-01", TipoSensor.PRECIPITACAO,
                NivelAlerta.CRITICO, 40.0, 60.0, PrevisaoMediaMovel.NOME, AGORA));
    }

    @Test
    void rejeitaLimiteQueNaoCorrespondeAoNivel() {
        // Arrange: CRÍTICO com limite 30 implicaria limiar de atenção 30 - 2×15 = 0
        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () -> new Alerta(1L, "PLU-01",
                TipoSensor.PRECIPITACAO, NivelAlerta.CRITICO, 31.0, 30.0, PrevisaoMediaMovel.NOME, AGORA));

        // Assert
        assertTrue(erro.getMessage().contains("baixo demais para o nível CRÍTICO"), erro.getMessage());
    }

    @Test
    void rejeitaNivelMenosGraveQueOAtingido() {
        // Arrange: limite de ATENÇÃO 30 → ALERTA em 45; previsão de 50 já é ALERTA
        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () -> new Alerta(1L, "PLU-01",
                TipoSensor.PRECIPITACAO, NivelAlerta.ATENCAO, 50.0, 30.0, PrevisaoMediaMovel.NOME, AGORA));

        // Assert
        assertTrue(erro.getMessage().contains("já atinge o nível ALERTA"), erro.getMessage());
    }

    @Test
    void aceitaAlertaCoerenteLidoDoBanco() {
        // Act / Assert: CRÍTICO não tem nível acima, então qualquer previsão ≥ limite vale
        assertDoesNotThrow(() -> new Alerta(3L, "PLU-01", TipoSensor.PRECIPITACAO, NivelAlerta.CRITICO,
                120.0, 60.0, PrevisaoComparacao.NOME, AGORA));
        assertDoesNotThrow(() -> new Alerta(4L, "PLU-01", TipoSensor.PRECIPITACAO, NivelAlerta.ATENCAO,
                44.9, 30.0, PrevisaoMediaMovel.NOME, AGORA));
    }
}
