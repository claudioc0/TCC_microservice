# E-commerce TCC — Microsserviços

Implementação da versão **Microsserviços** do experimento controlado de Engenharia de
Software Experimental (TCC PUCPR), comparada a um monólito de referência equivalente.

## Serviços

| Serviço | Porta | Banco (H2) | Responsabilidade |
|---|---|---|---|
| `user-service` | 8081 | `userdb` | Cadastro, autenticação e emissão de JWT |
| `product-service` | 8082 | `productdb` | Catálogo de produtos e controle de estoque |
| `order-service` | 8083 | `orderdb` | Pedidos, itens e máquina de status |
| `report-service` | 8084 | — (sem banco próprio) | Relatório de vendas por período |

Cada serviço é um módulo Maven **independente**, com seu próprio `pom.xml`, processo,
banco H2 em memória e configuração de JaCoCo/PITest/ArchUnit. Eles não compartilham
classpath — a fronteira entre domínios é garantida fisicamente pela separação em
deployables distintos, e não apenas por convenção/ArchUnit como no monólito de
referência (esse é justamente o ponto de comparação do experimento).

## Comunicação entre serviços

Toda comunicação entre serviços é feita via **HTTP** (nunca acesso direto a banco ou
classes de outro serviço):

- `order-service` → `product-service`: consulta produto e ajusta estoque
  (`ProductClient`, usando `RestClient`).
- `report-service` → `order-service`: consulta pedidos por período para agregação
  (`OrderClient`, usando `RestClient`).

### Autenticação distribuída (JWT stateless)

O `user-service` é o único que emite tokens JWT (login/registro). Os demais serviços
**nunca** consultam um banco de usuários — cada um valida o token localmente (mesma
chave HMAC compartilhada via `app.jwt.secret`) e extrai `userId`/`role` diretamente das
claims. Quando `order-service` chama `product-service`, ou `report-service` chama
`order-service`, o token original do usuário é repassado (*pass-through*) no header
`Authorization`, preservando a identidade da requisição ponta a ponta.

## Máquina de status do pedido

```
PENDENTE ──► CONFIRMADO ──► ENVIADO ──► ENTREGUE
    │               │
    └───────────────┴──► CANCELADO (terminal)
```

Implementada como enum com método abstrato (`OrderStatus.canTransitionTo`) dentro da
entidade `Order` do `order-service` — transições fora do fluxo lançam
`InvalidStatusTransitionException` (HTTP 422).

## Como rodar localmente

Cada serviço roda em um terminal separado, nesta ordem (user-service e product-service
não têm dependências entre si; order-service depende do product-service estar no ar;
report-service depende do order-service):

```bash
cd user-service    && mvn spring-boot:run   # :8081
cd product-service && mvn spring-boot:run   # :8082
cd order-service   && mvn spring-boot:run   # :8083
cd report-service  && mvn spring-boot:run   # :8084
```

### Usuários pré-cadastrados (user-service)

| E-mail | Senha | Role |
|---|---|---|
| `admin@tcc.com` | `admin123` | ADMIN |
| `joao@email.com` | `cliente123` | CUSTOMER |

## Testes

Cada serviço tem sua própria suíte (JUnit 5 + Mockito + ArchUnit; `order-service` e
`report-service` também testam seus clients HTTP com `MockRestServiceServer`):

```bash
cd user-service    && mvn test   # 24 testes
cd product-service && mvn test   # 19 testes
cd order-service   && mvn test   # 36 testes
cd report-service  && mvn test   # 18 testes
```

JaCoCo (branch coverage) e PITest (mutation score) estão configurados no `pom.xml` de
cada serviço individualmente:

```bash
cd <servico> && mvn test           # gera target/site/jacoco/index.html
cd <servico> && mvn org.pitest:pitest-maven:mutationCoverage
```
