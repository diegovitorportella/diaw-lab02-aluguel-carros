package com.example.demo.controller;

import com.example.demo.dto.ContratoCreditoDTO;
import com.example.demo.model.Contrato;
import com.example.demo.security.JwtService;
import com.example.demo.service.ContratoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    private final ContratoService service;

    public ContratoController(ContratoService service) {
        this.service = service;
    }

    /** Somente agentes; com dados de crédito, somente banco (o banco é o agente autenticado). */
    @PostMapping("/gerar/{pedidoId}")
    public ResponseEntity<Contrato> gerarContrato(@PathVariable Long pedidoId,
                                                  @AuthenticationPrincipal Jwt jwt,
                                                  @RequestBody(required = false) ContratoCreditoDTO contratoCredito) {
        return ResponseEntity.ok(service.gerarContrato(pedidoId, JwtService.idDo(jwt), contratoCredito));
    }

    @GetMapping
    public ResponseEntity<List<Contrato>> listar(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.listarPara(JwtService.idDo(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contrato> buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.buscar(id, JwtService.idDo(jwt)));
    }
}
