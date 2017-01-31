package com.codewise.httpclientbench;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class OldAsyncHttpClientExperiment extends BaseHttpExperiment {

    private static final Logger log = LoggerFactory.getLogger(OldAsyncHttpClientExperiment.class);

    private static final int SOCKET_CONNECTION_TIMEOUT = 3000;
    private static final int CONNECTION_TTL_IN_MILLIS = 300000; //5 minutes
    private final AsyncHttpClient asyncHttpClient;
    private final AtomicInteger requestSentCounter = new AtomicInteger(0);

    public OldAsyncHttpClientExperiment() {
        super();

        asyncHttpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
                .setAllowPoolingConnections(true)
                .setConnectTimeout(SOCKET_CONNECTION_TIMEOUT)
                .setRequestTimeout(1100)
                .setFollowRedirect(false)
                .setMaxRequestRetry(0)
                .setConnectionTTL(CONNECTION_TTL_IN_MILLIS)
                .build());
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests", "100000");
        System.setProperty("execution.isolation.strategy", "SEMAPHORE");
        System.setProperty("hystrix.command.default.circuitBreaker.errorThresholdPercentage", "1");

        try (OldAsyncHttpClientExperiment oldAsyncHttpClientExperiment = new OldAsyncHttpClientExperiment()) {
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
                    try {
                        if (response.getStatusCode() == 200) {
                            subscriber.onSuccess(response.getResponseBody());
                        } else {
                            throw new RuntimeException("Not 200");
                        }
                    } catch (IOException e) {
                        subscriber.onError(e);
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
