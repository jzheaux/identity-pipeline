package com.joshcummings.codeplay.concurrency;

import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;

public class Address extends Observable {
	private static AtomicInteger ID_SOURCE = new AtomicInteger();
	
	private final Integer id;
	private final String address1;
	private final String city;
	private final String state;
	private final String zipCode;
	
	private boolean verified;
	
	public Address(String address1, String city, String state, String zipCode) {
		this.id = ID_SOURCE.incrementAndGet();
		this.address1 = address1;
		this.city = city;
		this.state = state;
		this.zipCode = zipCode;
	}
	
	public Integer getId() {
		return id;
	}
	public String getAddress1() {
		return address1;
	}
	public String getCity() {
		return city;
	}
	public String getState() {
		return state;
	}
	public String getZipCode() {
		return zipCode;
	}
	public void setVerified(boolean verified) {
		this.verified = verified;
		this.setChanged();
		this.notifyObservers();
	}
	public boolean isVerified() {
		return verified;
	}
	public String toString() {
		return "Address #" + id;
	}
}
