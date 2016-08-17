package com.joshcummings.codeplay.concurrency.fireandforget;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.MalformedIdentityRepository;

/**
 * An implementation of fire-and-forget that naively submits the underlying task to a ThreadPoolExecutor.
 * 
 */
public class ThreadPoolExecutorMalformedIdentityRepository implements
		MalformedIdentityRepository {
	/* 
	 * Employing the Decorator pattern makes it easier for us to focus on the concurrency
	 * code in isolation and enable the class at the same time to participate in a larger
	 * application context. 
	 */

	private MalformedIdentityRepository delegate;

	public ThreadPoolExecutorMalformedIdentityRepository(MalformedIdentityRepository delegate) {
		this.delegate = delegate;
	}

	/* 
	 * Specifically, Decorator pattern’s layer of indirection here allows this new class
	 * to pivot away from waiting directly on the completion of addIdentity. The underlying
	 * addIdentity can take as much time as it needs, independent of the continuing operation
	 * of the caller.
	 */

	/*
	 * I instantiate the thread pool as a field because I want the pool of threads to be
	 * available across multiple method calls. This repository class will be treated as a singleton,
	 * which means that all invocations to addIdentity will request threads from the same pool.
	 */
	private ExecutorService pool = Executors.newCachedThreadPool();
	
	@Override
	public void addIdentity(Identity identity, String reason) {
		/*
		 * Implementing the fire and forget pattern here is as simple as moving our delegate call
		 * to the underlying addIdentity into a lambda expression, which in this case is a syntactic
		 * reduction of an anonymous implementation of the Runnable interface.
		 */
		pool.submit(() -> delegate.addIdentity(identity, reason));
		//new Thread(() -> delegate.addIdentity(identity, reason)).start(); // <== SLOWER!!!
	}

	@Override
	public void addIdentity(InputStream message, String reason) {
		pool.submit(() -> delegate.addIdentity(message, reason));
	}

}
