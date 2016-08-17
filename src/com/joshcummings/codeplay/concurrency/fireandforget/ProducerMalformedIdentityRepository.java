package com.joshcummings.codeplay.concurrency.fireandforget;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveAction;

import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.MalformedIdentityRepository;

/**
 * An implementation of fire-and-forget that manages its own pool of tasks and consumers.
 * 
 */
public class ProducerMalformedIdentityRepository implements
		MalformedIdentityRepository {
	private MalformedIdentityRepository delegate;

	private BlockingQueue<Runnable> todo = new LinkedBlockingQueue<>();
	
	//private ExecutorService pool = ForkJoinPool.commonPool();
	
	public ProducerMalformedIdentityRepository(MalformedIdentityRepository delegate) {
		this.delegate = delegate;
		
		// Typically, the consumer would be a separate process not under the control
		// of this class. It is included here as an inner class for simplicity
		new ForkJoinPool(4).execute(new Consumer());
	}
	
	@Override
	public void addIdentity(Identity identity, String reason) {
		todo.offer(() -> delegate.addIdentity(identity, reason));
	}
	
	@Override
	public void addIdentity(InputStream message, String reason) {
		todo.offer(() -> delegate.addIdentity(message, reason));
	}
	
	// =================== Consumer Starts Here =========================
	
	private class Consumer extends RecursiveAction {
		private static final long serialVersionUID = 1L;

		@Override
		protected void compute() {
			try {
				Runnable r = todo.take();
				new Consumer().fork(); // concurrent recursive invocation
				r.run();
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
		}
		
	}
}
