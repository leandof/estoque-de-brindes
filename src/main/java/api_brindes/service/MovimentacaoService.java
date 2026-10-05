package api_brindes.service;

import api_brindes.dto.*;
import api_brindes.model.*;
import api_brindes.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MovimentacaoService {
    private final MovimentacaoRepository repository;
    private final ItemRepository itens;
    private final UsuarioAutenticadoService usuarioAutenticado;

    public MovimentacaoService(MovimentacaoRepository repository, ItemRepository itens,
                               UsuarioAutenticadoService usuarioAutenticado) {
        this.repository = repository;
        this.itens = itens;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Transactional
    public MovimentacaoDTO registrar(RegistrarMovimentacaoDTO dados) {
        var usuario = usuarioAutenticado.obter();
        if (dados.item() == null || dados.item().id() == null || dados.tipo() == null
                || dados.quantidade() == null || dados.quantidade() <= 0) {
            throw new IllegalArgumentException("Informe item, tipo e quantidade positiva.");
        }
        Item item = itens.findByIdForUpdate(dados.item().id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado."));
        int anterior = item.getQuantidade();
        if (dados.tipo() == TipoMovimentacao.SAIDA && anterior < dados.quantidade()) {
            throw new IllegalArgumentException("Estoque insuficiente. Disponível: " + anterior);
        }
        final int posterior;
        try {
            posterior = dados.tipo() == TipoMovimentacao.ENTRADA
                    ? Math.addExact(anterior, dados.quantidade()) : anterior - dados.quantidade();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Quantidade acima do limite suportado.");
        }
        item.setQuantidade(posterior);
        itens.save(item);
        var movimentacao = new Movimentacao(item, usuario, dados.tipo(), dados.quantidade(),
                dados.observacao(), anterior, posterior);
        return MovimentacaoDTO.de(repository.save(movimentacao));
    }

    public List<MovimentacaoDTO> listarTodas() {
        return repository.findAllByOrderByDataMovimentacaoDescIdDesc().stream().map(MovimentacaoDTO::de).toList();
    }

    public List<MovimentacaoDTO> listarPorItem(Integer id) {
        return repository.findByItemIdOrderByDataMovimentacaoDescIdDesc(id).stream().map(MovimentacaoDTO::de).toList();
    }

    public List<MovimentacaoDTO.ResponsavelResumo> responsaveis() {
        return repository.findResponsaveisComMovimentacoes().stream()
                .map(u -> new MovimentacaoDTO.ResponsavelResumo(u.getId(), u.getLogin())).toList();
    }

    // Um único método combina filtros; cada um também funciona isoladamente.
    public PaginaMovimentacoesDTO consultar(LocalDate inicio, LocalDate fim, Long responsavelId,
            TipoMovimentacao tipo, Integer itemId, int pagina, int tamanho) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à final.");
        }
        if (fim != null && fim.equals(LocalDate.MAX)) {
            throw new IllegalArgumentException("Data final fora do intervalo suportado.");
        }
        if (pagina < 0 || tamanho < 1 || tamanho > 100
                || (responsavelId != null && responsavelId <= 0) || (itemId != null && itemId <= 0)) {
            throw new IllegalArgumentException("Filtros ou paginação inválidos (tamanho: 1 a 100).");
        }
        Specification<Movimentacao> filtros = (root, query, cb) -> {
            // Fetch apenas na consulta de dados; a contagem não pode conter fetch joins.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("item");
                root.fetch("responsavel", jakarta.persistence.criteria.JoinType.LEFT);
            }
            List<Predicate> condicoes = new ArrayList<>();
            if (inicio != null) condicoes.add(cb.greaterThanOrEqualTo(root.get("dataMovimentacao"), inicio.atStartOfDay()));
            if (fim != null) condicoes.add(cb.lessThan(root.get("dataMovimentacao"), fim.plusDays(1).atStartOfDay()));
            if (responsavelId != null) condicoes.add(cb.equal(root.get("responsavel").get("id"), responsavelId));
            if (tipo != null) condicoes.add(cb.equal(cb.upper(root.get("tipo").as(String.class)), tipo.name()));
            if (itemId != null) condicoes.add(cb.equal(root.get("item").get("id"), itemId));
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        var ordem = Sort.by(Sort.Order.desc("dataMovimentacao"), Sort.Order.desc("id"));
        return PaginaMovimentacoesDTO.de(repository.findAll(filtros, PageRequest.of(pagina, tamanho, ordem))
                .map(MovimentacaoDTO::de));
    }
}
