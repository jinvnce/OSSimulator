import java.util.*;

/** Round Robin: each process runs for at most one time quantum, then goes to the back of the queue. */
public class RoundRobinScheduler extends Scheduler {
    private int quantum;

    public RoundRobinScheduler(int quantum) { setQuantum(quantum); }

    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) {
        if (quantum < 1) throw new IllegalArgumentException("Time quantum must be at least 1.");
        this.quantum = quantum;
    }

    @Override public String getName() { return "Round Robin"; }

    @Override
    protected void execute(List<PCB> processes, List<Segment> timeline) {
        List<PCB> sorted = sortedByArrival(processes);
        Map<PCB, Integer> remaining = new HashMap<>();
        for (PCB p : sorted) remaining.put(p, p.getBurst());
        Deque<PCB> queue = new ArrayDeque<>();
        int t = 0, next = 0, finished = 0, n = sorted.size();
        while (finished < n) {
            if (queue.isEmpty() && sorted.get(next).getArrival() > t) {
                int na = sorted.get(next).getArrival();
                addSegment(timeline, null, t, na);
                t = na;
            }
            while (next < n && sorted.get(next).getArrival() <= t) queue.add(sorted.get(next++));
            PCB p = queue.poll();
            p.markStart(t);
            int slice = Math.min(quantum, remaining.get(p));
            addSegment(timeline, p, t, t + slice);
            t += slice;
            remaining.put(p, remaining.get(p) - slice);
            while (next < n && sorted.get(next).getArrival() <= t) queue.add(sorted.get(next++)); // arrivals join first
            if (remaining.get(p) > 0) queue.add(p);
            else { p.setCompletion(t); finished++; }
        }
    }
}
