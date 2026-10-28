package com.auction.client.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidatorsTest {
    @ParameterizedTest
    @ValueSource(strings = {"25020580@vnu.edu.vn", "a.b+c@gmail.com", "user_1@mail.co",
            "Hieu.Mai@VNU.EDU.VN", "x@sub.domain.com.vn", "  a@gmail.com  "})
    void acceptsValidEmails(String email) {
        assertTrue(Validators.email(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"wefuwhoi@jipdsffds.vsdipokcvj", "abc", "abc@", "@gmail.com",
            "a@b", "a@@b.com", "a b@gmail.com", "a..b@gmail.com", ".a@gmail.com",
            "a.@gmail.com", "a@gmail.c", "a@gmail.comm", "a@-gmail.com", "a@gmail-.com",
            "a@gmail..com", "a@.gmail.com", "a@gmail.com.", "a@gmail.123", "   "})
    void rejectsInvalidEmails(String email) {
        assertFalse(Validators.email(email));
    }

    static Stream<String> oversizedEmails() {
        return Stream.of("a".repeat(65) + "@gmail.com", "a@" + "b".repeat(64) + ".com",
                "a@" + ("b".repeat(63) + ".").repeat(4) + "com");
    }

    @ParameterizedTest
    @MethodSource("oversizedEmails")
    void rejectsOversizedEmails(String email) {
        assertFalse(Validators.email(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Hieu Mai", "user_1"})
    void acceptsUsernames(String username) {
        assertTrue(Validators.username(username));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "bad@name"})
    void rejectsUsernames(String username) {
        assertFalse(Validators.username(username));
    }

    @ParameterizedTest
    @ValueSource(strings = {"password1", "1234567a"})
    void acceptsPasswords(String password) {
        assertTrue(Validators.password(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "abcdefgh", "12345678"})
    void rejectsPasswords(String password) {
        assertFalse(Validators.password(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {"password1", "another2"})
    void acceptsMatchingPasswords(String password) {
        assertTrue(Validators.matches(password, password));
    }

    @ParameterizedTest
    @ValueSource(strings = {"password1", "another2"})
    void rejectsMismatchedPasswords(String password) {
        assertFalse(Validators.matches(password, password + "x"));
    }
}
