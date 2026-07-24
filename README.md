# E-commerce TCC — Microsserviços

Implementação da versão **Microsserviços** do experimento controlado de Engenharia de
Software Experimental (TCC PUCPR), comparada a um monólito de referência equivalente.

## Serviços

| Serviço | Porta | Banco (H2) | Responsabilidade |
|---|---|---|---|
| `user-service` | 8085 | `userdb` | Cadastro, autenticação e emissão de JWT |
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
cd user-service    && mvn spring-boot:run   # :8085
cd product-service && mvn spring-boot:run   # :8082
cd order-service   && mvn spring-boot:run   # :8083
cd report-service  && mvn spring-boot:run   # :8084
```

> A porta padrão do user-service é 8085 (não 8081) porque 8081 é um valor comum
> demais e frequentemente já ocupado por outros processos de desenvolvimento
> na máquina. Ajuste `server.port` em `user-service/src/main/resources/application.properties`
> (e a constante `SERVICES.user` em `frontend/index.html`) se precisar mudar.

### Usuários pré-cadastrados (user-service)

| E-mail | Senha | Role |
|---|---|---|
| `admin@tcc.com` | `admin123` | ADMIN |
| `joao@email.com` | `cliente123` | CUSTOMER |

## Frontend

`frontend/index.html` é uma página estática (HTML/CSS/JS puro, sem build) que cobre as
principais funcionalidades: login/cadastro, listagem e cadastro de produtos, montagem de
carrinho e checkout, listagem de "meus pedidos" com cancelamento, painel ADMIN com todos
os pedidos (atualização de status) e geração do relatório de vendas por período. Ela fala
diretamente com os 4 serviços via `fetch`, guarda o token JWT no `localStorage` e mostra
um log de todas as requisições feitas.

Basta abrir o arquivo no navegador com os 4 serviços rodando (ou servir com qualquer
servidor estático, ex. `npx serve frontend`, para evitar peculiaridades de `file://`).

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
