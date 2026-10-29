import java.util.*;

/**
 * Abstract page replacement algorithm. simulate() is the template method (the loop is the same for
 * every policy); subclasses only decide which frame to evict and keep their own bookkeeping.
 */
public abstract class PageReplacement {
    protected int[] frames;   // page held by each frame, -1 = empty
    protected int[] ref;      // the reference string

    public abstract String getName();
    protected abstract void init(int numFrames);
    protected abstract int chooseVictim(int now);          // called only when every frame is full
    protected void onHit(int slot, int now) { }
    protected void onLoad(int slot, int now) { }

    public final PagingResult simulate(int[] reference, int numFrames) {
        if (numFrames < 1) throw new IllegalArgumentException("Number of frames must be at least 1.");
        ref = reference;
        frames = new int[numFrames];
        Arrays.fill(frames, -1);
        init(numFrames);
        PagingResult result = new PagingResult();
        for (int i = 0; i < ref.length; i++) {
            int slot = -1;
            for (int k = 0; k < numFrames; k++) if (frames[k] == ref[i]) { slot = k; break; }
            boolean hit = slot >= 0;
            if (hit) onHit(slot, i);
            else {
                for (int k = 0; k < numFrames && slot == -1; k++) if (frames[k] == -1) slot = k;
                if (slot == -1) slot = chooseVictim(i);
                frames[slot] = ref[i];
                onLoad(slot, i);
            }
            result.addStep(ref[i], frames, hit);
        }
        result.buildPageTable(frames);
        return result;
    }

    @Override public String toString() { return getName(); }
}
