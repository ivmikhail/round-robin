package com.github.ivmikhail.application.api;

import com.github.ivmikhail.common.http.JsonValidator;
import com.github.ivmikhail.common.http.handler.ExceptionHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import static java.net.HttpURLConnection.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiEndpointHandlerTest {
    private HttpServer server;
    private HttpClient client;
    @Mock
    private JsonValidator validator;

    @BeforeEach
    public void beforeEach() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", new ApiEndpointHandler(new ExceptionHandler(), validator));
        server.start();

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void afterEach() {
        client.close();
        server.stop(0);
    }


    @Test
    void shouldReturnCorrectResponse() throws URISyntaxException, IOException, InterruptedException {
        when(validator.isValid(anyString())).thenReturn(true);
        URI uri = new URI("http://localhost:" + server.getAddress().getPort() + "/");

        HttpResponse<String> apiResponse = client.send(
                HttpRequest.newBuilder().uri(uri).POST(BodyPublishers.ofString("{\"test\": true}")).build(),
                BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(HTTP_OK, apiResponse.statusCode());
        assertEquals("{\"test\": true}", apiResponse.body());
        verify(validator).isValid(eq("{\"test\": true}"));
    }

    @Test
    void shouldReturn405IfGet() throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI("http://localhost:" + server.getAddress().getPort() + "/");

        HttpResponse<String> apiResponse = client.send(
                HttpRequest.newBuilder().uri(uri).GET().build(),
                BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(HTTP_BAD_METHOD, apiResponse.statusCode());
    }

    @Test
    void shouldReturn500IfUnsupportedCharset() throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI("http://localhost:" + server.getAddress().getPort() + "/");

        HttpResponse<String> apiResponse = client.send(
                HttpRequest.newBuilder().uri(uri)
                        .POST(BodyPublishers.ofString("{\"test\": true}"))
                        .header("Content-Type", "charset=hello").build(),
                BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(HTTP_INTERNAL_ERROR, apiResponse.statusCode());
        assertTrue(apiResponse.body().startsWith("java.nio.charset.UnsupportedCharsetException: hello"));
    }
}