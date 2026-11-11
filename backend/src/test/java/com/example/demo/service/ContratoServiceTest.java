package com.example.demo.service;

import com.example.demo.dto.ContratoCreditoDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.Banco;
import com.example.demo.model.Cliente;
import com.example.demo.model.Contrato;
import com.example.demo.model.ContratoCredito;
import com.example.demo.model.Empresa;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.repository.ContratoRepository;
import com.example.demo.repository.PedidoAluguelRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContratoServiceTest {

    @Mock ContratoRepository contratoRepository;
    @Mock PedidoAluguelRepository pedidoRepository;
    @Mock UsuarioService usuarioService;

    ContratoService service;
    Banco banco;
    Empresa empresa;
    Cliente cliente;
    PedidoAluguel pedido;

    @BeforeEach
    void setUp() {
        service = new ContratoService(contratoRepository, pedidoRepository, usuarioService);

        banco = new Banco();
        banco.setId(3L);
        empresa = new Empresa();
        empresa.setId(4L);
        cliente = new Cliente();
        cliente.setId(1L);

        pedido = new PedidoAluguel();
        pedido.setId(10L);
        pedido.setCliente(cliente);
        pedido.setStatus(PedidoAluguelService.APROVADO);
        pedido.setValorTotal(1000.0);

        when(usuarioService.buscar(1L)).thenReturn(cliente);
        when(usuarioService.buscar(3L)).thenReturn(banco);
        when(usuarioService.buscar(4L)).thenReturn(empresa);
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(i -> i.getArgument(0));
        when(pedidoRepository.save(any(PedidoAluguel.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ContratoCreditoDTO credito(Double financiado, Double entrada, Integer parcelas, Double juros) {
        return new ContratoCreditoDTO(financiado, entrada, parcelas, juros);
    }

    // ---------- com crédito ----------

    @Test
    void bancoGeraCreditoDerivandoOFinanciadoDoTotalMenosEntradaEConcluiOPedido() {
        Contrato contrato = service.gerarContrato(10L, 3L, credito(null, 200.0, 12, 12.0));

        ContratoCredito c = assertInstanceOf(ContratoCredito.class, contrato);
        assertEquals(800.0, c.getValorFinanciado());
        assertEquals(200.0, c.getValorEntrada());
        assertEquals(12, c.getNumeroParcelas());
        assertEquals(12.0, c.getTaxaJuros());
        assertEquals(SimulacaoService.parcela(800, 12, 12), c.getValorParcela());
        assertSame(banco, c.getBanco());
        assertSame(pedido, c.getPedidoAluguel());
        assertNotNull(c.getDataAssinatura());

        assertEquals("CONCLUIDO", pedido.getStatus());
        assertSame(c, pedido.getContrato());
        assertEquals("CONCLUIDO", pedido.getHistorico().get(pedido.getHistorico().size() - 1).getStatus());
        assertEquals("BANCO", pedido.getHistorico().get(pedido.getHistorico().size() - 1).getResponsavelTipo());
    }

    @Test
    void aceitaValorFinanciadoInformadoPeloBanco() {
        ContratoCredito c = (ContratoCredito) service.gerarContrato(10L, 3L, credito(600.0, null, 6, 0.0));
        assertEquals(600.0, c.getValorFinanciado());
        assertEquals(0.0, c.getValorEntrada());
        assertEquals(100.0, c.getValorParcela(), 0.001);
    }

    @Test
    void creditoExigeParcelasEJurosValidos() {
        assertTrue(assertThrows(ValidacaoException.class,
                () -> service.gerarContrato(10L, 3L, credito(null, 0.0, 0, 5.0))).getCampos().containsKey("numeroParcelas"));
        assertTrue(assertThrows(ValidacaoException.class,
                () -> service.gerarContrato(10L, 3L, credito(null, 0.0, 12, null))).getCampos().containsKey("taxaJuros"));
        assertThrows(ValidacaoException.class, () -> service.gerarContrato(10L, 3L, credito(500.0, null, null, null)));
    }

    @Test
    void validaValoresDoFinanciamento() {
        assertThrows(ValidacaoException.class, () -> service.gerarContrato(10L, 3L, credito(null, -1.0, 12, 5.0)));
        assertThrows(ValidacaoException.class, () -> service.gerarContrato(10L, 3L, credito(-10.0, 0.0, 12, 5.0)));
        assertThrows(ValidacaoException.class, () -> service.gerarContrato(10L, 3L, credito(null, 1000.0, 12, 5.0))); // financiado 0
        assertThrows(ValidacaoException.class, () -> service.gerarContrato(10L, 3L, credito(1500.0, 0.0, 12, 5.0))); // > total
    }

    @Test
    void pedidoSemValorTotalExigeValorFinanciado() {
        pedido.setValorTotal(null);
        assertTrue(assertThrows(ValidacaoException.class,
                () -> service.gerarContrato(10L, 3L, credito(null, 0.0, 12, 5.0))).getCampos().containsKey("valorFinanciado"));
        assertDoesNotThrow(() -> service.gerarContrato(10L, 3L, credito(700.0, 0.0, 12, 5.0)));
    }

    @Test
    void empresaNaoConcedeCredito() {
        assertThrows(AcessoNegadoException.class, () -> service.gerarContrato(10L, 4L, credito(null, 0.0, 12, 5.0)));
        assertEquals("APROVADO", pedido.getStatus()); // nada mudou
    }

    // ---------- sem crédito ----------

    @Test
    void empresaOuBancoExecutamContratoSimplesEConcluemOPedido() {
        Contrato contrato = service.gerarContrato(10L, 4L, null);
        assertFalse(contrato instanceof ContratoCredito);
        assertEquals("CONCLUIDO", pedido.getStatus());

        pedido.setStatus(PedidoAluguelService.APROVADO);
        pedido.setContrato(null);
        assertFalse(service.gerarContrato(10L, 3L, credito(null, null, null, null)) instanceof ContratoCredito);
    }

    // ---------- regras do pedido ----------

    @Test
    void clienteNaoGeraContrato() {
        assertThrows(AcessoNegadoException.class, () -> service.gerarContrato(10L, 1L, null));
    }

    @Test
    void somenteParaPedidoAprovado() {
        for (String status : List.of("PENDENTE", "REJEITADO", "CANCELADO", "CONCLUIDO")) {
            pedido.setStatus(status);
            assertThrows(IllegalStateException.class, () -> service.gerarContrato(10L, 3L, null), status);
        }
    }

    @Test
    void naoGeraSegundoContratoParaOMesmoPedido() {
        pedido.setContrato(new Contrato());
        assertThrows(IllegalStateException.class, () -> service.gerarContrato(10L, 3L, null));
    }
}
