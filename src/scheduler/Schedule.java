package scheduler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.Formatter;

/**
 * This class represents a single schedule.
 * @author ruschke
 */
public class Schedule {
	
	/**
	 * Map of nodes and their intervals in the schedule
	 */
	private Map<Node, Interval> nodes;
	/**
	 * Map of time steps and a set of the currently scheduled nodes
	 */
	private Map<Integer, Set<Node>> slots;
	/**
	 * Map of resource types and a sorted mapping of resources and the time steps they are used in
	 */
	private Map<RT, TreeMap<Resource, Integer>> sort_res;
	/**
	 * Map of resource types and a sorted mapping of the time steps and resource that is used
	 */
	private Map<RT, Map<Integer, Resource>> tsort_res;
	/**
	 * Map of nodes and the resource used for this node
	 */
	private Map<Node, String> resources = new HashMap<Node, String>();
		
	public Schedule() {
		nodes = new HashMap<Node, Interval>();
		slots = new TreeMap<Integer, Set<Node>>();
		sort_res = new HashMap<RT, TreeMap<Resource, Integer>>();
		tsort_res = new HashMap<RT, Map<Integer, Resource>>();
	}
	
	/**
	 * Add a node and to the schedule during the given interval
	 * @param nd - the node to be scheduled
	 * @param i - the interval the node will be scheduled in
	 */
	public void add(Node nd, Interval i) {
		if (nodes.containsKey(nd))
			remove(nd);
		
		nodes.put(nd, i);
		
		for (int ii = i.lbound; ii <= i.ubound; ii++) {
			Set<Node> ss = slots.get(ii);
			if (ss == null)
				ss = new HashSet<Node>();
			ss.add(nd);
			slots.put(ii, ss);
			
			{
				TreeMap<Resource, Integer> rmm = sort_res.get(nd.getRT());
				if (rmm == null) {
					rmm = new TreeMap<Resource, Integer>();
					sort_res.put(nd.getRT(), rmm);
				}
				Map<Integer, Resource> rtm = tsort_res.get(nd.getRT());
				if (rtm == null) {
					rtm = new HashMap<Integer, Resource>();
					tsort_res.put(nd.getRT(), rtm);
				}
				Resource rss = rtm.get(ii);
				if (rss == null) {
					rss = new Resource(nd.getRT(), ii);
					rss.inc();
					rmm.put(rss, ii);
					rtm.put(ii, rss);
				} else {
					rmm.remove(rss);
					rss.inc();
					rmm.put(rss, ii);
				}
			}
		}
	}
	
	/**
	 * Add a node to the schedule during the given interval and using the given resource
	 * @param nd - node to be scheduled
	 * @param i - interval to schedule it in
	 * @param resource - resource to be used for the node
	 */
	public void add(Node nd, Interval i, String resource) {
		resources.put(nd, resource);
		add(nd, i);
	}
	
	/**
	 * Remove a node from the schedule
	 * @param nd - node to be removed
	 */
	public void remove(Node nd) {
		Interval i = nodes.get(nd);
		
		nodes.remove(nd);
		resources.remove(nd);
		
		if (i == null)
			return;

		for (int ii = i.lbound; ii <= i.ubound; ii++) {
			slots.get(ii).remove(nd);
			
			{
				Map<Integer, Resource> rtm = tsort_res.get(nd.getRT());
				if (rtm == null)
					continue;
				Resource r = rtm.get(ii);
				if (r == null)
					continue;
				sort_res.get(nd.getRT()).remove(r);
				r.dec();
				sort_res.get(nd.getRT()).put(r, ii);
			}
		}
	}
	
	/**
	 * Calculate this schedules cost
	 * @return cost of this schedule
	 */
	public Double cost() {
		Double c = 0.0;
		for (RT rt : sort_res.keySet())
			c += sort_res.get(rt).firstKey().weight();
			
		return c;
	}

	/**
	 * @return a map of resource types and their cost in the schedule
	 */
	public Map<RT, Double> costPerResource() {
		Map<RT, Double> cm = new TreeMap<RT, Double>();
		Double w;
		for (RT rt : sort_res.keySet()) {
			w = sort_res.get(rt).firstKey().weight();
			if (w.compareTo(0.0) > 0)
				cm.put(rt, w);
		}
		return cm;
	}

	/**
	 * Shift the schedule by the given amount. I.e. add the given amount to all intervals.
	 * @param shift the amount to shift the schedule by
	 * @return this schedule after shifting
	 */
	public Schedule shift(Integer shift) {
		TreeMap<Integer, Set<Node>> ns = new TreeMap<Integer, Set<Node>>();
		HashMap<Node, Interval> nds = new HashMap<Node, Interval>();
		for (Node nd : nodes.keySet())
			nds.put(nd, nodes.get(nd).shift(shift));

		for (RT rt : tsort_res.keySet()) {
			Map<Integer, Resource> rm = tsort_res.get(rt);
			Map<Integer, Resource> nrm = new HashMap<Integer, Resource>();
			for (Integer i : rm.keySet())
				nrm.put(i+shift, rm.get(i));
			tsort_res.put(rt, nrm);
	
			TreeMap<Resource, Integer> rtm = sort_res.get(rt);
			TreeMap<Resource, Integer> nrtm = new TreeMap<Resource, Integer>();
			
			Set<Resource> rset;
			
			if (shift > 0)
				rset = rtm.keySet();
			else
				rset = rtm.descendingKeySet();
			
			for (Resource r : rset) {
				r.step(rtm.get(r)+shift);
				nrtm.put(r, rtm.get(r)+shift);		
			}
			sort_res.put(rt, nrtm);
		}
		for (Integer i : slots.keySet()) {
			ns.put(i + shift, slots.get(i));

		}

		nodes = nds;
		slots = ns;
		return this;
	}
	
	/**
	 * Get the slot (interval) for the given node
	 * @param nd - the node of interest
	 * @return the interval it is scheduled in
	 */
	public Interval slot(Node nd) {
		return nodes.get(nd);
	}
	
	/**
	 * Get the nodes currently scheduled in the given time step. Note that if a node is started at an earlier time step but overlaps the 
	 * given, such a node is also part of the returned set.
	 * @param slot - time step of interest
	 * @return set of nodes scheduled at the given time
	 */
	public Set<Node> nodes(int slot) {		
		return slots.get(slot);
	}
	
	/**
	 * @return a set of all currently scheduled nodes
	 */
	public Set<Node> nodes() {
		return nodes.keySet();
	}
	
	public Schedule clone() {
		Schedule sched = new Schedule();
		
		for (Node nd : nodes.keySet()) {
			sched.add(nd, new Interval(nodes.get(nd).lbound, nodes.get(nd).ubound));
		}

		return sched;
	}
	
	/**
	 * Calculate the length of the given schedule. I.e. the span from the first to the last scheduled node.
	 * @return this schedule's length
	 */
	public Integer length() {
		Integer min, max;
		min = Integer.MAX_VALUE;
		max = Integer.MIN_VALUE;
		for (Integer ii : slots.keySet()) {
			if (ii < min)
				min = ii;
			if (ii > max)
				max = ii;
		}
		return 1 + max - min;
	}
	
	/**
	 * Find the earliest lower bound of a node. Commonly this is a non-negative number.
	 * @return the earliest lower bound of the currently scheduled nodes
	 */
	public int min() {
		int min;
		min = Integer.MAX_VALUE;
		for (Integer ii : slots.keySet()) {
			if (ii < min)
				min = ii;
		}
		return min;
	}
	
	/**
	 * Find the latest upper bound of a node. Commonly this is a non-negative number.
	 * @return the latest upper bound of the currently scheduled nodes
	 */
	public int max() {
		int max;
		max = Integer.MIN_VALUE;
		for (Integer ii : slots.keySet()) {
			if (ii > max)
				max = ii;
		}
		return max;
	}

	/**
	 * Check the schedule for simple conflicts. If any node overlaps its successors the first conflicting node is returned.
	 * @return null iff the schedule has no illegal overlaps, a conflicting node otherwise
	 */
	public Node validate() {
		for (Node nd : nodes.keySet())
			for (Node sn : nd.successors()) {
				if (slot(sn) == null)
					continue;
				if (slot(nd).ubound.compareTo(slot(sn).lbound) >= 0) {
					return nd;
				}
			}
		return null;
	}

	/**
	 * @return number of currently scheduled nodes
	 */
	public Integer size() {
		return nodes.keySet().size();
	}
	
	/**
	 * @return a string with a textual representation of the schedule and the resources
	 */
	public String diagnose() {
		if (nodes.keySet().size() <= 0)
			return "%n";
			
		Formatter f = new Formatter();
		f.format("Found schedule of length %d%n%n", length());
		Set<Node> os = new HashSet<Node>();
		for (Integer ii : slots.keySet())
			for (Node nd : slots.get(ii)) {
				if (os.contains(nd))
					continue;
				os.add(nd);
				f.format("%s : %s%n", nd, nodes.get(nd));
			}
		f.format("%nRegistered resources%n");
		for (RT rt : sort_res.keySet()) {
			for (Resource r : sort_res.get(rt).keySet())
				f.format(" %s %s %s %n", rt, r.step(), r.weight());
		}
		
		String str = f.toString();
		f.close();
		return str;
	}

	/**
	 * Write a dot-file of the schedule. If a resource is specified for each node each column of the schedule represents one resource.
	 * @param dotFileName - the file to be written
	 */
	public void draw(String dotFileName) {
		try {
			BufferedWriter dotFile = new BufferedWriter(new FileWriter(dotFileName));
			int scaleY = 2;
			int scaleX = 2;
			int maxY = length() * scaleY;

			int X = 0;
			int Y = maxY;
			
			int min = min();

			dotFile.write("//do not use DOT to generate pdf use NEATO or FDP\n");
			dotFile.write("digraph{\n");
			dotFile.write("layout=\"neato\";\n");
			dotFile.write("splines=\"ortho\";\n");

			int maxNodes = 0;

			for (int i = 0; i <= max(); i++) {
				if (nodes(i) != null) maxNodes = nodes(i).size() > maxNodes ? nodes(i).size() : maxNodes;
			}

			boolean allResourcesGiven = true;
			for (Node n : nodes()) {
				if (!resources.containsKey(n)) {
					allResourcesGiven = false;
					break;
				}
			}

			int[] slots = new int[maxNodes];

			Map<String, Integer> peSlots = new HashMap<String, Integer>();
			if (allResourcesGiven) {
				int x = 0;
				for (String s : resources.values()) {
					peSlots.put(s, x++);
				}
			} else {
				for (int i = 0; i < maxNodes; i++) {
					slots[i] = 0;
				}
			}
			
			for(int i=0; i<=max(); i++){
				Y = maxY - i*scaleY;
				X = 0;
				
				if(!allResourcesGiven){
					for(int j=0; j<slots.length; j++){
						if(slots[j]>0){
							slots[j]--;
						}
					}
				}
				
				if (nodes(i) != null) for(Node n : nodes(i)){

					if( i==min || (nodes(i-1) != null && !nodes(i-1).contains(n))){
						
						int slot=0;
						
							if (allResourcesGiven) {
								slot = peSlots.get(resources.get(n));
							} else {
								while (slots[slot] > 0) {
									slot++;
								}
								slots[slot] += n.getDelay();
							}
							X = slot * scaleX;
						
						int nodeHeight = n.getDelay() * scaleY - 1;
						int nodeY = Y - nodeHeight/2; 
						int nodeWidth = 1;
						dotFile.write(n.toString() + "[shape=\"ellipse\", style=\"filled\", color=\"#004E8ABF\", pos=\"" + X + "," + nodeY + "!\", height=\"" + nodeHeight + "\", width=\"" + nodeWidth + "\"];\n");
						for(Node suc : n.successors()){
							dotFile.write(n.toString() + " -> " + suc + ";\n");
						}
					}
				}
			}
			
			dotFile.write("}");	
			dotFile.flush();
			dotFile.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
