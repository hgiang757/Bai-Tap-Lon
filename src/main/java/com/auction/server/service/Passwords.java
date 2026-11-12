package com.auction.server.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted PBKDF2 credentials, never included in public user DTOs. */
final class Passwords {
    private Passwords() {}

    /** Load the crypto provider before accepting clients, rather than during a cold first login. */
    static void warmUp() {
        java.util.UUID.randomUUID(); // Login tokens use a lazily initialized secure generator.
        derive("startup-check", new byte[16]); // Not a credential; this value is never stored.
    }

    static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + derive(password, salt);
    }

    static boolean verify(String password, String hash) {
        String[] parts = hash.split(":");
        return MessageDigest.isEqual(
                derive(password, Base64.getDecoder().decode(parts[0]))
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                parts[1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120_000, 256);
        try {
            return Base64.getEncoder()
                    .encodeToString(
                            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                                    .generateSecret(spec)
                                    .getEncoded());
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
