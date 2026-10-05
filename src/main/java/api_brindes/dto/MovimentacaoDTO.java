package api_brindes.dto;

import api_brindes.model.Movimentacao;
import api_brindes.model.TipoMovimentacao;
import java.time.LocalDateTime;

/** Projeção segura: nunca serializa Usuario nem seus hashes de senha. */
public record MovimentacaoDTO(Integer id, LocalDateTime dataMovimentacao,
        TipoMovimentacao tipo, ItemResumo item, Integer quantidade,
        ResponsavelResumo responsavel, String observacao,
        Integer estoqueAnterior, Integer estoquePosterior) {
    public record ItemResumo(Integer id, String codigo, String nome) {}
    public record ResponsavelResumo(Long id, String login) {}

    public static MovimentacaoDTO de(Movimentacao m) {
        var item = m.getItem();
        var usuario = m.getResponsavel();
        return new MovimentacaoDTO(m.getId(), m.getDataMovimentacao(), m.getTipo(),
                new ItemResumo(item.getId(),
                        m.getItemCodigo() == null ? item.getCodigo() : m.getItemCodigo(),
                        m.getItemNome() == null ? item.getNome() : m.getItemNome()),
                m.getQuantidade(), usuario == null ? null :
                        new ResponsavelResumo(usuario.getId(), usuario.getLogin()),
                m.getObservacao(), m.getEstoqueAnterior(), m.getEstoquePosterior());
    }
}
