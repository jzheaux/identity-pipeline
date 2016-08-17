package com.joshcummings.codeplay.concurrency.single;

import java.util.HashMap;
import java.util.Map;

import com.joshcummings.codeplay.concurrency.StatsLedger;

public class SimpleStatsLedger implements StatsLedger {
	private Map<String, Integer> firstNameMap = new HashMap<>();
	private Map<String, Integer> lastNameMap = new HashMap<>();
	private Map<Integer, Integer> ageMap = new HashMap<>();
	private Integer recordCount = 0;

	@Override
	public void recordEntry(StatsEntry entry) {
		Integer count = withDefault(firstNameMap.get(entry.getFirstName()), 0);
		firstNameMap.put(entry.getFirstName(), count + 1);
		
		count = withDefault(lastNameMap.get(entry.getLastName()), 0);
		lastNameMap.put(entry.getLastName(), count);
		
		count = withDefault(ageMap.get(entry.getAge()), 0);
		ageMap.put(entry.getAge(), count);
		
		System.out.println("Increasing record count");
		recordCount++;
	}
	
	private Integer withDefault(Integer value, Integer backup) {
		if ( value != null ) {
			return value;
		}
		return backup;
	}

	@Override
	public Integer getRecordCount() {
		return recordCount;
	}

	@Override
	public Integer getAgeCount(Integer age) {
		return ageMap.get(age);
	}
	
	@Override
	public Integer getFirstNameCount(String firstName) {
		return firstNameMap.get(firstName);
	}

	@Override
	public Integer getLastNameCount(String lastName) {
		return lastNameMap.get(lastName);
	}
}
