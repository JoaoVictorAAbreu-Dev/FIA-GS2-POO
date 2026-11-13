package servico;

import dominio.EstrategiaPrevisao;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricaPrevisaoTest {

    private final ConfiguracaoSistema config = ConfiguracaoSistema.getInstancia();
    private final FabricaPrevisao fabrica = new FabricaPrevisao(config);

    @AfterEach
    void restaurar() {
        config.restaurarPadroes();
    }

    @Test
    void criaCadaEstrategiaPeloTipo() {
        // Act
        EstrategiaPrevisao media = fabrica.criar(TipoPrevisao.MEDIA_MOVEL);
        EstrategiaPrevisao comparacao = fabrica.criar("comparacao");

        // Assert
        assertInstanceOf(PrevisaoMediaMovel.class, media);
        assertInstanceOf(PrevisaoComparacao.class, comparacao);
    }

    @Test
    void usaOsParametrosAtuaisDaConfiguracao() {
        // Arrange
        config.definirJanelaMediaMovel(7);
        config.definirLeiturasPorPeriodoComparacao(4);

        // Act
        PrevisaoMediaMovel media = (PrevisaoMediaMovel) fabrica.criar(TipoPrevisao.MEDIA_MOVEL);
        EstrategiaPrevisao comparacao = fabrica.criar(TipoPrevisao.COMPARACAO);

        // Assert
        assertEquals(7, media.getJanela());
        assertEquals(8, comparacao.getMinimoLeituras());
    }

    @Test
    void criaAPadraoEUmaDeCadaTipo() {
        // Arrange
        config.definirTipoPrevisaoPadrao(TipoPrevisao.COMPARACAO);

        // Act
        EstrategiaPrevisao padrao = fabrica.criarPadrao();
        List<EstrategiaPrevisao> todas = fabrica.criarTodas();

        // Assert
        assertInstanceOf(PrevisaoComparacao.class, padrao);
        assertEquals(TipoPrevisao.values().length, todas.size());
    }

    @Test
    void codigoDesconhecidoLancaDadosInvalidosComAsOpcoes() {
        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> fabrica.criar("regressao"));

        // Assert
        assertTrue(erro.getMessage().contains("Opções: media-movel, comparacao"), erro.getMessage());
    }
}
