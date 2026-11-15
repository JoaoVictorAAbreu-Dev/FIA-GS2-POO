package servico;

import dominio.TipoSensor;
import excecao.DadosInvalidosException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Configuração única do sistema (Singleton): limiares padrão e parâmetros das
 * previsões.
 *
 * <p>Por que Singleton: fábrica, serviço e menu precisam enxergar <b>os mesmos</b>
 * parâmetros; se cada um criasse sua configuração, mudar a janela da média
 * móvel no menu não afetaria as previsões. Construtor privado + inicialização
 * preguiçosa e thread-safe pelo idiom "holder". Os parâmetros alteráveis são
 * {@code volatile}, então uma alteração fica visível para todas as threads
 * (cada setter altera um único campo; não há operação composta a proteger).</p>
 *
 * <p>O que fica aqui: só parâmetros globais. Os limiares padrão são somente
 * leitura e servem para criar o {@link dominio.DadosTerritorio}; a alteração
 * de limiares do território passa pelo código de acesso em
 * {@code DadosTerritorio#alterarLimiar}, não por aqui.</p>
 */
public final class ConfiguracaoSistema {

    public static final int JANELA_MEDIA_MOVEL_PADRAO = 5;
    public static final int LEITURAS_POR_PERIODO_PADRAO = 3;
    public static final TipoPrevisao TIPO_PREVISAO_PADRAO = TipoPrevisao.MEDIA_MOVEL;

    private static final int JANELA_MINIMA = 2;
    private static final int LEITURAS_POR_PERIODO_MINIMAS = 1;

    private static final Map<TipoSensor, Double> LIMIARES_PADRAO;

    static {
        Map<TipoSensor, Double> limiares = new EnumMap<>(TipoSensor.class);
        limiares.put(TipoSensor.PRECIPITACAO, 30.0);  // mm/h: chuva forte, risco de alagamento
        limiares.put(TipoSensor.NIVEL_CORREGO, 2.0);  // m: 1 m abaixo do ponto em que a água sai da margem
        limiares.put(TipoSensor.UMIDADE_SOLO, 80.0);  // %: solo perto da saturação, a chuva passa a escorrer
        LIMIARES_PADRAO = Collections.unmodifiableMap(limiares);
    }

    private volatile int janelaMediaMovel;
    private volatile int leiturasPorPeriodoComparacao;
    private volatile TipoPrevisao tipoPrevisaoPadrao;

    // Privado: ninguém fora da classe cria outra instância.
    private ConfiguracaoSistema() {
        restaurarPadroes();
    }

    // Idiom "holder": a JVM só carrega Holder na primeira chamada a getInstancia(), e o
    // carregamento de classe já é thread-safe, então não precisa de synchronized.
    private static final class Holder {
        private static final ConfiguracaoSistema INSTANCIA = new ConfiguracaoSistema();
    }

    /** Ponto único de acesso à configuração. */
    public static ConfiguracaoSistema getInstancia() {
        return Holder.INSTANCIA;
    }

    /** Volta aos valores de fábrica (útil no menu e para isolar testes). */
    public void restaurarPadroes() {
        janelaMediaMovel = JANELA_MEDIA_MOVEL_PADRAO;
        leiturasPorPeriodoComparacao = LEITURAS_POR_PERIODO_PADRAO;
        tipoPrevisaoPadrao = TIPO_PREVISAO_PADRAO;
    }

    /** Limiar de ATENÇÃO padrão de um tipo de sensor. */
    public double getLimiarPadrao(TipoSensor tipo) {
        if (tipo == null) {
            throw new DadosInvalidosException("Informe o tipo de sensor para consultar o limiar padrão.");
        }
        return LIMIARES_PADRAO.get(tipo);
    }

    /** Todos os limiares padrão (somente leitura), prontos para criar um DadosTerritorio. */
    public Map<TipoSensor, Double> getLimiaresPadrao() {
        return LIMIARES_PADRAO;
    }

    public int getJanelaMediaMovel() {
        return janelaMediaMovel;
    }

    /** @throws DadosInvalidosException se a janela for menor que 2 */
    public void definirJanelaMediaMovel(int janela) {
        if (janela < JANELA_MINIMA) {
            throw new DadosInvalidosException("A janela da média móvel deve ter pelo menos " + JANELA_MINIMA
                    + " leituras; "
                    + "valor recebido: " + janela);
        }
        this.janelaMediaMovel = janela;
    }

    public int getLeiturasPorPeriodoComparacao() {
        return leiturasPorPeriodoComparacao;
    }

    /** @throws DadosInvalidosException se o período for menor que 1 */
    public void definirLeiturasPorPeriodoComparacao(int leituras) {
        if (leituras < LEITURAS_POR_PERIODO_MINIMAS) {
            throw new DadosInvalidosException("Cada período da comparação deve ter pelo menos "
                    + LEITURAS_POR_PERIODO_MINIMAS + " leitura; "
                    + "valor recebido: " + leituras);
        }
        this.leiturasPorPeriodoComparacao = leituras;
    }

    public TipoPrevisao getTipoPrevisaoPadrao() {
        return tipoPrevisaoPadrao;
    }

    public void definirTipoPrevisaoPadrao(TipoPrevisao tipo) {
        if (tipo == null) {
            throw new DadosInvalidosException("Informe a estratégia de previsão padrão.");
        }
        this.tipoPrevisaoPadrao = tipo;
    }
}
