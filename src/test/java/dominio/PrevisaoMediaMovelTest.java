package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoMediaMovelTest {

    private final PrevisaoMediaMovel mediaMovel = new PrevisaoMediaMovel(5);

    @Test
    void calculaMediaDasUltimasCincoLeituras() {
        // Arrange: a leitura de 100 é antiga e fica fora da janela
        List<Medicao> historico = HistoricoDeTeste.chuva(100, 10, 20, 30, 40, 50);

        // Act
        ResultadoPrevisao resultado = mediaMovel.prever(historico);

        // Assert: (10+20+30+40+50)/5 = 30
        assertTrue(resultado.temPrevisao());
        assertEquals(30.0, resultado.valorPrevisto().getAsDouble(), 1e-9);
        assertEquals(5, resultado.leiturasUsadas());
        assertEquals("PLU-01", resultado.sensorId());
        assertTrue(resultado.interpretacao().contains("30,0 mm/h"), resultado.interpretacao());
    }

    @Test
    void ordenaPorDataAntesDeCalcular() {
        // Arrange: mesmo histórico, entregue fora de ordem (como poderia vir do banco)
        List<Medicao> historico = new ArrayList<>(HistoricoDeTeste.chuva(100, 10, 20, 30, 40, 50));
        Collections.reverse(historico);

        // Act
        ResultadoPrevisao resultado = mediaMovel.prever(historico);

        // Assert
        assertEquals(30.0, resultado.valorPrevisto().getAsDouble(), 1e-9);
    }

    @Test
    void comMenosDeCincoLeiturasSeCalaEExplica() {
        // Arrange
        List<Medicao> historico = HistoricoDeTeste.chuva(10, 20, 30, 40);

        // Act
        ResultadoPrevisao resultado = mediaMovel.prever(historico);

        // Assert
        assertFalse(resultado.temPrevisao());
        assertEquals(4, resultado.leiturasUsadas());
        assertTrue(resultado.interpretacao().startsWith("Dados insuficientes: 4 leitura(s)"),
                resultado.interpretacao());
    }

    @Test
    void historicoVazioTambemNaoGeraPrevisao() {
        // Act
        ResultadoPrevisao resultado = mediaMovel.prever(List.of());

        // Assert
        assertFalse(resultado.temPrevisao());
        assertEquals(0, resultado.leiturasUsadas());
    }

    @Test
    void rejeitaHistoricoQueMisturaSensores() {
        // Arrange
        List<Medicao> historico = new ArrayList<>(HistoricoDeTeste.chuva(10, 20, 30));
        historico.addAll(HistoricoDeTeste.de("PLU-02", TipoSensor.PRECIPITACAO, 40, 50));

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> mediaMovel.prever(historico));

        // Assert
        assertTrue(erro.getMessage().contains("mistura os sensores PLU-01 e PLU-02"), erro.getMessage());
    }

    @Test
    void mensagemExplicaQuandoOMesmoSensorMisturaTipos() {
        // Arrange
        List<Medicao> historico = new ArrayList<>(HistoricoDeTeste.de("S1", TipoSensor.TEMPERATURA, 20, 21));
        historico.addAll(HistoricoDeTeste.de("S1", TipoSensor.PRECIPITACAO, 5));

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> mediaMovel.prever(historico));

        // Assert
        assertEquals("O histórico do sensor S1 mistura os tipos temperatura e chuva; "
                + "cada sensor mede uma única grandeza.", erro.getMessage());
    }

    @Test
    void rejeitaHistoricoNuloEJanelaPequenaDemais() {
        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> mediaMovel.prever(null));
        assertThrows(DadosInvalidosException.class, () -> new PrevisaoMediaMovel(1));
    }

    @Test
    void descricaoIncluiParametrosDaFilha() {
        // Act
        String descricao = mediaMovel.descrever();

        // Assert
        assertEquals("Média móvel (mínimo de 5 leituras): média das últimas 5 leituras", descricao);
    }
}
