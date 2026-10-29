/** First-Come First-Served: no extra criterion, so arrival order decides. */
public class FCFSScheduler extends NonPreemptiveScheduler {
    @Override public String getName() { return "FCFS"; }
    @Override protected int compare(PCB a, PCB b) { return 0; }
}
