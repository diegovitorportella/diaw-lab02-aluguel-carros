package com.example.demo.dto;

/** Dados editáveis de um automóvel. O proprietário é sempre o usuário autenticado. */
public record AutomovelDTO(
        String matricula,
        Integer ano,
        String marca,
        String modelo,
        String placa) {}
