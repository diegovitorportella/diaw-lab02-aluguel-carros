package com.example.demo.service;

import com.example.demo.dto.AutomovelDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.Automovel;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import com.example.demo.validation.Validadores;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutomovelService {

    static final double MAX_DIARIA = 100_000.0;

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
        validar(dto, null);
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
        validar(dto, id);
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
        automovel.setPlaca(Validadores.normalizarPlaca(dto.placa()));
        automovel.setValorDiaria(SimulacaoService.arredondar(dto.valorDiaria()));
    }

    /** Valida todos os campos e devolve as mensagens por campo. */
    private void validar(AutomovelDTO dto, Long idAtual) {
        Map<String, String> erros = new LinkedHashMap<>();

        if (vazio(dto.marca())) erros.put("marca", "A marca é obrigatória.");
        if (vazio(dto.modelo())) erros.put("modelo", "O modelo é obrigatório.");
        if (vazio(dto.matricula())) erros.put("matricula", "A matrícula é obrigatória.");

        int anoMaximo = Year.now().getValue() + 1;
        if (dto.ano() == null) {
            erros.put("ano", "O ano é obrigatório.");
        } else if (dto.ano() < 1900 || dto.ano() > anoMaximo) {
            erros.put("ano", "O ano deve estar entre 1900 e " + anoMaximo + ".");
        }

        if (vazio(dto.placa())) {
            erros.put("placa", "A placa é obrigatória.");
        } else if (!Validadores.placaValida(dto.placa())) {
            erros.put("placa", "Placa inválida. Use o formato ABC-1234 ou ABC1D23 (Mercosul).");
        } else {
            String placa = Validadores.normalizarPlaca(dto.placa());
            boolean duplicada = idAtual == null
                    ? repository.existsByPlaca(placa)
                    : repository.existsByPlacaAndIdNot(placa, idAtual);
            if (duplicada) erros.put("placa", "Já existe um automóvel cadastrado com esta placa.");
        }

        if (dto.valorDiaria() == null || dto.valorDiaria() <= 0) {
            erros.put("valorDiaria", "O valor da diária deve ser maior que zero.");
        } else if (dto.valorDiaria() > MAX_DIARIA) {
            erros.put("valorDiaria", "O valor da diária deve ser de no máximo R$ 100.000,00.");
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }

    private boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
