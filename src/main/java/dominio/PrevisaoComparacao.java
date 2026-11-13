package dominio;

import java.util.List;

/**
 * Compara o período atual (últimas N leituras) com o anterior (N leituras
 * antes dele) e projeta que a variação se repete:
 * {@code previsto = mediaAtual + (mediaAtual - mediaAnterior)}.
 *
 * <p>Dados insuficientes: exige 2×N leituras; com menos, não prevê. O valor
 * projetado é limitado à faixa física do sensor (ex.: chuva nunca negativa).
 * Percebe tendências antes da média móvel, mas exagera quando há um pico
 * isolado.</p>
 */
public class PrevisaoComparacao extends EstrategiaPrevisao {

    public static final String NOME = "Comparação entre períodos";

    private final int leiturasPorPeriodo;

    /** @param leiturasPorPeriodo tamanho de cada período comparado (mínimo 1; exige o dobro no histórico) */
    public PrevisaoComparacao(int leiturasPorPeriodo) {
        super(NOME, 2 * leiturasPorPeriodo);
        this.leiturasPorPeriodo = leiturasPorPeriodo;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) {
        List<Medicao> ordenado = validarEOrdenar(historico);
        if (!temLeiturasSuficientes(ordenado)) {
            return dadosInsuficientes(ordenado);
        }
        int fim = ordenado.size();
        double mediaAtual = media(ordenado.subList(fim - leiturasPorPeriodo, fim));
        double mediaAnterior = media(ordenado.subList(fim - 2 * leiturasPorPeriodo, fim - leiturasPorPeriodo));
        double variacao = mediaAtual - mediaAnterior;
        TipoSensor tipo = ordenado.get(0).tipo();
        double previsto = tipo.limitarAFaixa(mediaAtual + variacao);

        String interpretacao = "Período atual (" + leiturasPorPeriodo + " leituras): " + tipo.formatar(mediaAtual)
                + "; período anterior: " + tipo.formatar(mediaAnterior) + " (" + descreverTendencia(variacao,
                mediaAnterior, tipo) + "). Se a tendência continuar, a próxima leitura deve ficar perto de "
                + tipo.formatar(previsto) + ".";
        return comValor(ordenado, previsto, 2 * leiturasPorPeriodo, interpretacao);
    }

    private static String descreverTendencia(double variacao, double mediaAnterior, TipoSensor tipo) {
        if (variacao == 0) {
            return "estável";
        }
        String sentido = variacao > 0 ? "alta" : "queda";
        if (mediaAnterior == 0) {
            return sentido + " de " + tipo.formatar(Math.abs(variacao));
        }
        double percentual = Math.abs(variacao) / Math.abs(mediaAnterior) * 100;
        return sentido + " de " + Formatos.numero(percentual) + "%";
    }

    @Override
    public String descrever() {
        return super.descrever() + ": compara as últimas " + leiturasPorPeriodo + " leituras com as "
                + leiturasPorPeriodo + " anteriores";
    }

    public int getLeiturasPorPeriodo() {
        return leiturasPorPeriodo;
    }
}
