package com.example.demo.service;

import com.example.demo.dto.AutomovelDTO;
import com.example.demo.exception.AcessoNegadoException;
import com.example.demo.exception.ValidacaoException;
import com.example.demo.model.Automovel;
import com.example.demo.model.Cliente;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.PedidoAluguelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Year;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutomovelServiceTest {

    @Mock AutomovelRepository repository;
    @Mock PedidoAluguelRepository pedidoRepository;
    @Mock UsuarioService usuarioService;

    AutomovelService service;
    Cliente dono;

    @BeforeEach
    void setUp() {
        service = new AutomovelService(repository, pedidoRepository, usuarioService);
        dono = new Cliente();
        dono.setId(1L);
        when(usuarioService.buscar(1L)).thenReturn(dono);
        when(repository.save(any(Automovel.class))).thenAnswer(i -> i.getArgument(0));
    }

    private AutomovelDTO dto(String placa, Integer ano, Double diaria) {
        return new AutomovelDTO("M-1", ano, "Hyundai", "Ioniq 6", placa, diaria);
    }

    @Test
    void salvaComProprietarioDoUsuarioEPlacaNormalizada() {
        Automovel salvo = service.salvar(1L, dto("abc-1234", 2024, 180.556));
        assertSame(dono, salvo.getProprietario());
        assertEquals("ABC1234", salvo.getPlaca());
        assertEquals(180.56, salvo.getValorDiaria(), 0.001);
    }

    @Test
    void aceitaPlacaMercosul() {
        assertEquals("ABC1D23", service.salvar(1L, dto("abc1d23", 2024, 100.0)).getPlaca());
    }

    @Test
    void rejeitaPlacaInvalida() {
        ValidacaoException e = assertThrows(ValidacaoException.class, () -> service.salvar(1L, dto("XX-99", 2024, 100.0)));
        assertTrue(e.getCampos().containsKey("placa"));
    }

    @Test
    void rejeitaPlacaDuplicadaNoCadastroEMasPermiteMantelaNaEdicao() {
        when(repository.existsByPlaca("ABC1234")).thenReturn(true);
        ValidacaoException e = assertThrows(ValidacaoException.class, () -> service.salvar(1L, dto("ABC-1234", 2024, 100.0)));
        assertTrue(e.getCampos().get("placa").contains("já existe") || e.getCampos().get("placa").contains("Já existe"));

        // edição do próprio veículo: a checagem exclui o próprio id, então não há duplicidade
        Automovel existente = new Automovel();
        existente.setId(7L);
        existente.setProprietario(dono);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.existsByPlacaAndIdNot("ABC1234", 7L)).thenReturn(false);
        assertDoesNotThrow(() -> service.atualizar(7L, 1L, dto("ABC-1234", 2024, 100.0)));
    }

    @Test
    void validaAnoEDiaria() {
        int anoMax = Year.now().getValue() + 1;
        assertDoesNotThrow(() -> service.salvar(1L, dto("ABC1234", anoMax, 100.0)));

        assertTrue(assertThrows(ValidacaoException.class, () -> service.salvar(1L, dto("ABC1234", 1899, 100.0)))
                .getCampos().containsKey("ano"));
        assertTrue(assertThrows(ValidacaoException.class, () -> service.salvar(1L, dto("ABC1234", anoMax + 1, 100.0)))
                .getCampos().containsKey("ano"));
        assertTrue(assertThrows(ValidacaoException.class, () -> service.salvar(1L, dto("ABC1234", 2024, 0.0)))
                .getCampos().containsKey("valorDiaria"));
        assertTrue(assertThrows(ValidacaoException.class, () -> service.salvar(1L, dto("ABC1234", 2024, null)))
                .getCampos().containsKey("valorDiaria"));
        assertTrue(assertThrows(ValidacaoException.class, () -> service.salvar(1L, dto("ABC1234", 2024, 100_000.01)))
                .getCampos().containsKey("valorDiaria"));
    }

    @Test
    void acumulaOsErrosDeTodosOsCampos() {
        ValidacaoException e = assertThrows(ValidacaoException.class,
                () -> service.salvar(1L, new AutomovelDTO(" ", null, "", null, "", null)));
        assertTrue(e.getCampos().keySet().containsAll(java.util.List.of("matricula", "ano", "marca", "modelo", "placa", "valorDiaria")));
    }

    @Test
    void somenteOProprietarioAlteraOuExclui() {
        Automovel existente = new Automovel();
        existente.setId(7L);
        existente.setProprietario(dono);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));

        assertThrows(AcessoNegadoException.class, () -> service.atualizar(7L, 2L, dto("ABC1234", 2024, 100.0)));
        assertThrows(AcessoNegadoException.class, () -> service.excluir(7L, 2L));
    }

    @Test
    void naoExcluiAutomovelComPedidos() {
        Automovel existente = new Automovel();
        existente.setId(7L);
        existente.setProprietario(dono);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(pedidoRepository.existsByAutomovelId(7L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.excluir(7L, 1L));
        verify(repository, never()).delete(any());

        when(pedidoRepository.existsByAutomovelId(7L)).thenReturn(false);
        service.excluir(7L, 1L);
        verify(repository).delete(existente);
    }
}
