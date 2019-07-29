package de.unipaderborn.visuflow.ui.graph;

import java.util.Iterator;

import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class EsgLayout {

	private static double spacingX = 5.0;
	private static double spacingY = 1.0;
	private static double spacingXRight = 1.0;

	/**
	 * Set layout of esg.
	 * @param graph
	 */
	public static void layout(Graph graph) {
		Iterator<Node> nodeIterator = graph.getNodeIterator();
		Node first = nodeIterator.next();
		Iterator<Node> depthFirstIterator = first.getDepthFirstIterator();

		//Assign the layer to each node
		first.setAttribute("layoutLayer", 0);
		while (depthFirstIterator.hasNext()) {
			Node curr = depthFirstIterator.next();
			int inDegree = curr.getInDegree();
			int layer = 1;
			if (inDegree == 1) {
				Iterable<Edge> edges = curr.getEachEnteringEdge();
				for (Edge edge : edges) {
					layer = edge.getOpposite(curr).getAttribute("layoutLayer");
					layer++;
				}
			}
			if (inDegree > 1) {
				Iterable<Edge> edges = curr.getEachEnteringEdge();
				int parentLayer = layer;
				for (Edge edge : edges) {
					Node parent = edge.getOpposite(curr);
					if (curr.hasAttribute("layoutLayer")) {
						int currLayer = parent.getAttribute("layoutLayer");
						if(currLayer > parentLayer)
							parentLayer = currLayer;
					}
				}
				layer = parentLayer++;
			}
			if(curr.hasAttribute("layoutLayer"))
				curr.removeAttribute("layoutLayer");
			curr.setAttribute("layoutLayer", layer);
		}

		//Assign the coordinates to each node
		Iterator<Node> breadthFirstIterator = first.getBreadthFirstIterator();
		first.setAttribute("xyz", spacingX, spacingY * graph.getNodeCount(), 0.0);
		first.setAttribute("layouted", "true");
		while (breadthFirstIterator.hasNext()) {
			Node curr = breadthFirstIterator.next();
			positionNode(curr);
		}

		// reset the "layouted" flag
		breadthFirstIterator = first.getBreadthFirstIterator();
		while (breadthFirstIterator.hasNext()) {
			Node curr = breadthFirstIterator.next();
			curr.removeAttribute("layouted");
		}
		int zeroDegCount = 0;
		System.out.println("for graph ");
		 for (int i = 0; i < graph.getNodeCount(); i++) {
		        Node node = graph.getNode(i); 
		        if(node.getDegree() == 0){
		        	zeroDegCount++;
		        	Node temp = graph.getNodeIterator().next();
		        	double[] positionOfFirst = Toolkit.nodePosition(temp);
		        	node.setAttribute("xyz", positionOfFirst[0]+(spacingXRight*zeroDegCount),positionOfFirst[1],0.0);
		        }
		        double nodePos[] = Toolkit.nodePosition(node); 
		         
		  //     System.out.println("pos "+ node.getAttribute("ui.label")+" x "+nodePos[0]+" , y "+nodePos[1]+" , z "+nodePos[2]); 
		 }
	}

	/**
	 * Assign coordinates to the node in the esg.
	 * @param node
	 */
	private static void positionNode(Node node) {
		Node parent = HierarchicalLayout.findParentWithHighestLevel(node);
		if(parent == null)
			return;

		double[] positionOfParent = Toolkit.nodePosition(parent);
		int outDegreeOfParent = parent.getOutDegree();
		if (outDegreeOfParent == 1) {
			node.setAttribute("xyz", positionOfParent[0], positionOfParent[1] - spacingY, 0.0);
			node.setAttribute("layouted", "true");
		 } else {
			if(node.getAttribute("below").equals(parent.getAttribute("below"))) {
				node.setAttribute("xyz", positionOfParent[0], positionOfParent[1] - spacingY, 0.0);
				node.setAttribute("layouted", "true");
			 } else {
				node.setAttribute("xyz", positionOfParent[0] + spacingXRight, positionOfParent[1] - spacingY, 0.0);
				node.setAttribute("layouted", "true");
			 }
		}
	}
}
