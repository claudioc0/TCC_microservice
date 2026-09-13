# Resultados de Testabilidade — Microsserviços (TCC)

> Documento gerado a partir da execução real das suítes de teste, JaCoCo, PITest, ArchUnit e
> `ck` nos quatro microsserviços (`user-service`, `product-service`, `order-service`,
> `report-service`). Sem números estimados ou hipotéticos — tudo abaixo veio de execução
> direta das ferramentas, sob **JDK 21.0.12.1 (Eclipse Temurin)**.

## Resumo

| Métrica | Ferramenta | Dimensão (GQM) | Status |
|---|---|---|---|
| Branch Coverage | JaCoCo | Adequação da suíte | Coletado |
| Mutation Score | PITest | Força da suíte | Coletado — Test Strength 100% |
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

| Serviço | Instruções | Ramos |
|---|---:|---:|
| user-service | 98,6% | 100% |
| product-service | 99,3% | 100% |
| order-service | 95,0% | 96,4% |
| report-service | 95,7% | 100% |

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

O resíduo restante em `order-service` (95,0% / 96,4%, o único não-100%) está concentrado em
ramos de tratamento de erro de infraestrutura (`ProductServiceUnavailableException` em cenários
de rede não simulados) — não regra de negócio sem teste.

---

## 2. Mutation Score (PITest)

Alvo: pacotes `*.entity`, `*.service` e `*.security` (onde vive a regra de negócio), mutadores
`DEFAULTS` do PITest, gerado sob JDK 21.0.12.1.

| Serviço | Mutantes | Mortos | Mutation Score | Sem cobertura | Test Strength |
|---|---:|---:|---:|---:|---:|
| user-service | 32 | 26 | 81% | 6 | **100%** |
| product-service | 41 | 36 | 88% | 5 | **100%** |
| order-service | 54 | 49 | 91% | 5 | **100%** |
| report-service | 22 | 17 | 77% | 5 | **100%** |

**Nota sobre a recontagem em `order-service`:** a mesma suíte, recompilada sob JDK 21 em vez de
17, gerou 54 mutantes em vez dos 57 medidos antes — o Mutation Score (91%) e o Test Strength
(100%) não mudaram, mas o total de mutantes mudou porque o bytecode que o PITest analisa é
diferente entre compiladores. É exatamente a razão pela qual a seção 0 registra a *patch
version* exata: números de mutação de lados diferentes do experimento só são comparáveis se
gerados no mesmo JDK, e essa é a primeira coleta com essa garantia.

**Por que reportamos Test Strength junto do Mutation Score:** Mutation Score bruto
(mortos / total gerado) penaliza igualmente um mutante que a suíte tentou matar e falhou e um
mutante em código que nenhum teste jamais executa — os dois casos viram "sobrevivente".
Test Strength isola o primeiro caso (mortos / (mortos + sobreviventes), excluindo
`NO_COVERAGE`) e responde à pergunta real do experimento: *dos mutantes que a suíte tinha
chance de matar, quantos ela matou?* Nos quatro serviços esse número é **100%** — nenhum
mutante executado sobreviveu sem ser pego por uma asserção.

A diferença entre Mutation Score e Test Strength (5–6 mutantes "sem cobertura" por serviço) é,
em todos os casos, o mesmo artefato de ferramenta: métodos `@Bean` de `SecurityConfig`, que o
PITest não instrumenta corretamente sob o proxy CGLIB gerado pelo Spring para classes
`@Configuration` — uma limitação documentada da combinação PITest+Spring, não uma lacuna de
teste real. O comportamento desses beans já foi validado de forma independente via chamadas
`curl` ponta a ponta contra os serviços em execução.

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

Item respondido a partir de `pendencias-microservicos.md` (lado do monólito):

| # | Ameaça | Status | Como foi fechada |
|---|---|---|---|
| 1 | JDK 21 não declarado formalmente | **Resolvido** | Temurin 21.0.12.1 instalado, `<java.version>` atualizado nos 4 `pom.xml`, `mvn clean test` revalidado nos 4 serviços |
| 2 | CBO sem par comparativo | **Resolvido** | `ck` 0.7.0 rodado nos 4 serviços — ver seção 4 e `docs/reports/acoplamento-cbo.md` |
| 3 | Assimetria de testes de integração | **Resolvido (Opção A — equalizado)** | 4 testes `@SpringBootTest` adicionados (um por serviço), branch coverage remedido — ver seção 1 |

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
| Mutation Score / Test Strength | não executado | executado sob JDK 21 — Test Strength 100% (4 serviços) |
| CBO (`ck`) | não instrumentado | 6,71 médio agregado (4 serviços, 48 classes) |
