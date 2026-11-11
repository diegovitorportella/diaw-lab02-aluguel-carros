package com.example.demo;

import java.util.Random;

/** Geradores de dados válidos e únicos para os testes. */
public final class TestData {

    private static final Random RANDOM = new Random();

    private TestData() {}

    /** CPF válido (11 dígitos, sem máscara), com 9 dígitos aleatórios e dígitos verificadores corretos. */
    public static String cpf() {
        while (true) {
            int[] d = new int[11];
            for (int i = 0; i < 9; i++) d[i] = RANDOM.nextInt(10);
            d[9] = digito(d, 9);
            d[10] = digito(d, 10);
            StringBuilder sb = new StringBuilder();
            for (int n : d) sb.append(n);
            String cpf = sb.toString();
            if (cpf.chars().distinct().count() > 1) return cpf;
        }
    }

    private static int digito(int[] d, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) soma += d[i] * (quantidade + 1 - i);
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** Placa no padrão antigo (ABC1234) com letras e números aleatórios. */
    public static String placa() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) sb.append((char) ('A' + RANDOM.nextInt(26)));
        for (int i = 0; i < 4; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }
}
