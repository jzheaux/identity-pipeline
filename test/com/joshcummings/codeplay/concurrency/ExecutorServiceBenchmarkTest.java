package com.joshcummings.codeplay.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

public class ExecutorServiceBenchmarkTest {
	@Test
	public void usingExecutorService() throws Exception {
		ExecutorService es = Executors.newCachedThreadPool();
		for ( int i = 0; i < 1000; i++ ) {
			Future<?> one = es.submit(this::doTask);
			Future<?> two = es.submit(this::doTask);
			Future<?> three = es.submit(this::doTask);
			Future<?> four = es.submit(this::doTask);
			
			one.get();
			two.get();
			three.get();
			four.get();
		}
	}
	
	@Test
	public void usingThreadDirectly() throws Exception {
		for ( int i = 0; i < 1000; i++ ) {
			Thread one = new Thread(this::doTask);
			Thread two = new Thread(this::doTask);
			Thread three = new Thread(this::doTask);
			Thread four = new Thread(this::doTask);
			
			one.start();
			two.start();
			three.start();
			four.start();
			
			one.join();
			two.join();
			three.join();
			four.join();
		}
	}
	
	private void doTask() {
		for ( int i = 0; i < 10; i++ ) {
			try {
				Thread.sleep(1);
			} catch ( InterruptedException e ) {
				// keep going
			}
		}
	}
}
