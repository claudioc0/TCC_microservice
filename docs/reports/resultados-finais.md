# Resultados Finais — Microsserviços (estado consolidado)

Ponto único de consulta para "qual é o número atual de cada métrica" do lado de
microsserviços. Espelha o `resultados-finais.md` do monólito e serve de base para o
capítulo de resultados do TCC.

**Regra deste documento:** todo número abaixo sai de um arquivo gerado pelas ferramentas e
versionado nesta pasta. Nenhum valor é estimado nem digitado à mão: o script
[`extrair_metricas.py`](extrair_metricas.py) lê os arquivos brutos e gera
[`metricas.json`](metricas.json), de onde as tabelas foram tiradas. A coluna "Fonte" de cada
tabela diz qual arquivo conferir.

| Pasta | Arquivo bruto | Ferramenta | Métrica |
|---|---|---|---|
| `jacoco/<servico>/` | `jacoco.csv`, `jacoco.xml` | JaCoCo | Branch Coverage |
| `pitest/<servico>/` | `mutations.xml` | PITest | Mutation Score, Test Strength |
| `surefire/<servico>/` | `TEST-*.xml` | Maven Surefire | Testes, ArchUnit |
| `ck/<servico>/` | `class.csv`, `method.csv` | ck | CBO |

---

## 1. Ambiente de execução

| Item | Valor |
|---|---|
| Código medido | `main`, commit `13e58b9` (código idêntico ao de `6231961`, onde o escopo do PITest foi alinhado) |
| Data da coleta dos arquivos brutos | 2026-10-19 (`mvn clean test` + PITest, árvore limpa) |
| JDK | Eclipse Temurin **21.0.12.1** (`21.0.12+1-LTS`) |
| Maven | 3.9.16 |
| Spring Boot | 3.2.0 |
| JUnit / Mockito / AssertJ | 5.10.1 / 5.7.0 / 3.24.2 |
| JaCoCo | 0.8.11 |
| PITest | 1.15.3 + `pitest-junit5-plugin` 1.2.1, mutadores `DEFAULTS` |
| ArchUnit | 1.2.1 (`archunit-junit5`) |
| ck | 0.7.0 |

As versões de JaCoCo, PITest, ArchUnit e ck são as mesmas do monólito, assim como o JDK.

Os arquivos brutos desta pasta têm o mesmo hash MD5 de `jacoco.csv` das três execuções
independentes (seção 8). Por isso não são "mais uma medição": são os mesmos dados que as três
pessoas obtiveram.

| Serviço | MD5 de `jacoco/<servico>/jacoco.csv` |
|---|---|
| user-service | `BDA1079DDA384C0CD9C07FEEE8FA77FB` |
| product-service | `88B6273F2A28C6EF654777A7EF68FBA8` |
| order-service | `380AAF115FF687AFE8D5ABA76C5C489C` |
| report-service | `5191940855EDB034A3C03D2A6B45A5ED` |

---

## 2. Tabela consolidada

| Métrica | Ferramenta | Microsserviços | Monólito (mesmo JDK e ferramentas) | Fonte |
|---|---|---|---|---|
| Testes | JUnit 5 | **171/171**, 0 falhas | 98/98 | `surefire/*/TEST-*.xml` |
| Branch Coverage | JaCoCo | **63/64 = 98,4%** | 34/42 = 81,0% | `jacoco/*/jacoco.csv` |
| Mutation Score | PITest | **174/210 = 82,9%** | 137/170 = 80,6% | `pitest/*/mutations.xml` |
| Test Strength | PITest | **174/204 = 85,3%** | 137/143 = 95,8% | `pitest/*/mutations.xml` |
| Violações ArchUnit | ArchUnit | **0 em 17 regras** | 0 em 7 regras | `surefire/*/TEST-*ArchitectureTest.xml` |
| CBO médio | ck | **6,26** (53 tipos) | 7,45 (29 tipos) | `ck/*/class.csv` |

Os números do monólito foram reconferidos a partir dos arquivos brutos dele
(`docs/reports/jacoco/jacoco.csv`, `docs/reports/pitest/mutations.xml` e
`docs/reports/ck/class.csv` do repositório `TCC_monolith`, commit `1c05487`), e não copiados
do relatório.

> **Monólito equalizado.** A coluna "Monólito" acima é o baseline (98 testes). O monólito
> tem também um estado equalizado (commit `518b8c9`, 180 testes, com `@WebMvcTest`): 41/42 =
> 97,6% de ramos e 167/170 = 98,2% de Mutation Score, reproduzidos em
> [`monolito-equalizado/`](monolito-equalizado/). A comparação com esse estado está em
> [`analise-comparativa.md`](analise-comparativa.md).

> **Atenção ao CBO:** o valor publicado antes deste documento (6,71) usava um critério de
> contagem de classes diferente do monólito. Com o mesmo critério dos dois lados, o valor é
> 6,26. Ver seção 6.

---

## 3. Composição da suíte de testes

| Tipo de teste | user | product | order | report | Total |
|---|---:|---:|---:|---:|---:|
| Unidade (JUnit 5 + Mockito) | 25 | 30 | 37 | 14 | **106** |
| Controller (`@WebMvcTest`) | 5 | 14 | 12 | 3 | **34** |
| Cliente HTTP (`MockRestServiceServer`) | — | — | 6 | 2 | **8** |
| Integração (`@SpringBootTest`) | 1 | 1 | 2 | 2 | **6** |
| Arquitetura (ArchUnit) | 4 | 4 | 6 | 3 | **17** |
| **Total** | **35** | **49** | **63** | **24** | **171** |

Fonte: atributo `tests` de cada `surefire/<servico>/TEST-*.xml`. Unidade agrupa os testes de
entidade, service, `JwtService` e `JwtAuthenticationFilter`.

O monólito tem 98 testes: 69 de unidade e 29 de integração, sem testes `@WebMvcTest`. Essa
diferença de composição é a principal ameaça à validade da comparação (seção 9).

---

## 4. Branch Coverage (JaCoCo)

Contagens das colunas `BRANCH_*`, `INSTRUCTION_*` e `LINE_*` do `jacoco.csv`, o mesmo método
usado no monólito depois da correção de coluna.

| Serviço | Ramos | Linhas | Instruções |
|---|---:|---:|---:|
| user-service | 16/16 = 100,0% | 158/161 = 98,1% | 761/772 = 98,6% |
| product-service | 10/10 = 100,0% | 142/144 = 98,6% | 703/708 = 99,3% |
| order-service | 27/28 = 96,4% | 220/236 = 93,2% | 1.072/1.129 = 95,0% |
| report-service | 10/10 = 100,0% | 90/96 = 93,8% | 446/466 = 95,7% |
| **Agregado** | **63/64 = 98,4%** | **610/637 = 95,8%** | **2.982/3.075 = 97,0%** |

**Por camada (agregado dos 4 serviços):**

| Camada | Ramos | Linhas |
|---|---:|---:|
| security | 24/24 | 208/208 |
| service | 16/16 | 96/96 |
| entity | 14/14 | 107/107 |
| exception | 6/6 | 78/88 |
| controller | 3/4 | 40/41 |
| client | — | 47/55 |
| dto | — | 23/23 |
| config | — | 2/2 |
| (raiz: `*Application`, `DatabaseSeeder`) | — | 9/17 |

**O resíduo:** o único ramo não coberto nos 4 serviços está em `OrderController.findAll`,
linha 62 (`if (startDate != null && endDate != null)`). Nenhum teste chama
`GET /api/orders` com só uma das duas datas.

**Leitura:** o sistema em microsserviços tem 64 ramos, contra 42 do monólito. O número de
decisões cresce com a decomposição, porque os filtros JWT, os handlers de erro e os clientes
HTTP existem em várias cópias. Comparar só os percentuais esconde essa diferença de escala.

---

## 5. Mutation Score e Test Strength (PITest)

**Escopo:** pacote inteiro de cada serviço (`com.tcc.<servico>.*`) como alvo, todos os testes
exceto `architecture.*`, mutadores `DEFAULTS`. É o mesmo critério do monólito.

| Serviço | Mutantes | Mortos | Sobreviventes | Sem cobertura | Mutation Score | Test Strength |
|---|---:|---:|---:|---:|---:|---:|
| user-service | 50 | 39 | 10 | 1 | 78,0% | 39/49 = 79,6% |
| product-service | 56 | 50 | 6 | 0 | 89,3% | 50/56 = 89,3% |
| order-service | 75 | 64 | 8 | 3 | 85,3% | 64/72 = 88,9% |
| report-service | 29 | 21 | 6 | 2 | 72,4% | 21/27 = 77,8% |
| **Agregado** | **210** | **174** | **30** | **6** | **82,9%** | **174/204 = 85,3%** |

*Test Strength* = mortos / (mortos + sobreviventes). Ele exclui os mutantes que nenhum teste
executa e mede a força das asserções onde a suíte de fato passa.

**Por camada (agregado):**

| Camada | Mortos / total | Sobreviventes | Sem cobertura |
|---|---:|---:|---:|
| service | 48/48 | 0 | 0 |
| entity | 43/43 | 0 | 0 |
| client | 3/3 | 0 | 0 |
| controller | 25/26 | 0 | 1 |
| dto | 4/5 | 1 | 0 |
| security | 37/58 | 21 | 0 |
| exception | 14/23 | 4 | 5 |
| config | 0/1 | 1 | 0 |
| (raiz) | 0/3 | 3 | 0 |

Toda a regra de negócio (`service`, `entity`) está em 100%.

### 5.1 Os 30 sobreviventes, por categoria

| Categoria | Qtde | Classe e método | Por que sobrevive |
|---|---:|---|---|
| CORS | 16 | `SecurityConfig.corsConfigurationSource` (4 por serviço) | Nenhum teste verifica os headers CORS da resposta. |
| Cadeia de filtros | 4 | `SecurityConfig.securityFilterChain` (1 por serviço) | Mutante devolve `null`; o mesmo caso existe no monólito. |
| Encoder de senha | 1 | `SecurityConfig.passwordEncoder` (user-service) | Mutante devolve `null` para o `@Bean`. |
| Corpo de erro | 4 | `GlobalExceptionHandler$ErrorResponse.of` (1 por serviço) | Os testes conferem o status HTTP do erro, não o corpo JSON. |
| Seeder | 3 | `DatabaseSeeder.seedUsers` e lambda (user-service) | Mesma categoria de 3 dos 6 sobreviventes do monólito. |
| Configuração HTTP | 1 | `RestClientConfig.restClientBuilder` (order-service) | Wiring de framework. |
| DTO de resposta | 1 | `OrderResponse$OrderItemResponse.from` (order-service) | Nenhum teste inspeciona os itens na resposta do pedido. |

**21 dos 30 sobreviventes estão na `SecurityConfig`, que existe copiada nos 4 serviços.** É
um efeito estrutural da decomposição: no monólito há uma `SecurityConfig`; aqui há quatro, e
a mesma lacuna de teste conta quatro vezes.

### 5.2 Os 6 sem cobertura

| Serviço | Método | Por quê |
|---|---|---|
| order-service | `GlobalExceptionHandler.handleInsufficientStock`, `handleProductNotFound`, `handleProductServiceUnavailable` | Nenhum teste propaga esses erros do product-service até o handler HTTP. |
| report-service | `GlobalExceptionHandler.handleAccessDenied`, `handleOrderServiceUnavailable` | O mesmo, para acesso negado e order-service indisponível. |
| user-service | `UserController.findById` | Endpoint sem teste. |

4 dos 6 são caminhos de falha de **serviço remoto** (os três do order-service, lançados pelo
`ProductClient` a partir da resposta HTTP, e `handleOrderServiceUnavailable`), que não existem
no monólito. A decomposição criou uma categoria de falha que a suíte não cobriu.
`handleAccessDenied` e `UserController.findById` são locais.

### 5.3 Análise de sensibilidade: sem código de configuração

Tirando da conta, dos dois lados, as classes de configuração e de inicialização
(`SecurityConfig`, `ApplicationConfig`, `RestClientConfig`, `DatabaseSeeder`):

| | Test Strength bruto | Test Strength sem configuração |
|---|---:|---:|
| Monólito | 137/143 = 95,8% | 126/127 = 99,2% |
| Microsserviços | 174/204 = 85,3% | 174/179 = 97,2% |

A diferença de Test Strength cai de 10,5 para 2,0 pontos percentuais. A diferença bruta vem
quase toda do código de configuração replicado, não de testes mais fracos em regra de
negócio.

---

## 6. Violações Arquiteturais (ArchUnit)

| Serviço | Regras (`@Test`) | Violações |
|---|---:|---:|
| user-service | 4 | 0 |
| product-service | 4 | 0 |
| order-service | 6 | 0 |
| report-service | 3 | 0 |
| **Total** | **17** | **0** |

Fonte: `surefire/<servico>/TEST-com.tcc.<servico>.architecture.ArchitectureTest.xml`
(0 falhas e 0 erros em todas).

As 17 regras são 6 regras distintas, replicadas conforme o serviço:

| Regra | user | product | order | report |
|---|:-:|:-:|:-:|:-:|
| Controller não acessa repository | ✓ | ✓ | ✓ | — |
| Controller não depende de entidade (`@Entity`) | ✓ | ✓ | ✓ | — |
| Convenção de nomes (Controller, Service, Repository) | ✓ | ✓ | ✓ | ✓ |
| Repository só é acessado por service | ✓ | ✓ | ✓ | — |
| Cliente HTTP só é acessado por service | — | — | ✓ | ✓ |
| Fronteira: não depende de pacotes de outro serviço | — | — | ✓ | ✓ |

O report-service não tem banco nem repositório, por isso não tem as regras de repository.

**Leitura: fronteira física contra disciplina.** No monólito, a fronteira entre domínios
depende de verificação ativa: as regras de ArchUnit já encontraram **11 violações reais**
(entidade vazando entre domínios, acesso ao repositório de outro domínio), que foram
corrigidas. Nos microsserviços, a regra de fronteira não tem como falhar, porque as classes
de outro serviço não existem no classpath. O "0 contra 0" sozinho não diz nada; o que importa
é que um lado precisa da regra para manter a fronteira e o outro tem a fronteira garantida
pela estrutura.

**A densidade de violações não é comparável diretamente:** as regras não são as mesmas dos
dois lados (7 no monólito, 17 aqui).

---

## 7. Acoplamento (CBO, `ck` 0.7.0)

### 7.1 Critério de contagem (correção)

O `ck` gera uma linha por tipo Java: classe, interface, enum e classe anônima. Os dois lados
filtraram de forma diferente:

- **Monólito:** excluiu só as classes anônimas (as constantes de `OrderStatus` com método
  próprio) e manteve interfaces e enums. São 29 tipos, CBO médio 7,45.
- **Relatório anterior de microsserviços:** manteve só `type = class`, ou seja, excluiu
  também os repositórios (interfaces) e os enums. Eram 48 tipos, CBO médio 6,71.

Calculado dos dois lados com cada critério:

| Critério | Monólito | Microsserviços |
|---|---:|---:|
| Sem classes anônimas (**critério do monólito, adotado aqui**) | 29 tipos · **7,45** | 53 tipos · **6,26** |
| Só `type = class` (critério do relatório anterior) | 24 tipos · 8,46 | 48 tipos · 6,71 |
| Todos os tipos | 34 tipos · 6,50 | 59 tipos · 5,71 |

A direção não muda em nenhum critério: o CBO médio dos microsserviços é menor. A magnitude
muda, e a comparação publicada antes (7,45 contra 6,71) misturava os dois critérios. O valor
de referência passa a ser **6,26**, pelo mesmo critério do monólito.

### 7.2 Resultado por serviço (critério do monólito)

| Serviço | Tipos | CBO médio | CBO máximo |
|---|---:|---:|---|
| user-service | 15 | 6,07 | 14 (`UserService`) |
| product-service | 11 | 6,73 | 17 (`ProductController`) |
| order-service | 18 | 6,17 | 19 (`OrderController`) |
| report-service | 9 | 6,22 | 11 (`SecurityConfig`) |
| **Agregado** | **53** | **6,26** | |

**Por camada (agregado):** controller 13,40 · service 9,50 · security 8,83 · client 8,00 ·
entity 6,33 · config 4,00 · repository 3,00 · exception 2,80 · raiz 2,40.

### 7.3 Ressalva: o acoplamento mudou de forma

Parte da queda de CBO vem do acoplamento entre domínios (por exemplo, pedido → produto) ter
deixado de ser dependência Java direta e virado contrato HTTP. Ele passou para os clientes
`ProductClient` (CBO 9) e `OrderClient` (CBO 7), e o resto virou formato de payload e
endpoint, que o CBO não mede. Esse acoplamento continua real e tem custo de teste: é por ele
que existem os testes com `MockRestServiceServer`. Não é correto ler 6,26 contra 7,45 como
"a decomposição reduziu o acoplamento entre os domínios".

Detalhe por classe: [`acoplamento-cbo.md`](acoplamento-cbo.md) e `ck/<servico>/class.csv`.

---

## 8. Determinismo e verificações independentes

| Execução | Máquina | JDK | Commit | Testes | Ramos | Mutantes mortos |
|---|---|---|---|---|---|---|
| Referência (Claudio, 2026-09-30) | DESKTOP-BDKDDA6 | Temurin 21.0.12.1 | `6231961` | 171 | 63/64 | 174/210 |
| Vinicius (2026-10-09) | não informado | não informado | não informado | 171 | 63/64 | 174/210 |
| Eric (2026-10-16) | KEKEP | Oracle 21.0.9 | `13e58b9` | 171 | 63/64 | 174/210 |
| Arquivos desta pasta (2026-10-19) | DESKTOP-BDKDDA6 | Temurin 21.0.12.1 | `13e58b9` | 171 | 63/64 | 174/210 |

Em cada execução, os testes foram rodados 3 vezes e o PITest 2 vezes, com `rodar-testes.bat`.
O `jacoco.csv` saiu idêntico byte a byte nas 3 rodadas e entre as máquinas, e os mutantes
tiveram o mesmo status nas 2 rodadas. Trocar o fornecedor e a versão do JDK (Oracle 21.0.9)
não mudou nenhum número, nem o total de 210 mutantes.

---

## 9. Linhagem dos números

| Data | O que mudou | Efeito |
|---|---|---|
| 2026-08 | Primeira suíte (97 testes) | Ramos entre 20% e 70% por serviço |
| 2026-09-13 | `@WebMvcTest`, testes do filtro JWT e 4 `@SpringBootTest` (equalização com o monólito); JDK 17 → 21 | 171 testes; ramos 96,4–100%; PITest 149 mutantes, Test Strength "100%" |
| 2026-09-30 | **Escopo do PITest alinhado ao do monólito** | 210 mutantes; Mutation Score 82,9%; Test Strength 85,3% |
| 2026-10-19 | **Critério do CBO alinhado ao do monólito** | CBO 6,71 → 6,26 |

**Por que o Test Strength caiu de 100% para 85,3%.** Até 30/09 o PITest mutava só
`entity/service/security`, e só os testes desses pacotes participavam. Os testes de
controller e os de integração ficavam de fora. O relatório da época atribuía os mutantes sem
cobertura da `SecurityConfig` a "um artefato do PITest com o proxy CGLIB". **Isso estava
errado:** com o escopo alinhado, esses métodos passam a ser executados pelos testes que sobem
o contexto Spring, e os mutantes sobrevivem. O "100%" era efeito do escopo restrito, não uma
propriedade da suíte.

**Por que o ramo residual mudou de explicação.** O relatório anterior atribuía o ramo não
coberto do order-service a `ProductServiceUnavailableException`. O relatório HTML do JaCoCo
mostra que ele está em `OrderController.findAll`, linha 62.

**Branch `resultados-e` (2026-10-12), fora da referência.** Uma execução sobre esse branch deu
203 testes e 187/210. O branch acrescenta 32 testes, sem mudar código de produção. A
comparação mutante a mutante mostrou o mesmo conjunto de 210 mutantes, com 13 passando a
mortos. O branch fica fora do `main` para preservar a referência; ver
[`RESULTADOS_METRICAS.md`](../../RESULTADOS_METRICAS.md).

---

## 10. Ameaças à validade

### Resolvidas

| # | Ameaça | Como foi resolvida |
|---|---|---|
| 1 | JDK divergente entre os lados | Os dois lados em JDK 21; a verificação com Oracle 21.0.9 confirmou que o JDK não altera os números |
| 2 | Versão do PITest divergente | Os dois lados em 1.15.3 |
| 3 | Escopo do PITest divergente | Alinhado em 2026-09-30 (pacote inteiro, todos os testes exceto `architecture.*`) |
| 4 | Falta de testes de integração nos microsserviços | 4 classes `@SpringBootTest` adicionadas (Opção A) |
| 5 | CBO sem par comparativo | `ck` 0.7.0 rodado dos dois lados |
| 6 | Critério de contagem do CBO divergente | Alinhado em 2026-10-19 (seção 7.1) |
| 7 | Medição não reproduzida | 3 execuções independentes, em 3 máquinas, com resultado idêntico |

### Em aberto

| # | Ameaça | Efeito na comparação |
|---|---|---|
| A | **Esforço de teste assimétrico:** 171 testes contra 98, e os 34 `@WebMvcTest` só existem aqui. Os 27 mutantes sem cobertura do monólito estão justamente nos controllers sem esse tipo de teste | A diferença de cobertura (98,4% contra 81,0%) não pode ser atribuída só à arquitetura |
| B | **Regras de ArchUnit diferentes** (7 contra 17) | A densidade de violações não é comparável diretamente |
| C | **Tamanho diferente:** 64 ramos contra 42 e 210 mutantes contra 170, parte por duplicação | Percentuais escondem o efeito de escala |
| D | **n = 1 por arquitetura**, domínio pequeno, mesmos autores nos dois lados | As conclusões valem para estas implementações, sem generalização |
| E | **Métricas determinísticas:** não há variância | A comparação é descritiva; teste de hipótese não se aplica |

---

## 11. Como reproduzir

```bat
:: na raiz do repositório (Windows): testes 3x + PITest 2x, gera resumo.txt
rodar-testes.bat

:: por serviço, manualmente
cd <servico>
mvn clean test
mvn org.pitest:pitest-maven:mutationCoverage
```

Para regenerar o `metricas.json` a partir dos arquivos brutos desta pasta:

```bash
python docs/reports/extrair_metricas.py
```

O CBO foi gerado com:

```bash
java -cp "<ck-0.7.0.jar e dependências>" com.github.mauricioaniche.ck.Runner \
    <servico>/src/main/java false 0 false docs/reports/ck/<servico>/
```

## Relatórios por métrica (.docx)

| Arquivo | Métrica |
|---|---|
| [`Relatorio-Cobertura-Codigo-JaCoCo.docx`](Relatorio-Cobertura-Codigo-JaCoCo.docx) | Branch Coverage |
| [`Relatorio-Mutation-Score-PITest.docx`](Relatorio-Mutation-Score-PITest.docx) | Mutation Score e Test Strength |
| [`Relatorio-Violacoes-Arquiteturais-ArchUnit.docx`](Relatorio-Violacoes-Arquiteturais-ArchUnit.docx) | Violações Arquiteturais |
| [`Relatorio-Acoplamento-CBO-ck.docx`](Relatorio-Acoplamento-CBO-ck.docx) | CBO |
