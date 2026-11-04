package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class PedidoAluguel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;
    private String parecerFinanceiro;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "automovel_id", nullable = false)
    private Automovel automovel;

    @ManyToOne
    @JoinColumn(name = "agente_avaliador_id")
    private Agente agenteAvaliador;

    @OneToOne(mappedBy = "pedidoAluguel", cascade = CascadeType.ALL)
    private Contrato contrato;

    public PedidoAluguel() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getParecerFinanceiro() { return parecerFinanceiro; }
    public void setParecerFinanceiro(String parecerFinanceiro) { this.parecerFinanceiro = parecerFinanceiro; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Automovel getAutomovel() { return automovel; }
    public void setAutomovel(Automovel automovel) { this.automovel = automovel; }

    public Agente getAgenteAvaliador() { return agenteAvaliador; }
    public void setAgenteAvaliador(Agente agenteAvaliador) { this.agenteAvaliador = agenteAvaliador; }

    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
}