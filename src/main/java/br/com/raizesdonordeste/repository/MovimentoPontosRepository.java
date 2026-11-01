package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.MovimentoPontos;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimentoPontosRepository extends JpaRepository<MovimentoPontos, Long> {

    List<MovimentoPontos> findByClienteIdOrderByDataHoraDescIdDesc(Long clienteId, Pageable pageable);

    @Query("select coalesce(sum(m.pontos), 0L) from MovimentoPontos m where m.cliente.id = :clienteId")
    Long saldo(@Param("clienteId") Long clienteId);
}
