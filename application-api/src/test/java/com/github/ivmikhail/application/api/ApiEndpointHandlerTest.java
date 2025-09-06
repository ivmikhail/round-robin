package com.github.ivmikhail.application.api;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.concurrent.Executors;

import static java.net.HttpURLConnection.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ApiEndpointHandlerTest {
    private HttpServer server;
    private HttpClient client;

    @BeforeEach
    public void beforeEach() throws IOException {
        Meter meter = OpenTelemetry.noop().getMeter("noop");
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", new ApiEndpointHandler(meter));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void afterEach() {
        client.close();
        server.stop(0);
    }

    @Test
    void shouldReturnRequestBodyAsResponse() throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI("http://localhost:" + server.getAddress().getPort() + "/");

        HttpResponse<String> apiResponse = client.send(HttpRequest.newBuilder().uri(uri).POST(BodyPublishers.ofString("{\"test\": true}")).build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(HTTP_BAD_REQUEST, apiResponse.statusCode());
        assertEquals("{\"test\": true}", apiResponse.body());
    }

    @Test
    void shouldReturn405IfGet() throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI("http://localhost:" + server.getAddress().getPort() + "/");

        HttpResponse<String> apiResponse = client.send(HttpRequest.newBuilder().uri(uri).GET().build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(HTTP_BAD_METHOD, apiResponse.statusCode());
    }

    @Test
    void shouldReturn500IfUnsupportedCharset() throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI("http://localhost:" + server.getAddress().getPort() + "/");

        HttpResponse<String> apiResponse = client.send(HttpRequest.newBuilder().uri(uri).POST(BodyPublishers.ofString("{\"test\": true}")).header("Content-Type", "charset=hello").build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(HTTP_INTERNAL_ERROR, apiResponse.statusCode());
        assertTrue(apiResponse.body().startsWith("java.nio.charset.UnsupportedCharsetException: hello"));
    }
}