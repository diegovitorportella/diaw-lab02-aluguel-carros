package com.example.demo.controller;

import com.example.demo.dto.AutomovelDTO;
import com.example.demo.model.Automovel;
import com.example.demo.security.JwtService;
import com.example.demo.service.AutomovelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/automoveis")
public class AutomovelController {

    private final AutomovelService service;

    public AutomovelController(AutomovelService service) {
        this.service = service;
    }

    /** O proprietário do automóvel é o usuário autenticado. */
    @PostMapping
    public ResponseEntity<Automovel> criar(@AuthenticationPrincipal Jwt jwt, @RequestBody AutomovelDTO automovel) {
        return ResponseEntity.ok(service.salvar(JwtService.idDo(jwt), automovel));
    }

    @GetMapping
    public ResponseEntity<List<Automovel>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/meus")
    public ResponseEntity<List<Automovel>> listarMeus(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.listarDoProprietario(JwtService.idDo(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Automovel> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Automovel> atualizar(@PathVariable Long id,
                                               @AuthenticationPrincipal Jwt jwt,
                                               @RequestBody AutomovelDTO automovel) {
        return ResponseEntity.ok(service.atualizar(id, JwtService.idDo(jwt), automovel));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.excluir(id, JwtService.idDo(jwt));
        return ResponseEntity.noContent().build();
    }
}
