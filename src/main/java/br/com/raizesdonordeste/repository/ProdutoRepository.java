package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.Produto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findAllByOrderByNome();
}
