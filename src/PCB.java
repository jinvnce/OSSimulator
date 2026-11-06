/**
 * Process Control Block: one job/process with encapsulated data and computed statistics.
 * Times are exact whole numbers in "fixed point": a time of 2.5 with 1 decimal place is stored as 25.
 * This keeps decimals exact (no 0.1 + 0.2 errors) and allows very large values.
 */
public class PCB {
    private final int id;
    private final long arrival, burst;
    private long completion;
    private long start = -1;   // time the job first gets the CPU (-1 = not started yet)

    public PCB(int id, long arrival, long burst) {
        if (arrival < 0 || burst < 1) throw new IllegalArgumentException("Invalid process values.");
        this.id = id; this.arrival = arrival; this.burst = burst;
    }

    public int getId() { return id; }
    public long getArrival() { return arrival; }
    public long getBurst() { return burst; }
    public long getCompletion() { return completion; }
    public long getStart() { return start; }
    /** Records the start time only the first time the job runs (matters for preemptive algorithms). */
    public void markStart(long time) { if (start < 0) start = time; }
    public void setCompletion(long completion) { this.completion = completion; }

    public long getTurnaround() { return completion - arrival; }
    public long getWaiting() { return getTurnaround() - burst; }
    public String getLabel() { return "J" + (id + 1); }
}
