# Mapa de commits — migração de 2026-10-23

Em 2026-10-23 o repositório foi recriado no GitHub com o histórico reescrito, para retirar
as linhas de coautoria automática (`Co-Authored-By`) das mensagens de commit. **Só as
mensagens mudaram:** o conteúdo de cada commit (a árvore de arquivos), o autor e a data são
idênticos aos do original. O repositório antigo foi arquivado como `TCC_microservice-old`.

Mudar a mensagem muda o hash do commit e de todos os que vêm depois dele. Os 15 commits
anteriores a 2026-09-13 mantiveram o hash. Os 11 abaixo ganharam hash novo.

**Para conferir um registro antigo:** os resumos de execução recebidos antes da migração (por
exemplo, os `resumo.txt` do Vinicius e do Eric, com `Commit: 13e58b9`) citam o hash antigo.
O código é o mesmo do hash novo correspondente. Os documentos do repositório já usam os
hashes novos.

| Hash antigo | Hash novo | Data | Commit |
|---|---|---|---|
| `1ada60c` | `8f04507` | 2026-09-13 | Update README.md |
| `9b34da3` | `ea51f83` | 2026-09-13 | Merge remote-tracking branch 'origin/main' |
| `eb81cc1` | `080d9cd` | 2026-09-13 | Fecha lacunas de teste e ameacas a validade (JDK 21, CBO, testes de integracao) |
| `6231961` | `cb92d7b` | 2026-09-30 | Alinha escopo do PITest ao monolito, registra rodada de 30/09 e adiciona script de testes |
| `13e58b9` | `8e6ce9d` | 2026-10-09 | Registra verificacao independente do Vinicius (rodada identica a referencia) |
| `0a3f88b` | `995a942` | 2026-10-14 | Adiciona testes de GlobalExceptionHandler, UserController e DatabaseSeeder e relatorios |
| `e000fc2` | `4bf2083` | 2026-10-19 | Traz o prompt de testes do monolito para o main, corrigido |
| `22b7035` | `38708e1` | 2026-10-22 | Guarda os arquivos brutos das metricas byte a byte (sem conversao CRLF) |
| `46643d3` | `901d157` | 2026-10-22 | Consolida documento de metricas dos microsservicos com dados brutos rastreaveis |
| `b0a94d2` | `8f349e7` | 2026-10-22 | Adiciona analise comparativa monolito x microsservicos nas 4 metricas do GQM |
| `e0e9f21` | `d68ed75` | 2026-10-22 | Atualiza analise com a correcao do monolito, remove docx antigos e adiciona roteiro da Sprint 2 |

Branches migrados: `main` e `resultados-e`.
