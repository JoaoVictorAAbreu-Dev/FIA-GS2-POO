package dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatosTest {

    @Test
    void formataEmPtBrComUmaOuDuasCasas() {
        // Act / Assert
        assertEquals("30,0", Formatos.numero(30));
        assertEquals("12,35", Formatos.numero(12.345));
        assertEquals("-5,0", Formatos.numero(-5));
    }

    @Test
    void negativoQueArredondaParaZeroNaoMostraSinal() {
        // Act / Assert
        assertEquals("0,0", Formatos.numero(-0.001));
        assertEquals("0,0", Formatos.numero(-0.0));
    }
}
