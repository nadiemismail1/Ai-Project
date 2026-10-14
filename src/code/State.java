package code;

/**
 * Abstract data type for a state of ANY search problem.
 * A state only has to be able to tell the search procedure how to recognise
 * "the same state again" (repeated-state checking).
 */
public abstract class State {

    /**
     * A number that identifies this state for repeated-state checking.
     * Two states with the same key are treated as the same state by the search.
     */
    public abstract long key();
}