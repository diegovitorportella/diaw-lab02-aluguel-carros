package com.example.demo.controller;

import com.example.demo.model.Automovel;
import com.example.demo.service.AutomovelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/automoveis")
public class AutomovelController {

    private final AutomovelService service;

    public AutomovelController(AutomovelService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Automovel> criar(@RequestBody Automovel automovel) {
        return ResponseEntity.ok(service.salvar(automovel));
    }

    @GetMapping
    public ResponseEntity<List<Automovel>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
}