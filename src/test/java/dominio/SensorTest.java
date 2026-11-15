package dominio;

import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 13, 10, 0);

    @Test
    void cadaSensorGeraMedicoesValidasDoProprioTipo() throws FonteIndisponivelException {
        // Arrange
        List<SensorSimulado> sensores = List.of(
                new SensorNivelCorrego("NIV-01", "Régua do córrego", "Zona Leste", new Random(1)),
                new SensorPluviometro("PLU-01", "Pluviômetro Córrego", "Zona Leste", new Random(2)),
                new SensorUmidadeSolo("UMI-01", "Sonda da encosta", "Zona Leste", new Random(3)));

        for (SensorSimulado sensor : sensores) {
            for (int i = 0; i < 500; i++) {
                // Act
                Medicao medicao = sensor.lerMedicao(AGORA);

                // Assert: o construtor de Medicao já valida a faixa; conferimos tipo e origem
                assertEquals(sensor.getTipo(), medicao.tipo());
                assertEquals(sensor.getId(), medicao.sensorId());
            }
        }
    }

    @Test
    void sensorEmFalhaLancaFonteIndisponivelEFicaIndisponivel() {
        // Arrange
        SensorPluviometro sensor = new SensorPluviometro("PLU-02", "Pluviômetro Escola", "Zona Norte");
        sensor.simularFalha();

        // Act
        FonteIndisponivelException erro = assertThrows(FonteIndisponivelException.class, sensor::lerValor);
        StatusComponente status = sensor.verificarStatus();

        // Assert
        assertEquals("PLU-02", erro.getIdFonte());
        assertTrue(erro.getMessage().contains("não respondeu"), erro.getMessage());
        assertFalse(sensor.estaFuncionando());
        assertEquals(StatusComponente.INDISPONIVEL, status);
    }

    @Test
    void sensorRestabelecidoVoltaALer() throws FonteIndisponivelException {
        // Arrange
        SensorNivelCorrego sensor = new SensorNivelCorrego("NIV-02", "Régua da ponte", "Zona Sul", new Random(5));
        sensor.simularFalha();

        // Act
        sensor.restabelecer();
        double valor = sensor.lerValor();

        // Assert
        assertEquals(StatusComponente.OPERACIONAL, sensor.verificarStatus());
        assertTrue(valor >= 0 && valor <= 8, "nível fora da faixa: " + valor);
    }

    @Test
    void pontoDeMonitoramentoDerivaStatusDosSensores() {
        // Arrange
        SensorNivelCorrego regua = new SensorNivelCorrego("NIV-01", "Régua", "Zona Leste");
        SensorPluviometro pluviometro = new SensorPluviometro("PLU-01", "Pluviômetro", "Zona Leste");
        PontoMonitoramento ponto = new PontoMonitoramento("PT-01", "Estação Córrego", "Zona Leste");
        ponto.adicionarSensor(regua);
        ponto.adicionarSensor(pluviometro);

        // Act
        StatusComponente todosOk = ponto.verificarStatus();
        pluviometro.simularFalha();
        StatusComponente umEmFalha = ponto.verificarStatus();
        regua.simularFalha();
        StatusComponente nenhum = ponto.verificarStatus();

        // Assert
        assertEquals(StatusComponente.OPERACIONAL, todosOk);
        assertEquals(StatusComponente.DEGRADADO, umEmFalha);
        assertEquals(StatusComponente.INDISPONIVEL, nenhum);
    }

    @Test
    void statusDoSensorMudaJuntoComAFalhaSemPrecisarVerificar() {
        // Arrange
        SensorNivelCorrego sensor = new SensorNivelCorrego("NIV-03", "Régua", "Zona Oeste");

        // Act
        sensor.simularFalha();
        String emFalha = sensor.toString();
        StatusComponente statusEmFalha = sensor.getStatus();
        sensor.restabelecer();

        // Assert
        assertEquals(StatusComponente.INDISPONIVEL, statusEmFalha);
        assertTrue(emFalha.endsWith("[INDISPONIVEL]"), emFalha);
        assertEquals(StatusComponente.OPERACIONAL, sensor.getStatus());
    }

    @Test
    void pontoSemSensoresApareceIndisponivelEAcompanhaFalhas() {
        // Arrange
        PontoMonitoramento ponto = new PontoMonitoramento("PT-02", "Estação Nova", "Zona Oeste");
        SensorPluviometro pluviometro = new SensorPluviometro("PLU-09", "Pluviômetro", "Zona Oeste");

        // Act
        String vazio = ponto.toString();
        ponto.adicionarSensor(pluviometro);
        StatusComponente comSensor = ponto.getStatus();
        pluviometro.simularFalha();
        StatusComponente aposFalha = ponto.getStatus();

        // Assert
        assertEquals("PT-02 - Estação Nova (Zona Oeste) [INDISPONIVEL]", vazio);
        assertEquals(StatusComponente.OPERACIONAL, comSensor);
        assertEquals(StatusComponente.INDISPONIVEL, aposFalha);
    }

    @Test
    void defeitoDeCalibracaoGeraLeituraQueAValidacaoRejeita() {
        // Arrange
        SensorPluviometro sensor = new SensorPluviometro("PLU-05", "Pluviômetro Praça", "Zona Sul", new Random(7));

        // Act
        sensor.simularDefeitoDeCalibracao();
        excecao.DadosInvalidosException erro = assertThrows(excecao.DadosInvalidosException.class,
                () -> sensor.lerMedicao(AGORA));

        // Assert: responde (não é falha de fonte), mas com valor impossível
        assertEquals("Medição de chuva negativa: -15,0 mm/h.", erro.getMessage());
        assertTrue(sensor.estaFuncionando());
        assertEquals(StatusComponente.DEGRADADO, sensor.getStatus());
    }

    @Test
    void restabelecerCorrigeDefeitoDeCalibracao() throws FonteIndisponivelException {
        // Arrange
        SensorUmidadeSolo sensor = new SensorUmidadeSolo("UMI-05", "Sonda", "Zona Sul", new Random(8));
        sensor.simularDefeitoDeCalibracao();

        // Act
        sensor.restabelecer();
        Medicao medicao = sensor.lerMedicao(AGORA);

        // Assert
        assertFalse(sensor.estaDescalibrado());
        assertEquals(StatusComponente.OPERACIONAL, sensor.getStatus());
        assertTrue(medicao.valor() >= 0);
    }

    @Test
    void pontoComSensorDescalibradoFicaDegradado() {
        // Arrange
        SensorPluviometro pluviometro = new SensorPluviometro("PLU-06", "Pluviômetro", "Zona Sul");
        PontoMonitoramento ponto = new PontoMonitoramento("PT-06", "Estação", "Zona Sul");
        ponto.adicionarSensor(pluviometro);

        // Act
        pluviometro.simularDefeitoDeCalibracao();

        // Assert: responde, mas com leituras impossíveis
        assertEquals(StatusComponente.DEGRADADO, ponto.getStatus());
    }

    @Test
    void falhaTemPrioridadeSobreDefeito() {
        // Arrange
        SensorNivelCorrego sensor = new SensorNivelCorrego("NIV-05", "Régua", "Zona Sul");
        sensor.simularDefeitoDeCalibracao();

        // Act
        sensor.simularFalha();

        // Assert: sem resposta é mais grave que resposta errada
        assertThrows(FonteIndisponivelException.class, sensor::lerValor);
        assertEquals(StatusComponente.INDISPONIVEL, sensor.getStatus());
    }

    @Test
    void entradasNulasLancamDadosInvalidos() {
        // Arrange
        PontoMonitoramento ponto = new PontoMonitoramento("PT-05", "Estação", "Zona Sul");

        // Act / Assert
        assertThrows(excecao.DadosInvalidosException.class, () -> ponto.adicionarSensor(null));
        assertThrows(excecao.DadosInvalidosException.class,
                () -> new SensorNivelCorrego("NIV-06", "Régua", "Zona Sul", null));
    }

    @Test
    void pontoRejeitaSensorComIdRepetido() {
        // Arrange
        PontoMonitoramento ponto = new PontoMonitoramento("PT-01", "Estação", "Zona Leste");
        ponto.adicionarSensor(new SensorNivelCorrego("NIV-01", "Régua A", "Zona Leste"));

        // Act / Assert
        assertThrows(excecao.DadosInvalidosException.class,
                () -> ponto.adicionarSensor(new SensorNivelCorrego("NIV-01", "Régua B", "Zona Leste")));
    }

    @Test
    void componenteExigeIdNomeERegiao() {
        // Act / Assert
        assertThrows(excecao.DadosInvalidosException.class,
                () -> new SensorNivelCorrego("", "Régua", "Zona Leste"));
        assertThrows(excecao.DadosInvalidosException.class,
                () -> new PontoMonitoramento("PT-01", "Estação", null));
    }
}
