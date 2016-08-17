package com.joshcummings.codeplay.concurrency.single;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantLock;

public class CopyingInputStream extends InputStream {
	private InputStream is;
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	
	private ReentrantLock lock;
	
	public CopyingInputStream(InputStream is) {
		this.is = is;
	}
	
	@Override
	public int read() throws IOException {
		int i = is.read();
		if ( i != -1 ) {
			baos.write(i);
		}
		return i;
	}
	
	public CopyingInputStream reread() {
		return new CopyingInputStream(new ByteArrayInputStream(baos.toByteArray()));
	}
	
	public ReentrantLock getLock() {
		return lock;
	}
	
	@Override
	public void close() throws IOException {
	}
}
