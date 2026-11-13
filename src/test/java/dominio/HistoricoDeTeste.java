package dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Monta históricos de um sensor com leituras de hora em hora, na ordem dada. */
final class HistoricoDeTeste {

    static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 13, 0, 0);

    private HistoricoDeTeste() {
    }

    static List<Medicao> de(String sensorId, TipoSensor tipo, double... valores) {
        List<Medicao> historico = new ArrayList<>();
        for (int i = 0; i < valores.length; i++) {
            historico.add(Medicao.nova(sensorId, tipo, valores[i], INICIO.plusHours(i)));
        }
        return historico;
    }

    static List<Medicao> chuva(double... valores) {
        return de("PLU-01", TipoSensor.PRECIPITACAO, valores);
    }
}
