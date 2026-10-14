package code;

/**
 * A state of the cave explorer problem. It is immutable: every operator
 * creates a NEW CaveState instead of changing the old one.
 */
public class CaveState extends State {

    public final int pos;          // cell index = y * width + x
    public final int energy;       // energy the explorer still has
    public final int rope;         // rope (in meters) the explorer still has
    public final int lives;        // lives the explorer still has
    public final boolean carrying; // true if the explorer holds a key
    public final int doorsMask;    // bit i = 1  =>  door i is still locked
    public final int keysMask;     // bit i = 1  =>  key i is still lying on the floor

    public CaveState(int pos, int energy, int rope, int lives,
                     boolean carrying, int doorsMask, int keysMask) {
        this.pos = pos;
        this.energy = energy;
        this.rope = rope;
        this.lives = lives;
        this.carrying = carrying;
        this.doorsMask = doorsMask;
        this.keysMask = keysMask;
    }

    /**
     * Packs everything EXCEPT the energy into one long.
     * Energy is left out on purpose: for two states that are identical in all
     * the other fields, the one reached with LESS energy used is always at least
     * as good, and the search keeps only the cheapest path to each key.
     * (Lives ARE part of the key because fewer lives used is "worse energy-wise"
     * only in the lexicographic cost, so such states must not be merged.)
     */
    @Override
    public long key() {
        long k = pos;                         // 10 bits
        k = (k << 8) | rope;                  // 8 bits
        k = (k << 3) | lives;                 // 3 bits
        k = (k << 1) | (carrying ? 1 : 0);    // 1 bit
        k = (k << 16) | doorsMask;            // 16 bits
        k = (k << 16) | keysMask;             // 16 bits
        return k;
    }
}