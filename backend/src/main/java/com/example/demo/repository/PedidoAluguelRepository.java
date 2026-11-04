package com.example.demo.repository;

import com.example.demo.model.PedidoAluguel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoAluguelRepository extends JpaRepository<PedidoAluguel, Long> {
}