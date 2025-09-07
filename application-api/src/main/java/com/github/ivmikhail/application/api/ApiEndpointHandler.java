package com.github.ivmikhail.application.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import static com.github.ivmikhail.common.Metrics.APPLICATION_API_PROCESS_TIME_MS;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.net.HttpURLConnection.*;

public class ApiEndpointHandler implements HttpHandler {
    static final Logger logger = LoggerFactory.getLogger(ApiEndpointHandler.class);

    private final LongHistogram processTimeHistogram;

    public ApiEndpointHandler(Meter meter) {
        this.processTimeHistogram = meter.histogramBuilder(APPLICATION_API_PROCESS_TIME_MS)
                .setDescription("Time taken for each request in ms")
                .setUnit("ms")
                .ofLongs()
                .build();
    }

    @Override
    public void handle(HttpExchange exchange) {
        // test branch
        long startTime = System.currentTimeMillis();

        try {
            byte[] request = exchange.getRequestBody().readAllBytes();

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(HTTP_BAD_METHOD, request, Headers.of(), exchange);
                return;
            }
            Charset charset = getCharset(exchange, StandardCharsets.UTF_8);
            String body = new String(request, charset);

            logger.info("handling request with body {} ", body);

            byte[] response = body.getBytes(charset);
            Headers headers = Headers.of("Content-Type", "application/json; charset=" + charset.displayName());
            sendResponse(HTTP_OK, response, headers, exchange);
        } catch (Exception e) {
            handeException(exchange, e);
        } finally {
            reportProcessTime(startTime);
            exchange.close();
        }
    }

    private void reportProcessTime(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        Attributes attrs = Attributes.of(stringKey("handler"), "api");
        processTimeHistogram.record(duration, attrs);
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
            var headers = Headers.of("Content-Type", "text/plain; charset=UTF-8");

            sendResponse(HTTP_INTERNAL_ERROR, response, headers, exchange);
        } catch (IOException ignored) {
        }
    }

    private void sendResponse(int httpCode, byte[] response, Headers headers, HttpExchange e) throws IOException {
        if (headers != null) {
            e.getResponseHeaders().putAll(headers);
        }
        e.sendResponseHeaders(httpCode, response.length);
        e.getResponseBody().write(response);
    }
}