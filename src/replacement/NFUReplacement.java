package replacement;

import java.util.Arrays;

public class NFUReplacement extends PageReplacement {
    private int[] use;
    private int ptr;

    @Override
    public String getName() {
        return "NFU";
    }

    @Override
    protected void init(int n) {
        use = new int[n];
        Arrays.fill(use, 0);
        ptr = 0;
    }

    @Override
    protected void onHit(int slot, int now) {
        use[slot] = 1;
    }

    @Override
    protected int chooseVictim(int now) {
        while (use[ptr] == 1) {
            use[ptr] = 0;
            ptr = (ptr + 1) % use.length;
        }
        return ptr;
    }

    @Override
    protected void onLoad(int slot, int now) {
        use[slot] = 1;
        ptr = (slot + 1) % use.length;
    }

    @Override
    protected int[] useBits() {
        return use;
    }

    @Override
    protected int pointer() {
        return ptr;
    }
}