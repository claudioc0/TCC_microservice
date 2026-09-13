# Fechamento das Pendências — Ameaças à Validade (lado microsserviços)

> Resposta direta a `pendencias-microservicos.md` (documento produzido do lado do monólito).
> As três pendências ali listadas dependiam de uma ação neste repositório
> (`TCC_microservice`) — este documento registra exatamente o que foi feito, como foi
> verificado, e o que muda nos números já publicados em `RESULTADOS_METRICAS.md`.

## 1. Declarar JDK 21 formalmente no `pom.xml`

**Status: Resolvido.**

- Instalado **Eclipse Temurin 21.0.12.1** (`21.0.12+1-LTS`) via `winget`, na mesma máquina
  onde a suíte já roda.
- `<java.version>` atualizado de `17` para `21` nos 4 `pom.xml`
  (`user-service`, `product-service`, `order-service`, `report-service`).
- Rodado `mvn -version` para confirmar que o Maven está de fato resolvendo para o JDK 21
  recém-instalado (não um JDK 17/11 remanescente do `PATH`/`JAVA_HOME` anterior).
- Rodado `mvn clean test` — com `clean`, não só `test` — nos 4 serviços, para garantir que o
  bytecode analisado é gerado pelo compilador do JDK 21, não "grudado" numa compilação
  anterior. Resultado: **171 testes, 0 falhas, 0 erros**, nos 4 serviços.
- Versão exata registrada em `RESULTADOS_METRICAS.md`, seção "Ambiente de execução" — não
  ficou como "JDK 21" genérico, exatamente pelo motivo que o documento original apontou: a
  *patch version* do compilador pode mudar a contagem de mutantes do PITest. E mudou de fato
  (ver item 3 abaixo) — o que valida a preocupação original e confirma que a captura da versão
  exata era necessária, não perfeccionismo.

**Onde conferir:** `user-service/pom.xml:21`, `product-service/pom.xml:21`,
`order-service/pom.xml:21`, `report-service/pom.xml:21`; `RESULTADOS_METRICAS.md`, seção 0.

## 2. Rodar a métrica de CBO (`ck`) em microsserviços

**Status: Resolvido.**

- Baixado `com.github.mauricioaniche:ck:0.7.0` — mesma versão do lado do monólito, garantindo
  comparabilidade metodológica — via um projeto Maven auxiliar descartável
  (`mvn dependency:copy-dependencies`).
- Rodado contra `src/main/java` de cada um dos 4 serviços (sem compilar antes, como o próprio
  `ck` permite), gerando `class.csv` por serviço.
- CBO médio calculado por serviço, por camada (agregado dos 4 serviços) e por classe individual.
- Criado `docs/reports/acoplamento-cbo.md`, espelhando o relatório equivalente do monólito,
  incluindo uma tabela completa por classe e uma seção conceitual explícita sobre o ponto que o
  documento original pediu para não deixar implícito: parte da queda de CBO
  (6,71 em microsserviços vs. 7,45 no monólito) é acoplamento Java-a-Java entre domínios que
  migrou para classes cliente HTTP (`ProductClient`, CBO 9; `OrderClient`, CBO 7) — isto é,
  virou acoplamento de contrato de rede, que o CBO não mede. O número comparativo, sozinho,
  contaria uma história de "menos acoplamento" que não é exatamente verdadeira.

**Resultado (resumo — detalhe completo em `docs/reports/acoplamento-cbo.md`):**

| Serviço | Classes | CBO médio | CBO máximo |
|---|---:|---:|---:|
| user-service | 13 | 6,77 | 14 (`UserService`) |
| product-service | 10 | 7,10 | 17 (`ProductController`) |
| order-service | 16 | 6,69 | 19 (`OrderController`) |
| report-service | 9 | 6,22 | 11 (`SecurityConfig`) |

**CBO médio agregado: 6,71** (48 classes) — comparável ao 7,45 (29 classes) do monólito.

**Onde conferir:** `docs/reports/acoplamento-cbo.md`; `RESULTADOS_METRICAS.md`, seção 4.

## 3. Assimetria de esforço de teste (testes de integração)

**Status: Resolvido — Opção A (equalizar esforço), decisão confirmada pelo responsável deste
lado do experimento.**

Entre as duas opções que o documento original colocava — equalizar esforço ou apenas reportar
as duas leituras lado a lado — foi escolhida a **Opção A**: escrever, do lado de
microsserviços, o equivalente aos 4 testes de integração `@SpringBootTest` que já existiam no
monólito.

- `AuthIntegrationTest` (user-service): cadastro, login, `BCryptPasswordEncoder` real, emissão
  e validação real de JWT, autorização por papel (`@PreAuthorize`) real, incluindo o usuário
  semeado por `DatabaseSeeder`.
- `ProductIntegrationTest` (product-service): CRUD completo, leitura pública vs. escrita
  ADMIN-only, ajuste de estoque, tudo contra o repositório H2 real.
- `OrderIntegrationTest` (order-service): ciclo de vida completo do pedido — criação,
  transição de status, consulta — via controller→service→repositório H2 reais; o único ponto
  substituído é a fronteira de rede com o product-service (que não está de fato em execução
  neste teste), via `MockRestServiceServer` amarrado ao mesmo `RestClient.Builder` que o
  `ProductClient` de produção usa.
- `ReportIntegrationTest` (report-service): agregação real de relatório de vendas, com a mesma
  técnica de `MockRestServiceServer` na fronteira com o order-service.

Em nenhum dos quatro nada dentro do processo do próprio serviço é mockado — só a rede para um
serviço vizinho que não está de fato rodando no teste. É a mesma lógica que já era usada em
`ProductClientTest`/`OrderClientTest`, agora aplicada à pilha inteira (controller até
repositório), não só ao client HTTP isolado.

**Efeito medido — a assimetria de fato fechou, não foi só documentada:**

| Serviço | Branch coverage (ramos) antes | Branch coverage (ramos) depois |
|---|---:|---:|
| user-service | 87% | 100% |
| product-service | 100% | 100% |
| order-service | 92% | 96,4% |
| report-service | 100% | 100% |

O ganho mais visível é em `order-service` e `user-service`, os dois serviços cujo teste de
integração cobre o maior número de ramos de tratamento de erro (transições de status inválidas,
credenciais erradas, e-mail duplicado) que os testes unitários e de `@WebMvcTest` já cobriam
parcialmente, mas não pela pilha real inteira.

**Onde conferir:** `user-service/src/test/java/com/tcc/user/AuthIntegrationTest.java`,
`product-service/src/test/java/com/tcc/product/ProductIntegrationTest.java`,
`order-service/src/test/java/com/tcc/order/OrderIntegrationTest.java`,
`report-service/src/test/java/com/tcc/report/ReportIntegrationTest.java`;
`RESULTADOS_METRICAS.md`, seção 1 e seção "Ameaças à validade".

## Efeito colateral honesto, não escondido: recontagem de mutantes em `order-service`

A recompilação sob JDK 21 (pendência 1) mudou o total de mutantes gerados pelo PITest em
`order-service`: de 57 para 54. O Mutation Score (91%) e o Test Strength (100%) não mudaram —
só o total de mutantes, porque o bytecode que o PITest analisa é diferente entre compiladores
de versões diferentes. Isso não é um erro de medição; é exatamente o motivo pelo qual a
pendência 1 exigia fixar a *patch version* exata antes de comparar os dois lados — e por isso
está registrado aqui em vez de silenciado.

## Estado final

Todas as três pendências listadas em `pendencias-microservicos.md` estão fechadas do lado
deste repositório. As quatro métricas do protocolo GQM (Branch Coverage, Mutation Score,
Densidade de Violações ArchUnit, CBO) têm agora par comparativo com o monólito, medido sob a
mesma versão de JDK e a mesma versão da ferramenta `ck`. Números completos, com todas as
tabelas e leituras qualitativas, em `RESULTADOS_METRICAS.md`.
