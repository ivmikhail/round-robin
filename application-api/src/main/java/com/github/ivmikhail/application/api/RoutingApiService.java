package com.github.ivmikhail.application.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class RoutingApiService {
    static final Logger logger = LoggerFactory.getLogger(RoutingApiService.class);

    private final List<String> addresses; // routing-api base URLs
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public RoutingApiService(List<String> addresses, HttpClient httpClient, Duration requestTimeout) {
        this.addresses = addresses;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    public void deregister(String addr) {
        for (String remoteAddr : addresses) {
            httpRequest(remoteAddr + "/server/deregister", addr);
        }
    }

    public void heartbeat(String addr) {
        for (String remoteAddr : addresses) {
            httpRequest(remoteAddr + "/server/heartbeat", addr);
        }
    }

    private void httpRequest(String endpoint, String payload) {
        try {
            URI uri = URI.create(endpoint);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(requestTimeout)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            String responseBody = response.body();
            logger.info("received HTTP response code {} from {}, responseBody={}", responseCode, endpoint, responseBody);
        } catch (Exception e) {
            logger.warn("error while executing request to {}", endpoint, e);
        }
    }
}
