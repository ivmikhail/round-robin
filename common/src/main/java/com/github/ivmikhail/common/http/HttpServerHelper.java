package com.github.ivmikhail.common.http;

import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpServerHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerHelper.class);

    public static HttpServer start(Config config) throws IOException {
        int port = config.getInt("http.server.port");
        int backlog = config.getInt("http.server.backlog");
        InetSocketAddress addr = new InetSocketAddress(port);

        HttpServer server = HttpServer.create();
        server.bind(addr, backlog);
        server.createContext("/health", new StatusHandler());
        server.setExecutor(Executors.newCachedThreadPool());

        server.start();
        logger.info("HTTP server starts on {}", server.getAddress());

        // Add shutdown hook to stop the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down HTTP server...");
            server.stop(0); // 0 = stop immediately
            logger.info("HTTP server stopped");
        }));

        return server;
    }
}
