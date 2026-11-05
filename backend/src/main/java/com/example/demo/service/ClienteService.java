package com.example.demo.service;

import com.example.demo.model.Cliente;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    static final int MAX_RENDIMENTOS = 3;

    private final ClienteRepository repository;
    private final UsuarioRepository usuarioRepository;

    public ClienteService(ClienteRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    public Cliente salvar(Cliente cliente) {
        validarRendimentos(cliente);

        if (cliente.getLogin() == null || cliente.getLogin().isBlank()
                || cliente.getSenha() == null || cliente.getSenha().isBlank()) {
            throw new IllegalArgumentException("Login e senha são obrigatórios.");
        }
        if (usuarioRepository.findByLogin(cliente.getLogin()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com este login.");
        }

        cliente.setId(null); // garante INSERT: nunca sobrescreve um usuário existente
        return repository.save(cliente);
    }

    public List<Cliente> listarTodos() {
        return repository.findAll();
    }

    /** Regra de negócio: no máximo 3 entidades empregadoras / rendimentos auferidos. */
    private void validarRendimentos(Cliente cliente) {
        List<Double> rendimentos = cliente.getRendimentosAuferidos();
        List<String> empregadores = cliente.getEntidadesEmpregadoras();

        if (rendimentos != null) {
            if (rendimentos.size() > MAX_RENDIMENTOS) {
                throw new IllegalArgumentException("O cliente pode ter no máximo 3 rendimentos auferidos.");
            }
            if (rendimentos.stream().anyMatch(r -> r == null || r < 0)) {
                throw new IllegalArgumentException("Os rendimentos devem ser valores não negativos.");
            }
        }
        if (empregadores != null && empregadores.size() > MAX_RENDIMENTOS) {
            throw new IllegalArgumentException("O cliente pode ter no máximo 3 entidades empregadoras.");
        }
    }
}
