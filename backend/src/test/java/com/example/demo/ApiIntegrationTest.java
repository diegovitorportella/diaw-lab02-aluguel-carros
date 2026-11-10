package com.example.demo;

import com.example.demo.model.Banco;
import com.example.demo.model.Empresa;
import com.example.demo.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração ponta a ponta da API (Spring Security + JWT + JPA em H2 em memória).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String SENHA = "senha123";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    // ---------------------------------------------------------------- helpers

    private String unico(String prefixo) {
        return prefixo + "-" + System.nanoTime() + "-" + SEQ.incrementAndGet();
    }

    private ResultActions enviar(MockHttpServletRequestBuilder req, String token, String corpo) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (corpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mvc.perform(req);
    }

    private String lerTexto(ResultActions r, String caminho) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), caminho);
    }

    private long lerId(ResultActions r) throws Exception {
        Number n = JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
        return n.longValue();
    }

    private String login(String login, String senha) throws Exception {
        ResultActions r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + login + "\",\"senha\":\"" + senha + "\"}")).andExpect(status().isOk());
        return lerTexto(r, "$.token");
    }

    /** Cadastra um cliente pela rota pública e devolve {login, id}. */
    private Object[] cadastrarCliente() throws Exception {
        String login = unico("cliente");
        ResultActions r = enviar(post("/api/clientes"), null,
                "{\"login\":\"" + login + "\",\"senha\":\"" + SENHA + "\",\"nome\":\"Fulano\",\"cpf\":\"123\","
                        + "\"profissao\":\"Dev\",\"rendimentosAuferidos\":[5000],\"entidadesEmpregadoras\":[\"ACME\"]}")
                .andExpect(status().isOk());
        return new Object[]{login, lerId(r)};
    }

    private String tokenDeNovoCliente() throws Exception {
        return login((String) cadastrarCliente()[0], SENHA);
    }

    private String tokenDeBanco() throws Exception {
        Banco b = new Banco();
        b.setLogin(unico("banco"));
        b.setSenha(encoder.encode(SENHA));
        b.setCnpj("00");
        b.setRazaoSocial("Banco Teste");
        usuarios.save(b);
        return login(b.getLogin(), SENHA);
    }

    private String tokenDeEmpresa() throws Exception {
        Empresa e = new Empresa();
        e.setLogin(unico("empresa"));
        e.setSenha(encoder.encode(SENHA));
        e.setCnpj("11");
        e.setRazaoSocial("Empresa Teste");
        usuarios.save(e);
        return login(e.getLogin(), SENHA);
    }

    private long criarAutomovel(String token) throws Exception {
        ResultActions r = enviar(post("/api/automoveis"), token,
                "{\"marca\":\"Hyundai\",\"modelo\":\"Ioniq 6\",\"ano\":2025,\"placa\":\"ABC-1234\",\"matricula\":\"M-1\"}")
                .andExpect(status().isOk());
        return lerId(r);
    }

    private long criarPedido(String tokenCliente, long automovelId) throws Exception {
        ResultActions r = enviar(post("/api/pedidos"), tokenCliente,
                "{\"automovel\":{\"id\":" + automovelId + "},\"dataInicio\":\"2026-12-01\",\"dataFim\":\"2026-12-10\"}")
                .andExpect(status().isOk());
        return lerId(r);
    }

    // ---------------------------------------------------------------- autenticação

    @Test
    void rotasProtegidasExigemToken() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/automoveis")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/contratos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pedidos").header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginInvalidoRetorna401() throws Exception {
        Object[] c = cadastrarCliente();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + c[0] + "\",\"senha\":\"errada\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"nao-existe\",\"senha\":\"x\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginRetornaTokenETipoENuncaExpoeASenha() throws Exception {
        Object[] c = cadastrarCliente();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + c[0] + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.tipo").value("CLIENTE"))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist());
    }

    @Test
    void senhaEhArmazenadaComHashBcrypt() throws Exception {
        Object[] c = cadastrarCliente();
        String armazenada = usuarios.findByLogin((String) c[0]).orElseThrow().getSenha();
        org.junit.jupiter.api.Assertions.assertTrue(armazenada.startsWith("$2"));
        org.junit.jupiter.api.Assertions.assertNotEquals(SENHA, armazenada);
    }

    @Test
    void meRetornaOUsuarioDoToken() throws Exception {
        Object[] c = cadastrarCliente();
        String token = login((String) c[0], SENHA);
        enviar(get("/api/auth/me"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(c[0]))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    // ---------------------------------------------------------------- cliente

    @Test
    void cadastroRejeitaMaisDeTresRendimentos() throws Exception {
        enviar(post("/api/clientes"), null,
                "{\"login\":\"" + unico("x") + "\",\"senha\":\"" + SENHA + "\",\"rendimentosAuferidos\":[1,2,3,4]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("no máximo 3 rendimentos")));
    }

    @Test
    void cadastroRejeitaLoginDuplicadoESenhaCurta() throws Exception {
        Object[] c = cadastrarCliente();
        enviar(post("/api/clientes"), null, "{\"login\":\"" + c[0] + "\",\"senha\":\"" + SENHA + "\"}")
                .andExpect(status().isBadRequest());
        enviar(post("/api/clientes"), null, "{\"login\":\"" + unico("y") + "\",\"senha\":\"123\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void somenteAgenteListaClientes() throws Exception {
        String cliente = tokenDeNovoCliente();
        enviar(get("/api/clientes"), cliente, null).andExpect(status().isForbidden());
        enviar(get("/api/clientes"), tokenDeBanco(), null).andExpect(status().isOk());
    }

    @Test
    void clienteAtualizaOsProprioDadosMasNaoOsDeOutro() throws Exception {
        Object[] a = cadastrarCliente();
        Object[] b = cadastrarCliente();
        String tokenA = login((String) a[0], SENHA);

        enviar(put("/api/clientes/" + a[1]), tokenA, "{\"nome\":\"Novo Nome\",\"rendimentosAuferidos\":[1,2]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"))
                .andExpect(jsonPath("$.rendimentosAuferidos", hasSize(2)));

        enviar(put("/api/clientes/" + a[1]), tokenA, "{\"rendimentosAuferidos\":[1,2,3,4]}")
                .andExpect(status().isBadRequest());
        enviar(get("/api/clientes/" + b[1]), tokenA, null).andExpect(status().isForbidden());
        enviar(delete("/api/clientes/" + b[1]), tokenA, null).andExpect(status().isForbidden());
    }

    @Test
    void clienteExcluiAPropriaContaSemPedidos() throws Exception {
        Object[] c = cadastrarCliente();
        String token = login((String) c[0], SENHA);
        enviar(delete("/api/clientes/" + c[1]), token, null).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + c[0] + "\",\"senha\":\"" + SENHA + "\"}")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- automóvel

    @Test
    void crudDeAutomovelComProprietarioDoToken() throws Exception {
        String token = tokenDeNovoCliente();
        long id = criarAutomovel(token);

        enviar(get("/api/automoveis/" + id), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca").value("Hyundai"))
                .andExpect(jsonPath("$.proprietario.tipo").value("CLIENTE"));

        enviar(get("/api/automoveis/meus"), token, null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        enviar(put("/api/automoveis/" + id), token,
                "{\"marca\":\"Kia\",\"modelo\":\"EV6\",\"ano\":2024,\"placa\":\"XYZ-9876\",\"matricula\":\"M-2\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.marca").value("Kia"));

        enviar(delete("/api/automoveis/" + id), token, null).andExpect(status().isNoContent());
        enviar(get("/api/automoveis/" + id), token, null).andExpect(status().isNotFound());
    }

    @Test
    void bancoEEmpresaTambemPodemSerProprietarios() throws Exception {
        enviar(get("/api/automoveis/" + criarAutomovel(tokenDeBanco())), tokenDeNovoCliente(), null)
                .andExpect(jsonPath("$.proprietario.tipo").value("BANCO"));
        enviar(get("/api/automoveis/" + criarAutomovel(tokenDeEmpresa())), tokenDeNovoCliente(), null)
                .andExpect(jsonPath("$.proprietario.tipo").value("EMPRESA"));
    }

    @Test
    void somenteOProprietarioAlteraOuExcluiOAutomovel() throws Exception {
        long id = criarAutomovel(tokenDeNovoCliente());
        String outro = tokenDeNovoCliente();
        enviar(put("/api/automoveis/" + id), outro,
                "{\"marca\":\"X\",\"modelo\":\"Y\",\"ano\":2020,\"placa\":\"P\",\"matricula\":\"M\"}")
                .andExpect(status().isForbidden());
        enviar(delete("/api/automoveis/" + id), outro, null).andExpect(status().isForbidden());
    }

    @Test
    void automovelExigeCamposObrigatorios() throws Exception {
        enviar(post("/api/automoveis"), tokenDeNovoCliente(), "{\"marca\":\"So marca\"}")
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- pedidos

    @Test
    void clienteVeSomenteOsProprioPedidosEAgenteVeTodos() throws Exception {
        String dono = tokenDeNovoCliente();
        String outro = tokenDeNovoCliente();
        long carro = criarAutomovel(dono);
        long pedido = criarPedido(dono, carro);

        enviar(get("/api/pedidos"), dono, null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        enviar(get("/api/pedidos"), outro, null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        enviar(get("/api/pedidos/" + pedido), outro, null).andExpect(status().isForbidden());
        enviar(get("/api/pedidos/" + pedido), tokenDeBanco(), null).andExpect(status().isOk());
        enviar(get("/api/pedidos?status=PENDENTE"), tokenDeBanco(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id==" + pedido + ")]", hasSize(1)));
    }

    @Test
    void agenteNaoCriaPedidoECriacaoNaoAceitaAutomovelInexistente() throws Exception {
        long carro = criarAutomovel(tokenDeNovoCliente());
        enviar(post("/api/pedidos"), tokenDeBanco(), "{\"automovel\":{\"id\":" + carro + "}}")
                .andExpect(status().isForbidden());
        enviar(post("/api/pedidos"), tokenDeNovoCliente(), "{\"automovel\":{\"id\":999999}}")
                .andExpect(status().isNotFound());
    }

    @Test
    void clienteEAgenteModificamPedidoPendenteMasOutroClienteNao() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, criarAutomovel(dono));

        enviar(put("/api/pedidos/" + pedido), dono, "{\"observacao\":\"do cliente\",\"dataInicio\":\"2026-12-02\",\"dataFim\":\"2026-12-12\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.observacao").value("do cliente"));
        enviar(put("/api/pedidos/" + pedido), tokenDeEmpresa(), "{\"observacao\":\"do agente\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.observacao").value("do agente"));
        enviar(put("/api/pedidos/" + pedido), tokenDeNovoCliente(), "{\"observacao\":\"intruso\"}")
                .andExpect(status().isForbidden());
        enviar(put("/api/pedidos/" + pedido), dono, "{\"dataInicio\":\"2026-12-10\",\"dataFim\":\"2026-12-01\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void clienteCancelaPedidoEDepoisNaoPodeMaisModificar() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, criarAutomovel(dono));

        enviar(put("/api/pedidos/" + pedido + "/cancelar"), tokenDeNovoCliente(), null).andExpect(status().isForbidden());
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), tokenDeBanco(), null).andExpect(status().isForbidden());
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), dono, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        enviar(put("/api/pedidos/" + pedido), dono, "{\"observacao\":\"x\"}").andExpect(status().isConflict());
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), dono, null).andExpect(status().isConflict());
    }

    @Test
    void somenteAgenteAvaliaPedido() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, criarAutomovel(dono));

        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=POSITIVO"), dono, null).andExpect(status().isForbidden());
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=TALVEZ"), tokenDeBanco(), null).andExpect(status().isBadRequest());
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=NEGATIVO"), tokenDeBanco(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADO"));
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=POSITIVO"), tokenDeBanco(), null)
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- contratos

    @Test
    void fluxoCompletoBancoAprovaEGeraContratoDeCredito() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, criarAutomovel(dono));
        String banco = tokenDeBanco();

        // contrato antes da aprovação não é permitido
        enviar(post("/api/contratos/gerar/" + pedido), banco, "{}").andExpect(status().isConflict());

        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=POSITIVO"), banco, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.agenteAvaliador.tipo").value("BANCO"));

        // cliente não gera contrato
        enviar(post("/api/contratos/gerar/" + pedido), dono, "{}").andExpect(status().isForbidden());

        ResultActions contrato = enviar(post("/api/contratos/gerar/" + pedido), banco,
                "{\"valorFinanciado\":38900,\"taxaJuros\":8.75}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorFinanciado").value(38900.0))
                .andExpect(jsonPath("$.taxaJuros").value(8.75))
                .andExpect(jsonPath("$.banco.tipo").value("BANCO"))
                .andExpect(jsonPath("$.pedidoAluguel.id").value(pedido))
                .andExpect(jsonPath("$.pedidoAluguel.contrato").doesNotExist());
        long contratoId = lerId(contrato);

        // segundo contrato para o mesmo pedido
        enviar(post("/api/contratos/gerar/" + pedido), banco, "{}").andExpect(status().isConflict());

        // pedido aprovado COM contrato não pode mais ser cancelado
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), dono, null).andExpect(status().isConflict());

        // dono e agentes enxergam o contrato; outro cliente não
        enviar(get("/api/contratos/" + contratoId), dono, null).andExpect(status().isOk());
        enviar(get("/api/contratos/" + contratoId), tokenDeEmpresa(), null).andExpect(status().isOk());
        enviar(get("/api/contratos/" + contratoId), tokenDeNovoCliente(), null).andExpect(status().isForbidden());
        enviar(get("/api/contratos"), dono, null).andExpect(jsonPath("$", hasSize(1)));
        enviar(get("/api/contratos"), tokenDeNovoCliente(), null).andExpect(jsonPath("$", hasSize(0)));

        // a listagem de pedidos serializa o contrato sem recursão infinita
        enviar(get("/api/pedidos/" + pedido), dono, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contrato.id").value(contratoId))
                .andExpect(jsonPath("$.contrato.pedidoAluguel").doesNotExist());
    }

    @Test
    void empresaGeraContratoSimplesMasNaoConcedeCredito() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, criarAutomovel(dono));
        String empresa = tokenDeEmpresa();
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=POSITIVO"), empresa, null).andExpect(status().isOk());

        enviar(post("/api/contratos/gerar/" + pedido), empresa, "{\"valorFinanciado\":1000,\"taxaJuros\":5}")
                .andExpect(status().isForbidden());
        enviar(post("/api/contratos/gerar/" + pedido), empresa, "{}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorFinanciado").doesNotExist());
    }

    @Test
    void pedidoAprovadoSemContratoAindaPodeSerCancelado() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, criarAutomovel(dono));
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=POSITIVO"), tokenDeBanco(), null).andExpect(status().isOk());
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), dono, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    // ---------------------------------------------------------------- integridade

    @Test
    void naoExcluiAutomovelNemClienteQuePossuemPedidos() throws Exception {
        Object[] c = cadastrarCliente();
        String dono = login((String) c[0], SENHA);
        long carro = criarAutomovel(dono);
        criarPedido(dono, carro);

        enviar(delete("/api/automoveis/" + carro), dono, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(not(containsString("null"))));
        enviar(delete("/api/clientes/" + c[1]), dono, null).andExpect(status().isConflict());
    }

    @Test
    void corpoInvalidoRetorna400() throws Exception {
        enviar(post("/api/automoveis"), tokenDeNovoCliente(), "{isto nao e json")
                .andExpect(status().isBadRequest());
    }
}
