package com.joshcummings.codeplay.concurrency.throttle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.joshcummings.codeplay.concurrency.Address;
import com.joshcummings.codeplay.concurrency.AddressVerifier;

public class BatchingAddressVerifier implements AddressVerifier {
	private BlockingQueue<BatchOperation> jobQueue = new LinkedBlockingQueue<>();
	
	private final AddressVerifier delegate;
	
	private final CyclicBarrier batcher;

	private final int batchSize;
	private final int timeout;
	
	private final ExecutorService fetcher = Executors.newFixedThreadPool(500);
	private final ExecutorService sender = Executors.newFixedThreadPool(500);
	private final ExecutorService latch = Executors.newFixedThreadPool(500);
	
	public BatchingAddressVerifier(AddressVerifier delegate, int batchSize, int timeout) {
		this.delegate = delegate;
		this.batchSize = batchSize;
		this.timeout = timeout;
		batcher = new CyclicBarrier(batchSize);
	}
	
	private void fetchThenSendBatch() {
		List<BatchOperation> batch = new ArrayList<>(batchSize);
		jobQueue.drainTo(batch, batchSize);
		
		sender.submit(() -> {
			List<Address> addresses = batch.stream().map(p -> p.a).collect(Collectors.toList());
			delegate.verify(addresses);
			batch.stream().forEach(p -> p.c.countDown());
		});
	}
	
	@Override
	public void verify(List<Address> addresses) {
		CountDownLatch cdl = new CountDownLatch(addresses.size());
		
		addresses.stream()
			.forEach(address -> {
				jobQueue.offer(new BatchOperation(address, cdl));
				fetcher.submit(() -> {
					try {
						if ( batcher.await(timeout, TimeUnit.MILLISECONDS) == 0 ) {
							fetchThenSendBatch();
						}
					}
					catch (TimeoutException | BrokenBarrierException | InterruptedException e) {
						fetchThenSendBatch();
					}
				});
			});
		
		latch.submit(() -> {
			try {
				cdl.await();
			} catch ( InterruptedException e ) {
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
		public final Address a;
		public final CountDownLatch c;
		
		public BatchOperation(Address a, CountDownLatch c) {
			this.a = a;
			this.c = c;
		}
	}
	
	public static Address mockAddress(int id) {
		return new Address(id + ": asdf", "asdf", "asdf", "asdf");
	}
	
/*	public static void main(String[] args) {
		int numberOfJobs = 9997;
		int batchSize = 10;
		AtomicInteger timesInvoked = new AtomicInteger(0);
		AtomicInteger addressVerified = new AtomicInteger(0);
		
		CountDownLatch cdl = new CountDownLatch(numberOfJobs);
		AddressVerifier av = new AddressVerifier() {
			@Override
			public void verify(List<Address> addresses) {
				timesInvoked.incrementAndGet();
				try { Thread.sleep(250); } catch ( InterruptedException e ) {  just move on  }
				addresses.forEach((address) -> {
					System.out.println("Verifying address #" + address.getAddress1());
					address.setVerified(true);
					cdl.countDown();
					addressVerified.incrementAndGet();
				});
			}
		};
		
		BatchingAddressVerifier pav = new BatchingAddressVerifier(av, batchSize, 500);
		
		Queue<Long> times = new ConcurrentLinkedQueue<>();
		
		Random rand = new Random();
		int total = 0;
		while ( total < numberOfJobs ) {
			int maxHowMany = Math.min(5, numberOfJobs - total);
			int howMany = rand.nextInt(maxHowMany) + 1;
			List<Address> addresses = new ArrayList<>(howMany);
			for ( int j = 0; j < howMany; j++ ) {
				addresses.add(mockAddress(total));
				total++;
			}
			
			new Thread(() -> {
				long time = System.nanoTime();
				pav.verify(addresses);
				times.add(System.nanoTime() - time);
			}).start();
		}
		
		System.out.println("Total: " + total);
		
		try {
			cdl.await();
		} catch ( InterruptedException e ) {
			System.out.println("Couldn't wait");
		}
		
		System.out.println(times);
		double average = times.stream().mapToLong((time) -> time.longValue()).average().getAsDouble() / 1000000;
		System.out.println(average);
		System.out.println(timesInvoked);
		System.out.println(average * timesInvoked.get());
		
		pav.es.shutdown();
		
		System.out.println("Total verified: " + addressVerified.get());
	}*/
}