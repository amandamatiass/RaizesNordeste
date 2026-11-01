package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.Estoque;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

    List<Estoque> findByUnidadeIdOrderByProdutoNome(Long unidadeId);

    /** Reserva atômica: só atualiza se houver saldo disponível (quantidade - reservado). */
    @Modifying(flushAutomatically = true)
    @Query("update Estoque e set e.reservado = e.reservado + :qtd "
            + "where e.id = :id and (e.quantidade - e.reservado) >= :qtd")
    int reservar(@Param("id") Long id, @Param("qtd") int qtd);

    @Modifying(flushAutomatically = true)
    @Query("update Estoque e set e.reservado = e.reservado - :qtd where e.id = :id and e.reservado >= :qtd")
    int liberar(@Param("id") Long id, @Param("qtd") int qtd);

    /** Consome o estoque reservado quando o pedido é concluído. */
    @Modifying(flushAutomatically = true)
    @Query("update Estoque e set e.quantidade = e.quantidade - :qtd, e.reservado = e.reservado - :qtd "
            + "where e.id = :id and e.quantidade >= :qtd and e.reservado >= :qtd")
    int baixar(@Param("id") Long id, @Param("qtd") int qtd);

    @Modifying(flushAutomatically = true)
    @Query("update Estoque e set e.quantidade = e.quantidade + :qtd where e.id = :id")
    int entrada(@Param("id") Long id, @Param("qtd") int qtd);

    @Modifying(flushAutomatically = true)
    @Query("update Estoque e set e.quantidade = e.quantidade - :qtd "
            + "where e.id = :id and (e.quantidade - e.reservado) >= :qtd")
    int saida(@Param("id") Long id, @Param("qtd") int qtd);

    @Modifying(flushAutomatically = true)
    @Query("update Estoque e set e.quantidade = :nova where e.id = :id and e.reservado <= :nova")
    int ajustar(@Param("id") Long id, @Param("nova") int nova);
}
