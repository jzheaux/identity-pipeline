package com.joshcummings.codeplay.concurrency;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class BadIdentity implements Identity {

	public Integer getId() {
		return 0;
	}
	
	@Override
	public String getUsername() {
		return null;
	}

	@Override
	public char[] getPassword() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getPhoneNumber() {
		return null;
	}

	@Override
	public String getEmailAddress() {
		return null;
	}
	
	public Integer getAge() {
		return 0;
	}

	@Override
	public List<Address> getAddresses() {
		return Collections.emptyList();
	}

	@Override
	public ReentrantLock getLock() {
		return null;
	}
}
