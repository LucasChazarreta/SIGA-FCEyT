package ar.edu.unse.siga.common;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {
    private PasswordUtil() {}

    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt());
    }

    public static boolean check(String plain, String hashed) {
        return BCrypt.checkpw(plain, hashed);
    }
}
