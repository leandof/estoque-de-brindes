package api_brindes.controller;

import api_brindes.model.Item;
import api_brindes.dto.CadastrarItemDTO;
import api_brindes.dto.AtualizarValorDTO;
import api_brindes.service.ItemService;
import jakarta.validation.Valid;
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
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;

    @GetMapping
    public List<Item> listarTodos() {
        return itemRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @GetMapping("/historico")
    public List<Item> listarParaHistorico() {
        return itemRepository.findAll(org.springframework.data.domain.Sort.by("nome"));
    }

    @PostMapping
    public Item salvar(@RequestBody @Valid CadastrarItemDTO item) {
        return itemService.cadastrar(item);
    }

    @GetMapping("/relatorio")
    public ResponseEntity<Map<String, Object>> obterRelatorio() {
        List<Item> itens = itemRepository.findByAtivoTrueOrderByNomeAsc();

        long quantidadeTotal = 0;
        java.math.BigDecimal patrimonioTotal = java.math.BigDecimal.ZERO;

        // Faz a matemática real lendo o banco de dados
        for (Item item : itens) {
            quantidadeTotal += item.getQuantidade();
            patrimonioTotal = patrimonioTotal.add(java.math.BigDecimal.valueOf(item.getValor())
                    .multiply(java.math.BigDecimal.valueOf(item.getQuantidade())));
        }

        Map<String, Object> relatorio = new HashMap<>();

        // A MÁGICA ACONTECE AQUI: Nomes idênticos ao seu HTML!
        relatorio.put("totalDeBrindesCadastrados", quantidadeTotal);
        relatorio.put("valorTotalArmazenado", patrimonioTotal.setScale(2, java.math.RoundingMode.HALF_UP));
        relatorio.put("totalTiposDeBrindes", itens.size());

        return ResponseEntity.ok(relatorio);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        itemService.apagar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> atualizarValor(@PathVariable Integer id,
                                             @RequestBody @Valid AtualizarValorDTO dados) {
        return ResponseEntity.ok(itemService.atualizarValor(id, dados));
    }
}
