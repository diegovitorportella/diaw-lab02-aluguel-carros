package com.example.demo.dto;

import com.example.demo.model.Usuario;

public record LoginResponseDTO(String token, Usuario usuario) {}
