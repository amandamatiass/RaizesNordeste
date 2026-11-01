package br.com.raizesdonordeste.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cardapio_unidade", uniqueConstraints = @UniqueConstraint(name = "uk_cardapio_unidade_produto", columnNames = {"unidade_id", "produto_id"}))
public class CardapioUnidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "preco", nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "habilitado", nullable = false)
    private boolean habilitado;

    @Column(name = "inicio_venda")
    private LocalDate inicioVenda;

    @Column(name = "fim_venda")
    private LocalDate fimVenda;

    public Long getId() {
        return id;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public void setUnidade(Unidade unidade) {
        this.unidade = unidade;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public LocalDate getInicioVenda() {
        return inicioVenda;
    }

    public void setInicioVenda(LocalDate inicioVenda) {
        this.inicioVenda = inicioVenda;
    }

    public LocalDate getFimVenda() {
        return fimVenda;
    }

    public void setFimVenda(LocalDate fimVenda) {
        this.fimVenda = fimVenda;
    }

    /** O item só é vendável se estiver habilitado e dentro do período de comercialização (itens sazonais). */
    public boolean estaDisponivel(LocalDate hoje) {
        if (!habilitado) {
            return false;
        }
        if (inicioVenda != null && hoje.isBefore(inicioVenda)) {
            return false;
        }
        return fimVenda == null || !hoje.isAfter(fimVenda);
    }
}
