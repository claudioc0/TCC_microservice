# Resultados de Testabilidade — Microsserviços (TCC)

> Documento gerado a partir da execução real das suítes de teste, JaCoCo, PITest, ArchUnit e
> `ck` nos quatro microsserviços (`user-service`, `product-service`, `order-service`,
> `report-service`). Sem números estimados ou hipotéticos — tudo abaixo veio de execução
> direta das ferramentas, sob **JDK 21.0.12.1 (Eclipse Temurin)**.

## Resumo

| Métrica | Ferramenta | Dimensão (GQM) | Status |
|---|---|---|---|
| Branch Coverage | JaCoCo | Adequação da suíte | Coletado |
| Mutation Score | PITest | Força da suíte | Coletado — 82,9% (174/210), Test Strength 85,3% |
| Densidade de Violações | ArchUnit | Integridade estrutural | Coletado — 0/17 |
| Acoplamento (CBO) | `ck` 0.7.0 | Esforço de isolamento/mock | Coletado — CBO médio 6,71 |

**171 testes automatizados** (JUnit 5 + Mockito, incluindo testes de client HTTP com
`MockRestServiceServer`, testes de Controller com `@WebMvcTest` e 4 testes de integração
`@SpringBootTest` de ponta a ponta) sustentam a coleta das quatro métricas nos quatro
microsserviços. Todas as quatro dimensões do protocolo GQM têm par comparativo com o
monólito de referência.

---

## 0. Ambiente de execução

| Item | Valor |
|---|---|
| JDK | Eclipse Temurin **21.0.12.1** (`21.0.12+1-LTS`) |
| Maven | 3.9.16 |
| Declarado em | `<java.version>` nos 4 `pom.xml` (antes: `17`, sem `mvn clean` após qualquer troca de versão) |

A versão exata (não só "JDK 21" genérico) é registrada aqui porque a *patch version* do
compilador pode mudar o total de mutantes gerados no PITest — e mudou: ver nota na seção 2.

---

## 1. Branch Coverage (JaCoCo)

Contagens absolutas (colunas `BRANCH_*`/`INSTRUCTION_*` do `jacoco.csv`, mesmo método do
monólito), rodada de 2026-09-30:

| Serviço | Instruções | Ramos |
|---|---:|---:|
| user-service | 761/772 = 98,6% | 16/16 = 100% |
| product-service | 703/708 = 99,3% | 10/10 = 100% |
| order-service | 1.072/1.129 = 95,0% | 27/28 = 96,4% |
| report-service | 446/466 = 95,7% | 10/10 = 100% |
| **Agregado** | **2.982/3.075 = 97,0%** | **63/64 = 98,4%** |

Monólito, mesmo JDK/JaCoCo 0.8.11: **34/42 = 81,0%** de ramos. Nota de leitura: o sistema em
microsserviços tem 64 ramos contra 42 do monólito — o número de decisões cresce com a
decomposição (4 filtros JWT, 4 handlers de erro, clients HTTP), não só a cobertura.

**Determinismo:** `mvn clean test` rodado 3× por serviço; hash MD5 do `jacoco.csv` idêntico
nas 3 execuções em todos os 4 serviços.

**Leitura honesta — duas rodadas de fechamento de lacuna nesta sprint:**

1. Primeira rodada: `product-service` e `order-service` não tinham teste de integração de
   Controller (`*.controller` em 0%), e nenhum dos quatro serviços tinha teste dedicado para o
   `JwtAuthenticationFilter`. Fechada com `ProductControllerTest`, `OrderControllerTest`
   (`@WebMvcTest`) e um `JwtAuthenticationFilterTest` por serviço — ramos subiram de 20–70%
   para 87–100%.
2. Segunda rodada (ameaça à validade nº 3, ver seção "Ameaças à validade" abaixo): o monólito
   tinha 4 testes de integração `@SpringBootTest` que a versão em microsserviços não tinha,
   respondendo por uma diferença medida de 7,1 pontos percentuais de branch coverage do lado
   do monólito. Fechada escrevendo o equivalente aqui — um `@SpringBootTest` de fluxo principal
   por serviço (`AuthIntegrationTest`, `ProductIntegrationTest`, `OrderIntegrationTest`,
   `ReportIntegrationTest`) — subindo o agregado para 95–99% de instruções e 96–100% de ramos.

O único ramo não coberto nos 4 serviços é `OrderController.findAll`, linha 62
(`startDate != null && endDate != null`): nenhum teste chama `GET /api/orders` com só uma
das duas datas. *(A rodada anterior atribuía esse resíduo a `ProductServiceUnavailableException`
— a leitura do relatório HTML do JaCoCo mostra que não é o caso.)*

---

## 2. Mutation Score (PITest)

> **Rodada de 2026-09-30 — escopo alinhado com o monólito.** Até a rodada anterior, o PITest
> daqui mutava só `*.entity`, `*.service` e `*.security`, e `targetTests` estava restrito
> aos testes desses mesmos pacotes — ou seja, os testes de Controller (`@WebMvcTest`) e os 4
> `@SpringBootTest` de integração não participavam da matança de mutantes. O monólito muta o
> pacote inteiro (`com.pucpr.tcc.monolith.*`) com todos os testes, exceto `architecture.*`,
> e já tinha removido suas próprias exclusões por inflarem o score (ameaça 2 do lado dele).
> Os 4 `pom.xml` agora usam exatamente o mesmo critério (`com.tcc.<servico>.*` como alvo e
> como testes, `excludedTestClasses` = `com.tcc.<servico>.architecture.*`, mutadores
> `DEFAULTS`, PITest 1.15.3). Os números abaixo substituem os da rodada anterior; os antigos
> ficam em "Histórico" no fim desta seção.

| Serviço | Mutantes | Mortos | Mutation Score | Sobreviventes | Sem cobertura | Test Strength |
|---|---:|---:|---:|---:|---:|---:|
| user-service | 50 | 39 | 78,0% | 10 | 1 | 79,6% (39/49) |
| product-service | 56 | 50 | 89,3% | 6 | 0 | 89,3% (50/56) |
| order-service | 75 | 64 | 85,3% | 8 | 3 | 88,9% (64/72) |
| report-service | 29 | 21 | 72,4% | 6 | 2 | 77,8% (21/27) |
| **Agregado** | **210** | **174** | **82,9%** | **30** | **6** | **85,3% (174/204)** |

Monólito, mesmo JDK e mesma configuração: **137/170 = 80,6%**, Test Strength
**137/143 = 95,8%** (6 sobreviventes, 27 sem cobertura).

**Determinismo:** PITest rodado 2× por serviço — conjunto de mutantes e status de cada um
idênticos mutante a mutante (classe, método, linha, mutador, índice) nas duas execuções.

### Correção de uma afirmação da rodada anterior

A rodada anterior atribuía os mutantes `NO_COVERAGE` de `SecurityConfig` a "um artefato do
PITest com o proxy CGLIB de classes `@Configuration`". **Isso estava errado.** Com o escopo
de testes alinhado, esses mesmos métodos passam a ser executados (pelos `@SpringBootTest` e
`@WebMvcTest`) — não eram inalcançáveis, só não eram executados pelos testes que estavam no
escopo. E, uma vez executados, **sobrevivem**: o "Test Strength 100%" anterior era um efeito
do escopo restrito, não uma propriedade da suíte.

### Os 30 sobreviventes, por categoria

| Categoria | Qtde | Onde | Leitura |
|---|---:|---|---|
| Wiring de `SecurityConfig` (`corsConfigurationSource` ×4 mutantes, `securityFilterChain`, `passwordEncoder`) | 21 | 4 serviços (5–6 por serviço) | Mesma categoria de 1 dos sobreviventes do monólito (`SecurityConfig#securityFilterChain`). Nenhum teste verifica headers CORS nem que a cadeia de filtros foi de fato a customizada. |
| `GlobalExceptionHandler$ErrorResponse.of` retornando `null` | 4 | 4 serviços | Os testes checam o status HTTP do erro, não o corpo JSON. |
| `DatabaseSeeder` (user-service) | 3 | user-service | Mesma categoria de 3 dos 6 sobreviventes do monólito. |
| `RestClientConfig#restClientBuilder` | 1 | order-service | Wiring de framework. |
| `OrderResponse$OrderItemResponse.from` retornando `null` | 1 | order-service | Nenhum teste inspeciona os itens na resposta do pedido. |

**Leitura para a comparação:** a maior parte do resíduo (21 de 30) é o mesmo código de
configuração transversal (`SecurityConfig`) **replicado nos 4 serviços** — cada cópia gera
seu próprio conjunto de mutantes sobreviventes. É um efeito arquitetural direto da
decomposição: no monólito existe uma `SecurityConfig`; aqui existem quatro, e a mesma lacuna
de teste conta quatro vezes. Isso explica boa parte da queda de Test Strength em relação ao
monólito (85,3% vs. 95,8%) e deve ser relatado como tal, não como "suíte mais fraca" em
regra de negócio — em `entity`/`service` dos 4 serviços não há nenhum sobrevivente.

### Os 6 sem cobertura

| Serviço | Método | Por quê |
|---|---|---|
| order-service | `GlobalExceptionHandler.handleInsufficientStock`, `handleProductNotFound`, `handleProductServiceUnavailable` | Nenhum teste propaga esses erros do product-service até o handler HTTP. |
| report-service | `GlobalExceptionHandler.handleAccessDenied`, `handleOrderServiceUnavailable` | Idem, para o order-service indisponível e acesso negado. |
| user-service | `UserController.findById` | Endpoint sem teste. |

Todos são teste faltando em caminho de erro — nenhum exige mudança em código de produção.

### Histórico — rodada anterior (escopo restrito, 2026-09-13)

| Serviço | Mutantes | Mortos | Mutation Score | Sem cobertura | Test Strength |
|---|---:|---:|---:|---:|---:|
| user-service | 32 | 26 | 81% | 6 | 100% |
| product-service | 41 | 36 | 88% | 5 | 100% |
| order-service | 54 | 49 | 91% | 5 | 100% |
| report-service | 22 | 17 | 77% | 5 | 100% |

Não comparável com o monólito: escopo de classes e de testes diferente do dele. A nota de
2026-09-13 sobre JDK 17 → 21 (57 → 54 mutantes em `order-service`) continua válida como
registro histórico.

---

## 3. ArchUnit (Densidade de Violações)

| Indicador | Valor |
|---|---:|
| Regras verificadas | 17 (4 serviços · camadas, nomenclatura, DTOs, fronteira física) |
| Violações | 0 |
| Densidade | 0,0 (violações / regra verificada) |

O valor "zero violações" é menos interessante isoladamente do que em comparação: no monólito,
zero violações significa "a disciplina do time se manteve até agora — mas pode quebrar no
próximo commit". Nos microsserviços, zero violações na regra de fronteira física
(`shouldNotDependOnOtherServicesPackages`) é uma tautologia estrutural: *não pode* deixar de
ser zero, porque a violação exigiria um import que não compila (as classes de um serviço
simplesmente não existem no classpath de outro).

---

## 4. Acoplamento (CBO) — `ck` 0.7.0

Detalhe completo, incluindo tabela por camada e por classe, em
[`docs/reports/acoplamento-cbo.md`](docs/reports/acoplamento-cbo.md).

| Serviço | Classes | CBO médio | CBO máximo |
|---|---:|---:|---:|
| user-service | 13 | 6,77 | 14 (`UserService`) |
| product-service | 10 | 7,10 | 17 (`ProductController`) |
| order-service | 16 | 6,69 | 19 (`OrderController`) |
| report-service | 9 | 6,22 | 11 (`SecurityConfig`) |

**CBO médio agregado (4 serviços, 48 classes): 6,71** — contra **7,45** (29 classes) do lado
do monólito.

Leitura obrigatória junto do número (detalhada no relatório completo): parte da redução vem de
acoplamento entre domínios (ex.: `Order` → `Product`) ter migrado de dependência Java direta
(contável pelo CBO) para uma classe cliente HTTP dedicada (`ProductClient`, CBO 9;
`OrderClient`, CBO 7) — acoplamento de contrato de rede que o CBO não mede. Não é correto ler
6,71 vs. 7,45 como "a decomposição reduziu o acoplamento real entre produto e pedido".

---

## Ameaças à validade — status

Itens 1–3 respondidos a partir de `pendencias-microservicos.md` (lado do monólito); item 4
encontrado deste lado, na rodada de 2026-09-30:

| # | Ameaça | Status | Como foi fechada |
|---|---|---|---|
| 1 | JDK 21 não declarado formalmente | **Resolvido** | Temurin 21.0.12.1 instalado, `<java.version>` atualizado nos 4 `pom.xml`, `mvn clean test` revalidado nos 4 serviços |
| 2 | CBO sem par comparativo | **Resolvido** | `ck` 0.7.0 rodado nos 4 serviços — ver seção 4 e `docs/reports/acoplamento-cbo.md` |
| 3 | Assimetria de testes de integração | **Resolvido (Opção A — equalizado)** | 4 testes `@SpringBootTest` adicionados (um por serviço), branch coverage remedido — ver seção 1 |
| 4 | Escopo do PITest divergente do monólito (só `entity/service/security`, e só os testes desses pacotes) | **Resolvido (2026-09-30)** | Escopo alinhado ao do monólito nos 4 `pom.xml`; Test Strength foi de 100% para 85,3% — ver seção 2 |

---

## Evolução da sprint (estado inicial → estado final desta rodada)

| Item | Antes | Depois |
|---|---|---|
| Testes automatizados | 97 | 171 |
| Branch coverage (ramos), agregado | 20–70% | 96,4–100% |
| JDK declarado/validado | 17 (não confirmado com o monólito) | 21.0.12.1 Temurin, `clean test` revalidado |
| Controller com `@WebMvcTest` (product/order) | ausente | presente |
| Teste dedicado de `JwtAuthenticationFilter` | ausente (4 serviços) | presente (4 serviços) |
| Teste de integração `@SpringBootTest` por serviço | ausente (4 serviços) | presente (4 serviços) |
| Mutation Score / Test Strength | não executado | executado sob JDK 21, escopo alinhado ao monólito — 82,9% / Test Strength 85,3% (4 serviços) |
| CBO (`ck`) | não instrumentado | 6,71 médio agregado (4 serviços, 48 classes) |

---

## Log de execuções — rodada de 2026-09-30

Ambiente: JDK 21.0.12.1 Temurin, Maven 3.9.16, JaCoCo 0.8.11, PITest 1.15.3, ArchUnit 1.2.1.

| Serviço | Testes (3 rodadas) | Duração | MD5 `jacoco.csv` (3 rodadas) | PITest 2× |
|---|---|---|---|---|
| user-service | 35/35, 0 falhas ×3 | 22–24s | `bda1079d…` idêntico | idêntico mutante a mutante |
| product-service | 49/49, 0 falhas ×3 | 23–24s | `88b6273f…` idêntico | idêntico mutante a mutante |
| order-service | 63/63, 0 falhas ×3 | 24s | `380aaf11…` idêntico | idêntico mutante a mutante |
| report-service | 24/24, 0 falhas ×3 | 18s | `51919408…` idêntico | idêntico mutante a mutante |
| **Total** | **171/171, 0 falhas** | | | |

ArchUnit incluído nos testes acima: 0 violações em todas as rodadas. CBO não foi remedido:
nenhum arquivo de `src/main` mudou desde a medição de 2026-09-13 (só `pom.xml`).

### Verificação independente — Vinicius (2026-10-09)

Executada com `rodar-testes.bat` (modo completo) em outra máquina. Resultado **idêntico** à
rodada de referência em todos os pontos: 171/171 testes nas 3 rodadas, MD5 do `jacoco.csv`
igual aos 4 hashes de referência (relatório idêntico byte a byte), ramos 63/64, PITest
174/210 (Test Strength 174/204) com as 2 rodadas idênticas mutante a mutante, inclusive a
contagem de sobreviventes e de mutantes sem cobertura por serviço.

*Pendente: o cabeçalho do `resumo.txt` (máquina, JDK, commit) não foi recebido junto. A
igualdade do total de mutantes indica o mesmo JDK, mas ele ainda não foi confirmado.*

## Comparação consolidada com o monólito (mesmo JDK, mesmas versões de ferramenta)

| Métrica | Monólito | Microsserviços |
|---|---|---|
| Testes | 98/98 | 171/171 |
| Branch Coverage | 34/42 = 81,0% | 63/64 = 98,4% |
| Mutation Score | 137/170 = 80,6% | 174/210 = 82,9% |
| Test Strength | 137/143 = 95,8% | 174/204 = 85,3% |
| Violações ArchUnit | 0/7 | 0/17 |
| CBO médio | 7,45 (29 classes) | 6,71 (48 classes) |

Leitura resumida: microsserviços têm mais cobertura (menos código não exercitado: 6 mutantes
`NO_COVERAGE` vs. 27), mas Test Strength menor, puxado pela `SecurityConfig` replicada nos 4
serviços (21 dos 30 sobreviventes). Há duas ressalvas. A diferença de cobertura também reflete
esforço de teste diferente: 171 testes contra 98, incluindo `@WebMvcTest` que o monólito não
tem. E as regras ArchUnit (7 vs. 17) não são as mesmas.
