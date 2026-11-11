package com.example.demo.config;

import com.example.demo.model.Banco;
import com.example.demo.model.Empresa;
import com.example.demo.model.PedidoAluguel;
import com.example.demo.repository.PedidoAluguelRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.PedidoAluguelService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Carga e ajustes de dados do ambiente de desenvolvimento (perfil "dev"):
 * agentes de demonstração (não há tela de cadastro de agentes), com senhas em hash BCrypt,
 * e a migração de pedidos do fluxo antigo.
 */
@Configuration
@Profile({"dev", "seed"})
public class DataInitializer {

    @Bean
    CommandLineRunner seedAgentes(UsuarioRepository usuarios, PasswordEncoder encoder,
                                  @Value("${app.seed.senha}") String senhaSeed) {
        return args -> {
            if (usuarios.findByLogin("banco.agente").isEmpty()) {
                Banco banco = new Banco();
                banco.setLogin("banco.agente");
                banco.setSenha(encoder.encode(senhaSeed));
                banco.setCnpj("00.000.000/0001-00");
                banco.setRazaoSocial("Banco Agente S.A.");
                usuarios.save(banco);
            }
            if (usuarios.findByLogin("empresa.agente").isEmpty()) {
                Empresa empresa = new Empresa();
                empresa.setLogin("empresa.agente");
                empresa.setSenha(encoder.encode(senhaSeed));
                empresa.setCnpj("11.111.111/0001-11");
                empresa.setRazaoSocial("Locadora Agente Ltda.");
                usuarios.save(empresa);
            }
        };
    }

    /**
     * No fluxo antigo a aprovação já gerava o contrato e o pedido ficava APROVADO. No fluxo atual
     * o contrato executado leva o pedido a CONCLUIDO; este ajuste (idempotente) migra esses pedidos.
     */
    @Bean
    CommandLineRunner migrarPedidosComContrato(PedidoAluguelRepository pedidos, TransactionTemplate transacao) {
        return args -> transacao.executeWithoutResult(status -> {
            for (PedidoAluguel pedido : pedidos.findAll()) {
                if (PedidoAluguelService.APROVADO.equals(pedido.getStatus()) && pedido.getContrato() != null) {
                    pedido.setStatus(PedidoAluguelService.CONCLUIDO);
                    pedido.registrar(PedidoAluguelService.CONCLUIDO, "Contrato executado (registro migrado)", null);
                    pedidos.save(pedido);
                }
            }
        });
    }
}
