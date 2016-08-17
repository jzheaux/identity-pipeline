package com.joshcummings.codeplay.concurrency.fireandforget;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.MalformedIdentityRepository;

/**
 * An implementation of fire-and-forget that naively submits the underlying task to a ForkJoinPool.
 * 
 */
public class ForkJoinPoolMalformedIdentityRepository implements
		MalformedIdentityRepository {
	// To get a work-stealing thread pool is as simple as calling a different static
	// method.

	private ExecutorService pool = Executors.newWorkStealingPool();

	private MalformedIdentityRepository delegate;
	
	public ForkJoinPoolMalformedIdentityRepository(MalformedIdentityRepository delegate) {
		this.delegate = delegate;
	}
	
	// Notice the similarities. Using a work-stealing pool doesn't really 
	// buy the application any performance enhancements in this situation because 
	// there are no running threads that are blocked on the execution of other threads;
	// here, each thread's job stands independent of any other
	@Override
	public void addIdentity(Identity identity, String reason) {
		pool.submit(() -> delegate.addIdentity(identity, reason));
	}

	@Override
	public void addIdentity(InputStream message, String reason) {
		pool.submit(() -> delegate.addIdentity(message, reason));
	}
}
