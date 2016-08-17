package com.joshcummings.codeplay.concurrency.throttle;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A class for guessing a line length given an arrival rate, a service rate, and number of servers.
 * 
 * This is based off a formula published in: http://www.csus.edu/indiv/b/blakeh/mgmt/documents/opm101supplc.pdf
 * 
 * If your solution doesn't require considering backpressure, a quick back of the napkin calculation for number
 * of workers is the number of arrivals per time unit divided by the number of server invocations completed per the same time unit, e.g.
 * 
 * A server receives 200 requests per second and each request takes 10 milliseconds, meaning that 100 per second can be serviced, then
 * the number of threads ought to be about 200/100 = 2. Since in the address verifier unit test the arrival rate is about 1500 per second and
 * each invocation takes about 250 milliseconds (when it is healthy), then this means it can serve 4 per second. This means that the number of
 * threads ought to be around 1500/4 = 325.
 * 
 */
public class LineLength {
	public static void main(String[] args) {
	 	// The higher this number is, the more memory and time this utility will take to complete.
		// The precision increases relative to the size of s
		int precision = 1000;
		
		
		BigDecimal lambda = new BigDecimal(3); // 3 arrivals per millisecond 
		BigDecimal s = new BigDecimal(1210); // back of napkin calculation. This should be greater than lambda/mu
		BigDecimal mu = BigDecimal.ONE.divide(new BigDecimal(400)); // one method completed per 400 milliseconds
		BigDecimal lambda_over_mu = lambda.divide(mu, precision, BigDecimal.ROUND_HALF_UP);
		
		BigDecimal p = lambda.divide(s.multiply(mu), precision, BigDecimal.ROUND_HALF_UP);
		
		BigDecimal p_0 = BigDecimal.ZERO;
		
		BigDecimal firstSum = BigDecimal.ZERO;
		for ( int i = 0; i < s.intValue(); i++ ) {
			BigDecimal n_factorial = new BigDecimal(factorial(new BigInteger(String.valueOf(i))));
			BigDecimal numerator = lambda_over_mu.pow(i);
			BigDecimal ratio = numerator.divide(n_factorial, precision, BigDecimal.ROUND_HALF_UP);
			firstSum = firstSum.add(ratio);
		}
		BigDecimal numerator = lambda_over_mu.pow(s.intValue());
		BigDecimal s_factorial = new BigDecimal(factorial(new BigInteger(String.valueOf(s.intValue()))));
		BigDecimal ratio = numerator.divide(s_factorial, 10, BigDecimal.ROUND_HALF_UP);
		BigDecimal inv = BigDecimal.ONE.divide(BigDecimal.ONE.subtract(p), precision, BigDecimal.ROUND_HALF_UP);
		firstSum = firstSum.add(ratio.multiply(inv));

		p_0 = BigDecimal.ONE.divide(firstSum, precision, BigDecimal.ROUND_HALF_UP);
		
		numerator = p_0.multiply(numerator.multiply(p));
		BigDecimal denominator = s_factorial.multiply(BigDecimal.ONE.subtract(p).pow(2));
		
		BigDecimal length = numerator.divide(denominator, precision, BigDecimal.ROUND_HALF_UP);
		
		System.out.println(length);
	}
	
	public static BigInteger factorial(BigInteger n) {
	    BigInteger result = BigInteger.ONE;

	    while (!n.equals(BigInteger.ZERO)) {
	        result = result.multiply(n);
	        n = n.subtract(BigInteger.ONE);
	    }

	    return result;
	}
}
