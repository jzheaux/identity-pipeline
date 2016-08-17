package com.joshcummings.codeplay.concurrency;

public class SimplePhoneNumberFormatter implements PhoneNumberFormatter {

	@Override
	public void format(Identity identity) {
		Person p = (Person)identity;
		String phoneNumber = p.getPhoneNumber();
		phoneNumber = phoneNumber.replaceAll("[\\-\\.\\(\\)\\+\\s]", "");
		p.setPhoneNumber(phoneNumber);
	}

}
