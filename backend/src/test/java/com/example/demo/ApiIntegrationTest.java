package com.example.demo;

import com.example.demo.model.Banco;
import com.example.demo.model.Empresa;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.SimulacaoService;
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

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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

    private String dia(int deslocamento) {
        return LocalDate.now().plusDays(deslocamento).toString();
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

    private String clienteJson(String login, String cpf, int renda) {
        return "{\"login\":\"" + login + "\",\"senha\":\"" + SENHA + "\",\"nome\":\"Fulano de Tal\",\"cpf\":\"" + cpf
                + "\",\"rg\":\"MG1234567\",\"endereco\":\"Rua A, 10\",\"profissao\":\"Dev\","
                + "\"rendimentosAuferidos\":[" + renda + "],\"entidadesEmpregadoras\":[\"ACME\"]}";
    }

    /** Cadastra um cliente pela rota pública e devolve {login, id, cpf}. */
    private Object[] cadastrarCliente(int renda) throws Exception {
        String login = unico("cliente");
        String cpf = TestData.cpf();
        ResultActions r = enviar(post("/api/clientes"), null, clienteJson(login, cpf, renda)).andExpect(status().isOk());
        return new Object[]{login, lerId(r), cpf};
    }

    private Object[] cadastrarCliente() throws Exception {
        return cadastrarCliente(5000);
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

    private String automovelJson(String placa) {
        return "{\"marca\":\"Hyundai\",\"modelo\":\"Ioniq 6\",\"ano\":2025,\"placa\":\"" + placa
                + "\",\"matricula\":\"M-1\",\"valorDiaria\":100}";
    }

    private long criarAutomovel(String token) throws Exception {
        return lerId(enviar(post("/api/automoveis"), token, automovelJson(TestData.placa())).andExpect(status().isOk()));
    }

    /** Veículo de uma empresa (o cliente não pode alugar o próprio). */
    private long carroDeEmpresa() throws Exception {
        return criarAutomovel(tokenDeEmpresa());
    }

    /** Cria um pedido de 5 diárias (R$ 100,00 cada = R$ 500,00). */
    private long criarPedido(String tokenCliente, long automovelId) throws Exception {
        ResultActions r = enviar(post("/api/pedidos"), tokenCliente,
                "{\"automovel\":{\"id\":" + automovelId + "},\"dataInicio\":\"" + dia(1) + "\",\"dataFim\":\"" + dia(5) + "\"}")
                .andExpect(status().isOk());
        return lerId(r);
    }

    private void aprovar(long pedido, String tokenAgente) throws Exception {
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=POSITIVO"), tokenAgente, null).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- autenticação

    @Test
    void rotasProtegidasExigemToken() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/automoveis")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/contratos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pedidos/1/simulacao?parcelas=12&juros=5")).andExpect(status().isUnauthorized());
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
        String corpo = clienteJson(unico("x"), TestData.cpf(), 1000)
                .replace("[1000]", "[1,2,3,4]").replace("[\"ACME\"]", "[\"A\",\"B\",\"C\",\"D\"]");
        enviar(post("/api/clientes"), null, corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("no máximo 3 rendimentos")))
                .andExpect(jsonPath("$.campos.rendimentosAuferidos").value(containsString("no máximo 3")));
    }

    @Test
    void cadastroDevolveErroPorCampo() throws Exception {
        enviar(post("/api/clientes"), null, "{\"login\":\"" + unico("v") + "\",\"senha\":\"123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senha").exists())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.cpf").exists())
                .andExpect(jsonPath("$.campos.rg").exists())
                .andExpect(jsonPath("$.campos.endereco").exists())
                .andExpect(jsonPath("$.campos.profissao").exists())
                .andExpect(jsonPath("$.campos.rendimentosAuferidos").exists());
    }

    @Test
    void cadastroRejeitaCpfInvalido() throws Exception {
        enviar(post("/api/clientes"), null, clienteJson(unico("c"), "111.111.111-11", 1000))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpf").value("CPF inválido."));
    }

    @Test
    void cadastroRejeitaCpfELoginDuplicados() throws Exception {
        Object[] c = cadastrarCliente();
        enviar(post("/api/clientes"), null, clienteJson((String) c[0], TestData.cpf(), 1000))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.login").exists());
        enviar(post("/api/clientes"), null, clienteJson(unico("outro"), (String) c[2], 1000))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.cpf").exists());
    }

    @Test
    void cadastroNormalizaOCpfParaSomenteDigitos() throws Exception {
        String cpf = TestData.cpf();
        String mascarado = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        String login = unico("m");
        long id = lerId(enviar(post("/api/clientes"), null, clienteJson(login, mascarado, 1000)).andExpect(status().isOk()));
        enviar(get("/api/clientes/" + id), login(login, SENHA), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.cpf").value(cpf));
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

        String novo = clienteJson((String) a[0], (String) a[2], 1000)
                .replace("Fulano de Tal", "Novo Nome").replace("[1000]", "[1000,2000]").replace("[\"ACME\"]", "[\"A\",\"B\"]")
                .replace("\"senha\":\"" + SENHA + "\",", "");
        enviar(put("/api/clientes/" + a[1]), tokenA, novo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"))
                .andExpect(jsonPath("$.rendimentosAuferidos", hasSize(2)));
        login((String) a[0], SENHA); // a senha antiga continua valendo

        enviar(put("/api/clientes/" + a[1]), tokenA, novo.replace("[1000,2000]", "[1,2,3,4]").replace("[\"A\",\"B\"]", "[\"A\",\"B\",\"C\",\"D\"]"))
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
        String placa = TestData.placa();
        String digitada = (placa.substring(0, 3) + "-" + placa.substring(3)).toLowerCase(); // ex.: abc-1234
        long id = lerId(enviar(post("/api/automoveis"), token, automovelJson(digitada))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorDiaria").value(100.0)));

        enviar(get("/api/automoveis/" + id), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca").value("Hyundai"))
                .andExpect(jsonPath("$.placa").value(placa)) // normalizada: maiúsculas e sem hífen
                .andExpect(jsonPath("$.proprietario.tipo").value("CLIENTE"));

        enviar(get("/api/automoveis/meus"), token, null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        String placaNova = TestData.placa();
        enviar(put("/api/automoveis/" + id), token,
                "{\"marca\":\"Kia\",\"modelo\":\"EV6\",\"ano\":2024,\"placa\":\"" + placaNova + "\",\"matricula\":\"M-2\",\"valorDiaria\":250.5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca").value("Kia"))
                .andExpect(jsonPath("$.valorDiaria").value(250.5));

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
        enviar(put("/api/automoveis/" + id), outro, automovelJson(TestData.placa())).andExpect(status().isForbidden());
        enviar(delete("/api/automoveis/" + id), outro, null).andExpect(status().isForbidden());
    }

    @Test
    void automovelValidaCamposObrigatoriosPlacaEDiaria() throws Exception {
        String token = tokenDeNovoCliente();
        enviar(post("/api/automoveis"), token, "{\"marca\":\"So marca\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.modelo").exists())
                .andExpect(jsonPath("$.campos.placa").exists())
                .andExpect(jsonPath("$.campos.ano").exists())
                .andExpect(jsonPath("$.campos.valorDiaria").exists());
        enviar(post("/api/automoveis"), token, automovelJson("XX-99"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.placa").value(containsString("inválida")));
        enviar(post("/api/automoveis"), token, automovelJson("ABC1D23").replace("\"valorDiaria\":100", "\"valorDiaria\":0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.valorDiaria").exists());
    }

    @Test
    void placaNaoPodeSerRepetida() throws Exception {
        String token = tokenDeNovoCliente();
        String placa = TestData.placa();
        enviar(post("/api/automoveis"), token, automovelJson(placa)).andExpect(status().isOk());
        String mascarada = placa.substring(0, 3) + "-" + placa.substring(3);
        enviar(post("/api/automoveis"), tokenDeNovoCliente(), automovelJson(mascarada))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.placa").value(containsString("Já existe")));
    }

    // ---------------------------------------------------------------- pedidos

    @Test
    void clienteVeSomenteOsProprioPedidosEAgenteVeTodos() throws Exception {
        String dono = tokenDeNovoCliente();
        String outro = tokenDeNovoCliente();
        long pedido = criarPedido(dono, carroDeEmpresa());

        enviar(get("/api/pedidos"), dono, null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        enviar(get("/api/pedidos"), outro, null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        enviar(get("/api/pedidos/" + pedido), outro, null).andExpect(status().isForbidden());
        enviar(get("/api/pedidos/" + pedido), tokenDeBanco(), null).andExpect(status().isOk());
        enviar(get("/api/pedidos?status=PENDENTE"), tokenDeBanco(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id==" + pedido + ")]", hasSize(1)));
    }

    @Test
    void agenteNaoCriaPedidoECriacaoNaoAceitaAutomovelInexistente() throws Exception {
        long carro = carroDeEmpresa();
        String corpo = "{\"automovel\":{\"id\":" + carro + "},\"dataInicio\":\"" + dia(1) + "\",\"dataFim\":\"" + dia(2) + "\"}";
        enviar(post("/api/pedidos"), tokenDeBanco(), corpo).andExpect(status().isForbidden());
        enviar(post("/api/pedidos"), tokenDeNovoCliente(),
                "{\"automovel\":{\"id\":999999},\"dataInicio\":\"" + dia(1) + "\",\"dataFim\":\"" + dia(2) + "\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void clienteNaoAlugaOProprioVeiculo() throws Exception {
        String cliente = tokenDeNovoCliente();
        long meuCarro = criarAutomovel(cliente);
        enviar(post("/api/pedidos"), cliente,
                "{\"automovel\":{\"id\":" + meuCarro + "},\"dataInicio\":\"" + dia(1) + "\",\"dataFim\":\"" + dia(2) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.automovel").value("Você não pode alugar o próprio veículo."));
    }

    @Test
    void pedidoValidaAutomovelEDatas() throws Exception {
        String cliente = tokenDeNovoCliente();
        enviar(post("/api/pedidos"), cliente, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.automovel").exists())
                .andExpect(jsonPath("$.campos.dataInicio").exists())
                .andExpect(jsonPath("$.campos.dataFim").exists());

        long carro = carroDeEmpresa();
        enviar(post("/api/pedidos"), cliente,
                "{\"automovel\":{\"id\":" + carro + "},\"dataInicio\":\"" + dia(-2) + "\",\"dataFim\":\"" + dia(3) + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.dataInicio").exists());
        enviar(post("/api/pedidos"), cliente,
                "{\"automovel\":{\"id\":" + carro + "},\"dataInicio\":\"" + dia(5) + "\",\"dataFim\":\"" + dia(2) + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.dataFim").exists());
    }

    @Test
    void pedidoCalculaValorTotalERegistraHistorico() throws Exception {
        String cliente = tokenDeNovoCliente();
        long pedido = criarPedido(cliente, carroDeEmpresa());

        enviar(get("/api/pedidos/" + pedido), cliente, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.valorTotal").value(500.0)) // 5 diárias x R$ 100
                .andExpect(jsonPath("$.historico", hasSize(1)))
                .andExpect(jsonPath("$.historico[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.historico[0].responsavelTipo").value("CLIENTE"))
                .andExpect(jsonPath("$.historico[0].dataHora").isNotEmpty());

        // trocar o período recalcula o total e acrescenta um evento
        enviar(put("/api/pedidos/" + pedido), cliente, "{\"dataInicio\":\"" + dia(1) + "\",\"dataFim\":\"" + dia(2) + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(200.0))
                .andExpect(jsonPath("$.historico", hasSize(2)))
                .andExpect(jsonPath("$.historico[1].descricao").value("Pedido modificado"));
    }

    @Test
    void clienteEAgenteModificamPedidoPendenteMasOutroClienteNao() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, carroDeEmpresa());

        enviar(put("/api/pedidos/" + pedido), dono, "{\"observacao\":\"do cliente\",\"dataInicio\":\"" + dia(2) + "\",\"dataFim\":\"" + dia(6) + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.observacao").value("do cliente"));
        enviar(put("/api/pedidos/" + pedido), tokenDeEmpresa(), "{\"observacao\":\"do agente\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.observacao").value("do agente"))
                .andExpect(jsonPath("$.historico[2].responsavelTipo").value("EMPRESA"));
        // agente não altera datas
        enviar(put("/api/pedidos/" + pedido), tokenDeEmpresa(), "{\"dataInicio\":\"" + dia(3) + "\"}")
                .andExpect(status().isForbidden());
        enviar(put("/api/pedidos/" + pedido), tokenDeNovoCliente(), "{\"observacao\":\"intruso\"}")
                .andExpect(status().isForbidden());
        enviar(put("/api/pedidos/" + pedido), dono, "{\"dataInicio\":\"" + dia(10) + "\",\"dataFim\":\"" + dia(1) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void clienteCancelaPedidoEDepoisNaoPodeMaisModificar() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, carroDeEmpresa());

        enviar(put("/api/pedidos/" + pedido + "/cancelar"), tokenDeNovoCliente(), null).andExpect(status().isForbidden());
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), tokenDeBanco(), null).andExpect(status().isForbidden());
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), dono, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.historico[1].status").value("CANCELADO"));
        enviar(put("/api/pedidos/" + pedido), dono, "{\"observacao\":\"x\"}").andExpect(status().isConflict());
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), dono, null).andExpect(status().isConflict());
    }

    @Test
    void somenteAgenteAvaliaPedido() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, carroDeEmpresa());

        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=POSITIVO"), dono, null).andExpect(status().isForbidden());
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=TALVEZ"), tokenDeBanco(), null).andExpect(status().isBadRequest());
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=NEGATIVO"), tokenDeBanco(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADO"));
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=POSITIVO"), tokenDeBanco(), null)
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- simulação

    @Test
    void agenteSimulaOFinanciamentoComAlertaDeRenda() throws Exception {
        String dono = tokenDeNovoCliente(); // renda 5.000
        long pedido = criarPedido(dono, carroDeEmpresa()); // total 500
        String banco = tokenDeBanco();
        double parcelaEsperada = SimulacaoService.parcela(400, 12, 12);

        enviar(get("/api/pedidos/" + pedido + "/simulacao?parcelas=12&juros=12&entrada=100"), banco, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(500.0))
                .andExpect(jsonPath("$.valorEntrada").value(100.0))
                .andExpect(jsonPath("$.valorFinanciado").value(400.0))
                .andExpect(jsonPath("$.valorParcela").value(parcelaEsperada))
                .andExpect(jsonPath("$.rendaTotal").value(5000.0))
                .andExpect(jsonPath("$.limiteComprometimento").value(0.3))
                .andExpect(jsonPath("$.acimaDoLimite").value(false));
    }

    @Test
    void simulacaoAlertaQuandoAParcelaPassaDe30PorCentoDaRenda() throws Exception {
        Object[] pobre = cadastrarCliente(100); // renda 100
        long pedido = criarPedido(login((String) pobre[0], SENHA), carroDeEmpresa()); // total 500
        enviar(get("/api/pedidos/" + pedido + "/simulacao?parcelas=2&juros=10"), tokenDeBanco(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.acimaDoLimite").value(true));
    }

    @Test
    void simulacaoEhSomenteParaAgentesEValidaParametros() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, carroDeEmpresa());
        String url = "/api/pedidos/" + pedido + "/simulacao?parcelas=12&juros=12";

        enviar(get(url), dono, null).andExpect(status().isForbidden());
        enviar(get("/api/pedidos/" + pedido + "/simulacao?parcelas=0&juros=12"), tokenDeEmpresa(), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.numeroParcelas").exists());
        enviar(get("/api/pedidos/999999/simulacao?parcelas=12&juros=12"), tokenDeBanco(), null).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- contratos e fluxo completo

    @Test
    void fluxoCompletoPendenteAprovadoConcluidoComCredito() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, carroDeEmpresa());
        String banco = tokenDeBanco();

        // antes da aprovação não há contrato
        enviar(post("/api/contratos/gerar/" + pedido), banco, "{}").andExpect(status().isConflict());

        // a aprovação NÃO gera contrato: o pedido fica aprovado, aguardando execução
        enviar(put("/api/pedidos/" + pedido + "/avaliar?parecer=POSITIVO"), banco, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.agenteAvaliador.tipo").value("BANCO"))
                .andExpect(jsonPath("$.contrato").doesNotExist());

        // cliente não executa contrato
        enviar(post("/api/contratos/gerar/" + pedido), dono, "{}").andExpect(status().isForbidden());

        // o banco executa com crédito: entrada 100, 12x, 12% a.a. => financia 400
        ResultActions contrato = enviar(post("/api/contratos/gerar/" + pedido), banco,
                "{\"valorEntrada\":100,\"numeroParcelas\":12,\"taxaJuros\":12}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorFinanciado").value(400.0))
                .andExpect(jsonPath("$.valorEntrada").value(100.0))
                .andExpect(jsonPath("$.numeroParcelas").value(12))
                .andExpect(jsonPath("$.taxaJuros").value(12.0))
                .andExpect(jsonPath("$.valorParcela").value(SimulacaoService.parcela(400, 12, 12)))
                .andExpect(jsonPath("$.banco.tipo").value("BANCO"))
                .andExpect(jsonPath("$.pedidoAluguel.id").value(pedido))
                .andExpect(jsonPath("$.pedidoAluguel.contrato").doesNotExist());
        long contratoId = lerId(contrato);

        // o pedido está CONCLUIDO e o histórico mostra as três etapas
        enviar(get("/api/pedidos/" + pedido), dono, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.historico", hasSize(3)))
                .andExpect(jsonPath("$.historico[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.historico[1].status").value("APROVADO"))
                .andExpect(jsonPath("$.historico[2].status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.contrato.id").value(contratoId))
                .andExpect(jsonPath("$.contrato.pedidoAluguel").doesNotExist());

        // não dá para repetir, cancelar nem modificar um pedido concluído
        enviar(post("/api/contratos/gerar/" + pedido), banco, "{}").andExpect(status().isConflict());
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), dono, null).andExpect(status().isConflict());
        enviar(put("/api/pedidos/" + pedido), dono, "{\"observacao\":\"x\"}").andExpect(status().isConflict());

        // dono e agentes enxergam o contrato; outro cliente não
        enviar(get("/api/contratos/" + contratoId), dono, null).andExpect(status().isOk());
        enviar(get("/api/contratos/" + contratoId), tokenDeEmpresa(), null).andExpect(status().isOk());
        enviar(get("/api/contratos/" + contratoId), tokenDeNovoCliente(), null).andExpect(status().isForbidden());
        enviar(get("/api/contratos"), dono, null).andExpect(jsonPath("$", hasSize(1)));
        enviar(get("/api/contratos"), tokenDeNovoCliente(), null).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void creditoValidaOsParametros() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, carroDeEmpresa());
        String banco = tokenDeBanco();
        aprovar(pedido, banco);

        enviar(post("/api/contratos/gerar/" + pedido), banco, "{\"numeroParcelas\":0,\"taxaJuros\":12}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.numeroParcelas").exists());
        enviar(post("/api/contratos/gerar/" + pedido), banco, "{\"valorFinanciado\":900,\"numeroParcelas\":12,\"taxaJuros\":12}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.valorFinanciado").exists()); // > total (500)
        enviar(get("/api/pedidos/" + pedido), dono, null).andExpect(jsonPath("$.status").value("APROVADO")); // nada mudou
    }

    @Test
    void empresaExecutaContratoSimplesMasNaoConcedeCredito() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, carroDeEmpresa());
        String empresa = tokenDeEmpresa();
        aprovar(pedido, empresa);

        enviar(post("/api/contratos/gerar/" + pedido), empresa, "{\"numeroParcelas\":12,\"taxaJuros\":5}")
                .andExpect(status().isForbidden());
        enviar(post("/api/contratos/gerar/" + pedido), empresa, "{}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorFinanciado").doesNotExist());
        enviar(get("/api/pedidos/" + pedido), dono, null).andExpect(jsonPath("$.status").value("CONCLUIDO"));
    }

    @Test
    void pedidoAprovadoSemContratoAindaPodeSerCancelado() throws Exception {
        String dono = tokenDeNovoCliente();
        long pedido = criarPedido(dono, carroDeEmpresa());
        aprovar(pedido, tokenDeBanco());
        enviar(put("/api/pedidos/" + pedido + "/cancelar"), dono, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    // ---------------------------------------------------------------- integridade

    @Test
    void naoExcluiAutomovelNemClienteQuePossuemPedidos() throws Exception {
        Object[] c = cadastrarCliente();
        String dono = login((String) c[0], SENHA);
        String empresa = tokenDeEmpresa();
        long carro = criarAutomovel(empresa);
        criarPedido(dono, carro);

        enviar(delete("/api/automoveis/" + carro), empresa, null)
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
