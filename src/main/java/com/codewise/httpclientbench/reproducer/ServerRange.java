package com.codewise.httpclientbench.reproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServerRange implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ServerRange.class);
    private final int startPort;
    private final int endPort;
    private final List<HttpServer> servers;

    public ServerRange(int startPort, int endPort) {
        this.startPort = startPort;
        this.endPort = endPort;
        this.servers = new ArrayList<>();
        for (int i = startPort; i <= endPort; i++) {
            HttpServer server = new HttpServer(i);
            servers.add(server);
        }
    }

    @Override
    public void close() {
        for (HttpServer server : servers) {
            try {
                server.close();
            } catch (Exception e) {
                log.error("Closing server failed", e);
            }
        }
    }
}
