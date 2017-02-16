package com.codewise.httpclientbench.reproducer;

import io.reactivex.Observable;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NewReproducer extends Reproducer {

    private static final Logger log = LoggerFactory.getLogger(NewReproducer.class);
    private final AsyncHttpClient asyncHttpClient;

    public NewReproducer() {

        DefaultAsyncHttpClientConfig.Builder configBuilder = new DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(1000)
                .setRequestTimeout(500)
                .setFollowRedirect(false)
                .setMaxRequestRetry(0)
                .setMaxConnectionsPerHost(200)
                .setMaxConnections(1000)
                .setConnectionTtl(300_000)
                .setPooledConnectionIdleTimeout(300_000)
                .setSoReuseAddress(true)
                .setKeepAlive(true);
        this.asyncHttpClient = new DefaultAsyncHttpClient(configBuilder.build());
    }

    public static void main(String[] args) throws Exception {
        try (NewReproducer newClientEnvironment = new NewReproducer()) {
            newClientEnvironment.start();
        } catch (IOException e) {
            log.error("Error closing environment", e);
        }
    }

    @Override
    protected Observable<String> callSingleServer(int port) {
        Request request = asyncHttpClient.prepareGet("http://localhost:" + port + "/").build();
        return Observable.<String>create(subscriber ->
                asyncHttpClient.executeRequest(request, new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        subscriber.onNext(response.getResponseBody());
                        subscriber.onComplete();
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        subscriber.onError(t);
                    }
                }))
                .doOnError(e -> errorCount.incrementAndGet())
                .doOnNext(r -> successCount.incrementAndGet());
    }

    @Override
    public void close() throws IOException {
        super.close();
        asyncHttpClient.close();
    }
}
