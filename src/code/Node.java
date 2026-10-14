package code;

/**
 * Abstract data type for a search-tree node (as presented in class).
 * A node wraps a state together with the bookkeeping the search needs:
 * parent, the operator that produced it, depth and the path cost g(n).
 */
public class Node {

    public final State state;        // the state this node holds
    public final Node parent;        // node we came from (null for the root)
    public final String operator;    // operator applied to the parent to reach this node
    public final int depth;          // number of operators applied since the root
    public final long pathCost;      // g(n): cost accumulated from the root

    // Used only by the best-first strategies (UC / A*) to order the frontier.
    public long priority;            // UC: g(n)      A*: g(n) + h(n)
    public long sequence;            // insertion order, used to break ties (FIFO)

    public Node(State state, Node parent, String operator, int depth, long pathCost) {
        this.state = state;
        this.parent = parent;
        this.operator = operator;
        this.depth = depth;
        this.pathCost = pathCost;
    }
}