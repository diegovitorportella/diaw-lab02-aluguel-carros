package com.example.demo.dto;

/**
 * Dados do crédito. O banco concedente é sempre o agente (banco) autenticado.
 * Se {@code valorFinanciado} for nulo, é calculado: valor total do pedido menos a entrada.
 * A taxa de juros é anual (% a.a.).
 */
public record ContratoCreditoDTO(
        Double valorFinanciado,
        Double valorEntrada,
        Integer numeroParcelas,
        Double taxaJuros) {}
