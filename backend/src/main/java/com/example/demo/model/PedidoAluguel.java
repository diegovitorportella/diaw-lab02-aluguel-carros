package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class PedidoAluguel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;
    private String parecerFinanceiro;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String observacao;
    private Double valorTotal;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "automovel_id", nullable = false)
    private Automovel automovel;

    @ManyToOne
    @JoinColumn(name = "agente_avaliador_id")
    private Agente agenteAvaliador;

    @OneToOne(mappedBy = "pedidoAluguel")
    @JsonIgnoreProperties("pedidoAluguel")
    private Contrato contrato;

    /** Acompanhamento: cada mudança de situação do pedido, com data e responsável. */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataHora ASC, id ASC")
    private List<PedidoHistorico> historico = new ArrayList<>();

    public PedidoAluguel() {}

    /** Registra um evento no histórico do pedido. */
    public void registrar(String statusAtual, String descricao, Usuario responsavel) {
        PedidoHistorico evento = new PedidoHistorico();
        evento.setPedido(this);
        evento.setStatus(statusAtual);
        evento.setDescricao(descricao);
        evento.setDataHora(LocalDateTime.now());
        if (responsavel != null) {
            evento.setResponsavelNome(responsavel.getNomeExibicao());
            evento.setResponsavelTipo(responsavel.getTipo());
        }
        historico.add(evento);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getParecerFinanceiro() { return parecerFinanceiro; }
    public void setParecerFinanceiro(String parecerFinanceiro) { this.parecerFinanceiro = parecerFinanceiro; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }

    public List<PedidoHistorico> getHistorico() { return historico; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Automovel getAutomovel() { return automovel; }
    public void setAutomovel(Automovel automovel) { this.automovel = automovel; }

    public Agente getAgenteAvaliador() { return agenteAvaliador; }
    public void setAgenteAvaliador(Agente agenteAvaliador) { this.agenteAvaliador = agenteAvaliador; }

    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
}