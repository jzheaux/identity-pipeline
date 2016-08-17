package com.joshcummings.codeplay.concurrency.throttle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.joshcummings.codeplay.concurrency.Address;
import com.joshcummings.codeplay.concurrency.AddressVerifier;

/**
 * An example of using CyclicBarrer--an advanced concurrency class for coordinating threads
 * 
 * This class will add method invocations to a pool of tasks and them pull them off 
 * in batches, delegating those batches down to the underlying address verifier.
 * 
 * For simplicity in the demo, this class assumes usage of the AddressVerifier, however,
 * it has been written in such a way as to make it generalizable.
 * 
 */
public class CyclicBarrierBatcher implements Batcher {
	/* The backlog of jobs to perform */
	private BlockingQueue<BatchOperation> jobQueue = new LinkedBlockingQueue<>();
	
	/* The cyclic barrier causes {batchSize} threads to wait at a time */
	private final CyclicBarrier batcher;

	private final int batchSize;
	private final int timeout;
	
	private final ExecutorService fetcher = Executors.newFixedThreadPool(500);
	private final ExecutorService sender = Executors.newFixedThreadPool(500);
	private final ExecutorService latch = Executors.newFixedThreadPool(500);
	
	private final AddressVerifier delegate;
	
	/**
	 * 
	 * @param batchSize - The target size for each batch. If the timeout is reached before
	 * 	the batch is full, then the incomplete batch will be sent
	 * @param timeout - How long to wait for each batch to fill
	 * @param delegate - The underlying AddressVerifier to invoke
	 * 
	 */
	public CyclicBarrierBatcher(int batchSize, int timeout, AddressVerifier delegate) {
		this.batchSize = batchSize;
		this.timeout = timeout;
		batcher = new CyclicBarrier(batchSize);
		this.delegate = delegate;
	}
	
	private void fetchThenSendBatch() {
		List<BatchOperation> batch = new ArrayList<>(batchSize);
		
		// This will pull off up to batchSize elements. Precisely what we want.
		jobQueue.drainTo(batch, batchSize);
		
		sender.submit(() -> {
			List<Address> addresses = batch.stream().map(p -> p.address).collect(Collectors.toList());
			delegate.verify(addresses);
			batch.stream().forEach(p -> p.countDownLatch.countDown());
		});
	}
	
	public Future<?> submit(List<Address> addresses) {
		CountDownLatch cdl = new CountDownLatch(addresses.size());
		
		addresses.stream()
			.forEach(address -> {
				jobQueue.offer(new BatchOperation(address, cdl));
				fetcher.submit(() -> {
					try {
						// My experience has been that this is a more effective pattern
						// for executing the task common to the waiting threads than
						// supplying a Runnable in the CyclicBarrier constructor.
						// See the lead description at
						// https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CyclicBarrier.html
						if ( batcher.await(timeout, TimeUnit.MILLISECONDS) == 0 ) {
							fetchThenSendBatch();
						}
					} catch (TimeoutException | BrokenBarrierException e) {
						fetchThenSendBatch();
					} catch ( InterruptedException e ) {
						Thread.currentThread().interrupt();
					}
				});
			});
		
		return latch.submit(() -> {
			try {
				cdl.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}
	
	public void close() {
		fetcher.shutdownNow();
		sender.shutdownNow();
		latch.shutdownNow();
	}
	
	private static class BatchOperation {
		public final Address address;
		public final CountDownLatch countDownLatch;
		
		public BatchOperation(Address a, CountDownLatch c) {
			this.address = a;
			this.countDownLatch = c;
		}
	}
}
