package com.example.demo.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Cliente extends Usuario {

    private String rg;
    private String cpf;
    private String nome;
    private String endereco;
    private String profissao;

    @ElementCollection
    @CollectionTable(name = "cliente_empregadores", joinColumns = @JoinColumn(name = "cliente_id"))
    @Column(name = "empregador")
    private List<String> entidadesEmpregadoras;

    @ElementCollection
    @CollectionTable(name = "cliente_rendimentos", joinColumns = @JoinColumn(name = "cliente_id"))
    @Column(name = "rendimento")
    private List<Double> rendimentosAuferidos;

    public Cliente() {}

    @Override
    public String getTipo() { return "CLIENTE"; }

    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getProfissao() { return profissao; }
    public void setProfissao(String profissao) { this.profissao = profissao; }

    public List<String> getEntidadesEmpregadoras() { return entidadesEmpregadoras; }
    public void setEntidadesEmpregadoras(List<String> entidadesEmpregadoras) { this.entidadesEmpregadoras = entidadesEmpregadoras; }

    public List<Double> getRendimentosAuferidos() { return rendimentosAuferidos; }
    public void setRendimentosAuferidos(List<Double> rendimentosAuferidos) { this.rendimentosAuferidos = rendimentosAuferidos; }
}