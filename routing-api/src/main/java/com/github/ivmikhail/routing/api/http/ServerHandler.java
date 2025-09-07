package com.github.ivmikhail.routing.api.http;

import com.github.ivmikhail.routing.api.RoundRobinService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

public class ServerHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private final RoundRobinService rrService;

    public ServerHandler(RoundRobinService rrService) {
        this.rrService = rrService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try (exchange) {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            logger.info("Handling {} request to {}", method, path);

            byte[] response = "OK".getBytes();
            int responseCode = HTTP_OK;

            if (path.equals("/server/deregister") && "POST".equals(method)) {
                rrService.deregister(readServiceAddress(exchange));
            } else if (path.equals("/server/heartbeat") && "POST".equals(method)) {
                rrService.heartbeat(readServiceAddress(exchange));
            } else if (path.equals("/server/list") && "GET".equals(method)) {
                response = String.join(", ", rrService.getAllServers()).getBytes();
            } else {
                response = "not found".getBytes();
                responseCode = HTTP_NOT_FOUND;
            }

            exchange.sendResponseHeaders(responseCode, response.length);
            exchange.getResponseBody().write(response);
        }
    }

    private String readServiceAddress(HttpExchange exchange) throws IOException {
        byte[] payload = exchange.getRequestBody().readAllBytes();
        String addr = new String(payload, StandardCharsets.UTF_8);
        return addr;
    }
}
