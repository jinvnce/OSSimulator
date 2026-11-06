import java.util.*;


public abstract class Scheduler {
    public abstract String getName();

    public abstract String getDescription();

    public abstract boolean isPreemptive();

    protected abstract void execute(List<PCB> processes, List<Segment> timeline);

    public final List<Segment> schedule(List<PCB> processes) {
        List<Segment> timeline = new ArrayList<>();
        if (!processes.isEmpty()) execute(processes, timeline);
        return timeline;
    }

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
