package com.joshcummings.codeplay.concurrency.aggregation;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.Person;
import com.joshcummings.codeplay.concurrency.StatsLedger;
import com.joshcummings.codeplay.concurrency.StatsLedger.StatsEntry;


public class StatsLedgerTest {
	private Identity identity = new Person(
			"jim",
			"jim".toCharArray(),
			"Jim Chetrey",
			null, null, Collections.emptyList(),
			34);
	private StatsEntry se = new StatsEntry(identity);
	
	private ExecutorService testPool = Executors.newWorkStealingPool();
	private final Integer NUM_IDENTITIES = 1000000;
	
	@Test
	public void testThreadUnsafeVersion() throws Exception {
		StatsLedger sl = new SimpleStatsLedger();
		testIdentities(sl);
	}

	private void testIdentities(StatsLedger sl) throws Exception {
		CountDownLatch cdl = new CountDownLatch(NUM_IDENTITIES);
		for ( int i = 0; i < NUM_IDENTITIES; i++ ) {
			testPool.submit(() -> {
				sl.recordEntry(new StatsEntry(identity));
				cdl.countDown();
			});
		}
		cdl.await();
		
		System.out.println("===============" + sl.getClass().getSimpleName() + "==============");
		System.out.println("Number of records processed: " + sl.getRecordCount());
		System.out.println("Number of ages processed: " + sl.getAgeCount(se.getAge()));
		System.out.println("Number of first names processed: " + sl.getFirstNameCount(se.getFirstName()));
		System.out.println("Number of last names processed: " + sl.getLastNameCount(se.getLastName()));
	}
	
	@Test
	public void testThreadSafeVersion() throws Exception {
		StatsLedger sl = new ThreadSafeStatsLedger();
		testIdentities(sl);
	}
	
	@Test
	public void testLockableVersion() throws Exception {
		StatsLedger sl = new LockableStatsLedger(new SimpleStatsLedger());
		testIdentities(sl);
	}
}
