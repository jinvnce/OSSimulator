import java.util.*;

public class PagingResult {
    private final List<Integer> pages = new ArrayList<>();
    private final List<int[]> frameStates = new ArrayList<>();
    private final List<Boolean> hits = new ArrayList<>();
    private final Map<Integer, Integer> pageTable = new HashMap<>();
    private int faults;

    void addStep(int page, int[] frames, boolean hit) {
        pages.add(page); frameStates.add(frames.clone()); hits.add(hit);
        if (!hit) faults++;
    }

    void buildPageTable(int[] frames) {
        for (int k = 0; k < frames.length; k++) if (frames[k] != -1) pageTable.put(frames[k], k);
    }

    public int getSteps() { return pages.size(); }
    public int getPage(int i) { return pages.get(i); }
    public int[] getFrames(int i) { return frameStates.get(i); }
    public boolean isHit(int i) { return hits.get(i); }
    public int getFaults() { return faults; }
    public int getHits() { return pages.size() - faults; }
    public double getHitRatio() { return pages.isEmpty() ? 0 : 100.0 * getHits() / pages.size(); }
    public Integer frameOf(int page) { return pageTable.get(page); }
}
