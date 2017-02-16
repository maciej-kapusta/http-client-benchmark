package com.codewise.httpclientbench;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class NewAsyncHttpClientExperiment extends BaseHttpExperiment {

    private static final Logger log = LoggerFactory.getLogger(NewAsyncHttpClientExperiment.class);

    private static final int SOCKET_CONNECTION_TIMEOUT = 3000;
    private static final int CONNECTION_TTL_IN_MILLIS = 300000; //5 minutes
    private final AsyncHttpClient asyncHttpClient;
    private final AtomicInteger requestSentCounter = new AtomicInteger(0);

    public NewAsyncHttpClientExperiment() {
        super();

        DefaultAsyncHttpClientConfig.Builder configBuilder = Dsl.config()
                .setConnectTimeout(SOCKET_CONNECTION_TIMEOUT)
                .setRequestTimeout(1100)
                .setFollowRedirect(false)
                .setMaxRequestRetry(0)
                .setMaxConnectionsPerHost(200)
                .setMaxConnections(1000)
                .setConnectionTtl(300_000)
                .setPooledConnectionIdleTimeout(300_000)
                .setSoReuseAddress(true)
                .setKeepAlive(true);
        if (System.getProperty("os.name").toUpperCase().startsWith("LINUX")) {
            configBuilder.setUseNativeTransport(true);
        }
        asyncHttpClient = new DefaultAsyncHttpClient(configBuilder.build());
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests", "100000");
        System.setProperty("execution.isolation.strategy", "SEMAPHORE");
        System.setProperty("hystrix.command.default.circuitBreaker.errorThresholdPercentage", "1");

        try (NewAsyncHttpClientExperiment oldAsyncHttpClientExperiment = new NewAsyncHttpClientExperiment()) {
            int result = oldAsyncHttpClientExperiment.experimentObservable();
            log.info("success {}, actually sent {}", result, oldAsyncHttpClientExperiment.requestSentCounter.get());
        }
    }

    @Override
    public void close() throws IOException {
        asyncHttpClient.close();
    }

    @Override
    protected Observable<String> getObservableResponse() {

        Request request = asyncHttpClient.prepareGet("http://localhost:8080/").build();
        requestSentCounter.incrementAndGet();
        return Single.<String>create(subscriber -> {
            if (subscriber.isDisposed()) {
                return;
            }
            asyncHttpClient.executeRequest(request, new AsyncCompletionHandler<Response>() {
                @Override
                public Response onCompleted(Response response) {
                    if (response.getStatusCode() == 200) {
                        subscriber.onSuccess(response.getResponseBody());
                    } else {
                        throw new RuntimeException("Not 200");
                    }
                    return response;
                }

                @Override
                public void onThrowable(Throwable t) {
                    subscriber.onError(t);
                }
            });
        }).toObservable();
    }
}
