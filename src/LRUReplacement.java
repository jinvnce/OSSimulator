/** LRU: evicts the page that was used least recently. */
public class LRUReplacement extends PageReplacement {
    private int[] lastUsed;
    @Override public String getName() { return "LRU"; }
    @Override protected void init(int n) { lastUsed = new int[n]; }
    @Override protected void onHit(int slot, int now) { lastUsed[slot] = now; }
    @Override protected void onLoad(int slot, int now) { lastUsed[slot] = now; }
    @Override protected int chooseVictim(int now) {
        int v = 0;
        for (int k = 1; k < lastUsed.length; k++) if (lastUsed[k] < lastUsed[v]) v = k;
        return v;
    }
}
