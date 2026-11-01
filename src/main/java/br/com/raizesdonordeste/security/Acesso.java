package br.com.raizesdonordeste.security;

import org.springframework.security.access.AccessDeniedException;

import br.com.raizesdonordeste.domain.Pedido;

/** Regras de acesso por perfil e por unidade (além das permissões por rota). */
public final class Acesso {

    private Acesso() {
    }

    public static boolean ehAdmin(AuthUser u) {
        return "ADMIN_MATRIZ".equals(u.perfil());
    }

    public static boolean ehCliente(AuthUser u) {
        return "CLIENTE".equals(u.perfil());
    }

    /** Admin acessa qualquer unidade; gerente e funcionário apenas a própria. */
    public static void exigirUnidade(AuthUser u, Long unidadeId) {
        if (ehAdmin(u)) {
            return;
        }
        if (!ehCliente(u) && u.unidadeId() != null && u.unidadeId().equals(unidadeId)) {
            return;
        }
        throw new AccessDeniedException("Acesso negado a esta unidade");
    }

    /** Cliente enxerga apenas os próprios pedidos; equipe, os da própria unidade. */
    public static void exigirAcessoPedido(AuthUser u, Pedido pedido) {
        if (ehAdmin(u)) {
            return;
        }
        if (ehCliente(u)) {
            if (pedido.getCliente() != null && pedido.getCliente().getId().equals(u.id())) {
                return;
            }
            throw new AccessDeniedException("Pedido pertence a outro cliente");
        }
        exigirUnidade(u, pedido.getUnidade().getId());
    }
}
