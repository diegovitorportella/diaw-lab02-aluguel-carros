package com.example.demo.controller;

import com.example.demo.dto.PedidoDTO;
import com.example.demo.dto.SimulacaoDTO;
import com.example.demo.service.SimulacaoService;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.security.JwtService;
import com.example.demo.service.PedidoAluguelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoAluguelController {

    private final PedidoAluguelService service;
    private final SimulacaoService simulacaoService;

    public PedidoAluguelController(PedidoAluguelService service, SimulacaoService simulacaoService) {
        this.service = service;
        this.simulacaoService = simulacaoService;
    }

    /** Somente clientes; o cliente do pedido é o usuário autenticado. */
    @PostMapping
    public ResponseEntity<PedidoAluguel> criar(@AuthenticationPrincipal Jwt jwt, @RequestBody PedidoDTO pedido) {
        return ResponseEntity.ok(service.criar(JwtService.idDo(jwt), pedido));
    }

    /** Cliente: os próprios pedidos. Agente: todos. Filtro opcional: ?status=PENDENTE. */
    @GetMapping
    public ResponseEntity<List<PedidoAluguel>> listar(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.listarPara(JwtService.idDo(jwt), status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoAluguel> buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.buscar(id, JwtService.idDo(jwt)));
    }

    /** Modificação por cliente (dono) ou agente, enquanto o pedido está pendente. */
    @PutMapping("/{id}")
    public ResponseEntity<PedidoAluguel> modificar(@PathVariable Long id,
                                                   @AuthenticationPrincipal Jwt jwt,
                                                   @RequestBody PedidoDTO pedido) {
        return ResponseEntity.ok(service.modificar(id, JwtService.idDo(jwt), pedido));
    }

    /** Cancelamento pelo cliente dono do pedido. */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PedidoAluguel> cancelar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.cancelar(id, JwtService.idDo(jwt)));
    }

    /**
     * Simulação de financiamento (somente agentes): parcela pela tabela Price e comprometimento da renda
     * do cliente. Parâmetros: parcelas, juros (% a.a.), entrada (opcional) e valorFinanciado (opcional).
     */
    @GetMapping("/{id}/simulacao")
    public ResponseEntity<SimulacaoDTO> simular(@PathVariable Long id,
                                                @RequestParam Integer parcelas,
                                                @RequestParam Double juros,
                                                @RequestParam(required = false) Double entrada,
                                                @RequestParam(required = false) Double valorFinanciado) {
        return ResponseEntity.ok(simulacaoService.simular(id, entrada, valorFinanciado, parcelas, juros));
    }

    /** Somente agentes. */
    @PutMapping("/{id}/avaliar")
    public ResponseEntity<PedidoAluguel> avaliar(@PathVariable Long id,
                                                 @AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam String parecer) {
        return ResponseEntity.ok(service.avaliarPedido(id, parecer, JwtService.idDo(jwt)));
    }
}
