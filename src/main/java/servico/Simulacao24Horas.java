package servico;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.Medicao;
import dominio.Sensor;
import dominio.SensorRoteirizado;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import persistencia.AlertaDAO;
import persistencia.FabricaConexao;
import persistencia.MedicaoDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bônus: simula um dia inteiro, com uma leitura por sensor a cada hora, e
 * mostra os alertas aparecendo ao longo do tempo.
 *
 * <p>Usa o mesmo fluxo do sistema real ({@link ServicoMonitoramento}: ler →
 * gravar → buscar histórico → prever → alertar), mas num banco H2 <b>em
 * memória</b> criado só para a simulação. Assim o histórico real não é
 * misturado com dados de um dia inventado. O banco é descartado ao final.</p>
 *
 * <p>O cenário é fixo para ser reproduzível: chuva forte à tarde (pico de
 * 80 mm/h às 14h), calor no meio do dia (40 °C às 15h) e um vazamento de água
 * que começa às 19h.</p>
 */
public class Simulacao24Horas {

    public static final int HORAS = 24;

    // Valor de cada sensor em cada hora do dia (posição 0 = 00h, posição 23 = 23h).
    static final double[] CHUVA = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 15, 30, 45, 65, 80, 70, 45, 25, 10, 3, 0, 0, 0, 0};
    static final double[] TEMPERATURA = {
            22, 21, 21, 20, 20, 21, 23, 25, 28, 31, 33, 35, 37, 38, 39, 40, 39, 37, 34, 31, 28, 26, 24, 23};
    static final double[] CONSUMO_AGUA = {
            60, 55, 50, 50, 55, 65, 90, 110, 115, 105, 95, 90, 95, 100, 95, 90, 95, 105, 110, 140, 160, 170, 175,
            180};

    private final FabricaPrevisao fabricaPrevisao;
    private final DadosTerritorio territorio;

    // Relógio da simulação: avança uma hora a cada passo do laço.
    private LocalDateTime horaAtual;

    public Simulacao24Horas(FabricaPrevisao fabricaPrevisao, DadosTerritorio territorio) {
        if (fabricaPrevisao == null || territorio == null) {
            throw new DadosInvalidosException("A simulação precisa da fábrica de previsões e do território.");
        }
        this.fabricaPrevisao = fabricaPrevisao;
        this.territorio = territorio;
    }

    /**
     * Executa as 24 horas do {@code dia} com a estratégia escolhida.
     *
     * @return uma {@link HoraSimulada} por hora, de 00h a 23h
     * @throws FonteIndisponivelException se o banco da simulação falhar
     */
    public List<HoraSimulada> executar(LocalDate dia, TipoPrevisao tipoPrevisao) throws FonteIndisponivelException {
        if (dia == null || tipoPrevisao == null) {
            throw new DadosInvalidosException("Informe o dia e a estratégia da simulação.");
        }
        // Nome único: duas simulações seguidas nunca compartilham dados.
        FabricaConexao bancoTemporario = new FabricaConexao("jdbc:h2:mem:simulacao-" + UUID.randomUUID());

        // Um banco H2 em memória existe enquanto houver uma conexão aberta; esta o mantém vivo até o fim.
        try (Connection manterBancoAberto = bancoTemporario.abrirConexao()) {
            bancoTemporario.criarTabelasSeNecessario();
            ServicoMonitoramento servico = new ServicoMonitoramento(new MedicaoDAO(bancoTemporario),
                    new AlertaDAO(bancoTemporario), fabricaPrevisao, territorio, () -> horaAtual);
            return simularDia(servico, dia, tipoPrevisao);
        } catch (SQLException e) {
            throw new FonteIndisponivelException(FabricaConexao.ID_FONTE,
                    "Erro ao fechar o banco da simulação: " + e.getMessage(), e);
        }
    }

    private List<HoraSimulada> simularDia(ServicoMonitoramento servico, LocalDate dia, TipoPrevisao tipoPrevisao)
            throws FonteIndisponivelException {
        List<Sensor> sensores = List.of(
                new SensorRoteirizado("PLU-01", TipoSensor.PRECIPITACAO, CHUVA),
                new SensorRoteirizado("TMP-01", TipoSensor.TEMPERATURA, TEMPERATURA),
                new SensorRoteirizado("AGU-01", TipoSensor.CONSUMO_AGUA, CONSUMO_AGUA));

        List<HoraSimulada> horas = new ArrayList<>();
        for (int hora = 0; hora < HORAS; hora++) {
            horaAtual = dia.atTime(hora, 0);
            List<Medicao> leituras = new ArrayList<>();
            List<Alerta> alertas = new ArrayList<>();
            for (Sensor sensor : sensores) {
                leituras.add(servico.registrarLeitura(sensor));
            }
            // Prevê depois de gravar: a leitura desta hora já entra no histórico usado.
            for (Sensor sensor : sensores) {
                servico.preverEAvaliar(sensor.getId(), tipoPrevisao).alerta().ifPresent(alertas::add);
            }
            horas.add(new HoraSimulada(horaAtual, leituras, alertas));
        }
        return horas;
    }
}
