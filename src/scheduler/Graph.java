package scheduler;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Formatter;

public class Graph implements Iterable<Node> {
	private HashMap<Node, Node> nodes;
		
	public Graph() {
		nodes = new HashMap<Node, Node>();
	}
		
	public Node add(final Node nd) {
		if (!nodes.containsKey(nd)) {
			nodes.put(nd, nd);
			return nd;
		}
		return nodes.get(nd);
	}
	
	/**
	 * Links two nodes to each other for top-down linking.
	 * @param pred preceding node
	 * @param successing node
	 * @return returns the SUCC if everything went fine, NULL otherwise
	 */
	public Node link(Node pred, Node succ, int it) {
		pred = add(pred);
		succ = add(succ);
		return succ.prepend(pred.append(succ, it), it);
	}
	
	public int size() {
		return nodes.size();
	}
		
	/**
	 * Links two nodes to each other for bottom-up linking.
	 * @param pred preceding node
	 * @param successing node
	 * @return returns the PRED if everything went fine, NULL otherwise
	 */
	public Node rlink(Node pred, Node succ, int it) {
		pred = add(pred);
		succ = add(succ);
		return pred.append(succ.prepend(pred, it), it);
	}
		
	public Node get(Node nd) {
		return nodes.get(nd);
	}
		
	public Iterator<Node> iterator() {
		return nodes.keySet().iterator();
	}
	
	public void unlink(Node a, Node b) {
		a.remove(b);
		b.remove(a);
	}
	
	public void handle(Node a, Node b) {
		a.handle(b);
		b.handle(a);
	}
	public void reset() {
		for (Node n : nodes.keySet())
			n.reset();
	}
	
	/*public Graph clone() {
		Graph newg = new Graph();
		Map<Node, Node> old_new = new HashMap();
		for (Node nd : nodes.keySet()) {
			Node newd = new Node(nd.id);
			old_new.put(nd, newd);
			newg.add(newd);
		}
		
		for (Node nd : old_new.keySet())
			for (Node su : nd.successors())
				newg.link(old_new.get(nd), old_new.get(su));
		return newg;
	}
	*/
	
	public Integer dijkstra(Node src) {
		HashMap<Node,Integer> dist = new HashMap<Node, Integer>();
		HashMap<Node, Node> prev = new HashMap<Node, Node>();
		Set<Node> Q = new HashSet<Node>();
		for (Node nd : nodes.keySet()) {
			dist.put(nd, Integer.MAX_VALUE);
			prev.put(nd, null);
			Q.add(nd);
		}
		for (Node sn : src.successors()) {
			Integer a = 1;
			if (a < dist.get(sn)) {
				dist.put(sn, a);
				prev.put(sn, src);
			}
		}
		while (Q.size() > 0) {
			Node u = find_node(dist, Q, src);
			if ((u == null)) {
				break;
			}
			if (!Q.remove(u)) {
				System.out.printf("Not found!%n");
				System.exit(-1);
			}
			for (Node sn : u.successors()) {
				Integer a = dist.get(u) + 1;
				if (a < dist.get(sn)) {
					dist.put(sn, a);
					prev.put(sn, u);
				}
			}
		}
		return dist.get(src);
	}

	public Node find_node(HashMap<Node, Integer>dist, Set<Node> Q, Node src) {
		Integer d = Integer.MAX_VALUE;
		Node dest = null;
		for (Node nd : Q) {
			if (dist.get(nd).compareTo(d) < 0) {
				d = dist.get(nd);
				dest = nd;
			}
		}
		return dest;
	}

	public Node validate() {
		System.out.printf("Validating graph%n");
		Integer s;
		for (Node nd: nodes.keySet()) {
			if (nd.root()) {
				s = dijkstra(nd);
				if (s.compareTo(Integer.MAX_VALUE) != 0)
					return nd;
			}
		}
		return null;
	}

	/**
	 * Simple diagnostic function. Invokes diagnose() for each registered
	 * node in turn.
	 * @return A String containing the output of diagnose() of each node
	 * separated by newlines.
	 */
	public String diagnose() {
		Formatter f = new Formatter();
		for (Node nd : nodes.keySet()) {
			f.format("%s%n", nd.diagnose());
		}
		f.format("Nr of Nodes : %s", nodes.size());
		String str = f.toString();
		f.close();
		return str;
	}
}
