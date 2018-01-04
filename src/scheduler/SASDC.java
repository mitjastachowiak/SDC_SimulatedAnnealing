package scheduler;

import java.util.HashMap;
import java.util.function.Supplier;

import com.sun.org.apache.xpath.internal.functions.Function;

import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.constraints.LinearConstraint;
import scpsolver.constraints.LinearSmallerThanEqualsConstraint;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;

/**
 * Provides an SDC scheduler using interative optimization via simulated
 * annealing.
 * 
 * @author Mitja Stachowiak, Ludwig Meysel
 *
 */
public class SASDC extends Scheduler {

	HashMap<Node, Integer> vars;

	@SuppressWarnings("unused")
	@Override
	public Schedule schedule(Graph sg) {
		// index all nodes
		vars = new HashMap<>();
		for (Node n : sg)
			vars.put(n, vars.size());

		LinearProgram lp = getBaseLP();

		// test resource constraints:
		HashMap<String, Integer> ids = new HashMap<>();
		for (Node n : sg)
			ids.put(n.id, vars.get(n));


		// mul
		lp.addConstraint(resourceConstraint(ids.get("N1_MUL"), ids.get("N2_MUL")));
		lp.addConstraint(resourceConstraint(ids.get("N2_MUL"), ids.get("N3_MUL")));
		lp.addConstraint(resourceConstraint(ids.get("N3_MUL"), ids.get("N6_MUL")));
		lp.addConstraint(resourceConstraint(ids.get("N6_MUL"), ids.get("N7_MUL")));
		lp.addConstraint(resourceConstraint(ids.get("N7_MUL"), ids.get("N8_MUL")));
		
		// alu
		lp.addConstraint(resourceConstraint(ids.get("N4_SUB"), ids.get("N10_ADD")));
		lp.addConstraint(resourceConstraint(ids.get("N10_ADD"), ids.get("N5_SUB")));
		lp.addConstraint(resourceConstraint(ids.get("N5_SUB"), ids.get("N9_ADD")));
		lp.addConstraint(resourceConstraint(ids.get("N9_ADD"), ids.get("N11_CMP")));

		LinearProgramSolver solver = SolverFactory.newDefault();
		double[] solution = solver.solve(lp);
		System.out.printf("%s %s%n", "tMax", solution[vars.size()]);
		for (Node n : vars.keySet())
			System.out.printf("%s (%s)  %s%n", n.id, vars.get(n), solution[vars.get(n)]);
		return null;
	}

	/**
	 * Gets a resource constraint for the given node-indices.
	 */
	private LinearConstraint resourceConstraint(int parent, int node) {
		double[] d = new double[vars.size()];
		d[parent] = 1;
		d[node] = -1;
		return new LinearSmallerThanEqualsConstraint(d, -1, String.format("r%s_%s", node, parent));
	}

	/**
	 * Sets up a linear program according to the graph with flow and bounding
	 * constraints.
	 */
	private LinearProgram getBaseLP() {
		int i = 0, num = vars.size() + 1, tmax = vars.size();
		double[] d = new double[num];
		d[tmax] = 1;
		LinearProgram lp = new LinearProgram(d);
		for (Node n : vars.keySet()) {
			// t_n >= 0
			d = new double[num];
			d[vars.get(n)] = 1;
			lp.addConstraint(new LinearBiggerThanEqualsConstraint(d, 0, String.format("c%s", i++)));

			// t_n <= tMax (described as t_n - tMax <= 0)
			d = new double[num];
			d[vars.get(n)] = 1;
			d[tmax] = -1;
			lp.addConstraint(new LinearSmallerThanEqualsConstraint(d, 0, String.format("c%s", i++)));

			for (Node p : n.predecessors()) {
				// flow constraints (t_p - t_n <= -delay(t_p))
				d = new double[num];
				d[vars.get(p)] = 1;
				d[vars.get(n)] = -1;
				lp.addConstraint(new LinearSmallerThanEqualsConstraint(d, -1, String.format("c%s", i++)));
				// lp.addConstraint(new LinearSmallerThanEqualsConstraint(d, -p.getDelay(),
				// String.format("c%s", i++)));
			}

		}
		lp.setMinProblem(true);
		return lp;
	}

}
