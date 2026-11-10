package com.example.demo.service;

import com.example.demo.exception.CredenciaisInvalidasException;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final String PREFIXO_BCRYPT = "$2";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    /**
     * Valida login e senha. A mensagem de falha é a mesma para usuário inexistente e senha errada.
     * Contas antigas com senha em texto puro são aceitas uma única vez e migradas para BCrypt.
     */
    @Transactional
    public Usuario autenticar(String login, String senha) {
        if (login == null || senha == null) {
            throw new CredenciaisInvalidasException();
        }
        Usuario usuario = repository.findByLogin(login).orElseThrow(CredenciaisInvalidasException::new);
        String armazenada = usuario.getSenha();

        if (armazenada != null && armazenada.startsWith(PREFIXO_BCRYPT)) {
            if (!passwordEncoder.matches(senha, armazenada)) {
                throw new CredenciaisInvalidasException();
            }
        } else {
            if (armazenada == null || !armazenada.equals(senha)) {
                throw new CredenciaisInvalidasException();
            }
            usuario.setSenha(passwordEncoder.encode(senha)); // migração transparente para hash
            repository.save(usuario);
        }
        return usuario;
    }

    public String codificar(String senhaPura) {
        return passwordEncoder.encode(senhaPura);
    }
}
