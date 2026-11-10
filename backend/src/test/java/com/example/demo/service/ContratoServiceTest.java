package com.example.demo.service;

import com.example.demo.dto.ContratoCreditoDTO;
import com.example.demo.exception.AcessoNegadoException;
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

        when(usuarioService.buscar(1L)).thenReturn(cliente);
        when(usuarioService.buscar(3L)).thenReturn(banco);
        when(usuarioService.buscar(4L)).thenReturn(empresa);
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void bancoGeraContratoDeCreditoVinculadoAEle() {
        Contrato contrato = service.gerarContrato(10L, 3L, new ContratoCreditoDTO(38900.0, 8.75));

        ContratoCredito credito = assertInstanceOf(ContratoCredito.class, contrato);
        assertEquals(38900.0, credito.getValorFinanciado());
        assertEquals(8.75, credito.getTaxaJuros());
        assertSame(banco, credito.getBanco());
        assertSame(pedido, credito.getPedidoAluguel());
        assertNotNull(credito.getDataAssinatura());
    }

    @Test
    void empresaGeraContratoSimplesSemCredito() {
        Contrato contrato = service.gerarContrato(10L, 4L, null);
        assertFalse(contrato instanceof ContratoCredito);
    }

    @Test
    void empresaNaoConcedeCredito() {
        assertThrows(AcessoNegadoException.class,
                () -> service.gerarContrato(10L, 4L, new ContratoCreditoDTO(1000.0, 5.0)));
    }

    @Test
    void clienteNaoGeraContrato() {
        assertThrows(AcessoNegadoException.class, () -> service.gerarContrato(10L, 1L, null));
    }

    @Test
    void somenteParaPedidoAprovado() {
        pedido.setStatus(PedidoAluguelService.PENDENTE);
        assertThrows(IllegalStateException.class, () -> service.gerarContrato(10L, 3L, null));
    }

    @Test
    void naoGeraSegundoContratoParaOMesmoPedido() {
        pedido.setContrato(new Contrato());
        assertThrows(IllegalStateException.class, () -> service.gerarContrato(10L, 3L, null));
    }

    @Test
    void validaValoresDoCredito() {
        assertThrows(IllegalArgumentException.class,
                () -> service.gerarContrato(10L, 3L, new ContratoCreditoDTO(-1.0, 5.0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.gerarContrato(10L, 3L, new ContratoCreditoDTO(1000.0, null)));
    }
}
