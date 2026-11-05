package com.example.demo.dto;

import java.time.LocalDate;

public record PedidoDTO(
        IdRefDTO cliente,
        IdRefDTO automovel,
        LocalDate dataInicio,
        LocalDate dataFim,
        String observacao) {}
