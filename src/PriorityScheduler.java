/** Priority scheduling (non-preemptive): the smaller priority number runs first. */
public class PriorityScheduler extends NonPreemptiveScheduler {
    @Override public String getName() { return "Priority"; }
    @Override protected int compare(PCB a, PCB b) { return Integer.compare(a.getPriority(), b.getPriority()); }
}
