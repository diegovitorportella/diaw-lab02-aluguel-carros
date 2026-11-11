package com.example.demo.service;

import com.example.demo.dto.SimulacaoDTO;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.Cliente;
import com.example.demo.model.PedidoAluguel;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimulacaoServiceTest {

    @Mock PedidoAluguelRepository repository;

    SimulacaoService service;
    Cliente cliente;
    PedidoAluguel pedido;

    @BeforeEach
    void setUp() {
        service = new SimulacaoService(repository);
        cliente = new Cliente();
        cliente.setRendimentosAuferidos(List.of(3000.0, 2000.0)); // renda total 5000
        pedido = new PedidoAluguel();
        pedido.setId(10L);
        pedido.setCliente(cliente);
        pedido.setValorTotal(1000.0);
        when(repository.findById(10L)).thenReturn(Optional.of(pedido));
    }

    // ---------- fórmulas ----------

    @Test
    void taxaMensalEquivalenteATaxaAnual() {
        assertEquals(0.0, SimulacaoService.taxaMensal(0), 1e-12);
        // (1 + 0,12)^(1/12) - 1
        assertEquals(0.009488793, SimulacaoService.taxaMensal(12), 1e-8);
    }

    @Test
    void parcelaPelaTabelaPrice() {
        // PV=10.000, 12x, 12% a.a. => PMT = PV*i / (1 - (1+i)^-12) = 885,62
        assertEquals(885.62, SimulacaoService.parcela(10_000, 12, 12), 0.01);
    }

    @Test
    void semJurosAParcelaEOValorDividido() {
        assertEquals(250.0, SimulacaoService.parcela(1000, 4, 0), 0.001);
    }

    @Test
    void arredondaParaDuasCasas() {
        assertEquals(10.13, SimulacaoService.arredondar(10.125001), 0.0001);
        assertEquals(3.33, SimulacaoService.arredondar(3.3333), 0.0001);
    }

    // ---------- parâmetros ----------

    @Test
    void validaParcelasEJuros() {
        assertDoesNotThrow(() -> SimulacaoService.validarParametros(12, 8.75));
        assertDoesNotThrow(() -> SimulacaoService.validarParametros(1, 0.0));

        ValidacaoException e = assertThrows(ValidacaoException.class, () -> SimulacaoService.validarParametros(0, -1.0));
        assertTrue(e.getCampos().containsKey("numeroParcelas"));
        assertTrue(e.getCampos().containsKey("taxaJuros"));

        assertThrows(ValidacaoException.class, () -> SimulacaoService.validarParametros(121, 5.0));
        assertThrows(ValidacaoException.class, () -> SimulacaoService.validarParametros(12, 201.0));
        assertThrows(ValidacaoException.class, () -> SimulacaoService.validarParametros(null, null));
    }

    // ---------- simulação do pedido ----------

    @Test
    void simulaFinanciamentoComEntradaECompromentimentoDeRenda() {
        SimulacaoDTO s = service.simular(10L, 200.0, null, 12, 12.0);

        assertEquals(1000.0, s.valorTotal());
        assertEquals(200.0, s.valorEntrada());
        assertEquals(800.0, s.valorFinanciado()); // total - entrada
        assertEquals(70.85, s.valorParcela(), 0.01);
        assertEquals(850.2, s.totalPago(), 0.1);
        assertEquals(5000.0, s.rendaTotal());
        assertEquals(70.85 / 5000.0, s.comprometimentoRenda(), 0.0001);
        assertEquals(0.30, s.limiteComprometimento());
        assertFalse(s.acimaDoLimite());
        assertEquals(0.9489, s.taxaJurosMensal(), 0.0001); // % a.m.
    }

    @Test
    void alertaQuandoAParcelaPassaDe30PorCentoDaRenda() {
        cliente.setRendimentosAuferidos(List.of(200.0)); // parcela ~70,85 => 35% da renda
        SimulacaoDTO s = service.simular(10L, 200.0, null, 12, 12.0);
        assertTrue(s.comprometimentoRenda() > 0.30);
        assertTrue(s.acimaDoLimite());
    }

    @Test
    void naoAlertaExatamenteNoLimiteNemAbaixo() {
        cliente.setRendimentosAuferidos(List.of(500.0)); // 70,85 / 500 = 14%
        assertFalse(service.simular(10L, 200.0, null, 12, 12.0).acimaDoLimite());
    }

    @Test
    void semRendaSempreAlerta() {
        cliente.setRendimentosAuferidos(List.of());
        assertTrue(service.simular(10L, 0.0, null, 12, 12.0).acimaDoLimite());
    }

    @Test
    void usaOValorFinanciadoInformadoNoLugarDoTotalMenosEntrada() {
        SimulacaoDTO s = service.simular(10L, null, 500.0, 10, 0.0);
        assertEquals(500.0, s.valorFinanciado());
        assertEquals(50.0, s.valorParcela(), 0.001);
    }

    @Test
    void pedidoSemValorTotalExigeValorFinanciado() {
        pedido.setValorTotal(null);
        ValidacaoException e = assertThrows(ValidacaoException.class, () -> service.simular(10L, null, null, 12, 12.0));
        assertTrue(e.getCampos().containsKey("valorFinanciado"));
        assertEquals(300.0, service.simular(10L, null, 300.0, 12, 12.0).valorFinanciado());
    }

    @Test
    void entradaNaoPodeSerNegativaNemMaiorQueOTotal() {
        assertThrows(ValidacaoException.class, () -> service.simular(10L, -1.0, null, 12, 12.0));
        assertThrows(ValidacaoException.class, () -> service.simular(10L, 1000.0, null, 12, 12.0)); // financiado = 0
    }

    @Test
    void pedidoInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.simular(99L, 0.0, null, 12, 12.0));
    }
}
