/** One block of the Gantt chart. A null process means the CPU was idle. */
public class Segment {
    private final PCB process;
    private final long start;
    private long end;

    public Segment(PCB process, long start, long end) { this.process = process; this.start = start; this.end = end; }

    public PCB getProcess() { return process; }
    public long getStart() { return start; }
    public long getEnd() { return end; }
    public void setEnd(long end) { this.end = end; }
    public boolean isIdle() { return process == null; }
    public String getLabel() { return isIdle() ? "Idle" : process.getLabel(); }
}
