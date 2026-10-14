package code;

import java.util.List;

/**
 * Abstract data type for a generic search problem (as presented in class):
 * an initial state, a set of operators (via expand), a goal test and a path cost.
 */
public abstract class GenericSearchProblem {

    /** Value returned by heuristic() when a state can never reach a goal. */
    public static final long INFINITY = 1_000_000_000L;

    /** The state the search starts from. */
    protected State initialState;

    public State getInitialState() {
        return initialState;
    }

    /** @return true if the given state satisfies the goal. */
    public abstract boolean goalTest(State state);

    /**
     * Applies every applicable operator to the node's state.
     * @return one child node per applicable operator (path cost already updated).
     */
    public abstract List<Node> expand(Node node);

    /**
     * Estimated remaining cost to a goal, used by A*.
     * Must never over-estimate (admissible).
     */
    public abstract long heuristic(State state);
}