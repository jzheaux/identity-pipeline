package com.joshcummings.codeplay.concurrency;

public class SimpleEmailFormatter implements EmailFormatter {

	@Override
	public void format(Identity identity) {
		Person p = (Person)identity;
		String emailAddress = p.getEmailAddress();
		String[] parts = emailAddress.split("@");
		p.setEmailAddress(
			parts[0].replaceAll("\\.", "").toLowerCase() +
			"@" +
			parts[1].toLowerCase());
	}

}
