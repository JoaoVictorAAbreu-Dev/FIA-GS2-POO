-- Tabelas do histórico. Executado a cada início do programa por FabricaConexao;
-- IF NOT EXISTS garante que os dados já gravados não são apagados.
-- tipo e nivel guardam o name() dos enums TipoSensor e NivelAlerta.

CREATE TABLE IF NOT EXISTS medicao (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensorId  VARCHAR(50)      NOT NULL,
    tipo      VARCHAR(30)      NOT NULL,
    valor     DOUBLE PRECISION NOT NULL,
    dataHora  TIMESTAMP        NOT NULL
);

-- Acelera a busca do histórico de um sensor em ordem de data.
CREATE INDEX IF NOT EXISTS idx_medicao_sensor_data ON medicao (sensorId, dataHora);

CREATE TABLE IF NOT EXISTS alerta (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensorId           VARCHAR(50)      NOT NULL,
    tipo               VARCHAR(30)      NOT NULL,
    nivel              VARCHAR(20)      NOT NULL,
    valorPrevisto      DOUBLE PRECISION NOT NULL,
    limiteUltrapassado DOUBLE PRECISION NOT NULL,
    estrategia         VARCHAR(80)      NOT NULL,
    dataHora           TIMESTAMP        NOT NULL
);
