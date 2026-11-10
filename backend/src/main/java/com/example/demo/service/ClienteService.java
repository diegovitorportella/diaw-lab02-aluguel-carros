package com.example.demo.service;

import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Agente;
import com.example.demo.model.Cliente;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClienteService {

    static final int MAX_RENDIMENTOS = 3;
    static final int TAMANHO_MINIMO_SENHA = 6;

    private final ClienteRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoAluguelRepository pedidoRepository;
    private final AutomovelRepository automovelRepository;
    private final UsuarioService usuarioService;

    public ClienteService(ClienteRepository repository,
                          UsuarioRepository usuarioRepository,
                          PedidoAluguelRepository pedidoRepository,
                          AutomovelRepository automovelRepository,
                          UsuarioService usuarioService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.automovelRepository = automovelRepository;
        this.usuarioService = usuarioService;
    }

    /** Cadastro (autocadastro público): valida regras de negócio e grava a senha com hash BCrypt. */
    @Transactional
    public Cliente salvar(Cliente cliente) {
        validarRendimentos(cliente);

        if (cliente.getLogin() == null || cliente.getLogin().isBlank()) {
            throw new IllegalArgumentException("Login e senha são obrigatórios.");
        }
        validarSenha(cliente.getSenha());
        if (usuarioRepository.findByLogin(cliente.getLogin()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com este login.");
        }

        cliente.setId(null); // garante INSERT: nunca sobrescreve um usuário existente
        cliente.setSenha(usuarioService.codificar(cliente.getSenha()));
        return repository.save(cliente);
    }

    public List<Cliente> listarTodos() {
        return repository.findAll();
    }

    public Cliente buscar(Long id, Long solicitanteId) {
        exigirPropriaContaOuAgente(id, solicitanteId);
        return buscarExistente(id);
    }

    /** Atualiza os dados cadastrais. O login não muda; a senha só muda se for informada. */
    @Transactional
    public Cliente atualizar(Long id, Long solicitanteId, Cliente dados) {
        exigirPropriaContaOuAgente(id, solicitanteId);
        Cliente cliente = buscarExistente(id);
        validarRendimentos(dados);

        cliente.setNome(dados.getNome());
        cliente.setCpf(dados.getCpf());
        cliente.setRg(dados.getRg());
        cliente.setEndereco(dados.getEndereco());
        cliente.setProfissao(dados.getProfissao());
        cliente.setEntidadesEmpregadoras(copia(dados.getEntidadesEmpregadoras()));
        cliente.setRendimentosAuferidos(copia(dados.getRendimentosAuferidos()));

        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            validarSenha(dados.getSenha());
            cliente.setSenha(usuarioService.codificar(dados.getSenha()));
        }
        return repository.save(cliente);
    }

    @Transactional
    public void excluir(Long id, Long solicitanteId) {
        exigirPropriaContaOuAgente(id, solicitanteId);
        Cliente cliente = buscarExistente(id);
        if (pedidoRepository.existsByClienteId(id)) {
            throw new IllegalStateException("Não é possível excluir um cliente que possui pedidos de aluguel.");
        }
        if (automovelRepository.existsByProprietarioId(id)) {
            throw new IllegalStateException("Não é possível excluir um cliente que possui automóveis cadastrados.");
        }
        repository.delete(cliente);
    }

    private Cliente buscarExistente(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
    }

    private void exigirPropriaContaOuAgente(Long id, Long solicitanteId) {
        if (!id.equals(solicitanteId) && !(usuarioService.buscar(solicitanteId) instanceof Agente)) {
            throw new AcessoNegadoException("Você só pode acessar os dados da sua própria conta.");
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
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

    private <T> List<T> copia(List<T> origem) {
        return origem == null ? new ArrayList<>() : new ArrayList<>(origem);
    }
}
