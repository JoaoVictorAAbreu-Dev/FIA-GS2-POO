package servico;

import dominio.DadosTerritorio;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracaoSistemaTest {

    @AfterEach
    void restaurar() {
        // O Singleton é compartilhado entre testes: devolvemos ao estado inicial.
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
    }

    @Test
    void sempreDevolveAMesmaInstanciaEConstrutorEPrivado() {
        // Act
        ConfiguracaoSistema primeira = ConfiguracaoSistema.getInstancia();
        ConfiguracaoSistema segunda = ConfiguracaoSistema.getInstancia();
        Constructor<?>[] construtores = ConfiguracaoSistema.class.getDeclaredConstructors();

        // Assert
        assertSame(primeira, segunda);
        assertEquals(1, construtores.length);
        assertTrue(Modifier.isPrivate(construtores[0].getModifiers()));
    }

    @Test
    void limiaresPadraoCriamTerritorioValidoESaoSomenteLeitura() {
        // Arrange
        ConfiguracaoSistema config = ConfiguracaoSistema.getInstancia();

        // Act
        DadosTerritorio territorio = new DadosTerritorio("Bacia do Córrego", "Zona Leste", 45_000, "coord-2026",
                config.getLimiaresPadrao());

        // Assert
        assertEquals(30.0, territorio.getLimiar(TipoSensor.PRECIPITACAO));
        assertThrows(UnsupportedOperationException.class,
                () -> config.getLimiaresPadrao().put(TipoSensor.PRECIPITACAO, 1.0));
    }

    @Test
    void rejeitaParametrosDePrevisaoInvalidosSemAlterarOValorAtual() {
        // Arrange
        ConfiguracaoSistema config = ConfiguracaoSistema.getInstancia();

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> config.definirJanelaMediaMovel(1));

        // Assert
        assertEquals("A janela da média móvel deve ter pelo menos 2 leituras; valor recebido: 1",
                erro.getMessage());
        assertEquals(ConfiguracaoSistema.JANELA_MEDIA_MOVEL_PADRAO, config.getJanelaMediaMovel());
        assertThrows(DadosInvalidosException.class, () -> config.definirLeiturasPorPeriodoComparacao(0));
        assertThrows(DadosInvalidosException.class, () -> config.definirTipoPrevisaoPadrao(null));
    }
}
