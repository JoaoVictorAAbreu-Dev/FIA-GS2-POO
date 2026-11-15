package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 13, 10, 0);

    @Test
    void criaMedicaoValidaSemIdEDepoisAtribuiIdDoBanco() {
        // Arrange
        Medicao nova = Medicao.nova("  PLU-01 ", TipoSensor.PRECIPITACAO, 12.5, AGORA);

        // Act
        Medicao gravada = nova.comId(7L);

        // Assert
        assertNull(nova.id());
        assertEquals(7L, gravada.id());
        assertEquals("PLU-01", gravada.sensorId());
        assertEquals("12,5 mm/h", gravada.valorFormatado());
    }

    @Test
    void rejeitaChuvaNegativaComMensagemClara() {
        // Arrange
        double chuvaNegativa = -5.0;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> Medicao.nova("PLU-01", TipoSensor.PRECIPITACAO, chuvaNegativa, AGORA));

        // Assert
        assertEquals("Medição de chuva negativa: -5,0 mm/h.", erro.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejeitaValoresNaoFinitos(double valor) {
        // Arrange / Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> Medicao.nova("NIV-01", TipoSensor.NIVEL_CORREGO, valor, AGORA));

        // Assert
        assertTrue(erro.getMessage().contains("número finito"), erro.getMessage());
    }

    @Test
    void rejeitaUmidadeForaDaFaixaFisica() {
        // Arrange / Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> Medicao.nova("UMI-01", TipoSensor.UMIDADE_SOLO, 120.0, AGORA));

        // Assert
        assertTrue(erro.getMessage().contains("fora da faixa física"), erro.getMessage());
    }

    @Test
    void aceitaValoresNosExtremosDaFaixa() {
        // Arrange / Act
        Medicao minima = Medicao.nova("NIV-01", TipoSensor.NIVEL_CORREGO, 0.0, AGORA);
        Medicao maxima = Medicao.nova("NIV-01", TipoSensor.NIVEL_CORREGO, 8.0, AGORA);

        // Assert
        assertEquals(0.0, minima.valor());
        assertEquals(8.0, maxima.valor());
    }

    @Test
    void rejeitaCamposObrigatoriosAusentesEIdNaoPositivo() {
        // Arrange / Act / Assert
        assertThrows(DadosInvalidosException.class,
                () -> Medicao.nova(" ", TipoSensor.UMIDADE_SOLO, 60.0, AGORA));
        assertThrows(DadosInvalidosException.class,
                () -> Medicao.nova("UMI-01", null, 60.0, AGORA));
        assertThrows(DadosInvalidosException.class,
                () -> Medicao.nova("UMI-01", TipoSensor.UMIDADE_SOLO, 60.0, null));
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao(0L, "UMI-01", TipoSensor.UMIDADE_SOLO, 60.0, AGORA));
    }
}
