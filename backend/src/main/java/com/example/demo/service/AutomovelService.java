package com.example.demo.service;

import com.example.demo.model.Automovel;
import com.example.demo.repository.AutomovelRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AutomovelService {

    private final AutomovelRepository repository;

    public AutomovelService(AutomovelRepository repository) {
        this.repository = repository;
    }

    public Automovel salvar(Automovel automovel) {
        return repository.save(automovel);
    }

    public List<Automovel> listarTodos() {
        return repository.findAll();
    }
}