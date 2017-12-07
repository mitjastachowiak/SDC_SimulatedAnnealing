package scheduler;

/**
 * Abstract scheduler class. The graph SG is declared final
 * for a reason. You must work on a local clone. (See Graph)
 */
public abstract class Scheduler {
	
	/**
	 * Use the graph given to create a schedule. 
	 * @param sg - the dependency graph
	 * @return a schedule for the given graph
	 */
	public abstract Schedule schedule(final Graph sg);
}
