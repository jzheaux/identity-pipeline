package com.joshcummings.codeplay.concurrency;

import java.io.IOException;
import java.io.InputStream;

public class LineReader {
	public static String readLine(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		int ch = -1;
		while ( ( ch = is.read() ) != '\n' ) {
			if ( ch == -1 ) return null;
			sb.append((char)ch);
		}
		return sb.toString().trim();
	}
}
