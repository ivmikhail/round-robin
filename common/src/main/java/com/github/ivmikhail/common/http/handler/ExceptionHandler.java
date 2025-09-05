package com.github.ivmikhail.common.http.handler;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

public class ExceptionHandler {
    public void handle(HttpExchange exchange, Exception e) throws IOException {
        byte[] response = getStackTraceAsString(e).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(HTTP_INTERNAL_ERROR, response.length);
        exchange.getResponseBody().write(response);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
        }
        return sw.toString();
    }
}
