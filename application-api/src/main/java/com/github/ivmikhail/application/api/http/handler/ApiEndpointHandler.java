package com.github.ivmikhail.application.api.http.handler;

import com.github.ivmikhail.application.api.JsonValidator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import static java.net.HttpURLConnection.*;

public class ApiEndpointHandler implements HttpHandler {
    private ExceptionHandler exceptionHandler;
    private JsonValidator validator;

    public ApiEndpointHandler(ExceptionHandler exceptionHandler, JsonValidator validator) {
        this.exceptionHandler = exceptionHandler;
        this.validator = validator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try (InputStream is = exchange.getRequestBody();
             OutputStream os = exchange.getResponseBody()
        ) {
            byte[] responseBody = is.readAllBytes();
            String requestString = new String(responseBody, getCharset(exchange));

            int responseCode = HTTP_BAD_METHOD;
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                responseCode = validator.isValid(requestString) ? HTTP_OK : HTTP_BAD_REQUEST;
            }

            exchange.sendResponseHeaders(responseCode, responseBody.length);
            os.write(responseBody); // return whatever was received
        } catch (Exception e) {
            exceptionHandler.handle(exchange, e);
        }
    }

    private Charset getCharset(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType != null && contentType.contains("charset=")) {
            String[] parts = contentType.split("charset=");
            if (parts.length > 1) {
                return Charset.forName(parts[1].trim()); // may throw UnsupportedCharsetException
            }
        }
        return StandardCharsets.UTF_8;
    }
}
