package api_brindes.repository;

import api_brindes.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List; // Não esqueça dessa importação para a Lista!

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    // O método novo que criamos para limpar os filhos antes de apagar o pai
    @Transactional
    void deleteAllByItemId(Integer itemId);

    // O método que já existia para listar o histórico e tinha sumido (agora usando Integer!)
    List<Movimentacao> findByItemIdOrderByDataMovimentacaoDesc(Integer itemId);

}