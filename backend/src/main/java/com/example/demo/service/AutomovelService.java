package com.example.demo.service;

import com.example.demo.dto.AutomovelDTO;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Automovel;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutomovelService {

    private final AutomovelRepository repository;
    private final UsuarioRepository usuarioRepository;

    public AutomovelService(AutomovelRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    public Automovel salvar(AutomovelDTO dto) {
        if (dto.proprietario() == null || dto.proprietario().id() == null) {
            throw new IllegalArgumentException("O proprietário do automóvel é obrigatório.");
        }
        if (vazio(dto.matricula()) || vazio(dto.marca()) || vazio(dto.modelo()) || vazio(dto.placa()) || dto.ano() == null) {
            throw new IllegalArgumentException("Matrícula, ano, marca, modelo e placa são obrigatórios.");
        }

        // Busca o proprietário real (Cliente, Empresa ou Banco) em vez de usar o stub { id }.
        Usuario proprietario = usuarioRepository.findById(dto.proprietario().id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proprietário não encontrado."));

        Automovel automovel = new Automovel();
        automovel.setMatricula(dto.matricula().trim());
        automovel.setAno(dto.ano());
        automovel.setMarca(dto.marca().trim());
        automovel.setModelo(dto.modelo().trim());
        automovel.setPlaca(dto.placa().trim());
        automovel.setProprietario(proprietario);
        return repository.save(automovel);
    }

    public List<Automovel> listarTodos() {
        return repository.findAll();
    }

    private boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
