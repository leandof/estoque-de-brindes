# Análise técnica do projeto e correções

Repositório: https://github.com/leandof/estoque-de-brindes
Base: commit `629c4fd`, inspecionado em 09/10/2026. A análise considera os arquivos obtidos do GitHub; não confirma que o Render esteja executando esse mesmo commit.

## Visão geral

O projeto é uma aplicação Java 21 com Spring Boot 3.3.2, Maven, Spring Web, Spring Data JPA, MySQL, Spring Security, JWT e frontend HTML/CSS/JavaScript servido pela própria aplicação. O Dockerfile usa Java 21 e executa o JAR. É uma estrutura adequada para um projeto de portfólio backend com interface funcional.

O fluxo principal é: navegador → controller → service → repository → MySQL. No login, Spring valida a senha BCrypt e gera um token com validade de duas horas. Nas demais requisições, o filtro JWT identifica o usuário; o service usa esse usuário como responsável pela movimentação.

## Pontos positivos encontrados

- Movimentações com `@Transactional`: saldo e histórico são gravados juntos.
- Bloqueio pessimista do item, compartilhado por movimentação e alteração de valor, reduzindo o risco de duas saídas consumirem o mesmo saldo.
- Rejeição de saída maior que o estoque disponível e uso de `Math.addExact` para impedir overflow nas entradas.
- DTOs para entradas de dados e projeções de histórico que não expõem o hash da senha.
- Fotografia do nome e código do item no momento da movimentação.
- Autor obtido da autenticação, não escolhido livremente pelo cliente.
- Relatório paginado, com filtros combináveis e ordenação por data e ID.
- Preservação de dados legados sem inventar responsáveis e saldos.
- Uso de `textContent` para inserir texto vindo da API, evitando interpretar nomes como HTML.
- Testes de integração já existentes cobrindo regras de negócio e concorrência. A existência desses testes é positiva, mas não comprova que estejam passando no commit atual.

## Achados e tratamento

| Achado no código original | Consequência | Correção nesta entrega |
|---|---|---|
| Logo referenciada em `/logo-truckvan.png`, fora das rotas estáticas públicas | A requisição de imagem do navegador não envia o token da API e pode ser recusada | Logo enviada em `/assets/truckvan-logo.jpeg`, liberada pela configuração existente |
| Logo horizontal exibida em caixa quadrada de 54/68 px | Marca pequena e difícil de ler | Proporção original preservada, com área horizontal maior |
| Senha MySQL e chave JWT com fallback literal versionado | Configuração pode usar valores públicos sem perceber | Valores padrão removidos; variáveis obrigatórias |
| Testes exigem rejeitar campos extras, mas falta configuração Jackson | Contrato efetivo pode divergir do documentado | `fail-on-unknown-properties=true` |
| Conversão padrão de número decimal para inteiro não desativada | Quantidades fracionadas podem sofrer coerção | `accept-float-as-int=false` |
| Sem tratamento explícito de 401/403 na cadeia de segurança | Sessão ausente e permissão negada pouco claras | Respostas JSON e códigos explícitos |
| Interface apaga token tanto em 401 quanto em 403 | Uma negativa de permissão pode desconectar usuário válido | Somente 401 apaga token e redireciona |
| Login exibe mesma mensagem para qualquer erro HTTP | Falha de servidor pode parecer senha incorreta | Mensagens distintas para credencial, servidor e conexão |
| Soma de estoque em `int` | Total pode ultrapassar limite de 32 bits | Soma com `long` no relatório da API |
| Soma monetária da API em `double` | Acúmulo de erro binário | Agregação com `BigDecimal` e duas casas no retorno |
| Preço sem limite de casas e teto explícito | Dados fora do padrão esperado pela interface | Até duas casas e valor entre zero e 999999999,99 |
| Exclusão recusada quando existe histórico | Impedia a flexibilidade solicitada | Exclusão lógica com campo `ativo` |
| Histórico dependente do item | Apagar fisicamente poderia romper relacionamentos ou perder rastreabilidade | Item permanece no banco; somente inventário ativo deixa de listá-lo |
| Indicador “Sistema operacional” fixo | Aparência de conexão normal mesmo em erro | Indicador reflete carga bem-sucedida ou falha |
| Contadores e tabela buscados separadamente | Podem representar instantes diferentes | Frontend calcula indicadores a partir da mesma lista recebida |
| Inventário sem busca visual | Trabalho maior para encontrar produtos | Busca por nome/código e filtros por saldo |
| Alertas/prompt para cadastro e edição | Interrupção do fluxo e pouco contexto | Formulários em `dialog`, feedback na página e bloqueio de envio repetido |
| Botão de exportação desativado | Funcionalidade anunciada sem uso | Exportação CSV da página atual, com escopo explícito |
| HTML com CSS e JS embutidos | Manutenção difícil para iniciante | Separação em arquivos identificados no guia |
| Classes `.java` antigas dentro de `resources` | Cópias divergentes incluídas como recursos, embora não sejam a fonte Java compilada | Remoção das três cópias obsoletas |

Os achados acima decorrem da inspeção do código. A reprodução Java em execução ficou bloqueada pela indisponibilidade das dependências no ambiente de revisão.

## Exclusão: comportamento e integridade

`DELETE /itens/{id}` agora marca `ativo=false` em uma transação. A consulta com bloqueio só retorna itens ativos. Assim, exclusão, edição de valor e movimentação usam o mesmo mecanismo de bloqueio por item.

Se uma movimentação obtiver o bloqueio primeiro, termina antes da exclusão; se a exclusão concluir primeiro, uma nova movimentação não encontra item ativo. O histórico continua ligado ao item original. A validação efetiva desse comportamento com MySQL continua necessária: testes H2 não substituem a verificação do banco usado em produção.

`GET /itens` e `GET /itens/relatorio` consideram somente ativos. `GET /itens/historico` inclui ativos e excluídos para o filtro histórico. `GET /movimentacoes/relatorio` preserva o comportamento anterior.

Excluir com saldo retira esse saldo dos totais ativos. Não é baixa por consumo, não altera movimentos passados e não gera uma movimentação com significado inventado. O usuário vê essa informação antes da confirmação. O código permanece único/reservado; restauração não faz parte desta entrega.

## Limites e próximas melhorias de arquitetura

1. **Dinheiro na entidade:** `Item.valor` ainda é `Double` para evitar alterar silenciosamente o tipo da coluna de produção. Foi corrigida a agregação e foram limitados novos valores. Uma evolução completa exige migração planejada para `DECIMAL`/`BigDecimal`, revisando valores legados antes de converter. A interface usa aritmética JavaScript, não deve ser tratada como livro contábil.
2. **Permissões:** todos os usuários recebem `ROLE_USER`; os endpoints protegidos exigem autenticação, mas não distinguem administrador e operador. Portanto usuários autenticados continuam podendo cadastrar outros usuários e excluir itens. Não foi inventada uma nova política de permissões sem saber quem deve ter cada acesso.
3. **Usuários concorrentes:** a tabela original de usuários não define unicidade de login no modelo. O cadastro consulta antes de inserir, mas dois cadastros simultâneos podem disputar o mesmo login. Uma correção robusta requer verificar duplicidades reais e migrar a restrição única, evitando quebrar o banco existente. Não foi aplicado DDL destrutivo nem deduplicação automática.
4. **Migrações:** `ddl-auto=update` foi mantido por compatibilidade. Para evolução de produção, migrações versionadas e `validate` são preferíveis. Os scripts SQL entregues são manuais e condicionados à existência das colunas.
5. **Volume de dados:** inventário e endpoints antigos de listagem total carregam tudo. O relatório paginado deve ser o caminho principal para histórico. Paginação de inventário e remoção/depreciação dos endpoints sem limite são melhorias futuras.
6. **Autenticação:** armazenamento do JWT em `localStorage` foi preservado. Cookies HttpOnly, renovação de sessão, revogação e limitação de tentativas exigem uma revisão conjunta do frontend e da segurança.
7. **Exportação:** o CSV contém até 20 registros, exatamente a página carregada. Exportação completa, PDF e processamento assíncrono não foram implementados.
8. **Versões de dependências:** versões Maven foram preservadas. Não foi possível verificar o catálogo atual nem executar uma auditoria de vulnerabilidades; este relatório não certifica a versão do Spring como atualizada.
9. **Homologação:** não foi realizada uma operação no banco real, nem foram usados dados de usuários ou credenciais de produção. Nenhum deploy foi feito.

## Avaliação como portfólio

O projeto demonstra regras de negócio mais relevantes do que um CRUD simples: transações, histórico, autenticação, consultas paginadas e concorrência. Para apresentá-lo profissionalmente, explique essas decisões no README e mostre um fluxo completo de cadastro → entrada → saída → relatório → exclusão lógica.

Os próximos avanços com maior valor são: homologar a suíte com Java 21/MySQL, migrar dinheiro para DECIMAL, separar permissões, versionar o schema e acrescentar testes de integração em CI. A interface é uma vitrine; as regras consistentes são o que sustenta o projeto.
