import java.util.*;

public abstract class NonPreemptiveScheduler extends Scheduler {
    protected abstract int compare(PCB a, PCB b);

    @Override public boolean isPreemptive() { return false; }

    private int order(PCB a, PCB b) {
        int c = compare(a, b);
        if (c == 0) c = Long.compare(a.getArrival(), b.getArrival());
        if (c == 0) c = Integer.compare(a.getId(), b.getId());
        return c;
    }

    @Override
    protected void execute(List<PCB> processes, List<Segment> timeline) {
        List<PCB> byArrival = sortedByArrival(processes);
        PriorityQueue<PCB> ready = new PriorityQueue<>(this::order);
        long t = 0;
        int next = 0, n = byArrival.size();
        while (next < n || !ready.isEmpty()) {
            if (ready.isEmpty() && byArrival.get(next).getArrival() > t) { // CPU idle until next arrival
                long na = byArrival.get(next).getArrival();
                addSegment(timeline, null, t, na);
                t = na;
            }
            while (next < n && byArrival.get(next).getArrival() <= t) ready.add(byArrival.get(next++));
            PCB p = ready.poll();
            p.markStart(t);
            addSegment(timeline, p, t, t + p.getBurst());
            t += p.getBurst();
            p.setCompletion(t);
        }
    }
}
