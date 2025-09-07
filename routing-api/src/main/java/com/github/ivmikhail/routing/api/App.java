package com.github.ivmikhail.routing.api;

import com.github.ivmikhail.common.ExecutorServiceHelper;
import com.github.ivmikhail.common.http.HttpServerHelper;
import com.github.ivmikhail.routing.api.http.RoutingHandler;
import com.github.ivmikhail.routing.api.http.ServerHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MILLIS;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        var meter = GlobalOpenTelemetry.get().getMeter("routing-api");
        var config = ConfigFactory.load();

        var scheduler = Executors.newSingleThreadScheduledExecutor();
        var rrService = createRoundRobinService(config, scheduler);

        var server = HttpServerHelper.start(config);
        var requestTimeout = config.getDuration("routing.upstream.request-timeout");
        var connectTimeout = config.getDuration("routing.upstream.connect-timeout");
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();

        server.createContext("/server", new ServerHandler(rrService));
        server.createContext("/", new RoutingHandler(rrService, httpClient, meter, requestTimeout));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("shutting down scheduler service...");
            ExecutorServiceHelper.shutdown(scheduler);
        }));

        logger.info("routing-api started successfully");
    }

    private static RoundRobinService createRoundRobinService(Config config, ScheduledExecutorService scheduler) {
        var cleanupInterval = config.getLong("round-robin.cleanup.interval-ms");
        var maxKeepAliveMs = config.getLong("round-robin.max-keep-alive-ms");

        var rrService = new RoundRobinService(maxKeepAliveMs);

        scheduler.scheduleAtFixedRate(() -> {
            int removed = rrService.cleanupDeadServers();
            logger.info("scheduled clean up dead servers, removed servers = {}", removed);
        }, 30, cleanupInterval, TimeUnit.MILLISECONDS);

        return rrService;
    }
}
