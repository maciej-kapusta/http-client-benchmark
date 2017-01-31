package com.codewise.httpclientbench;

import io.undertow.Undertow;
import io.undertow.util.Headers;

public class SampleHttpServer implements AutoCloseable {

    private final Undertow server;

    public SampleHttpServer() {
        server = Undertow.builder()
                .addHttpListener(8080, "localhost")
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
        SampleHttpServer sampleHttpServer = new SampleHttpServer();
        Runtime.getRuntime().addShutdownHook(new Thread(sampleHttpServer::close));
    }

    @Override
    public void close() {
        server.stop();
    }
}
