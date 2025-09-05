package com.github.ivmikhail.application.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.net.HttpURLConnection.*;

public class ApiEndpointHandler implements HttpHandler {

    private final JsonValidator jsonValidator;
    private final LongHistogram histogram;

    public ApiEndpointHandler(JsonValidator jsonValidator, LongHistogram durationHistogram) {
        this.jsonValidator = jsonValidator;
        this.histogram = durationHistogram;
    }

    @Override
    public void handle(HttpExchange exchange) {
        long start = System.currentTimeMillis();

        try (InputStream is = exchange.getRequestBody()) {
            byte[] request = is.readAllBytes();

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(HTTP_BAD_METHOD, request, exchange);
                return;
            }
            Charset charset = getCharset(exchange, StandardCharsets.UTF_8);
            String body = new String(request, charset);

            if (!jsonValidator.isValid(body)) {
                sendResponse(HTTP_BAD_REQUEST, request, exchange);
                return;
            }

            byte[] response = body.getBytes(charset);
            Headers headers = Headers.of("Content-Type", "application/json; charset=" + charset.displayName());
            sendResponse(HTTP_BAD_REQUEST, response, headers, exchange);
        } catch (Exception e) {
            handeException(exchange, e);
        } finally {
            exchange.close();
            long duration = System.currentTimeMillis() - start;
            Attributes attrs = Attributes.of(stringKey("handler"), "api");
            histogram.record(duration, attrs);
        }
    }

    private Charset getCharset(HttpExchange exchange, Charset defaultCharset) throws UnsupportedCharsetException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType != null && contentType.contains("charset=")) {
            String[] parts = contentType.split("charset=");
            if (parts.length > 1) {
                return Charset.forName(parts[1].trim()); // may throw UnsupportedCharsetException
            }
        }
        return defaultCharset;
    }

    private void handeException(HttpExchange exchange, Exception e) {
        try {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                e.printStackTrace(pw);
            }
            byte[] response = sw.toString().getBytes(StandardCharsets.UTF_8);
            Headers.of("Content-Type", "text/plain; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

            try (OutputStream os = exchange.getResponseBody()) {
                exchange.sendResponseHeaders(HTTP_INTERNAL_ERROR, response.length);
                os.write(response);
            }
        } catch (IOException ignored) {
        }
    }

    private void sendResponse(int httpCode, byte[] response, HttpExchange e) throws IOException {
        sendResponse(httpCode, response, null, e);
    }

    private void sendResponse(int httpCode, byte[] response, Headers headers, HttpExchange e) throws IOException {
        if (headers != null) {
            e.getResponseHeaders().putAll(headers);
        }
        e.sendResponseHeaders(httpCode, response.length);
        e.getResponseBody().write(response);
    }
}