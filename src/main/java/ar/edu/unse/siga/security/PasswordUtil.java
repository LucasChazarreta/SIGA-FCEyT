package ar.edu.unse.siga.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class PasswordUtil {
    private PasswordUtil(){}

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new RuntimeException("Error generando hash", e);
        }
    }

    public static boolean check(String raw, String hash) {
        return hash(raw).equalsIgnoreCase(hash);
    }
}
