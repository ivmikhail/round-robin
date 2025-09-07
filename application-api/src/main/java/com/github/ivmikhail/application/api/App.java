package com.github.ivmikhail.application.api;

import com.github.ivmikhail.common.ExecutorServiceHelper;
import com.github.ivmikhail.common.http.HttpServerHelper;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class App {

    static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        Meter meter = GlobalOpenTelemetry.get().getMeter("application-api");
        Config config = ConfigFactory.load();

        HttpServer server = HttpServerHelper.start(config);
        server.createContext("/", new ApiEndpointHandler(meter));

        registerToRoutingApi(config, server);

        logger.info("application-api started");
    }

    private static void registerToRoutingApi(Config config, HttpServer server) throws UnknownHostException {
        String addr = InetAddress.getLocalHost().getHostAddress() + ":" + server.getAddress().getPort();
        logger.info("my addr {}", addr);

        var routingApiAddrs = config.getStringList("routing-api.addresses");
        var requestTimeout = config.getDuration("routing-api.request-timeout");
        var connectTimeout = config.getDuration("routing-api.connect-timeout");
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();

        RoutingApiService routingApiService = new RoutingApiService(routingApiAddrs, httpClient, requestTimeout);

        var scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            logger.info("scheduled heartbeat to routing-api: {}", String.join(",", routingApiAddrs));
            routingApiService.heartbeat(addr);
        }, 0, config.getLong("routing-api.heartbeat.interval-ms"), MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("deregister from routing api");
            routingApiService.deregister(addr);

            logger.info("shutting down scheduler service...");
            ExecutorServiceHelper.shutdown(scheduler);
        }));
    }
}
