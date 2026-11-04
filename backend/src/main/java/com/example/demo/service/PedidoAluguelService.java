package com.example.demo.service;

import com.example.demo.model.PedidoAluguel;
import com.example.demo.repository.PedidoAluguelRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PedidoAluguelService {

    private final PedidoAluguelRepository repository;

    public PedidoAluguelService(PedidoAluguelRepository repository) {
        this.repository = repository;
    }

    public PedidoAluguel criar(PedidoAluguel pedido) {
        pedido.setStatus("PENDENTE");
        return repository.save(pedido);
    }

    public List<PedidoAluguel> listarTodos() {
        return repository.findAll();
    }

    public PedidoAluguel avaliarPedido(Long id, String parecer) {
        PedidoAluguel pedido = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        
        pedido.setParecerFinanceiro(parecer);
        
        if ("POSITIVO".equalsIgnoreCase(parecer)) {
            pedido.setStatus("APROVADO");
        } else {
            pedido.setStatus("REJEITADO");
        }
        
        return repository.save(pedido);
    }
}