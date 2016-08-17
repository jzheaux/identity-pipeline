package com.joshcummings.codeplay.concurrency.throttle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.joshcummings.codeplay.concurrency.Address;
import com.joshcummings.codeplay.concurrency.AddressVerifier;

/**
 * An example of using Phaser--an advanced concurrency class for coordinating threads.
 * 
 * This class will add method invocations to a pool of tasks and them pull them off 
 * in batches, delegating those batches down to the underlying address verifier.
 * 
 * For simplicity in the demo, this class assumes usage of the AddressVerifier, however,
 * it has been written in such a way as to make it generalizable.
 *
 */
public class PhaserBatcher implements Batcher {
	private BlockingQueue<PhasedOperation> jobQueue = new LinkedBlockingQueue<>();
	
	private final Phaser batcher;
	
	// batchSize is an AtomicInteger since this class allows for throttling up and down, which
	// is happening on a different thread from the fetching process local to this class
	private final AtomicInteger batchSize;
	
	private final AddressVerifier delegate;
	
	private final ExecutorService fetcher = Executors.newFixedThreadPool(1);
	private final ExecutorService sender = Executors.newFixedThreadPool(500);
	private final ExecutorService latch = Executors.newFixedThreadPool(500);
	
	/**
	 * 
	 * @param batchSize - The target size for each batch. If the timeout is reached before
	 * 	the batch is full, then the incomplete batch will be sent
	 * @param timeout - How long to wait for each batch to fill
	 * @param delegate - The underlying AddressVerifier to invoke
	 * 
	 */
	public PhaserBatcher(int batchSize, int timeout, AddressVerifier delegate) {
		this.batchSize = new AtomicInteger(batchSize);
		this.delegate = delegate;
		this.batcher = new Phaser(batchSize) {
			@Override
			/**
			 * Answers the question: Should this phaser stop?
			 * 
			 * Note that some pretty serious exceptions may occur if this has a significant
			 * amount of logic in it. 
			 */
			protected boolean onAdvance(int phase, int registeredParties) {
				return false;
			}
		};
		
		// There is one thread here doing all the work; but if necessary, more could be introduced
		// by splitting up the phases, say by a modulo of the number of threads. This can get much
		// more sophisticated depending on your needs, and I'd encourage those interested to read
		// up on how Kafka's consumer client works.
		fetcher.submit(() -> {
			int phase = 0;
			while ( !Thread.currentThread().isInterrupted() ) {
				try {
					batcher.awaitAdvanceInterruptibly(phase, timeout, TimeUnit.MILLISECONDS);
					fetchThenSendBatch();
					phase++;
				} catch ( TimeoutException e ) {
					// If we time out, then that simply means it's time to send the batch
					// anyway, even if it isn't full
					fetchThenSendBatch();
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
				}
			}
		});
	}
	
	private void fetchThenSendBatch() {
		List<PhasedOperation> batch = new ArrayList<>(batchSize.get());
		
		// This will pull off up to batchSize elements. Precisely what we want.
		jobQueue.drainTo(batch, batchSize.get());
		
		sender.submit(() -> {
			List<Address> jobs = batch.stream().map(p -> p.address).collect(Collectors.toList());
			delegate.verify(jobs);
			batch.stream().forEach(p -> p.phaserLatch.arrive());
		});
	}
	
	public void throttleUp(int by) {
		// note that this is a compound operation. Since our batchSize is only 
		// a target, we are okay with the occasional inconsistency when, say,
		// the phaser's registration count increases and then advances before 
		// we are able to increment batchSize.
		batcher.bulkRegister(by);
		batchSize.addAndGet(by);
	}
	
	public void throttleDown(int by) {
		for ( int i = 0; i < by; i++ ) {
			// note that this is a compound operation. See comments above.
			batcher.arriveAndDeregister();
			batchSize.decrementAndGet();
		}
	}

	@Override
	public Future<?> submit(List<Address> addresses) {
		// We don't need to override the onAdvance method for this Phaser since
		// we'll only use it once
		Phaser phaserLatch = new Phaser(addresses.size());
		
		addresses.forEach(address -> {
			jobQueue.offer(new PhasedOperation(address, phaserLatch));
			try {
				batcher.arrive();
			} catch ( IllegalStateException e ) {
				// under heavy concurrency, I found that arrive() would throw an
				// IllegalStateException because of a race condition precipitated
				// from having more threads sent simultaneously into submit than the batch size.
				// See https://bugs.openjdk.java.net/browse/JDK-7058828 for details.
				// In our case, this still works, but in situations similar to these, but
				// where invocation loss isn't acceptable, you can synchronize
				// the call to arrive to prevent the race condition.
			}
		});
		
		return latch.submit(() -> {
			// wait for the zeroth phase to advance
			phaserLatch.awaitAdvance(0);
		});
	}
	
	public void close() {
		fetcher.shutdownNow();
		sender.shutdownNow();
		latch.shutdownNow();
	}
	
	private static class PhasedOperation {
		public final Address address;
		public final Phaser phaserLatch;
		
		public PhasedOperation(Address a, Phaser p) {
			this.address = a;
			this.phaserLatch = p;
		}
	}
}
