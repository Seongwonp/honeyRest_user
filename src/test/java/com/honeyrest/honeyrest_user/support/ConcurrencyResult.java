package com.honeyrest.honeyrest_user.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** 같은 작업을 여러 스레드에서 동시에 시작해 성공 수와 실패 예외를 모은다 (동시성 회귀 테스트용). */
public record ConcurrencyResult(int successes, List<Throwable> failures) {

    @FunctionalInterface
    public interface Task {
        void run(int index) throws Exception;
    }

    public static ConcurrencyResult run(int threads, Task task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        task.run(index);
                        successes.incrementAndGet();
                    } catch (Throwable t) {
                        failures.add(t);
                    }
                    return null;
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        return new ConcurrencyResult(successes.get(), List.copyOf(failures));
    }
}
