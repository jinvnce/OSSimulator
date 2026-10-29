/** Shortest Job First (non-preemptive): smallest burst time runs first. */
public class SJFScheduler extends NonPreemptiveScheduler {
    @Override public String getName() { return "SJF"; }
    @Override protected int compare(PCB a, PCB b) { return Integer.compare(a.getBurst(), b.getBurst()); }
}
