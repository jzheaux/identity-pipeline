package com.joshcummings.codeplay.concurrency;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FileIdentityReader implements IdentityReader {
	
	@Override
	public Identity read(InputStream is) {
		try {
			String line = LineReader.readLine(is);
			if ( line != null ) {
				String[] parts = line.split("\t");
				Person p = new Person(parts[0], parts[1].toCharArray(),
						parts[2], parts[3], parts[4],
						Arrays.asList(new Address(parts[5], parts[6], parts[7], parts[8])),
						Integer.parseInt(parts[9]));
				return p;
			} else {
				return null;
			}
		} catch ( IOException e ) {
			e.printStackTrace();
			return null;
		} catch ( Exception e ) {
			//System.out.println("[" + this.getClass() + "] failed to read identity: " + e.getMessage());
			return new BadIdentity();
		}
	}

}
