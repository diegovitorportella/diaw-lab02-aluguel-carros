package com.example.demo.validation;

import com.example.demo.TestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidadoresTest {

    @Test
    void aceitaCpfValidoComEMascaraOuSemMascara() {
        assertTrue(Validadores.cpfValido("529.982.247-25"));
        assertTrue(Validadores.cpfValido("52998224725"));
        assertTrue(Validadores.cpfValido("111.444.777-35"));
        assertTrue(Validadores.cpfValido(TestData.cpf()));
    }

    @Test
    void rejeitaCpfInvalido() {
        assertFalse(Validadores.cpfValido("529.982.247-24")); // dígito verificador errado
        assertFalse(Validadores.cpfValido("111.111.111-11")); // dígitos repetidos
        assertFalse(Validadores.cpfValido("123.456.789-00"));
        assertFalse(Validadores.cpfValido("1234567890")); // tamanho
        assertFalse(Validadores.cpfValido(""));
        assertFalse(Validadores.cpfValido(null));
    }

    @Test
    void normalizaCpfParaSomenteDigitos() {
        assertEquals("52998224725", Validadores.somenteDigitos("529.982.247-25"));
        assertEquals("", Validadores.somenteDigitos(null));
    }

    @Test
    void aceitaPlacaAntigaEMercosul() {
        assertTrue(Validadores.placaValida("ABC-1234"));
        assertTrue(Validadores.placaValida("abc1234"));
        assertTrue(Validadores.placaValida("ABC1D23"));
        assertTrue(Validadores.placaValida("abc-1d23"));
    }

    @Test
    void rejeitaPlacaInvalida() {
        assertFalse(Validadores.placaValida("AB-12345"));
        assertFalse(Validadores.placaValida("ABCD123"));
        assertFalse(Validadores.placaValida("1234567"));
        assertFalse(Validadores.placaValida("ABC-12"));
        assertFalse(Validadores.placaValida(""));
        assertFalse(Validadores.placaValida(null));
    }

    @Test
    void normalizaPlacaParaMaiusculasSemSeparador() {
        assertEquals("ABC1234", Validadores.normalizarPlaca("abc-1234"));
        assertEquals("ABC1D23", Validadores.normalizarPlaca(" abc 1d23 "));
    }

    @Test
    void validaRg() {
        assertTrue(Validadores.rgValido("MG-12.345.678"));
        assertTrue(Validadores.rgValido("12.345.678-X"));
        assertFalse(Validadores.rgValido("123"));
        assertFalse(Validadores.rgValido(""));
        assertFalse(Validadores.rgValido(null));
        assertEquals("MG12345678", Validadores.normalizarRg("mg-12.345.678"));
    }
}
