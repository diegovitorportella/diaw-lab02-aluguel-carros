package com.example.demo.config;

import com.example.demo.model.Automovel;
import com.example.demo.model.Banco;
import com.example.demo.model.Cliente;
import com.example.demo.model.Empresa;
import com.example.demo.repository.AutomovelRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Contas de demonstração para a versão publicada (ative com DEMO_ENABLED=true): um cliente, um banco e
 * uma empresa com senha pública, mais um veículo para alugar. A senha é conhecida de propósito, pois o
 * objetivo é deixar qualquer visitante explorar o sistema; não ative em ambiente com dados reais.
 * A carga é idempotente: só cria o que ainda não existe.
 */
@Configuration
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoInitializer {

    public static final String SENHA_DEMO = "demo12345";
    public static final String LOGIN_CLIENTE = "demo.cliente";
    public static final String LOGIN_BANCO = "demo.banco";
    public static final String LOGIN_EMPRESA = "demo.empresa";

    @Bean
    CommandLineRunner carregarContasDemo(UsuarioRepository usuarios, AutomovelRepository automoveis,
                                         PasswordEncoder encoder) {
        return args -> {
            String senha = encoder.encode(SENHA_DEMO);

            if (usuarios.findByLogin(LOGIN_CLIENTE).isEmpty()) {
                Cliente cliente = new Cliente();
                cliente.setLogin(LOGIN_CLIENTE);
                cliente.setSenha(senha);
                cliente.setNome("Cliente Demonstração");
                cliente.setCpf("52998224725"); // CPF fictício com dígitos verificadores válidos
                cliente.setRg("MG1234567");
                cliente.setEndereco("Rua das Flores, 100, Centro, Belo Horizonte");
                cliente.setProfissao("Analista");
                cliente.setEntidadesEmpregadoras(List.of("Empresa Exemplo Ltda."));
                cliente.setRendimentosAuferidos(List.of(9000.0));
                usuarios.save(cliente);
            }

            if (usuarios.findByLogin(LOGIN_BANCO).isEmpty()) {
                Banco banco = new Banco();
                banco.setLogin(LOGIN_BANCO);
                banco.setSenha(senha);
                banco.setCnpj("22.222.222/0001-22");
                banco.setRazaoSocial("Banco Demonstração S.A.");
                usuarios.save(banco);
            }

            Empresa empresa = (Empresa) usuarios.findByLogin(LOGIN_EMPRESA).orElse(null);
            if (empresa == null) {
                empresa = new Empresa();
                empresa.setLogin(LOGIN_EMPRESA);
                empresa.setSenha(senha);
                empresa.setCnpj("33.333.333/0001-33");
                empresa.setRazaoSocial("Locadora Demonstração Ltda.");
                empresa = usuarios.save(empresa);
            }

            // Veículo de demonstração, para o cliente ter o que alugar
            if (!automoveis.existsByPlaca("DEM1O26")) {
                Automovel carro = new Automovel();
                carro.setMarca("Fiat");
                carro.setModelo("Argo Drive");
                carro.setAno(2024);
                carro.setPlaca("DEM1O26");
                carro.setMatricula("DEMO-0001");
                carro.setValorDiaria(180.0);
                carro.setProprietario(empresa);
                automoveis.save(carro);
            }
        };
    }
}
