import java.util.ArrayList;
import java.util.List;

/**
 * The segment table of one process. Every segment has a BASE (where it starts in physical memory)
 * and a LIMIT (how long it is). A logical address is a pair  segment : offset.
 *
 *     valid     when  0 <= offset < limit        physical address = base + offset
 *     invalid   otherwise                        the hardware traps (addressing error)
 *
 * This class only does the arithmetic; the screen is in SegmentationPanel.
 */
public class SegmentTable {
    /** One row of the table. */
    public static final class Entry {
        public final long base, limit;
        Entry(long base, long limit) { this.base = base; this.limit = limit; }
        public long end() { return base + limit - 1; }          // last physical address of the segment
    }

    /** The answer for one logical address. */
    public static final class Result {
        public final String logical;
        public final int segment;
        public final long offset;
        public final boolean valid;
        public final long physical;             // only meaningful when valid
        public final Entry entry;               // null when the segment does not exist
        public final String message;            // one-line explanation (used in the calculation steps)

        Result(String logical, int segment, long offset, boolean valid, long physical, Entry entry, String message) {
            this.logical = logical; this.segment = segment; this.offset = offset;
            this.valid = valid; this.physical = physical; this.entry = entry; this.message = message;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void add(long base, long limit) {
        if (base < 0) throw new IllegalArgumentException("A base cannot be negative.");
        if (limit < 1) throw new IllegalArgumentException("A limit must be at least 1.");
        entries.add(new Entry(base, limit));
    }

    public int size() { return entries.size(); }

    public Entry get(int segment) { return entries.get(segment); }

    /** Translates segment:offset. Never throws: a bad address gives a Result with valid = false. */
    public Result translate(int segment, long offset) {
        String logical = segment + ":" + offset;
        if (segment < 0 || segment >= entries.size())
            return new Result(logical, segment, offset, false, -1, null,
                    "Segment " + segment + " does not exist (the table has segments 0 to " + (entries.size() - 1)
                            + "), so this is an addressing error.");
        Entry e = entries.get(segment);
        if (offset < 0)
            return new Result(logical, segment, offset, false, -1, e, "The offset " + offset + " is negative, so this is an addressing error.");
        if (offset >= e.limit)
            return new Result(logical, segment, offset, false, -1, e,
                    "Offset " + offset + " is not less than the limit " + e.limit + " of segment " + segment
                            + " (offset " + offset + " >= " + e.limit + "), so the address is out of range: trap to the operating system.");
        long phys = e.base + offset;
        return new Result(logical, segment, offset, true, phys, e,
                "Offset " + offset + " < limit " + e.limit + ", so the address is valid.  Physical = base " + e.base + " + offset " + offset + " = " + phys + ".");
    }

    /** Pairs of segment numbers whose physical memory overlaps (usually a mistake in the table). */
    public List<int[]> overlaps() {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++)
            for (int j = i + 1; j < entries.size(); j++) {
                Entry a = entries.get(i), b = entries.get(j);
                if (a.base <= b.end() && b.base <= a.end()) out.add(new int[]{i, j});
            }
        return out;
    }
}
