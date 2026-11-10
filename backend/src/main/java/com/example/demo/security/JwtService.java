package com.example.demo.security;

import com.example.demo.model.Agente;
import com.example.demo.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expiracaoMinutos;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.expiration-minutes:480}") long expiracaoMinutos) {
        this.encoder = encoder;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("driveja")
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES))
                .subject(String.valueOf(usuario.getId()))
                .claim("tipo", usuario.getTipo())
                .claim("roles", papeis(usuario))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Todo agente (banco ou empresa) também recebe o papel agregador AGENTE. */
    private List<String> papeis(Usuario usuario) {
        List<String> papeis = new ArrayList<>();
        papeis.add(usuario.getTipo());
        if (usuario instanceof Agente) {
            papeis.add("AGENTE");
        }
        return papeis;
    }

    /** Extrai o id do usuário autenticado a partir do token. */
    public static Long idDo(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
