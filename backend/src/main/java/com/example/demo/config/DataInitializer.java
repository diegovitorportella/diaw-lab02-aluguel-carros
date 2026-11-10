package com.example.demo.config;

import com.example.demo.model.Banco;
import com.example.demo.model.Empresa;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cria agentes de demonstração (não há tela de cadastro de agentes).
 * Ativo apenas no perfil "dev"; as senhas são gravadas com hash BCrypt.
 */
@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    CommandLineRunner seedAgentes(UsuarioRepository usuarios, PasswordEncoder encoder) {
        return args -> {
            if (usuarios.findByLogin("banco.agente").isEmpty()) {
                Banco banco = new Banco();
                banco.setLogin("banco.agente");
                banco.setSenha(encoder.encode("password123"));
                banco.setCnpj("00.000.000/0001-00");
                banco.setRazaoSocial("Banco Agente S.A.");
                usuarios.save(banco);
            }
            if (usuarios.findByLogin("empresa.agente").isEmpty()) {
                Empresa empresa = new Empresa();
                empresa.setLogin("empresa.agente");
                empresa.setSenha(encoder.encode("password123"));
                empresa.setCnpj("11.111.111/0001-11");
                empresa.setRazaoSocial("Locadora Agente Ltda.");
                usuarios.save(empresa);
            }
        };
    }
}
