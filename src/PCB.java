/** Process Control Block: one job/process with encapsulated data and computed statistics. */
public class PCB {
    private final int id;
    private final int arrival, burst, priority;
    private int completion;
    private int start = -1;   // time the job first gets the CPU (-1 = not started yet)

    public PCB(int id, int arrival, int burst, int priority) {
        if (arrival < 0 || burst < 1 || priority < 0) throw new IllegalArgumentException("Invalid process values.");
        this.id = id; this.arrival = arrival; this.burst = burst; this.priority = priority;
    }

    public int getId() { return id; }
    public int getArrival() { return arrival; }
    public int getBurst() { return burst; }
    public int getPriority() { return priority; }
    public int getCompletion() { return completion; }
    public int getStart() { return start; }
    /** Records the start time only the first time the job runs (matters for Round Robin). */
    public void markStart(int time) { if (start < 0) start = time; }
    public void setCompletion(int completion) { this.completion = completion; }

    public int getTurnaround() { return completion - arrival; }
    public int getWaiting() { return getTurnaround() - burst; }
    public String getLabel() { return "J" + (id + 1); }
}
