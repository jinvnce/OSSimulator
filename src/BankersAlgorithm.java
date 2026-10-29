import java.util.*;

/**
 * Banker's Algorithm (deadlock avoidance). Contains the two parts from the lecture:
 *   1. Safety algorithm        - checkSafety()
 *   2. Resource-request algorithm - request()
 * Need[i][j] = Max[i][j] - Allocation[i][j].
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

    // ---------------- Resource-request algorithm ----------------
    /** Tests a request from process p without changing this object; the pretend state is returned in the result. */
    public RequestResult request(int p, int[] req) {
        int n = allocation.length, r = available.length;
        if (p < 0 || p >= n) throw new IllegalArgumentException("Process must be between P0 and P" + (n - 1) + ".");
        if (req.length != r) throw new IllegalArgumentException("Request needs " + r + " values (one per resource type).");
        for (int j = 0; j < r; j++) if (req[j] < 0) throw new IllegalArgumentException("Request " + Util.resourceName(j) + " cannot be negative.");

        RequestResult res = new RequestResult();
        res.log.add("Request by P" + p + " = " + format(req));
        boolean ok1 = fits(req, need[p]);
        res.log.add("Step 1: Request <= Need?  " + format(req) + " <= " + format(need[p]) + "  " + (ok1 ? "YES" : "NO"));
        if (!ok1) {
            res.status = Status.EXCEEDED_MAX;
            res.log.add("Error: P" + p + " has exceeded its maximum claim.");
            return res;
        }
        boolean ok2 = fits(req, available);
        res.log.add("Step 2: Request <= Available?  " + format(req) + " <= " + format(available) + "  " + (ok2 ? "YES" : "NO"));
        if (!ok2) {
            res.status = Status.MUST_WAIT;
            res.log.add("P" + p + " must wait, the resources are not available.");
            return res;
        }
        int[][] a2 = new int[n][];
        for (int i = 0; i < n; i++) a2[i] = allocation[i].clone();
        int[] av2 = available.clone();
        for (int j = 0; j < r; j++) { av2[j] -= req[j]; a2[p][j] += req[j]; }
        res.after = new BankersAlgorithm(a2, max, av2);
        res.log.add("Step 3: Pretend to allocate:");
        res.log.add("   Available = " + format(available) + " - " + format(req) + " = " + format(av2));
        res.log.add("   Allocation of P" + p + " = " + format(allocation[p]) + " + " + format(req) + " = " + format(a2[p]));
        res.log.add("   Need of P" + p + " = " + format(need[p]) + " - " + format(req) + " = " + format(res.after.need[p]));
        res.safety = res.after.checkSafety();
        if (res.safety.safe) {
            res.status = Status.GRANTED;
            res.log.add("The new state is SAFE. Safe sequence: " + res.safety.sequenceText());
            res.log.add("REQUEST GRANTED.");
        } else {
            res.status = Status.DENIED_UNSAFE;
            res.log.add("The new state is NOT SAFE, so the old state is restored.");
            res.log.add("REQUEST DENIED. P" + p + " must wait.");
        }
        return res;
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
    public enum Status { EXCEEDED_MAX, MUST_WAIT, GRANTED, DENIED_UNSAFE }

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

    public static class RequestResult {
        public Status status;
        public final List<String> log = new ArrayList<>();
        public BankersAlgorithm after;   // pretend state (null if step 1 or 2 failed)
        public SafetyResult safety;      // safety check of the pretend state
    }
}
