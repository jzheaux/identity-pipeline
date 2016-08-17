package com.joshcummings.codeplay.concurrency.splitting;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.joshcummings.codeplay.concurrency.Identity;

/**
 * An example of ExecutorCompletionService--a wrapper for ExecutorService that blocks in the order of  task completion
 * 
 * This class will loop through given options, firing each onto a separate thread. It will then block and wait until
 * the fastest one completes.
 *
 */
public class ExecutorCompletionServiceScatterGatherer implements ScatterGatherer {
	private final ExecutorService pool = Executors.newCachedThreadPool();

	@Override
	public Identity go(Scatterer s, Gatherer g) {
		ExecutorCompletionService<Identity> ecs = new ExecutorCompletionService<>(pool);
		
		int numberOfTasks = 0;
		while ( s.hasNext() ) {
			ecs.submit(s.next());
			numberOfTasks++;
		}
		
		// Since take is a blocking method, we only want to call it at most the number of tasks
		// submitted. take will block even if there are no currently running tasks
		while ( numberOfTasks > 0 && g.needsMore() ) {
			try {
				Identity i = ecs.take().get();
				g.gatherResult(i);
				numberOfTasks--;
			} catch ( InterruptedException | ExecutionException e ) {
				e.printStackTrace();
			}
		}
		
		return g.getFinalResult();
	}
}
