package api_brindes.service;

import api_brindes.dto.*;
import api_brindes.model.Item;
import api_brindes.model.TipoMovimentacao;
import api_brindes.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItemService {
    private final ItemRepository itens;
    private final MovimentacaoRepository movimentacoes;
    private final MovimentacaoService movimentacaoService;

    public ItemService(ItemRepository itens, MovimentacaoRepository movimentacoes, MovimentacaoService movimentacaoService) {
        this.itens = itens;
        this.movimentacoes = movimentacoes;
        this.movimentacaoService = movimentacaoService;
    }

    @Transactional
    public Item cadastrar(CadastrarItemDTO dados) {
        // Nunca recebe ID nem altera saldo de item existente através do cadastro.
        Item item = itens.save(new Item(dados.codigo().trim(), dados.nome().trim(), 0, dados.valor()));
        if (dados.quantidade() > 0) {
            movimentacaoService.registrar(new RegistrarMovimentacaoDTO(
                    new RegistrarMovimentacaoDTO.ItemReferencia(item.getId()), TipoMovimentacao.ENTRADA,
                    dados.quantidade(), "Estoque inicial do cadastro"));
        }
        return item;
    }

    @Transactional
    public void apagar(Integer id) {
        var item = buscarComBloqueio(id);
        if (movimentacoes.existsByItemId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este item possui histórico e não pode ser excluído.");
        }
        itens.delete(item);
    }

    @Transactional
    public Item atualizarValor(Integer id, AtualizarValorDTO dados) {
        // Mesmo bloqueio da movimentação: evita sobrescrever um saldo concorrente.
        var item = buscarComBloqueio(id);
        item.setValor(dados.valor());
        return itens.save(item);
    }

    private Item buscarComBloqueio(Integer id) {
        return itens.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado."));
    }
}
