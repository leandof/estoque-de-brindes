package api_brindes.repository;

import api_brindes.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {


    // Serializa alterações do mesmo item durante a transação.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Integer id);

    List<Item> findByNomeContainingIgnoreCase(String nome);

    // E aqui está a busca pelo código exato do brinde
    Item findByCodigo(String codigo);
}
