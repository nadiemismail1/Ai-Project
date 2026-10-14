package code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * The generic search procedure: takes ANY GenericSearchProblem and a strategy
 *   "ID" = iterative deepening, "UC" = uniform cost, "AS" = A*
 * and returns the goal node (or null if there is no solution).
 */
public class GenericSearch {

    private long nodesExpanded;                       // nodes chosen for expansion
    private long sequenceCounter;                     // for FIFO tie-breaking in the queue

    public long getNodesExpanded() {
        return nodesExpanded;
    }

    /** Runs the requested strategy on the problem. */
    public Node search(GenericSearchProblem problem, String strategy) {
        nodesExpanded = 0;
        sequenceCounter = 0;
        switch (strategy) {
            case "ID": return iterativeDeepening(problem);
            case "UC": return bestFirst(problem, false);
            case "AS": return bestFirst(problem, true);
            default: throw new IllegalArgumentException("Unknown strategy: " + strategy);
        }
    }

    // =====================================================================
    //  UC and A*  (best-first search; they only differ in the priority)
    // =====================================================================
    private Node bestFirst(GenericSearchProblem problem, boolean useHeuristic) {
        PriorityQueue<Node> frontier = new PriorityQueue<>((a, b) -> {
            if (a.priority != b.priority) return Long.compare(a.priority, b.priority);
            return Long.compare(a.sequence, b.sequence);
        });
        // cheapest path cost found so far for each state key (repeated-state handling)
        HashMap<Long, Long> bestCost = new HashMap<>();

        Node root = new Node(problem.getInitialState(), null, null, 0, 0);
        if (useHeuristic) {
            long h = problem.heuristic(root.state);
            if (h >= GenericSearchProblem.INFINITY) return null;
            root.priority = h;
        }
        root.sequence = sequenceCounter++;
        frontier.add(root);
        bestCost.put(root.state.key(), 0L);

        while (!frontier.isEmpty()) {
            Node node = frontier.poll();

            // A cheaper way to this same state was found after this node was queued: skip it.
            if (node.pathCost > bestCost.get(node.state.key())) continue;

            nodesExpanded++;
            if (problem.goalTest(node.state)) return node;   // goal test when chosen => optimal

            for (Node child : problem.expand(node)) {
                long key = child.state.key();
                Long known = bestCost.get(key);
                if (known != null && known <= child.pathCost) continue;   // redundant state

                if (useHeuristic) {
                    long h = problem.heuristic(child.state);
                    if (h >= GenericSearchProblem.INFINITY) continue;     // can never reach a goal
                    child.priority = child.pathCost + h;                  // f = g + h
                } else {
                    child.priority = child.pathCost;                      // f = g
                }
                child.sequence = sequenceCounter++;
                bestCost.put(key, child.pathCost);
                frontier.add(child);
            }
        }
        return null;   // frontier empty: no solution
    }

    private Node iterativeDeepening(GenericSearchProblem problem) { return null; } // TEMP: replaced in Part 11
}