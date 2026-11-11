package com.example.demo.service;

import com.example.demo.dto.IdRefDTO;
import com.example.demo.dto.PedidoDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.Automovel;
import com.example.demo.model.Banco;
import com.example.demo.model.Cliente;
import com.example.demo.model.Contrato;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.model.PedidoHistorico;
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
    Automovel carro;
    PedidoAluguel pedido;

    LocalDate hoje = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new PedidoAluguelService(repository, automovelRepository, usuarioService);

        dono = new Cliente();
        dono.setId(1L);
        dono.setRendimentosAuferidos(List.of(5000.0));
        outro = new Cliente();
        outro.setId(2L);
        banco = new Banco();
        banco.setId(3L);

        carro = new Automovel();
        carro.setId(5L);
        carro.setProprietario(banco);
        carro.setValorDiaria(100.0);

        pedido = new PedidoAluguel();
        pedido.setId(10L);
        pedido.setCliente(dono);
        pedido.setAutomovel(carro);
        pedido.setDataInicio(hoje.plusDays(1));
        pedido.setDataFim(hoje.plusDays(5));
        pedido.setValorTotal(500.0);
        pedido.setStatus(PedidoAluguelService.PENDENTE);

        when(usuarioService.buscar(1L)).thenReturn(dono);
        when(usuarioService.buscar(2L)).thenReturn(outro);
        when(usuarioService.buscar(3L)).thenReturn(banco);
        when(automovelRepository.findById(5L)).thenReturn(Optional.of(carro));
        when(repository.findById(10L)).thenReturn(Optional.of(pedido));
        when(repository.save(any(PedidoAluguel.class))).thenAnswer(i -> i.getArgument(0));
    }

    private PedidoDTO dto(Long automovelId, LocalDate inicio, LocalDate fim) {
        return new PedidoDTO(automovelId == null ? null : new IdRefDTO(automovelId), inicio, fim, "obs");
    }

    private PedidoHistorico ultimoEvento(PedidoAluguel p) {
        return p.getHistorico().get(p.getHistorico().size() - 1);
    }

    // ---------- valor total ----------

    @Test
    void calculaTotalComPeriodoInclusivo() {
        assertEquals(100.0, PedidoAluguelService.calcularTotal(carro, hoje, hoje)); // mesmo dia = 1 diária
        assertEquals(500.0, PedidoAluguelService.calcularTotal(carro, hoje, hoje.plusDays(4)));
        carro.setValorDiaria(33.333);
        assertEquals(100.0, PedidoAluguelService.calcularTotal(carro, hoje, hoje.plusDays(2)), 0.001);
    }

    @Test
    void totalNuloQuandoNaoHaDiariaOuDatas() {
        assertNull(PedidoAluguelService.calcularTotal(null, hoje, hoje));
        assertNull(PedidoAluguelService.calcularTotal(carro, null, hoje));
        carro.setValorDiaria(null);
        assertNull(PedidoAluguelService.calcularTotal(carro, hoje, hoje));
    }

    // ---------- criar ----------

    @Test
    void criaPedidoPendenteComTotalEHistorico() {
        PedidoAluguel criado = service.criar(1L, dto(5L, hoje.plusDays(1), hoje.plusDays(3)));

        assertEquals("PENDENTE", criado.getStatus());
        assertSame(dono, criado.getCliente());
        assertSame(carro, criado.getAutomovel());
        assertEquals(300.0, criado.getValorTotal()); // 3 diárias x 100
        assertEquals(1, criado.getHistorico().size());
        assertEquals("PENDENTE", criado.getHistorico().get(0).getStatus());
        assertEquals("CLIENTE", criado.getHistorico().get(0).getResponsavelTipo());
        assertNotNull(criado.getHistorico().get(0).getDataHora());
    }

    @Test
    void aceitaIniciarHoje() {
        assertDoesNotThrow(() -> service.criar(1L, dto(5L, hoje, hoje.plusDays(1))));
    }

    @Test
    void agenteNaoCriaPedido() {
        assertThrows(AcessoNegadoException.class, () -> service.criar(3L, dto(5L, hoje, hoje)));
    }

    @Test
    void exigeAutomovelEDatas() {
        ValidacaoException e = assertThrows(ValidacaoException.class, () -> service.criar(1L, dto(null, null, null)));
        assertTrue(e.getCampos().keySet().containsAll(List.of("automovel", "dataInicio", "dataFim")));
    }

    @Test
    void rejeitaDataInicialNoPassado() {
        ValidacaoException e = assertThrows(ValidacaoException.class,
                () -> service.criar(1L, dto(5L, hoje.minusDays(1), hoje.plusDays(2))));
        assertTrue(e.getCampos().containsKey("dataInicio"));
    }

    @Test
    void rejeitaPeriodoInvertido() {
        ValidacaoException e = assertThrows(ValidacaoException.class,
                () -> service.criar(1L, dto(5L, hoje.plusDays(10), hoje.plusDays(1))));
        assertTrue(e.getCampos().containsKey("dataFim"));
    }

    @Test
    void clienteNaoAlugaOProprioVeiculo() {
        carro.setProprietario(dono);
        ValidacaoException e = assertThrows(ValidacaoException.class, () -> service.criar(1L, dto(5L, hoje, hoje)));
        assertEquals("Você não pode alugar o próprio veículo.", e.getCampos().get("automovel"));
    }

    @Test
    void veiculoSemDiariaGeraPedidoSemTotal() {
        carro.setValorDiaria(null);
        assertNull(service.criar(1L, dto(5L, hoje, hoje.plusDays(1))).getValorTotal());
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
    void donoModificaDatasERecalculaTotalERegistraHistorico() {
        PedidoAluguel alterado = service.modificar(10L, 1L, dto(null, hoje.plusDays(2), hoje.plusDays(3)));

        assertEquals(hoje.plusDays(3), alterado.getDataFim());
        assertEquals(200.0, alterado.getValorTotal()); // 2 diárias
        assertEquals("obs", alterado.getObservacao());
        assertEquals("Pedido modificado", ultimoEvento(alterado).getDescricao());
        assertEquals("PENDENTE", ultimoEvento(alterado).getStatus());
    }

    @Test
    void modificacaoParcialMantemOsDemaisCampos() {
        PedidoAluguel alterado = service.modificar(10L, 1L, new PedidoDTO(null, null, null, "novo"));
        assertEquals(hoje.plusDays(1), alterado.getDataInicio());
        assertEquals(hoje.plusDays(5), alterado.getDataFim());
        assertEquals(500.0, alterado.getValorTotal());
        assertSame(carro, alterado.getAutomovel());
        assertEquals("novo", alterado.getObservacao());
    }

    @Test
    void trocarDeVeiculoRecalculaOTotal() {
        Automovel outroCarro = new Automovel();
        outroCarro.setId(6L);
        outroCarro.setProprietario(banco);
        outroCarro.setValorDiaria(200.0);
        when(automovelRepository.findById(6L)).thenReturn(Optional.of(outroCarro));

        PedidoAluguel alterado = service.modificar(10L, 1L, dto(6L, null, null));
        assertSame(outroCarro, alterado.getAutomovel());
        assertEquals(1000.0, alterado.getValorTotal()); // 5 diárias x 200
    }

    @Test
    void agenteAlteraApenasAObservacaoDoPedidoPendente() {
        PedidoAluguel alterado = service.modificar(10L, 3L, new PedidoDTO(null, null, null, "parecer"));
        assertEquals("parecer", alterado.getObservacao());
        assertEquals(hoje.plusDays(1), alterado.getDataInicio());
        assertEquals("BANCO", ultimoEvento(alterado).getResponsavelTipo());
    }

    @Test
    void agenteNaoAlteraDatasNemVeiculo() {
        assertThrows(AcessoNegadoException.class, () -> service.modificar(10L, 3L, dto(null, hoje.plusDays(2), null)));
        assertThrows(AcessoNegadoException.class, () -> service.modificar(10L, 3L, dto(null, null, hoje.plusDays(9))));
        assertThrows(AcessoNegadoException.class, () -> service.modificar(10L, 3L, dto(6L, null, null)));
        assertEquals(hoje.plusDays(1), pedido.getDataInicio());
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

    @Test
    void naoPermiteNovaDataInicialNoPassadoMasMantemAExistente() {
        assertThrows(ValidacaoException.class, () -> service.modificar(10L, 1L, dto(null, hoje.minusDays(3), null)));

        // pedido antigo cujo início já passou: reenviar a mesma data não é erro
        pedido.setDataInicio(hoje.minusDays(2));
        pedido.setDataFim(hoje.plusDays(2));
        assertDoesNotThrow(() -> service.modificar(10L, 1L, dto(null, hoje.minusDays(2), null)));
    }

    @Test
    void modificacaoRejeitaPeriodoInvertidoEVeiculoProprio() {
        assertThrows(ValidacaoException.class, () -> service.modificar(10L, 1L, dto(null, null, hoje)));

        Automovel proprio = new Automovel();
        proprio.setId(7L);
        proprio.setProprietario(dono);
        when(automovelRepository.findById(7L)).thenReturn(Optional.of(proprio));
        assertThrows(ValidacaoException.class, () -> service.modificar(10L, 1L, dto(7L, null, null)));
    }

    // ---------- cancelar ----------

    @Test
    void donoCancelaPedidoPendenteERegistraHistorico() {
        PedidoAluguel cancelado = service.cancelar(10L, 1L);
        assertEquals("CANCELADO", cancelado.getStatus());
        assertEquals("CANCELADO", ultimoEvento(cancelado).getStatus());
    }

    @Test
    void donoCancelaPedidoAprovadoSemContrato() {
        pedido.setStatus(PedidoAluguelService.APROVADO);
        assertEquals("CANCELADO", service.cancelar(10L, 1L).getStatus());
    }

    @Test
    void naoCancelaPedidoAprovadoComContratoNemConcluido() {
        pedido.setStatus(PedidoAluguelService.APROVADO);
        pedido.setContrato(new Contrato());
        assertThrows(IllegalStateException.class, () -> service.cancelar(10L, 1L));

        pedido.setStatus(PedidoAluguelService.CONCLUIDO);
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
    void parecerPositivoApenasAprovaERegistraAgenteEHistorico() {
        PedidoAluguel avaliado = service.avaliarPedido(10L, "positivo", 3L);
        assertEquals("APROVADO", avaliado.getStatus()); // o contrato é um passo posterior
        assertEquals("POSITIVO", avaliado.getParecerFinanceiro());
        assertSame(banco, avaliado.getAgenteAvaliador());
        assertEquals("APROVADO", ultimoEvento(avaliado).getStatus());
        assertEquals("BANCO", ultimoEvento(avaliado).getResponsavelTipo());
        assertNull(avaliado.getContrato());
    }

    @Test
    void parecerNegativoRejeita() {
        PedidoAluguel avaliado = service.avaliarPedido(10L, "NEGATIVO", 3L);
        assertEquals("REJEITADO", avaliado.getStatus());
        assertEquals("REJEITADO", ultimoEvento(avaliado).getStatus());
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
