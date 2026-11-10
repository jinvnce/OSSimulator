import java.util.*;

public abstract class DiskScheduler {
    public abstract String getName();
    public abstract String getDescription();

    protected abstract List<Integer> order(int head, List<Integer> requests, boolean up, int maxCyl);

    public final DiskResult schedule(int head, List<Integer> requests, boolean up, int maxCyl) {
        List<Integer> path = new ArrayList<>();
        path.add(head);
        for (int c : order(head, requests, up, maxCyl)) path.add(c);
        return new DiskResult(getName(), path);
    }

    protected static List<Integer> sorted(List<Integer> r) {
        List<Integer> s = new ArrayList<>(r);
        Collections.sort(s);
        return s;
    }

    // Helper method: determines proximity to decide starting direction
    protected boolean determineDirection(int head, List<Integer> lo, List<Integer> hi) {
        if (lo.isEmpty()) return true;  // Go UP if no requests below
        if (hi.isEmpty()) return false; // Go DOWN if no requests above
        int closestLo = lo.get(lo.size() - 1);
        int closestHi = hi.get(0);
        return (closestHi - head) < (head - closestLo);
    }

    @Override public String toString() { return getName(); }
}

class FIFODisk extends DiskScheduler {
    public String getName() { return "FIFO"; }
    public String getDescription() { return ""; }

    protected List<Integer> order(int head, List<Integer> r, boolean up, int max) {
        return new ArrayList<>(r);
    }
}

class DDTFDisk extends DiskScheduler {
    public String getName() { return "SSTF"; }
    public String getDescription() { return "Always serves the pending request closest to the head (shortest distance first). Fewer moves, but far requests can starve."; }

    protected List<Integer> order(int head, List<Integer> r, boolean up, int max) {
        List<Integer> left = new ArrayList<>(r), out = new ArrayList<>();
        int pos = head;
        while (!left.isEmpty()) {
            int best = 0;
            for (int i = 1; i < left.size(); i++)
                if (Math.abs(left.get(i) - pos) < Math.abs(left.get(best) - pos)) best = i;   // ties: earliest arrival
            pos = left.remove(best);
            out.add(pos);
        }
        return out;
    }
}

class LookDisk extends DiskScheduler {
    public String getName() { return "LOOK"; }
    public String getDescription() { return "Moves toward the closest request first, services to the lowest/highest request, then reverses without hitting endpoints."; }

    protected List<Integer> order(int head, List<Integer> r, boolean up, int max) {
        List<Integer> s = sorted(r), lo = new ArrayList<>(), hi = new ArrayList<>();
        for (int c : s) if (c < head) lo.add(c); else hi.add(c);

        boolean goUp = determineDirection(head, lo, hi);

        List<Integer> loDesc = new ArrayList<>(lo); Collections.reverse(loDesc);
        List<Integer> out = new ArrayList<>();

        if (goUp) {
            out.addAll(hi);
            out.addAll(loDesc);
        } else {
            out.addAll(loDesc);
            out.addAll(hi);
        }
        return out;
    }
}

class ElevatorDisk extends DiskScheduler {
    public String getName() { return "Elevator"; }
    public String getDescription() { return "Moves toward the closest request first, services to the lowest/highest request, then reverses."; }

    protected List<Integer> order(int head, List<Integer> r, boolean up, int max) {
        List<Integer> s = sorted(r), lo = new ArrayList<>(), hi = new ArrayList<>();
        for (int c : s) if (c < head) lo.add(c); else hi.add(c);

        boolean goUp = determineDirection(head, lo, hi);

        List<Integer> loDesc = new ArrayList<>(lo); Collections.reverse(loDesc);
        List<Integer> out = new ArrayList<>();

        if (goUp) {
            out.addAll(hi);
            out.addAll(loDesc);
        } else {
            out.addAll(loDesc);
            out.addAll(hi);
        }
        return out;
    }
}

class CScanDisk extends DiskScheduler {
    public String getName() { return "C-SCAN"; }
    public String getDescription() { return "Circular SCAN: moves toward closest request first to max boundary, then wraps around in a circular direction."; }

    protected List<Integer> order(int head, List<Integer> r, boolean up, int max) {
        List<Integer> s = sorted(r), lo = new ArrayList<>(), hi = new ArrayList<>();
        for (int c : s) if (c < head) lo.add(c); else hi.add(c);

        boolean goUp = determineDirection(head, lo, hi);

        List<Integer> loDesc = new ArrayList<>(lo); Collections.reverse(loDesc);
        List<Integer> hiDesc = new ArrayList<>(hi); Collections.reverse(hiDesc);
        List<Integer> out = new ArrayList<>();

        if (goUp) {
            out.addAll(hi);
            out.add(max);     // Only C-SCAN uses max boundary
            out.addAll(lo);
        } else {
            out.addAll(loDesc);
            out.add(max);     // Only C-SCAN uses max boundary
            out.addAll(hiDesc);
        }
        return out;
    }
}