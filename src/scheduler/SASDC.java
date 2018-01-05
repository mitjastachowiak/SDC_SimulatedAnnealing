package scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import scheduler.sdcutil.SDCNodeList;
import scpsolver.constraints.Constraint;
import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
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

	private HashMap<Node, Integer> vars;
	private RC constraints;
	private LinearProgramSolver lpSolver;

	/**
	 * Indices for node swapping.
	 */
	private int i1, i2;

	/**
	 * Index of first resource constraint (for faster modification of the
	 * LinearProgram).
	 */
	private int rc0;

	public SASDC(RC constraints) {
		if (constraints == null)
			throw new IllegalArgumentException("Resource constraints cannot be null.");

		this.constraints = constraints;
		this.lpSolver = SolverFactory.newDefault();
	}

	@Override
	public Schedule schedule(Graph sg) {
		// index all nodes
		vars = new HashMap<>();
		for (Node n : sg) {
			System.out.printf("x%s => %s%n", vars.size(), n.id);
			vars.put(n, vars.size());
		}

		SDCNodeList nodes = new SDCNodeList(constraints, sg);
		LinearProgram lp = getBaseLP();

		i1 = 0;
		i2 = vars.size() - 1;

		Schedule best = initial(nodes, lp);

		best.draw("debug.dot");

		// double[] solution = solver.solve(lp);
		// System.out.printf("%s %s%n", "tMax", solution[vars.size()]);
		// for (Node n : vars.keySet())
		// System.out.printf("%s (%s) %s%n", n.id, vars.get(n), solution[vars.get(n)]);
		return null;
	}

	/**
	 * Sets up the initial schedule.
	 * 
	 * @return The initial schedule.
	 */
	private Schedule initial(SDCNodeList nodes, LinearProgram lp) {
		ArrayList<Constraint> lpc = lp.getConstraints();
		rc0 = lpc.size();

		for (int i = 0; i < nodes.length; i++) {
			Node n1 = nodes.get(i), n2 = nodes.nextOfType(n1.getRT(), i + 1);
			if (n2 == null)
				continue;
			double[] d = new double[vars.size()];
			d[vars.get(n1)] = 1;
			d[vars.get(n2)] = -1;
			lpc.add(new LinearSmallerThanEqualsConstraint(d, -n1.getDelay(), String.format("r%s", i)));
		}

		return lp2Schedule(lp);
	}

	/**
	 * Creates a schedule from a linear program.
	 * 
	 * @param lp The linear program which will be solved in order to create the
	 *            schedule.
	 */
	private Schedule lp2Schedule(LinearProgram lp) {
		Schedule ret = new Schedule();
		double[] d = lpSolver.solve(lp);
		for (Entry<Node, Integer> e : vars.entrySet()) {
			Node n = e.getKey();
			int i0 = (int) Math.ceil(d[e.getValue()]);
			Interval i = new Interval(i0, i0 + n.getRT().delay);
			ret.add(n, i);
		}
		return ret;
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
				// lp.addConstraint(new LinearSmallerThanEqualsConstraint(d, -1,
				// String.format("c%s", i++)));
				lp.addConstraint(new LinearSmallerThanEqualsConstraint(d, -p.getDelay(), String.format("c%s", i++)));
			}

		}
		lp.setMinProblem(true);
		return lp;
	}

}
