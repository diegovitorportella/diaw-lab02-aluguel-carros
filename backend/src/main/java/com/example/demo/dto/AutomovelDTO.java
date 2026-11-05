package com.example.demo.dto;

public record AutomovelDTO(
        String matricula,
        Integer ano,
        String marca,
        String modelo,
        String placa,
        IdRefDTO proprietario) {}
