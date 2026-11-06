public class FIFOReplacement extends PageReplacement {
    private int next, size;
    @Override public String getName() { return "FIFO"; }
    @Override protected void init(int n) { next = 0; size = n; }
    @Override protected int chooseVictim(int now) { int v = next; next = (next + 1) % size; return v; }
}
