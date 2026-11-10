package com.example.demo.service;

import com.example.demo.dto.ContratoCreditoDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.RecursoNaoEncontradoException;
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
     * Executa o contrato de um pedido aprovado. Se houver dados de crédito (valor financiado),
     * gera um {@link ContratoCredito} concedido pelo banco autenticado.
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

        Contrato contrato;
        if (credito != null && credito.valorFinanciado() != null) {
            if (!(agente instanceof Banco banco)) {
                throw new AcessoNegadoException("Apenas bancos podem conceder crédito.");
            }
            if (credito.valorFinanciado() <= 0 || credito.taxaJuros() == null || credito.taxaJuros() < 0) {
                throw new IllegalArgumentException("Valor financiado deve ser positivo e a taxa de juros informada.");
            }
            ContratoCredito contratoCredito = new ContratoCredito();
            contratoCredito.setValorFinanciado(credito.valorFinanciado());
            contratoCredito.setTaxaJuros(credito.taxaJuros());
            contratoCredito.setBanco(banco);
            contrato = contratoCredito;
        } else {
            contrato = new Contrato();
        }

        contrato.setPedidoAluguel(pedido);
        contrato.setDataAssinatura(LocalDate.now());
        return contratoRepository.save(contrato);
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
