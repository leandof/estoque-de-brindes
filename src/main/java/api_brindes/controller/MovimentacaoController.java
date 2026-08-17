package api_brindes.controller;

import api_brindes.model.Movimentacao;
import api_brindes.repository.MovimentacaoRepository;
import api_brindes.service.MovimentacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movimentacoes") // A rota base agora é localhost:8080/movimentacoes
public class MovimentacaoController {

    @Autowired
    private MovimentacaoService movimentacaoService;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    // 1. REGISTRAR ENTRADA OU SAÍDA
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Movimentacao movimentacao) {
        try {
            // Repassa a bola para o nosso "Cérebro" validar a regra de negócio
            Movimentacao salva = movimentacaoService.registrar(movimentacao);
            return ResponseEntity.ok(salva); // Retorna 200 OK com os dados
        } catch (IllegalArgumentException e) {
            // Se cair na trava de segurança (ex: sem estoque), devolve 400 Bad Request com a mensagem
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. LISTAR HISTÓRICO GERAL
    @GetMapping
    public List<Movimentacao> listarTodas() {
        return movimentacaoRepository.findAll();
    }

    // 3. LISTAR HISTÓRICO DE UM ITEM ESPECÍFICO (Ex: localhost:8080/movimentacoes/item/4)
    @GetMapping("/item/{idItem}")
    public List<Movimentacao> listarPorItem(@PathVariable Integer idItem) {
        return movimentacaoRepository.findByItemIdOrderByDataMovimentacaoDesc(idItem);
    }
}