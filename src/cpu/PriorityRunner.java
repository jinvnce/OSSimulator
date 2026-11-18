package cpu;

import java.util.ArrayList;
import java.util.List;

/**
 * Priority scheduling (a LOWER number means a HIGHER priority). Works on plain arrays, so it does not depend on
 * the Scheduler / PCB classes. Ties are broken by arrival time, then by job number.
 *
 *   non-preemptive : once a job gets the CPU it runs until it finishes.
 *   preemptive     : when a job with a better priority arrives, it takes the CPU at once.
 *
 * All times are the fixed-point longs the panel already uses (decimals are scaled to whole numbers).
 */
public final class PriorityRunner {
    private PriorityRunner() { }

    public static final class Result {
        public final long[] start, finish;
        /** One entry per Gantt block: {job index, from, to}. A job index of -1 means the CPU was idle. */
        public final List<long[]> segments = new ArrayList<>();

        Result(int n) { start = new long[n]; finish = new long[n]; }
    }

    public static Result run(long[] arrival, long[] burst, int[] priority, boolean preemptive) {
        int n = arrival.length;
        Result res = new Result(n);
        long[] left = burst.clone();
        boolean[] started = new boolean[n], done = new boolean[n];
        int finished = 0;
        long time = Long.MAX_VALUE;                                      // start at the earliest arrival
        for (long a : arrival) time = Math.min(time, a);

        while (finished < n) {
            int pick = -1;
            for (int i = 0; i < n; i++) {
                if (done[i] || arrival[i] > time) continue;
                if (pick < 0 || better(i, pick, arrival, priority)) pick = i;
            }
            if (pick < 0) {                                              // nobody is here yet: the CPU idles until the next arrival
                long next = Long.MAX_VALUE;
                for (int i = 0; i < n; i++) if (!done[i]) next = Math.min(next, arrival[i]);
                addSegment(res, -1, time, next);
                time = next;
                continue;
            }
            long run = left[pick];
            if (preemptive) {
                long nextArrival = Long.MAX_VALUE;                       // run only until something new could arrive
                for (int i = 0; i < n; i++) if (!done[i] && arrival[i] > time) nextArrival = Math.min(nextArrival, arrival[i]);
                run = Math.min(run, nextArrival - time);
            }
            if (!started[pick]) { started[pick] = true; res.start[pick] = time; }
            addSegment(res, pick, time, time + run);
            time += run;
            left[pick] -= run;
            if (left[pick] == 0) { done[pick] = true; res.finish[pick] = time; finished++; }
        }
        return res;
    }

    /** True when job a should run before job b. */
    private static boolean better(int a, int b, long[] arrival, int[] priority) {
        if (priority[a] != priority[b]) return priority[a] < priority[b];
        if (arrival[a] != arrival[b]) return arrival[a] < arrival[b];
        return a < b;
    }

    /** Adds a Gantt block, joining it to the previous one when the same job keeps the CPU. */
    private static void addSegment(Result r, int job, long from, long to) {
        if (to <= from) return;
        if (!r.segments.isEmpty()) {
            long[] last = r.segments.get(r.segments.size() - 1);
            if (last[0] == job && last[2] == from) { last[2] = to; return; }
        }
        r.segments.add(new long[]{job, from, to});
    }
}