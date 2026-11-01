package br.com.raizesdonordeste.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste.domain.StatusPedido;
import br.com.raizesdonordeste.exception.RegraNegocioException;
import br.com.raizesdonordeste.repository.ItemPedidoRepository;
import br.com.raizesdonordeste.repository.PedidoRepository;
import br.com.raizesdonordeste.web.dto.ProdutoRankingDto;
import br.com.raizesdonordeste.web.dto.VendasPorUnidadeDto;

/** Considera apenas pedidos CONCLUIDOS, evitando contar pedidos cancelados ou ainda em andamento. */
@Service
public class RelatorioService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public RelatorioService(PedidoRepository pedidoRepository, ItemPedidoRepository itemPedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<VendasPorUnidadeDto> vendas(LocalDate de, LocalDate ate, Long unidadeId) {
        validarPeriodo(de, ate);
        LocalDateTime ini = de.atStartOfDay();
        LocalDateTime fim = ate.plusDays(1).atStartOfDay();
        if (unidadeId == null) {
            return pedidoRepository.vendasPorUnidade(StatusPedido.CONCLUIDO, ini, fim);
        }
        return pedidoRepository.vendasDaUnidade(StatusPedido.CONCLUIDO, ini, fim, unidadeId);
    }

    @Transactional(readOnly = true)
    public List<ProdutoRankingDto> produtosMaisVendidos(LocalDate de, LocalDate ate, int limite) {
        validarPeriodo(de, ate);
        return itemPedidoRepository.ranking(StatusPedido.CONCLUIDO, de.atStartOfDay(),
                ate.plusDays(1).atStartOfDay(), PageRequest.of(0, Math.min(Math.max(limite, 1), 50)));
    }

    private void validarPeriodo(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new RegraNegocioException("A data final não pode ser anterior à data inicial");
        }
    }
}
