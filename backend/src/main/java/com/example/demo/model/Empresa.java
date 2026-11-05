package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class Empresa extends Agente {
    public Empresa() {}

    @Override
    public String getTipo() { return "EMPRESA"; }
}