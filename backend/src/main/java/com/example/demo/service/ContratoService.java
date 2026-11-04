package com.example.demo.service;

import com.example.demo.model.Contrato;
import com.example.demo.model.ContratoCredito;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.repository.ContratoRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final PedidoAluguelRepository pedidoRepository;

    public ContratoService(ContratoRepository contratoRepository, PedidoAluguelRepository pedidoRepository) {
        this.contratoRepository = contratoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public Contrato gerarContrato(Long pedidoId, ContratoCredito contratoCredito) {
        PedidoAluguel pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));

        if (!"APROVADO".equalsIgnoreCase(pedido.getStatus())) {
            throw new IllegalStateException("O contrato só pode ser executado para pedidos aprovados.");
        }

        Contrato contrato = (contratoCredito != null && contratoCredito.getValorFinanciado() != null) 
                            ? contratoCredito : new Contrato();
        
        contrato.setPedidoAluguel(pedido);
        contrato.setDataAssinatura(LocalDate.now());

        return contratoRepository.save(contrato);
    }

    public List<Contrato> listarTodos() {
        return contratoRepository.findAll();
    }
}