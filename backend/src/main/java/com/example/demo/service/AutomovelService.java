package com.example.demo.service;

import com.example.demo.dto.AutomovelDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Automovel;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AutomovelService {

    private final AutomovelRepository repository;
    private final PedidoAluguelRepository pedidoRepository;
    private final UsuarioService usuarioService;

    public AutomovelService(AutomovelRepository repository,
                            PedidoAluguelRepository pedidoRepository,
                            UsuarioService usuarioService) {
        this.repository = repository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioService = usuarioService;
    }

    /** O proprietário (cliente, empresa ou banco) é o usuário autenticado. */
    @Transactional
    public Automovel salvar(Long proprietarioId, AutomovelDTO dto) {
        validar(dto);
        Automovel automovel = new Automovel();
        automovel.setProprietario(usuarioService.buscar(proprietarioId));
        preencher(automovel, dto);
        return repository.save(automovel);
    }

    public List<Automovel> listarTodos() {
        return repository.findAll();
    }

    public List<Automovel> listarDoProprietario(Long proprietarioId) {
        return repository.findByProprietarioId(proprietarioId);
    }

    public Automovel buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Automóvel não encontrado."));
    }

    @Transactional
    public Automovel atualizar(Long id, Long solicitanteId, AutomovelDTO dto) {
        Automovel automovel = buscar(id);
        exigirProprietario(automovel, solicitanteId);
        validar(dto);
        preencher(automovel, dto);
        return repository.save(automovel);
    }

    @Transactional
    public void excluir(Long id, Long solicitanteId) {
        Automovel automovel = buscar(id);
        exigirProprietario(automovel, solicitanteId);
        if (pedidoRepository.existsByAutomovelId(id)) {
            throw new IllegalStateException("Não é possível excluir um automóvel que possui pedidos de aluguel.");
        }
        repository.delete(automovel);
    }

    private void exigirProprietario(Automovel automovel, Long solicitanteId) {
        if (!automovel.getProprietario().getId().equals(solicitanteId)) {
            throw new AcessoNegadoException("Apenas o proprietário pode alterar este automóvel.");
        }
    }

    private void preencher(Automovel automovel, AutomovelDTO dto) {
        automovel.setMatricula(dto.matricula().trim());
        automovel.setAno(dto.ano());
        automovel.setMarca(dto.marca().trim());
        automovel.setModelo(dto.modelo().trim());
        automovel.setPlaca(dto.placa().trim());
    }

    private void validar(AutomovelDTO dto) {
        if (vazio(dto.matricula()) || vazio(dto.marca()) || vazio(dto.modelo()) || vazio(dto.placa()) || dto.ano() == null) {
            throw new IllegalArgumentException("Matrícula, ano, marca, modelo e placa são obrigatórios.");
        }
        if (dto.ano() < 1900 || dto.ano() > 2100) {
            throw new IllegalArgumentException("Ano do automóvel inválido.");
        }
    }

    private boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
