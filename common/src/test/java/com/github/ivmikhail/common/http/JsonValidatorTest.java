package com.github.ivmikhail.common.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonValidatorTest {

    @Test
    public void shouldValidateCorrectly() {
        JsonValidator validator = new JsonValidator();
        String input = "{\"game\":\"Mobile Legends\", \"gamerID\":\"GYUTDTE\", \"points\":20}";
        Assertions.assertTrue(validator.isValid(input));
    }

    @Test
    public void shouldFailValidation() {
        JsonValidator validator = new JsonValidator();
        String input = "\"sss\")";
        Assertions.assertFalse(validator.isValid(input));
    }

    @Test
    public void shouldCorrecltyValidateOneWord() {
        JsonValidator validator = new JsonValidator();
        String input = "\"sss\"";
        Assertions.assertTrue(validator.isValid(input));
    }
}