package com.example.demo.controller;

import com.example.demo.dto.PedidoDTO;
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
    public ResponseEntity<PedidoAluguel> criar(@RequestBody PedidoDTO pedido) {
        return ResponseEntity.ok(service.criar(pedido));
    }

    @GetMapping
    public ResponseEntity<List<PedidoAluguel>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    /** Modificação por cliente (dono) ou agente, enquanto o pedido está pendente. */
    @PutMapping("/{id}")
    public ResponseEntity<PedidoAluguel> modificar(@PathVariable Long id,
                                                   @RequestParam Long usuarioId,
                                                   @RequestBody PedidoDTO pedido) {
        return ResponseEntity.ok(service.modificar(id, usuarioId, pedido));
    }

    /** Cancelamento pelo cliente dono do pedido. */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PedidoAluguel> cancelar(@PathVariable Long id, @RequestParam Long usuarioId) {
        return ResponseEntity.ok(service.cancelar(id, usuarioId));
    }

    @PutMapping("/{id}/avaliar")
    public ResponseEntity<PedidoAluguel> avaliar(@PathVariable Long id,
                                                 @RequestParam String parecer,
                                                 @RequestParam Long agenteId) {
        return ResponseEntity.ok(service.avaliarPedido(id, parecer, agenteId));
    }
}
