import java.util.*;

/**
 * Banker's Algorithm (deadlock avoidance): the safety algorithm, checkSafety().
 * Need[i][j] = Max[i][j] - Allocation[i][j]. Available = Total - sum of Allocation (worked out by the panel).
 */
public class BankersAlgorithm {
    private final int[][] allocation, max, need;
    private final int[] available;

    /** Validates and copies the data; throws IllegalArgumentException for inconsistent input. */
    public BankersAlgorithm(int[][] allocation, int[][] max, int[] available) {
        int p = allocation.length, r = available.length;
        this.allocation = new int[p][]; this.max = new int[p][]; this.need = new int[p][r];
        for (int i = 0; i < p; i++) {
            this.allocation[i] = allocation[i].clone();
            this.max[i] = max[i].clone();
            for (int j = 0; j < r; j++) {
                String where = "P" + i + " " + Util.resourceName(j);
                if (allocation[i][j] < 0 || max[i][j] < 0) throw new IllegalArgumentException("Negative value at " + where + ".");
                if (max[i][j] < allocation[i][j])
                    throw new IllegalArgumentException("Inconsistent input: Max of " + where + " (" + max[i][j] + ") is less than its Allocation (" + allocation[i][j] + ").");
                need[i][j] = max[i][j] - allocation[i][j];
            }
        }
        for (int v : available) if (v < 0) throw new IllegalArgumentException("Available cannot be negative.");
        this.available = available.clone();
    }

    public int getProcessCount() { return allocation.length; }
    public int[] getNeed(int i) { return need[i].clone(); }
    public int[] getAllocation(int i) { return allocation[i].clone(); }
    public int[] getAvailable() { return available.clone(); }

    // ---------------- Safety algorithm ----------------
    public SafetyResult checkSafety() {
        int p = allocation.length, r = available.length;
        int[] work = available.clone();                 // Work = Available
        boolean[] finished = new boolean[p];            // Finish[i] = false
        SafetyResult result = new SafetyResult();
        int step = 1;
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < p; i++) {
                if (finished[i]) continue;
                int[] before = work.clone();
                boolean ok = fits(need[i], work);       // Finish[i] = false and Need[i] <= Work
                if (ok) {
                    for (int j = 0; j < r; j++) work[j] += allocation[i][j];   // Work = Work + Allocation[i]
                    finished[i] = true; progress = true;
                    result.sequence.add(i);
                }
                result.trace.add(new TraceRow(step++, i, need[i].clone(), before, ok, work.clone()));
            }
        }
        for (int i = 0; i < p; i++) if (!finished[i]) result.stuck.add(i);
        result.safe = result.stuck.isEmpty();
        return result;
    }

    private boolean fits(int[] a, int[] b) {
        for (int j = 0; j < a.length; j++) if (a[j] > b[j]) return false;
        return true;
    }

    public static String format(int[] v) {
        StringBuilder sb = new StringBuilder("(");
        for (int j = 0; j < v.length; j++) sb.append(j == 0 ? "" : ",").append(v[j]);
        return sb.append(")").toString();
    }

    // ---------------- result classes ----------------
    /** One check in the safety trace (one row of the lecture's example table). */
    public static class TraceRow {
        public final int step, process;
        public final int[] need, workBefore, workAfter;
        public final boolean ok;
        TraceRow(int step, int process, int[] need, int[] workBefore, boolean ok, int[] workAfter) {
            this.step = step; this.process = process; this.need = need;
            this.workBefore = workBefore; this.ok = ok; this.workAfter = workAfter;
        }
    }

    public static class SafetyResult {
        public boolean safe;
        public final List<Integer> sequence = new ArrayList<>();
        public final List<Integer> stuck = new ArrayList<>();
        public final List<TraceRow> trace = new ArrayList<>();
        public String sequenceText() {
            StringBuilder sb = new StringBuilder();
            for (int i : sequence) sb.append(sb.length() == 0 ? "" : " -> ").append("P").append(i);
            return sb.toString();
        }
    }
}
