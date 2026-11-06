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

    @Override public String toString() { return getName(); }
}

class FIFODisk extends DiskScheduler {
    public String getName() { return "FIFO"; }
    public String getDescription() { return "Serves the requests in the order they arrived. Simple and fair, but the head can jump back and forth."; }
    protected List<Integer> order(int head, List<Integer> r, boolean up, int max) { return new ArrayList<>(r); }
}

class DDTFDisk extends DiskScheduler {
    public String getName() { return "DDTF"; }
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
    public String getDescription() { return "Moves in one direction serving requests until the last request in that direction, then reverses. It does not travel to the end of the disk."; }
    protected List<Integer> order(int head, List<Integer> r, boolean up, int max) {
        List<Integer> s = sorted(r), lo = new ArrayList<>(), hi = new ArrayList<>();
        for (int c : s) if (c < head) lo.add(c); else hi.add(c);
        Collections.reverse(lo);                       // below the head, nearest first
        List<Integer> out = new ArrayList<>();
        if (up) { out.addAll(hi); out.addAll(lo); } else { out.addAll(lo); out.addAll(hi); }
        return out;
    }
}

class ElevatorDisk extends DiskScheduler {
    public String getName() { return "Elevator (SCAN)"; }
    public String getDescription() { return "Like an elevator: moves one way to the very end of the disk, then reverses and serves the rest on the way back."; }
    protected List<Integer> order(int head, List<Integer> r, boolean up, int max) {
        List<Integer> s = sorted(r), lo = new ArrayList<>(), hi = new ArrayList<>();
        for (int c : s) if (c < head) lo.add(c); else hi.add(c);
        Collections.reverse(lo);
        List<Integer> out = new ArrayList<>();
        if (up) { out.addAll(hi); out.add(max); out.addAll(lo); }
        else { out.addAll(lo); out.add(0); out.addAll(hi); }
        return out;
    }
}

class CScanDisk extends DiskScheduler {
    public String getName() { return "C-SCAN"; }
    public String getDescription() { return "Circular SCAN: moves one way to the end of the disk, jumps back to the start, and keeps going the same way. The jump counts as head movement."; }
    protected List<Integer> order(int head, List<Integer> r, boolean up, int max) {
        List<Integer> s = sorted(r), lo = new ArrayList<>(), hi = new ArrayList<>();
        for (int c : s) if (c < head) lo.add(c); else hi.add(c);
        List<Integer> out = new ArrayList<>();
        if (up) { out.addAll(hi); out.add(max); out.add(0); out.addAll(lo); }
        else {
            List<Integer> loDesc = new ArrayList<>(lo); Collections.reverse(loDesc);
            List<Integer> hiDesc = new ArrayList<>(hi); Collections.reverse(hiDesc);
            out.addAll(loDesc); out.add(0); out.add(max); out.addAll(hiDesc);
        }
        return out;
    }
}
