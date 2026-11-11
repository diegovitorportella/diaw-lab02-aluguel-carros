package com.example.demo.dto;

/** Resultado da simulação de financiamento de um pedido (tabela Price). */
public record SimulacaoDTO(
        Double valorTotal,
        Double valorEntrada,
        Double valorFinanciado,
        Integer numeroParcelas,
        Double taxaJurosAnual,
        Double taxaJurosMensal,
        Double valorParcela,
        Double totalPago,
        Double rendaTotal,
        Double comprometimentoRenda,
        Double limiteComprometimento,
        boolean acimaDoLimite) {}
