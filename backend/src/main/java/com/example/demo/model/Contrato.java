package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dataAssinatura;

    @OneToOne
    @JsonIgnoreProperties("contrato")
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private PedidoAluguel pedidoAluguel;

    public Contrato() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDataAssinatura() { return dataAssinatura; }
    public void setDataAssinatura(LocalDate dataAssinatura) { this.dataAssinatura = dataAssinatura; }

    public PedidoAluguel getPedidoAluguel() { return pedidoAluguel; }
    public void setPedidoAluguel(PedidoAluguel pedidoAluguel) { this.pedidoAluguel = pedidoAluguel; }
}