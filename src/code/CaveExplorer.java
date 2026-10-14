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

    // =====================================================================
    //  Operators
    // =====================================================================
    @Override
    public List<Node> expand(Node node) {
        CaveState s = (CaveState) node.state;
        List<Node> children = new ArrayList<>();

        // unlock: carrying a key and standing on a locked door -> door opens, key is used up
        int door = doorAt[s.pos];
        if (s.carrying && door >= 0 && (s.doorsMask & (1 << door)) != 0) {
            CaveState next = new CaveState(s.pos, s.energy, s.rope, s.lives,
                    false, s.doorsMask & ~(1 << door), s.keysMask);
            children.add(new Node(next, node, "unlock", node.depth + 1, node.pathCost));
        }

        // collect: hands free and standing on a key -> pick it up
        int key = keyAt[s.pos];
        if (!s.carrying && key >= 0 && (s.keysMask & (1 << key)) != 0) {
            CaveState next = new CaveState(s.pos, s.energy, s.rope, s.lives,
                    true, s.doorsMask, s.keysMask & ~(1 << key));
            children.add(new Node(next, node, "collect", node.depth + 1, node.pathCost));
        }

        //            operator     dx  dy  needsRope needsLife
        addMove(children, node, "left",      -1,  0, false, false);
        addMove(children, node, "right",      1,  0, false, false);
        addMove(children, node, "climbup",    0, -1, true,  false);
        addMove(children, node, "climbdown",  0,  1, true,  false);
        addMove(children, node, "jumpdown",   0,  1, false, true);
        return children;
    }

    /** Tries one movement operator; adds the child to the list only if it is legal. */
    private void addMove(List<Node> children, Node node, String name,
                         int dx, int dy, boolean needsRope, boolean needsLife) {
        CaveState s = (CaveState) node.state;
        int x = s.pos % cols + dx;
        int y = s.pos / cols + dy;

        if (x < 0 || x >= cols || y < 0 || y >= rows) return;   // outside the grid
        int cost = difficulty[y * cols + x];
        if (cost == 0) return;                                  // wall
        if (s.energy < cost) return;                            // not enough energy for that cell
        if (needsRope && s.rope < 1) return;                    // no rope left to climb
        if (needsLife && s.lives <= 1) return;                  // jumping needs MORE than one life

        CaveState next = new CaveState(y * cols + x, s.energy - cost,
                s.rope - (needsRope ? 1 : 0), s.lives - (needsLife ? 1 : 0),
                s.carrying, s.doorsMask, s.keysMask);
        long stepCost = cost + (needsLife ? LIFE_WEIGHT : 0);
        children.add(new Node(next, node, name, node.depth + 1, node.pathCost + stepCost));
    }

    // =====================================================================
    //  Heuristic for A*
    // =====================================================================
    /*
     * HEURISTIC EXPLANATION
     * ---------------------
     * We relax the problem (ignore walls' effect on lives/rope along the way, ignore
     * that the same energy is shared by all doors, allow free movement in 4 directions)
     * and compute a LOWER BOUND on the cost still needed to unlock the LAST door.
     * The bound uses BOTH resources that make up the path cost:
     *
     *  ENERGY: dist[a][b] is the cheapest total cell difficulty to walk from a to b
     *          (Dijkstra / Floyd-Warshall on the real grid, walls removed from the
     *          graph, but every move allowed). Real plans can never beat it.
     *
     *  LIVES : going up is only possible with climbup (1 rope each) and going down
     *          needs climbdown (1 rope) or jumpdown (1 life). So a route that needs V
     *          vertical moves with only R meters of rope left must spend at least
     *          V - R lives. If the route needs more climbups than R, it is impossible.
     *
     * For every locked door D we need a route from the current cell to D:
     *    - if we already carry a key:  current -> D
     *    - otherwise we must first pick up some key K:  current -> K -> D
     *      (we take the K that gives the smallest bound).
     * The cost for D is  lives * LIFE_WEIGHT + energy  (same units as the real path cost).
     * All doors must be opened, so the real remaining cost is at least the LARGEST of
     * these per-door bounds:  h = max over locked doors.
     *
     * Admissible: every part is a lower bound of what any real plan must pay, and
     * "max" of lower bounds is still a lower bound. It is not a plain distance: it
     * counts cell difficulty (energy), rope and lives. A state that cannot possibly
     * finish (not enough energy / rope / lives / keys) gets INFINITY and is pruned.
     */
    @Override
    public long heuristic(State state) {
        CaveState s = (CaveState) state;
        if (s.doorsMask == 0) return 0;

        // Not enough keys left (on the floor + in hand) for the doors that are still locked.
        int keysAvailable = Integer.bitCount(s.keysMask) + (s.carrying ? 1 : 0);
        if (keysAvailable < Integer.bitCount(s.doorsMask)) return INFINITY;

        long worst = 0;
        for (int d = 0; d < doorPos.length; d++) {
            if ((s.doorsMask & (1 << d)) == 0) continue;     // already unlocked

            long best = INFINITY;
            if (s.carrying) {
                best = routeBound(s, -1, doorPos[d]);        // current -> door
            } else {
                for (int k = 0; k < keyPos.length; k++) {
                    if ((s.keysMask & (1 << k)) == 0) continue;
                    best = Math.min(best, routeBound(s, keyPos[k], doorPos[d]));  // current -> key -> door
                }
            }
            if (best >= INFINITY) return INFINITY;           // this door can never be opened
            worst = Math.max(worst, best);
        }
        return worst;
    }

    /**
     * Lower bound (in path-cost units) for walking from the current cell to 'target',
     * optionally passing through cell 'via' first (pass -1 for a direct route).
     * Returns INFINITY if even this relaxed route is impossible.
     */
    private long routeBound(CaveState s, int via, int target) {
        int curY = s.pos / cols;
        int targetY = target / cols;
        long energyNeeded;
        int verticalMoves;
        int climbUpsNeeded;

        if (via < 0) {
            energyNeeded = dist[s.pos][target];
            verticalMoves = Math.abs(curY - targetY);
            climbUpsNeeded = Math.max(0, curY - targetY);
        } else {
            int viaY = via / cols;
            energyNeeded = (long) dist[s.pos][via] + dist[via][target];
            verticalMoves = Math.abs(curY - viaY) + Math.abs(viaY - targetY);
            climbUpsNeeded = Math.max(0, curY - viaY) + Math.max(0, viaY - targetY);
        }

        if (energyNeeded >= NO_PATH || energyNeeded > s.energy) return INFINITY;  // unreachable / too tired
        if (climbUpsNeeded > s.rope) return INFINITY;                            // going up needs rope
        int livesNeeded = Math.max(0, verticalMoves - s.rope);                   // the rest must be jumpdowns
        if (livesNeeded > s.lives - 1) return INFINITY;                          // jumping keeps >= 1 life

        return livesNeeded * LIFE_WEIGHT + energyNeeded;
    }

    /** Floyd-Warshall on the cells: cheapest energy between every pair of cells (walls excluded). */
    private int[][] computeEnergyDistances() {
        int n = rows * cols;
        int[][] d = new int[n][n];
        for (int a = 0; a < n; a++) {
            java.util.Arrays.fill(d[a], NO_PATH);
            d[a][a] = 0;
        }
        int[] dxs = { -1, 1, 0, 0 };
        int[] dys = { 0, 0, -1, 1 };
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (difficulty[y * cols + x] == 0) continue;          // wall
                for (int i = 0; i < 4; i++) {
                    int nx = x + dxs[i], ny = y + dys[i];
                    if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) continue;
                    int cost = difficulty[ny * cols + nx];
                    if (cost == 0) continue;                          // cannot enter a wall
                    d[y * cols + x][ny * cols + nx] = cost;           // cost = difficulty of the cell we enter
                }
            }
        }
        for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++) {
                if (d[i][k] >= NO_PATH) continue;
                for (int j = 0; j < n; j++) {
                    if (d[i][k] + d[k][j] < d[i][j]) d[i][j] = d[i][k] + d[k][j];
                }
            }
        return d;
    }
}