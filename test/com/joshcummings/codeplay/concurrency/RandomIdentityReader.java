package com.joshcummings.codeplay.concurrency;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

public class RandomIdentityReader implements IdentityReader {
	private int index;
	
	public RandomIdentityReader(int index) {
		this.index = index;
	}
	
	@Override
	public Identity read(InputStream is) {
		try {
			int i = is.read();
			Generator.waitFor(i - 48 == index ? 100 : 1000);
			return i - 48 == index ? new Person(null, null, null, null, null, Collections.emptyList(), null) : new BadIdentity();
		} catch (IOException e) {
			return new BadIdentity();
		}
	}
};
