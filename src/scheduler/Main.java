package scheduler;

public class Main {

	public static void main(String[] args) {
		RC rc = null;
		if (args.length > 1) {
			System.out.println("Reading resource constraints from " + args[1] + "\n");
			rc = new RC();
			rc.parse(args[1]);
		}
		int quality = 1;
		if (args.length > 2) {
			try {
				quality = Integer.parseInt(args[2]);
				if (quality < 1 || quality > 10)
					throw new NumberFormatException();
			} catch (NumberFormatException x) {
				System.err.println("Argument quality must be an integer value between 1 and 10");
				System.exit(-1);
			}
		}

		Dot_reader dr = new Dot_reader(false);
		if (args.length < 1) {
			System.err.printf("Usage: scheduler dotfile resource_constraints [quality (1-10)]%n");
			System.exit(-1);
		} else {
			System.out.println("Scheduling " + args[0]);
			System.out.println();
		}
		Graph g = dr.parse(args[0]);

		Scheduler s = new ASAP();
		Schedule sched = s.schedule(g);
		System.out.printf("Cost (ASAP) = %s%n", sched.cost());

		sched.draw("schedules/ASAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));

		s = new ALAP();
		sched = s.schedule(g);
		System.out.printf("Cost (ALAP) = %s%n", sched.cost());
		sched.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));

		s = new SASDC(rc, quality);
		sched = s.schedule(g);
		System.out.printf("Cost (SA/SDC) = %s%n", sched.cost());
		sched.draw("schedules/SASDC_" + args[0].substring(args[0].lastIndexOf("/") + 1));
	}
}
