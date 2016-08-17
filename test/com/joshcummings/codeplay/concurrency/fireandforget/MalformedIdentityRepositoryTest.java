package com.joshcummings.codeplay.concurrency.fireandforget;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.MalformedIdentityRepository;

public class MalformedIdentityRepositoryTest {	
	private static final int NUMBER_OF_IDENTITIES = 5000;
	
	private static class MalformedIdentityRepositoryStub implements MalformedIdentityRepository {
		private CountDownLatch cdl = new CountDownLatch(NUMBER_OF_IDENTITIES * 2);
		public int j;
		
		@Override
		public void addIdentity(InputStream message, String reason) {
			for ( int i = 0; i < 1000000; i++ ) {
				j+=i;
			}
			cdl.countDown();
		}
		
		@Override
		public void addIdentity(Identity identity, String reason) {
			for ( int i = 0; i < 1000000; i++ ) {
				j+=i;
			}
			cdl.countDown();
		}
		
		public void await() throws InterruptedException {
			cdl.await();
		}
	};
	
	@Test
	public void testProducerConsumer() throws Exception {
		MalformedIdentityRepositoryStub delegate = new MalformedIdentityRepositoryStub();
		MalformedIdentityRepository pc = new ProducerMalformedIdentityRepository(delegate);
		for ( int i = 0; i < NUMBER_OF_IDENTITIES; i++ ) {
			pc.addIdentity((Identity)null, "");
			pc.addIdentity((InputStream)null, "");
		}
		delegate.await();
	}

	@Test
	public void testThreadPoolExecutor() throws Exception {
		MalformedIdentityRepositoryStub delegate = new MalformedIdentityRepositoryStub();
		MalformedIdentityRepository ex = new ThreadPoolExecutorMalformedIdentityRepository(delegate);
		for ( int i = 0; i < NUMBER_OF_IDENTITIES; i++ ) {
			ex.addIdentity((Identity)null, "");
			ex.addIdentity((InputStream)null, "");
		}
		delegate.await();
	}

	@Test
	public void testForkJoinPool() throws Exception {
		MalformedIdentityRepositoryStub delegate = new MalformedIdentityRepositoryStub();
		MalformedIdentityRepository fjp = new ForkJoinPoolMalformedIdentityRepository(delegate);
		for ( int i = 0; i < NUMBER_OF_IDENTITIES; i++ ) {
			fjp.addIdentity((Identity)null, "");
			fjp.addIdentity((InputStream)null, "");
		}
		delegate.await();
	}
}
