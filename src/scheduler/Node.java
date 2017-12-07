package scheduler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Formatter;

/**
 * This class represents a single node.
 */
public class Node {
	/**
	 * Map of successor nodes and their edge weight
	 */
	private HashMap<Node, Integer> successors;			//contains all successors as key - the value is the edge weight
	/**
	 * Map of predecessors and their edge weight
	 */
	private HashMap<Node, Integer> predecessors;		//contains all predecessors as key - the value is the edge weight
	/**
	 * Set of unhandled successors
	 */
	private HashSet<Node> unhandled_succ;
	/**
	 * Set of unhandled predecessors
	 */
	private HashSet<Node> unhandled_pred;
	
	/**
	 * ID for this node - unique name
	 */
	public final String id;
	/**
	 * Resource type of this node
	 */
	private RT rt;
	
	/**
	 * @param id - ID of the new node
	 * @param rt - resource type
	 */
	public Node(String id, RT rt) {
		this.id = id;
		successors = new HashMap<Node, Integer>();
		predecessors = new HashMap<Node, Integer>();
		unhandled_succ = new HashSet<Node>();
		unhandled_pred = new HashSet<Node>();
		this.rt = rt;
	}
	
	/**
	 * @param id - ID of the new node
	 */
	public Node(String id) {
		this.id = id;
		rt = RT.getRT(id);
		successors = new HashMap<Node, Integer>();
		predecessors = new HashMap<Node, Integer>();
		unhandled_succ = new HashSet<Node>();
		unhandled_pred = new HashSet<Node>();
	}
		
	/**
	 * Adds node n to the successors of this node. The edge weight is w.
	 * @param n - new successor
	 * @param w - edge weight
	 * @return null iff node was not appended, this node otherwise
	 */
	public Node append(Node n, int w) {
		if (n == null)
			return null;
		successors.put(n,w);
		if(w == 0)
			unhandled_succ.add(n);
		return this;
	}
		
	/**
	 * Adds node n to the predecessors of this node. The edge weight is w.
	 * @param n - new predecessor
	 * @param w - edge weight
	 * @return null iff node was not appended, this node otherwise
	 */
	public Node prepend(Node n, int it) {
		if (n == null)
			return null;
		predecessors.put(n, it);
		if(it == 0)
			unhandled_pred.add(n);
		return this;
	}
		
	/**
	 * Removes a node from successors and/or predecessors.
	 * @param n - Node to remove
	 * @return true iff a node has been removed
	 */
	public boolean remove(Node n) {
		unhandled_succ.remove(n);
		unhandled_pred.remove(n);
		return successors.remove(n)!=null || predecessors.remove(n)!=null;
	}
	
	/**
	 * Mark a node as handled. Useful during scheduling.
	 * @param n - Node to mark handled.
	 * @return true iff a node has been marked.
	 */
	public boolean handle(Node n) {
		return unhandled_succ.remove(n) || unhandled_pred.remove(n);
	}
	
	/**
	 * Check if this node is a root node. I.e. it has no predecessors with 0 edge weight
	 * @return true iff this node is a root
	 */
	public boolean root() {
		for(Node n: predecessors.keySet())
			if(predecessors.get(n) == 0)
				return false;
		return true;
	}
	
	/**
	 * Check if this node is a leaf node. I.e. it has no successors with 0 edge weight
	 * @return true iff this node is a leaf
	 */
	public boolean leaf() {
		for(Node n: successors.keySet())
			if(successors.get(n) == 0)
				return false;
		return true;
	}
	
	/**
	 * Checks if any unhandled predecessors are left. For scheduling only - use root()
	 * to test for graph properties!
	 * @return true if this node has no unhandled predecessors, false otherwise.
	 */
	public boolean top() {
		return unhandled_pred.size() == 0;
	}
	
	/**
	 * TRUE if no unhandled successors are left. For scheduling only! Use leaf()
	 * to test for graph properties!
	 * @return TRUE if this node has no unhandled successors, FALSE otherwise.
	 */
	public boolean bottom() {
		return unhandled_succ.size() == 0;
	}
	
	/**
	 * Mark all nodes as unhandled again
	 */
	public void reset() {
		unhandled_succ = successors();
		unhandled_pred = predecessors();
	}
	
	/**
	 * Return all successors of this node within one Iteration. Means all successors with edge weight 0
	 * @return A set of all successors of this iteration
	 */
	public HashSet<Node> successors() {
		HashSet<Node> succ = new HashSet<Node>();
		for(Node n: successors.keySet()){
			if(successors.get(n) == 0)
				succ.add(n);
		}
		return succ;
	}
	
	/**
	 * Return all successors with arbitrary edge weight
	 * @return A map of all successors and their edge weight
	 */
	@SuppressWarnings("unchecked")
	public HashMap<Node,Integer> allSuccessors(){
		return (HashMap<Node,Integer>)successors.clone();
	}
	
	/**
	 * Return all predecessors of this node within one Iteration. Means all predecessors with edge weight 0
	 * @return A set of all predecessors of this iteration
	 */
	public HashSet<Node> predecessors() {
		HashSet<Node> pred = new HashSet<Node>();
		for(Node n: predecessors.keySet()){
			if(predecessors.get(n) == 0)
				pred.add(n);
		}
		return pred;
	}
	
	/**
	 * Return all predecessors with arbitrary edge weight
	 * @return A map of all predecessors and their edge weight
	 */
	@SuppressWarnings("unchecked")
	public HashMap<Node,Integer> allPredecessors(){
		return (HashMap<Node,Integer>)predecessors.clone();
	}
		
	public String toString() {
		return id;
	}
		
	public int hashCode() {
		return id.hashCode();
	}
		
	public boolean equals(Object e) {
		try {
			Node nd = (Node)e;
			return nd.id.equals(this.id);
			
		} catch (Throwable err) {
			return false;
		}
	}
	
	
	/**
	 * Prints a string for diagnosis.
	 * @return a string giving information about the node and its successors and predecessors
	 */
	public String diagnose() {
		Formatter f = new Formatter();
		
		f.format("%s (%s):%n", id, rt.name);
		if (successors.size() > 0) {
			f.format("  successors%n");
			for (Node nd : successors.keySet())
				f.format("   %s \t%d%n", nd, successors.get(nd));		//write the successors plus the edge weight
		}
		if (predecessors.size() > 0) {
			f.format("  predecessors%n");
			for (Node nd : predecessors.keySet())
				f.format("   %s \t%d%n", nd, predecessors.get(nd));		//write the predecessors plus the edge weight
		}
		
		String ret = f.toString();
		f.close();
		
		return ret;
	}
	
	
	/**
	 * Returns the edge weight for the given predecessor.
	 * @param nd - the predecessor to find the weight for
	 * @return weight for given predecessor
	 */
	public int getPredWeight(Node nd){
		return predecessors.get(nd);
	}
	
	/**
	 * Returns the edge weight for the given successor.
	 * @param nd - the successor to find the weight for
	 * @return weight for given successor
	 */
	public int getSuccWeight(Node nd){
		return successors.get(nd);
	}
	
	/**
	 * Set the resource type for this node
	 * @param rt - new resource type of this node
	 */
	public void setRT(RT rt){
		this.rt = rt;
	}

	/**
	 * Get the resource type for this node
	 * @return this nodes resource type
	 */
	public RT getRT(){
		return this.rt;
	}
	
	/**
	 * Get the delay for this nodes operation
	 * @return the delay/duration of this operation
	 */
	public int getDelay(){
		return rt.delay;
	}
}
