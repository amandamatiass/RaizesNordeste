package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.Campanha;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampanhaRepository extends JpaRepository<Campanha, Long> {

    @Query("select c from Campanha c where c.ativa = true and c.inicio <= :hoje and c.fim >= :hoje")
    List<Campanha> findVigentes(@Param("hoje") LocalDate hoje);

    List<Campanha> findAllByOrderByInicioDesc();
}
