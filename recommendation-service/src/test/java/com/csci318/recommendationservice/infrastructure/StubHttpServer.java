package com.csci318.recommendationservice.infrastructure;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * A local HTTP server that stands in for a downstream service in the REST client tests.
 */
class StubHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, String> lastAuthorizationByPath = new ConcurrentHashMap<>();

    StubHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/api";
    }

    /** Answers GET /api{path} with the given status and JSON body. */
    void stub(String path, int status, String body) {
        server.createContext("/api" + path, exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (authorization != null) {
                lastAuthorizationByPath.put(path, authorization);
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }

    /** Answers GET /api{path} only after the given delay (to test client timeouts). */
    void stubSlow(String path, Duration delay) {
        server.createContext("/api" + path, exchange -> {
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
    }

    String lastAuthorization(String path) {
        return lastAuthorizationByPath.get(path);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /** A base URL on which nothing is listening (connection refused). */
    static String unreachableBaseUrl() throws IOException {
        try (StubHttpServer stopped = new StubHttpServer()) {
            return stopped.baseUrl();
        }
    }
}
