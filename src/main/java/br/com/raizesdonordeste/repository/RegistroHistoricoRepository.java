package br.com.raizesdonordeste.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.raizesdonordeste.domain.RegistroHistorico;

public interface RegistroHistoricoRepository
        extends JpaRepository<RegistroHistorico, Long>, JpaSpecificationExecutor<RegistroHistorico> {
}
