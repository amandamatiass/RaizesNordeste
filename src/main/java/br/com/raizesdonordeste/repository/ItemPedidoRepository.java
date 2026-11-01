package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.ItemPedido;
import br.com.raizesdonordeste.domain.StatusPedido;
import br.com.raizesdonordeste.web.dto.ProdutoRankingDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    @Query("select new br.com.raizesdonordeste.web.dto.ProdutoRankingDto(pr.id, pr.nome, sum(i.quantidade)) "
            + "from ItemPedido i join i.produto pr join i.pedido p "
            + "where p.status = :status and p.criadoEm >= :ini and p.criadoEm < :fim "
            + "group by pr.id, pr.nome order by sum(i.quantidade) desc")
    List<ProdutoRankingDto> ranking(@Param("status") StatusPedido status,
                                    @Param("ini") LocalDateTime ini,
                                    @Param("fim") LocalDateTime fim,
                                    Pageable pageable);
}
