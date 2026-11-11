package com.example.demo.service;

import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.Agente;
import com.example.demo.model.Cliente;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.validation.Validadores;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClienteService {

    static final int MAX_RENDIMENTOS = 3;
    static final int TAMANHO_MINIMO_SENHA = 8;

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
        Map<String, String> erros = new LinkedHashMap<>();

        if (vazio(cliente.getLogin())) {
            erros.put("login", "O login é obrigatório.");
        } else if (usuarioRepository.findByLogin(cliente.getLogin()).isPresent()) {
            erros.put("login", "Já existe um usuário com este login.");
        }
        if (cliente.getSenha() == null || cliente.getSenha().length() < TAMANHO_MINIMO_SENHA) {
            erros.put("senha", "A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        validarDados(cliente, null, erros);
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
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

        Map<String, String> erros = new LinkedHashMap<>();
        boolean trocaSenha = dados.getSenha() != null && !dados.getSenha().isBlank();
        if (trocaSenha && dados.getSenha().length() < TAMANHO_MINIMO_SENHA) {
            erros.put("senha", "A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        validarDados(dados, id, erros);
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        cliente.setNome(dados.getNome());
        cliente.setCpf(dados.getCpf());
        cliente.setRg(dados.getRg());
        cliente.setEndereco(dados.getEndereco());
        cliente.setProfissao(dados.getProfissao());
        cliente.setEntidadesEmpregadoras(copia(dados.getEntidadesEmpregadoras()));
        cliente.setRendimentosAuferidos(copia(dados.getRendimentosAuferidos()));
        if (trocaSenha) {
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

    /**
     * Valida e normaliza os dados cadastrais (CPF só com dígitos, RG sem máscara) acumulando
     * as mensagens por campo em {@code erros}.
     */
    private void validarDados(Cliente c, Long idAtual, Map<String, String> erros) {
        if (vazio(c.getNome()) || c.getNome().trim().length() < 3) {
            erros.put("nome", "O nome é obrigatório (mínimo de 3 caracteres).");
        } else {
            c.setNome(c.getNome().trim());
        }

        if (vazio(c.getCpf())) {
            erros.put("cpf", "O CPF é obrigatório.");
        } else if (!Validadores.cpfValido(c.getCpf())) {
            erros.put("cpf", "CPF inválido.");
        } else {
            String cpf = Validadores.somenteDigitos(c.getCpf());
            boolean duplicado = idAtual == null ? repository.existsByCpf(cpf) : repository.existsByCpfAndIdNot(cpf, idAtual);
            if (duplicado) {
                erros.put("cpf", "Já existe um cliente cadastrado com este CPF.");
            } else {
                c.setCpf(cpf);
            }
        }

        if (vazio(c.getRg())) {
            erros.put("rg", "O RG é obrigatório.");
        } else if (!Validadores.rgValido(c.getRg())) {
            erros.put("rg", "RG inválido (5 a 14 caracteres, apenas letras e números).");
        } else {
            c.setRg(Validadores.normalizarRg(c.getRg()));
        }

        if (vazio(c.getEndereco())) erros.put("endereco", "O endereço é obrigatório.");
        if (vazio(c.getProfissao())) erros.put("profissao", "A profissão é obrigatória.");

        validarRendimentos(c, erros);
    }

    /** Regra de negócio: de 1 a 3 fontes de renda (empregador + rendimento), valores positivos. */
    private void validarRendimentos(Cliente cliente, Map<String, String> erros) {
        List<Double> rendimentos = cliente.getRendimentosAuferidos();
        List<String> empregadores = cliente.getEntidadesEmpregadoras();
        int qtdRendimentos = rendimentos == null ? 0 : rendimentos.size();
        int qtdEmpregadores = empregadores == null ? 0 : empregadores.size();

        if (qtdRendimentos > MAX_RENDIMENTOS) {
            erros.put("rendimentosAuferidos", "O cliente pode ter no máximo 3 rendimentos auferidos.");
        } else if (qtdRendimentos == 0) {
            erros.put("rendimentosAuferidos", "Informe ao menos uma fonte de renda.");
        } else if (rendimentos.stream().anyMatch(r -> r == null || r <= 0)) {
            erros.put("rendimentosAuferidos", "Os rendimentos devem ser valores maiores que zero.");
        }

        if (qtdEmpregadores > MAX_RENDIMENTOS) {
            erros.put("entidadesEmpregadoras", "O cliente pode ter no máximo 3 entidades empregadoras.");
        } else if (qtdEmpregadores != qtdRendimentos) {
            erros.put("entidadesEmpregadoras", "Informe um empregador para cada rendimento.");
        } else if (empregadores != null && empregadores.stream().anyMatch(e -> e == null || e.isBlank())) {
            erros.put("entidadesEmpregadoras", "O nome do empregador é obrigatório em cada fonte de renda.");
        }
    }

    private boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private <T> List<T> copia(List<T> origem) {
        return origem == null ? new ArrayList<>() : new ArrayList<>(origem);
    }
}
