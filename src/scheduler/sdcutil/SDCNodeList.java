package scheduler.sdcutil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import scheduler.Graph;
import scheduler.Node;
import scheduler.RC;
import scheduler.RT;

/**
 * An ordered list of nodes with support for the SASDC scheduler.
 * 
 * @author Mitja Stachowiak, Ludwig Meysel
 *
 */
public class SDCNodeList {
	public final int length;
	private int iSwap1, iSwap2;
	private boolean temporary = false;
	private HashMap<RT, Set<RT>> rtSet; // maps types to a resource (which could contain more types).
	private HashMap<RT, Integer> rtCount; // the number of resources compatible with certain type.
	private Node[] list;

	public SDCNodeList(RC constraints, Graph sg) {
		this.length = sg.size();
		preprocessResourceConstraints(constraints, sg);

		list = new Node[length];
		ArrayList<Node> ordered = new ArrayList<Node>();
		for (Node node : sg)
			ordered.add(node);
		ordered.sort((n1, n2) -> n1.getDepth() - n2.getDepth());
		ordered.toArray(list);
	}

	/**
	 * Extracts necessary information from the node constraints and checks whether
	 * the resources are overlapping-free.
	 * 
	 * @param rc The resource constraints.
	 * @param sched The graph.
	 */
	private void preprocessResourceConstraints(RC rc, Graph sg) {
		rtSet = new HashMap<>();
		rtCount = new HashMap<>();
		Map<String, Set<RT>> allRes = rc.getAllRes();
		HashSet<RT> required = new HashSet<>();
		for (Node n : sg) { // find required node types
			if (!required.contains(n.getRT()))
				required.add(n.getRT());
		}
		for (RT rt : required) { // check whether rc is overlapping-free for the required resources.
			if (rc.getResCount(rt) > 0) {
				int hash = -1;
				for (String set : rc.getRes(rt)) {
					if (hash == -1) {
						hash = allRes.get(set).hashCode();
						rtSet.put(rt, allRes.get(set));
						rtCount.put(rt, rc.getResCount(rt));
					} else if (allRes.get(set).hashCode() != hash)
						throw new RuntimeException("Resource constraints containing overlapping resources are not supported when using SDC scheduling.");
				}
			} else
				throw new RuntimeException(String.format("Resource constrataints do not contain a resource which is compatible with operation \"%s\".", rt.name));

		}
	}

	/**
	 * Swaps the two nodes at the given indices in the list.
	 * 
	 * @param i1 The index of the one node.
	 * @param i2 The index of the other node.
	 */
	private void swap(int i1, int i2) {
		Node tmp = list[i1];
		list[i1] = list[i2];
		list[i2] = tmp;
	}

	/**
	 * Swaps the two nodes at the given indices in the list and marks the list as
	 * temporary.
	 * 
	 * @param i1 The index of the one node.
	 * @param i2 The index of the other node.
	 * @param force true to ignore flow dependencies or the possible temporary
	 *            state. When forcing, state is not revertable.
	 * @return True if the nodes are swapped, false if a swap is not possible (due
	 *         to a dataflow dependency).
	 */
	public boolean swapNodes(int i1, int i2, boolean force) {
		if (force) {
			swap(i1, i2);
			return false;
		}

		if (list[i1].hasFlowDependency(list[i2]))
			return false;
		if (temporary)
			throw new RuntimeException("Cannot swap nodes - the last swap neither has been accepted nor reverted.");
		swap(iSwap1 = i1, iSwap2 = i2);
		return temporary = true;
	}

	/**
	 * Reverts the last swap.
	 */
	public void revert() {
		if (!temporary)
			throw new RuntimeException("Nothing to revert. Require a call of swapNodes() in order to revert this swap.");
		swap(iSwap2, iSwap1);
		temporary = false;
	}

	/**
	 * Accepts the last swap.
	 */
	public void accept() {
		if (!temporary)
			throw new RuntimeException("Nothing to accept. Require call of swapNodes() in order to accept this swap.");
		temporary = false;
	}

	/**
	 * Gets the next node matching the resource compatible with the specified type
	 * and furthermore considers the number of resources for this type. <br>
	 * <br>
	 * If the list e.g. contains 4 operations ADD, SUB, SUB, SUB and the resource
	 * contraints specify 2 ALUs, then (with rt=ADD, start=1) the returned node will
	 * be the second SUB operation which is the second (&rarr; 2 ALUs) operation
	 * which uses the same resource as ADD. <br>
	 * <br>
	 * See also the lecture slides chapter 3, slide 99 et seqq. and 110 et seqq.
	 * 
	 * @param rt The type.
	 * @param start The position in the list where to start the search.
	 * @return The next node according to the resource constraints. Null, if no node
	 *         has been found.
	 */
	public Node nextOfType(RT rt, int start) {
		int c = rtCount.get(rt);
		Set<RT> res = rtSet.get(rt);
		for (int i = start; i < list.length; i++) {
			if (res.contains(list[i].getRT())) {
				c--;
				if (c == 0)
					return list[i];
			}
		}
		return null;
	}

	/**
	 * Gets the node at the specified index.
	 * 
	 * @param i The index of the requested node.
	 * @return The node at index i.
	 */
	public Node get(int i) {
		return list[i];
	}
}
