import java.util.*;

/**
 * Abstract base class for CPU scheduling algorithms.
 * schedule() is a template method: it fixes the overall steps and lets each subclass
 * supply the algorithm in execute() (inheritance + polymorphism).
 */
public abstract class Scheduler {
    public abstract String getName();

    /** One or two plain sentences that explain the algorithm on screen. */
    public abstract String getDescription();

    /** True if a running job can be taken off the CPU before it finishes. */
    public abstract boolean isPreemptive();

    /** Runs the algorithm: must append Gantt segments and call setCompletion() on every process. */
    protected abstract void execute(List<PCB> processes, List<Segment> timeline);

    public final List<Segment> schedule(List<PCB> processes) {
        List<Segment> timeline = new ArrayList<>();
        if (!processes.isEmpty()) execute(processes, timeline);
        return timeline;
    }

    /** Adds a segment, merging it with the previous one if the same process keeps running. */
    protected void addSegment(List<Segment> timeline, PCB p, long start, long end) {
        if (!timeline.isEmpty()) {
            Segment last = timeline.get(timeline.size() - 1);
            if (last.getProcess() == p && last.getEnd() == start) { last.setEnd(end); return; }
        }
        timeline.add(new Segment(p, start, end));
    }

    protected List<PCB> sortedByArrival(List<PCB> ps) {
        List<PCB> sorted = new ArrayList<>(ps);
        sorted.sort(Comparator.comparingLong(PCB::getArrival).thenComparingInt(PCB::getId));
        return sorted;
    }

    @Override public String toString() { return getName(); }
}
