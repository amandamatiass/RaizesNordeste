# API Raízes do Nordeste (back-end)

Back-end REST da rede **Raízes do Nordeste**: cardápio por unidade, pedidos, pagamentos (via provedor externo),
estoque por unidade, histórico de operações críticas, controle de acesso por perfil, LGPD e programa de fidelidade.

**Stack:** Java 17 · Spring Boot 3.5 · Spring Data JPA (Hibernate) · Spring Security + JWT · Flyway ·
H2 (desenvolvimento) / PostgreSQL (produção) · springdoc (Swagger UI).

---

## 1. Como executar

**Requisitos:** JDK 17 ou superior e Maven 3.9+ (ou abra a pasta direto no IntelliJ / Eclipse / VS Code, que já
reconhecem o `pom.xml`).

```bash
mvn spring-boot:run
```

Sobe em `http://localhost:8080` com banco **H2 em memória** (nada para instalar) e **dados de exemplo**.

| O quê | Endereço |
|---|---|
| Swagger UI (testar a API pelo navegador) | http://localhost:8080/swagger-ui.html |
| Contrato OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Saúde da aplicação | http://localhost:8080/actuator/health |

### Usuários de exemplo

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador da matriz | `admin@raizes.com` | `admin123` |
| Gerente (Boa Viagem) | `gerente.recife@raizes.com` | `123456` |
| Funcionário (Boa Viagem) | `func.recife@raizes.com` | `123456` |
| Cliente | `cliente@teste.com` | `123456` |

Dados criados: 2 unidades (ids 1 e 2), 6 produtos (ids 1 a 6; o 6 é sazonal de junho/julho), cardápio e estoque
(50 por produto) em cada unidade e a campanha "Pontos em dobro".

### Testar pelo Swagger

1. Abra `POST /api/v1/auth/login`, clique em **Try it out**, informe e-mail e senha e execute.
2. Copie o `accessToken` da resposta.
3. Clique em **Authorize** (cadeado, no topo) e cole **somente o token** (sem a palavra `Bearer`).
4. Agora os endpoints protegidos funcionam para o perfil desse usuário.

### Rodar os testes

```bash
mvn test
```

Há um teste unitário das transições de status e um teste de integração que percorre o fluxo completo (pedido,
idempotência, pagamento, webhook repetido, preparo, conclusão, pontos, cancelamento e histórico).

---

## 2. Roteiro de teste com curl (fluxo completo)

```bash
BASE=http://localhost:8080/api/v1

# 1) Login do cliente e da equipe
CLI=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"cliente@teste.com","senha":"123456"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
FUNC=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"func.recife@raizes.com","senha":"123456"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

# 2) Cardápio da unidade 1 (público)
curl -s $BASE/unidades/1/cardapio

# 3) Criar pedido (canal APP => pagamento antecipado => status AGUARDANDO_PAGAMENTO)
curl -s -X POST $BASE/pedidos -H "Authorization: Bearer $CLI" -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: pedido-001' \
  -d '{"unidadeId":1,"canal":"APP","itens":[{"produtoId":1,"quantidade":2}]}'
# Repetir o mesmo comando devolve o mesmo pedido (idempotência), sem duplicar.

# 4) Sem pagamento aprovado o pedido NÃO avança (esperado: 422)
curl -s -X PATCH $BASE/pedidos/1/status -H "Authorization: Bearer $FUNC" \
  -H 'Content-Type: application/json' -d '{"status":"EM_PREPARO"}'

# 5) Iniciar pagamento (anote o idTransacaoExterno da resposta)
curl -s -X POST $BASE/pedidos/1/pagamentos -H "Authorization: Bearer $CLI"

# 6) Simular o retorno do provedor (webhook). A assinatura é HMAC-SHA256 do corpo com o segredo configurado.
BODY='{"transacaoId":"COLE_AQUI_O_ID_DA_TRANSACAO","status":"APROVADO"}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac 'segredo-webhook-dev' | sed 's/^.* //')
curl -s -X POST $BASE/webhooks/pagamentos -H "X-Signature: $SIG" -H 'Content-Type: application/json' -d "$BODY"
# Enviar de novo devolve {"resultado":"IGNORADO"}: notificação repetida não confirma duas vezes.

# 7) Agora o pedido está RECEBIDO; a equipe conduz até o fim
for S in EM_PREPARO PRONTO CONCLUIDO; do
  curl -s -X PATCH $BASE/pedidos/1/status -H "Authorization: Bearer $FUNC" \
    -H 'Content-Type: application/json' -d "{\"status\":\"$S\"}"; echo
done

# 8) Pontos do cliente e histórico de status
curl -s $BASE/clientes/me/pontos -H "Authorization: Bearer $CLI"
curl -s $BASE/pedidos/1/historico-status -H "Authorization: Bearer $CLI"
```

No Windows (PowerShell) é mais simples usar o Swagger UI.

---

## 3. Endpoints

Prefixo `/api/v1`. Erros em JSON padronizado: `400` dados inválidos · `401` não autenticado · `403` sem permissão ·
`404` não encontrado · `409` conflito (ex.: transição de status inválida, chave duplicada) ·
`422` regra de negócio violada (ex.: produto indisponível, estoque insuficiente).

| Método | Rota | Descrição | Perfil |
|---|---|---|---|
| POST | `/auth/login` | Autentica e devolve access e refresh token | Público |
| POST | `/auth/refresh` | Renova o token de acesso | Público (com refresh token) |
| GET | `/unidades` | Lista as unidades ativas | Público |
| POST | `/unidades` | Cadastra unidade | Administrador |
| PUT | `/unidades/{id}` | Atualiza funcionamento e atendimento | Administrador |
| GET | `/produtos` | Lista produtos | Administrador, Gerente |
| POST | `/produtos` | Cadastra produto | Administrador |
| GET | `/unidades/{id}/cardapio` | Itens habilitados e dentro do período de venda, com preço da unidade | Público |
| PUT | `/unidades/{id}/cardapio/{produtoId}` | Habilita o produto, define preço regional e período | Administrador |
| POST | `/pedidos` | Valida, reserva estoque e registra o pedido (aceita `Idempotency-Key`) | Cliente, Funcionário, Gerente |
| GET | `/pedidos/{id}` | Consulta e acompanha o pedido | Cliente (próprios), equipe da unidade, Admin |
| GET | `/pedidos?unidadeId=&status=&page=&size=` | Fila de pedidos | Admin, Gerente, Funcionário |
| PATCH | `/pedidos/{id}/status` | Avança o status pela sequência válida | Gerente, Funcionário |
| POST | `/pedidos/{id}/cancelamento` | Cancela com motivo, libera estoque e audita | Gerente, Funcionário |
| POST | `/pedidos/{id}/descontos` | Concede desconto e audita | Gerente |
| GET | `/pedidos/{id}/historico-status` | Etapas percorridas pelo pedido | Cliente (próprios), equipe, Admin |
| POST | `/pedidos/{id}/pagamentos` | Solicita o pagamento ao provedor | Cliente, Funcionário, Gerente |
| GET | `/pedidos/{id}/pagamentos` | Resultado dos pagamentos do pedido | Cliente (próprios), equipe, Admin |
| POST | `/webhooks/pagamentos` | Retorno do provedor (idempotente, assinatura HMAC) | Provedor de pagamento |
| GET | `/unidades/{id}/estoque` | Posição do estoque | Admin, Gerente, Funcionário (da unidade) |
| POST | `/unidades/{id}/estoque/movimentacoes` | Entrada, saída ou ajuste (audita) | Admin, Gerente |
| GET | `/unidades/{id}/estoque/movimentacoes` | Lista movimentações | Admin, Gerente |
| GET, POST | `/usuarios` | Lista / cria contas de acesso | Administrador |
| PATCH | `/usuarios/{id}` | Altera perfil, unidade, situação ou senha (audita) | Administrador |
| GET | `/historico-operacoes?acao=&usuarioId=&de=&ate=` | Consulta o histórico de operações críticas | Administrador |
| GET | `/relatorios/vendas?de=&ate=&unidadeId=` | Vendas por unidade e período | Administrador |
| GET | `/relatorios/produtos-mais-vendidos?de=&ate=&limite=` | Ranking de produtos | Administrador |
| POST | `/clientes` | Cadastro de cliente | Público |
| GET, PUT | `/clientes/me/consentimentos` | Consulta e atualiza consentimentos (LGPD) | Cliente |
| GET | `/clientes/me/pontos` | Saldo e extrato de pontos | Cliente |
| GET | `/campanhas` | Lista campanhas | Autenticado |
| POST, PUT | `/campanhas`, `/campanhas/{id}` | Gerencia campanhas | Administrador |

Gerente e funcionário só enxergam e alteram dados **da própria unidade**; o cliente só enxerga os próprios pedidos.

---


### Ciclo de vida do pedido

```
 antecipado:   AGUARDANDO_PAGAMENTO --(pagamento aprovado)--> RECEBIDO
 não antecip.:                                                RECEBIDO
                                      RECEBIDO -> EM_PREPARO -> PRONTO -> CONCLUIDO
 Cancelamento permitido até EM_PREPARO (RECEBIDO, AGUARDANDO_PAGAMENTO ou EM_PREPARO).
```

Canais `APP` e `WEB` usam pagamento antecipado por padrão; `TOTEM` e `BALCAO` não (pode ser sobrescrito com
`pagamentoAntecipado` no corpo do pedido).

---

## 4. PostgreSQL (perfil `postgres`)

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

O Flyway aplica `src/main/resources/db/migration` (`V1` cria as tabelas, `V2` torna o histórico imutável) e o
Hibernate apenas **valida** o esquema. Variáveis: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_JWT_SECRET`,
`APP_WEBHOOK_SECRET`. Se a validação do Hibernate acusar divergência, ajuste a migração ou use
`spring.jpa.hibernate.ddl-auto=none` temporariamente.

---

## 5. Estrutura

```
src/main/java/br/com/raizesdonordeste
 ├── config       SecurityConfig, OpenApiConfig, DataLoader (dados de exemplo)
 ├── security     JwtService, JwtAuthFilter, AuthUser, Acesso (regras por unidade)
 ├── domain       entidades JPA (15) e enums
 ├── repository   Spring Data JPA
 ├── service      regras de negócio (PedidoService, PagamentoService, EstoqueService, ...)
 ├── integracao   ProvedorPagamento (interface) e implementação simulada
 ├── exception    exceções e tratamento padronizado de erros
 └── web          controllers REST e web/dto (requisições e respostas)
```

---
Desenvolviddo por **Amanda Matias**.
RU: **4528383**.
