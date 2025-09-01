package com.github.ivmikhail.routing.api;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        Config config = ConfigFactory.load();

        logger.info("routing-api started");
    }
}
