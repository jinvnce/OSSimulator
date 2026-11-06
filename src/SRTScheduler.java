import java.util.*;


public class SRTScheduler extends Scheduler {
    @Override public String getName() { return "SRT"; }
    @Override public boolean isPreemptive() { return true; }
    @Override public String getDescription() {
        return "Shortest Remaining Time: the job with the least time left runs. A new job that needs less time than the running job's remaining time interrupts it.";
    }

    @Override
    protected void execute(List<PCB> processes, List<Segment> timeline) {
        List<PCB> byArrival = sortedByArrival(processes);
        Map<PCB, Long> remaining = new HashMap<>();
        for (PCB p : byArrival) remaining.put(p, p.getBurst());
        List<PCB> ready = new ArrayList<>();
        long t = 0;
        int next = 0, finished = 0, n = byArrival.size();
        while (finished < n) {
            while (next < n && byArrival.get(next).getArrival() <= t) ready.add(byArrival.get(next++));
            if (ready.isEmpty()) {
                long na = byArrival.get(next).getArrival();
                addSegment(timeline, null, t, na);
                t = na;
                continue;
            }
            PCB best = ready.get(0);
            for (PCB p : ready) if (better(p, best, remaining)) best = p;
            best.markStart(t);
            long run = remaining.get(best);
            if (next < n) run = Math.min(run, byArrival.get(next).getArrival() - t);
            addSegment(timeline, best, t, t + run);
            t += run;
            remaining.put(best, remaining.get(best) - run);
            if (remaining.get(best) == 0) { best.setCompletion(t); ready.remove(best); finished++; }
        }
    }

    private boolean better(PCB a, PCB b, Map<PCB, Long> remaining) {
        int c = Long.compare(remaining.get(a), remaining.get(b));
        if (c == 0) c = Long.compare(a.getArrival(), b.getArrival());
        if (c == 0) c = Integer.compare(a.getId(), b.getId());
        return c < 0;
    }
}
