package dominio;

/**
 * Qualquer elemento físico monitorado no território (sensor, ponto de
 * monitoramento...). Cada filho sabe verificar o próprio estado de um jeito
 * diferente, por isso {@link #verificarStatus()} é abstrato.
 */
public abstract class ComponenteTerritorial {

    private final String id;
    private final String nome;
    private final String regiao;
    // Único campo mutável: guarda o resultado da última verificação.
    private StatusComponente status;

    protected ComponenteTerritorial(String id, String nome, String regiao) {
        // exigirTexto rejeita nulo/vazio e já devolve o texto sem espaços nas pontas.
        this.id = Validacoes.exigirTexto(id, "id");
        this.nome = Validacoes.exigirTexto(nome, "nome");
        this.regiao = Validacoes.exigirTexto(regiao, "região");
        // Todo componente nasce operacional; as subclasses ajustam ao verificar.
        this.status = StatusComponente.OPERACIONAL;
    }

    /**
     * Consulta o estado real do componente, atualiza {@link #getStatus()} e o
     * devolve.
     */
    public abstract StatusComponente verificarStatus();

    /** Só as subclasses mudam o status, e sempre a partir de uma verificação. */
    protected final void atualizarStatus(StatusComponente novoStatus) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("status não pode ser nulo");
        }
        this.status = novoStatus;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getRegiao() {
        return regiao;
    }

    /** Último status verificado (não consulta o componente de novo). */
    public StatusComponente getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return id + " - " + nome + " (" + regiao + ") [" + getStatus() + "]";
    }
}
