package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Agente extends Usuario {

    private String cnpj;
    private String razaoSocial;

    public Agente() {}

    @Override
    public String getNomeExibicao() { return razaoSocial != null && !razaoSocial.isBlank() ? razaoSocial : getLogin(); }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
}