# Análise Comparativa — Monólito × Microsserviços

Texto de base para o capítulo de resultados do TCC. Compara as duas arquiteturas nas quatro
métricas do protocolo GQM. Cada número vem de um arquivo gerado pelas ferramentas e
versionado nesta pasta ou no repositório `TCC_monolith`. A seção 10 lista as fontes.

---

## 1. Natureza da comparação

### 1.1 Métricas determinísticas

As quatro métricas são **determinísticas**: o mesmo código, com o mesmo JDK e as mesmas
ferramentas, produz sempre o mesmo número. Isso foi verificado empiricamente. Nos
microsserviços, três execuções independentes, em três máquinas e com dois fornecedores de
JDK, produziram `jacoco.csv` idênticos byte a byte e o mesmo estado para cada um dos 210
mutantes. No monólito, os dois estados medidos foram repetidos (3 rodadas de testes, 2 de
PITest) com resultado idêntico.

Duas consequências metodológicas:

1. **Não há variância amostral.** Teste de hipótese, p-valor e intervalo de confiança não se
   aplicam: repetir a medição produz o mesmo valor, não uma distribuição.
2. **A comparação é descritiva.** Para cada métrica, o texto reporta a **direção**, a
   **magnitude** e o **mecanismo** da diferença, rastreado até as classes e os testes
   responsáveis. O mecanismo é o resultado principal: é ele que separa o efeito da
   arquitetura de outros fatores.

### 1.2 Estados comparados

| Estado | Repositório e commit | Testes | Papel na análise |
|---|---|---:|---|
| Monólito, baseline | `TCC_monolith` `1c05487` | 98 | Histórico: medição antes da equalização |
| **Monólito, equalizado** | `TCC_monolith` `518b8c9` (branch `feature/equalizacao-testes-monolito`) | **180** | **Estado principal do monólito** |
| **Microsserviços** | `TCC_microservice` `13e58b9` (`main`) | **171** | **Estado principal dos microsserviços** |
| Microsserviços + testes transversais | `TCC_microservice` `0a3f88b` (branch `resultados-e`) | 203 | Análise de sensibilidade (4.4) |

O código de produção é o mesmo em todos os estados de um mesmo lado. Só a suíte de testes
muda. A equalização do monólito acrescentou slices `@WebMvcTest` para os 5 controllers,
testes de entidade, testes de `DatabaseSeeder` e 3 regras ArchUnit de paridade.

Com a equalização, a composição das duas suítes ficou comparável:

| Tipo de teste | Monólito baseline | Monólito equalizado | Microsserviços |
|---|---:|---:|---:|
| Unidade | 62 | 101 | 106 |
| Controller (`@WebMvcTest`) | 0 | 40 | 34 |
| Cliente HTTP (`MockRestServiceServer`) | — | — | 8 |
| Integração (`@SpringBootTest`) | 29 | 29 | 6 |
| Arquitetura (ArchUnit) | 7 | 10 | 17 |
| **Total** | **98** | **180** | **171** |

A assimetria que resta está nos testes de integração (29 contra 6) e, como mostra a seção
4.3, em testes unitários de código transversal que o monólito tem e os microsserviços não.

### 1.3 Análises complementares

| Análise | Pergunta que responde | Seção |
|---|---|---|
| Comparação por domínio | A diferença está no código de negócio ou no código transversal? | 3.2, 4.2, 6.2 |
| Antes e depois da equalização | Quanto da diferença vinha da composição da suíte? | 3.3, 4.5 |
| Sensibilidade a testes transversais | O que muda quando os microsserviços ganham os testes transversais que faltam? | 4.4 |
| Sensibilidade sem configuração | Quanto da diferença vem do código de configuração de framework? | 4.6 |
| Regras ArchUnit alinhadas | Os dois códigos passam no mesmo conjunto de regras? | 5.2 |

---

## 2. Tabela base

| Métrica (pergunta GQM) | Monólito equalizado | Microsserviços | Direção |
|---|---|---|---|
| Testes | 180 | 171 | — |
| Branch Coverage (Q1) | 41/42 = 97,6% | 63/64 = 98,4% | equivalente (0,8 p.p.) |
| Mutation Score (Q2) | 167/170 = 98,2% | 174/210 = 82,9% | monólito +15,3 p.p. |
| Test Strength (Q2) | 167/170 = 98,2% | 174/204 = 85,3% | monólito +12,9 p.p. |
| Violações ArchUnit, regras alinhadas (Q3) | 3 de 8 regras violadas | 0 de 8 | microsserviços |
| CBO médio (Q4) | 7,45 (29 tipos) | 6,26 (53 tipos) | microsserviços −1,19 |

Ambiente nos dois lados: JDK 21.0.12.1 Temurin, JaCoCo 0.8.11, PITest 1.15.3 (mutadores
`DEFAULTS`, pacote inteiro como alvo, testes de arquitetura excluídos), ArchUnit 1.2.1, ck
0.7.0. O CBO não muda com a equalização, porque o código de produção é o mesmo.

> **Correção sobre o relatório do monólito equalizado.** O `monolito-mutation-score.md` do
> commit `518b8c9` registra 171/174 = 98,3%. Reproduzido em JDK 21.0.12.1, o resultado é
> **167/170 = 98,2%**, com os mesmos 3 sobreviventes. 174 é o total de mutantes que o
> monólito gera em JDK 17; em JDK 21 são 170. O mesmo relatório dá 424/467 linhas para o
> baseline, e o valor verificado é 407/450. Os números desta análise vêm da reprodução em
> `monolito-equalizado/`.

---

## 3. Q1 — Adequação da suíte (Branch Coverage)

### 3.1 Comparação direta

**Direção e magnitude:** monólito equalizado 97,6% (41/42), microsserviços 98,4% (63/64).
A diferença é de 0,8 ponto, o equivalente a menos de um ramo em cada sistema.

| | Monólito equalizado | Microsserviços |
|---|---|---|
| Ramos | 41/42 = 97,6% | 63/64 = 98,4% |
| Linhas | 448/450 = 99,6% | 610/637 = 95,8% |
| Ramo em aberto | `JwtService.isTokenExpired` (inalcançável: o JJWT lança `ExpiredJwtException` antes) | `OrderController.findAll` (filtro com uma só data) |

**Escala:** o sistema decomposto tem 64 ramos contra 42. Os ramos a mais vêm do código
replicado por serviço (quatro filtros JWT, quatro handlers de erro) e dos clientes HTTP.

### 3.2 Por domínio

| Recorte | Monólito equalizado | Microsserviços |
|---|---|---|
| Código de domínio (4 domínios) | 22/22 = 100,0% | 33/34 = 97,1% |
| Código transversal | 19/20 = 95,0% | 30/30 = 100,0% |

### 3.3 Antes e depois da equalização

| | Monólito baseline (98) | Monólito equalizado (180) | Microsserviços (171) | Microsserviços sem `@WebMvcTest` (137) |
|---|---|---|---|---|
| Ramos | 34/42 = 81,0% | 41/42 = 97,6% | 63/64 = 98,4% | 54/64 = 84,4% |

A diferença de 17,4 pontos da comparação inicial vinha da composição das suítes. Ela pode
ser medida de duas formas, e as duas apontam para o mesmo lado: tirando os `@WebMvcTest` dos
microsserviços, a diferença cai para 3,4 pontos; acrescentando testes equivalentes ao
monólito, cai para 0,8 ponto.

**Conclusão para Q1:** com esforço de teste comparável, a adequação das duas suítes é
equivalente. A arquitetura não tornou nenhum dos dois sistemas mais fácil ou mais difícil de
cobrir. Ela aumentou o número de ramos a cobrir (64 contra 42).

---

## 4. Q2 — Força da suíte (Mutation Score e Test Strength)

### 4.1 Comparação direta

| | Monólito equalizado | Microsserviços |
|---|---|---|
| Mutantes | 170 | 210 |
| Mortos | 167 | 174 |
| Sobreviventes | 3 | 30 |
| Sem cobertura | 0 | 6 |
| Mutation Score | 98,2% | 82,9% |
| Test Strength | 98,2% | 85,3% |

Com esforço comparável, o monólito fica 15,3 pontos acima em Mutation Score e 12,9 em Test
Strength. As seções seguintes mostram onde está essa diferença.

### 4.2 Por domínio

| Recorte | Monólito equalizado | Microsserviços |
|---|---|---|
| **Domínio**: Mutation Score | 116/116 = 100,0% | 123/125 = 98,4% |
| **Domínio**: Test Strength | 116/116 = 100,0% | 123/124 = 99,2% |
| **Domínio**: sobreviventes / sem cobertura | 0 / 0 | 1 / 1 |
| **Transversal**: mutantes | 54 | 85 |
| **Transversal**: Mutation Score | 51/54 = 94,4% | 51/85 = 60,0% |
| **Transversal**: sobreviventes / sem cobertura | 3 / 0 | 29 / 5 |

Por domínio:

| Domínio | Monólito equalizado | Microsserviços |
|---|---|---|
| Pedidos | 51/51 = 100,0% | 51/52 = 98,1% |
| Produtos | 33/33 = 100,0% | 37/37 = 100,0% |
| Usuários | 30/30 = 100,0% | 24/25 = 96,0% |
| Relatórios | 2/2 = 100,0% | 11/11 = 100,0% |

(Os clientes HTTP dos microsserviços estão contados no domínio que os usa.)

**Em regra de negócio, a força dos testes é equivalente:** Mutation Score de domínio de
100,0% contra 98,4%. Os dois resíduos de domínio dos microsserviços são um método de DTO
(`OrderItemResponse.from`, sobrevivente) e `UserController.findById` (sem cobertura). **A
diferença de Q2 está quase toda no código transversal:** 34 dos 36 mutantes não mortos dos
microsserviços (29 sobreviventes e 5 sem cobertura) estão nele.

### 4.3 O mecanismo: o código transversal é replicado, e os testes dele também precisam ser

O código transversal do monólito gera 54 mutantes; o dos microsserviços gera 85, porque
`SecurityConfig`, `JwtService`, `JwtAuthenticationFilter` e `GlobalExceptionHandler` existem
uma vez por serviço.

Os 29 sobreviventes transversais dos microsserviços se concentram em três pontos. Os três
estão cobertos no monólito por testes unitários que os microsserviços não têm:

| Ponto | Sobreviventes nos microsserviços | Teste que o mata no monólito | Equivalente nos microsserviços |
|---|---:|---|---|
| CORS (`SecurityConfig.corsConfigurationSource`) | 16 (4 por serviço) | `SecurityConfigTest`, 1 teste chamando o método de fábrica direto | nenhum |
| Corpo da resposta de erro (`ErrorResponse.of`) | 4 (1 por serviço) | `GlobalExceptionHandlerTest`, que verifica o corpo | nenhum (os testes verificam só o status) |
| `DatabaseSeeder` | 3 | `DatabaseSeederTest` | nenhum |

Os outros 6 sobreviventes transversais (`securityFilterChain` ×4, `passwordEncoder`,
`RestClientConfig`) são wiring de framework. Os 2 sobreviventes desse tipo no monólito
(`securityFilterChain` e `authenticationManager`) são da mesma categoria.

A diferença de Q2, portanto, não é de qualidade das asserções em regra de negócio. É de
**testes transversais presentes de um lado e ausentes do outro**, e o custo de escrevê-los é
diferente nos dois lados. No monólito, um teste em `SecurityConfigTest` cobre a configuração
de CORS do sistema inteiro. Nos microsserviços, a mesma verificação precisa ser escrita uma
vez em cada serviço, porque cada um tem a sua cópia da configuração.

### 4.4 Sensibilidade: microsserviços com os testes transversais

O branch `resultados-e` dos microsserviços acrescenta exatamente dois dos três tipos de teste
da tabela acima (`GlobalExceptionHandlerTest` nos 4 serviços e `DatabaseSeederTest`), além de
`UserControllerTest`. Ele não tem teste de CORS.

| | Monólito equalizado | Microsserviços (`main`) | Microsserviços (`resultados-e`) |
|---|---|---|---|
| Testes | 180 | 171 | 203 |
| Mutation Score | 167/170 = 98,2% | 174/210 = 82,9% | 187/210 = 89,0% |
| Sobreviventes | 3 | 30 | 23 |
| Sem cobertura | 0 | 6 | 0 |

Com esses testes, a diferença de Mutation Score cai de 15,3 para 9,2 pontos. Dos 23
sobreviventes que restam, 21 estão nas quatro `SecurityConfig` (16 de CORS). A diferença
restante corresponde ao teste de CORS que existe uma vez no monólito e não existe em nenhum
dos quatro serviços.

### 4.5 Antes e depois da equalização

| | Monólito baseline (98) | Monólito equalizado (180) | Microsserviços (171) | Microsserviços sem `@WebMvcTest` (137) |
|---|---|---|---|---|
| Mutation Score | 137/170 = 80,6% | 167/170 = 98,2% | 174/210 = 82,9% | 159/210 = 75,7% |
| Test Strength | 137/143 = 95,8% | 167/170 = 98,2% | 174/204 = 85,3% | 159/189 = 84,1% |
| Sem cobertura | 27 | 0 | 6 | 21 |
| Sobreviventes | 6 | 3 | 30 | 30 |

Na comparação inicial, o Mutation Score dos dois lados era parecido (80,6% contra 82,9%), mas
por razões opostas: o monólito perdia por alcance (27 sem cobertura) e os microsserviços por
asserção (30 sobreviventes). A equalização eliminou os 27 mutantes sem cobertura do monólito.
Os 30 sobreviventes dos microsserviços não dependem dos `@WebMvcTest`: são os mesmos com e
sem eles.

### 4.6 Sensibilidade: sem código de configuração de framework

**Critério de exclusão:** classes anotadas com `@Configuration` ou `@SpringBootApplication`.
É uma regra objetiva, aplicada igualmente aos dois códigos. Ela seleciona `SecurityConfig`,
`ApplicationConfig`, `DatabaseSeeder` e `MonolithApplication` no monólito, e as quatro
`SecurityConfig`, `RestClientConfig`, `DatabaseSeeder` e as quatro `*Application` nos
microsserviços.

| | Test Strength bruto | Test Strength sem configuração |
|---|---|---|
| Monólito baseline | 137/143 = 95,8% | 126/127 = 99,2% |
| Monólito equalizado | 167/170 = 98,2% | 153/154 = 99,4% |
| Microsserviços | 174/204 = 85,3% | 174/179 = 97,2% |

Sem o código de configuração, a diferença entre monólito equalizado e microsserviços cai de
12,9 para 2,2 pontos. O resíduo que fica (`ErrorResponse.of` ×4 e `OrderItemResponse.from`,
nos microsserviços; `JwtService.isTokenExpired`, no monólito) também é transversal ou de DTO.

> **Transparência sobre o critério:** a análise foi feita primeiro de forma exploratória, com
> uma lista de classes, e o critério por anotação foi formalizado depois. Ele seleciona
> exatamente as mesmas classes da lista, mas não é um critério pré-registrado. A mitigação é
> que ele é uma regra mecânica, aplicada igualmente aos dois lados.

### 4.7 Caminhos de falha distribuída

4 dos 6 mutantes sem cobertura dos microsserviços estão nos handlers de erro de **serviço
remoto**: no order-service, estoque insuficiente, produto não encontrado e product-service
indisponível (as três exceções são lançadas pelo `ProductClient` a partir da resposta HTTP);
no report-service, order-service indisponível. Esses caminhos não existem no monólito, onde a
mesma falha é uma exceção Java no mesmo processo. O monólito equalizado não tem nenhum
mutante sem cobertura.

**Conclusão para Q2:** em regra de negócio, a força dos testes é equivalente nas duas
arquiteturas. A diferença está no código transversal, e o mecanismo é a replicação: os
microsserviços têm uma cópia do código transversal por serviço, e cada teste transversal que
o monólito escreve uma vez precisa ser escrito em cada serviço. Com o mesmo esforço total, a
suíte do monólito equalizado chegou a esses testes, e a dos microsserviços não. A
decomposição também criou caminhos de falha remota que exigem testes próprios.

---

## 5. Q3 — Integridade estrutural (Violações ArchUnit)

### 5.1 Comparação direta

Cada lado passa nas próprias regras: monólito equalizado com 0 violações em 10 regras,
microsserviços com 0 em 17. Os dois números não são comparáveis. Os conjuntos de regras são
diferentes, e **duas das três regras de paridade do monólito equalizado foram relaxadas** com
exceções:

- `repositoriesShouldOnlyBeAccessedByServices` aceita acesso a partir de `..security..`, além
  de `..service..`;
- `controllersShouldNotDependOnEntities` exclui a entidade `User`.

As violações estão documentadas no Javadoc da regra e no relatório do monólito, mas a regra
que roda já não é a mesma dos microsserviços.

### 5.2 Regras alinhadas

Para comparar, foi escrito um conjunto único de regras, a união das regras distintas dos dois
lados, **sem exceções**, e aplicado aos dois códigos com o mesmo teste ArchUnit. A regra de
fronteira entre domínios foi aplicada em duas versões: a do monólito, que olha só `service` e
`repository`, e uma estrita, que olha qualquer classe. O resultado vale para os dois estados
do monólito, porque o código de produção é o mesmo.

| Regra | Origem | Monólito | Microsserviços (4 serviços) |
|---|---|---:|---:|
| A1 Controller não acessa repository | os dois | 0 | 0 |
| A2 Service não depende de controller | monólito | 0 | 0 |
| A3 Endpoint não retorna entidade (`ResponseEntity<Entidade>`) | monólito | 0 | 0 |
| A4 Controller não depende de entidade (`@Entity`) | microsserviços | **5** | 0 |
| A5 Convenção de nomes (Controller, Service, Repository) | microsserviços | 0 | 0 |
| A6 Repository só é acessado por service | microsserviços | **3** | 0 |
| A7a Fronteira: service/repository não acessa entity/repository de outro domínio | monólito | 0 | 0 |
| A7b Fronteira estrita: nenhuma classe depende de outro domínio | nova | **21** | 0 |
| **Regras violadas** | | **3 de 8** | **0 de 8** |

O que são as violações do monólito:

- **A4 (5):** `OrderController.create`, `OrderController.myOrders` e `UserController.me`
  recebem a entidade `User` via `@AuthenticationPrincipal`. Nos microsserviços, o mesmo papel
  é cumprido por `AuthenticatedPrincipal`, montado a partir das claims do JWT, porque a
  entidade `User` só existe no user-service.
- **A6 (3):** `ApplicationConfig.userDetailsService` e `DatabaseSeeder.seedAdmin` acessam o
  `UserRepository` diretamente, a partir do pacote `security`.
- **A7b (21):** as dependências Java entre domínios. Com A7a em zero, elas não são acesso
  indevido a internals: são chamadas entre serviços de domínio, permitidas pela arquitetura
  do monólito (ver seção 6.3).

A equalização confirma esse resultado por outro caminho: ao adicionar as regras de paridade,
o monólito encontrou exatamente as mesmas 8 violações de A4 e A6, e optou por documentá-las
e criar exceções em vez de mudar o código de produção.

**Leitura: fronteira física contra disciplina.** As regras do monólito passam, e a equipe já
corrigiu 11 violações reais ao longo do projeto. Mas um conjunto de regras um pouco mais
estrito revela 8 pontos de dependência que o desenho permitiu, e a forma de "passar" nessas
regras foi relaxá-las. Nos microsserviços, as mesmas regras passam em todos os serviços sem
exceção, e a regra de fronteira não tem como falhar, porque a classe de outro serviço não
existe no classpath. **No monólito, a integridade depende de quais regras a equipe decide
escrever e de quais exceções aceita. Nos microsserviços, a fronteira entre domínios é
garantida pela separação física em módulos.**

**Conclusão para Q3:** com regras alinhadas e sem exceções, os microsserviços têm
integridade estrutural maior (0 de 8 regras violadas, contra 3 de 8). Uma parte dessa
diferença é imposta pela arquitetura (A4, A7b): a decomposição obriga a trocar a entidade
compartilhada por um contrato. Outra parte é de desenho e poderia ser corrigida no monólito
(A6).

---

## 6. Q4 — Acoplamento (CBO)

O CBO é medido sobre o código de produção e não muda com a equalização das suítes.

### 6.1 Comparação direta

Monólito 7,45 (29 tipos), microsserviços 6,26 (53 tipos): 1,19 a menos nos microsserviços.
A direção se mantém nos três critérios de contagem testados (ver `resultados-finais.md`,
seção 7.1).

### 6.2 Por domínio

| Recorte | Monólito | Microsserviços |
|---|---|---|
| Código de domínio | 22 tipos · 6,95 | 31 tipos · 5,45 |
| Código transversal | 7 tipos · 9,00 | 22 tipos · 7,41 |

A redução aparece nos dois recortes. No domínio, o principal fator é a troca da entidade
`User` e dos serviços de outros domínios por clientes e DTOs locais. No código transversal
(22 tipos contra 7), há dois fatores. Primeiro, a autenticação stateless: o monólito tem
`ApplicationConfig` (CBO 12), que carrega usuários do banco para o Spring Security, e nos
microsserviços ela não existe, porque cada serviço só lê as claims do JWT; pelo mesmo motivo,
cada cópia de `JwtAuthenticationFilter` e `JwtService` é um pouco menos acoplada (10 e 5,
contra 11 e 7). Segundo, as quatro classes `*Application`, com CBO 2, entram na média.

### 6.3 O acoplamento mudou de forma

As 21 dependências Java entre domínios do monólito (regra A7b) são exatamente as integrações
que, nos microsserviços, viraram comunicação remota:

| Dependência no monólito | Ocorrências | Equivalente nos microsserviços |
|---|---:|---|
| `ReportService` → `OrderService` | 6 | `OrderClient` (HTTP, CBO 7) |
| `OrderService` → `ProductService` | 5 | `ProductClient` (HTTP, CBO 9) |
| `OrderController` → entidade `User` | 4 | `AuthenticatedPrincipal` (claims do JWT) |
| `OrderService` → `ProductResponse` | 3 | `ProductDto` local do order-service |
| `SalesReportResponse` → `OrderResponse` | 3 | `OrderSummaryDto` local do report-service |

O acoplamento entre domínios não desapareceu. Ele saiu do código Java, onde o CBO o conta,
e passou para contratos de rede: formato de payload, endpoint, propagação do token e
disponibilidade do serviço remoto. O CBO não mede esses contratos. Eles continuam sendo
acoplamento real e têm custo de teste: são eles que exigem os testes com
`MockRestServiceServer` e que geraram os caminhos de falha da seção 4.7.

**Conclusão para Q4:** o acoplamento estático em código Java é menor nos microsserviços, nos
dois recortes e em qualquer critério de contagem. Não é correto concluir que a decomposição
reduziu o acoplamento entre os domínios: ela trocou dependências contáveis por contratos de
rede que a métrica não enxerga.

---

## 7. Síntese dos achados

| # | Achado | Pergunta | Evidência |
|---|---|---|---|
| 1 | Com esforço de teste comparável, a adequação das suítes é equivalente | Q1 | 97,6% contra 98,4% de ramos (3.1); a diferença inicial de 17,4 pontos vinha da composição das suítes (3.3) |
| 2 | Em regra de negócio, a força dos testes é equivalente | Q2 | Mutation Score de domínio 100,0% contra 98,4% (4.2) |
| 3 | O código transversal replicado multiplica o custo de teste | Q2 | 85 mutantes transversais contra 54; 29 dos 30 sobreviventes; cada teste transversal do monólito precisaria existir uma vez por serviço (4.3, 4.4) |
| 4 | A decomposição cria caminhos de falha distribuída que exigem testes próprios | Q2 | 4 dos 6 mutantes sem cobertura em handlers de falha remota (4.7) |
| 5 | Com regras alinhadas e sem exceções, os microsserviços têm integridade estrutural maior, e parte dela é imposta pela separação física | Q3 | 0 de 8 regras violadas contra 3 de 8; o monólito só passa nas regras de paridade com exceções (5.1, 5.2) |
| 6 | O acoplamento mudou de forma, não necessariamente de quantidade | Q4 | 21 dependências Java entre domínios no monólito, com correspondência direta com os clientes HTTP e o principal JWT (6.3) |

---

## 8. O que pode e o que não pode ser afirmado

### Pode ser afirmado (para estas duas implementações)

- Com esforço de teste comparável, as duas arquiteturas atingem a mesma cobertura de ramos.
- A força dos testes sobre a regra de negócio é praticamente a mesma nas duas arquiteturas.
- A decomposição replicou o código transversal, e isso multiplicou os mutantes e o número de
  testes necessários para cobri-lo: um teste que basta uma vez no monólito precisa existir em
  cada serviço.
- A decomposição criou uma categoria de caminho de falha (falha de serviço remoto) que não
  existe no monólito e que exige testes próprios.
- A separação física em módulos garante a fronteira entre domínios sem depender de regras e
  exceções escolhidas pela equipe.
- O acoplamento estático em código Java é menor nos microsserviços, e o acoplamento entre
  domínios passou a ser de contrato de rede.

### Não pode ser afirmado

| Afirmação | Por que não se sustenta |
|---|---|
| "Microsserviços têm cobertura maior" | Com esforço comparável, a diferença é de 0,8 ponto (3.1) |
| "Os testes do monólito são mais fortes" | Em regra de negócio, 100,0% contra 98,4%; a diferença está em testes transversais que um lado escreveu e o outro não (4.3) |
| "Microsserviços são menos testáveis" | A força em regra de negócio é igual; o que muda é o **custo** de testar o código transversal, não a possibilidade |
| "A decomposição reduziu o acoplamento entre domínios" | O acoplamento migrou para contratos de rede, que o CBO não mede (6.3) |
| "O monólito equalizado tem 0 violações, como os microsserviços" | As regras de paridade do monólito têm exceções; sem elas, são 8 violações em 2 regras (5.1) |
| Qualquer diferença "estatisticamente significativa" | Métricas determinísticas, sem variância; teste de hipótese não se aplica (1.1) |
| Generalização para monólitos e microsserviços em geral | n = 1 por arquitetura (ver 9) |

---

## 9. Ameaças à validade

### Validade de construção

- **CBO não mede acoplamento de rede.** O achado 6 depende de complementar o CBO com a
  análise de dependências da seção 6.3.
- **Test Strength depende do conjunto de mutadores.** Foram usados os mutadores `DEFAULTS` nos
  dois lados; outros conjuntos poderiam mudar os números absolutos.

### Validade interna

- **Esforço de teste ainda não idêntico.** Depois da equalização, as suítes têm tamanho e
  composição parecidos, mas restam duas diferenças: 29 testes de integração no monólito
  contra 6, e testes unitários transversais (CORS, corpo de erro, seeder) que o monólito tem
  e os microsserviços não. A segunda diferença é quantificada na seção 4.4.
- **A equalização foi feita depois das primeiras medições**, por quem já conhecia as lacunas
  da suíte dos microsserviços. Os testes novos do monólito podem ter sido direcionados aos
  mutantes conhecidos.
- **Regras ArchUnit relaxadas no monólito equalizado.** A comparação de Q3 usa o conjunto
  alinhado sem exceções (5.2), não as regras de cada lado.
- **Critério de sensibilidade formalizado depois da análise exploratória** (4.6). Mitigado por
  ser uma regra mecânica, aplicada igualmente aos dois lados.
- **Mesmos autores nos dois lados.**

### Validade externa

- **n = 1 por arquitetura.** Uma implementação de cada, num domínio pequeno (29 tipos no
  monólito e 53 nos microsserviços), com quatro serviços. Sistemas maiores, com mais serviços
  ou com bibliotecas compartilhadas para o código transversal, podem se comportar de outra
  forma. O achado 3, por exemplo, depende de o código transversal ser copiado, e não extraído
  para uma biblioteca comum.
- **Desenvolvimento assistido por IA nos dois lados**, o que pode reduzir a variação de estilo
  que equipes diferentes produziriam.

### Validade de conclusão

- **Sem inferência estatística.** As diferenças são exatas para estes sistemas, mas não há
  como estimar quanto variariam em outras implementações do mesmo domínio.

---

## 10. Rastreabilidade

| Análise | Arquivos |
|---|---|
| Microsserviços (`main`) | `jacoco/*/jacoco.csv`, `pitest/*/mutations.xml`, `surefire/*/TEST-*.xml`, `ck/*/class.csv`, `metricas.json` |
| Monólito equalizado | `monolito-equalizado/jacoco.csv`, `mutations.xml`, `surefire/` (reprodução do commit `518b8c9` em JDK 21.0.12.1, 3 rodadas de testes e 2 de PITest idênticas) |
| Monólito baseline | `TCC_monolith/docs/reports/jacoco/jacoco.csv`, `pitest/mutations.xml`, `ck/class.csv` (commit `1c05487`) |
| Por domínio, baseline | `por-dominio.json` |
| Microsserviços sem `@WebMvcTest` | `sensibilidade/sem-webmvctest/<servico>/` |
| Microsserviços `resultados-e` | `RESULTADOS_METRICAS.md` (verificação do Eric, reproduzida em outra máquina) |
| Regras alinhadas | `arquitetura-alinhada/AlignedRulesTest-*.java` e `resultado-*.txt` |
