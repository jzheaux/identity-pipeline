package com.joshcummings.codeplay.concurrency.throttle;

import java.util.List;
import java.util.concurrent.Semaphore;

import com.joshcummings.codeplay.concurrency.Address;
import com.joshcummings.codeplay.concurrency.AddressVerifier;

/**
 * An example of Semphore--a lock-issuing concurrency class similar to Java's Lock family.
 * 
 * This class demonstrates 1) how one can limit connections to an underlying method invocation
 * and 2) how one can increase and decrease the limit at runtime.
 * 
 */
public class ConnectionLimitingAddressVerifier implements AddressVerifier {
	private ConfigurableSemaphore limiter;
	private AddressVerifier delegate;
	
	public ConnectionLimitingAddressVerifier(AddressVerifier delegate, int permits) {
		this.delegate = delegate;
		this.limiter = new ConfigurableSemaphore(permits);
	}
	
	@Override
	public void verify(List<Address> address) {
		try {
			// It's possible for the current thread to call release even if acquire failed,
			// so we need to wrap the acquire call in an outer try-catch block so it doesn't
			// share the finally of the inner try-catch.
			limiter.acquire();
			try {
				delegate.verify(address);
			} finally {
				// don't forget to put this in a finally! This follows the same principle as
				// closing files and network connections
				limiter.release();
			}
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}
	
	public void throttleUp(int by) {
		limiter.increasePermits(by);
	}
	
	public void throttleDown(int by) {
		limiter.reducePermits(by);
	}
	
	public static class ConfigurableSemaphore extends Semaphore {
		private static final long serialVersionUID = 1L;

		public ConfigurableSemaphore(int permits) {
			super(permits);
		}

		public void increasePermits(int by) {
			super.release(by);
		}
		
		public void reducePermits(int by) {
			super.reducePermits(by);
		}
		
	}
	
	
	public void close() {
		limiter.drainPermits();
	}
}
