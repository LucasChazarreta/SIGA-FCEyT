package ar.edu.unse.siga.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utilidades de validación para entradas de usuario.
 * Pensado para validar campos de formularios (Inventario/Trámites, etc.).
 *
 * Todas las validaciones son "puros" (sin I/O) para facilitar pruebas unitarias.
 */
public final class ValidacionesUsuario {

    private static final Pattern EMAIL_SIMPLE =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ValidacionesUsuario() { }

    // ====== Helpers ======

    /** null-safe trim. */
    public static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /** Reemplaza coma por punto para permitir ingreso local (ej: 12,34). */
    public static String normalizarDecimal(String s) {
        return trimOrEmpty(s).replace(',', '.');
    }

    // ====== Validaciones básicas ======

    public static boolean noVacio(String s) {
        return !trimOrEmpty(s).isEmpty();
    }

    public static boolean longitudMax(String s, int max) {
        return trimOrEmpty(s).length() <= max;
    }

    public static boolean esEntero(String s) {
        String t = trimOrEmpty(s);
        if (t.isEmpty()) return false;
        try {
            Integer.parseInt(t);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean esEnteroNoNegativo(String s) {
        if (!esEntero(s)) return false;
        return Integer.parseInt(trimOrEmpty(s)) >= 0;
    }

    public static boolean esEnteroPositivo(String s) {
        if (!esEntero(s)) return false;
        return Integer.parseInt(trimOrEmpty(s)) > 0;
    }

    public static boolean esDecimal(String s) {
        String t = normalizarDecimal(s);
        if (t.isEmpty()) return false;
        try {
            new BigDecimal(t);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean esDecimalNoNegativo(String s) {
        if (!esDecimal(s)) return false;
        return new BigDecimal(normalizarDecimal(s)).compareTo(BigDecimal.ZERO) >= 0;
    }

    public static boolean esDecimalPositivo(String s) {
        if (!esDecimal(s)) return false;
        return new BigDecimal(normalizarDecimal(s)).compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean enRangoInt(String s, int min, int max) {
        if (!esEntero(s)) return false;
        int v = Integer.parseInt(trimOrEmpty(s));
        return v >= min && v <= max;
    }

    public static boolean enRangoDecimal(String s, BigDecimal min, BigDecimal max) {
        if (!esDecimal(s)) return false;
        BigDecimal v = new BigDecimal(normalizarDecimal(s));
        return v.compareTo(min) >= 0 && v.compareTo(max) <= 0;
    }

    public static boolean esEmailBasico(String s) {
        String t = trimOrEmpty(s);
        if (t.isEmpty()) return false;
        return EMAIL_SIMPLE.matcher(t).matches();
    }

    /** Fecha en formato ISO (yyyy-MM-dd). */
    public static boolean esFechaISO(String s) {
        String t = trimOrEmpty(s);
        if (t.isEmpty()) return false;
        try {
            LocalDate.parse(t, ISO_DATE);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    // ====== Validación de casos de uso típicos (ej. Inventario) ======

    /**
     * Valida campos típicos de un Insumo antes de persistir.
     * @return lista de errores; vacía si todo OK.
     */
    public static List<String> validarInsumo(
            String nombre,
            String categoria,
            String stockStr,
            String precioStr,
            String descripcion
    ) {
        List<String> errores = new ArrayList<>();

        // Nombre
        if (!noVacio(nombre)) errores.add("El nombre es obligatorio.");
        else if (!longitudMax(nombre, 120)) errores.add("El nombre supera 120 caracteres.");

        // Categoría (asumimos combo/lista -> llega texto/ID como String)
        if (!noVacio(categoria)) errores.add("La categoría es obligatoria.");

        // Stock entero no negativo
        if (!esEnteroNoNegativo(stockStr)) {
            errores.add("El stock debe ser un entero no negativo.");
        } else if (!enRangoInt(stockStr, 0, 1_000_000)) {
            errores.add("El stock debe estar entre 0 y 1.000.000.");
        }

        // Precio decimal no negativo (permite 0)
        if (!esDecimalNoNegativo(precioStr)) {
            errores.add("El precio debe ser un número decimal válido (use punto o coma) y no negativo.");
        } else if (!enRangoDecimal(precioStr, new BigDecimal("0"), new BigDecimal("9999999.99"))) {
            errores.add("El precio está fuera de rango (0 a 9.999.999,99).");
        }

        // Descripción (opcional) con tope
        if (!longitudMax(Objects.toString(descripcion, ""), 500)) {
            errores.add("La descripción supera 500 caracteres.");
        }

        return errores;
    }
}
