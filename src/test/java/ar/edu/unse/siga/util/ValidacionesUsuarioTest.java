package ar.edu.unse.siga.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidacionesUsuarioTest {

    @Nested
    @DisplayName("noVacio / longitudMax")
    class TextoBasico {
        @Test
        void noVacio_ok() {
            assertTrue(ValidacionesUsuario.noVacio("hola"));
        }

        @Test
        void noVacio_falla_en_null_y_espacios() {
            assertFalse(ValidacionesUsuario.noVacio(null));
            assertFalse(ValidacionesUsuario.noVacio("   "));
        }

        @Test
        void longitudMax_limite() {
            assertTrue(ValidacionesUsuario.longitudMax("abc", 3));
            assertFalse(ValidacionesUsuario.longitudMax("abcd", 3));
        }
    }

    @Nested
    @DisplayName("Enteros")
    class Enteros {
        @ParameterizedTest
        @ValueSource(strings = {"0","1","+10","999"})
        void esEntero_valoresValidos(String v) {
            assertTrue(ValidacionesUsuario.esEntero(v));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "a12", "12a", "1.5", "-", "+", "2,3"})
        void esEntero_valoresInvalidos(String v) {
            assertFalse(ValidacionesUsuario.esEntero(v));
        }

        @ParameterizedTest
        @CsvSource({
                "0,true",
                "10,true",
                "-1,false"
        })
        void esEnteroNoNegativo(String v, boolean esperado) {
            assertEquals(esperado, ValidacionesUsuario.esEnteroNoNegativo(v));
        }

        @ParameterizedTest
        @CsvSource({
                "1,true",
                "0,false",
                "-5,false"
        })
        void esEnteroPositivo(String v, boolean esperado) {
            assertEquals(esperado, ValidacionesUsuario.esEnteroPositivo(v));
        }

        @ParameterizedTest
        @CsvSource({
                "5,1,10,true",
                "1,1,10,true",
                "10,1,10,true",
                "0,1,10,false",
                "11,1,10,false"
        })
        void enRangoInt(String v, int min, int max, boolean esperado) {
            assertEquals(esperado, ValidacionesUsuario.enRangoInt(v, min, max));
        }
    }

    @Nested
    @DisplayName("Decimales")
    class Decimales {
        @ParameterizedTest
        @ValueSource(strings = {"0", "0.0", "12.5", "12,5", "9999999.99"})
        void esDecimal_valoresValidos(String v) {
            assertTrue(ValidacionesUsuario.esDecimal(v));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "a12", "12.", ".", ",", "12,,"})
        void esDecimal_valoresInvalidos(String v) {
            assertFalse(ValidacionesUsuario.esDecimal(v));
        }

        @ParameterizedTest
        @CsvSource({
                "0,true",
                "0.01,true",
                "-0.01,false"
        })
        void esDecimalNoNegativo(String v, boolean esperado) {
            assertEquals(esperado, ValidacionesUsuario.esDecimalNoNegativo(v));
        }

        @ParameterizedTest
        @CsvSource({
                "10,0,100,true",
                "0,0,100,true",
                "100,0,100,true",
                "-1,0,100,false",
                "101,0,100,false"
        })
        void enRangoDecimal(String v, String min, String max, boolean esperado) {
            boolean ok = ValidacionesUsuario.enRangoDecimal(
                    v, new BigDecimal(min), new BigDecimal(max));
            assertEquals(esperado, ok);
        }
    }

    @Nested
    @DisplayName("Email y Fecha")
    class Otros {
        @ParameterizedTest
        @ValueSource(strings = {"a@b.com","user.name+tag@domain.edu","x@y.zw"})
        void email_ok(String mail) {
            assertTrue(ValidacionesUsuario.esEmailBasico(mail));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "a@", "@b.com", "a@b", "a b@c.com"})
        void email_mal(String mail) {
            assertFalse(ValidacionesUsuario.esEmailBasico(mail));
        }

        @ParameterizedTest
        @ValueSource(strings = {"2025-10-28","1999-01-01"})
        void fechaISO_ok(String f) {
            assertTrue(ValidacionesUsuario.esFechaISO(f));
        }

        @ParameterizedTest
        @ValueSource(strings = {"28-10-2025","2025/10/28","", "2025-13-01"})
        void fechaISO_mal(String f) {
            assertFalse(ValidacionesUsuario.esFechaISO(f));
        }
    }

    @Nested
    @DisplayName("Caso de uso: Insumo")
    class Insumo {
        @Test
        void validarInsumo_ok() {
            List<String> errs = ValidacionesUsuario.validarInsumo(
                    "Alcohol en gel", "Higiene", "50", "1200,50", "Presentación 500ml");
            assertTrue(errs.isEmpty(), "No debería haber errores: " + errs);
        }

        @Test
        void validarInsumo_erroresVarios() {
            List<String> errs = ValidacionesUsuario.validarInsumo(
                    "   ", "", "-5", "-10", "x".repeat(600));
            assertFalse(errs.isEmpty());
            assertTrue(errs.stream().anyMatch(e -> e.toLowerCase().contains("obligatorio")));
            assertTrue(errs.stream().anyMatch(e -> e.toLowerCase().contains("entero no negativo")));
            assertTrue(errs.stream().anyMatch(e -> e.toLowerCase().contains("no negativo")));
            assertTrue(errs.stream().anyMatch(e -> e.toLowerCase().contains("supera 500")));
        }
    }
}
