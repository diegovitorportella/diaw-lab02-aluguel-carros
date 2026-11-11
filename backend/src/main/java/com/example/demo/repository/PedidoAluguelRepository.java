package com.example.demo.repository;

import com.example.demo.model.PedidoAluguel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoAluguelRepository extends JpaRepository<PedidoAluguel, Long> {

    // Associações de valor único vêm na mesma consulta; as coleções vêm em lote (batch fetch).
    @Override
    @EntityGraph(attributePaths = {"cliente", "automovel", "automovel.proprietario", "agenteAvaliador", "contrato"})
    List<PedidoAluguel> findAll();

    @EntityGraph(attributePaths = {"cliente", "automovel", "automovel.proprietario", "agenteAvaliador", "contrato"})
    List<PedidoAluguel> findByClienteId(Long clienteId);

    boolean existsByClienteId(Long clienteId);
    boolean existsByAutomovelId(Long automovelId);
}
