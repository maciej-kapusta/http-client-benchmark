package com.codewise.httpclientbench;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(4)
@State(Scope.Benchmark)
public class TestHarness {

    private HttpServer server;
    private BaseHttpExperiment oldAsynchExperiment;
    private NewAsyncHttpClientExperiment newAsynchExperiment;

    @Setup(Level.Trial)
    public void setUp() {
        server = new HttpServer(8080);

        sleepUninterruptibly(4, TimeUnit.SECONDS);
        oldAsynchExperiment = new OldAsyncHttpClientExperiment();
        newAsynchExperiment = new NewAsyncHttpClientExperiment();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        oldAsynchExperiment.close();
        newAsynchExperiment.close();
        server.close();
    }

    @Benchmark
    public int benchmarkOld() throws Exception {
        return oldAsynchExperiment.experimentObservable();
    }

    @Benchmark
    public int benchmarkNew() throws Exception {
        return newAsynchExperiment.experimentObservable();
    }

    public static void main(String[] args) throws RunnerException {

        Options opts = new OptionsBuilder()
                .include(".*" + TestHarness.class.getSimpleName() + ".*")
                .build();
        new Runner(opts).run();
    }

    private void sleepUninterruptibly(int time, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(time));
        } catch (InterruptedException e) {
            //ignored
        }
    }
}
