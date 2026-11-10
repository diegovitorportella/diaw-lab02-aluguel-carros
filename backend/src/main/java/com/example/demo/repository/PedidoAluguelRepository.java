package com.example.demo.repository;

import com.example.demo.model.PedidoAluguel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoAluguelRepository extends JpaRepository<PedidoAluguel, Long> {
    List<PedidoAluguel> findByClienteId(Long clienteId);
    boolean existsByClienteId(Long clienteId);
    boolean existsByAutomovelId(Long automovelId);
}
