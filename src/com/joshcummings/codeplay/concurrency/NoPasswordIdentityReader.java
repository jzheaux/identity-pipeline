package com.joshcummings.codeplay.concurrency;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
	
public class NoPasswordIdentityReader implements IdentityReader {
	
	@Override
	public Identity read(InputStream is) {
		try {
			String line = LineReader.readLine(is);
			if ( line != null ) {
				String[] parts = line.split("\t");
				Person p = new Person(parts[0], new char[0],
						parts[1], parts[2], parts[3],
						Arrays.asList(new Address(parts[4], parts[5], parts[6], parts[7])),
						Integer.parseInt(parts[8]));
				return p;
			} else {
				return null;
			}
		} catch ( IOException e ) {
			e.printStackTrace();
			return null;
		} catch ( Exception e ) {
			System.out.println("[" + this.getClass() + "] failed to read identity: " + e.getMessage());
			return new BadIdentity();
		}
	}


}
