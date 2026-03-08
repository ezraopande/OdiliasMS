package com.odilias.odms.util;

import org.mindrot.jbcrypt.BCrypt;

public class SecurityUtil {

    // Hash a string using BCrypt
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }

    // Verify a plain text string against a hashed string
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            // Backward compatibility for the hardcoded admin123 placeholder
            return plainTextPassword.equals(hashedPassword);
        }
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}
