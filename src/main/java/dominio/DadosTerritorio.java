package dominio;

import excecao.DadosInvalidosException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dados do território monitorado e seus limiares de risco.
 *
 * <p>Não há setters públicos: população e limiares só mudam por
 * {@link #alterarLimiar} e {@link #alterarPopulacaoEstimada}, que exigem o
 * código de acesso da coordenação e validam o novo valor antes de gravar. Se
 * algo for inválido, nada é alterado.</p>
 *
 * <p>Os limiares iniciais normalmente vêm de
 * {@code servico.ConfiguracaoSistema#getLimiaresPadrao()}.</p>
 */
public class DadosTerritorio {

    private static final int TAMANHO_MINIMO_CODIGO = 4;

    private final String nome;
    private final String regiao;
    private int populacaoEstimada;
    // Limiar de ATENÇÃO por tipo de sensor; os demais níveis são calculados em NivelAlerta.
    private final Map<TipoSensor, Double> limiares = new EnumMap<>(TipoSensor.class);
    // Guardado em bytes para a comparação em tempo constante de exigirCodigoValido.
    private final byte[] codigoAcesso;

    /**
     * @param limiaresIniciais limiar de ATENÇÃO para cada {@link TipoSensor}; todos os tipos são obrigatórios
     * @throws DadosInvalidosException se algum dado for inválido
     */
    public DadosTerritorio(String nome, String regiao, int populacaoEstimada, String codigoAcesso,
                           Map<TipoSensor, Double> limiaresIniciais) {
        this.nome = Validacoes.exigirTexto(nome, "nome do território");
        this.regiao = Validacoes.exigirTexto(regiao, "região");
        validarPopulacao(populacaoEstimada);
        this.populacaoEstimada = populacaoEstimada;
        if (codigoAcesso == null || codigoAcesso.strip().length() < TAMANHO_MINIMO_CODIGO) {
            throw new DadosInvalidosException("O código de acesso deve ter pelo menos "
                    + TAMANHO_MINIMO_CODIGO + " caracteres.");
        }
        this.codigoAcesso = codigoAcesso.strip().getBytes(StandardCharsets.UTF_8);
        if (limiaresIniciais == null) {
            throw new DadosInvalidosException("Informe os limiares iniciais do território.");
        }
        // Percorre todos os tipos do enum (e não o mapa recebido) para garantir que nenhum ficou sem limiar.
        for (TipoSensor tipo : TipoSensor.values()) {
            Double limiar = limiaresIniciais.get(tipo);
            if (limiar == null) {
                throw new DadosInvalidosException("Falta o limiar inicial de " + tipo.getDescricao() + ".");
            }
            tipo.validarLimiar(limiar);
            limiares.put(tipo, limiar);
        }
    }

    /**
     * Altera o limiar de ATENÇÃO de um tipo de sensor.
     *
     * @throws DadosInvalidosException se o código estiver errado ou o limiar for inválido
     */
    public void alterarLimiar(String codigoInformado, TipoSensor tipo, double novoLimiar) {
        // Valida tudo antes de gravar: se algo falhar, o limiar antigo continua valendo.
        exigirCodigoValido(codigoInformado);
        if (tipo == null) {
            throw new DadosInvalidosException("Informe o tipo de sensor do limiar.");
        }
        tipo.validarLimiar(novoLimiar);
        limiares.put(tipo, novoLimiar);
    }

    /** @throws DadosInvalidosException se o código estiver errado ou a população for negativa */
    public void alterarPopulacaoEstimada(String codigoInformado, int novaPopulacao) {
        exigirCodigoValido(codigoInformado);
        validarPopulacao(novaPopulacao);
        this.populacaoEstimada = novaPopulacao;
    }

    /** Limiar de ATENÇÃO atual do tipo informado. */
    public double getLimiar(TipoSensor tipo) {
        if (tipo == null) {
            throw new DadosInvalidosException("Informe o tipo de sensor para consultar o limiar.");
        }
        return limiares.get(tipo);
    }

    /** Cópia somente leitura dos limiares atuais. */
    public Map<TipoSensor, Double> getLimiares() {
        return Collections.unmodifiableMap(new EnumMap<>(limiares));
    }

    public String getNome() {
        return nome;
    }

    public String getRegiao() {
        return regiao;
    }

    public int getPopulacaoEstimada() {
        return populacaoEstimada;
    }

    private void exigirCodigoValido(String codigoInformado) {
        // Código nulo vira array vazio, que nunca coincide com o código cadastrado (mínimo de 4 caracteres).
        byte[] informado = codigoInformado == null
                ? new byte[0]
                : codigoInformado.strip().getBytes(StandardCharsets.UTF_8);
        // Comparação em tempo constante: não revela quantos caracteres coincidiram.
        if (!MessageDigest.isEqual(codigoAcesso, informado)) {
            throw new DadosInvalidosException("Código de acesso inválido: alteração não autorizada.");
        }
    }

    private static void validarPopulacao(int populacao) {
        if (populacao < 0) {
            throw new DadosInvalidosException("A população estimada não pode ser negativa; valor recebido: "
                    + populacao);
        }
    }
}
