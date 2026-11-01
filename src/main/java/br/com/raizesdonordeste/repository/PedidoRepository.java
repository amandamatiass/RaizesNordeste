package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.Pedido;
import br.com.raizesdonordeste.domain.StatusPedido;
import br.com.raizesdonordeste.web.dto.VendasPorUnidadeDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByChaveIdempotencia(String chave);

    List<Pedido> findAllByOrderByCriadoEmDesc(Pageable pageable);

    List<Pedido> findByStatusOrderByCriadoEmDesc(StatusPedido status, Pageable pageable);

    List<Pedido> findByUnidadeIdOrderByCriadoEmDesc(Long unidadeId, Pageable pageable);

    List<Pedido> findByUnidadeIdAndStatusOrderByCriadoEmDesc(Long unidadeId, StatusPedido status, Pageable pageable);

    @Query("select new br.com.raizesdonordeste.web.dto.VendasPorUnidadeDto(u.id, u.nome, count(p), sum(p.valorTotal)) "
            + "from Pedido p join p.unidade u "
            + "where p.status = :status and p.criadoEm >= :ini and p.criadoEm < :fim "
            + "group by u.id, u.nome order by sum(p.valorTotal) desc")
    List<VendasPorUnidadeDto> vendasPorUnidade(@Param("status") StatusPedido status,
                                               @Param("ini") LocalDateTime ini,
                                               @Param("fim") LocalDateTime fim);

    @Query("select new br.com.raizesdonordeste.web.dto.VendasPorUnidadeDto(u.id, u.nome, count(p), sum(p.valorTotal)) "
            + "from Pedido p join p.unidade u "
            + "where u.id = :unidadeId and p.status = :status and p.criadoEm >= :ini and p.criadoEm < :fim "
            + "group by u.id, u.nome")
    List<VendasPorUnidadeDto> vendasDaUnidade(@Param("status") StatusPedido status,
                                              @Param("ini") LocalDateTime ini,
                                              @Param("fim") LocalDateTime fim,
                                              @Param("unidadeId") Long unidadeId);
}
