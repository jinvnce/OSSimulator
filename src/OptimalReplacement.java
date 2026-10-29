import java.util.*;

/**
 * Optimal: evicts the page that will not be used for the longest time.
 * Next-use positions are precomputed once, so the run is O(n * frames) instead of O(n^2 * frames).
 */
public class OptimalReplacement extends PageReplacement {
    private int[] nextUse;      // nextUse[i] = next index after i where ref[i] appears again
    private int[] slotNext;     // for each frame, when its page is needed next

    @Override public String getName() { return "Optimal"; }

    @Override protected void init(int n) {
        slotNext = new int[n];
        nextUse = new int[ref.length];
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = ref.length - 1; i >= 0; i--) {
            nextUse[i] = seen.getOrDefault(ref[i], Integer.MAX_VALUE);
            seen.put(ref[i], i);
        }
    }
    @Override protected void onHit(int slot, int now) { slotNext[slot] = nextUse[now]; }
    @Override protected void onLoad(int slot, int now) { slotNext[slot] = nextUse[now]; }
    @Override protected int chooseVictim(int now) {
        int v = 0;
        for (int k = 1; k < slotNext.length; k++) if (slotNext[k] > slotNext[v]) v = k;
        return v;
    }
}
