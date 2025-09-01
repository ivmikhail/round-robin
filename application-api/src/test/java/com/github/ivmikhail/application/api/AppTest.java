package com.github.ivmikhail.application.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AppTest {

    @Test
    public void shouldRunWithoutExceptions() {
        String[] args = {};
        assertDoesNotThrow(() -> App.main(args));
    }
}
