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
	private int quality;

	/**
	 * Index of first resource constraint (for faster modification of the
	 * LinearProgram).
	 */
	private int rc0;

	public SASDC(RC constraints, int quality) {
		if (constraints == null)
			throw new IllegalArgumentException("Resource constraints cannot be null.");
		if (quality < 1 || quality > 10)
			throw new IllegalArgumentException("Argument quality must be in range 0 <= quality <= 10");

		this.constraints = constraints;
		this.quality = quality;
		this.lpSolver = SolverFactory.newDefault();
	}

	@Override
	public Schedule schedule(Graph sg) {
		// index all nodes
		vars = new HashMap<>();
		for (Node n : sg) {
			// System.out.printf("x%s => %s%n", vars.size(), n.id);
			vars.put(n, vars.size());
		}

		SDCNodeList nodes = new SDCNodeList(constraints, sg);
		LinearProgram lp = getBaseLP();

		rc0 = lp.getConstraints().size() - 1;

		// shuffle initial schedule and calc start temperature
		System.out.println("SDC with SA: Setting up initial configuration.");
		Schedule current = makeSchedule(nodes, lp);
		double[] cost = new double[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			current = modify(nodes, lp);
			cost[i] = current.cost();
		}

		// run SA...
		double T = 20 * stdDeviation(cost), // initial temperature
				tu = .5, // temperature update factor
				ar = 1, // acceptance ratio
				changes = 1, // total changes
				acceptedChanges = 1; // accepted Changes

		int inner = (int) Math.ceil(this.quality * Math.pow(nodes.length, 4.0 / 3));

		System.out.printf("SDC with SA: Running annealing with quality = %s and T0 = %.2f ...%n", quality, T);
		double time = System.nanoTime();
		while (ar > .12) {
			for (int i = 0; i < inner; i++) {
				changes++;
				Schedule temp = modify(nodes, lp);
				double dc = temp.cost() - current.cost();
				// if (dc == 0) {
				// dc = 1e-4;// (double) -i / ;
				// }
				double r = Math.random();
				if (r < Math.exp(-dc / T)) {
					current = temp;
					acceptedChanges++;
				} else
					nodes.revert();
			}
			ar = acceptedChanges / changes;
			double tutmp = tu;
			if (ar > .96)
				tu = .5;
			else if (ar > .8)
				tu = .9;
			else if (ar > .15)
				tu = .95;
			else
				tu = .8;
			if (tutmp != tu)
				System.out.printf("\t- Updating temperature factor %.2f (iterations: %.0f, temperature: %.2f, elapsed time: %.1fsec)%n", tu, changes, T, (System.nanoTime() - time) / 1e9);
			T *= tu;
		}

		System.out.printf("Convergence after %.0f iterations in %.1fsec (cost: %.2f).%n", changes, (System.nanoTime() - time) / 1e9, current.cost());

		return current;
	}

	/**
	 * Creates a schedule from the given node list.
	 */
	private Schedule makeSchedule(SDCNodeList nodes, LinearProgram lp) {
		ArrayList<Constraint> lpc = new ArrayList<Constraint>(lp.getConstraints().subList(0, rc0));

		for (int i = 0; i < nodes.length; i++) {
			Node n1 = nodes.get(i), n2 = nodes.nextOfType(n1.getRT(), i + 1);
			if (n2 == null)
				continue;
			double[] d = new double[vars.size()];
			d[vars.get(n1)] = 1;
			d[vars.get(n2)] = -1;
			lpc.add(new LinearSmallerThanEqualsConstraint(d, -n1.getDelay(), String.format("r%s", i)));
		}
		lp.setConstraints(lpc);

		Schedule ret = new Schedule();
		double[] d = lpSolver.solve(lp);
		if (d[0] + d[1] + d[2] + d[3] + d[4] + d[5] == 0) {
			System.err.println("Infeasable model for order:\n\t" + nodes);
			System.exit(1);
		}
		for (Entry<Node, Integer> e : vars.entrySet()) {
			Node n = e.getKey();
			int i0 = (int) Math.ceil(d[e.getValue()]);
			Interval i = new Interval(i0, i0 + n.getRT().delay);
			ret.add(n, i);
		}
		return ret;
	}

	/**
	 * Modifies a node list and creates a new schedule from it.
	 * 
	 * @param nodes The nodelist
	 * @param lp The linear program
	 * @return A modified schedule
	 */
	private Schedule modify(SDCNodeList nodes, LinearProgram lp) {
		int i0 = Integer.MIN_VALUE;
		do {
			// if (i0 != Integer.MIN_VALUE)
			// System.out.println("impossible");

			i0 = (int) Math.round((2 * Math.random() - 1) * (nodes.length - 1));
			// System.out.printf("Shove " + (i0 < 0 ? "left: " : "right: ") + "%1$-3s ",
			// Math.abs(i0));
		} while (!(i0 < 0 ? nodes.shoveLeft(-i0) : nodes.shoveRight(i0)));
		// System.out.println("ok " + nodes.toString());
		return makeSchedule(nodes, lp);
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

	/**
	 * Calculates the standard deviation of the given values. See:
	 * {@link https://stackoverflow.com/questions/7988486/how-do-you-calculate-the-variance-median-and-standard-deviation-in-c-or-java}
	 * 
	 * @param vals The values.
	 */
	private double stdDeviation(double[] vals) {
		double mean = 0;
		for (double d : vals)
			mean += d;
		mean = mean / vals.length;

		double tmp = 0;
		for (double d : vals)
			tmp += (d - mean) * (d - mean);

		return Math.sqrt(tmp / vals.length);
	}
}
