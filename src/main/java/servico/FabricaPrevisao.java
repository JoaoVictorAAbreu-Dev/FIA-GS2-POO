package servico;

import dominio.EstrategiaPrevisao;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory das estratégias de previsão: único lugar do sistema que conhece as
 * classes concretas {@link PrevisaoMediaMovel} e {@link PrevisaoComparacao} e
 * seus parâmetros.
 *
 * <p>A escolha não usa {@code if}/{@code switch} nem {@code instanceof}: cada
 * {@link TipoPrevisao} está associado a uma função criadora num
 * {@link EnumMap}. Nova estratégia = nova constante no enum + uma linha em
 * {@link #registrarCriadores()}; serviço e menu não mudam.</p>
 */
public class FabricaPrevisao {

    private final ConfiguracaoSistema configuracao;
    private final Map<TipoPrevisao, Function<ConfiguracaoSistema, EstrategiaPrevisao>> criadores =
            new EnumMap<>(TipoPrevisao.class);

    /** Usa a configuração única do sistema. */
    public FabricaPrevisao() {
        this(ConfiguracaoSistema.getInstancia());
    }

    public FabricaPrevisao(ConfiguracaoSistema configuracao) {
        if (configuracao == null) {
            throw new DadosInvalidosException("A fábrica de previsões precisa da configuração do sistema.");
        }
        this.configuracao = configuracao;
        registrarCriadores();
        for (TipoPrevisao tipo : TipoPrevisao.values()) {
            if (!criadores.containsKey(tipo)) {
                throw new IllegalStateException("Estratégia sem criador registrado na fábrica: " + tipo);
            }
        }
    }

    private void registrarCriadores() {
        criadores.put(TipoPrevisao.MEDIA_MOVEL,
                config -> new PrevisaoMediaMovel(config.getJanelaMediaMovel()));
        criadores.put(TipoPrevisao.COMPARACAO,
                config -> new PrevisaoComparacao(config.getLeiturasPorPeriodoComparacao()));
    }

    /** Cria a estratégia pedida com os parâmetros atuais da configuração. */
    public EstrategiaPrevisao criar(TipoPrevisao tipo) {
        if (tipo == null) {
            throw new DadosInvalidosException("Informe a estratégia de previsão.");
        }
        return criadores.get(tipo).apply(configuracao);
    }

    /**
     * Cria a partir do código textual (ex.: {@code "media-movel"}).
     *
     * @throws DadosInvalidosException se o código não existir
     */
    public EstrategiaPrevisao criar(String codigo) {
        return criar(TipoPrevisao.deCodigo(codigo));
    }

    /** Estratégia definida como padrão na configuração. */
    public EstrategiaPrevisao criarPadrao() {
        return criar(configuracao.getTipoPrevisaoPadrao());
    }

    /** Uma instância de cada estratégia, para rodar e comparar todas. */
    public List<EstrategiaPrevisao> criarTodas() {
        List<EstrategiaPrevisao> todas = new ArrayList<>();
        for (TipoPrevisao tipo : TipoPrevisao.values()) {
            todas.add(criar(tipo));
        }
        return todas;
    }
}
