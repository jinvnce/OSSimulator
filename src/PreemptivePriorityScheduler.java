/** Priority scheduling (preemptive): a newly arrived job with a smaller priority number takes the CPU. */
public class PreemptivePriorityScheduler extends PreemptiveScheduler {
    @Override public String getName() { return "Priority (Preemptive)"; }
    @Override protected int key(PCB p, int remaining) { return p.getPriority(); }
}
