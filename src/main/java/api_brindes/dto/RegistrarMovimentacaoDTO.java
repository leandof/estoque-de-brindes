package api_brindes.dto;

import api_brindes.model.TipoMovimentacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

// Mantém o JSON atual: {"item":{"id":1},"tipo":"ENTRADA","quantidade":10}.
// Não aceita ID da movimentação, responsável, saldos ou data fornecidos pelo cliente.
public record RegistrarMovimentacaoDTO(
        @NotNull @Valid ItemReferencia item,
        @NotNull TipoMovimentacao tipo,
        @NotNull @Positive Integer quantidade,
        @Size(max = 500) String observacao) {
    public record ItemReferencia(@NotNull @Positive Integer id) {}
}
