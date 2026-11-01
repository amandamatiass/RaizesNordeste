package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.domain.Consentimento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {

    List<Consentimento> findByClienteIdOrderByRegistradoEmDescIdDesc(Long clienteId);
}
