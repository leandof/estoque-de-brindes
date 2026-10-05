package api_brindes.repository;

import api_brindes.model.Movimentacao;
import api_brindes.model.Usuario;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Integer>,
        JpaSpecificationExecutor<Movimentacao> {
    boolean existsByItemId(Integer itemId);

    @EntityGraph(attributePaths = {"item", "responsavel"})
    List<Movimentacao> findAllByOrderByDataMovimentacaoDescIdDesc();

    @EntityGraph(attributePaths = {"item", "responsavel"})
    List<Movimentacao> findByItemIdOrderByDataMovimentacaoDescIdDesc(Integer itemId);

    @Query("select distinct m.responsavel from Movimentacao m where m.responsavel is not null order by m.responsavel.login")
    List<Usuario> findResponsaveisComMovimentacoes();
}
