package api_brindes.repository;

import api_brindes.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Integer> {

    // O Spring gera o SQL automático para buscar todo o histórico de um item específico!
    List<Movimentacao> findByItemIdOrderByDataMovimentacaoDesc(Integer idItem);

}