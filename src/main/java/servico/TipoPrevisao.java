package servico;

import excecao.DadosInvalidosException;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Estratégias de previsão disponíveis, identificadas por um código usado na configuração/menu. */
public enum TipoPrevisao {
    MEDIA_MOVEL("media-movel"),
    COMPARACAO("comparacao");

    private final String codigo;

    TipoPrevisao(String codigo) {
        this.codigo = codigo;
    }

    /** @throws DadosInvalidosException se o código não corresponder a nenhuma estratégia */
    public static TipoPrevisao deCodigo(String codigo) {
        String procurado = codigo == null ? "" : codigo.strip();
        return Arrays.stream(values())
                .filter(tipo -> tipo.codigo.equalsIgnoreCase(procurado))
                .findFirst()
                .orElseThrow(() -> new DadosInvalidosException("Estratégia de previsão desconhecida: '"
                        + procurado + "'. Opções: " + Arrays.stream(values()).map(TipoPrevisao::getCodigo)
                        .collect(Collectors.joining(", ")) + "."));
    }

    public String getCodigo() {
        return codigo;
    }
}
