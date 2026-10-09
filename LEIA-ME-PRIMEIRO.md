# Estoque de brindes Truckvan — projeto completo revisado

Base analisada: https://github.com/leandof/estoque-de-brindes
Commit de referência: `629c4fd` (09/10/2026, data desta revisão).

Este pacote contém o projeto Maven completo, arquivos finais e a logo enviada. Abra esta pasta no IntelliJ. Não são trechos soltos. A análise detalhada está em `docs/ANALISE_E_CORRECOES.md`.

## O que mudou

- Interface em vermelho Truckvan, branco e grafite; logo original sem distorção.
- Inventário com busca, filtros, quatro indicadores e formulários em janelas.
- Cadastro, entrada, saída, edição de valor e exclusão com confirmação.
- **Exclusão de itens COM histórico:** retira do inventário ativo e dos totais, preservando todas as movimentações. Itens excluídos aparecem nos filtros históricos.
- Autenticação com respostas 401/403 distintas e redirecionamento quando a sessão expira.
- Rejeição de campos extras, quantidades fracionadas e preços inválidos.
- CSV da página exibida do relatório, com proteção contra fórmulas.
- HTML, CSS e JavaScript separados para facilitar a manutenção.

## 1. Preparar o IntelliJ gratuito

1. Faça uma cópia do seu projeto atual e um backup do banco antes da atualização.
2. Extraia o ZIP. No IntelliJ, abra a pasta `estoque-de-brindes` ou o `pom.xml` como projeto Maven.
3. Configure o **JDK 21** em Project Structure > Project SDK. Use também o JDK 21 para o Maven Runner.
4. Aguarde a importação das dependências. Caso necessário, use Reload All Maven Projects.
5. Configure as variáveis abaixo em Run > Edit Configurations > Environment variables da classe principal. Os rótulos dos menus podem variar conforme a edição do IntelliJ.
6. Execute o método `main` em `src/main/java/api_brindes/ApiBrindesApplication.java`. Não é necessário plugin pago de Spring para executar esse método Java.
7. Abra http://localhost:8080/login.html. Não abra o HTML direto pelo explorador de arquivos.

### Variáveis de execução

| Variável | Valor a preencher |
|---|---|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/estoque_brindes` ou a URL JDBC do seu banco de desenvolvimento |
| `MYSQL_USER` | Seu usuário MySQL |
| `MYSQL_PASSWORD` | Sua senha MySQL real |
| `JWT_SECRET` | Segredo aleatório privado; use pelo menos 32 caracteres |
| `PORT` | `8080` no computador, opcional |

Não copie os textos descritivos da tabela como senhas. Não publique essas variáveis no GitHub. Arquivos `.env` não são carregados automaticamente por este projeto.

O banco deve existir. Para um banco local novo, execute no MySQL:

```sql
CREATE DATABASE estoque_brindes CHARACTER SET utf8mb4;
```

O Hibernate criará as tabelas com `ddl-auto=update`. Para criar o primeiro usuário em um banco LOCAL vazio, acrescente temporariamente às variáveis:

```text
SPRING_PROFILES_ACTIVE=local
BOOTSTRAP_ENABLED=true
BOOTSTRAP_LOGIN=seu_usuario
BOOTSTRAP_PASSWORD=uma_senha_privada_escolhida_por_voce
```

A senha deve ter pelo menos 8 caracteres e até 72 bytes UTF-8. A configuração só cria um usuário quando a tabela está vazia e não modifica usuários existentes. Depois da primeira execução, remova `BOOTSTRAP_ENABLED`, `BOOTSTRAP_LOGIN` e `BOOTSTRAP_PASSWORD`. Não use esse perfil no Render. Os usuários existentes continuam com suas senhas atuais.

### Executar os testes pelo terminal do IntelliJ (PowerShell)

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Os testes Java usam H2 em memória, sem alterar o MySQL. Node não é necessário para executar a aplicação. Opcionalmente, com Node instalado:

```powershell
node tests/logica-interface.cjs
```

## 2. Aplicar no projeto que você já usa

A opção mais simples é abrir a cópia completa em outra pasta e conferir antes de substituir. Para atualizar seu projeto existente:

1. Copie os arquivos da pasta `src` deste pacote mantendo os caminhos.
2. Apague estes arquivos antigos: `src/main/resources/ApiBrindesApplication.java`, `src/main/resources/ItemController.java`, `src/main/resources/CorsConfig.java` e `src/main/resources/static/logo-truckvan.png`.
3. Mantenha o `pom.xml`, Dockerfile e Maven Wrapper do pacote. Não há atualização de versão do Spring neste trabalho.
4. Copie `tests` e `docs` para manter os testes e instruções correspondentes à nova versão.
5. Configure as variáveis obrigatórias e execute os testes antes de publicar.

A logo passa a ficar em `src/main/resources/static/assets/truckvan-logo.jpeg`.

### Onde editar cada parte

| Arquivo/pasta | Responsabilidade |
|---|---|
| `src/main/resources/static/index.html` | Estrutura do inventário, relatórios e formulários |
| `src/main/resources/static/css/index.css` | Cores, espaçamento e adaptação de tela |
| `src/main/resources/static/js/index.js` | Chamadas da API, filtros e ações da interface |
| `src/main/resources/static/login.html` | Estrutura do login |
| `src/main/resources/static/css/login.css` | Visual do login |
| `src/main/resources/static/js/login.js` | Autenticação |
| `src/main/java/api_brindes/model/Item.java` | Campo `ativo` da exclusão lógica |
| `src/main/java/api_brindes/service/ItemService.java` | Cadastro, edição de valor e exclusão |
| `src/main/java/api_brindes/service/MovimentacaoService.java` | Entradas, saídas e histórico |
| `src/main/java/api_brindes/repository/ItemRepository.java` | Consultas de itens ativos e bloqueio concorrente |
| `src/main/java/api_brindes/config` | Segurança, erros e criação opcional de usuário local |
| `src/main/resources/application.properties` | Banco, servidor e validação JSON |

## 3. Exclusão com histórico: regra implementada

Ao clicar em Remover, a confirmação mostra o nome e saldo do item. Confirmar define `ativo=false`:

- O item desaparece do inventário, dos indicadores e do seletor de novas movimentações.
- O histórico mantém autor, data, quantidade, nome/código registrado e saldos.
- O filtro de produtos no relatório inclui o item marcado como “excluído”.
- Entradas, saídas e edição do item excluído retornam 404.
- O saldo antigo não é zerado e não se cria uma saída artificial. Exclusão não significa entrega de brindes.
- O código continua reservado pelo índice único. Use um novo código para cadastrar um produto novo.
- A interface não inclui restauração nem exclusão definitiva de histórico.

A nova coluna `itens.ativo` usa `TRUE` como padrão para preservar os itens existentes. Confira `docs/sql/02_exclusao_logica_mysql.sql`. Escolha uma das rotas: deixar `ddl-auto=update` criar a coluna ou aplicar a migração manual antes de subir a aplicação; não execute o ALTER duas vezes.

## 4. Atualizar no Render

O serviço online NÃO foi alterado neste trabalho.

1. Teste primeiro localmente e, de preferência, em um banco separado com dados de teste.
2. Faça backup do banco de produção.
3. Confira as variáveis `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD` e `JWT_SECRET` do seu serviço. A senha MySQL e o segredo JWT agora são obrigatórios; a aplicação falhará ao iniciar se faltarem.
4. Se usava os valores padrão publicados no código antigo, substitua-os por credenciais privadas. Trocar o segredo JWT exige novo login dos usuários.
5. Confira a migração aditiva de `itens.ativo`, especialmente se já utiliza `ddl-auto=validate`.
6. Faça commit/push dos arquivos revisados na branch conectada ao serviço e acompanhe o deploy. O Dockerfile existente foi preservado, com Java 21 e porta definida por `PORT`.
7. Valide login, logo, cadastro, entrada, saída, edição, exclusão com histórico e consulta do produto excluído. Use um item de teste claramente identificado.
8. Após a atualização, atualize a página com Ctrl+F5 para carregar os novos arquivos estáticos.

O SQL `01_relatorio_movimentacoes_mysql.sql` é legado e NÃO deve ser executado novamente se as colunas de auditoria já existem. Para esta revisão a mudança de schema é a coluna `ativo`.

## 5. O que foi e não foi validado aqui

- Passaram: análise sintática dos JavaScripts, testes de lógica Node e conferência dos caminhos HTML/arquivos estáticos.
- Não concluído: compilação e testes Java; este ambiente tem JDK 17 e não conseguiu obter as dependências Maven. O projeto continua configurado para Java 21.
- Não executados: testes Playwright e conferência visual em navegador, pois não foi possível instalar o Chromium neste ambiente.
- Não acessados: painel do Render, credenciais, banco real e sessão autenticada de produção.

Não trate este ZIP como uma versão já homologada em produção. O teste no seu IntelliJ com JDK 21 é o próximo passo necessário. A análise de código e os cenários de regressão adicionados estão documentados em `docs/ANALISE_E_CORRECOES.md` e `docs/VALIDACAO.md`.
