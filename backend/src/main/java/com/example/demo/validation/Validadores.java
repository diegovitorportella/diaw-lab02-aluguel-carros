package com.example.demo.validation;

import java.util.regex.Pattern;

/** Validações e normalizações de documentos e identificadores (CPF, RG e placa). */
public final class Validadores {

    // Placa antiga (ABC1234) ou Mercosul (ABC1D23)
    private static final Pattern PLACA = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

    private Validadores() {}

    public static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    /** Valida o CPF pelos dígitos verificadores (aceita com ou sem máscara). */
    public static boolean cpfValido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digitoCpf(d, 9) == d.charAt(9) - '0' && digitoCpf(d, 10) == d.charAt(10) - '0';
    }

    private static int digitoCpf(String d, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (d.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** RG: 5 a 14 caracteres alfanuméricos (o dígito final pode ser X). */
    public static boolean rgValido(String rg) {
        String n = normalizarRg(rg);
        return n.length() >= 5 && n.length() <= 14;
    }

    public static String normalizarRg(String rg) {
        return rg == null ? "" : rg.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
    }

    public static String normalizarPlaca(String placa) {
        return placa == null ? "" : placa.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
    }

    public static boolean placaValida(String placa) {
        return PLACA.matcher(normalizarPlaca(placa)).matches();
    }
}
