package code;

import java.util.ArrayList;
import java.util.List;

/**
 * The cave explorer problem (a subclass of the generic search problem).
 *
 * Path cost is lexicographic: fewer lives used is ALWAYS better, then less energy.
 * It is encoded in one number:   cost = livesUsed * LIFE_WEIGHT + energyUsed
 * LIFE_WEIGHT is bigger than any possible energy total (energy <= 500), so one
 * life is always "more expensive" than all the energy in the game.
 */
public class CaveExplorer extends GenericSearchProblem {

    private static final int START_LIVES = 3;
    private static final long LIFE_WEIGHT = 10_000;
    private static final int NO_PATH = 1_000_000;   // "infinite" distance between two cells

    // ---------- the cave (never changes during the search) ----------
    private final int rows;            // H: number of rows
    private final int cols;            // W: number of columns
    private final int[] difficulty;    // by cell index; 0 = wall
    private final int[] doorAt;        // by cell index; door number or -1
    private final int[] keyAt;         // by cell index; key number or -1
    private final int[] doorPos;       // by door number -> cell index
    private final int[] keyPos;        // by key number  -> cell index
    private final int initialEnergy;
    private final int[][] dist;        // dist[a][b] = cheapest energy to walk from cell a to cell b (used by the heuristic)

    // =====================================================================
    //  Entry point required by the assignment
    // =====================================================================
    public static String solve(String initString, String strategy) {
        CaveExplorer problem = new CaveExplorer(initString);
        GenericSearch search = new GenericSearch();
        Node goal = search.search(problem, strategy);

        if (goal == null) {
            return "No Solution";
        }

        // Walk back from the goal to the root to rebuild the plan.
        ArrayList<String> actions = new ArrayList<>();
        for (Node n = goal; n.parent != null; n = n.parent) {
            actions.add(0, n.operator);
        }
        CaveState end = (CaveState) goal.state;
        int livesUsed = START_LIVES - end.lives;
        int energyUsed = problem.initialEnergy - end.energy;

        return String.join(",", actions) + ";" + livesUsed + ";" + energyUsed + ";" + search.getNodesExpanded();
    }

    // =====================================================================
    //  Parsing the grid string
    // =====================================================================
    public CaveExplorer(String init) {
        String[] parts = init.split(";");

        String[] size = parts[0].split(",");
        rows = Integer.parseInt(size[0].trim());
        cols = Integer.parseInt(size[1].trim());

        String[] start = parts[1].split(",");
        int startX = Integer.parseInt(start[0].trim());
        int startY = Integer.parseInt(start[1].trim());

        String[] resources = parts[2].split(",");
        initialEnergy = Integer.parseInt(resources[0].trim());
        int initialRope = Integer.parseInt(resources[1].trim());

        difficulty = new int[rows * cols];
        for (int y = 0; y < rows; y++) {
            String[] cells = parts[3 + y].split(",");
            for (int x = 0; x < cols; x++) {
                difficulty[y * cols + x] = Integer.parseInt(cells[x].trim());
            }
        }

        int[] doorXY = parseNumbers(parts, 3 + rows);
        int[] keyXY = parseNumbers(parts, 4 + rows);

        doorAt = new int[rows * cols];
        keyAt = new int[rows * cols];
        java.util.Arrays.fill(doorAt, -1);
        java.util.Arrays.fill(keyAt, -1);

        doorPos = new int[doorXY.length / 2];
        for (int i = 0; i < doorPos.length; i++) {
            doorPos[i] = doorXY[2 * i + 1] * cols + doorXY[2 * i];   // y * cols + x
            doorAt[doorPos[i]] = i;
        }
        keyPos = new int[keyXY.length / 2];
        for (int i = 0; i < keyPos.length; i++) {
            keyPos[i] = keyXY[2 * i + 1] * cols + keyXY[2 * i];
            keyAt[keyPos[i]] = i;
        }

        dist = computeEnergyDistances();

        initialState = new CaveState(
                startY * cols + startX, initialEnergy, initialRope, START_LIVES,
                false, (1 << doorPos.length) - 1, (1 << keyPos.length) - 1);
    }

    /** Reads "a,b,c,d" from parts[index] into an int array (empty if the part is missing). */
    private static int[] parseNumbers(String[] parts, int index) {
        if (index >= parts.length || parts[index].trim().isEmpty()) return new int[0];
        String[] tokens = parts[index].split(",");
        int[] numbers = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) numbers[i] = Integer.parseInt(tokens[i].trim());
        return numbers;
    }

    // =====================================================================
    //  Goal test
    // =====================================================================
    @Override
    public boolean goalTest(State state) {
        // Success = no locked door left (the explorer always keeps >= 1 life by construction).
        return ((CaveState) state).doorsMask == 0;
    }

    @Override public List<Node> expand(Node node) { return new ArrayList<>(); } // TEMP: replaced in Part 9

    @Override public long heuristic(State state) { return 0; } // TEMP: replaced in Part 10 (step 10A)

    private int[][] computeEnergyDistances() { return new int[0][0]; } // TEMP: replaced in Part 10 (step 10B)
}