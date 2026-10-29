import java.util.*;

/**
 * Shared logic for the preemptive versions of SJF (SRTF) and Priority.
 * At every arrival or completion the CPU is given to the best ready job, so a newly arrived
 * job can take the CPU away from the running one. Subclasses only supply the key to compare.
 */
public abstract class PreemptiveScheduler extends Scheduler {
    /** Smaller key runs first. 'remaining' is the burst time the job still needs. */
    protected abstract int key(PCB p, int remaining);

    @Override
    protected void execute(List<PCB> processes, List<Segment> timeline) {
        List<PCB> byArrival = sortedByArrival(processes);
        Map<PCB, Integer> remaining = new HashMap<>();
        for (PCB p : byArrival) remaining.put(p, p.getBurst());
        List<PCB> ready = new ArrayList<>();
        int t = 0, next = 0, finished = 0, n = byArrival.size();
        while (finished < n) {
            while (next < n && byArrival.get(next).getArrival() <= t) ready.add(byArrival.get(next++));
            if (ready.isEmpty()) {                                   // CPU idle until the next arrival
                int na = byArrival.get(next).getArrival();
                addSegment(timeline, null, t, na);
                t = na;
                continue;
            }
            PCB best = ready.get(0);
            for (PCB p : ready) if (better(p, best, remaining)) best = p;
            best.markStart(t);
            // run until it finishes or until the next job arrives (which may preempt it)
            int run = remaining.get(best);
            if (next < n) run = Math.min(run, byArrival.get(next).getArrival() - t);
            addSegment(timeline, best, t, t + run);
            t += run;
            remaining.put(best, remaining.get(best) - run);
            if (remaining.get(best) == 0) { best.setCompletion(t); ready.remove(best); finished++; }
        }
    }

    private boolean better(PCB a, PCB b, Map<PCB, Integer> remaining) {
        int c = Integer.compare(key(a, remaining.get(a)), key(b, remaining.get(b)));
        if (c == 0) c = Integer.compare(a.getArrival(), b.getArrival());
        if (c == 0) c = Integer.compare(a.getId(), b.getId());
        return c < 0;
    }
}
