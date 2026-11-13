package dominio;

import java.util.List;

/**
 * Prevê o próximo valor como a média das últimas N leituras do sensor.
 *
 * <p>Dados insuficientes: com menos de N leituras, não prevê (N é também o
 * mínimo exigido). Estável e fácil de explicar, mas reage devagar a mudanças
 * bruscas.</p>
 */
public class PrevisaoMediaMovel extends EstrategiaPrevisao {

    public static final String NOME = "Média móvel";

    private final int janela;

    /** @param janela quantas leituras recentes entram na média (mínimo 2) */
    public PrevisaoMediaMovel(int janela) {
        super(NOME, janela);
        this.janela = janela;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) {
        List<Medicao> ordenado = validarEOrdenar(historico);
        if (!temLeiturasSuficientes(ordenado)) {
            return dadosInsuficientes(ordenado);
        }
        List<Medicao> recentes = ordenado.subList(ordenado.size() - janela, ordenado.size());
        double previsto = media(recentes);
        TipoSensor tipo = recentes.get(0).tipo();
        String interpretacao = "Média das últimas " + janela + " leituras: " + tipo.formatar(previsto)
                + ". Se o padrão recente se mantiver, a próxima leitura deve ficar perto desse valor.";
        return comValor(ordenado, previsto, janela, interpretacao);
    }

    @Override
    public String descrever() {
        return super.descrever() + ": média das últimas " + janela + " leituras";
    }

    public int getJanela() {
        return janela;
    }
}
