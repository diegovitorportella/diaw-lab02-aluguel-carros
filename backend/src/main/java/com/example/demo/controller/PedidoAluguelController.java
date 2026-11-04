package com.example.demo.controller;

import com.example.demo.model.PedidoAluguel;
import com.example.demo.service.PedidoAluguelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoAluguelController {

    private final PedidoAluguelService service;

    public PedidoAluguelController(PedidoAluguelService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PedidoAluguel> criar(@RequestBody PedidoAluguel pedido) {
        return ResponseEntity.ok(service.criar(pedido));
    }

    @GetMapping
    public ResponseEntity<List<PedidoAluguel>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @PutMapping("/{id}/avaliar")
    public ResponseEntity<PedidoAluguel> avaliar(@PathVariable Long id, @RequestParam String parecer) {
        try {
            return ResponseEntity.ok(service.avaliarPedido(id, parecer));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}