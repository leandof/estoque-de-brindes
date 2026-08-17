package api_brindes.controller;

import api_brindes.model.Item;
import api_brindes.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;

import java.util.List;

@RestController
@RequestMapping("/itens") // Define que a URL base será localhost:8080/itens
public class ItemController {

    // O Spring injeta o repositório aqui automaticamente
    @Autowired
    private ItemRepository itemRepository;

    // 1. LISTAR TODOS (Equivalente à opção 1 do seu antigo menu)
    // Acessado via: GET http://localhost:8080/itens
    @GetMapping
    public List<Item> listarTodos() {
        return itemRepository.findAll();
    }

    // 2. BUSCAR POR NOME (Equivalente à opção 2 do seu menu)
    // Acessado via: GET http://localhost:8080/itens/busca?nome=bone
    @GetMapping("/busca")
    public List<Item> buscarPorNome(@RequestParam String nome) {
        return itemRepository.findByNomeContainingIgnoreCase(nome);
    }

    // 3. CADASTRAR ITEM (Equivalente à opção 3 do seu menu)
    @PostMapping
    public Item cadastrar(@RequestBody Item novoItem) {
        return itemRepository.save(novoItem);
    }


    // 4. RELATÓRIO DE ESTOQUE (Equivalente à opção 3 do seu menu antigo)
    // Acessado via: GET http://localhost:8080/itens/relatorio
    @GetMapping("/relatorio")
    public ResponseEntity<Map<String, Object>> obterRelatorio() {
        List<Item> itens = itemRepository.findAll();

        long totalTiposDeBrindes = itens.size();

        // Calcula o valor total financeiro (quantidade * valor unitário de cada item)
        double valorTotalFinanceiro = itens.stream()
                .mapToDouble(item -> item.getQuantidade() * item.getValor())
                .sum();

        // Monta um objeto JSON dinâmico com o resultado
        Map<String, Object> relatorio = new HashMap<>();
        relatorio.put("totalDeBrindesCadastrados", totalTiposDeBrindes);
        relatorio.put("valorTotalArmazenado", String.format("R$ %.2f", valorTotalFinanceiro));

        return ResponseEntity.ok(relatorio);
    }


}