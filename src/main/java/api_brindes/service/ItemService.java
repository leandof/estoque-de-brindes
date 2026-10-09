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
    private final MovimentacaoService movimentacaoService;

    public ItemService(ItemRepository itens, MovimentacaoService movimentacaoService) {
        this.itens = itens;
        this.movimentacaoService = movimentacaoService;
    }

    @Transactional
    public Item cadastrar(CadastrarItemDTO dados) {
        validarValor(dados.valor());
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
        // Não zera o saldo nem cria uma saída fictícia. Apenas remove do inventário ativo.
        item.setAtivo(false);
        itens.save(item);
    }

    @Transactional
    public Item atualizarValor(Integer id, AtualizarValorDTO dados) {
        validarValor(dados.valor());
        // Mesmo bloqueio da movimentação: evita sobrescrever um saldo concorrente.
        var item = buscarComBloqueio(id);
        item.setValor(dados.valor());
        return itens.save(item);
    }

    private void validarValor(Double valor) {
        if (valor == null || !Double.isFinite(valor) || valor < 0 || valor > 999999999.99
                || java.math.BigDecimal.valueOf(valor).stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Informe um valor entre 0 e 999999999,99, com até duas casas decimais.");
        }
    }

    private Item buscarComBloqueio(Integer id) {
        return itens.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado."));
    }
}
