import java.util.*;


public class PageTable {
    private final int pageSize, numFrames;
    private final int[] frameOf;
    private final int[] owner;

    public PageTable(int pageSize, int numFrames, int[] frameOf) {
        if (pageSize < 1) throw new IllegalArgumentException("Page size must be at least 1.");
        if (numFrames < 1) throw new IllegalArgumentException("Number of frames must be at least 1.");
        if (frameOf.length < 1) throw new IllegalArgumentException("There must be at least one page.");
        if ((long) pageSize * frameOf.length > Integer.MAX_VALUE) throw new IllegalArgumentException("Logical memory is too large (pages x page size must be below 2,147,483,647).");
        if ((long) pageSize * numFrames > Integer.MAX_VALUE) throw new IllegalArgumentException("Physical memory is too large (frames x page size must be below 2,147,483,647).");
        this.pageSize = pageSize;
        this.numFrames = numFrames;
        this.frameOf = frameOf.clone();
        this.owner = new int[numFrames];
        Arrays.fill(owner, -1);
        for (int p = 0; p < frameOf.length; p++) {
            int f = frameOf[p];
            if (f == -1) continue;
            if (f < 0 || f >= numFrames)
                throw new IllegalArgumentException("Page " + p + " maps to frame " + f + ", but valid frames are 0 to " + (numFrames - 1) + ".");
            if (owner[f] != -1)
                throw new IllegalArgumentException("Frame " + f + " is used by both page " + owner[f] + " and page " + p + ".");
            owner[f] = p;
        }
    }

    public int getPageSize() { return pageSize; }
    public int getPageCount() { return frameOf.length; }
    public int getFrameCount() { return numFrames; }
    public int getFrameOf(int page) { return frameOf[page]; }
    public int getPageInFrame(int frame) { return owner[frame]; }
    public int getLogicalSize() { return pageSize * frameOf.length; }
    public int getPhysicalSize() { return pageSize * numFrames; }

    public Translation translate(int address) {
        if (address < 0 || address >= getLogicalSize())
            throw new IllegalArgumentException("Logical address " + address + " is outside the logical memory (0 to " + (getLogicalSize() - 1) + ").");
        Translation t = new Translation();
        t.logical = address;
        t.page = address / pageSize;
        t.offset = address % pageSize;
        t.frame = frameOf[t.page];
        t.steps.add("Logical address " + address);
        t.steps.add("Page number = " + address + " / " + pageSize + " = " + t.page);
        t.steps.add("Offset = " + address + " mod " + pageSize + " = " + t.offset);
        if (t.frame == -1) {
            t.physical = -1;
            t.steps.add("Page table[" + t.page + "] = not in memory -> PAGE FAULT (no physical address)");
        } else {
            t.physical = (long) t.frame * pageSize + t.offset;
            t.steps.add("Page table[" + t.page + "] = frame " + t.frame);
            t.steps.add("Physical address = " + t.frame + " x " + pageSize + " + " + t.offset + " = " + t.physical);
        }
        return t;
    }

    public static class Translation {
        public int logical, page, offset, frame;
        public long physical;
        public final List<String> steps = new ArrayList<>();
        public boolean inMemory() { return frame != -1; }
    }
}
