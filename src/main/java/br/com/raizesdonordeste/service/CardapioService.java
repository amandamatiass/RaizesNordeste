package br.com.raizesdonordeste.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste.domain.CardapioUnidade;
import br.com.raizesdonordeste.domain.Estoque;
import br.com.raizesdonordeste.domain.Produto;
import br.com.raizesdonordeste.domain.Unidade;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.exception.RegraNegocioException;
import br.com.raizesdonordeste.repository.CardapioUnidadeRepository;
import br.com.raizesdonordeste.repository.EstoqueRepository;
import br.com.raizesdonordeste.repository.ProdutoRepository;
import br.com.raizesdonordeste.repository.UnidadeRepository;
import br.com.raizesdonordeste.web.dto.CardapioDtos.ItemCardapioRequest;
import br.com.raizesdonordeste.web.dto.CardapioDtos.ItemCardapioResponse;
import br.com.raizesdonordeste.web.dto.CardapioDtos.ProdutoRequest;
import br.com.raizesdonordeste.web.dto.CardapioDtos.ProdutoResponse;

@Service
public class CardapioService {

    private final ProdutoRepository produtoRepository;
    private final UnidadeRepository unidadeRepository;
    private final CardapioUnidadeRepository cardapioRepository;
    private final EstoqueRepository estoqueRepository;

    public CardapioService(ProdutoRepository produtoRepository, UnidadeRepository unidadeRepository,
                           CardapioUnidadeRepository cardapioRepository, EstoqueRepository estoqueRepository) {
        this.produtoRepository = produtoRepository;
        this.unidadeRepository = unidadeRepository;
        this.cardapioRepository = cardapioRepository;
        this.estoqueRepository = estoqueRepository;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarProdutos() {
        return produtoRepository.findAllByOrderByNome().stream().map(ProdutoResponse::de).toList();
    }

    @Transactional
    public ProdutoResponse criarProduto(ProdutoRequest req) {
        Produto p = new Produto();
        p.setNome(req.nome().trim());
        p.setDescricao(req.descricao());
        p.setPrecoBase(req.precoBase());
        p.setSazonal(req.sazonal());
        return ProdutoResponse.de(produtoRepository.save(p));
    }

    /** Cria ou atualiza a configuração do produto na unidade (preço regional, habilitação e período de venda). */
    @Transactional
    public ItemCardapioResponse configurarItem(Long unidadeId, Long produtoId, ItemCardapioRequest req) {
        Unidade unidade = buscarUnidade(unidadeId);
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + produtoId));
        if (req.inicioVenda() != null && req.fimVenda() != null && req.fimVenda().isBefore(req.inicioVenda())) {
            throw new RegraNegocioException("O fim do período de venda não pode ser anterior ao início");
        }
        CardapioUnidade item = cardapioRepository.findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                .orElseGet(() -> {
                    CardapioUnidade novo = new CardapioUnidade();
                    novo.setUnidade(unidade);
                    novo.setProduto(produto);
                    return novo;
                });
        item.setPreco(req.preco());
        item.setHabilitado(req.habilitado() == null || req.habilitado());
        item.setInicioVenda(req.inicioVenda());
        item.setFimVenda(req.fimVenda());
        CardapioUnidade salvo = cardapioRepository.save(item);
        return paraResposta(salvo, null);
    }

    /** Cardápio vendável hoje na unidade: itens habilitados e dentro do período de comercialização. */
    @Transactional(readOnly = true)
    public List<ItemCardapioResponse> cardapio(Long unidadeId) {
        Unidade unidade = buscarUnidade(unidadeId);
        if (!unidade.isAtiva()) {
            throw new RegraNegocioException("Unidade inativa");
        }
        LocalDate hoje = LocalDate.now();
        Map<Long, Estoque> estoques = new HashMap<>();
        for (Estoque e : estoqueRepository.findByUnidadeIdOrderByProdutoNome(unidadeId)) {
            estoques.put(e.getProduto().getId(), e);
        }
        return cardapioRepository.findByUnidadeId(unidadeId).stream()
                .filter(c -> c.estaDisponivel(hoje))
                .map(c -> paraResposta(c, estoques.get(c.getProduto().getId())))
                .sorted(Comparator.comparing(ItemCardapioResponse::nome))
                .toList();
    }

    private ItemCardapioResponse paraResposta(CardapioUnidade c, Estoque estoque) {
        Produto p = c.getProduto();
        boolean temEstoque = estoque != null && estoque.disponivel() > 0;
        return new ItemCardapioResponse(p.getId(), p.getNome(), p.getDescricao(), c.getPreco(), p.isSazonal(),
                c.getInicioVenda(), c.getFimVenda(), c.isHabilitado(), temEstoque);
    }

    private Unidade buscarUnidade(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada: " + id));
    }
}
