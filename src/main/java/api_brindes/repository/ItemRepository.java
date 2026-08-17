package api_brindes.repository;

import api_brindes.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {


    List<Item> findByNomeContainingIgnoreCase(String nome);

    // E aqui está a busca pelo código exato do brinde
    Item findByCodigo(String codigo);
}
