package com.example.demo.controller;

import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.model.Usuario;
import com.example.demo.security.JwtService;
import com.example.demo.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        Usuario usuario = usuarioService.autenticar(loginDTO.getLogin(), loginDTO.getSenha());
        return ResponseEntity.ok(new LoginResponseDTO(jwtService.gerarToken(usuario), usuario));
    }

    /** Dados do usuário dono do token. */
    @GetMapping("/me")
    public ResponseEntity<Usuario> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usuarioService.buscar(JwtService.idDo(jwt)));
    }
}
