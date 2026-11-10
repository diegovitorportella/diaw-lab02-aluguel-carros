package com.example.demo.service;

import com.example.demo.exception.AcessoNegadoException;
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

    private Cliente cliente(String login, String senha, List<Double> rendimentos) {
        Cliente c = new Cliente();
        c.setLogin(login);
        c.setSenha(senha);
        c.setRendimentosAuferidos(rendimentos);
        return c;
    }

    @Test
    void salvaClienteComTresRendimentos() {
        Cliente salvo = service.salvar(cliente("ana", "senha123", List.of(1000.0, 2000.0, 3000.0)));
        assertEquals(3, salvo.getRendimentosAuferidos().size());
    }

    @Test
    void rejeitaMaisDeTresRendimentos() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.salvar(cliente("ana", "senha123", List.of(1.0, 2.0, 3.0, 4.0))));
        assertTrue(e.getMessage().contains("no máximo 3 rendimentos"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaMaisDeTresEmpregadores() {
        Cliente c = cliente("ana", "senha123", List.of(1.0));
        c.setEntidadesEmpregadoras(List.of("A", "B", "C", "D"));
        assertThrows(IllegalArgumentException.class, () -> service.salvar(c));
    }

    @Test
    void rejeitaRendimentoNegativoOuNulo() {
        assertThrows(IllegalArgumentException.class, () -> service.salvar(cliente("ana", "senha123", List.of(-5.0))));
        assertThrows(IllegalArgumentException.class,
                () -> service.salvar(cliente("ana", "senha123", java.util.Arrays.asList(1.0, null))));
    }

    @Test
    void rejeitaSenhaCurta() {
        assertThrows(IllegalArgumentException.class, () -> service.salvar(cliente("ana", "123", List.of())));
    }

    @Test
    void rejeitaLoginDuplicado() {
        when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(new Cliente()));
        assertThrows(IllegalArgumentException.class, () -> service.salvar(cliente("ana", "senha123", List.of())));
    }

    @Test
    void gravaSenhaComHashEIgnoraIdInformado() {
        Cliente c = cliente("ana", "senha123", List.of());
        c.setId(99L);
        Cliente salvo = service.salvar(c);
        assertEquals("hash:senha123", salvo.getSenha());
        assertNull(salvo.getId());
    }

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
    void naoExcluiClienteComPedidos() {
        Cliente c = new Cliente();
        c.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(pedidoRepository.existsByClienteId(1L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.excluir(1L, 1L));
        verify(repository, never()).delete(any());
    }

    @Test
    void atualizaMantendoLoginEValidandoRendimentos() {
        Cliente existente = cliente("ana", "hash:antiga", List.of(1.0));
        existente.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        Cliente dados = cliente("outro-login", "", List.of(1.0, 2.0));
        dados.setNome("Ana Maria");
        Cliente atualizado = service.atualizar(1L, 1L, dados);

        assertEquals("ana", atualizado.getLogin());
        assertEquals("Ana Maria", atualizado.getNome());
        assertEquals("hash:antiga", atualizado.getSenha()); // senha em branco não altera

        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(1L, 1L, cliente("x", null, List.of(1.0, 2.0, 3.0, 4.0))));
    }
}
