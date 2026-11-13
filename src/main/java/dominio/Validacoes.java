package dominio;

import excecao.DadosInvalidosException;

/** Validações de texto compartilhadas pelas classes do domínio. */
final class Validacoes {

    private Validacoes() {
    }

    /** Retorna o texto sem espaços nas pontas ou lança se estiver nulo/vazio. */
    static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo '" + campo + "' é obrigatório e não pode ficar vazio.");
        }
        return valor.strip();
    }
}
