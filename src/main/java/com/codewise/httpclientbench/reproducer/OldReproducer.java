package com.codewise.httpclientbench.reproducer;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import io.reactivex.Observable;

import java.io.IOException;

public class OldReproducer extends Reproducer {

    private final AsyncHttpClient asyncHttpClient;

    public OldReproducer() {
        asyncHttpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
                .setAllowPoolingConnections(true)
                .setConnectTimeout(1000)
                .setRequestTimeout(500)
                .setFollowRedirect(false)
                .setMaxRequestRetry(0)
                .setMaxConnectionsPerHost(200)
                .setMaxConnections(1000)
                .setConnectionTTL(300_000)
                .setPooledConnectionIdleTimeout(300_000)
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        try (OldReproducer reproducer = new OldReproducer()) {
            reproducer.start();
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
