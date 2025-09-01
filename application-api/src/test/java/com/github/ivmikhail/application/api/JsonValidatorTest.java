package com.github.ivmikhail.application.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonValidatorTest {

    @Test
    public void shouldValidateCorrectly() {
        JsonValidator validator = new JsonValidator();
        String input = "{\"game\":\"Mobile Legends\", \"gamerID\":\"GYUTDTE\", \"points\":20}";
        assertTrue(validator.isValid(input));
    }

    @Test
    public void shouldFailValidation() {
        JsonValidator validator = new JsonValidator();
        String input = "\"sss\")";
        assertFalse(validator.isValid(input));
    }

    @Test
    public void shouldCorrecltyValidateOneWord() {
        JsonValidator validator = new JsonValidator();
        String input = "\"sss\"";
        assertTrue(validator.isValid(input));
    }
}