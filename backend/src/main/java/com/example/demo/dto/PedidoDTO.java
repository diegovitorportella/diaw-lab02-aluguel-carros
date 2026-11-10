package com.example.demo.dto;

import java.time.LocalDate;

/** Dados de um pedido. O cliente é sempre o usuário autenticado. */
public record PedidoDTO(
        IdRefDTO automovel,
        LocalDate dataInicio,
        LocalDate dataFim,
        String observacao) {}
