package org.example.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public class PasswordService {
    public static byte[] hashPassword(String password, byte[] salt, int iterations) {
        char[] passwordChar = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(passwordChar, salt, iterations, Constant.KEY_SIZE);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(Constant.HASH_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Ошибка хеширования паролей", e);
        }  finally {
            spec.clearPassword();
        }
    }

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[Constant.SALT_SIZE];
        random.nextBytes(salt);
        return salt;
    }
}
