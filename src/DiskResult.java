import java.util.*;

/** Result of one disk scheduling run: the head path and the movement per step (encapsulated, getters only). */
public class DiskResult {
    private final String name;
    private final List<Integer> path;
    private final int total;

    public DiskResult(String name, List<Integer> path) {
        this.name = name;
        this.path = Collections.unmodifiableList(new ArrayList<>(path));
        int t = 0;
        for (int i = 1; i < path.size(); i++) t += Math.abs(path.get(i) - path.get(i - 1));
        this.total = t;
    }

    public String getName() { return name; }
    public List<Integer> getPath() { return path; }
    public int getSteps() { return path.size() - 1; }
    public int getMove(int step) { return Math.abs(path.get(step + 1) - path.get(step)); }
    public int getTotal() { return total; }
    /** Average head movement per step (per seek). */
    public double getAverage() { return getSteps() == 0 ? 0 : (double) total / getSteps(); }
}
