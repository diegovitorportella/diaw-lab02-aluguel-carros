package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class Banco extends Agente {
    public Banco() {}

    @Override
    public String getTipo() { return "BANCO"; }
}