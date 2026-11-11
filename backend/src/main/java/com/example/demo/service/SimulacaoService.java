package com.example.demo.service;

import com.example.demo.dto.SimulacaoDTO;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.repository.PedidoAluguelRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simulação de financiamento pela tabela Price. A taxa informada é anual (% a.a.) e convertida
 * para a taxa mensal equivalente. Também compara a parcela com a renda total do cliente.
 */
@Service
public class SimulacaoService {

    /** Alerta quando a parcela compromete mais de 30% da renda total do cliente. */
    public static final double LIMITE_COMPROMETIMENTO_RENDA = 0.30;
    public static final int MAX_PARCELAS = 120;
    public static final double MAX_JUROS_ANUAL = 200.0;

    private final PedidoAluguelRepository pedidoRepository;

    public SimulacaoService(PedidoAluguelRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    /** Taxa mensal efetiva equivalente a uma taxa anual em %. */
    public static double taxaMensal(double jurosAnualPercentual) {
        return Math.pow(1 + jurosAnualPercentual / 100.0, 1.0 / 12.0) - 1;
    }

    /** Parcela fixa (Price): PMT = PV * i / (1 - (1 + i)^-n). Sem juros: PV / n. */
    public static double parcela(double valorFinanciado, int parcelas, double jurosAnualPercentual) {
        double i = taxaMensal(jurosAnualPercentual);
        double pmt = i == 0
                ? valorFinanciado / parcelas
                : valorFinanciado * i / (1 - Math.pow(1 + i, -parcelas));
        return arredondar(pmt);
    }

    public static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /** Valida os parâmetros de um financiamento. */
    public static void validarParametros(Integer parcelas, Double jurosAnual) {
        Map<String, String> erros = new LinkedHashMap<>();
        if (parcelas == null || parcelas < 1 || parcelas > MAX_PARCELAS) {
            erros.put("numeroParcelas", "O número de parcelas deve estar entre 1 e " + MAX_PARCELAS + ".");
        }
        if (jurosAnual == null || jurosAnual < 0 || jurosAnual > MAX_JUROS_ANUAL) {
            erros.put("taxaJuros", "A taxa de juros anual deve estar entre 0 e " + (int) MAX_JUROS_ANUAL + "%.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }

    /**
     * Simula o financiamento de um pedido. Se {@code valorFinanciado} for nulo, usa
     * valor total do pedido menos a entrada.
     */
    public SimulacaoDTO simular(Long pedidoId, Double entrada, Double valorFinanciado,
                                Integer parcelas, Double jurosAnual) {
        PedidoAluguel pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        validarParametros(parcelas, jurosAnual);

        double valorEntrada = entrada == null ? 0 : entrada;
        Double total = pedido.getValorTotal();
        if (valorEntrada < 0) {
            throw new ValidacaoException(Map.of("valorEntrada", "A entrada não pode ser negativa."));
        }
        double financiado = valorFinanciado != null ? valorFinanciado : (total != null ? total - valorEntrada : 0);
        if (financiado <= 0) {
            throw new ValidacaoException(Map.of("valorFinanciado",
                    "Informe um valor financiado maior que zero (o pedido pode não ter valor total)."));
        }

        double parcela = parcela(financiado, parcelas, jurosAnual);
        double renda = pedido.getCliente().getRendimentosAuferidos() == null ? 0
                : pedido.getCliente().getRendimentosAuferidos().stream().mapToDouble(Double::doubleValue).sum();
        double comprometimento = renda > 0 ? parcela / renda : 1.0;

        return new SimulacaoDTO(
                total, arredondar(valorEntrada), arredondar(financiado), parcelas, jurosAnual,
                Math.round(taxaMensal(jurosAnual) * 1_000_000.0) / 10_000.0, // % a.m. com 4 casas
                parcela, arredondar(parcela * parcelas), arredondar(renda),
                Math.round(comprometimento * 10_000.0) / 10_000.0,
                LIMITE_COMPROMETIMENTO_RENDA,
                renda <= 0 || comprometimento > LIMITE_COMPROMETIMENTO_RENDA);
    }
}
