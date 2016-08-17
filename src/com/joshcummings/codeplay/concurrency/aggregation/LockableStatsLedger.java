package com.joshcummings.codeplay.concurrency.aggregation;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.joshcummings.codeplay.concurrency.StatsLedger;

/**
 * A stats ledger that naively wraps the recordEntry call in a ReentrantLock, effectively 
 * synchronizing access to the method.
 *
 */
public class LockableStatsLedger implements StatsLedger {
	private volatile StatsLedger delegate;

	private ReentrantLock lock = new ReentrantLock();
	
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public LockableStatsLedger(StatsLedger delegate) {
		this.delegate = delegate;
		scheduler.scheduleAtFixedRate(this::publish, 1000, 5000, TimeUnit.MILLISECONDS);
	}
	
	private <T> T withLock(Supplier<T> supplier) {
		lock.lock();
		try {
			return supplier.get();
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void recordEntry(StatsEntry entry) {
		withLock(() -> { delegate.recordEntry(entry); return null; });
	}
	
	@Override
	public Integer getRecordCount() {
		lock.lock();
		try {
			return delegate.getRecordCount();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Integer getAgeCount(Integer age) {
		return delegate.getAgeCount(age);
	}
	
	@Override
	public Integer getFirstNameCount(String firstName) {
		return delegate.getFirstNameCount(firstName);
	}
	
	@Override
	public Integer getLastNameCount(String lastName) {
		return delegate.getLastNameCount(lastName);
	}

	public void publish() {
		System.out.println("Number of Records: " + getRecordCount());
	}

}
