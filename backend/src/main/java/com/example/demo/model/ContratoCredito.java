package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class ContratoCredito extends Contrato {

    private Double valorFinanciado;
    private Double taxaJuros;

    @ManyToOne
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco; // Banco que concede o crédito

    public ContratoCredito() {}

    public Double getValorFinanciado() { return valorFinanciado; }
    public void setValorFinanciado(Double valorFinanciado) { this.valorFinanciado = valorFinanciado; }

    public Double getTaxaJuros() { return taxaJuros; }
    public void setTaxaJuros(Double taxaJuros) { this.taxaJuros = taxaJuros; }

    public Banco getBanco() { return banco; }
    public void setBanco(Banco banco) { this.banco = banco; }
}