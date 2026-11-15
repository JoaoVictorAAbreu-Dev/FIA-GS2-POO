package servico;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.NivelAlerta;
import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Executa a simulação com o banco H2 em memória e confere, hora a hora, os
 * níveis calculados à mão a partir do cenário.
 */
class Simulacao24HorasTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 15);

    private Simulacao24Horas simulacao;

    @BeforeEach
    void criarSimulacao() {
        ConfiguracaoSistema config = ConfiguracaoSistema.getInstancia();
        config.restaurarPadroes();
        DadosTerritorio territorio = new DadosTerritorio("Zona Leste", "São Paulo", 1000, "1234",
                config.getLimiaresPadrao());
        simulacao = new Simulacao24Horas(new FabricaPrevisao(config), territorio);
    }

    @AfterEach
    void restaurarConfiguracao() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
    }

    @Test
    void geraUmaHoraPorPassoComTresLeiturasGravadas() throws FonteIndisponivelException {
        // Act
        List<HoraSimulada> horas = simulacao.executar(DIA, TipoPrevisao.MEDIA_MOVEL);

        // Assert
        assertEquals(24, horas.size());
        assertEquals(DIA.atTime(0, 0), horas.get(0).dataHora());
        assertEquals(DIA.atTime(23, 0), horas.get(23).dataHora());
        horas.forEach(hora -> {
            assertEquals(3, hora.leituras().size());
            hora.leituras().forEach(m -> assertNotNull(m.id(), "medição deveria estar gravada"));
            hora.alertas().forEach(a -> assertNotNull(a.id(), "alerta deveria estar gravado"));
        });
    }

    @Test
    void mediaMovelAcompanhaAChuvaComAtraso() throws FonteIndisponivelException {
        // Act
        List<HoraSimulada> horas = simulacao.executar(DIA, TipoPrevisao.MEDIA_MOVEL);

        // Assert: média das 5 últimas leituras; limiar 30 → ATENÇÃO 30, ALERTA 45, CRÍTICO 60
        // 13h: (5+15+30+45+65)/5 = 32 · 14h: 47 · 15h: 58 · 16h: 61 · 17h: 57 · 18h: 46 · 19h: 30,6
        Map<Integer, NivelAlerta> esperado = new TreeMap<>(Map.of(
                13, NivelAlerta.ATENCAO, 14, NivelAlerta.ALERTA, 15, NivelAlerta.ALERTA, 16, NivelAlerta.CRITICO,
                17, NivelAlerta.ALERTA, 18, NivelAlerta.ALERTA, 19, NivelAlerta.ATENCAO));
        assertEquals(esperado, niveisPorHora(horas, "PLU-01"));
    }

    @Test
    void comparacaoAvisaAntesDaMediaMovel() throws FonteIndisponivelException {
        // Act
        List<HoraSimulada> horas = simulacao.executar(DIA, TipoPrevisao.COMPARACAO);
        Map<Integer, NivelAlerta> chuva = niveisPorHora(horas, "PLU-01");

        // Assert: 11h: atual (5+15+30)/3 = 16,67, anterior 0 → 33,3 (ATENÇÃO), 2 horas antes da média móvel
        // 13h: atual (30+45+65)/3 = 46,67, anterior (0+5+15)/3 = 6,67 → 86,67 (CRÍTICO), 3 horas antes da média móvel
        assertEquals(11, chuva.keySet().iterator().next());
        assertEquals(NivelAlerta.ATENCAO, chuva.get(11));
        assertEquals(NivelAlerta.CRITICO, chuva.get(13));
        assertEquals(86.67, valorPrevistoNaHora(horas, "PLU-01", 13), 0.01);
    }

    private static double valorPrevistoNaHora(List<HoraSimulada> horas, String sensorId, int hora) {
        return horas.get(hora).alertas().stream()
                .filter(alerta -> alerta.sensorId().equals(sensorId))
                .findFirst().orElseThrow().valorPrevisto();
    }

    @Test
    void corregoPassaDaMargemDepoisQueAChuvaDiminui() throws FonteIndisponivelException {
        // Act
        List<HoraSimulada> horas = simulacao.executar(DIA, TipoPrevisao.MEDIA_MOVEL);

        // Assert: limiar 2,0 m → ALERTA 2,5 · CRÍTICO 3,0. Média das 5 últimas:
        // 15h: 2,08 · 16h: 2,52 · 17h: 2,92 · 18h: 3,2 · 19h: 3,3 · 20h: 3,22 · 21h: 2,96 · 22h: 2,62 · 23h: 2,28
        Map<Integer, NivelAlerta> esperado = new TreeMap<>(Map.of(
                15, NivelAlerta.ATENCAO, 16, NivelAlerta.ALERTA, 17, NivelAlerta.ALERTA,
                18, NivelAlerta.CRITICO, 19, NivelAlerta.CRITICO, 20, NivelAlerta.CRITICO,
                21, NivelAlerta.ALERTA, 22, NivelAlerta.ALERTA, 23, NivelAlerta.ATENCAO));
        assertEquals(esperado, niveisPorHora(horas, "NIV-01"));
    }

    @Test
    void soloSaturadoContinuaEmAlertaANoite() throws FonteIndisponivelException {
        // Act
        List<HoraSimulada> horas = simulacao.executar(DIA, TipoPrevisao.MEDIA_MOVEL);

        // Assert: limiar 80% → ALERTA 87 · CRÍTICO 94. Média das 5 últimas:
        // 15h: 84,2 · 16h: 89,6 · 17h: 93,4 · 18h: 95,6 · 19h: 96,2 · 20h: 95,8 · 21h: 94,8 · 22h: 93,4 · 23h: 91,8
        Map<Integer, NivelAlerta> esperado = new TreeMap<>(Map.of(
                15, NivelAlerta.ATENCAO, 16, NivelAlerta.ALERTA, 17, NivelAlerta.ALERTA,
                18, NivelAlerta.CRITICO, 19, NivelAlerta.CRITICO, 20, NivelAlerta.CRITICO, 21, NivelAlerta.CRITICO,
                22, NivelAlerta.ALERTA, 23, NivelAlerta.ALERTA));
        assertEquals(esperado, niveisPorHora(horas, "UMI-01"));
    }

    @Test
    void duasSimulacoesSeguidasNaoCompartilhamDados() throws FonteIndisponivelException {
        // Act
        List<HoraSimulada> primeira = simulacao.executar(DIA, TipoPrevisao.MEDIA_MOVEL);
        List<HoraSimulada> segunda = simulacao.executar(DIA, TipoPrevisao.MEDIA_MOVEL);

        // Assert: cada uma começa num banco novo, então os ids recomeçam do 1
        assertEquals(1L, primeira.get(0).leituras().get(0).id());
        assertEquals(1L, segunda.get(0).leituras().get(0).id());
        assertEquals(niveisPorHora(primeira, "PLU-01"), niveisPorHora(segunda, "PLU-01"));
    }

    private static Map<Integer, NivelAlerta> niveisPorHora(List<HoraSimulada> horas, String sensorId) {
        Map<Integer, NivelAlerta> niveis = new TreeMap<>();
        for (HoraSimulada hora : horas) {
            for (Alerta alerta : hora.alertas()) {
                if (alerta.sensorId().equals(sensorId)) {
                    niveis.put(hora.dataHora().getHour(), alerta.nivel());
                }
            }
        }
        return niveis;
    }
}
