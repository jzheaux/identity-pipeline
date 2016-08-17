package com.joshcummings.codeplay.concurrency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Person implements Identity {
	private static Integer ID_SOURCE = 0;
	
	private final Integer id;
	private final String username;
	private final char[] password;
	
	private final String name;
	private String phoneNumber;
	private String emailAddress;
	
	private final Integer age = 34;
	
	private final List<Address> addresses = new ArrayList<>();
	
	private final ReentrantLock lock = new ReentrantLock();
	
	public Person(String username, char[] password, String name,
			String phoneNumber, String emailAddress, List<Address> addresses,
			Integer age) {
		this.id = ++ID_SOURCE;
		this.username = username;
		this.password = password;
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.emailAddress = emailAddress;
		this.addresses.addAll(addresses);
	}

	public Integer getId() {
		return id;
	}
	
	public String getUsername() {
		return username;
	}

	public char[] getPassword() {
		return password;
	}

	public String getName() {
		return name;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Override
	public Integer getAge() {
		return age;
	}
	
	public List<Address> getAddresses() {
		return Collections.unmodifiableList(addresses);
	}

	public void addAddresses(List<Address> addresses) {
		this.addresses.addAll(addresses);
	}
	
	public ReentrantLock getLock() {
		return lock;
	}
}
