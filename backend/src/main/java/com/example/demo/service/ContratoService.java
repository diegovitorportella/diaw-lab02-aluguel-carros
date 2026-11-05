package com.example.demo.service;

import com.example.demo.dto.ContratoCreditoDTO;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Banco;
import com.example.demo.model.Contrato;
import com.example.demo.model.ContratoCredito;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.repository.ContratoRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final PedidoAluguelRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;

    public ContratoService(ContratoRepository contratoRepository,
                           PedidoAluguelRepository pedidoRepository,
                           UsuarioRepository usuarioRepository) {
        this.contratoRepository = contratoRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Executa o contrato de um pedido aprovado. Se houver dados de crédito (valor financiado),
     * gera um {@link ContratoCredito} concedido pelo banco informado.
     */
    @Transactional
    public Contrato gerarContrato(Long pedidoId, ContratoCreditoDTO credito) {
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
            if (credito.valorFinanciado() <= 0 || credito.taxaJuros() == null || credito.taxaJuros() < 0) {
                throw new IllegalArgumentException("Valor financiado deve ser positivo e a taxa de juros informada.");
            }
            if (credito.bancoId() == null) {
                throw new IllegalArgumentException("O banco concedente do crédito é obrigatório.");
            }
            Banco banco = usuarioRepository.findById(credito.bancoId())
                    .filter(Banco.class::isInstance).map(Banco.class::cast)
                    .orElseThrow(() -> new IllegalArgumentException("O crédito deve ser concedido por um banco cadastrado."));

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

    public List<Contrato> listarTodos() {
        return contratoRepository.findAll();
    }
}
