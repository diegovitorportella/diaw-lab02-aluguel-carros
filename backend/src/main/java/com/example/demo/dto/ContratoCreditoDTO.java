package com.example.demo.dto;

/** Dados do crédito. O banco concedente é sempre o agente (banco) autenticado. */
public record ContratoCreditoDTO(Double valorFinanciado, Double taxaJuros) {}
