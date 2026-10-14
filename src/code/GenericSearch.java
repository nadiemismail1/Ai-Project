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

    // ---- state used by iterative deepening ----
    private HashMap<Long, ArrayList<long[]>> visited; // key -> list of {depth, cost} already explored
    private boolean cutoff;                           // did the depth limit cut off some branch?

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

    // =====================================================================
    //  Iterative deepening search
    // =====================================================================
    private Node iterativeDeepening(GenericSearchProblem problem) {
        for (int limit = 0; ; limit++) {
            visited = new HashMap<>();
            cutoff = false;
            Node root = new Node(problem.getInitialState(), null, null, 0, 0);
            Node result = depthLimited(problem, root, limit);
            if (result != null) return result;
            if (!cutoff) return null;   // whole space explored, nothing was cut: no solution
        }
    }

    /** Depth-limited DFS that skips states already explored at least as cheaply and as shallowly. */
    private Node depthLimited(GenericSearchProblem problem, Node node, int limit) {
        if (isRedundant(node)) return null;

        // Dead end: this state can provably never reach a goal (not enough energy, rope, lives or keys).
        if (problem.heuristic(node.state) >= GenericSearchProblem.INFINITY) return null;

        nodesExpanded++;
        if (problem.goalTest(node.state)) return node;

        if (node.depth == limit) {
            // We are not allowed to go deeper. Remember if there was anything left to explore.
            if (!problem.expand(node).isEmpty()) cutoff = true;
            return null;
        }
        for (Node child : problem.expand(node)) {
            Node result = depthLimited(problem, child, limit);
            if (result != null) return result;
        }
        return null;
    }

    /**
     * (Dead-end states are also skipped, see depthLimited.)
     * A node is redundant if the same state was already visited in this iteration
     * with depth <= ours AND cost <= ours (it had at least as much room and energy left).
     * Otherwise it is recorded as visited.
     */
    private boolean isRedundant(Node node) {
        long key = node.state.key();
        ArrayList<long[]> seen = visited.get(key);
        if (seen == null) {
            seen = new ArrayList<>();
            visited.put(key, seen);
        }
        for (long[] entry : seen) {
            if (entry[0] <= node.depth && entry[1] <= node.pathCost) return true;
        }
        // forget entries that the new node makes useless, then record the new one
        seen.removeIf(entry -> entry[0] >= node.depth && entry[1] >= node.pathCost);
        seen.add(new long[] { node.depth, node.pathCost });
        return false;
    }
}