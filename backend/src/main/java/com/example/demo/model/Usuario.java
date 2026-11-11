package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String login;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String senha;

    public Usuario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    /** Papel do usuário no sistema (CLIENTE, BANCO ou EMPRESA); exposto no JSON para o front-end. */
    @JsonProperty(value = "tipo", access = JsonProperty.Access.READ_ONLY)
    public abstract String getTipo();

    /** Nome mostrado em históricos e notificações (não é serializado no JSON). */
    @JsonIgnore
    public String getNomeExibicao() { return login; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}