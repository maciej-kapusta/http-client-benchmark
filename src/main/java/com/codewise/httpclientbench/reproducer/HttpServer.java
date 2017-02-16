package com.codewise.httpclientbench.reproducer;

import io.undertow.Undertow;
import io.undertow.util.Headers;

public class HttpServer implements AutoCloseable {

    private final Undertow server;

    public HttpServer(int port) {
        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    if (exchange.getQueryParameters().get("shouldFail") == null) {
                        exchange.getResponseSender().send("Hello World");
                    } else {
                        exchange.setStatusCode(404);
                    }
                }).build();
        server.start();
    }

    public static void main(String[] args) {
        HttpServer httpServer = new HttpServer(8080);
        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::close));
    }

    @Override
    public void close() {
        server.stop();
    }
}
