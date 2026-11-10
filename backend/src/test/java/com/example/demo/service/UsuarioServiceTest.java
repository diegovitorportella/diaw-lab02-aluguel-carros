package com.example.demo.service;

import com.example.demo.exception.CredenciaisInvalidasException;
import com.example.demo.model.Cliente;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository repository;

    PasswordEncoder encoder = new BCryptPasswordEncoder();
    UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, encoder);
    }

    private Usuario usuario(String senhaArmazenada) {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setLogin("ana");
        c.setSenha(senhaArmazenada);
        return c;
    }

    @Test
    void autenticaComSenhaComHash() {
        Usuario u = usuario(encoder.encode("segredo123"));
        when(repository.findByLogin("ana")).thenReturn(Optional.of(u));

        assertSame(u, service.autenticar("ana", "segredo123"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaSenhaErrada() {
        when(repository.findByLogin("ana")).thenReturn(Optional.of(usuario(encoder.encode("segredo123"))));
        assertThrows(CredenciaisInvalidasException.class, () -> service.autenticar("ana", "errada"));
    }

    @Test
    void rejeitaUsuarioInexistenteComAMesmaExcecao() {
        when(repository.findByLogin("ninguem")).thenReturn(Optional.empty());
        assertThrows(CredenciaisInvalidasException.class, () -> service.autenticar("ninguem", "x"));
    }

    @Test
    void rejeitaCamposNulos() {
        assertThrows(CredenciaisInvalidasException.class, () -> service.autenticar(null, "x"));
        assertThrows(CredenciaisInvalidasException.class, () -> service.autenticar("ana", null));
    }

    @Test
    void migraSenhaAntigaEmTextoPuroParaHash() {
        Usuario u = usuario("senha-antiga");
        when(repository.findByLogin("ana")).thenReturn(Optional.of(u));

        service.autenticar("ana", "senha-antiga");

        assertTrue(u.getSenha().startsWith("$2"), "a senha deveria ter sido convertida para BCrypt");
        assertTrue(encoder.matches("senha-antiga", u.getSenha()));
        verify(repository).save(u);
    }

    @Test
    void senhaAntigaErradaNaoMigra() {
        Usuario u = usuario("senha-antiga");
        when(repository.findByLogin("ana")).thenReturn(Optional.of(u));

        assertThrows(CredenciaisInvalidasException.class, () -> service.autenticar("ana", "outra"));
        assertEquals("senha-antiga", u.getSenha());
        verify(repository, never()).save(any());
    }
}
