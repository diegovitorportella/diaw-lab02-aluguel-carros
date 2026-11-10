package com.example.demo.service;

import com.example.demo.dto.PedidoDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Agente;
import com.example.demo.model.Automovel;
import com.example.demo.model.Cliente;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PedidoAluguelService {

    public static final String PENDENTE = "PENDENTE";
    public static final String APROVADO = "APROVADO";
    public static final String REJEITADO = "REJEITADO";
    public static final String CANCELADO = "CANCELADO";

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

    /** O cliente do pedido é o usuário autenticado (deve ser do tipo Cliente). */
    @Transactional
    public PedidoAluguel criar(Long clienteId, PedidoDTO dto) {
        if (!(usuarioService.buscar(clienteId) instanceof Cliente cliente)) {
            throw new AcessoNegadoException("Apenas clientes podem criar pedidos de aluguel.");
        }
        Automovel automovel = buscarAutomovel(dto);
        if (automovel == null) {
            throw new IllegalArgumentException("O automóvel do pedido é obrigatório.");
        }
        validarPeriodo(dto);

        PedidoAluguel pedido = new PedidoAluguel();
        pedido.setCliente(cliente);
        pedido.setAutomovel(automovel);
        pedido.setDataInicio(dto.dataInicio());
        pedido.setDataFim(dto.dataFim());
        pedido.setObservacao(dto.observacao());
        pedido.setStatus(PENDENTE);
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
     * Modifica um pedido. O cliente só pode alterar os próprios pedidos; agentes podem alterar
     * qualquer pedido. Em ambos os casos, apenas enquanto o pedido está pendente (em análise).
     */
    @Transactional
    public PedidoAluguel modificar(Long id, Long usuarioId, PedidoDTO dto) {
        PedidoAluguel pedido = buscarPedido(id);
        validarPermissao(pedido, usuarioService.buscar(usuarioId));

        if (!PENDENTE.equals(pedido.getStatus())) {
            throw new IllegalStateException("Apenas pedidos pendentes (em análise) podem ser modificados.");
        }

        Automovel automovel = buscarAutomovel(dto);
        if (automovel != null) {
            pedido.setAutomovel(automovel);
        }
        validarPeriodo(dto);
        pedido.setDataInicio(dto.dataInicio());
        pedido.setDataFim(dto.dataFim());
        pedido.setObservacao(dto.observacao());
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

        pedido.setParecerFinanceiro(parecer.toUpperCase());
        pedido.setAgenteAvaliador(agente);
        pedido.setStatus("POSITIVO".equalsIgnoreCase(parecer) ? APROVADO : REJEITADO);
        return repository.save(pedido);
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

    private void validarPeriodo(PedidoDTO dto) {
        if (dto.dataInicio() != null && dto.dataFim() != null && dto.dataFim().isBefore(dto.dataInicio())) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }
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
