package com.github.ivmikhail.routing.api.http;

import com.github.ivmikhail.routing.api.RoundRobinService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.github.ivmikhail.common.Metrics.PROCESS_TIME_MS;
import static com.github.ivmikhail.common.Metrics.UPSTREAM_RESPONSE_TIME_MS;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;

public class RoutingHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(RoutingHandler.class);

    private final RoundRobinService rrService;
    private final HttpClient httpClient;
    private Duration requestTimeout;
    private LongHistogram processTimeHistogram;
    private LongHistogram upstreamResponseTimeHistogram;

    public RoutingHandler(RoundRobinService rrService, HttpClient httpClient, Meter meter, Duration requestTimeout) {
        this.rrService = rrService;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
        this.processTimeHistogram = meter.histogramBuilder(PROCESS_TIME_MS)
                .setDescription("Time taken for each request in ms")
                .setUnit("ms")
                .ofLongs()
                .build();
        this.upstreamResponseTimeHistogram = meter.histogramBuilder(UPSTREAM_RESPONSE_TIME_MS)
                .setDescription("Time taken for each request to process by upstream in ms")
                .setUnit("ms")
                .ofLongs()
                .build();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long start = System.currentTimeMillis();
        try (exchange) {
            var responseCode = HTTP_BAD_GATEWAY;
            var responseBytes = "502 Bad Gateway: No upstream available".getBytes();
            var upstreamOpt = rrService.getNext();

            if (upstreamOpt.isPresent()) {
                var upstream = upstreamOpt.get();
                try {
                    var upstreamResponse = forwardRequest(exchange, upstream);
                    responseCode = upstreamResponse.statusCode();
                    responseBytes = upstreamResponse.body();
                } catch (Exception e) {
                    // Upstream unreachable or failed
                    responseCode = HTTP_BAD_GATEWAY;
                    responseBytes = ("502 Bad Gateway: Upstream " + upstream + " unreachable or failed: " + e.getMessage()).getBytes();
                }
            }

            exchange.sendResponseHeaders(responseCode, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
        } finally {
            long duration = System.currentTimeMillis() - start;
            Attributes attrs = Attributes.of(stringKey("handler"), "routing");
            processTimeHistogram.record(duration, attrs);
        }
    }

    private HttpResponse<byte[]> forwardRequest(HttpExchange exchange, String remoteAddr) throws Exception {
        logger.info("Forwarding request to {}", remoteAddr);

        // Construct URI for remoteAddr
        String[] parts = remoteAddr.split(":");
        String hostname = parts[0];
        int port = Integer.parseInt(parts[1]);

        URI targetUri = new URI(
                "http",
                null,
                hostname,
                port,
                exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getQuery(),
                null
        );

        // Copy request method + body
        var bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(exchange.getRequestBody().readAllBytes());
        var requestBuilder = HttpRequest.newBuilder()
                .uri(targetUri)
                .timeout(requestTimeout)
                .method(exchange.getRequestMethod(), bodyPublisher);

        long start = System.currentTimeMillis();

        var res = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());

        long duration = System.currentTimeMillis() - start;
        Attributes attrs = Attributes.of(stringKey("remote_addr"), remoteAddr);
        upstreamResponseTimeHistogram.record(duration, attrs);

        return res;
    }
}
