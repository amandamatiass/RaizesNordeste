package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.MovimentacaoEstoque;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    List<MovimentacaoEstoque> findByEstoqueUnidadeIdOrderByDataHoraDesc(Long unidadeId, Pageable pageable);
}
