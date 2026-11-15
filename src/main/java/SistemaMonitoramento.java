import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.PontoMonitoramento;
import dominio.ResultadoPrevisao;
import dominio.SensorConsumoAgua;
import dominio.SensorPluviometro;
import dominio.SensorSimulado;
import dominio.SensorTemperatura;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import persistencia.AlertaDAO;
import persistencia.FabricaConexao;
import persistencia.MedicaoDAO;
import servico.AvaliacaoPrevisao;
import servico.ConfiguracaoSistema;
import servico.FabricaPrevisao;
import servico.ServicoMonitoramento;
import servico.TipoPrevisao;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Menu de console do sistema. Monta o território de exemplo, prepara o banco
 * e repassa cada opção ao {@link ServicoMonitoramento}.
 *
 * <p>Todo erro esperado é capturado aqui e mostrado com a mensagem da
 * exceção; nenhum catch é vazio.</p>
 */
public class SistemaMonitoramento {

    /** Código da coordenação para alterar dados do território (documentado no README). */
    private static final String CODIGO_ACESSO = "coord2026";

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Scanner entrada = new Scanner(System.in);
    private final ConfiguracaoSistema configuracao = ConfiguracaoSistema.getInstancia();
    private final DadosTerritorio territorio;
    private final PontoMonitoramento ponto;
    private final List<SensorSimulado> sensores;
    private final ServicoMonitoramento servico;

    private SistemaMonitoramento(FabricaConexao fabricaConexao) {
        territorio = new DadosTerritorio("Bacia do Córrego Aricanduva", "Zona Leste", 150_000, CODIGO_ACESSO,
                configuracao.getLimiaresPadrao());
        sensores = List.of(
                new SensorPluviometro("PLU-01", "Pluviômetro do córrego", "Zona Leste"),
                new SensorTemperatura("TMP-01", "Termômetro da praça central", "Zona Leste"),
                new SensorConsumoAgua("AGU-01", "Macromedidor do setor 3", "Zona Leste"));
        ponto = new PontoMonitoramento("PT-01", "Estação Córrego Aricanduva", "Zona Leste");
        sensores.forEach(ponto::adicionarSensor);
        servico = new ServicoMonitoramento(new MedicaoDAO(fabricaConexao), new AlertaDAO(fabricaConexao),
                new FabricaPrevisao(configuracao), territorio);
    }

    public static void main(String[] args) {
        FabricaConexao fabricaConexao = new FabricaConexao();
        try {
            fabricaConexao.criarTabelasSeNecessario();
        } catch (FonteIndisponivelException e) {
            System.out.println("Não foi possível preparar o banco de dados: " + e.getMessage());
            System.out.println("O sistema não pode continuar sem o histórico.");
            return;
        }
        System.out.println("Banco de dados pronto em " + fabricaConexao.getUrl());
        new SistemaMonitoramento(fabricaConexao).executar();
    }

    private void executar() {
        boolean continuar = true;
        while (continuar) {
            mostrarMenu();
            int opcao = lerInteiro("Escolha uma opção: ");
            try {
                continuar = executarOpcao(opcao);
            } catch (FonteIndisponivelException e) {
                System.out.println("Fonte de dados indisponível (" + e.getIdFonte() + "): " + e.getMessage());
            } catch (DadosInvalidosException e) {
                System.out.println("Dados inválidos: " + e.getMessage());
            }
        }
        System.out.println("Até logo! O histórico continua salvo no banco.");
    }

    private void mostrarMenu() {
        System.out.println();
        System.out.println("===== Territory Predictive Insights: " + territorio.getNome() + " =====");
        System.out.println("1. Verificar sensores (ler e gravar medições)");
        System.out.println("2. Rodar previsões e emitir alertas");
        System.out.println("3. Ver histórico de medições de um sensor");
        System.out.println("4. Ver alertas emitidos");
        System.out.println("5. Gerenciar dados do território");
        System.out.println("6. Status completo");
        System.out.println("7. Simular problema em um sensor");
        System.out.println("0. Sair");
    }

    /** @return {@code false} quando o usuário escolhe sair */
    private boolean executarOpcao(int opcao) throws FonteIndisponivelException {
        switch (opcao) {
            case 1 -> verificarSensores();
            case 2 -> rodarPrevisoes();
            case 3 -> verHistorico();
            case 4 -> verAlertas();
            case 5 -> gerenciarTerritorio();
            case 6 -> mostrarStatusCompleto();
            case 7 -> simularProblema();
            case 0 -> {
                return false;
            }
            default -> System.out.println("Opção inválida. Digite um número de 0 a 7.");
        }
        return true;
    }

    // ---------- 1. Verificar sensores ----------

    private void verificarSensores() {
        int rodadas = lerInteiro("Quantas rodadas de leitura? (a previsão precisa de pelo menos "
                + configuracao.getJanelaMediaMovel() + "): ");
        if (rodadas < 1) {
            System.out.println("Informe pelo menos 1 rodada.");
            return;
        }
        for (int rodada = 1; rodada <= rodadas; rodada++) {
            System.out.println("-- Rodada " + rodada + " --");
            for (SensorSimulado sensor : sensores) {
                lerEGravar(sensor);
            }
        }
        System.out.println("Status do ponto " + ponto.getId() + ": " + ponto.getStatus()
                + " (" + ponto.getStatus().getDescricao() + ")");
    }

    // Um sensor com problema não interrompe a leitura dos demais.
    private void lerEGravar(SensorSimulado sensor) {
        try {
            Medicao medicao = servico.registrarLeitura(sensor);
            System.out.println("  " + sensor.getId() + ": " + medicao.valorFormatado() + " gravada (id "
                    + medicao.id() + ")");
        } catch (FonteIndisponivelException e) {
            System.out.println("  " + sensor.getId() + " sem leitura: " + e.getMessage());
        } catch (DadosInvalidosException e) {
            System.out.println("  " + sensor.getId() + " leitura rejeitada e não gravada: " + e.getMessage());
        }
    }

    // ---------- 2. Previsões ----------

    private void rodarPrevisoes() throws FonteIndisponivelException {
        TipoPrevisao tipo = escolherEstrategia();
        for (SensorSimulado sensor : sensores) {
            AvaliacaoPrevisao avaliacao = servico.preverEAvaliar(sensor.getId(), tipo);
            ResultadoPrevisao previsao = avaliacao.previsao();
            System.out.println();
            System.out.println(sensor.getId() + " (" + sensor.getTipo().getDescricao() + ") - "
                    + previsao.estrategia());
            if (previsao.temPrevisao()) {
                System.out.println("  Previsão: " + sensor.getTipo().formatar(previsao.valorPrevisto().getAsDouble()));
            }
            System.out.println("  " + previsao.interpretacao());
            avaliacao.alerta().ifPresentOrElse(
                    alerta -> System.out.println("  " + alerta.explicacao()),
                    () -> System.out.println("  Sem alerta."));
        }
    }

    private TipoPrevisao escolherEstrategia() {
        TipoPrevisao[] tipos = TipoPrevisao.values();
        FabricaPrevisao fabrica = new FabricaPrevisao(configuracao);
        System.out.println("Estratégias:");
        for (int i = 0; i < tipos.length; i++) {
            System.out.println("  " + (i + 1) + ". " + fabrica.criar(tipos[i]).descrever());
        }
        int escolha = lerInteiro("Escolha (0 = padrão, " + configuracao.getTipoPrevisaoPadrao().getCodigo() + "): ");
        if (escolha < 1 || escolha > tipos.length) {
            return configuracao.getTipoPrevisaoPadrao();
        }
        return tipos[escolha - 1];
    }

    // ---------- 3 e 4. Histórico ----------

    private void verHistorico() throws FonteIndisponivelException {
        String sensorId = lerTexto("Id do sensor (" + idsDosSensores() + "): ");
        List<Medicao> historico = servico.historicoDoSensor(sensorId);
        if (historico.isEmpty()) {
            System.out.println("Nenhuma medição gravada para " + sensorId + ".");
            return;
        }
        System.out.println("Histórico de " + sensorId + " (" + historico.size() + " medições):");
        for (Medicao medicao : historico) {
            System.out.println("  #" + medicao.id() + "  " + medicao.dataHora().format(DATA_HORA) + "  " + medicao.valorFormatado());
        }
    }

    private void verAlertas() throws FonteIndisponivelException {
        int quantidade = lerInteiro("Quantos alertas mostrar? (0 = todos): ");
        List<Alerta> alertas = quantidade == 0 ? servico.alertasEmitidos() : servico.ultimosAlertas(quantidade);
        if (alertas.isEmpty()) {
            System.out.println("Nenhum alerta emitido até agora.");
            return;
        }
        System.out.println("Alertas (" + alertas.size() + "), do mais recente ao mais antigo:");
        for (Alerta alerta : alertas) {
            System.out.println("  #" + alerta.id() + "  " + alerta.dataHora().format(DATA_HORA) + "  " + alerta.explicacao());
        }
    }

    // ---------- 5. Dados do território ----------

    private void gerenciarTerritorio() {
        System.out.println("1. Alterar limiar de um tipo de sensor");
        System.out.println("2. Alterar população estimada");
        System.out.println("3. Configurar previsões");
        switch (lerInteiro("Escolha: ")) {
            case 1 -> alterarLimiar();
            case 2 -> alterarPopulacao();
            case 3 -> configurarPrevisoes();
            default -> System.out.println("Opção inválida.");
        }
    }

    private void alterarLimiar() {
        TipoSensor[] tipos = TipoSensor.values();
        for (int i = 0; i < tipos.length; i++) {
            System.out.println("  " + (i + 1) + ". " + tipos[i].getDescricao() + " (atual: "
                    + tipos[i].formatar(territorio.getLimiar(tipos[i])) + ", máximo: "
                    + tipos[i].formatar(tipos[i].getMaiorLimiarPermitido()) + ")");
        }
        int escolha = lerInteiro("Tipo: ");
        if (escolha < 1 || escolha > tipos.length) {
            System.out.println("Tipo inválido.");
            return;
        }
        double novoLimiar = lerDecimal("Novo limiar de ATENÇÃO: ");
        String codigo = lerTexto("Código de acesso da coordenação: ");
        territorio.alterarLimiar(codigo, tipos[escolha - 1], novoLimiar);
        System.out.println("Limiar alterado.");
    }

    private void alterarPopulacao() {
        int populacao = lerInteiro("Nova população estimada: ");
        String codigo = lerTexto("Código de acesso da coordenação: ");
        territorio.alterarPopulacaoEstimada(codigo, populacao);
        System.out.println("População alterada.");
    }

    private void configurarPrevisoes() {
        configuracao.definirJanelaMediaMovel(lerInteiro("Janela da média móvel (atual "
                + configuracao.getJanelaMediaMovel() + "): "));
        configuracao.definirLeiturasPorPeriodoComparacao(lerInteiro("Leituras por período da comparação (atual "
                + configuracao.getLeiturasPorPeriodoComparacao() + "): "));
        configuracao.definirTipoPrevisaoPadrao(TipoPrevisao.deCodigo(lerTexto("Estratégia padrão ("
                + codigosDasEstrategias() + "): ")));
        System.out.println("Configuração atualizada.");
    }

    // ---------- 6. Status ----------

    private void mostrarStatusCompleto() {
        System.out.println("Território: " + territorio.getNome() + " (" + territorio.getRegiao() + "), população estimada "
                + territorio.getPopulacaoEstimada());
        System.out.println("Limiares por nível:");
        for (TipoSensor tipo : TipoSensor.values()) {
            double limiar = territorio.getLimiar(tipo);
            StringBuilder linha = new StringBuilder("  " + tipo.getDescricao() + ":");
            for (NivelAlerta nivel : NivelAlerta.values()) {
                linha.append(' ').append(nivel.getRotulo()).append(" ≥ ").append(tipo.formatar(nivel.limite(limiar, tipo)));
            }
            System.out.println(linha);
        }
        System.out.println("Ponto: " + ponto);
        for (SensorSimulado sensor : sensores) {
            System.out.println("  " + sensor + " - " + sensor.getStatus().getDescricao());
        }
        System.out.println("Previsões: janela " + configuracao.getJanelaMediaMovel() + ", período "
                + configuracao.getLeiturasPorPeriodoComparacao() + ", padrão "
                + configuracao.getTipoPrevisaoPadrao().getCodigo());
    }

    // ---------- 7. Simulação de problemas ----------

    private void simularProblema() {
        SensorSimulado sensor = escolherSensor();
        if (sensor == null) {
            return;
        }
        System.out.println("1. Falha (não responde)");
        System.out.println("2. Defeito de calibração (responde com valor impossível)");
        System.out.println("3. Restabelecer");
        switch (lerInteiro("Escolha: ")) {
            case 1 -> sensor.simularFalha();
            case 2 -> sensor.simularDefeitoDeCalibracao();
            case 3 -> sensor.restabelecer();
            default -> {
                System.out.println("Opção inválida.");
                return;
            }
        }
        System.out.println(sensor.getId() + " agora está " + sensor.getStatus() + ".");
    }

    private SensorSimulado escolherSensor() {
        for (int i = 0; i < sensores.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + sensores.get(i));
        }
        int escolha = lerInteiro("Sensor: ");
        if (escolha < 1 || escolha > sensores.size()) {
            System.out.println("Sensor inválido.");
            return null;
        }
        return sensores.get(escolha - 1);
    }

    // ---------- Entrada ----------

    private int lerInteiro(String pergunta) {
        while (true) {
            String texto = lerTexto(pergunta);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("'" + texto + "' não é um número inteiro. Tente de novo.");
            }
        }
    }

    // Aceita vírgula ou ponto como separador decimal.
    private double lerDecimal(String pergunta) {
        while (true) {
            String texto = lerTexto(pergunta);
            try {
                return Double.parseDouble(texto.replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("'" + texto + "' não é um número. Use, por exemplo, 32,5.");
            }
        }
    }

    private String lerTexto(String pergunta) {
        System.out.print(pergunta);
        if (!entrada.hasNextLine()) {
            // Entrada encerrada (Ctrl+D ou fim de arquivo): não há como continuar o menu.
            System.out.println();
            System.out.println("Entrada encerrada. Saindo; o histórico continua salvo no banco.");
            System.exit(0);
        }
        return entrada.nextLine().strip();
    }

    private String idsDosSensores() {
        return String.join(", ", sensores.stream().map(SensorSimulado::getId).toList());
    }

    private static String codigosDasEstrategias() {
        return String.join(", ", Arrays.stream(TipoPrevisao.values()).map(TipoPrevisao::getCodigo).toList());
    }
}
