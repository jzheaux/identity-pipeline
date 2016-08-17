package com.joshcummings.codeplay.concurrency;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public interface Identity {
	Integer getId();
	
	String getUsername();

	char[] getPassword();

	String getName();

	String getPhoneNumber();

	String getEmailAddress();

	Integer getAge();
	
	List<Address> getAddresses();
	
	ReentrantLock getLock();
}
