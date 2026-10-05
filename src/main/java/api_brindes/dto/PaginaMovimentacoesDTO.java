package api_brindes.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaMovimentacoesDTO(List<MovimentacaoDTO> conteudo, int pagina,
        int tamanho, long totalElementos, int totalPaginas) {
    public static PaginaMovimentacoesDTO de(Page<MovimentacaoDTO> page) {
        return new PaginaMovimentacoesDTO(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
