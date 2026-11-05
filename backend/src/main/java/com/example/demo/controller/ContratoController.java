package com.example.demo.controller;

import com.example.demo.dto.ContratoCreditoDTO;
import com.example.demo.model.Contrato;
import com.example.demo.service.ContratoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    private final ContratoService service;

    public ContratoController(ContratoService service) {
        this.service = service;
    }

    @PostMapping("/gerar/{pedidoId}")
    public ResponseEntity<Contrato> gerarContrato(@PathVariable Long pedidoId,
                                                  @RequestBody(required = false) ContratoCreditoDTO contratoCredito) {
        return ResponseEntity.ok(service.gerarContrato(pedidoId, contratoCredito));
    }

    @GetMapping
    public ResponseEntity<List<Contrato>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
}
