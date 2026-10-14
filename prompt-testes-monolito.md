# Prompt — Suíte de testes do monólito nos mesmos moldes da versão microsserviços

> **Como usar:** abrir o Claude Code (ou agente equivalente) **na raiz deste repositório
> (`TCC_monolith`)** e colar todo o conteúdo da seção "Prompt" abaixo. O restante do
> arquivo é contexto de apoio (o que já existe hoje no monólito e o que a versão
> microsserviços produziu), e pode ser colado junto.

---

## Prompt

Você vai trabalhar no repositório **`monolith-ecommerce`** (pacote raiz
`com.pucpr.tcc.monolith`), versão monolítica do experimento controlado de Engenharia de
Software Experimental — TCC PUCPR. Este monólito é o **sistema de referência** comparado
a uma implementação equivalente em microsserviços (`TCC_microservice`), e a comparação só
é válida se as duas suítes forem construídas com **o mesmo critério, as mesmas
ferramentas e as mesmas versões**.

Sua tarefa: **elevar a suíte de testes automatizados do monólito ao mesmo padrão já
aplicado na versão microsserviços** e **gerar os quatro relatórios de métricas
correspondentes**.

### 0. Antes de escrever qualquer teste — inventário

1. Confirme a estrutura real de pacotes (`users`, `products`, `orders`, `reports`,
   `security`, `exception`) e liste, por camada, todas as classes de produção.
2. Rode `mvn test` e registre o **estado "Antes"**: número de testes por classe e por
   camada (use o relatório do Surefire, não a contagem de anotações), todos passando ou
   não.
3. Rode `mvn test` (JaCoCo) e `mvn org.pitest:pitest-maven:mutationCoverage` e registre
   os valores atuais de **branch coverage** e **mutation score** por pacote.
4. Só então planeje os testes novos. **Reporte esse inventário antes de começar a
   implementar** — ele é o baseline "Antes" dos relatórios.

### 1. Regras invioláveis (integridade do experimento)

- **Não altere código de produção para facilitar teste.** Se uma mudança em produção for
  realmente necessária (ex.: expor um DTO em vez de entidade para corrigir uma violação
  arquitetural), pare, explique o motivo e a alternativa descartada, e só siga depois de
  registrar isso explicitamente. Toda alteração de produção precisa aparecer numa seção
  "alterações em código de produção" do relatório final.
- **Não mude versões de ferramenta** sem registrar. Hoje: JaCoCo `0.8.11`, ArchUnit
  `1.2.1` (`archunit-junit5`), PITest `1.15.8` + `pitest-junit5-plugin 1.2.1`, JUnit 5 +
  Mockito + AssertJ. A versão microsserviços usa PITest `1.15.3` — essa divergência já
  existe e **deve ser declarada no relatório de mutation score**, não silenciada.
- **Os 4 testes de integração existentes (`AuthIntegrationTest`, `OrderIntegrationTest`,
  `ProductIntegrationTest`, `ReportIntegrationTest`, todos `@SpringBootTest`) ficam como
  estão** — fazem parte do histórico medido do monólito. Mas **todo ganho novo de
  cobertura tem que vir de teste unitário ou de slice**, nunca de novos
  `@SpringBootTest`: subir contexto completo para inflar cobertura descaracteriza a
  suíte e a torna incomparável com a dos microsserviços (que não tem nenhum). Na
  composição por camada, conte os testes de integração **em linha separada** e registre
  essa assimetria metodológica.
- **Não relaxe regra de ArchUnit para fazer teste passar.** Violação encontrada é
  resultado, não obstáculo: corrija o acoplamento no código ou documente a violação.
- **Nenhum teste pode depender de ordem de execução, relógio do sistema, rede ou estado
  de banco persistido.** O resultado tem que se repetir idêntico entre execuções.

### 2. O molde da suíte — como os testes devem ser escritos

Espelhe, camada a camada, o que foi feito nos microsserviços:

| Camada | Técnica exigida |
|---|---|
| **Service** | Mockito puro (`@ExtendWith(MockitoExtension.class)`, `@Mock` nos repositórios/colaboradores, `@InjectMocks`). Sem Spring. Cobrir caminho feliz, cada caminho de exceção e as regras de negócio (estoque, transições de status, agregações de relatório por período). |
| **Controller** | Slice web: `@WebMvcTest(XController.class)` + `@Import({SecurityConfig.class, JwtAuthenticationFilter.class})` para exercitar a **cadeia de segurança real**. Service mockado com `@MockBean`. Verificar: status HTTP, corpo JSON (`jsonPath`), validação de payload, autorização por role (`403` para não-ADMIN) e — crítico — que o `userId` usado vem **do principal autenticado, nunca do corpo da requisição**. |
| **Entidade / domínio** | Teste direto, sem mock: invariantes, cálculo de totais e a máquina de status do pedido (todas as transições válidas **e** todas as inválidas, incluindo os estados terminais). |
| **Security (filtro + JWT)** | `JwtAuthenticationFilter` testado **sem contexto Spring**, com Mockito puro + `MockHttpServletRequest`. Cobrir o contrato inteiro: a cadeia sempre prossegue; só autentica com header presente, prefixo `Bearer ` e token válido; a role da claim vira authority com prefixo `ROLE_`. `JwtService`: geração, extração de claims, assinatura inválida, token malformado, expiração. |
| **Exception handler** | Chamar os métodos do `GlobalExceptionHandler` **diretamente** (não só via HTTP), inclusive `MethodArgumentNotValidException` mockada sobre um `BeanPropertyBindingResult` real — assim se fixa o contrato "exceção de domínio → status + corpo" nos casos que nenhum teste de controller alcança. |
| **Arquitetura** | ArchUnit, ver seção 3. |

Convenções obrigatórias (idênticas às da outra versão):

- Classe de teste = `<ClasseDeProducao>Test`, no **mesmo pacote** da classe testada, sob
  `src/test/java`.
- `@DisplayName` **em português**, descrevendo a regra de negócio verificada
  (ex.: `"updateStatus deve seguir o fluxo PENDENTE → CONFIRMADO"`), não o nome do método.
- AssertJ (`assertThat`) para asserções; `verify(...)` / `verifyNoInteractions(...)` para
  afirmar colaborações — status HTTP sozinho não é asserção suficiente numa borda.
- Helpers de teste (fábrica de token, montagem de `SecurityContext`) em
  `.../testsupport/`, reaproveitados, nunca copiados entre classes.
- Javadoc curto no topo de cada classe de teste dizendo **o que aquela camada isola e por
  quê** — os relatórios se apoiam nesse texto. As classes de teste que já existem seguem
  esse padrão; mantenha-o.

### 3. ArchUnit — manter e ampliar

O monólito já tem `CommonArchitectureRulesTest` (3 regras) e
`DomainBoundaryArchitectureTest` (4 regras), hoje com 0 violações. Sua tarefa:

1. **Não remover nem enfraquecer** nenhuma das 7 regras existentes.
2. Verificar a paridade com as regras da versão microsserviços e **acrescentar as que
   faltarem**, adaptadas ao monólito:
   - `namingConventionShouldBeRespected` — `@RestController` termina em `Controller`,
     `@Service` em `Service`, `@Repository` em `Repository`.
   - `repositoriesShouldOnlyBeAccessedByServices` — classes em `..repository..` só são
     acessadas de `..service..`.
   - `controllersShouldNotDependOnEntities` — nenhuma classe `@RestController` depende de
     classe `@Entity`. **Atenção:** a regra existente
     (`controller_endpoints_should_not_return_entities`) verifica só o **tipo de
     retorno**; esta é mais forte e é a que os microsserviços usam, então tem chance real
     de acusar violação — inclusive por causa do `@AuthenticationPrincipal User
     currentUser` nos controllers. Se acusar, **não apague a regra**: decida entre
     corrigir o código ou registrar a exceção com justificativa explícita (a mesma que já
     consta hoje para as regras 4–7).
3. Toda regra nova que **encontrar violação real** deve ser reportada: quantas
   ocorrências, em que classes e qual correção foi aplicada. Esse é o dado mais
   importante do experimento — foi assim que as 11 violações originais apareceram.
4. Rodar como parte normal de `mvn test` (JUnit 5), aparecendo no Surefire como qualquer
   outra classe de teste.

### 4. Configuração de métricas (`pom.xml`)

- **JaCoCo 0.8.11**: `prepare-agent` + `report` na fase `test`. A métrica reportada é
  **branch coverage** (não line coverage).
- **PITest**: o escopo (`targetClasses`/`targetTests` = `com.pucpr.tcc.monolith.*`,
  mutators padrão, XML + HTML, `timestampedReports=false`) já está pareado com o dos
  microsserviços — **mantenha**. A única assimetria é o bloco `excludedClasses`
  (`MonolithApplication`, `security.DatabaseSeeder`, `architecture.*`), que a versão
  microsserviços não tem: **não mexa nele sem avisar**, e **declare a exclusão no
  relatório de mutation score**, porque ela desloca o denominador da comparação.

### 5. Alvos

- Toda a suíte **passando**, 100% verde.
- **Branch coverage**: subir dos 90,1% atuais o mais perto possível de 100%, atacando
  primeiro os pacotes fracos já identificados: `users.controller`, `products.controller`,
  `users.service`, `products.service`.
- **Mutation score**: eliminar os **28 NO_COVERAGE** (são falta de teste, não asserção
  fraca) e depois atacar os SURVIVED. Meta: ≥ 89% (patamar da versão microsserviços).
- **ArchUnit**: 0 violações ao final — mas **toda violação encontrada no caminho tem que
  ser documentada**, não apagada.
- Ciclo de trabalho: rode o PITest, leia a lista de **SURVIVED**, escreva teste dirigido
  para matar cada um, rode de novo. Repita até restar só resíduo justificável.

### 6. O que fica deliberadamente de fora (e precisa ser justificado no relatório)

Não tente cobrir, e explique por quê:

- `SecurityConfig#securityFilterChain` e `ApplicationConfig#authenticationManager` —
  wiring de framework (`HttpSecurity` / `AuthenticationConfiguration` internos do Spring);
  o efeito é observado indiretamente nos testes de controller, mas cobri-los exigiria
  contexto completo. Note que `SecurityConfig#corsConfigurationSource` **já é testado**
  como método de fábrica comum (`SecurityConfigTest`) — mantenha essa abordagem e
  estenda-a onde for possível, em vez de descartar a classe inteira.
- `JwtService#isTokenExpired` (mutante `boolean` sempre `false`) — inalcançável no fluxo
  normal, porque o parser do JJWT lança `ExpiredJwtException` antes de a comparação ser
  avaliada.

Cada mutante SURVIVED remanescente deve ser listado **individualmente** com
classe/método, mutação e a razão de sobreviver. Nenhum pode ser ignorado em silêncio.

### 7. Determinismo

Rode a suíte completa **3 vezes** e o PITest **2 vezes**. Compare não só os totais, mas o
**conjunto de sobreviventes mutante a mutante**. **Acrescente** as rodadas ao
`docs/reports/execution-log.md` já existente (data, comando, resultado de cada rodada),
sem apagar o histórico anterior. Se houver qualquer divergência entre rodadas, trate como
bug de teste e corrija — flakiness invalida a medição.

### 8. Entregáveis

Gere em `docs/reports/` **quatro relatórios em Markdown**, com os mesmos títulos, seções
e nível de detalhe dos da versão microsserviços:

1. **`monolito-testes-unitarios.md`** — ferramentas; critério medido; tabela
   **Antes / Depois / Acréscimo** por pacote de domínio; **composição por camada**
   (service, controller, entity, security, exception, architecture **e integração em
   linha separada**); seção "Decisões de teste que valem registro" (com os trechos de
   código dos helpers); seção "O que ficou deliberadamente de fora"; metodologia com os
   comandos exatos.
2. **`monolito-cobertura-codigo.md`** — JaCoCo 0.8.11; branch coverage geral (ramos
   cobertos / total) e por pacote; nota de determinismo referenciando o
   `execution-log.md`; comparação explícita com o valor "Antes" (1.785/1.982 = 90,1%).
3. **`monolito-mutation-score.md`** — PITest; tabela KILLED / SURVIVED / NO_COVERAGE com
   percentuais; **test strength** (mortos ÷ alcançados) e o que ele isola; score por
   pacote; **cada SURVIVED documentado individualmente**; a declaração das duas
   assimetrias com os microsserviços (versão 1.15.8 vs 1.15.3 e o bloco
   `excludedClasses`); histórico baseline → rodadas de refinamento; nota de determinismo.
4. **`monolito-violacoes-arquiteturais.md`** — ArchUnit 1.2.1; tabela suíte × regras ×
   violações; **a lista completa das regras com "o que cada uma verifica"**; violações
   encontradas e correções aplicadas; e a seção interpretativa: **num monólito a fronteira
   entre domínios existe só por convenção e só o ArchUnit a torna mensurável, enquanto no
   microsserviço ela é imposta fisicamente pela ausência de classpath compartilhado — a
   densidade de violações no monólito é a medida direta do que a fronteira física
   substitui.**

Além dos relatórios:

- `docs/reports/execution-log.md` atualizado (seção 7).
- `docs/reports/README.md` e o `README.md` da raiz atualizados com a nova contagem de
  testes por camada, os novos números de cobertura/mutation e os comandos de `mvn test` /
  `mutationCoverage`.
- Um resumo final no chat: número de testes antes → depois, branch coverage antes →
  depois, mutation score antes → depois, violações arquiteturais encontradas/corrigidas e
  qualquer alteração feita em código de produção.

### 9. Ordem de trabalho sugerida

1. Inventário + baseline (seção 0) → **reporte antes de seguir**.
2. Regras de ArchUnit faltantes → rodar → tratar/documentar violações.
3. Testes de entidade/domínio (`User`, `Product`, `Order`, `OrderItem`) — hoje só
   `OrderStatus` tem teste próprio; são os mais baratos e matam muito mutante.
4. Reforço dos testes de service com Mockito (`UserService` e `ProductService` são os
   mais fracos hoje).
5. **Slices de controller** com `SecurityConfig` real — hoje **não existe nenhum**, e é
   daí que vem a maior parte dos 28 NO_COVERAGE.
6. Security (filtro + `JwtService`) e `GlobalExceptionHandler` — completar as lacunas.
7. Rodada de PITest → matar SURVIVED dirigidamente → repetir.
8. Rodadas de determinismo + geração dos relatórios.

Ao final de cada etapa, rode `mvn test` e reporte o placar parcial (testes, cobertura).
Não avance com a suíte vermelha.

---

## Contexto de apoio

### Estado atual do monólito (commit de referência `a5fe3a0`; snapshot em `docs/reports/`)

| Métrica | Valor atual |
|---|---|
| Branch coverage (JaCoCo 0.8.11) | 1.785 / 1.982 ramos — **90,1%** |
| Mutation score (PITest 1.15.8) | 140 / 171 — **81,9%** (28 NO_COVERAGE, 3 SURVIVED) |
| Test strength | 97,9% (140/143) |
| Violações arquiteturais (ArchUnit 1.2.1) | 7 regras, **0 violações** (11 violações reais já corrigidas) |

Pacotes mais fracos, por NO_COVERAGE: `products.service` (7), `users.service` (5),
`users.controller` (5), `products.controller` (4). Os mesmos pacotes aparecem como pontos
fracos no relatório de branch coverage — o próximo ganho vem de **mais testes**, não de
reforçar asserções existentes.

### Suíte existente hoje (16 classes em `src/test/java/com/pucpr/tcc/monolith/`)

| Camada | Classes |
|---|---|
| architecture | `CommonArchitectureRulesTest` (3 regras), `DomainBoundaryArchitectureTest` (4 regras) |
| integração (`@SpringBootTest`) | `AuthIntegrationTest`, `OrderIntegrationTest`, `ProductIntegrationTest`, `ReportIntegrationTest` |
| service | `OrderServiceTest`, `ProductServiceTest`, `ReportServiceTest`, `UserServiceTest` |
| security | `JwtAuthenticationFilterTest`, `JwtServiceTest`, `ApplicationConfigTest`, `SecurityConfigTest` |
| entity | `OrderStatusTransitionTest` |
| exception | `GlobalExceptionHandlerTest` |
| **controller (slice)** | **nenhuma — a lacuna principal** |

Lacunas evidentes: **zero slices de controller** (`AuthController`, `UserController`,
`ProductController`, `OrderController`, `ReportController`) e nenhum teste de entidade
para `User`, `Product`, `Order` e `OrderItem`.

### Resultado da versão microsserviços (o alvo de paridade)

| Métrica | Microsserviços |
|---|---|
| Testes | 97 → **215** (+118), todos passando |
| Branch coverage | 64 / 64 ramos — **100,0%** (linha: 96,5%) |
| Mutation score | 188 / 210 — **89,5%** (21 SURVIVED, 1 NO_COVERAGE) |
| Test strength | 90,0% |
| ArchUnit | 17 regras (6 distintas), **0 violações** |

Composição por camada nos microsserviços (215 testes): controller 57, security 45,
service 40, entity/domínio 27, exception 19, architecture 17, client HTTP 8, seeder 2 —
e **nenhum teste de integração com contexto completo**. A distribuição é a de uma
pirâmide saudável: o grosso em regras de negócio (service + entity) e na borda HTTP, com
o mínimo necessário de verificação estrutural.

### Por que a paridade importa

O experimento compara **monólito × microsserviços** nas mesmas quatro métricas. Qualquer
diferença de ferramenta, de versão, de escopo do PITest ou de critério de teste
(unitário vs. contexto completo) vira variável de confusão e contamina a conclusão. Os
números têm que diferir **por causa da arquitetura**, não por causa da metodologia de
medição.
