package com.example.demo.controller;

import com.example.demo.model.Contrato;
import com.example.demo.model.ContratoCredito;
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
    public ResponseEntity<Contrato> gerarContrato(@PathVariable Long pedidoId, @RequestBody(required = false) ContratoCredito contratoCredito) {
        try {
            return ResponseEntity.ok(service.gerarContrato(pedidoId, contratoCredito));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Contrato>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
}