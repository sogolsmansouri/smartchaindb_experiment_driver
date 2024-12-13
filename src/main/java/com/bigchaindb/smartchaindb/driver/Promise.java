package com.bigchaindb.smartchaindb.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class Promise {

    /**
     * Executes a collection of Supplier tasks asynchronously and returns a CompletableFuture
     * that completes with a list of Settlement objects once all tasks have settled.
     *
     * @param tasks The collection of Supplier tasks to execute.
     * @param <T>   The type of the result produced by the tasks.
     * @return A CompletableFuture containing a list of Settlements for each task.
     */
    public static <T> CompletableFuture<List<Settlement<T>>> allSettled(Collection<Supplier<T>> tasks) {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(tasks.size(), Runtime.getRuntime().availableProcessors()));
        List<CompletableFuture<Settlement<T>>> futures = new ArrayList<>();

        for (Supplier<T> task : tasks) {
            CompletableFuture<Settlement<T>> future = CompletableFuture.supplyAsync(task, executor)
                    .<Settlement<T>>thenApply(Settlement::fulfilled)
                    .exceptionally(Settlement::rejected);
            futures.add(future);
        }

        // Combine all futures into one CompletableFuture
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // After all are done, collect the results
        CompletableFuture<List<Settlement<T>>> allSettledFuture = allOf.thenApply(v -> {
            List<Settlement<T>> settlements = new ArrayList<>();
            for (CompletableFuture<Settlement<T>> future : futures) {
                try {
                    settlements.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    settlements.add(Settlement.rejected(e));
                }
            }
            executor.shutdown();
            return settlements;
        });

        return allSettledFuture;
    }
}
