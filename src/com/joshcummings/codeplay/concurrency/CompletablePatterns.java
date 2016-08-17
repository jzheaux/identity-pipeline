package com.joshcummings.codeplay.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CompletablePatterns {
	public static <T> CompletableFuture<T> tryAnyOf(CompletableFuture<T>... futures) {
		final AtomicInteger completedExceptionally = new AtomicInteger();
		final CompletableFuture<T> promise = new CompletableFuture<>();
		for (CompletableFuture<T> future : futures) {
			future.whenComplete((result, ex) -> {
				if (ex == null) {
					if (!promise.isDone()) {
						promise.complete(result);
					}
				} else {
					if (completedExceptionally.incrementAndGet() == futures.length) {
						promise.completeExceptionally(ex);
					}
				}
			});
		}
		return promise;
	}
}
