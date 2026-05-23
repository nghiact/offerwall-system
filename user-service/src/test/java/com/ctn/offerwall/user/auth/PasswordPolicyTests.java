package com.ctn.offerwall.user.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTests {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void acceptsSixteenCharacterPassword() {
        assertThat(passwordPolicy.validate("abcdefghijklmnop").valid()).isTrue();
    }

    @Test
    void acceptsEightCharactersWithThreeCategories() {
        assertThat(passwordPolicy.validate("Pass1234").valid()).isTrue();
    }

    @Test
    void rejectsShortPassword() {
        assertThat(passwordPolicy.validate("A1!aaaa").valid()).isFalse();
    }

    @Test
    void rejectsPasswordWithTooFewCategories() {
        assertThat(passwordPolicy.validate("password").valid()).isFalse();
    }
}
