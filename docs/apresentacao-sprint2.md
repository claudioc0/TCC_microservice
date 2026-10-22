# Apresentação — Sprint 2 (banca)

Roteiro slide a slide da apresentação da Sprint 2 do TCC: implementação e teste da versão em
microsserviços, coleta das métricas e primeira comparação com o monólito.

## Instruções para gerar o PowerPoint

- **Um slide por seção `## Slide N`.** O título do slide é o texto depois do número.
- **"Mensagem"** é a frase principal do slide: deve aparecer em destaque, logo abaixo do
  título.
- **"Conteúdo"** vai no corpo do slide. Tabelas devem virar tabelas, sem resumir números.
- **"Visual"** descreve um gráfico ou diagrama a desenhar, já com os dados. Quando houver
  gráfico, ele substitui a tabela correspondente, ou fica ao lado dela se couber.
- **"Notas"** vão nas notas do apresentador, não no slide.
- Os slides marcados como **apoio** vão para o fim, depois do slide de encerramento, para uso
  nas perguntas.
- Os números são exatos e vêm de arquivos gerados pelas ferramentas. Não arredondar nem
  alterar casas decimais.
- Itens entre colchetes, como `[Nome do orientador]`, devem ficar como estão para serem
  preenchidos à mão.
- Estilo: sóbrio e acadêmico, fundo claro, uma cor de destaque. Em gráficos comparativos, usar
  sempre a mesma cor para o monólito e outra para os microsserviços.

---

## Slide 1 — [Título do TCC]

**Conteúdo:**
- Sprint 2: Implementação e teste da versão em microsserviços e comparação com o monólito
- Pontifícia Universidade Católica do Paraná — Bacharelado em Engenharia de Software
- Equipe: Claudio Colombo Ferreira, [Nome completo — Vinicius], [Nome completo — Eric]
- Orientador: [Nome do orientador]
- [Data da apresentação]

---

## Slide 2 — Roteiro

**Conteúdo:**
1. O que a Sprint 2 entregou
2. O sistema em microsserviços
3. Protocolo de medição
4. Resultados nas quatro métricas
5. Rigor metodológico: o que foi encontrado e corrigido
6. O que pode e o que não pode ser afirmado
7. Próximos passos (Sprint 3)

---

## Slide 3 — O que a Sprint 2 entregou

**Mensagem:** todas as tarefas da Sprint 2 estão concluídas, e cada número da comparação
pode ser rastreado até um arquivo gerado pelas ferramentas.

**Conteúdo:**

| Tarefa da Sprint 2 | Resultado |
|---|---|
| Testes unitários (JUnit 5 + Mockito) para cada serviço | 171 testes, 0 falhas |
| Testes de arquitetura (ArchUnit) por serviço | 17 regras, 0 violações |
| Coleta de métricas com base na execução dos testes | 3 execuções independentes, resultados idênticos |
| Documento de métricas dos microsserviços | Documento consolidado e 4 relatórios `.docx`, um por métrica |
| Comparação Monólito × Microsserviços | Análise descritiva nas 4 perguntas do GQM |

**Notas:** a entrega não é só o número de cada métrica. Os arquivos brutos (`jacoco.csv`,
`mutations.xml`, relatórios do Surefire e do ck) estão versionados, e as tabelas dos
relatórios são geradas por script a partir deles.

---

## Slide 4 — O sistema em microsserviços

**Mensagem:** o mesmo domínio do monólito, dividido em quatro serviços independentes que só se
comunicam por HTTP.

**Visual:** diagrama com quatro caixas (os serviços) e setas HTTP entre eles.
- `user-service` (porta 8085): cadastro, login e emissão do token JWT
- `product-service` (8082): catálogo e estoque
- `order-service` (8083): pedidos e máquina de status; seta HTTP para o `product-service`
- `report-service` (8084): relatório de vendas por período; seta HTTP para o `order-service`
- Cada serviço com seu próprio banco H2, desenhado como um cilindro ligado só a ele (o
  `report-service` não tem banco)
- O token JWT emitido pelo `user-service` é repassado em cada chamada entre serviços

**Conteúdo (abaixo do diagrama):**
- Cada serviço é um módulo Maven separado: não há classes compartilhadas entre eles
- Autenticação stateless: cada serviço valida o JWT sozinho, sem consultar o `user-service`
- Mesma pilha do monólito: Spring Boot 3.2, JDK 21, Spring Security, H2

**Notas:** a separação física em módulos é o ponto que mais pesa na comparação de arquitetura
(slide 11): a classe de um serviço simplesmente não existe no classpath de outro.

---

## Slide 5 — Protocolo de medição (GQM)

**Mensagem:** as duas arquiteturas foram medidas com as mesmas ferramentas, nas mesmas
versões e com a mesma configuração.

**Conteúdo:**

| Pergunta do GQM | Métrica | Ferramenta |
|---|---|---|
| Q1 — A suíte é adequada? | Branch Coverage | JaCoCo 0.8.11 |
| Q2 — A suíte é forte? | Mutation Score e Test Strength | PITest 1.15.3 |
| Q3 — A estrutura é íntegra? | Violações arquiteturais | ArchUnit 1.2.1 |
| Q4 — Quanto esforço de isolamento? | Acoplamento (CBO) | ck 0.7.0 |

- Ambiente nos dois lados: JDK 21.0.12.1 (Temurin), Maven 3.9
- PITest com o pacote inteiro como alvo, todos os testes exceto os de arquitetura, mutadores
  padrão
- **Test Strength** = mutantes mortos ÷ mutantes executados por algum teste. Mede a força das
  asserções, separando-a do alcance da suíte.

**Notas:** alinhar a configuração foi trabalho da própria sprint (slide 13). Até 30/09, o
PITest dos microsserviços tinha um escopo diferente do monólito.

---

## Slide 6 — Natureza da comparação

**Mensagem:** as métricas são determinísticas, então a comparação é descritiva: direção,
magnitude e mecanismo de cada diferença.

**Conteúdo:**
- O mesmo código dá sempre o mesmo número. Isso foi verificado: em 3 máquinas e com 2
  fornecedores de JDK, o `jacoco.csv` saiu idêntico byte a byte e os 210 mutantes tiveram o
  mesmo resultado
- Sem variância, não há teste de hipótese: p-valor e intervalo de confiança não se aplicam
- O resultado principal é o **mecanismo**: rastrear cada diferença até as classes e os testes
  que a explicam

**Notas:** antecipar a pergunta sobre estatística. A comparação não perde rigor por ser
descritiva; o rigor está em explicar cada diferença.

---

## Slide 7 — Equalização do esforço de teste

**Mensagem:** para comparar arquiteturas, e não suítes, o monólito ganhou os tipos de teste que
só os microsserviços tinham.

**Conteúdo:**

| Tipo de teste | Monólito (início) | Monólito equalizado | Microsserviços |
|---|---:|---:|---:|
| Unidade | 62 | 101 | 106 |
| Controller (`@WebMvcTest`) | 0 | 40 | 34 |
| Cliente HTTP | — | — | 8 |
| Integração (`@SpringBootTest`) | 29 | 29 | 6 |
| Arquitetura | 7 | 10 | 17 |
| **Total** | **98** | **180** | **171** |

**Visual:** barras empilhadas, uma barra por coluna da tabela, segmentos por tipo de teste.

**Notas:** a equalização não alterou nenhuma linha do código de produção do monólito. Os
números "início" são a medição da Sprint 1. Daqui em diante, a comparação principal é
monólito equalizado contra microsserviços.

---

## Slide 8 — Q1: Adequação da suíte (Branch Coverage)

**Mensagem:** com esforço de teste comparável, a cobertura das duas arquiteturas é
equivalente.

**Conteúdo:**

| | Monólito (início, 98 testes) | Monólito equalizado (180) | Microsserviços (171) |
|---|---|---|---|
| Ramos | 34/42 = 81,0% | 41/42 = 97,6% | 63/64 = 98,4% |
| Linhas | 407/450 = 90,4% | 448/450 = 99,6% | 610/637 = 95,8% |

- A diferença inicial de 17,4 pontos vinha da composição das suítes, não da arquitetura
- Medidos sem os `@WebMvcTest`, os microsserviços caem para 54/64 = 84,4%
- O sistema decomposto tem mais ramos para cobrir: 64 contra 42 (filtros JWT e handlers de
  erro replicados por serviço)

**Visual:** barras de cobertura de ramos: monólito início 81,0%, monólito equalizado 97,6%,
microsserviços 98,4%, microsserviços sem `@WebMvcTest` 84,4%.

---

## Slide 9 — Q2: Força da suíte (Mutation Score)

**Mensagem:** em regra de negócio, a força dos testes é a mesma; a diferença está toda no
código transversal.

**Conteúdo:**

| | Monólito equalizado | Microsserviços |
|---|---|---|
| Mutation Score (total) | 167/170 = 98,2% | 174/210 = 82,9% |
| Test Strength (total) | 167/170 = 98,2% | 174/204 = 85,3% |
| **Código de domínio** | **116/116 = 100,0%** | **123/125 = 98,4%** |
| **Código transversal** | **51/54 = 94,4%** | **51/85 = 60,0%** |

- Código transversal: segurança (JWT, `SecurityConfig`), tratamento global de erros,
  configuração e inicialização
- 34 dos 36 mutantes não mortos dos microsserviços estão no código transversal

**Visual:** barras agrupadas com dois grupos, "Domínio" e "Transversal", e duas barras em cada
(monólito e microsserviços): 100,0% e 98,4%; 94,4% e 60,0%.

---

## Slide 10 — O mecanismo: replicação do código transversal

**Mensagem:** nos microsserviços, cada teste transversal precisa ser escrito uma vez por
serviço.

**Conteúdo:**
- O código transversal gera 54 mutantes no monólito e 85 nos microsserviços: `SecurityConfig`,
  `JwtService`, `JwtAuthenticationFilter` e `GlobalExceptionHandler` existem em cada serviço

| Ponto do código transversal | Sobreviventes nos microsserviços | No monólito |
|---|---:|---|
| Configuração de CORS | 16 (4 por serviço) | morto por 1 teste em `SecurityConfigTest` |
| Corpo da resposta de erro | 4 (1 por serviço) | morto por `GlobalExceptionHandlerTest` |
| Seeder de usuários | 3 | morto por `DatabaseSeederTest` |

- Exemplo do CORS: no monólito, um teste cobre a configuração do sistema inteiro; nos
  microsserviços, a mesma verificação teria que existir em cada um dos 4 serviços

**Visual:** à esquerda, uma caixa "SecurityConfig" ligada a 1 teste (monólito); à direita,
quatro caixas "SecurityConfig" (uma por serviço), cada uma precisando do seu teste.

**Notas:** com os testes de erro e de seeder adicionados aos microsserviços (branch
`resultados-e`), o Mutation Score sobe para 187/210 = 89,0%, e 16 dos 23 sobreviventes
restantes são de CORS.

---

## Slide 11 — Q2: Caminhos de falha distribuída

**Mensagem:** a decomposição criou um tipo de falha que não existe no monólito e que exige
testes próprios.

**Conteúdo:**
- 4 dos 6 mutantes sem cobertura dos microsserviços estão em tratamento de falha de **serviço
  remoto**:
  - no `order-service`: estoque insuficiente, produto não encontrado e `product-service`
    indisponível (as três vindas da resposta HTTP do `product-service`)
  - no `report-service`: `order-service` indisponível
- No monólito, a mesma falha é uma exceção Java dentro do mesmo processo. O monólito
  equalizado não tem nenhum mutante sem cobertura.

**Notas:** é uma categoria de teste a mais que a arquitetura distribuída pede, e que uma suíte
construída com o mesmo esforço do monólito não cobriu.

---

## Slide 12 — Q3: Integridade estrutural (ArchUnit)

**Mensagem:** com as mesmas regras aplicadas aos dois códigos, os microsserviços passam em
todas; o monólito viola 3 de 8.

**Conteúdo:**

| Regra alinhada (mesmo teste nos dois códigos) | Monólito | Microsserviços |
|---|---:|---:|
| Controller não acessa repository | 0 | 0 |
| Service não depende de controller | 0 | 0 |
| Endpoint não retorna entidade | 0 | 0 |
| Controller não depende de entidade | **5** | 0 |
| Convenção de nomes | 0 | 0 |
| Repository só é acessado por service | **3** | 0 |
| Fronteira entre domínios (versão do monólito) | 0 | 0 |
| Fronteira entre domínios (versão estrita) | **21** | 0 |
| **Regras violadas** | **3 de 8** | **0 de 8** |

- As 5 violações de controller vêm da entidade `User` recebida como usuário autenticado; nos
  microsserviços, esse papel é feito pelas informações do token JWT
- **No monólito, a fronteira depende das regras que a equipe escreve. Nos microsserviços, ela é
  garantida pela separação física em módulos.**

**Notas:** cada lado passa nas próprias regras, mas os conjuntos eram diferentes (7 contra 17).
Por isso foi escrito um conjunto único e aplicado aos dois códigos.

---

## Slide 13 — Q4: Acoplamento (CBO)

**Mensagem:** o acoplamento entre domínios não diminuiu: ele saiu do código Java e virou
contrato de rede.

**Conteúdo:**
- CBO médio: monólito 7,45 (29 tipos), microsserviços 6,26 (53 tipos)
- As 21 dependências Java entre domínios do monólito correspondem diretamente às integrações
  que viraram HTTP:

| Dependência no monólito | Ocorrências | Nos microsserviços |
|---|---:|---|
| `ReportService` → `OrderService` | 6 | `OrderClient` (HTTP) |
| `OrderService` → `ProductService` | 5 | `ProductClient` (HTTP) |
| `OrderController` → entidade `User` | 4 | dados do token JWT |
| `OrderService` → `ProductResponse` | 3 | DTO local do `order-service` |
| `SalesReportResponse` → `OrderResponse` | 3 | DTO local do `report-service` |

- O CBO não mede contratos de rede (formato de payload, endpoint, disponibilidade), mas eles
  continuam sendo acoplamento, com custo de teste próprio

**Visual:** à esquerda, quatro caixas de domínio dentro de um retângulo "monólito", ligadas por
setas sólidas (dependência Java); à direita, quatro caixas separadas, ligadas por setas
tracejadas rotuladas "HTTP".

---

## Slide 14 — Síntese dos achados

**Mensagem:** a arquitetura não mudou a força dos testes sobre a regra de negócio; mudou onde e
quanto é preciso testar.

**Conteúdo:**

| # | Achado | Pergunta |
|---|---|---|
| 1 | Com esforço comparável, a cobertura é equivalente | Q1 |
| 2 | Em regra de negócio, a força dos testes é equivalente | Q2 |
| 3 | O código transversal replicado multiplica o custo de teste | Q2 |
| 4 | A decomposição cria caminhos de falha distribuída | Q2 |
| 5 | A separação física garante a fronteira entre domínios | Q3 |
| 6 | O acoplamento mudou de forma: virou contrato de rede | Q4 |

---

## Slide 15 — Rigor metodológico: o que a Sprint 2 encontrou e corrigiu

**Mensagem:** cada erro encontrado foi medido, corrigido e registrado, inclusive quando o número
ficou pior.

**Conteúdo:**

| Problema encontrado | Efeito da correção |
|---|---|
| O PITest dos microsserviços tinha escopo diferente do monólito, e os testes de controller e de integração não participavam | Test Strength foi de "100%" para 85,3% |
| O CBO dos dois lados usava critérios diferentes de contagem de classes | CBO dos microsserviços de 6,71 para 6,26 |
| A primeira medição do monólito equalizado veio de um build sujo, sem `mvn clean` | Mutation Score de 171/174 para 167/170 |
| As regras de paridade do monólito tinham sido relaxadas com exceções para passar | Voltaram a ser estritas, com as 8 violações documentadas |
| Uma verificação independente rodou um branch com testes extras e deu números diferentes | Divergência explicada mutante a mutante: era outro código, não erro de medição |
| O Git convertia quebras de linha dos arquivos brutos e mudava os hashes MD5 | Arquivos brutos passaram a ser guardados byte a byte |

**Notas:** a maior parte desses problemas foi encontrada porque os números de um lado eram
conferidos contra os arquivos brutos e contra a reprodução em outra máquina. É o mesmo padrão
da Sprint 1, agora aplicado aos dois lados.

---

## Slide 16 — Reprodutibilidade

**Mensagem:** três pessoas, em três máquinas, chegaram aos mesmos números, byte a byte.

**Conteúdo:**

| Execução | JDK | Testes | Ramos | Mutantes mortos |
|---|---|---:|---:|---:|
| Claudio (referência) | Temurin 21.0.12.1 | 171 | 63/64 | 174/210 |
| Vinicius | [não informado] | 171 | 63/64 | 174/210 |
| Eric | Oracle 21.0.9 | 171 | 63/64 | 174/210 |

- Cada execução rodou os testes 3 vezes e o PITest 2 vezes, pelo script `rodar-testes.bat`
- Os hashes MD5 do relatório de cobertura foram idênticos em todas as rodadas e máquinas
- Trocar o fornecedor e a versão do JDK não mudou nenhum número

**Notas:** o script de execução padronizada (`rodar-testes.bat`) foi criado nesta sprint para
que qualquer integrante reproduza a coleta com um comando.

---

## Slide 17 — O que pode e o que não pode ser afirmado

**Mensagem:** as conclusões valem para estas duas implementações; não se generalizam.

**Conteúdo:**

**Pode ser afirmado:**
- Com esforço comparável, as duas arquiteturas atingem a mesma cobertura
- A força dos testes sobre a regra de negócio é praticamente a mesma
- A replicação do código transversal multiplica o número de testes necessários
- A separação física garante a fronteira entre domínios

**Não pode ser afirmado:**
- "Microsserviços são mais (ou menos) testáveis"
- "A decomposição reduziu o acoplamento entre domínios"
- Qualquer diferença "estatisticamente significativa"
- Generalização para outros sistemas: há uma implementação de cada arquitetura (n = 1)

---

## Slide 18 — Ameaças à validade em aberto

**Conteúdo:**
- **n = 1 por arquitetura**, domínio pequeno (29 tipos no monólito, 53 nos microsserviços),
  quatro serviços
- **Esforço de teste ainda não idêntico:** 29 testes de integração no monólito contra 6, e
  testes transversais (CORS, corpo de erro, seeder) que só o monólito tem
- **A equalização do monólito foi feita por quem já conhecia as lacunas** da suíte dos
  microsserviços
- **O CBO não mede acoplamento de rede**, por isso é complementado pela análise de
  dependências do slide 13
- **Mesmos autores e desenvolvimento assistido por IA nos dois lados**

**Notas:** o achado 3 depende de o código transversal ser copiado em cada serviço. Com uma
biblioteca comum para segurança e tratamento de erros, o resultado poderia ser outro.

---

## Slide 19 — Próximos passos (Sprint 3)

**Conteúdo:**
- Redigir os capítulos de Resultados, Discussão e Conclusão, a partir da análise comparativa
- Escrever a seção de Ameaças à Validade
- Revisão geral de formatação ABNT
- Preparar a apresentação final

---

## Slide 20 — Encerramento

**Conteúdo:**
- Obrigado
- Perguntas?
- Repositórios: `github.com/claudioc0/TCC_microservice` e `github.com/claudioc0/TCC_monolith`

---

## Slide 21 (apoio) — Por que não há teste estatístico?

**Conteúdo:**
- As quatro métricas são determinísticas: o mesmo código, com o mesmo ambiente, dá sempre o
  mesmo número
- Repetir a medição não gera uma amostra, gera o mesmo valor; não há variância para estimar
- O que dá confiança aos números é a reprodução independente (3 máquinas, resultado idêntico)
  e a explicação de cada diferença até a classe e o teste responsáveis

---

## Slide 22 (apoio) — Por que o Test Strength dos microsserviços caiu de 100% para 85,3%?

**Conteúdo:**
- Até 30/09, o PITest dos microsserviços só mutava `entity`, `service` e `security`, e só os
  testes desses pacotes rodavam
- Os testes de controller e de integração ficavam de fora, e os métodos da `SecurityConfig`
  apareciam como "sem cobertura"
- Com o mesmo escopo do monólito, esses métodos passam a ser executados, e os mutantes
  sobrevivem: o "100%" era efeito do escopo, não da suíte

---

## Slide 23 (apoio) — Análise de sensibilidade: sem código de configuração

**Conteúdo:**
- Critério definido como regra objetiva: excluir as classes anotadas com `@Configuration` ou
  `@SpringBootApplication`, igualmente nos dois lados

| | Test Strength bruto | Sem código de configuração |
|---|---:|---:|
| Monólito equalizado | 98,2% | 99,4% |
| Microsserviços | 85,3% | 97,2% |

- Sem a configuração replicada, a diferença cai de 12,9 para 2,2 pontos

**Notas:** o critério foi formalizado depois de uma primeira análise exploratória; por isso ele
é uma regra mecânica, aplicada aos dois lados, e não uma lista escolhida caso a caso.

---

## Slide 24 (apoio) — Por que o CBO mudou de 6,71 para 6,26?

**Conteúdo:**
- O ck gera uma linha por tipo Java: classe, interface, enum e classe anônima
- O monólito excluía só as classes anônimas; os microsserviços excluíam também interfaces e
  enums

| Critério | Monólito | Microsserviços |
|---|---:|---:|
| Sem classes anônimas (adotado nos dois lados) | 7,45 | 6,26 |
| Só classes | 8,46 | 6,71 |
| Todos os tipos | 6,50 | 5,71 |

- Em qualquer critério, o CBO dos microsserviços é menor; a magnitude é que muda

---

## Fontes (não gerar slide)

Todos os números deste roteiro vêm dos documentos do repositório `TCC_microservice`:
- `docs/reports/analise-comparativa.md` — comparação nas quatro métricas
- `docs/reports/resultados-finais.md` — métricas dos microsserviços e verificações
- `docs/reports/monolito-equalizado/` — reprodução do monólito equalizado
- `docs/reports/arquitetura-alinhada/` — regras ArchUnit aplicadas aos dois códigos
