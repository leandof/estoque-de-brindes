-- Migração manual para banco EXISTENTE, anterior a esta funcionalidade.
-- Execute UMA VEZ, com aplicação parada e backup validado, ANTES de iniciar a nova versão.
-- Não executar depois de ddl-auto=update já ter criado essas colunas.
-- MySQL faz commit implícito de DDL. Não há rollback transacional de ALTER TABLE.
-- Verifique primeiro: tipos precisam ser ENTRADA/SAIDA, ignorando caixa.
SELECT DISTINCT tipo FROM movimentacao;

ALTER TABLE movimentacao
    ADD COLUMN id_responsavel BIGINT NULL,
    ADD COLUMN observacao VARCHAR(500) NULL,
    ADD COLUMN estoque_anterior INT NULL,
    ADD COLUMN estoque_posterior INT NULL,
    ADD COLUMN item_codigo VARCHAR(50) NULL,
    ADD COLUMN item_nome VARCHAR(100) NULL,
    ADD CONSTRAINT fk_movimentacao_responsavel
        FOREIGN KEY (id_responsavel) REFERENCES usuarios(id) ON DELETE RESTRICT;

CREATE INDEX idx_mov_data ON movimentacao(data_movimentacao, id);
CREATE INDEX idx_mov_responsavel_data ON movimentacao(id_responsavel, data_movimentacao);
CREATE INDEX idx_mov_item_data ON movimentacao(id_item, data_movimentacao);

-- Não preencher autor/saldos antigos: não existe evidência suficiente para reconstruí-los.
-- O converter Java lê ENTRADA/SAIDA em qualquer caixa sem mudar a coluna VARCHAR existente.
-- Depois da migração, pode-se usar SPRING_JPA_HIBERNATE_DDL_AUTO=validate.
-- Banco novo de desenvolvimento: ddl-auto=update cria tabelas/colunas, sem rodar este arquivo.
