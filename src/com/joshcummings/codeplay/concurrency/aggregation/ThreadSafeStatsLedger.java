package com.joshcummings.codeplay.concurrency.aggregation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import com.joshcummings.codeplay.concurrency.StatsLedger;

/**
 * A thread-safe stats ledger that exercises ConcurrentHashMap and LongAdder.
 * 
 */
public class ThreadSafeStatsLedger implements StatsLedger {
	private Map<String, LongAdder> firstNameMap = new ConcurrentHashMap<>();
	private Map<String, LongAdder> lastNameMap = new ConcurrentHashMap<>();
	private Map<Integer, LongAdder> ageMap = new ConcurrentHashMap<>();
	private LongAdder recordCount = new LongAdder();

	@Override
	public void recordEntry(StatsEntry entry) {
		increment(firstNameMap, entry.getFirstName());
		
		increment(lastNameMap, entry.getLastName());
		
		increment(ageMap, entry.getAge());
		
		recordCount.increment();
	}
	
	private <T> void increment(Map<T, LongAdder> map, T key) {
		map.computeIfAbsent(key, k -> new LongAdder()).increment();
	}

	@Override
	public Integer getRecordCount() {
		return recordCount.intValue();
	}

	@Override
	public Integer getAgeCount(Integer age) {
		return ageMap.get(age).intValue();
	}
	
	@Override
	public Integer getFirstNameCount(String firstName) {
		return firstNameMap.get(firstName).intValue();
	}
	
	@Override
	public Integer getLastNameCount(String lastName) {
		return lastNameMap.get(lastName).intValue();
	}
}
