package com.club.util;

import java.nio.charset.StandardCharsets;
import org.mindrot.jbcrypt.BCrypt;

/** BCrypt 密码存储；不允许明文回退或静默截断超过 72 字节的密码。 */
public final class PasswordUtil {
    private PasswordUtil() {}

    public static String hash(String password) {
        if (password == null || password.isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密码不能为空且 UTF-8 长度不能超过 72 字节");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean isHash(String value) {
        return value != null && value.matches("\\$2a\\$12\\$[./A-Za-z0-9]{53}");
    }

    public static boolean matches(String password, String stored) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !isHash(stored)) return false;
        try {
            return BCrypt.checkpw(password, stored);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
