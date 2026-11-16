# Territory Predictive Insights (GS 2026/2 · POO )

Plataforma de monitoramento territorial em console (tema **CitizenTech**). O sistema lê sensores simulados, prevê o próximo valor de cada sensor a partir do histórico e emite alertas **ATENÇÃO / ALERTA / CRÍTICO** explicando o motivo.

## Integrantes

| Nome completo | RM |
|---|---|
| João Victor Alves de Abreu | 564946 |
| Rodrigo Kenshin Viana Matayoshi | 564026 |

## Recorte e público beneficiado

**Recorte escolhido: 🌊 Clima local, chuva intensa e risco de alagamento em pontos vulneráveis.**

Escolhemos esse recorte porque alagamento é um problema que se repete todo ano em muitos bairros, e quase sempre dá algum sinal antes: a chuva aperta, o córrego começa a subir, o solo já não absorve mais nada.

**Quem sofre com isso** são as famílias que moram nas partes baixas e nas encostas perto de córregos urbanos. Quando a água sai da margem, elas perdem móveis, ficam sem conseguir sair de casa e, nas encostas, correm risco de deslizamento.

**Quem usaria o sistema** é a coordenação de uma associação de bairro ou a Defesa Civil local. A pergunta que a gente quer ajudar a responder é: *"o córrego vai transbordar nas próximas horas, e o que dá para fazer hoje?"*. Com o alerta em mãos, a coordenação pode reforçar a vigilância do córrego, avisar quem mora nas áreas baixas, deixar um abrigo de prontidão ou acionar o plano de contingência. O sistema não decide nada disso sozinho, só mostra os números e o motivo do alerta para quem vai decidir.

**Por que esses três sensores:** cada um mostra uma parte do mesmo problema, em momentos diferentes.

| Sensor | O que mede | Por que importa para alagamento |
|---|---|---|
| `PLU-01` pluviômetro | intensidade da chuva (mm/h) | é a **causa**: o primeiro sinal de risco |
| `NIV-01` nível do córrego | altura da água (m) | é o **efeito**: sobe com atraso e mostra quando a água vai sair da margem, mesmo depois que a chuva diminui |
| `UMI-01` umidade do solo | saturação da encosta ou várzea (%) | é a **condição**: solo encharcado não absorve mais chuva, a água escorre para o córrego e cresce o risco de deslizamento |

## Como compilar e testar

Requisitos: **JDK 17+** e **Maven 3.9+**.

```bash
mvn test                  # compila e roda os testes (JUnit 5 + Mockito)
mvn compile exec:java     # abre o menu do sistema
```

O banco é o **H2 em arquivo**, criado automaticamente em `./dados/territorio.mv.db` na primeira execução (as tabelas vêm de `src/main/resources/schema.sql`). Nada precisa ser instalado. O histórico sobrevive ao reiniciar o programa; para começar do zero, apague a pasta `dados/`.

**Código de acesso da coordenação** (menu 5, alterar limiares e população): `coord2026`.

Roteiro rápido de demonstração: opção 1 com 6 rodadas → opção 2 (previsões e alertas) → opções 3 e 4 (histórico) → feche e abra o programa de novo e repita a opção 3 para ver os dados persistidos. A opção 7 simula falha ou defeito de calibração de um sensor. A opção 8 roda a [simulação de 24 horas](#bônus-simulação-de-24-horas).

## Estrutura

```text
src/main/java/
├── dominio/   ComponenteTerritorial, StatusComponente, PontoMonitoramento,
│              Sensor, SensorSimulado, SensorPluviometro, SensorNivelCorrego, SensorUmidadeSolo,
│              SensorRoteirizado,
│              TipoSensor, DadosTerritorio, Medicao, Alerta, NivelAlerta,
│              EstrategiaPrevisao, PrevisaoMediaMovel, PrevisaoComparacao, ResultadoPrevisao, Formatos
├── excecao/       DadosInvalidosException, FonteIndisponivelException
├── persistencia/  FabricaConexao, BaseDAO, MedicaoDAO, AlertaDAO
├── servico/       ConfiguracaoSistema, FabricaPrevisao, TipoPrevisao,
│                  ServicoMonitoramento, AvaliacaoPrevisao,
│                  RelatorioBuilder,
│                  Simulacao24Horas, HoraSimulada   (bônus)
└── SistemaMonitoramento.java   (menu principal)
src/main/resources/schema.sql   (tabelas medicao e alerta)
src/test/java/
├── dominio/       MedicaoTest, SensorTest, DadosTerritorioTest, PrevisaoMediaMovelTest,
│                  PrevisaoComparacaoTest, AlertaTest, FormatosTest
├── persistencia/  MedicaoDAOTest, AlertaDAOTest   (H2 real em pasta temporária)
└── servico/       ConfiguracaoSistemaTest, FabricaPrevisaoTest,
                   ServicoMonitoramentoTest        (DAOs mockados com Mockito),
                   Simulacao24HorasTest, RelatorioBuilderTest
```

Os pacotes seguem a estrutura sugerida no enunciado (`dominio`, `excecao`, `persistencia`, `servico`).

## Decisões de projeto

### Domínio

- **`ComponenteTerritorial`** (abstrata): id, nome, região e status, com `verificarStatus()` abstrato. Tem duas linhas de filhas:
  - **`SensorSimulado`** (abstrata, também implementa `Sensor`) → `SensorPluviometro`, `SensorNivelCorrego`, `SensorUmidadeSolo`. Um sensor é um componente físico do território, então herda id/nome/região/status. A base cuida da falha simulada e do status. Cada filha sobrescreve apenas `simularLeitura()`: chuva com 60% de leituras zeradas e intensidade exponencial, nível do córrego com variação normal em torno de 1,2 m e cheias ocasionais, umidade do solo entre 35% e 75% com episódios de solo encharcado.
  - **`PontoMonitoramento`**: agrupa sensores e calcula o próprio status a partir deles (todos confiáveis → OPERACIONAL; algum sem resposta ou descalibrado → DEGRADADO; nenhum respondendo → INDISPONIVEL).
- **`Sensor`** (interface): `lerValor()`, `estaFuncionando()`, `getTipo()` e os métodos default `temLeiturasConfiaveis()` e `lerMedicao()`, que já devolve uma `Medicao` validada.
- **`TipoSensor`** (enum): unidade, faixa física e degrau entre níveis de alerta de cada grandeza. **Todas** as regras de valor de medição e de limiar estão aqui, então nenhum caminho de entrada consegue contorná-las.

  | Tipo | Unidade | Faixa física aceita | Limiar padrão | Degrau entre níveis |
  |---|---|---|---|---|
  | PRECIPITACAO | mm/h | 0 a 300 | 30 (ALERTA 45, CRÍTICO 60) | 15 |
  | NIVEL_CORREGO | m | 0 a 8 | 2,0 (ALERTA 2,5, CRÍTICO 3,0: água na margem) | 0,5 |
  | UMIDADE_SOLO | % | 0 a 100 | 80 (ALERTA 87, CRÍTICO 94) | 7 |

  Os valores são **parâmetros de simulação** escolhidos pela equipe, não dados oficiais.
- **`Medicao`** (`record`, imutável): `(id, sensorId, tipo, valor, dataHora)`. O construtor compacto rejeita sensor vazio, tipo ou data nulos, id ≤ 0, NaN, infinito e valores fora da faixa física. Por isso não existe `Medicao` inválida em memória.
- **`DadosTerritorio`**: atributos privados e **nenhum setter público**. Limiares e população só mudam por `alterarLimiar(codigo, tipo, valor)` e `alterarPopulacaoEstimada(codigo, valor)`, que conferem o código de acesso (comparação em tempo constante) e validam o valor antes de gravar. Se algo falhar, nada muda.
- **`NivelAlerta`** (enum): o limite de cada nível é `limiar + ordem × degrau do tipo`. Para chuva com limiar 30 mm/h: ATENÇÃO ≥ 30, ALERTA ≥ 45, CRÍTICO ≥ 60. Usamos um degrau aditivo em vez de percentual porque cada grandeza tem sua escala: 50% acima de 80% de umidade seria 120%, um valor impossível. Cada nível traz uma orientação ("o que fazer").
- **`Alerta`** (`record`): sensor, tipo, nível, valor previsto, limite atingido, estratégia e data/hora. `Alerta.avaliar(previsao, territorio, dataHora)` busca no território o limiar do tipo da própria previsão (impossível usar o limiar de outra grandeza) e cria o alerta do nível mais grave atingido. `explicacao()` gera o texto para o menu.

### Exceções

| Exceção | Tipo | Quando é lançada |
|---|---|---|
| `DadosInvalidosException` | não verificada | medição fora da faixa, NaN/infinito, campo obrigatório ausente, limiar ≤ 0 ou tão alto que o nível CRÍTICO ficaria fora da faixa física (ex.: chuva acima de 270 mm/h), alerta com nível incoerente com o limite, população negativa, código de acesso errado, histórico que mistura sensores ou tipos, estratégia desconhecida |
| `FonteIndisponivelException` | verificada | leitura de um sensor em falha (`simularFalha()`); guarda o id do sensor em `getIdFonte()` |

`DadosInvalidosException` é não verificada porque o construtor canônico de um `record` não pode declarar `throws`. `FonteIndisponivelException` é verificada porque falha de sensor é esperada em campo, e o compilador obriga quem lê um sensor a tratá-la. As mensagens citam o valor recebido, por exemplo `O limiar de chuva deve ser positivo; valor recebido: -5,0`. O menu captura as duas exceções e mostra `getMessage()`; nenhum `catch` é vazio. Uma falha do **banco** também vira `FonteIndisponivelException` (com `getIdFonte()` = `"banco de dados"`), pois o banco é uma fonte de dados. Um registro corrompido no banco vira `DadosInvalidosException` dizendo o id da linha. Na opção 1, um sensor com problema não interrompe a leitura dos demais.

### Previsões

`EstrategiaPrevisao` (abstrata) define o contrato e oferece validação comum. As filhas chamam `super(nome, minimoLeituras)` no construtor, sobrescrevem `prever()` e estendem `descrever()` com `super.descrever()`.

| Estratégia | Cálculo | Mínimo de leituras |
|---|---|---|
| `PrevisaoMediaMovel(N)` | média das últimas N leituras | N (padrão 5) |
| `PrevisaoComparacao(N)` | `mediaAtual + (mediaAtual − mediaAnterior)` entre os dois últimos períodos de N leituras, limitado à faixa física | 2N (padrão 6) |

Tratamento de dados ruins, igual nas duas estratégias:

- **Insuficientes** (menos leituras que o mínimo, inclusive histórico vazio): não é erro. A estratégia devolve `ResultadoPrevisao` **sem valor** e explica, por exemplo: *"Dados insuficientes: 4 leitura(s) no histórico de PLU-01; a estratégia Média móvel exige pelo menos 5. Nenhuma previsão nem alerta foi emitido."* `Alerta.avaliar` não gera alerta nesse caso.
- **Inválidos** (lista nula, elemento nulo, histórico que mistura sensores): lançam `DadosInvalidosException`, pois indicam erro de quem chamou.
- O histórico pode chegar em qualquer ordem do banco: a estratégia ordena por `dataHora` antes de calcular.

## Padrões criacionais

### Factory: `FabricaPrevisao`

**Por que aqui:** a escolha da estratégia é o único ponto do sistema em que um `if tipo ==`/`instanceof` apareceria, e o enunciado proíbe isso. A fábrica guarda um `EnumMap<TipoPrevisao, criador>` e é a **única** classe que conhece `PrevisaoMediaMovel`/`PrevisaoComparacao` e seus parâmetros (lidos de `ConfiguracaoSistema`). Serviço e menu pedem `fabrica.criar(tipo)`, `criarPadrao()` ou `criarTodas()` e trabalham só com `EstrategiaPrevisao`.

**Trade-off:** é mais uma classe e um nível de indireção. Para incluir uma estratégia é preciso registrá-la na fábrica (o construtor falha se algum `TipoPrevisao` ficar sem criador), mas o restante do código não muda.

### Singleton: `ConfiguracaoSistema`

**Por que aqui:** fábrica, serviço e menu precisam ver **os mesmos** parâmetros. Se o menu mudar a janela da média móvel, a próxima estratégia criada já usa o valor novo. Construtor privado e `getInstancia()` com o idiom *holder* (inicialização preguiçosa e thread-safe sem `synchronized`).

**O que fica nele e o que não fica:** apenas parâmetros globais, ou seja, os limiares **padrão** (somente leitura, usados para criar o `DadosTerritorio`), as janelas das previsões e a estratégia padrão. Os limiares **do território** ficam em `DadosTerritorio`, protegidos pelo código de acesso. Se ficassem no Singleton, haveria um segundo caminho para alterá-los sem esse código.

**Trade-off:** o acesso global é cômodo, mas cria estado compartilhado. Um teste que altera a configuração afeta os seguintes, por isso os testes chamam `restaurarPadroes()` no `@AfterEach`. Também esconde dependências; para reduzir isso, `FabricaPrevisao` recebe a configuração pelo construtor.

### Builder: `RelatorioBuilder`

**Por que aqui:** o "status completo" (opção 6 do menu) é um relatório com várias seções opcionais: território, limiares por nível, componentes e seus status, configuração das previsões e alertas recentes. Um construtor ou método com todos esses parâmetros, vários opcionais, seria difícil de ler e fácil de chamar na ordem errada. Com o Builder, cada seção é um passo com nome (`comTerritorio`, `comLimiares`, `comComponentes`, `comConfiguracao`, `comAlertasRecentes`), quem chama escolhe quais entram e em que ordem, e `construir()` monta o texto. O menu só encadeia os passos; a formatação de cada seção fica num lugar só.

**Por que não em outro lugar:** `Medicao` e `Alerta` têm poucos campos, todos obrigatórios, e já são `record`s validados no construtor; um Builder neles só adicionaria código.

**Trade-off:** mais uma classe. `construir()` sem nenhuma seção lança `DadosInvalidosException`, para não gerar um relatório vazio.

### Por que não usamos `sealed`

Restringir as implementações de `Sensor` não protege nenhuma regra (ninguém faz `switch` sobre tipos de sensor) e atrapalharia mocks/stubs nos testes. Por isso não usamos.

## Persistência (JDBC + H2)

- **Por que JDBC:** os `record`s `Medicao` e `Alerta` são usados diretamente nos DAOs, sem entidades JPA duplicadas, e a validação continua no domínio.
- **`FabricaConexao`** abre as conexões e roda o `schema.sql` (`CREATE TABLE IF NOT EXISTS`) ao iniciar. A URL padrão é em arquivo para cumprir "os dados sobrevivem ao reiniciar"; os testes passam outra URL pelo construtor.
- **`BaseDAO`** concentra o JDBC repetido: try-with-resources em conexão, `PreparedStatement` e `ResultSet`; valores sempre por `?`, nunca por concatenação; id gerado pelo banco (`AUTO_INCREMENT` + `getGeneratedKeys`).
- **`MedicaoDAO`**: `inserir`, `buscarHistoricoPorSensor` (menu) e `buscarUltimasPorSensor(id, N)` (previsões: só as N leituras que a estratégia usa, em ordem cronológica).
- **`AlertaDAO`**: `inserir`, `listarTodos` e `listarUltimosAlertas(N)` (mais recente primeiro; o menu pergunta quantos mostrar).
- **Sem UPDATE/DELETE:** medições e alertas são imutáveis e formam o histórico que justifica cada previsão; reescrevê-los apagaria a evidência.
- Ao ler, cada linha passa pelo construtor do `record`: um dado corrompido no banco é rejeitado em vez de entrar no cálculo.

### `ServicoMonitoramento`

Fica entre o menu e o banco: `registrarLeitura(sensor)` lê e grava (leitura inválida ou sensor em falha não gravam nada); `preverEAvaliar(sensorId, tipo)` pede a estratégia à `FabricaPrevisao`, busca o histórico no banco, prevê, chama `Alerta.avaliar` e grava o alerta, se houver. Recebe os DAOs pelo construtor, o que permite testá-lo com Mockito sem banco, e um relógio injetável para os testes terem horário fixo.

## Bônus: simulação de 24 horas

Opção **8** do menu. Simula um dia inteiro, com uma leitura de cada sensor por hora, e mostra os alertas aparecendo ao longo do tempo:

| Sensor | Cenário do dia |
|---|---|
| `PLU-01` chuva | seco até 8h, chuva forte à tarde com pico de 80 mm/h às 14h, para às 20h |
| `NIV-01` nível do córrego | cerca de 1 m até as 10h; sobe com atraso, passa de 3,0 m entre 15h e 19h (pico de 3,6 m às 17h) e desce devagar |
| `UMI-01` umidade do solo | 55% de manhã; passa de 90% às 14h e continua acima de 88% à noite |

- **Mesmo fluxo do sistema real:** cada hora passa por `ServicoMonitoramento` (ler → gravar → buscar histórico → prever → alertar), com a `FabricaPrevisao` e os limiares atuais do território. O relógio do serviço é injetado e avança uma hora por passo.
- **Banco separado:** roda num H2 **em memória**, criado só para a simulação e descartado no fim. O histórico real não é misturado com um dia inventado, e duas simulações seguidas não interferem uma na outra.
- **Reproduzível:** os valores vêm de `SensorRoteirizado`, uma quarta implementação de `Sensor` que devolve leituras definidas de antemão. As leituras continuam passando pela validação de `Medicao`.
- **O que ela mostra:** com a média móvel, a chuva gera ATENÇÃO às 13h, ALERTA às 14h e CRÍTICO só às 16h, duas horas depois do pico. Com a comparação entre períodos, o primeiro aviso vem às 11h e o CRÍTICO às 13h. O córrego chega ao CRÍTICO às 18h e fica nele até as 20h, quando a chuva já parou: o risco de alagamento continua depois da chuva, e é por isso que o pluviômetro sozinho não basta. `Simulacao24HorasTest` confere esses horários, calculados à mão.

## Testes

96 testes JUnit 5 no padrão **AAA**, em 14 classes, com vários `assertThrows` das exceções customizadas. Os valores esperados são calculados à mão nos próprios testes (ex.: média de 10..50 = 30; comparação 20 → 40 projeta 60), então um erro de cálculo deixa o teste vermelho. Sensores usam `Random` com semente fixa para serem reproduzíveis.

- **Mockito:** `ServicoMonitoramentoTest` mocka `MedicaoDAO` e `AlertaDAO` e verifica a lógica de alerta sem banco: previsão CRÍTICA grava o alerta certo (`ArgumentCaptor`), previsão abaixo do limiar ou histórico insuficiente **não** grava alerta (`never()`), leitura impossível ou sensor em falha não grava medição (`verifyNoInteractions`).
- **DAOs:** `MedicaoDAOTest` e `AlertaDAOTest` usam um H2 de verdade em pasta temporária: id gerado, ordem do histórico, últimas N leituras, dados que sobrevivem ao reabrir o banco, registro corrompido e banco inacessível.

## Reflexão crítica

**Quando o sistema deve se calar?** Quando o histórico do sensor tem menos leituras que o mínimo da estratégia: menos de 5 para a média móvel e menos de 6 para a comparação, nos valores padrão. Nesses casos o código não devolve uma previsão "chutada": `ResultadoPrevisao` vem sem valor, a interpretação diz quantas leituras faltam e `Alerta.avaliar` não emite alerta. Também se cala quando o sensor está em falha (`FonteIndisponivelException`): nenhuma medição é inventada para cobrir o buraco. Mesmo com dados suficientes, a comparação entre períodos exagera picos isolados. Uma única leitura alta pode gerar um CRÍTICO. Por isso o alerta informa qual estratégia o gerou. A simulação de 24 horas mostra o outro lado: a média móvel é estável, mas só dá o CRÍTICO de chuva duas horas depois do pico. Nenhuma estratégia é melhor em tudo, e a escolha cabe à coordenação.

**O que os dados não representam?** Um sensor descreve um ponto, não a região inteira. Um pluviômetro pode registrar 0 mm/h enquanto chove forte a dois quilômetros, e uma única régua não mostra se a água sai da margem em outro trecho do córrego. As leituras são **simuladas** com distribuições escolhidas por nós, não medidas reais. Os limiares (30 mm/h, 2,0 m, 80%) e os degraus entre níveis são parâmetros de exemplo, não normas da Defesa Civil. O sistema também não sabe quem está exposto ao risco: `DadosTerritorio` guarda só uma população estimada da região. Por exigência do enunciado, não há dados de pessoas.

**Por que o alerta apoia, e não decide?** O alerta mostra a evidência (sensor, valor previsto, limite atingido, estratégia) e uma orientação genérica por nível. A decisão depende de contexto que o sistema não tem: obras de drenagem recentes, um evento no bairro, a confiabilidade conhecida de um sensor. Por isso os limiares são ajustáveis pela coordenação, com código de acesso. A pessoa responsável continua respondendo pela ação, e um alarme falso automático custaria a confiança da comunidade.

## Uso de IA

Usamos o Claude Code (assistente de programação da Anthropic) durante o projeto, e não só para tirar dúvidas. Para ficar claro o que foi dele e o que foi nosso:

- **Dúvidas e explicações:** tiramos dúvidas de Java, JDBC, H2, Mockito e Git, e pedimos explicação de erros que apareciam.
- **Revisão de código:** pedimos para revisar o código procurando bugs. Ele achou alguns de verdade, como o ponto de monitoramento que aparecia OPERACIONAL mesmo com um sensor descalibrado, e vários erros nos DAOs da branch `JDBC` (colunas erradas no SQL, parâmetros que não eram preenchidos, a tabela escrita `meicao` e o banco em memória que perdia tudo ao fechar).
- **README:** ajudou a escrever e organizar este README.

**Como revisamos:** rodamos `mvn test` depois de cada mudança e não aceitamos nada com teste vermelho. Os valores esperados dos testes foram conferidos à mão (numa dessas conferências achamos um comentário com a conta errada, 76,67 em vez de 86,67, e corrigimos). Também rodamos o menu de verdade, fechando e abrindo o programa para ver os dados continuarem salvos. As decisões do projeto, como o recorte e o que entrar ou não, foram nossas.

## Divisão de tarefas

| Integrante | Responsabilidade |
|---|---|
| João Victor Alves de Abreu | Domínio e exceções; previsões, alertas e testes; menu e simulação de 24 horas |
| Rodrigo Kenshin Viana Matayoshi | Persistência: DAOs, conexão com o H2 e consultas do histórico |

O enunciado sugere 3 integrantes; nosso grupo tem 2, então as três frentes (domínio e exceções, persistência, previsões/alertas/testes) foram divididas entre nós dois.
