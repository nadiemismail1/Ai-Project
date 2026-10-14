package code;

import java.util.ArrayList;
import java.util.List;

public class CaveExplorer extends GenericSearchProblem {

    public static String solve(String initString, String strategy) {
        return "";
    }

    // Temporary placeholders so the project compiles; the real versions come in Parts 8-10.
    @Override public boolean goalTest(State state) { return false; }
    @Override public List<Node> expand(Node node) { return new ArrayList<>(); }
    @Override public long heuristic(State state) { return 0; }
}