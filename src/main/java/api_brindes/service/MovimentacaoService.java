package api_brindes.service;

import api_brindes.model.Item;
import api_brindes.model.Movimentacao;
import api_brindes.repository.ItemRepository;
import api_brindes.repository.MovimentacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentacaoService {

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private ItemRepository itemRepository;

    // @Transactional garante que se der erro no meio (ex: falta de estoque),
    // ele cancela tudo e não salva nada pela metade.
    @Transactional
    public Movimentacao registrar(Movimentacao movimentacao) {

        // 1. Busca o item no banco de dados para ver se ele existe
        Item item = itemRepository.findById(movimentacao.getItem().getId())
                .orElseThrow(() -> new IllegalArgumentException("❌ Item não encontrado no estoque!"));

        // 2. Validações básicas de segurança
        if (movimentacao.getQuantidade() <= 0) {
            throw new IllegalArgumentException("❌ A quantidade deve ser maior que zero.");
        }

        // 3. Verifica o tipo e atualiza o estoque
        if (movimentacao.getTipo().equalsIgnoreCase("SAIDA")) {
            if (item.getQuantidade() < movimentacao.getQuantidade()) {
                throw new IllegalArgumentException("❌ Estoque insuficiente para '" + item.getNome() +
                        "'. Disponível: " + item.getQuantidade());
            }
            // Subtrai do estoque
            item.setQuantidade(item.getQuantidade() - movimentacao.getQuantidade());

        } else if (movimentacao.getTipo().equalsIgnoreCase("ENTRADA")) {
            // Adiciona ao estoque
            item.setQuantidade(item.getQuantidade() + movimentacao.getQuantidade());

        } else {
            throw new IllegalArgumentException("❌ Tipo de movimentação inválido. Use 'ENTRADA' ou 'SAIDA'.");
        }

        // 4. Salva a nova quantidade do item no banco
        itemRepository.save(item);

        // 5. Salva a movimentação no histórico (a data entra automaticamente)
        return movimentacaoRepository.save(movimentacao);
    }
}