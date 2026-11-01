package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.Unidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    List<Unidade> findByAtivaTrueOrderByNome();
}
