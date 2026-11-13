package dominio;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formatação numérica em pt-BR usada nas mensagens para o usuário (ex.: "-5,0"). */
public final class Formatos {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Formatos() {
    }

    /** Uma casa decimal no mínimo, até duas: 30 → "30,0"; 12.345 → "12,35". */
    public static String numero(double valor) {
        if (Double.isNaN(valor)) {
            return "NaN";
        }
        if (Double.isInfinite(valor)) {
            return valor > 0 ? "+infinito" : "-infinito";
        }
        // DecimalFormat não é thread-safe; criar por chamada é barato para mensagens.
        return new DecimalFormat("0.0#", DecimalFormatSymbols.getInstance(PT_BR)).format(valor);
    }
}
