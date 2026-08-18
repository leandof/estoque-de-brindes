package api_brindes.controller;

import api_brindes.model.Item;
import api_brindes.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/itens")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @GetMapping
    public List<Item> listarTodos() {
        return itemRepository.findAll();
    }

    @PostMapping
    public Item salvar(@RequestBody Item item) {
        return itemRepository.save(item);
    }

    @GetMapping("/relatorio")
    public ResponseEntity<Map<String, Object>> obterRelatorio() {
        List<Item> itens = itemRepository.findAll();

        int quantidadeTotal = 0;
        double patrimonioTotal = 0.0;

        // Faz a matemática real lendo o banco de dados
        for (Item item : itens) {
            quantidadeTotal += item.getQuantidade();
            patrimonioTotal += (item.getQuantidade() * item.getValor());
        }

        Map<String, Object> relatorio = new HashMap<>();

        // A MÁGICA ACONTECE AQUI: Nomes idênticos ao seu HTML!
        relatorio.put("totalDeBrindesCadastrados", quantidadeTotal);
        relatorio.put("valorTotalArmazenado", patrimonioTotal);

        return ResponseEntity.ok(relatorio);
    }
    // 5. REMOVER ITEM
    // Acessado via: DELETE http://localhost:8080/itens/1
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Integer id) {
        if (itemRepository.existsById(id)) {
            itemRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<Item> atualizarValor(@PathVariable Integer id, @RequestBody Item itemAtualizado) {
        // Trocamos Long por Integer e colocamos itemRepository
        Item itemExistente = itemRepository.findById(id).orElseThrow();

        // Atualiza apenas o valor unitário
        itemExistente.setValor(itemAtualizado.getValor());

        itemRepository.save(itemExistente);
        return ResponseEntity.ok(itemExistente);
    }


}