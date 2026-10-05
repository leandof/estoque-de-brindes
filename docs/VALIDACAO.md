# Validação — Relatório de Movimentações

- Java 21: compilação e 16 testes concluídos, zero falhas/erros (15 novos testes de integração + 1 teste de contexto).
- Interface: teste Playwright em Chromium headless aprovado (abas, filtros, vazio, limpar, datas inválidas, legado, texto seguro e largura de 390 px). API simulada nesse teste.
- git diff --check: sem erros de whitespace.
- O teste de período verifica inclusive 23:59:59 do último dia e exclusão do dia seguinte.
- A suíte verifica identidade de João/Maria, saldos, data automática, tentativa de falsificação, preservação de histórico e duas saídas simultâneas.
- Os testes de integração usam H2, não MySQL. Migração e bloqueios devem ser homologados numa cópia do MySQL antes da implantação.
- Nenhuma conexão ao banco de produção, alteração remota no GitHub ou implantação foi realizada.

Neste ambiente, o Mockito exigiu carregar previamente o agente Byte Buddy:
`./mvnw -DargLine=-javaagent:/caminho/byte-buddy-agent-1.14.18.jar test`.
Foi necessário apenas para a restrição de autoanexação da JVM do ambiente de execução.
Em um JDK local normal, tente primeiro `./mvnw test`. A dependência do agente já vem pela infraestrutura de testes.


```text
-------------------------------------------------------------------------------
Test set: api_brindes.ApiBrindesApplicationTests
-------------------------------------------------------------------------------
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.522 s -- in api_brindes.ApiBrindesApplicationTests
```
```text
-------------------------------------------------------------------------------
Test set: api_brindes.RelatorioMovimentacoesTests
-------------------------------------------------------------------------------
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 21.35 s -- in api_brindes.RelatorioMovimentacoesTests
```