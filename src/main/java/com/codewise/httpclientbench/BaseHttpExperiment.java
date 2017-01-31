package com.codewise.httpclientbench;

import io.reactivex.Observable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class BaseHttpExperiment implements AutoCloseable {

    public int experimentObservable() throws Exception {
        List<Observable<String>> observables = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            observables.add(getObservableResponse());
        }

        return Observable.merge(observables)
                .filter(s -> !s.isEmpty())
                .toList()
                .timeout(2, TimeUnit.SECONDS)
                .blockingGet()
                .size();
    }

    protected abstract Observable<String> getObservableResponse() throws IOException;
}
