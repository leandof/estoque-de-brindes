-- Migração aditiva para MySQL. Faça backup antes de alterar a produção.
-- Confira antes: SHOW COLUMNS FROM itens LIKE 'ativo';
-- Execute o ALTER abaixo SOMENTE se a coluna ainda não existir.
-- Com ddl-auto=update, o Hibernate também pode criar esta coluna ao iniciar.
-- Não execute novamente se o Hibernate já a criou.
ALTER TABLE itens ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- Todos os registros existentes ficam ativos. Excluir passa a definir ativo=FALSE.
-- Nenhuma movimentação é apagada. Não remova as foreign keys.
-- Se preferir migrações manuais, após aplicá-las use:
-- SPRING_JPA_HIBERNATE_DDL_AUTO=validate
