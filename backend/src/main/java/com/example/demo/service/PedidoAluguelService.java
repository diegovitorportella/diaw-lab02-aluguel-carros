package com.example.demo.service;

import com.example.demo.dto.PedidoDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.Agente;
import com.example.demo.model.Automovel;
import com.example.demo.model.Cliente;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ciclo de vida do pedido: PENDENTE → APROVADO → CONCLUIDO (contrato executado),
 * com os desvios REJEITADO (parecer negativo) e CANCELADO (cliente desiste).
 */
@Service
public class PedidoAluguelService {

    public static final String PENDENTE = "PENDENTE";
    public static final String APROVADO = "APROVADO";
    public static final String REJEITADO = "REJEITADO";
    public static final String CANCELADO = "CANCELADO";
    public static final String CONCLUIDO = "CONCLUIDO";

    private final PedidoAluguelRepository repository;
    private final AutomovelRepository automovelRepository;
    private final UsuarioService usuarioService;

    public PedidoAluguelService(PedidoAluguelRepository repository,
                                AutomovelRepository automovelRepository,
                                UsuarioService usuarioService) {
        this.repository = repository;
        this.automovelRepository = automovelRepository;
        this.usuarioService = usuarioService;
    }

    /** Valor total do aluguel: diárias (período inclusivo) × valor da diária, ou nulo se não der para calcular. */
    public static Double calcularTotal(Automovel automovel, LocalDate inicio, LocalDate fim) {
        if (automovel == null || automovel.getValorDiaria() == null || inicio == null || fim == null) {
            return null;
        }
        long diarias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        return SimulacaoService.arredondar(diarias * automovel.getValorDiaria());
    }

    /** O cliente do pedido é o usuário autenticado (deve ser do tipo Cliente). */
    @Transactional
    public PedidoAluguel criar(Long clienteId, PedidoDTO dto) {
        if (!(usuarioService.buscar(clienteId) instanceof Cliente cliente)) {
            throw new AcessoNegadoException("Apenas clientes podem criar pedidos de aluguel.");
        }
        Automovel automovel = buscarAutomovel(dto);

        Map<String, String> erros = new LinkedHashMap<>();
        if (automovel == null) {
            erros.put("automovel", "O automóvel do pedido é obrigatório.");
        } else {
            validarProprietario(automovel, cliente, erros);
        }
        if (dto.dataInicio() == null) {
            erros.put("dataInicio", "Informe a data inicial.");
        } else if (dto.dataInicio().isBefore(LocalDate.now())) {
            erros.put("dataInicio", "A data inicial não pode ser anterior a hoje.");
        }
        if (dto.dataFim() == null) {
            erros.put("dataFim", "Informe a data final.");
        } else if (dto.dataInicio() != null && dto.dataFim().isBefore(dto.dataInicio())) {
            erros.put("dataFim", "A data final não pode ser anterior à data inicial.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        PedidoAluguel pedido = new PedidoAluguel();
        pedido.setCliente(cliente);
        pedido.setAutomovel(automovel);
        pedido.setDataInicio(dto.dataInicio());
        pedido.setDataFim(dto.dataFim());
        pedido.setObservacao(dto.observacao());
        pedido.setValorTotal(calcularTotal(automovel, dto.dataInicio(), dto.dataFim()));
        pedido.setStatus(PENDENTE);
        pedido.registrar(PENDENTE, "Pedido solicitado", cliente);
        return repository.save(pedido);
    }

    /** Cliente vê apenas os próprios pedidos; agentes veem todos. Filtro opcional por status. */
    public List<PedidoAluguel> listarPara(Long usuarioId, String status) {
        Usuario usuario = usuarioService.buscar(usuarioId);
        List<PedidoAluguel> pedidos = usuario instanceof Agente
                ? repository.findAll()
                : repository.findByClienteId(usuarioId);
        if (status == null || status.isBlank()) {
            return pedidos;
        }
        return pedidos.stream().filter(p -> status.equalsIgnoreCase(p.getStatus())).toList();
    }

    public PedidoAluguel buscar(Long id, Long usuarioId) {
        PedidoAluguel pedido = buscarPedido(id);
        validarPermissao(pedido, usuarioService.buscar(usuarioId));
        return pedido;
    }

    /**
     * Modifica um pedido (atualização parcial: campos nulos mantêm o valor atual). O cliente só pode
     * alterar os próprios pedidos; agentes podem alterar qualquer pedido. Apenas enquanto pendente.
     */
    @Transactional
    public PedidoAluguel modificar(Long id, Long usuarioId, PedidoDTO dto) {
        PedidoAluguel pedido = buscarPedido(id);
        Usuario usuario = usuarioService.buscar(usuarioId);
        validarPermissao(pedido, usuario);

        if (!PENDENTE.equals(pedido.getStatus())) {
            throw new IllegalStateException("Apenas pedidos pendentes (em análise) podem ser modificados.");
        }

        Automovel novoAutomovel = buscarAutomovel(dto);
        Automovel automovel = novoAutomovel != null ? novoAutomovel : pedido.getAutomovel();
        LocalDate inicio = dto.dataInicio() != null ? dto.dataInicio() : pedido.getDataInicio();
        LocalDate fim = dto.dataFim() != null ? dto.dataFim() : pedido.getDataFim();

        Map<String, String> erros = new LinkedHashMap<>();
        if (novoAutomovel != null) {
            validarProprietario(novoAutomovel, pedido.getCliente(), erros);
        }
        if (dto.dataInicio() != null && !dto.dataInicio().equals(pedido.getDataInicio())
                && dto.dataInicio().isBefore(LocalDate.now())) {
            erros.put("dataInicio", "A data inicial não pode ser anterior a hoje.");
        }
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            erros.put("dataFim", "A data final não pode ser anterior à data inicial.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        pedido.setAutomovel(automovel);
        pedido.setDataInicio(inicio);
        pedido.setDataFim(fim);
        if (dto.observacao() != null) {
            pedido.setObservacao(dto.observacao());
        }
        pedido.setValorTotal(calcularTotal(automovel, inicio, fim));
        pedido.registrar(PENDENTE, "Pedido modificado", usuario);
        return repository.save(pedido);
    }

    /** Cancela um pedido ativo (pendente, ou aprovado ainda sem contrato). Apenas o cliente dono. */
    @Transactional
    public PedidoAluguel cancelar(Long id, Long usuarioId) {
        PedidoAluguel pedido = buscarPedido(id);
        Usuario usuario = usuarioService.buscar(usuarioId);
        if (!(usuario instanceof Cliente) || !pedido.getCliente().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("Apenas o cliente dono do pedido pode cancelá-lo.");
        }

        boolean ativo = PENDENTE.equals(pedido.getStatus())
                || (APROVADO.equals(pedido.getStatus()) && pedido.getContrato() == null);
        if (!ativo) {
            throw new IllegalStateException("Este pedido não pode mais ser cancelado.");
        }

        pedido.setStatus(CANCELADO);
        pedido.registrar(CANCELADO, "Pedido cancelado pelo cliente", usuario);
        return repository.save(pedido);
    }

    @Transactional
    public PedidoAluguel avaliarPedido(Long id, String parecer, Long agenteId) {
        PedidoAluguel pedido = buscarPedido(id);
        if (!(usuarioService.buscar(agenteId) instanceof Agente agente)) {
            throw new AcessoNegadoException("Apenas agentes podem avaliar pedidos.");
        }
        if (!PENDENTE.equals(pedido.getStatus())) {
            throw new IllegalStateException("Apenas pedidos pendentes podem ser avaliados.");
        }
        if (!"POSITIVO".equalsIgnoreCase(parecer) && !"NEGATIVO".equalsIgnoreCase(parecer)) {
            throw new IllegalArgumentException("Parecer deve ser POSITIVO ou NEGATIVO.");
        }

        boolean positivo = "POSITIVO".equalsIgnoreCase(parecer);
        pedido.setParecerFinanceiro(parecer.toUpperCase());
        pedido.setAgenteAvaliador(agente);
        pedido.setStatus(positivo ? APROVADO : REJEITADO);
        pedido.registrar(pedido.getStatus(),
                positivo ? "Pedido aprovado na análise financeira" : "Pedido rejeitado na análise financeira", agente);
        return repository.save(pedido);
    }

    private void validarProprietario(Automovel automovel, Cliente cliente, Map<String, String> erros) {
        if (automovel.getProprietario() != null && automovel.getProprietario().getId() != null
                && automovel.getProprietario().getId().equals(cliente.getId())) {
            erros.put("automovel", "Você não pode alugar o próprio veículo.");
        }
    }

    private void validarPermissao(PedidoAluguel pedido, Usuario usuario) {
        if (usuario instanceof Agente) {
            return;
        }
        if (usuario instanceof Cliente && pedido.getCliente().getId().equals(usuario.getId())) {
            return;
        }
        throw new AcessoNegadoException("Você não tem permissão para acessar este pedido.");
    }

    private Automovel buscarAutomovel(PedidoDTO dto) {
        if (dto.automovel() == null || dto.automovel().id() == null) {
            return null;
        }
        return automovelRepository.findById(dto.automovel().id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Automóvel não encontrado."));
    }

    private PedidoAluguel buscarPedido(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }
}
