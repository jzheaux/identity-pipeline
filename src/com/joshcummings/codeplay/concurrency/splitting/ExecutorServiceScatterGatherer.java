package com.joshcummings.codeplay.concurrency.splitting;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.joshcummings.codeplay.concurrency.Identity;

/**
 * An example of the scatter-gather pattern as implemented using a basic ExecutorService.
 * 
 */
public class ExecutorServiceScatterGatherer implements ScatterGatherer {	
	private final ExecutorService pool = Executors.newCachedThreadPool();

	@Override
	public Identity go(Scatterer s, Gatherer g) {
		Queue<Future<Identity>> results = new LinkedList<>();
		while ( s.hasNext() ) {
			Future<Identity> futureResult = pool.submit(s.next());
			results.add(futureResult);
		}
		
		while ( !results.isEmpty() && g.needsMore() ) {
			try {
				Future<Identity> futureResult = results.poll();
				Identity result = futureResult.get();
				g.gatherResult(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return g.getFinalResult();
	}
}
