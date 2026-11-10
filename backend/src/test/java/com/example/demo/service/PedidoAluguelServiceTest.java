package com.example.demo.service;

import com.example.demo.dto.IdRefDTO;
import com.example.demo.dto.PedidoDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.model.Automovel;
import com.example.demo.model.Banco;
import com.example.demo.model.Cliente;
import com.example.demo.model.Contrato;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoAluguelServiceTest {

    @Mock PedidoAluguelRepository repository;
    @Mock AutomovelRepository automovelRepository;
    @Mock UsuarioService usuarioService;

    PedidoAluguelService service;
    Cliente dono;
    Cliente outro;
    Banco banco;
    PedidoAluguel pedido;

    @BeforeEach
    void setUp() {
        service = new PedidoAluguelService(repository, automovelRepository, usuarioService);

        dono = new Cliente();
        dono.setId(1L);
        outro = new Cliente();
        outro.setId(2L);
        banco = new Banco();
        banco.setId(3L);

        pedido = new PedidoAluguel();
        pedido.setId(10L);
        pedido.setCliente(dono);
        pedido.setStatus(PedidoAluguelService.PENDENTE);

        when(usuarioService.buscar(1L)).thenReturn(dono);
        when(usuarioService.buscar(2L)).thenReturn(outro);
        when(usuarioService.buscar(3L)).thenReturn(banco);
        when(repository.findById(10L)).thenReturn(Optional.of(pedido));
        when(repository.save(any(PedidoAluguel.class))).thenAnswer(i -> i.getArgument(0));
    }

    private PedidoDTO dto(Long automovelId, LocalDate inicio, LocalDate fim) {
        return new PedidoDTO(automovelId == null ? null : new IdRefDTO(automovelId), inicio, fim, "obs");
    }

    // ---------- criar ----------

    @Test
    void criaPedidoPendenteParaOClienteAutenticado() {
        Automovel carro = new Automovel();
        carro.setId(5L);
        when(automovelRepository.findById(5L)).thenReturn(Optional.of(carro));

        PedidoAluguel criado = service.criar(1L, dto(5L, null, null));

        assertEquals("PENDENTE", criado.getStatus());
        assertSame(dono, criado.getCliente());
        assertSame(carro, criado.getAutomovel());
    }

    @Test
    void agenteNaoCriaPedido() {
        assertThrows(AcessoNegadoException.class, () -> service.criar(3L, dto(5L, null, null)));
    }

    @Test
    void exigeAutomovelNoPedido() {
        assertThrows(IllegalArgumentException.class, () -> service.criar(1L, dto(null, null, null)));
    }

    @Test
    void rejeitaPeriodoInvertido() {
        Automovel carro = new Automovel();
        when(automovelRepository.findById(5L)).thenReturn(Optional.of(carro));
        assertThrows(IllegalArgumentException.class,
                () -> service.criar(1L, dto(5L, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1))));
    }

    // ---------- listar ----------

    @Test
    void clienteListaSomenteOsProprios() {
        when(repository.findByClienteId(1L)).thenReturn(List.of(pedido));
        assertEquals(1, service.listarPara(1L, null).size());
        assertEquals(0, service.listarPara(1L, "APROVADO").size());
    }

    @Test
    void agenteListaTodosEFiltraPorStatus() {
        PedidoAluguel aprovado = new PedidoAluguel();
        aprovado.setStatus("APROVADO");
        when(repository.findAll()).thenReturn(List.of(pedido, aprovado));
        assertEquals(2, service.listarPara(3L, null).size());
        assertEquals(1, service.listarPara(3L, "pendente").size());
    }

    // ---------- modificar ----------

    @Test
    void donoModificaPedidoPendente() {
        PedidoAluguel alterado = service.modificar(10L, 1L, dto(null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5)));
        assertEquals(LocalDate.of(2026, 12, 5), alterado.getDataFim());
        assertEquals("obs", alterado.getObservacao());
    }

    @Test
    void agenteModificaPedidoPendente() {
        assertDoesNotThrow(() -> service.modificar(10L, 3L, dto(null, null, null)));
    }

    @Test
    void outroClienteNaoModificaPedidoAlheio() {
        assertThrows(AcessoNegadoException.class, () -> service.modificar(10L, 2L, dto(null, null, null)));
    }

    @Test
    void naoModificaPedidoJaAvaliado() {
        pedido.setStatus(PedidoAluguelService.APROVADO);
        assertThrows(IllegalStateException.class, () -> service.modificar(10L, 1L, dto(null, null, null)));
    }

    // ---------- cancelar ----------

    @Test
    void donoCancelaPedidoPendente() {
        assertEquals("CANCELADO", service.cancelar(10L, 1L).getStatus());
    }

    @Test
    void donoCancelaPedidoAprovadoSemContrato() {
        pedido.setStatus(PedidoAluguelService.APROVADO);
        assertEquals("CANCELADO", service.cancelar(10L, 1L).getStatus());
    }

    @Test
    void naoCancelaPedidoAprovadoComContrato() {
        pedido.setStatus(PedidoAluguelService.APROVADO);
        pedido.setContrato(new Contrato());
        assertThrows(IllegalStateException.class, () -> service.cancelar(10L, 1L));
    }

    @Test
    void naoCancelaPedidoRejeitadoOuJaCancelado() {
        pedido.setStatus(PedidoAluguelService.REJEITADO);
        assertThrows(IllegalStateException.class, () -> service.cancelar(10L, 1L));
        pedido.setStatus(PedidoAluguelService.CANCELADO);
        assertThrows(IllegalStateException.class, () -> service.cancelar(10L, 1L));
    }

    @Test
    void somenteODonoCancela() {
        assertThrows(AcessoNegadoException.class, () -> service.cancelar(10L, 2L));
        assertThrows(AcessoNegadoException.class, () -> service.cancelar(10L, 3L));
    }

    // ---------- avaliar ----------

    @Test
    void parecerPositivoAprovaERegistraAgente() {
        PedidoAluguel avaliado = service.avaliarPedido(10L, "positivo", 3L);
        assertEquals("APROVADO", avaliado.getStatus());
        assertEquals("POSITIVO", avaliado.getParecerFinanceiro());
        assertSame(banco, avaliado.getAgenteAvaliador());
    }

    @Test
    void parecerNegativoRejeita() {
        assertEquals("REJEITADO", service.avaliarPedido(10L, "NEGATIVO", 3L).getStatus());
    }

    @Test
    void clienteNaoAvalia() {
        assertThrows(AcessoNegadoException.class, () -> service.avaliarPedido(10L, "POSITIVO", 1L));
    }

    @Test
    void naoAvaliaDuasVezesNemComParecerInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.avaliarPedido(10L, "TALVEZ", 3L));
        pedido.setStatus(PedidoAluguelService.APROVADO);
        assertThrows(IllegalStateException.class, () -> service.avaliarPedido(10L, "POSITIVO", 3L));
    }
}
