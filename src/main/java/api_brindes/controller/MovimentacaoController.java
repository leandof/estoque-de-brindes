package api_brindes.controller;

import api_brindes.dto.*;
import api_brindes.model.TipoMovimentacao;
import api_brindes.service.MovimentacaoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/movimentacoes")
public class MovimentacaoController {
    private final MovimentacaoService service;
    public MovimentacaoController(MovimentacaoService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<MovimentacaoDTO> registrar(@RequestBody @Valid RegistrarMovimentacaoDTO dados) {
        return ResponseEntity.status(201).body(service.registrar(dados));
    }
    @GetMapping
    public List<MovimentacaoDTO> listarTodas() { return service.listarTodas(); }
    @GetMapping("/item/{idItem}")
    public List<MovimentacaoDTO> listarPorItem(@PathVariable Integer idItem) { return service.listarPorItem(idItem); }
    @GetMapping("/responsaveis")
    public List<MovimentacaoDTO.ResponsavelResumo> responsaveis() { return service.responsaveis(); }

    @GetMapping("/relatorio")
    public PaginaMovimentacoesDTO relatorio(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false) Integer itemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.consultar(dataInicial, dataFinal, responsavelId, tipo, itemId, pagina, tamanho);
    }
}
