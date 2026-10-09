# Validação desta revisão — 09/10/2026

## Executado

- `node --check src/main/resources/static/js/index.js`: passou.
- `node --check src/main/resources/static/js/login.js`: passou.
- `node --check tests/ui-relatorio.cjs`: passou.
- `node tests/logica-interface.cjs`: passou. Usa DOM mínimo e HTTP simulado. Cobre escape e neutralização de fórmulas no CSV, ausência de sessão, respostas 401/403, DELETE com retorno 204, confirmação da exclusão com saldo e invalidação da exportação em período incorreto.
- Conferência de arquivos estáticos e identificadores referenciados pelo JS: passou.
- `git diff --check`: conferido após a edição.

## Bloqueado / não executado

A tentativa inicial `bash mvnw -q -Djava.version=17 test` não chegou à compilação: o Maven não resolveu `repo.maven.apache.org` para baixar o parent do Spring Boot 3.3.2. A tentativa foi diagnóstica, pois o ambiente disponível só possui Java 17. O projeto e Dockerfile continuam usando Java 21.

Não há resultado aprovado para os testes Java originais nem para os novos testes desta revisão. Execute `mvnw.cmd test` com JDK 21 no IntelliJ.

Playwright está disponível, mas o download do Chromium falhou. O teste de navegador foi atualizado para carregar os novos arquivos estáticos e cobrir busca, abertura de formulário e download CSV; ele não foi executado aqui. Não foi feita inspeção visual renderizada em desktop/mobile.

## Cenários Java incluídos

A suíte existente continua cobrindo entradas, saídas, autor, datas, filtros, paginação, dados legados e concorrência. A expectativa antiga de conflito ao excluir foi substituída pelo comportamento solicitado de exclusão lógica. Foram adicionados cenários de logo pública, 401, soma acima de Integer.MAX_VALUE, preços inválidos, bloqueio de edição de item excluído e CORS preflight.

## Roteiro manual de homologação

1. Iniciar com JDK 21 e banco de desenvolvimento. Conferir logo no login e na tela principal.
2. Testar senha incorreta, login correto e logout.
3. Cadastrar um item com saldo inicial positivo; conferir entrada, responsável e saldo no relatório.
4. Registrar entrada e saída; tentar saída superior ao disponível e quantidade fracionada.
5. Editar o preço com duas casas; tentar preço negativo ou com três casas.
6. Buscar item por código e nome; testar filtros de saldo baixo e zerado.
7. Excluir item com movimentações e saldo. Conferir que deixa de aparecer no inventário e nos totais.
8. Consultar seu histórico selecionando o produto marcado “excluído”. Conferir saldos anteriores preservados.
9. Tentar uma movimentação via API para o ID excluído; esperar 404 sem alteração do histórico.
10. Testar filtros combinados, período inválido, paginação e CSV da página exibida.
11. Testar token vencido; esperar redirecionamento ao login.
12. Conferir em desktop e celular, incluindo navegação por teclado e fechamento de diálogos com Escape.

## Teste opcional em navegador

Com Node instalado, na raiz do projeto:

```text
npm install --no-save playwright
npx playwright install chromium
node tests/ui-relatorio.cjs
```

Esse teste simula a API; não usa nem modifica o banco real. Screenshots vão para a pasta temporária do sistema, inclusive no Windows (variáveis `ESTOQUE_SCREENSHOT` e `RELATORIO_SCREENSHOT` para desktop).
