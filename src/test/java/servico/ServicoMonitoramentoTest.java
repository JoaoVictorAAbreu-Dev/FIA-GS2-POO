package servico;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.SensorPluviometro;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistencia.AlertaDAO;
import persistencia.MedicaoDAO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Testa a lógica de alerta com os DAOs mockados: nenhum banco é usado aqui. */
@ExtendWith(MockitoExtension.class)
class ServicoMonitoramentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 14, 12, 0);

    @Mock
    private MedicaoDAO medicaoDAO;

    @Mock
    private AlertaDAO alertaDAO;

    private ServicoMonitoramento servico;

    @BeforeEach
    void criarServico() {
        ConfiguracaoSistema config = ConfiguracaoSistema.getInstancia();
        config.restaurarPadroes();
        // Limiar padrão de chuva: 30 mm/h → ATENÇÃO 30, ALERTA 45, CRÍTICO 60.
        DadosTerritorio territorio = new DadosTerritorio("Zona Leste", "São Paulo", 1000, "1234",
                config.getLimiaresPadrao());
        servico = new ServicoMonitoramento(medicaoDAO, alertaDAO, new FabricaPrevisao(config), territorio,
                () -> AGORA);
    }

    @AfterEach
    void restaurarConfiguracao() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
    }

    @Test
    void previsaoAcimaDoLimiteCriticoGravaAlertaCritico() throws FonteIndisponivelException {
        // Arrange: média das 5 últimas = (55+60+65+60+60)/5 = 60 → CRÍTICO
        when(medicaoDAO.buscarUltimasPorSensor("PLU-01", 5)).thenReturn(chuva(55, 60, 65, 60, 60));
        when(alertaDAO.inserir(any())).thenAnswer(chamada -> chamada.<Alerta>getArgument(0).comId(1L));

        // Act
        AvaliacaoPrevisao avaliacao = servico.preverEAvaliar("PLU-01", TipoPrevisao.MEDIA_MOVEL);

        // Assert
        ArgumentCaptor<Alerta> gravado = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaDAO).inserir(gravado.capture());
        assertEquals(NivelAlerta.CRITICO, gravado.getValue().nivel());
        assertEquals(60.0, gravado.getValue().valorPrevisto(), 1e-9);
        assertEquals(60.0, gravado.getValue().limiteUltrapassado(), 1e-9);
        assertEquals(1L, avaliacao.alerta().orElseThrow().id());
    }

    @Test
    void previsaoAbaixoDoLimiarNaoGravaAlerta() throws FonteIndisponivelException {
        // Arrange: média = 10 mm/h, abaixo do limiar de 30
        when(medicaoDAO.buscarUltimasPorSensor("PLU-01", 5)).thenReturn(chuva(10, 10, 10, 10, 10));

        // Act
        AvaliacaoPrevisao avaliacao = servico.preverEAvaliar("PLU-01", TipoPrevisao.MEDIA_MOVEL);

        // Assert
        assertTrue(avaliacao.previsao().temPrevisao());
        assertFalse(avaliacao.gerouAlerta());
        verify(alertaDAO, never()).inserir(any());
    }

    @Test
    void historicoInsuficienteNaoPreveNemGravaAlerta() throws FonteIndisponivelException {
        // Arrange: só 3 leituras, mesmo que altíssimas; a média móvel exige 5
        when(medicaoDAO.buscarUltimasPorSensor("PLU-01", 5)).thenReturn(chuva(200, 200, 200));

        // Act
        AvaliacaoPrevisao avaliacao = servico.preverEAvaliar("PLU-01", TipoPrevisao.MEDIA_MOVEL);

        // Assert: o sistema se cala
        assertFalse(avaliacao.previsao().temPrevisao());
        assertFalse(avaliacao.gerouAlerta());
        verify(alertaDAO, never()).inserir(any());
    }

    @Test
    void comparacaoBuscaODobroDeLeiturasDaConfiguracao() throws FonteIndisponivelException {
        // Arrange: 3 leituras por período (padrão) → 6 do banco; 20 → 40 projeta 60 (CRÍTICO)
        when(medicaoDAO.buscarUltimasPorSensor("PLU-01", 6)).thenReturn(chuva(10, 20, 30, 30, 40, 50));
        when(alertaDAO.inserir(any())).thenAnswer(chamada -> chamada.<Alerta>getArgument(0).comId(2L));

        // Act
        AvaliacaoPrevisao avaliacao = servico.preverEAvaliar("PLU-01", TipoPrevisao.COMPARACAO);

        // Assert
        assertEquals(NivelAlerta.CRITICO, avaliacao.alerta().orElseThrow().nivel());
        assertEquals(60.0, avaliacao.previsao().valorPrevisto().getAsDouble(), 1e-9);
    }

    @Test
    void leituraValidaEGravadaComHorarioDoRelogio() throws FonteIndisponivelException {
        // Arrange
        SensorPluviometro sensor = new SensorPluviometro("PLU-01", "Pluviômetro", "Zona Leste", new Random(2));
        when(medicaoDAO.inserir(any())).thenAnswer(chamada -> chamada.<Medicao>getArgument(0).comId(7L));

        // Act
        Medicao gravada = servico.registrarLeitura(sensor);

        // Assert
        assertEquals(7L, gravada.id());
        assertEquals("PLU-01", gravada.sensorId());
        assertEquals(AGORA, gravada.dataHora());
    }

    @Test
    void leituraImpossivelNaoEGravada() {
        // Arrange
        SensorPluviometro sensor = new SensorPluviometro("PLU-01", "Pluviômetro", "Zona Leste", new Random(2));
        sensor.simularDefeitoDeCalibracao();

        // Act / Assert
        assertThrows(DadosInvalidosException.class, () -> servico.registrarLeitura(sensor));
        verifyNoInteractions(medicaoDAO);
    }

    @Test
    void sensorEmFalhaNaoGravaNada() {
        // Arrange
        SensorPluviometro sensor = new SensorPluviometro("PLU-01", "Pluviômetro", "Zona Leste", new Random(2));
        sensor.simularFalha();

        // Act / Assert
        assertThrows(FonteIndisponivelException.class, () -> servico.registrarLeitura(sensor));
        verifyNoInteractions(medicaoDAO);
    }

    @Test
    void falhaDoBancoChegaAoChamadorComoFonteIndisponivel() throws FonteIndisponivelException {
        // Arrange
        when(medicaoDAO.buscarUltimasPorSensor("PLU-01", 5))
                .thenThrow(new FonteIndisponivelException("banco de dados", "Banco fora do ar"));

        // Act
        FonteIndisponivelException erro = assertThrows(FonteIndisponivelException.class,
                () -> servico.preverEAvaliar("PLU-01", TipoPrevisao.MEDIA_MOVEL));

        // Assert
        assertEquals("banco de dados", erro.getIdFonte());
        verifyNoInteractions(alertaDAO);
    }

    /** Histórico de chuva com leituras de hora em hora terminando em AGORA. */
    private static List<Medicao> chuva(double... valores) {
        List<Medicao> historico = new ArrayList<>();
        for (int i = 0; i < valores.length; i++) {
            LocalDateTime dataHora = AGORA.minusHours(valores.length - 1L - i);
            historico.add(Medicao.nova("PLU-01", TipoSensor.PRECIPITACAO, valores[i], dataHora));
        }
        return historico;
    }
}
