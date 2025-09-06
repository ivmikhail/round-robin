package com.github.ivmikhail.application.api;

import com.github.ivmikhail.common.http.HttpServerHelper;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class App {

    static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        OpenTelemetry otel = GlobalOpenTelemetry.get();
        Meter meter = otel.getMeter("application-api");
        Config config = ConfigFactory.load();

        HttpServer server = HttpServerHelper.start(config);

        ApiEndpointHandler handler = new ApiEndpointHandler(new JsonValidator(), meter);
        server.createContext("/", handler);
        logger.info("application-api started");
    }
}
