package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.Pagamento;
import br.com.raizesdonordeste.domain.StatusPagamento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByIdTransacaoExterno(String idTransacaoExterno);

    List<Pagamento> findByPedidoIdOrderByIdDesc(Long pedidoId);

    boolean existsByPedidoIdAndStatusIn(Long pedidoId, Collection<StatusPagamento> status);
}
