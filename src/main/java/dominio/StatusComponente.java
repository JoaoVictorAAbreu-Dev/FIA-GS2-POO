package dominio;

/** Situação operacional de um {@link ComponenteTerritorial}. */
public enum StatusComponente {
    OPERACIONAL("operando normalmente"),
    DEGRADADO("operando com problemas: sensor sem resposta no ponto ou leituras impossíveis"),
    INDISPONIVEL("sem resposta");

    private final String descricao;

    StatusComponente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
