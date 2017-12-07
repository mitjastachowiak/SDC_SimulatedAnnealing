package scheduler;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads DOT-ish files. (see graphviz)
 * <p>
 * The reader works somewhat crude. It reads the file line-wise
 * expecting at most one expression per line. It distinguishes between two
 * types of expressions:
 * 1. NODE [.*];
 * 2. NODE1 -> NODE2;
 * Where NODE, NODE1 and NODE2 are given by the following regular expression:
 * [a-zA-Z_0-9][a-zA-Z_0-9]*
 * Number 1 is a definition expression and adds a node to the graph.
 * Number 2 represents a directed link between two nodes. Nodes not previously
 * found in the graph are added when found in a link expression. (As they are
 * in DOT)
 * <p>
 * Graphs must not be circular!
 * <p>
 * See parse().
 */
public class Dot_reader {
//	private BufferedReader file_reader;
	private Graph graph;
	private boolean readBackEdges = false;
	
	public Dot_reader(boolean readBackEdges) {
		this.readBackEdges = readBackEdges;
		graph = new Graph();
	}
	
	private void lex(BufferedReader input) {
		String line;
		Pattern pat_def = Pattern.compile("(\\w\\w*) (\\[.*\\]);.*");
		Pattern pat_use = Pattern.compile("(\\w\\w*) -> (\\w\\w*);.*");
		Pattern pat_itdep = Pattern.compile("(\\w\\w*) -> (\\w\\w*).*(\\[.*\\]);.*");
		Matcher m;
		try {
			line = input.readLine();
			while (line != null) {
			
				/* new node */
				m = pat_def.matcher(line);
				if (m.matches()){
					if (m.group(1).compareTo("node") != 0){
						Node n = new Node(m.group(1));
						graph.add(n);
						graph.get(n).setRT(RT.getRT(m.group(2)));
					}
				}
				/* link */
				m = pat_use.matcher(line);
				if (m.matches()){
					if (graph.link(new Node(m.group(1)), new Node(m.group(2)),0) == null) {
						System.err.printf("ERROR: Found circular graph%n");
						System.exit(-1);
					}
				}
				
				if(readBackEdges){
					m = pat_itdep.matcher(line);
					if (m.matches()){
						int it = Integer.parseInt(m.group(3).split("\"")[1]);
						if (graph.link(new Node(m.group(1)), new Node(m.group(2)),it) == null) {
							System.err.printf("ERROR: Found circular graph%n");
							System.exit(-1);
						}
					}
				}
				line = input.readLine();
			}
		} catch (Throwable e) {
			System.err.printf("FATAL: Could not read from input%n");
			e.printStackTrace(System.err);
			System.exit(-1);
		}
	}
	
	/**
	 * Parses the open stream.
	 */
	public Graph parse(String fn) {
		BufferedReader file_reader;
		
		graph = new Graph();
		
		try {
			file_reader = new BufferedReader(new FileReader(fn));
			lex(file_reader);
		} catch (FileNotFoundException e) {
			System.err.printf("FATAL: File not found: %s%n", fn);
			System.exit(-1);
		}
		
		return graph;
	}
}
