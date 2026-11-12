package com.example.demo.config;

import com.example.demo.model.Automovel;
import com.example.demo.model.Cliente;
import com.example.demo.model.Empresa;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.validation.Validadores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoInitializerTest {

    @Mock UsuarioRepository usuarios;
    @Mock AutomovelRepository automoveis;

    PasswordEncoder encoder = new BCryptPasswordEncoder();

    private CommandLineRunner runner() {
        return new DemoInitializer().carregarContasDemo(usuarios, automoveis, encoder);
    }

    @Test
    void criaClienteBancoEmpresaEVeiculoQuandoNaoExistem() throws Exception {
        when(usuarios.findByLogin(anyString())).thenReturn(Optional.empty());
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(automoveis.existsByPlaca("DEM1O26")).thenReturn(false);

        runner().run();

        ArgumentCaptor<Usuario> salvos = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios, times(3)).save(salvos.capture());
        assertEquals(java.util.List.of("CLIENTE", "BANCO", "EMPRESA"),
                salvos.getAllValues().stream().map(Usuario::getTipo).toList());

        Cliente cliente = (Cliente) salvos.getAllValues().get(0);
        assertTrue(Validadores.cpfValido(cliente.getCpf()), "o CPF de demonstração precisa ser válido");
        assertTrue(encoder.matches(DemoInitializer.SENHA_DEMO, cliente.getSenha()), "senha deve estar em hash BCrypt");
        assertNotEquals(DemoInitializer.SENHA_DEMO, cliente.getSenha());
        assertEquals(1, cliente.getRendimentosAuferidos().size());

        ArgumentCaptor<Automovel> carro = ArgumentCaptor.forClass(Automovel.class);
        verify(automoveis).save(carro.capture());
        assertTrue(Validadores.placaValida(carro.getValue().getPlaca()));
        assertEquals(DemoInitializer.LOGIN_EMPRESA, carro.getValue().getProprietario().getLogin());
        assertNotNull(carro.getValue().getValorDiaria());
    }

    @Test
    void ehIdempotenteQuandoTudoJaExiste() throws Exception {
        Cliente cliente = new Cliente();
        Empresa empresa = new Empresa();
        empresa.setLogin(DemoInitializer.LOGIN_EMPRESA);
        when(usuarios.findByLogin(DemoInitializer.LOGIN_CLIENTE)).thenReturn(Optional.of(cliente));
        when(usuarios.findByLogin(DemoInitializer.LOGIN_BANCO)).thenReturn(Optional.of(cliente));
        when(usuarios.findByLogin(DemoInitializer.LOGIN_EMPRESA)).thenReturn(Optional.of(empresa));
        when(automoveis.existsByPlaca("DEM1O26")).thenReturn(true);

        runner().run();

        verify(usuarios, never()).save(any());
        verify(automoveis, never()).save(any());
    }
}
