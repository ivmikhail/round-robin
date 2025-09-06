package com.github.ivmikhail.common.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthHandlerTest {
    private HttpServer server;
    private HttpClient client;

    @BeforeEach
    public void beforeEach() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/status", new HealthHandler());
        server.start();

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void afterEach() {
        client.close();
        server.stop(0);
    }


    @Test
    void shouldReturnOK() throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI("http://localhost:" + server.getAddress().getPort() + "/status");
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(uri).GET().build(),
                BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(HTTP_OK, response.statusCode());
        assertEquals("OK", response.body());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
    }
}