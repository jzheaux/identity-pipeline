package com.joshcummings.codeplay.concurrency.throttling;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.joshcummings.codeplay.concurrency.Address;
import com.joshcummings.codeplay.concurrency.AddressVerifier;
import com.joshcummings.codeplay.concurrency.throttle.BackpressureAddressVerifier;
import com.joshcummings.codeplay.concurrency.throttle.ConnectionLimitingAddressVerifier;
import com.joshcummings.codeplay.concurrency.throttle.CyclicBarrierBatcherAddressVerifier;
import com.joshcummings.codeplay.concurrency.throttle.PhaserBatcherAddressVerifier;

public class AddressVerifierTest {
	private CountDownLatch cdl;
	private Queue<Long> serverTimes;
	private Queue<Long> clientTimes;
	
	private AtomicInteger serverInvocations;
	private Integer clientInvocations;
	private AtomicInteger rejectedCount;
	private AtomicInteger verifiedCount;
	
	private AddressVerifier av = new AddressVerifier() {
		@Override
		public void verify(List<Address> addresses) {
			long time = System.nanoTime();
			
			serverInvocations.incrementAndGet();

			for ( int i = 0; i < 250; i++ ) {
				try { 			
					Thread.sleep(1);
				} catch ( InterruptedException e ) { 
					// just move on
				}
			}

			addresses.forEach((address) -> {
				address.setVerified(true);
			});
			serverTimes.add(System.nanoTime() - time);
		}
	};
	
	private ExecutorService clientPool;
	
	@Before
	public void setUp() {
		cdl = new CountDownLatch(NUMBER_OF_ADDRESSES);
		serverTimes = new ConcurrentLinkedQueue<>();
		clientTimes = new ConcurrentLinkedQueue<>();
		serverInvocations = new AtomicInteger();
		clientInvocations = 0;
		rejectedCount = new AtomicInteger();
		verifiedCount = new AtomicInteger();
		
		clientPool = Executors.newCachedThreadPool();
		
		if ( WAIT_FOR_JVISUALVM ) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@After
	public void report() {
		double average = clientTimes.stream().mapToLong((time) -> time.longValue()).average().getAsDouble() / 1000000;
		double delegateAverage = serverTimes.stream().mapToLong((time) -> time.longValue()).average().getAsDouble() / 1000000;
		System.out.println("Number of client verifications: " + clientInvocations);
		System.out.println("Number of server verifications: " + serverInvocations);
		System.out.println("Average amount of time client spent waiting: " + average);
		System.out.println("Average amount of time spent invoking actual address verifier: " + delegateAverage);
		System.out.println("Total computation time spent in client address verifier: " + Math.round(average * serverInvocations.get()));
		System.out.println("Total computation time spent in actual address verifier: " + Math.round(delegateAverage * clientInvocations));
		System.out.println("Speed up: " + (delegateAverage * clientInvocations) / (average * serverInvocations.get()) );
		System.out.println("Number verified: " + verifiedCount.get());
		System.out.println("Number rejected: " + rejectedCount.get());
		clientPool.shutdownNow();
	}
	
	private Address mockAddress(int id) {
		return new Address(id + ": asdf", "asdf", "asdf", "asdf") {
			@Override
			public void setVerified(boolean verified) {
				verifiedCount.incrementAndGet();
				cdl.countDown();
				super.setVerified(verified);
			}
		};
	}
	
	private static final int NUMBER_OF_ADDRESSES = 99997;
	private static final int ARRIVAL_DELAY = 2;
	private static final boolean WAIT_FOR_JVISUALVM = true;
	
	@Test
	public void testBackpressure() {
		BackpressureAddressVerifier bav = new BackpressureAddressVerifier(av, 380, 60);
		hammer(bav, NUMBER_OF_ADDRESSES);
	}
	
	@Test
	public void testConnectionLimiting() {
		ConnectionLimitingAddressVerifier clav = new ConnectionLimitingAddressVerifier(av, 380);
		hammer(clav, NUMBER_OF_ADDRESSES);
	}
	
	@Test
	public void testCyclicBarrier() {
		CyclicBarrierBatcherAddressVerifier cbbav = new CyclicBarrierBatcherAddressVerifier(av, 100, 50);
		hammer(cbbav, NUMBER_OF_ADDRESSES);
	}
	
	@Test
	public void testPhaser() {
		PhaserBatcherAddressVerifier pbav = new PhaserBatcherAddressVerifier(av, 100, 50);
		hammer(pbav, NUMBER_OF_ADDRESSES);
	}
	
	private void hammer(AddressVerifier av, int numberOfAddresses) {	
		Random rand = new Random(676325345568L);
		int total = 0;
		long arrivalStart = System.nanoTime();
		while ( total < numberOfAddresses ) {
			int maxHowMany = Math.min(5, numberOfAddresses - total);
			int howMany = rand.nextInt(maxHowMany) + 1;
			List<Address> addresses = new ArrayList<>(howMany);
			for ( int j = 0; j < howMany; j++ ) {
				addresses.add(mockAddress(total));
			}
			total += howMany;
			clientInvocations++;

			if ( ARRIVAL_DELAY > 0 ) {
				try {
					Thread.sleep(rand.nextInt(ARRIVAL_DELAY));
				} catch ( InterruptedException e ) {
					
				}
			}
			
			clientPool.submit(() -> {
				long time = System.nanoTime();
				try {
					av.verify(addresses);
				} catch ( Throwable e ) {
					for ( int i = 0; i < addresses.size(); i++ ) {
						rejectedCount.incrementAndGet();
						cdl.countDown();
					}
				}
				clientTimes.add(System.nanoTime() - time);
			});
		}
		System.out.println("Arrival rate: " + clientInvocations / ( ( System.nanoTime() - arrivalStart ) / 1000000000d ) );
		
		try {
			cdl.await();
		} catch ( InterruptedException e ) {
			System.out.println("Couldn't wait");
		}
		
		av.close();
	}
}
