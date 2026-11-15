package servico;

import dominio.Alerta;
import dominio.Medicao;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Uma hora da simulação de 24 horas: as leituras gravadas naquela hora e os
 * alertas emitidos a partir delas.
 */
public record HoraSimulada(LocalDateTime dataHora, List<Medicao> leituras, List<Alerta> alertas) {

    public HoraSimulada {
        // Cópias imutáveis: quem recebe o resultado não consegue alterá-lo.
        leituras = List.copyOf(leituras);
        alertas = List.copyOf(alertas);
    }
}
