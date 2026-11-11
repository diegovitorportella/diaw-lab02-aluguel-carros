package com.example.demo.service;

import com.example.demo.dto.ContratoCreditoDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.Agente;
import com.example.demo.model.Banco;
import com.example.demo.model.Contrato;
import com.example.demo.model.ContratoCredito;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ContratoRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final PedidoAluguelRepository pedidoRepository;
    private final UsuarioService usuarioService;

    public ContratoService(ContratoRepository contratoRepository,
                           PedidoAluguelRepository pedidoRepository,
                           UsuarioService usuarioService) {
        this.contratoRepository = contratoRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioService = usuarioService;
    }

    /**
     * Executa o contrato de um pedido aprovado e conclui o pedido (status CONCLUIDO). Se houver dados
     * de crédito, gera um {@link ContratoCredito} concedido pelo banco autenticado, com a parcela
     * calculada pela tabela Price.
     */
    @Transactional
    public Contrato gerarContrato(Long pedidoId, Long agenteId, ContratoCreditoDTO credito) {
        if (!(usuarioService.buscar(agenteId) instanceof Agente agente)) {
            throw new AcessoNegadoException("Apenas agentes podem executar contratos.");
        }
        PedidoAluguel pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));

        if (!PedidoAluguelService.APROVADO.equals(pedido.getStatus())) {
            throw new IllegalStateException("O contrato só pode ser executado para pedidos aprovados.");
        }
        if (pedido.getContrato() != null) {
            throw new IllegalStateException("Já existe um contrato para este pedido.");
        }

        boolean comCredito = credito != null && (credito.valorFinanciado() != null || credito.valorEntrada() != null
                || credito.numeroParcelas() != null || credito.taxaJuros() != null);

        Contrato contrato;
        if (comCredito) {
            if (!(agente instanceof Banco banco)) {
                throw new AcessoNegadoException("Apenas bancos podem conceder crédito.");
            }
            contrato = criarContratoDeCredito(pedido, banco, credito);
        } else {
            contrato = new Contrato();
        }

        contrato.setPedidoAluguel(pedido);
        contrato.setDataAssinatura(LocalDate.now());
        Contrato salvo = contratoRepository.save(contrato);

        pedido.setContrato(salvo);
        pedido.setStatus(PedidoAluguelService.CONCLUIDO);
        pedido.registrar(PedidoAluguelService.CONCLUIDO,
                comCredito ? "Contrato com crédito executado" : "Contrato executado", agente);
        pedidoRepository.save(pedido);
        return salvo;
    }

    private ContratoCredito criarContratoDeCredito(PedidoAluguel pedido, Banco banco, ContratoCreditoDTO dto) {
        SimulacaoService.validarParametros(dto.numeroParcelas(), dto.taxaJuros());

        double entrada = dto.valorEntrada() == null ? 0 : dto.valorEntrada();
        Double total = pedido.getValorTotal();
        if (entrada < 0) {
            throw new ValidacaoException(Map.of("valorEntrada", "A entrada não pode ser negativa."));
        }

        double financiado;
        if (dto.valorFinanciado() != null) {
            financiado = dto.valorFinanciado();
        } else if (total != null) {
            financiado = total - entrada;
        } else {
            throw new ValidacaoException(Map.of("valorFinanciado",
                    "O pedido não tem valor total: informe o valor financiado."));
        }
        if (financiado <= 0) {
            throw new ValidacaoException(Map.of("valorFinanciado", "O valor financiado deve ser maior que zero."));
        }
        if (total != null && financiado > total) {
            throw new ValidacaoException(Map.of("valorFinanciado", "O valor financiado não pode superar o valor total do pedido."));
        }

        ContratoCredito contrato = new ContratoCredito();
        contrato.setBanco(banco);
        contrato.setValorEntrada(SimulacaoService.arredondar(entrada));
        contrato.setValorFinanciado(SimulacaoService.arredondar(financiado));
        contrato.setNumeroParcelas(dto.numeroParcelas());
        contrato.setTaxaJuros(dto.taxaJuros());
        contrato.setValorParcela(SimulacaoService.parcela(financiado, dto.numeroParcelas(), dto.taxaJuros()));
        return contrato;
    }

    /** Cliente vê apenas contratos dos próprios pedidos; agentes veem todos. */
    public List<Contrato> listarPara(Long usuarioId) {
        return usuarioService.buscar(usuarioId) instanceof Agente
                ? contratoRepository.findAll()
                : contratoRepository.findByPedidoAluguelClienteId(usuarioId);
    }

    public Contrato buscar(Long id, Long usuarioId) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato não encontrado."));
        Usuario usuario = usuarioService.buscar(usuarioId);
        boolean dono = contrato.getPedidoAluguel().getCliente().getId().equals(usuarioId);
        if (!(usuario instanceof Agente) && !dono) {
            throw new AcessoNegadoException("Você não tem permissão para acessar este contrato.");
        }
        return contrato;
    }
}
