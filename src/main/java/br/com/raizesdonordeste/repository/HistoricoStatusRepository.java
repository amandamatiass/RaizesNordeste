package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.HistoricoStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {

    List<HistoricoStatus> findByPedidoIdOrderByDataHoraAscIdAsc(Long pedidoId);
}
