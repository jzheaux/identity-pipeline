package com.joshcummings.codeplay.concurrency.splitting;

import com.joshcummings.codeplay.concurrency.Identity;

/**
 * An example of the scatter-gather pattern when done in a single thread
 * 
 */
public class SingleThreadedScatterGatherer implements ScatterGatherer {
	@Override
	public Identity go(Scatterer s, Gatherer g) {
		// while there are more reader strategies to try
		// AND while there has not yet been a satisfactory result
		while ( s.hasNext() && g.needsMore() ) {
			try {
				// try to deserialize to an Identity
				Identity result = s.next().call();
				g.gatherResult(result);
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		return g.getFinalResult();
	}
}
