public class FCFSScheduler extends NonPreemptiveScheduler {
    @Override public String getName() { return "FCFS"; }
    @Override public String getDescription() {
        return "First-Come First-Served: jobs run in the order they arrive. A running job is never interrupted.";
    }
    @Override protected int compare(PCB a, PCB b) { return 0; }
}
