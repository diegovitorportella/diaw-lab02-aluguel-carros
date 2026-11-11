package com.example.demo.exception;

import java.util.Map;

/**
 * Falha de validação com a mensagem de cada campo. Estende IllegalArgumentException,
 * portanto continua resultando em HTTP 400 mesmo onde só se trata a exceção genérica.
 */
public class ValidacaoException extends IllegalArgumentException {

    private final Map<String, String> campos;

    public ValidacaoException(Map<String, String> campos) {
        super(String.join(" ", campos.values()));
        this.campos = Map.copyOf(campos);
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
