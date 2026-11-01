package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.CardapioUnidade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardapioUnidadeRepository extends JpaRepository<CardapioUnidade, Long> {

    List<CardapioUnidade> findByUnidadeId(Long unidadeId);

    Optional<CardapioUnidade> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);
}
