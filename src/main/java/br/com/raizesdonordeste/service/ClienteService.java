package br.com.raizesdonordeste.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste.domain.Cliente;
import br.com.raizesdonordeste.domain.Consentimento;
import br.com.raizesdonordeste.exception.ConflitoException;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.repository.ClienteRepository;
import br.com.raizesdonordeste.repository.ConsentimentoRepository;
import br.com.raizesdonordeste.web.dto.ClienteDtos.ClienteRequest;
import br.com.raizesdonordeste.web.dto.ClienteDtos.ClienteResponse;
import br.com.raizesdonordeste.web.dto.ClienteDtos.ConsentimentoRequest;
import br.com.raizesdonordeste.web.dto.ClienteDtos.ConsentimentoResponse;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ConsentimentoRepository consentimentoRepository;
    private final PasswordEncoder encoder;

    public ClienteService(ClienteRepository clienteRepository, ConsentimentoRepository consentimentoRepository,
                          PasswordEncoder encoder) {
        this.clienteRepository = clienteRepository;
        this.consentimentoRepository = consentimentoRepository;
        this.encoder = encoder;
    }

    @Transactional
    public ClienteResponse cadastrar(ClienteRequest req) {
        String email = req.email().trim().toLowerCase();
        if (clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um cliente com este e-mail");
        }
        Cliente c = new Cliente();
        c.setNome(req.nome().trim());
        c.setEmail(email);
        c.setTelefone(req.telefone());
        c.setSenhaHash(encoder.encode(req.senha()));
        c.setCriadoEm(LocalDateTime.now());
        return ClienteResponse.de(clienteRepository.save(c));
    }

    /** Consentimentos vigentes: o registro mais recente de cada finalidade. */
    @Transactional(readOnly = true)
    public List<ConsentimentoResponse> consentimentos(Long clienteId) {
        Map<String, Consentimento> vigentes = new LinkedHashMap<>();
        for (Consentimento c : consentimentoRepository.findByClienteIdOrderByRegistradoEmDescIdDesc(clienteId)) {
            vigentes.putIfAbsent(c.getFinalidade().toLowerCase(), c);
        }
        return vigentes.values().stream().map(ConsentimentoResponse::de).toList();
    }

    /** Cada alteração gera um novo registro, preservando o histórico de consentimentos. */
    @Transactional
    public List<ConsentimentoResponse> atualizarConsentimentos(Long clienteId, List<ConsentimentoRequest> itens) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + clienteId));
        LocalDateTime agora = LocalDateTime.now();
        for (ConsentimentoRequest item : itens) {
            Consentimento c = new Consentimento();
            c.setCliente(cliente);
            c.setFinalidade(item.finalidade().trim().toUpperCase());
            c.setBaseLegal(item.baseLegal().trim());
            c.setConcedido(item.concedido());
            c.setRegistradoEm(agora);
            consentimentoRepository.save(c);
        }
        return consentimentos(clienteId);
    }
}
