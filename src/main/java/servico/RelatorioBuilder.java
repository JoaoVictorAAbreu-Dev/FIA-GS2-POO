package servico;

import dominio.Alerta;
import dominio.ComponenteTerritorial;
import dominio.DadosTerritorio;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder do relatório de status completo (opção 6 do menu).
 *
 * <p>Por que Builder aqui: o relatório tem várias seções opcionais (território,
 * limiares, componentes, configuração, alertas recentes) e quem chama escolhe
 * quais entram e em que ordem. Um construtor com todos esses parâmetros, muitos
 * deles opcionais, seria difícil de ler; com o Builder cada seção é um passo
 * com nome, e {@link #construir()} monta o texto final.</p>
 *
 * <pre>{@code
 * String texto = new RelatorioBuilder("Status completo")
 *         .comTerritorio(territorio)
 *         .comLimiares(territorio)
 *         .comComponentes(componentes)
 *         .construir();
 * }</pre>
 */
public class RelatorioBuilder {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String titulo;
    // Cada seção já formatada, na ordem em que os métodos foram chamados.
    private final List<String> secoes = new ArrayList<>();

    public RelatorioBuilder(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new DadosInvalidosException("Informe o título do relatório.");
        }
        this.titulo = titulo.strip();
    }

    /** Nome, região e população estimada do território. */
    public RelatorioBuilder comTerritorio(DadosTerritorio territorio) {
        exigir(territorio, "o território");
        secoes.add("Território: " + territorio.getNome() + " (" + territorio.getRegiao() + "), população estimada "
                + territorio.getPopulacaoEstimada());
        return this;
    }

    /** Limite de cada nível de alerta para cada tipo de sensor. */
    public RelatorioBuilder comLimiares(DadosTerritorio territorio) {
        exigir(territorio, "o território");
        StringBuilder secao = new StringBuilder("Limiares por nível:");
        for (TipoSensor tipo : TipoSensor.values()) {
            double limiar = territorio.getLimiar(tipo);
            secao.append("\n  ").append(tipo.getDescricao()).append(':');
            for (NivelAlerta nivel : NivelAlerta.values()) {
                secao.append(' ').append(nivel.getRotulo()).append(" ≥ ").append(tipo.formatar(nivel.limite(limiar, tipo)));
            }
        }
        secoes.add(secao.toString());
        return this;
    }

    /** Status atual de cada componente (pontos e sensores). */
    public RelatorioBuilder comComponentes(List<? extends ComponenteTerritorial> componentes) {
        exigir(componentes, "a lista de componentes");
        StringBuilder secao = new StringBuilder("Componentes:");
        for (ComponenteTerritorial componente : componentes) {
            secao.append("\n  ").append(componente).append(" - ").append(componente.getStatus().getDescricao());
        }
        secoes.add(secao.toString());
        return this;
    }

    /** Parâmetros atuais das previsões. */
    public RelatorioBuilder comConfiguracao(ConfiguracaoSistema configuracao) {
        exigir(configuracao, "a configuração");
        secoes.add("Previsões: janela da média móvel " + configuracao.getJanelaMediaMovel()
                + ", leituras por período da comparação " + configuracao.getLeiturasPorPeriodoComparacao()
                + ", estratégia padrão " + configuracao.getTipoPrevisaoPadrao().getCodigo());
        return this;
    }

    /** Alertas mais recentes, um por linha, ou um aviso se não houver nenhum. */
    public RelatorioBuilder comAlertasRecentes(List<Alerta> alertas) {
        exigir(alertas, "a lista de alertas");
        StringBuilder secao = new StringBuilder("Alertas recentes:");
        if (alertas.isEmpty()) {
            secao.append("\n  Nenhum alerta emitido.");
        }
        for (Alerta alerta : alertas) {
            secao.append("\n  ").append(alerta.dataHora().format(DATA_HORA)).append("  [")
                    .append(alerta.nivel().getRotulo()).append("] ").append(alerta.sensorId()).append(": ")
                    .append(alerta.tipo().formatar(alerta.valorPrevisto())).append(" previsto, limite ")
                    .append(alerta.tipo().formatar(alerta.limiteUltrapassado()));
        }
        secoes.add(secao.toString());
        return this;
    }

    /**
     * Monta o texto final: título e seções separadas por uma linha em branco.
     *
     * @throws DadosInvalidosException se nenhuma seção foi adicionada
     */
    public String construir() {
        if (secoes.isEmpty()) {
            throw new DadosInvalidosException("O relatório \"" + titulo + "\" precisa de pelo menos uma seção.");
        }
        return "===== " + titulo + " =====\n" + String.join("\n\n", secoes);
    }

    private static void exigir(Object valor, String descricao) {
        if (valor == null) {
            throw new DadosInvalidosException("Informe " + descricao + " para o relatório.");
        }
    }
}
