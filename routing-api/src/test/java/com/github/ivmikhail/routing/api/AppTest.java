package com.github.ivmikhail.routing.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AppTest {

    @Test
    public void testMain() {
        String[] args = {};
        assertDoesNotThrow(() -> App.main(args));
    }
}
