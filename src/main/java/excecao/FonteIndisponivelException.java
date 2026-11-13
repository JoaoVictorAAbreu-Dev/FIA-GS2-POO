package excecao;

/**
 * Lançada quando uma fonte de dados (sensor ou gerador simulado) não consegue
 * entregar uma leitura.
 *
 * <p>É verificada (estende {@link Exception}): falha de sensor é uma situação
 * esperada em campo, então o compilador obriga quem lê um sensor a decidir o
 * que fazer (avisar no menu, pular o sensor e seguir com os demais etc.).</p>
 */
public class FonteIndisponivelException extends Exception {

    private final String idFonte;

    public FonteIndisponivelException(String idFonte, String mensagem) {
        super(mensagem);
        this.idFonte = idFonte;
    }

    public FonteIndisponivelException(String idFonte, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.idFonte = idFonte;
    }

    /** Identificador do sensor/fonte que falhou, útil para o menu e para logs. */
    public String getIdFonte() {
        return idFonte;
    }
}
