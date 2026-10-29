/** One block of the Gantt chart. A null process means the CPU was idle. */
public class Segment {
    private final PCB process;
    private final int start;
    private int end;

    public Segment(PCB process, int start, int end) { this.process = process; this.start = start; this.end = end; }

    public PCB getProcess() { return process; }
    public int getStart() { return start; }
    public int getEnd() { return end; }
    public void setEnd(int end) { this.end = end; }
    public boolean isIdle() { return process == null; }
    public String getLabel() { return isIdle() ? "Idle" : process.getLabel(); }
}
