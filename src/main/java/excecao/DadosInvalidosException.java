package excecao;

/**
 * Lançada quando um dado do território ou da configuração é inválido:
 * medição fora da faixa física do sensor, valor NaN/infinito, população
 * negativa, limiar não positivo, código de acesso incorreto etc.
 *
 * <p>É não verificada (estende {@link RuntimeException}) porque precisa ser
 * lançada pelo construtor compacto do record {@code Medicao}, e o construtor
 * canônico de um record não pode declarar {@code throws}. Mesmo assim, o menu
 * deve capturá-la e mostrar {@link #getMessage()} ao usuário.</p>
 */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }

    public DadosInvalidosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
