package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class ContratoCredito extends Contrato {

    private Double valorFinanciado;
    private Double taxaJuros;
    private Double valorEntrada;
    private Integer numeroParcelas;
    private Double valorParcela;

    @ManyToOne
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco; // Banco que concede o crédito

    public ContratoCredito() {}

    public Double getValorFinanciado() { return valorFinanciado; }
    public void setValorFinanciado(Double valorFinanciado) { this.valorFinanciado = valorFinanciado; }

    public Double getTaxaJuros() { return taxaJuros; }
    public void setTaxaJuros(Double taxaJuros) { this.taxaJuros = taxaJuros; }

    public Double getValorEntrada() { return valorEntrada; }
    public void setValorEntrada(Double valorEntrada) { this.valorEntrada = valorEntrada; }

    public Integer getNumeroParcelas() { return numeroParcelas; }
    public void setNumeroParcelas(Integer numeroParcelas) { this.numeroParcelas = numeroParcelas; }

    public Double getValorParcela() { return valorParcela; }
    public void setValorParcela(Double valorParcela) { this.valorParcela = valorParcela; }

    public Banco getBanco() { return banco; }
    public void setBanco(Banco banco) { this.banco = banco; }
}