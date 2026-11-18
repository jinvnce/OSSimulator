package replacement;

import java.util.Arrays;


public class NRUReplacement extends PageReplacement {
    private static final int INTERVAL = 4;
    private int[] r;
    private int epoch;

    @Override
    public String getName() {
        return "NRU";
    }

    @Override
    protected void init(int n) {
        r = new int[n];
        Arrays.fill(r, 0);
        epoch = 0;
    }

    private void tick(int now) {
        if (now / INTERVAL != epoch) {
            epoch = now / INTERVAL;
            Arrays.fill(r, 0);
        }
    }

    @Override
    protected void onHit(int slot, int now) {
        tick(now);
        r[slot] = 1;
    }

    @Override
    protected void beforeStep(int now) {
        tick(now);
    }

    @Override
    protected int chooseVictim(int now) {
        tick(now);
        for (int k = 0; k < r.length; k++) if (r[k] == 0) return k;
        return 0;
    }

    @Override
    protected void onLoad(int slot, int now) {
        tick(now);
        r[slot] = 1;
    }

    @Override
    protected int[] useBits() {
        return r;
    }
}