package dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoComparacaoTest {

    private final PrevisaoComparacao comparacao = new PrevisaoComparacao(3);

    @Test
    void projetaATendenciaEntrePeriodos() {
        // Arrange: anterior = (10+20+30)/3 = 20; atual = (30+40+50)/3 = 40
        List<Medicao> historico = HistoricoDeTeste.chuva(10, 20, 30, 30, 40, 50);

        // Act
        ResultadoPrevisao resultado = comparacao.prever(historico);

        // Assert: 40 + (40 - 20) = 60
        assertEquals(60.0, resultado.valorPrevisto().getAsDouble(), 1e-9);
        assertEquals(6, resultado.leiturasUsadas());
        assertTrue(resultado.interpretacao().contains("alta de 100,0%"), resultado.interpretacao());
    }

    @Test
    void quedaForteNaoGeraChuvaNegativa() {
        // Arrange: anterior = 50; atual = 5 → projeção crua = -40
        List<Medicao> historico = HistoricoDeTeste.chuva(50, 50, 50, 5, 5, 5);

        // Act
        ResultadoPrevisao resultado = comparacao.prever(historico);

        // Assert: limitado ao mínimo físico
        assertEquals(0.0, resultado.valorPrevisto().getAsDouble(), 1e-9);
        assertTrue(resultado.interpretacao().contains("queda"), resultado.interpretacao());
    }

    @Test
    void exigeODobroDoPeriodoEmLeituras() {
        // Arrange
        List<Medicao> historico = HistoricoDeTeste.chuva(10, 20, 30, 40, 50);

        // Act
        ResultadoPrevisao resultado = comparacao.prever(historico);

        // Assert
        assertFalse(resultado.temPrevisao());
        assertEquals(6, comparacao.getMinimoLeituras());
    }

    @Test
    void estrategiasDiferentesDaoRespostasDiferentesParaOMesmoHistorico() {
        // Arrange
        List<Medicao> historico = HistoricoDeTeste.chuva(10, 20, 30, 30, 40, 50);
        List<EstrategiaPrevisao> estrategias = List.of(new PrevisaoMediaMovel(5), comparacao);

        // Act
        double mediaMovel = estrategias.get(0).prever(historico).valorPrevisto().getAsDouble();
        double tendencia = estrategias.get(1).prever(historico).valorPrevisto().getAsDouble();

        // Assert: média móvel (20+30+30+40+50)/5 = 34; comparação = 60
        assertEquals(34.0, mediaMovel, 1e-9);
        assertEquals(60.0, tendencia, 1e-9);
    }
}
