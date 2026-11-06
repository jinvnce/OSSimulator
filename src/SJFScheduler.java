public class SJFScheduler extends NonPreemptiveScheduler {
    @Override public String getName() { return "SJF"; }
    @Override public String getDescription() {
        return "Shortest Job First: when the CPU is free, the waiting job with the smallest burst time runs next. A running job is never interrupted.";
    }
    @Override protected int compare(PCB a, PCB b) { return Long.compare(a.getBurst(), b.getBurst()); }
}
