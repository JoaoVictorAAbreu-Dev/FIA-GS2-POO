package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DadosTerritorioTest {

    private static final String CODIGO = "coord-2026";

    private DadosTerritorio territorio;

    @BeforeEach
    void prepararTerritorio() {
        Map<TipoSensor, Double> limiares = new EnumMap<>(TipoSensor.class);
        limiares.put(TipoSensor.TEMPERATURA, 35.0);
        limiares.put(TipoSensor.PRECIPITACAO, 30.0);
        limiares.put(TipoSensor.CONSUMO_AGUA, 120.0);
        territorio = new DadosTerritorio("Bacia do Córrego", "Zona Leste", 45_000, CODIGO, limiares);
    }

    @Test
    void alteraLimiarComCodigoCorreto() {
        // Act
        territorio.alterarLimiar(CODIGO, TipoSensor.PRECIPITACAO, 25.0);

        // Assert
        assertEquals(25.0, territorio.getLimiar(TipoSensor.PRECIPITACAO));
    }

    @Test
    void recusaCodigoErradoSemAlterarLimiar() {
        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiar("chute", TipoSensor.PRECIPITACAO, 10.0));

        // Assert
        assertEquals("Código de acesso inválido: alteração não autorizada.", erro.getMessage());
        assertEquals(30.0, territorio.getLimiar(TipoSensor.PRECIPITACAO));
    }

    @Test
    void recusaLimiarNegativoComMensagemDoEnunciado() {
        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiar(CODIGO, TipoSensor.PRECIPITACAO, -5.0));

        // Assert
        assertEquals("O limiar de chuva deve ser positivo; valor recebido: -5,0", erro.getMessage());
        assertEquals(30.0, territorio.getLimiar(TipoSensor.PRECIPITACAO));
    }

    @Test
    void recusaLimiarAcimaDaFaixaFisicaENaoNumerico() {
        // Act / Assert
        assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiar(CODIGO, TipoSensor.TEMPERATURA, 80.0));
        assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiar(CODIGO, TipoSensor.TEMPERATURA, Double.NaN));
    }

    @Test
    void recusaLimiarQueTornariaOsNiveisMaisGravesInalcancaveis() {
        // Arrange: chuva vai até 300 mm/h; CRÍTICO = limiar + 2×15, então o maior limiar é 270
        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiar(CODIGO, TipoSensor.PRECIPITACAO, 290.0));
        territorio.alterarLimiar(CODIGO, TipoSensor.PRECIPITACAO, 270.0);

        // Assert
        assertTrue(erro.getMessage().contains("não pode passar de 270,0 mm/h"), erro.getMessage());
        assertEquals(NivelAlerta.CRITICO, NivelAlerta.classificar(300.0,
                territorio.getLimiar(TipoSensor.PRECIPITACAO), TipoSensor.PRECIPITACAO).orElseThrow());
    }

    @Test
    void recusaPopulacaoNegativa() {
        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarPopulacaoEstimada(CODIGO, -1));

        // Assert
        assertEquals("A população estimada não pode ser negativa; valor recebido: -1", erro.getMessage());
        assertEquals(45_000, territorio.getPopulacaoEstimada());
    }

    @Test
    void limiaresExpostosSaoSomenteLeitura() {
        // Act / Assert
        assertThrows(UnsupportedOperationException.class,
                () -> territorio.getLimiares().put(TipoSensor.PRECIPITACAO, 1.0));
    }

    @Test
    void construtorExigeTodosOsLimiares() {
        // Arrange
        Map<TipoSensor, Double> incompleto = new EnumMap<>(TipoSensor.class);
        incompleto.put(TipoSensor.TEMPERATURA, 35.0);

        // Act / Assert
        assertThrows(DadosInvalidosException.class,
                () -> new DadosTerritorio("Bacia", "Zona Leste", 100, CODIGO, incompleto));
    }
}
