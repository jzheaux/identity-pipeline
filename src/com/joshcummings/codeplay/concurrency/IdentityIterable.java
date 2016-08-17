package com.joshcummings.codeplay.concurrency;

import java.io.InputStream;
import java.util.Iterator;

public class IdentityIterable implements Iterator<Identity>, Iterable<Identity> {
	private IdentityReader reader;
	private InputStream is;
	private Identity next;

	public IdentityIterable(InputStream is, IdentityReader reader) {
		this.reader = reader;
		this.is = is;
		next = reader.read(is);
	}

	public boolean hasNext() {
		return next != null;
	}

	public Identity next() {
		Identity toReturn = next;
		next = reader.read(is);
		return toReturn;
	}

	public Iterator<Identity> iterator() {
		return this;
	}
}
