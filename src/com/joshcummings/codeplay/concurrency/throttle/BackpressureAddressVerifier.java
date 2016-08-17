package com.joshcummings.codeplay.concurrency.throttle;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.joshcummings.codeplay.concurrency.Address;
import com.joshcummings.codeplay.concurrency.AddressVerifier;
import com.joshcummings.codeplay.concurrency.NoValidAddressesException;

/**
 * An example of ThreadPoolExecutor--a thread-management class.
 * 
 * This demonstrates how ThreadPoolExecutor can be used to apply backpressure on top of
 * limiting the number of threads available at a time to the process.
 * 
 */
public class BackpressureAddressVerifier implements AddressVerifier {

	private ThreadPoolExecutor pool;
	private AddressVerifier delegate;
	
	/**
	 * 
	 * @param delegate - The underlying address verifier to invoke
	 * @param workers - The number of concurrent workers allowed; this is primarily to keep the 
	 * 	underlying method call healthy
	 * @param lineLength - The number of invocations allowed to wait in line simultaneously; this is
	 * 	primarily to keep the original caller healthy instead of obligating him to wait in line
	 *  for an excessively long time
	 */
	public BackpressureAddressVerifier(AddressVerifier delegate, int workers, int lineLength) {
		this.delegate = delegate;
		this.pool = new ThreadPoolExecutor(workers, workers, 60L, TimeUnit.SECONDS,
						new ArrayBlockingQueue<>(lineLength), new RejectedExecutionHandler() {
							@Override
							public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
								// There are a number of possibilities about what could go here,
								// including logging the behavior, taking metrics, programmatically
								// recognizing that the workers may need throttling, etc.
								throw new NoValidAddressesException();
							}
			
						});
	}
	
	@Override
	public void verify(List<Address> addresses) {
		try {
			pool.submit(() -> 
				delegate.verify(addresses)
			).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			if ( e.getCause() instanceof NoValidAddressesException ) {
				throw (NoValidAddressesException)e.getCause();
			}
			throw new IllegalStateException(e);
		}
	}
	
	public void throttleUp(int by) {
		// It's not possible to change the size of ArrayBlockingQueue after it is instantiated;
		// however, we can indirectly make the line shorter by increasing the number of
		// threads.
		pool.setMaximumPoolSize(pool.getMaximumPoolSize() + by);
	}
	
	public void throttleDown(int by) {
		// It's not possible to change the size of ArrayBlockingQueue after it is instantiated;
		// however, we can indirectly make the line longer up to the configured cap by decreasing
		// the number of threads.
		pool.setMaximumPoolSize(pool.getMaximumPoolSize() - by);
	}
	
	public void close() {
		pool.shutdown();
	}
}
