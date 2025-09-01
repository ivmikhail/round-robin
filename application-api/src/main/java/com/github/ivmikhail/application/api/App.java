package com.github.ivmikhail.application.api;

import com.github.ivmikhail.application.api.http.handler.ApiEndpointHandler;
import com.github.ivmikhail.application.api.http.handler.ExceptionHandler;
import com.github.ivmikhail.application.api.http.handler.StatusHandler;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {

    static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        Config config = ConfigFactory.load();
        startHttpServer(config);
    }

    private static HttpServer startHttpServer(Config config) throws IOException {
        int port = config.getInt("http.server.port");
        int backlog = config.getInt("http.server.backlog");
        InetSocketAddress addr = new InetSocketAddress(port);

        HttpServer server = HttpServer.create();
        server.bind(addr, backlog);
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        server.createContext("/status", new StatusHandler(exceptionHandler));
        server.createContext("/", new ApiEndpointHandler(exceptionHandler, new JsonValidator()));
        server.setExecutor(Executors.newCachedThreadPool());

        server.start();
        logger.info("HTTP server starts on {}", server.getAddress());

        return server;
    }
}
