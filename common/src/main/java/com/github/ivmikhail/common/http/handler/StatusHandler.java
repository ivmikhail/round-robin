package com.github.ivmikhail.common.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static java.net.HttpURLConnection.HTTP_OK;

public class StatusHandler implements HttpHandler {
    private ExceptionHandler exceptionHandler;

    public StatusHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (OutputStream os = exchange.getResponseBody()) {
            byte[] responseBody = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(HTTP_OK, responseBody.length);
            os.write(responseBody);
        } catch (Exception e) {
            exceptionHandler.handle(exchange, e);
        }
    }
}
