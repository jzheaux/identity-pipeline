package com.joshcummings.codeplay.concurrency;

import java.util.Random;

public class Generator {
	private static Random random = new Random(29837420394L);
	
	private static int j;
	
	public static boolean nextBoolean() {
		return random.nextBoolean();
	}
	public static double exponential(double mean) {
		return - mean * Math.log(Math.random());
	}
	
	public static void waitFor(long mean) {
		for ( int i = 0; i < mean*1000000; i++ ) {
			j += i;
		}
	}
}
