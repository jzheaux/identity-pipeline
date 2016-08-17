package com.joshcummings.codeplay.concurrency.aggregation;

import java.util.HashMap;
import java.util.Map;

import com.joshcummings.codeplay.concurrency.StatsLedger;

/**
 * A thread unsafe stats ledger.
 * 
 * @author Josh
 *
 */
public class SimpleStatsLedger implements StatsLedger {
	private Map<String, Integer> firstNameMap = new HashMap<>();
	private Map<String, Integer> lastNameMap = new HashMap<>();
	private Map<Integer, Integer> ageMap = new HashMap<>();
	private Integer recordCount = 0;

	@Override
	public void recordEntry(StatsEntry entry) {
		increment(firstNameMap, entry.getFirstName());
		
		increment(lastNameMap, entry.getLastName());
		
		increment(ageMap, entry.getAge());

		recordCount++;
	}
	
	private <T> void increment(Map<T, Integer> map, T key) {
		Integer count = map.putIfAbsent(key, 1);
		if ( count != null ) {
			map.put(key, count + 1);
		}
	}

	@Override
	public Integer getRecordCount() {
		return recordCount;
	}

	public Integer getFirstNameCount(String firstName) {
		return firstNameMap.get(firstName);
	}
	
	public Integer getLastNameCount(String lastName) {
		return lastNameMap.get(lastName);
	}
	
	public Integer getAgeCount(Integer age) {
		return ageMap.get(age);
	}
}
