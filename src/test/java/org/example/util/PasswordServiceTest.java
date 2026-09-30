package org.example.util;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;


public class PasswordServiceTest {

    @Test
    public void checkGenerationWithIdenticalValues()  {
        String password1 = "password";
        String password2 = "password";
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        int iterations = 1000;

        byte[] passwordHash1 = PasswordService.hashPassword(password1, salt, iterations);
        byte[] passwordHash2 = PasswordService.hashPassword(password2, salt, iterations);

        assertTrue(MessageDigest.isEqual(passwordHash1, passwordHash2));
    }

    @Test
    public void checkGenerationWithDifferentPasswords() {
        String password1 = "password";
        String password2 = "drowssap";
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        int iterations = 1000;

        byte[] passwordHash1 = PasswordService.hashPassword(password1, salt, iterations);
        byte[] passwordHash2 = PasswordService.hashPassword(password2, salt, iterations);

        assertFalse(MessageDigest.isEqual(passwordHash1, passwordHash2));
    }

    @Test
    public void checkGenerationWithDifferentSalt() {
        String password1 = "password";
        String password2 = "password";
        byte[] salt1 = new byte[16];
        byte[] salt2 = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt1);
        random.nextBytes(salt2);
        int iterations = 1000;

        byte[] passwordHash1 = PasswordService.hashPassword(password1, salt1, iterations);
        byte[] passwordHash2 = PasswordService.hashPassword(password2, salt2, iterations);

        assertFalse(MessageDigest.isEqual(passwordHash1, passwordHash2));
    }

    @Test
    public void checkGenerationWithDifferentIterations() {
        String password1 = "password";
        String password2 = "password";
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        int iterations1 = 1000;
        int iterations2 = 1001;

        byte[] passwordHash1 = PasswordService.hashPassword(password1, salt, iterations1);
        byte[] passwordHash2 = PasswordService.hashPassword(password2, salt, iterations2);

        assertFalse(MessageDigest.isEqual(passwordHash1, passwordHash2));
    }

    @Test
    public void checkLengthSalt() {
        byte[] salt = PasswordService.generateSalt();

        assertEquals(Constant.SALT_SIZE, salt.length);
    }

    @Test
    public void checkUniqueSalt() {
        byte[] salt1 = PasswordService.generateSalt();
        byte[] salt2 = PasswordService.generateSalt();

        assertFalse(Arrays.equals(salt1, salt2));
    }
}
