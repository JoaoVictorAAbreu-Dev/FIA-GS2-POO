package dominio;

import excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Forma de prever o próximo valor de um sensor a partir do seu histórico.
 *
 * <p>Contrato comum a todas as estratégias:</p>
 * <ul>
 *   <li>o histórico deve ser de <b>um único sensor</b>; pode vir em qualquer
 *       ordem (é ordenado por data/hora antes do cálculo);</li>
 *   <li>histórico nulo, com elementos nulos ou misturando sensores/tipos é erro
 *       de uso e lança {@link DadosInvalidosException};</li>
 *   <li>histórico com menos de {@link #getMinimoLeituras()} leituras <b>não</b> é
 *       erro: a estratégia "se cala" e devolve um resultado sem valor
 *       ({@link ResultadoPrevisao#temPrevisao()} = false), explicando o motivo.</li>
 * </ul>
 */
public abstract class EstrategiaPrevisao {

    // Com menos de 2 leituras nenhuma estratégia consegue calcular média ou tendência.
    private static final int MINIMO_ABSOLUTO = 2;

    private final String nome;
    private final int minimoLeituras;

    protected EstrategiaPrevisao(String nome, int minimoLeituras) {
        this.nome = Validacoes.exigirTexto(nome, "nome da estratégia");
        if (minimoLeituras < MINIMO_ABSOLUTO) {
            throw new DadosInvalidosException("A estratégia " + nome + " precisa de pelo menos "
                    + MINIMO_ABSOLUTO + " leituras; configurado: " + minimoLeituras);
        }
        this.minimoLeituras = minimoLeituras;
    }

    /** Calcula a previsão para o histórico de um sensor (ver contrato na classe). */
    public abstract ResultadoPrevisao prever(List<Medicao> historico);

    /** Descrição para o menu; as filhas acrescentam seus parâmetros. */
    public String descrever() {
        return nome + " (mínimo de " + minimoLeituras + " leituras)";
    }

    /**
     * Valida o histórico segundo o contrato e devolve uma cópia em ordem
     * cronológica (da leitura mais antiga para a mais recente).
     */
    protected final List<Medicao> validarEOrdenar(List<Medicao> historico) {
        if (historico == null) {
            throw new DadosInvalidosException("O histórico para a previsão não foi informado.");
        }
        // Copia para não alterar (nem depender de) a lista de quem chamou.
        List<Medicao> ordenado = new ArrayList<>(historico.size());
        for (Medicao medicao : historico) {
            if (medicao == null) {
                throw new DadosInvalidosException("O histórico contém uma medição nula.");
            }
            // Basta comparar cada medição com a primeira: se todas batem com ela, batem entre si.
            if (!ordenado.isEmpty()) {
                Medicao primeira = ordenado.get(0);
                if (!primeira.sensorId().equals(medicao.sensorId())) {
                    throw new DadosInvalidosException("O histórico mistura os sensores " + primeira.sensorId()
                            + " e " + medicao.sensorId() + "; a previsão é feita por sensor.");
                }
                if (primeira.tipo() != medicao.tipo()) {
                    throw new DadosInvalidosException("O histórico do sensor " + primeira.sensorId()
                            + " mistura os tipos " + primeira.tipo().getDescricao() + " e "
                            + medicao.tipo().getDescricao() + "; cada sensor mede uma única grandeza.");
                }
            }
            ordenado.add(medicao);
        }
        ordenado.sort(Comparator.comparing(Medicao::dataHora));
        return ordenado;
    }

    /** {@code true} se o histórico (já validado) permite prever. */
    protected final boolean temLeiturasSuficientes(List<Medicao> ordenado) {
        return ordenado.size() >= minimoLeituras;
    }

    /** Resultado "sem previsão" padronizado, com a explicação para o usuário. */
    protected final ResultadoPrevisao dadosInsuficientes(List<Medicao> ordenado) {
        String sensorId = ordenado.isEmpty() ? null : ordenado.get(0).sensorId();
        TipoSensor tipo = ordenado.isEmpty() ? null : ordenado.get(0).tipo();
        return ResultadoPrevisao.dadosInsuficientes(nome, sensorId, tipo, ordenado.size(), minimoLeituras);
    }

    /** Resultado com valor, já trazido para a faixa física do tipo. */
    protected final ResultadoPrevisao comValor(List<Medicao> ordenado, double valor, int leiturasUsadas,
                                               String interpretacao) {
        // A leitura mais recente fornece sensor e tipo (todas são iguais após a validação).
        Medicao referencia = ordenado.get(ordenado.size() - 1);
        double ajustado = referencia.tipo().limitarAFaixa(valor);
        return ResultadoPrevisao.comValor(nome, referencia.sensorId(), referencia.tipo(), ajustado,
                leiturasUsadas, interpretacao);
    }

    /** Média aritmética dos valores de um trecho do histórico (não vazio). */
    protected static double media(List<Medicao> trecho) {
        return trecho.stream().mapToDouble(Medicao::valor).average()
                .orElseThrow(() -> new IllegalStateException("trecho vazio"));
    }

    public String getNome() {
        return nome;
    }

    public int getMinimoLeituras() {
        return minimoLeituras;
    }

    @Override
    public String toString() {
        return descrever();
    }
}
