/** Shortest Job First (preemptive), also called Shortest Remaining Time First: least remaining burst runs. */
public class SRTFScheduler extends PreemptiveScheduler {
    @Override public String getName() { return "SJF (Preemptive)"; }
    @Override protected int key(PCB p, int remaining) { return remaining; }
}
