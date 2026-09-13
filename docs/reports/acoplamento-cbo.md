# Acoplamento (CBO) — Microsserviços

> Resposta à Question 4 do protocolo GQM: "o esforço de isolamento/mock
> aumenta ou diminui com a decomposição?" Dado gerado com a ferramenta
> `ck` **0.7.0** — mesma versão usada no lado do monólito, para os números
> serem comparáveis metodologicamente.

## Como foi coletado

```bash
# projeto Maven auxiliar com com.github.mauricioaniche:ck:0.7.0 como única dependência
mvn dependency:copy-dependencies -DoutputDirectory=libs

# rodado contra o .java de cada serviço, sem precisar compilar antes
java -cp "libs/*" com.github.mauricioaniche.ck.Runner \
    <servico>/src/main/java false 0 false <diretorio-de-saida>/
```

Rodado uma vez por serviço (`user-service`, `product-service`, `order-service`,
`report-service`), sobre `src/main/java` — só código de produção, testes
excluídos. `ck` gera `class.csv`, do qual a coluna `cbo` foi extraída por
classe e agregada abaixo.

## Resultado por serviço

| Serviço | Classes | CBO médio | CBO máximo | Classe(s) com CBO máximo |
|---|---:|---:|---:|---|
| user-service | 13 | 6.77 | 14 | UserService |
| product-service | 10 | 7.10 | 17 | ProductController |
| order-service | 16 | 6.69 | 19 | OrderController |
| report-service | 9 | 6.22 | 11 | SecurityConfig |

**CBO médio agregado (4 serviços, 48 classes): 6.71**

Para referência, o CBO médio medido do lado do monólito é **7.45** (29
classes) — ver `acoplamento-cbo.md` do repositório do monólito.

## Por camada (agregado dos 4 serviços)

| Camada | Classes | CBO médio |
|---|---:|---:|
| controller | 5 | 13.40 |
| service | 4 | 9.50 |
| entity | 4 | 9.25 |
| security | 12 | 8.83 |
| client | 2 | 8.00 |
| config | 1 | 4.00 |
| exception | 15 | 2.80 |
| other | 5 | 2.40 |

A camada `controller` concentra o maior CBO médio em ambos os lados do
experimento — controllers dependem de DTOs de request/response, do
service, e (em microsserviços) da infraestrutura de segurança
(`AuthenticatedPrincipal`), então é esperado que fiquem no topo. A camada
`exception` tem CBO baixo por construção: são classes folha, sem
dependência de saída além de `RuntimeException`.

## Leitura conceitual: acoplamento entre domínios não desaparece, muda de forma

O ponto que este número, sozinho, não mostra: no monólito, uma chamada de
`OrderService` para regras de `ProductService` é uma dependência Java
direta, contável pelo CBO. Na decomposição em microsserviços, essa mesma
dependência de domínio existe — `order-service` continua precisando
consultar e ajustar estoque de produto —, mas o CBO *Java* que a representa
migrou para uma classe cliente HTTP dedicada:

- `order-service.ProductClient` — CBO 9 — é o ponto de acoplamento
  Java-a-Java que resta *dentro* do processo `order-service` para a
  dependência de domínio com produto; o resto da dependência virou
  contrato de rede (payload JSON, endpoint HTTP), que o CBO não enxerga.
- `report-service.OrderClient` — CBO 7 — mesmo padrão para a dependência
  de report em relação a pedidos.

Ou seja: o CBO agregado de microsserviços (6.71) já é levemente menor que
o do monólito (7.45), mas **não é correto ler essa diferença como "a
decomposição reduziu o acoplamento entre os domínios de produto e
pedido"**. O que ela mostra é que o acoplamento *estático, em bytecode
Java, contável em tempo de compilação* diminuiu — porque uma fatia dele
foi trocada por acoplamento de contrato de rede (formato do payload,
endpoint, disponibilidade do serviço remoto), que é real, tem custo de
manutenção e de teste (é exatamente por isso que `ProductClientTest` e
`OrderClientTest` existem, usando `MockRestServiceServer`), mas está fora
do que CBO — ou qualquer métrica de acoplamento estático de código-fonte
— consegue medir. O experimento reporta os dois números lado a lado
precisamente para não deixar essa leitura errada implícita.

## Detalhe por classe

### user-service

| Classe | Camada | CBO |
|---|---|---:|
| UserService | service | 14 |
| SecurityConfig | security | 13 |
| UserController | controller | 11 |
| GlobalExceptionHandler | exception | 11 |
| AuthController | controller | 10 |
| JwtAuthenticationFilter | security | 10 |
| User | entity | 8 |
| JwtService | security | 5 |
| DatabaseSeeder | other | 4 |
| UserServiceApplication | other | 2 |
| UserNotFoundException | exception | 0 |
| DuplicateEmailException | exception | 0 |
| InvalidCredentialsException | exception | 0 |

### product-service

| Classe | Camada | CBO |
|---|---|---:|
| ProductController | controller | 17 |
| SecurityConfig | security | 11 |
| GlobalExceptionHandler | exception | 10 |
| JwtAuthenticationFilter | security | 10 |
| ProductService | service | 8 |
| Product | entity | 8 |
| JwtService | security | 5 |
| ProductServiceApplication | other | 2 |
| InsufficientStockException | exception | 0 |
| ProductNotFoundException | exception | 0 |

### order-service

| Classe | Camada | CBO |
|---|---|---:|
| OrderController | controller | 19 |
| Order | entity | 13 |
| GlobalExceptionHandler | exception | 13 |
| OrderService | service | 12 |
| SecurityConfig | security | 11 |
| JwtAuthenticationFilter | security | 10 |
| ProductClient | client | 9 |
| OrderItem | entity | 8 |
| JwtService | security | 5 |
| RestClientConfig | config | 4 |
| OrderServiceApplication | other | 2 |
| InvalidStatusTransitionException | exception | 1 |
| OrderNotFoundException | exception | 0 |
| ProductNotFoundException | exception | 0 |
| InsufficientStockException | exception | 0 |
| ProductServiceUnavailableException | exception | 0 |

### report-service

| Classe | Camada | CBO |
|---|---|---:|
| SecurityConfig | security | 11 |
| ReportController | controller | 10 |
| JwtAuthenticationFilter | security | 10 |
| GlobalExceptionHandler | exception | 7 |
| OrderClient | client | 7 |
| JwtService | security | 5 |
| ReportService | service | 4 |
| ReportServiceApplication | other | 2 |
| OrderServiceUnavailableException | exception | 0 |
