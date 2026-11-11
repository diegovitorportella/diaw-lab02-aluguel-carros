package com.example.demo.service;

import com.example.demo.TestData;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.Banco;
import com.example.demo.model.Cliente;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteServiceTest {

    @Mock ClienteRepository repository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PedidoAluguelRepository pedidoRepository;
    @Mock AutomovelRepository automovelRepository;
    @Mock UsuarioService usuarioService;

    ClienteService service;

    @BeforeEach
    void setUp() {
        service = new ClienteService(repository, usuarioRepository, pedidoRepository, automovelRepository, usuarioService);
        when(repository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioService.codificar(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
    }

    /** Cliente com todos os dados válidos (uma fonte de renda). */
    private Cliente valido() {
        Cliente c = new Cliente();
        c.setLogin("ana");
        c.setSenha("senha123");
        c.setNome("Ana Souza");
        c.setCpf(TestData.cpf());
        c.setRg("MG1234567");
        c.setEndereco("Rua A, 10");
        c.setProfissao("Engenheira");
        c.setRendimentosAuferidos(List.of(5000.0));
        c.setEntidadesEmpregadoras(List.of("ACME"));
        return c;
    }

    private ValidacaoException falha(Cliente c) {
        return assertThrows(ValidacaoException.class, () -> service.salvar(c));
    }

    // ---------- cadastro válido ----------

    @Test
    void salvaClienteValidoComCpfERgNormalizados() {
        Cliente c = valido();
        c.setCpf("529.982.247-25");
        c.setRg("mg-12.345.678");

        Cliente salvo = service.salvar(c);

        assertEquals("52998224725", salvo.getCpf());
        assertEquals("MG12345678", salvo.getRg());
        assertEquals("hash:senha123", salvo.getSenha());
        assertNull(salvo.getId());
    }

    @Test
    void salvaClienteComTresFontesDeRenda() {
        Cliente c = valido();
        c.setRendimentosAuferidos(List.of(1000.0, 2000.0, 3000.0));
        c.setEntidadesEmpregadoras(List.of("A", "B", "C"));
        assertEquals(3, service.salvar(c).getRendimentosAuferidos().size());
    }

    @Test
    void ignoraIdInformadoNoCadastro() {
        Cliente c = valido();
        c.setId(99L);
        assertNull(service.salvar(c).getId());
    }

    // ---------- fontes de renda ----------

    @Test
    void rejeitaMaisDeTresRendimentos() {
        Cliente c = valido();
        c.setRendimentosAuferidos(List.of(1.0, 2.0, 3.0, 4.0));
        c.setEntidadesEmpregadoras(List.of("A", "B", "C", "D"));
        ValidacaoException e = falha(c);
        assertTrue(e.getMessage().contains("no máximo 3 rendimentos"));
        assertTrue(e.getCampos().containsKey("rendimentosAuferidos"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaMaisDeTresEmpregadores() {
        Cliente c = valido();
        c.setEntidadesEmpregadoras(List.of("A", "B", "C", "D"));
        assertTrue(falha(c).getCampos().containsKey("entidadesEmpregadoras"));
    }

    @Test
    void rejeitaRendimentoZeroNegativoOuNulo() {
        for (Double invalido : Arrays.asList(0.0, -5.0, null)) {
            Cliente c = valido();
            c.setRendimentosAuferidos(Arrays.asList(1000.0, invalido));
            c.setEntidadesEmpregadoras(List.of("A", "B"));
            assertTrue(falha(c).getCampos().containsKey("rendimentosAuferidos"), "rendimento " + invalido);
        }
    }

    @Test
    void exigeAoMenosUmaFonteDeRenda() {
        Cliente c = valido();
        c.setRendimentosAuferidos(List.of());
        c.setEntidadesEmpregadoras(List.of());
        assertTrue(falha(c).getCampos().get("rendimentosAuferidos").contains("ao menos uma"));

        Cliente semListas = valido();
        semListas.setRendimentosAuferidos(null);
        semListas.setEntidadesEmpregadoras(null);
        assertTrue(falha(semListas).getCampos().containsKey("rendimentosAuferidos"));
    }

    @Test
    void exigeUmEmpregadorParaCadaRendimento() {
        Cliente c = valido();
        c.setRendimentosAuferidos(List.of(1000.0, 2000.0));
        c.setEntidadesEmpregadoras(List.of("A"));
        assertTrue(falha(c).getCampos().containsKey("entidadesEmpregadoras"));
    }

    @Test
    void exigeNomeDoEmpregador() {
        Cliente c = valido();
        c.setEntidadesEmpregadoras(List.of("  "));
        assertTrue(falha(c).getCampos().containsKey("entidadesEmpregadoras"));
    }

    // ---------- documentos e dados pessoais ----------

    @Test
    void rejeitaCpfInvalido() {
        Cliente c = valido();
        c.setCpf("111.111.111-11");
        assertEquals("CPF inválido.", falha(c).getCampos().get("cpf"));
    }

    @Test
    void rejeitaCpfDuplicado() {
        Cliente c = valido();
        when(repository.existsByCpf(c.getCpf())).thenReturn(true);
        assertTrue(falha(c).getCampos().get("cpf").contains("Já existe"));
    }

    @Test
    void exigeDadosPessoaisObrigatorios() {
        Cliente c = new Cliente();
        c.setLogin("ana");
        c.setSenha("senha123");
        ValidacaoException e = falha(c);
        assertTrue(e.getCampos().keySet().containsAll(
                List.of("nome", "cpf", "rg", "endereco", "profissao", "rendimentosAuferidos")));
    }

    @Test
    void rejeitaRgInvalido() {
        Cliente c = valido();
        c.setRg("12");
        assertTrue(falha(c).getCampos().containsKey("rg"));
    }

    // ---------- login e senha ----------

    @Test
    void rejeitaSenhaComMenosDeOitoCaracteres() {
        Cliente c = valido();
        c.setSenha("1234567");
        assertTrue(falha(c).getCampos().containsKey("senha"));
        c.setSenha("12345678");
        assertDoesNotThrow(() -> service.salvar(c));
    }

    @Test
    void rejeitaLoginDuplicadoOuVazio() {
        when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(new Cliente()));
        assertTrue(falha(valido()).getCampos().containsKey("login"));

        Cliente semLogin = valido();
        semLogin.setLogin(" ");
        assertTrue(falha(semLogin).getCampos().containsKey("login"));
    }

    @Test
    void validacaoEhSubclasseDeIllegalArgument() {
        Cliente c = valido();
        c.setCpf("000");
        assertThrows(IllegalArgumentException.class, () -> service.salvar(c));
    }

    // ---------- permissões e atualização ----------

    @Test
    void clienteNaoAcessaDadosDeOutroCliente() {
        Usuario outro = new Cliente();
        outro.setId(2L);
        when(usuarioService.buscar(2L)).thenReturn(outro);
        assertThrows(AcessoNegadoException.class, () -> service.buscar(1L, 2L));
    }

    @Test
    void agenteAcessaDadosDeQualquerCliente() {
        Banco banco = new Banco();
        banco.setId(3L);
        when(usuarioService.buscar(3L)).thenReturn(banco);
        Cliente c = new Cliente();
        c.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        assertSame(c, service.buscar(1L, 3L));
    }

    @Test
    void naoExcluiClienteComPedidosNemComAutomoveis() {
        Cliente c = new Cliente();
        c.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(c));

        when(pedidoRepository.existsByClienteId(1L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.excluir(1L, 1L));

        when(pedidoRepository.existsByClienteId(1L)).thenReturn(false);
        when(automovelRepository.existsByProprietarioId(1L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.excluir(1L, 1L));
        verify(repository, never()).delete(any());

        when(automovelRepository.existsByProprietarioId(1L)).thenReturn(false);
        service.excluir(1L, 1L);
        verify(repository).delete(c);
    }

    @Test
    void atualizaMantendoLoginESenhaQuandoNaoInformada() {
        Cliente existente = valido();
        existente.setId(1L);
        existente.setSenha("hash:antiga");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        Cliente dados = valido();
        dados.setLogin("outro-login");
        dados.setSenha("");
        dados.setNome("Ana Maria");
        Cliente atualizado = service.atualizar(1L, 1L, dados);

        assertEquals("ana", atualizado.getLogin());
        assertEquals("Ana Maria", atualizado.getNome());
        assertEquals("hash:antiga", atualizado.getSenha());
    }

    @Test
    void atualizaComNovaSenhaEValidaOsDadosNovamente() {
        Cliente existente = valido();
        existente.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        Cliente comSenha = valido();
        comSenha.setSenha("nova-senha-123");
        assertEquals("hash:nova-senha-123", service.atualizar(1L, 1L, comSenha).getSenha());

        Cliente senhaCurta = valido();
        senhaCurta.setSenha("123");
        assertThrows(ValidacaoException.class, () -> service.atualizar(1L, 1L, senhaCurta));

        Cliente rendimentosDemais = valido();
        rendimentosDemais.setRendimentosAuferidos(List.of(1.0, 2.0, 3.0, 4.0));
        rendimentosDemais.setEntidadesEmpregadoras(List.of("A", "B", "C", "D"));
        assertThrows(ValidacaoException.class, () -> service.atualizar(1L, 1L, rendimentosDemais));
    }

    @Test
    void atualizacaoChecaCpfDuplicadoIgnorandoOProprioCliente() {
        Cliente existente = valido();
        existente.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        Cliente dados = valido();
        String cpfDigitos = dados.getCpf();
        when(repository.existsByCpfAndIdNot(cpfDigitos, 1L)).thenReturn(true);
        assertTrue(assertThrows(ValidacaoException.class, () -> service.atualizar(1L, 1L, dados))
                .getCampos().containsKey("cpf"));
    }
}
