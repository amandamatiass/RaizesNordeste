package br.com.raizesdonordeste.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste.domain.CardapioUnidade;
import br.com.raizesdonordeste.domain.Campanha;
import br.com.raizesdonordeste.domain.Cliente;
import br.com.raizesdonordeste.domain.Consentimento;
import br.com.raizesdonordeste.domain.Estoque;
import br.com.raizesdonordeste.domain.Perfil;
import br.com.raizesdonordeste.domain.Produto;
import br.com.raizesdonordeste.domain.Unidade;
import br.com.raizesdonordeste.domain.Usuario;
import br.com.raizesdonordeste.repository.CampanhaRepository;
import br.com.raizesdonordeste.repository.CardapioUnidadeRepository;
import br.com.raizesdonordeste.repository.ClienteRepository;
import br.com.raizesdonordeste.repository.ConsentimentoRepository;
import br.com.raizesdonordeste.repository.EstoqueRepository;
import br.com.raizesdonordeste.repository.ProdutoRepository;
import br.com.raizesdonordeste.repository.UnidadeRepository;
import br.com.raizesdonordeste.repository.UsuarioRepository;

/** Dados de exemplo para testar a API logo após subir a aplicação. Desative com app.seed.enabled=false. */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final UnidadeRepository unidades;
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final ConsentimentoRepository consentimentos;
    private final ProdutoRepository produtos;
    private final CardapioUnidadeRepository cardapios;
    private final EstoqueRepository estoques;
    private final CampanhaRepository campanhas;
    private final PasswordEncoder encoder;

    public DataLoader(UnidadeRepository unidades, UsuarioRepository usuarios, ClienteRepository clientes,
                      ConsentimentoRepository consentimentos, ProdutoRepository produtos,
                      CardapioUnidadeRepository cardapios, EstoqueRepository estoques,
                      CampanhaRepository campanhas, PasswordEncoder encoder) {
        this.unidades = unidades;
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.consentimentos = consentimentos;
        this.produtos = produtos;
        this.cardapios = cardapios;
        this.estoques = estoques;
        this.campanhas = campanhas;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarios.count() > 0) {
            return;
        }
        LocalDate hoje = LocalDate.now();

        Unidade recife = unidade("Raízes do Nordeste - Boa Viagem", "Recife", "Pernambuco");
        Unidade salvador = unidade("Raízes do Nordeste - Pelourinho", "Salvador", "Bahia");

        Produto baiao = produto("Baião de dois", "Arroz, feijão de corda, queijo coalho e manteiga de garrafa",
                "22.90", false);
        Produto carneDeSol = produto("Carne de sol com macaxeira", "Carne de sol na brasa com macaxeira cozida",
                "39.90", false);
        Produto tapioca = produto("Tapioca de queijo coalho", "Tapioca recheada com queijo coalho", "12.00", false);
        Produto cuscuz = produto("Cuscuz nordestino", "Cuscuz de milho com ovo e charque", "14.50", false);
        Produto sucoCaja = produto("Suco de cajá", "Suco natural de cajá", "9.00", false);
        Produto pamonha = produto("Pamonha junina", "Item sazonal de junho e julho", "10.00", true);

        for (Produto p : new Produto[] {baiao, carneDeSol, tapioca, cuscuz, sucoCaja}) {
            item(recife, p, p.getPrecoBase(), null, null);
            item(salvador, p, p.getPrecoBase().multiply(new BigDecimal("1.05")).setScale(2,
                    java.math.RoundingMode.HALF_UP), null, null);
        }
        // Item sazonal: só vendável entre 1/jun e 31/jul (fora do período, aparece como indisponível no pedido).
        item(recife, pamonha, pamonha.getPrecoBase(), LocalDate.of(hoje.getYear(), 6, 1),
                LocalDate.of(hoje.getYear(), 7, 31));

        usuario("Administrador da Matriz", "admin@raizes.com", "admin123", Perfil.ADMIN_MATRIZ, null);
        usuario("Gerente Boa Viagem", "gerente.recife@raizes.com", "123456", Perfil.GERENTE, recife);
        usuario("Funcionário Boa Viagem", "func.recife@raizes.com", "123456", Perfil.FUNCIONARIO, recife);

        Cliente cliente = new Cliente();
        cliente.setNome("Cliente de Teste");
        cliente.setEmail("cliente@teste.com");
        cliente.setTelefone("81999990000");
        cliente.setSenhaHash(encoder.encode("123456"));
        cliente.setCriadoEm(LocalDateTime.now());
        cliente = clientes.save(cliente);

        Consentimento c = new Consentimento();
        c.setCliente(cliente);
        c.setFinalidade("FIDELIDADE");
        c.setBaseLegal("CONSENTIMENTO");
        c.setConcedido(true);
        c.setRegistradoEm(LocalDateTime.now());
        consentimentos.save(c);

        Campanha dobro = new Campanha();
        dobro.setNome("Pontos em dobro");
        dobro.setRegra("Multiplica por 2 os pontos de cada pedido concluído");
        dobro.setMultiplicador(new BigDecimal("2.00"));
        dobro.setInicio(hoje.minusDays(1));
        dobro.setFim(hoje.plusMonths(6));
        dobro.setAtiva(true);
        campanhas.save(dobro);

        log.info("Dados de exemplo criados. Logins: admin@raizes.com/admin123, gerente.recife@raizes.com/123456, "
                + "func.recife@raizes.com/123456, cliente@teste.com/123456");
    }

    private Unidade unidade(String nome, String cidade, String regiao) {
        Unidade u = new Unidade();
        u.setNome(nome);
        u.setCidade(cidade);
        u.setRegiao(regiao);
        u.setTipoAtendimento("SALAO_E_DELIVERY");
        u.setHorarioFuncionamento("10h as 22h");
        u.setAtiva(true);
        return unidades.save(u);
    }

    private Produto produto(String nome, String descricao, String preco, boolean sazonal) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setDescricao(descricao);
        p.setPrecoBase(new BigDecimal(preco));
        p.setSazonal(sazonal);
        return produtos.save(p);
    }

    private void item(Unidade unidade, Produto produto, BigDecimal preco, LocalDate inicio, LocalDate fim) {
        CardapioUnidade ci = new CardapioUnidade();
        ci.setUnidade(unidade);
        ci.setProduto(produto);
        ci.setPreco(preco);
        ci.setHabilitado(true);
        ci.setInicioVenda(inicio);
        ci.setFimVenda(fim);
        cardapios.save(ci);

        Estoque e = new Estoque();
        e.setUnidade(unidade);
        e.setProduto(produto);
        e.setQuantidade(50);
        e.setReservado(0);
        estoques.save(e);
    }

    private void usuario(String nome, String email, String senha, Perfil perfil, Unidade unidade) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(senha));
        u.setPerfil(perfil);
        u.setUnidade(unidade);
        u.setAtivo(true);
        usuarios.save(u);
    }
}
