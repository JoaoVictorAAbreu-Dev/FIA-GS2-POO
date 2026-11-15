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
 * 80 mm/h às 14h); o córrego sobe com atraso e passa da margem (3,0 m) entre
 * 15h e 20h, depois que a chuva já diminuiu; e o solo fica saturado a partir
 * das 14h e continua encharcado à noite.</p>
 */
public class Simulacao24Horas {

    public static final int HORAS = 24;

    // Valor de cada sensor em cada hora do dia (posição 0 = 00h, posição 23 = 23h).
    static final double[] CHUVA = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 15, 30, 45, 65, 80, 70, 45, 25, 10, 3, 0, 0, 0, 0};
    static final double[] NIVEL_CORREGO = {
            1.0, 1.0, 0.9, 0.9, 0.9, 0.9, 1.0, 1.0, 1.0, 1.0, 1.1, 1.3, 1.6, 2.0, 2.5, 3.0, 3.5, 3.6, 3.4, 3.0, 2.6,
            2.2, 1.9, 1.7};
    static final double[] UMIDADE_SOLO = {
            58, 58, 57, 57, 56, 56, 55, 55, 55, 57, 62, 70, 78, 85, 92, 96, 97, 97, 96, 95, 94, 92, 90, 88};

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
                new SensorRoteirizado("NIV-01", TipoSensor.NIVEL_CORREGO, NIVEL_CORREGO),
                new SensorRoteirizado("UMI-01", TipoSensor.UMIDADE_SOLO, UMIDADE_SOLO));

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
