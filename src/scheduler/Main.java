package scheduler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
		double asapCost, alapCost, sasdcCost;

		int i = args[0].lastIndexOf("/");
		if (i == -1) // fck windwos
			i = args[0].lastIndexOf("\\");
		String fn = args[0].substring(i + 1);

		Scheduler s = new ASAP();
		Schedule sched = s.schedule(g);
		System.out.printf("Cost (ASAP) = %s%n", asapCost = sched.cost());
		sched.draw("schedules/ASAP_" + fn);

		s = new ALAP();
		sched = s.schedule(g);
		System.out.printf("Cost (ALAP) = %s%n", alapCost = sched.cost());
		sched.draw("schedules/ALAP_" + fn);

		SASDC sasdc = new SASDC(rc, quality);
		sched = sasdc.schedule(g);
		System.out.printf("Cost (SA/SDC) = %s%n", sasdcCost = sched.cost());
		sched.draw("schedules/SASDC_" + fn);

		File file = new File("benchmark.csv");
		try {
			boolean heading = !file.exists();
			FileWriter wtr = new FileWriter("benchmark.csv", true);
			if (heading)
				wtr.write(String.format("%s;%s;%s;%s;%s;%s;%s;%s%n", "File", "# Nodes", "ASAP", "ALAP", "SA/SDC", "Quality", "# iterations", "Runtime"));
			wtr.write(String.format("%s;%s;%.2f;%.2f;%.2f;%s;%.0f;%.2f%n", fn, g.size(), asapCost, alapCost, sasdcCost, quality, sasdc.iterations, sasdc.elapsedTime));
			wtr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
