package com.github.ivmikhail.routing.api;

import com.github.ivmikhail.common.http.HttpServerHelper;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        Config config = ConfigFactory.load();
        HttpServer server = HttpServerHelper.start(config);
        logger.info("routing-api started");
    }

}
