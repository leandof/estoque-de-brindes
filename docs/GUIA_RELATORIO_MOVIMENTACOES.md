# Relatório de movimentações — guia atualizado

O guia antigo continha cópias integrais de código que divergiam da aplicação. Os arquivos completos e atuais estão em `src/`.

- Instruções de instalação e atualização: `../LEIA-ME-PRIMEIRO.md`.
- Análise das regras e alterações: `ANALISE_E_CORRECOES.md`.
- Testes executados e pendentes: `VALIDACAO.md`.

O relatório preserva filtros por período, responsável, tipo e produto, com paginação de 20 registros na interface. O CSV exporta somente a página exibida.

A exclusão agora é lógica: itens excluídos continuam consultáveis no histórico, inclusive pelo filtro de produtos. Novas movimentações e alterações nesses itens são bloqueadas. Consulte a migração aditiva `sql/02_exclusao_logica_mysql.sql`.

O script `sql/01_relatorio_movimentacoes_mysql.sql` pertence à implantação anterior da auditoria e não deve ser repetido num banco que já contém essas colunas.
