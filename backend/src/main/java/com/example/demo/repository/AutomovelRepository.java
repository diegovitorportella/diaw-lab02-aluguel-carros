package com.example.demo.repository;

import com.example.demo.model.Automovel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutomovelRepository extends JpaRepository<Automovel, Long> {

    @Override
    @EntityGraph(attributePaths = "proprietario")
    List<Automovel> findAll();

    @EntityGraph(attributePaths = "proprietario")
    List<Automovel> findByProprietarioId(Long proprietarioId);

    boolean existsByProprietarioId(Long proprietarioId);
    boolean existsByPlaca(String placa);
    boolean existsByPlacaAndIdNot(String placa, Long id);
}
