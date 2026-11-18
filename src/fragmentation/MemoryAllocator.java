package fragmentation;

import java.util.ArrayList;
import java.util.List;


public final class MemoryAllocator {
    private MemoryAllocator() {
    }

    public enum Strategy {
        FIRST_FIT("First Fit", "Take the first block (from the top) that is big enough."),
        BEST_FIT("Best Fit", "Take the smallest block that is big enough (leaves the smallest leftover)."),
        WORST_FIT("Worst Fit", "Take the biggest block (leaves the biggest leftover).");

        private final String name, description;

        Strategy(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String description() {
            return description;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Kind {PROCESS, INTERNAL, FREE}

    public static final class Cell {
        public final Kind kind;
        public final int size, process;

        Cell(Kind kind, int size, int process) {
            this.kind = kind;
            this.size = size;
            this.process = process;
        }
    }

    public static final class Placement {
        public final int process, size;
        public final int block;
        public final int internal;
        public final String note;
        public final boolean external;

        Placement(int process, int size, int block, int internal, String note, boolean external) {
            this.process = process;
            this.size = size;
            this.block = block;
            this.internal = internal;
            this.note = note;
            this.external = external;
        }

        public boolean allocated() {
            return block >= 0;
        }
    }

    public static final class Result {
        public final Strategy strategy;
        public final boolean fixed;
        public final int[] blocks;
        public final List<Placement> placements = new ArrayList<>();
        public final List<List<Cell>> cells = new ArrayList<>();
        public int totalMemory, allocatedMemory, internalFragmentation, freeMemory, largestFree, externalFragmentation, waiting;

        Result(Strategy strategy, boolean fixed, int[] blocks) {
            this.strategy = strategy;
            this.fixed = fixed;
            this.blocks = blocks;
        }
    }


    public static Result run(int[] blocks, int[] procs, Strategy strategy, boolean fixed) {
        Result r = new Result(strategy, fixed, blocks.clone());
        int nb = blocks.length;
        int[] room = blocks.clone();
        List<List<Cell>> placed = new ArrayList<>();
        for (int b = 0; b < nb; b++) {
            placed.add(new ArrayList<>());
            r.totalMemory += blocks[b];
        }
        int[] usedInternal = new int[nb];

        for (int p = 0; p < procs.length; p++) {
            int size = procs[p], pick = -1;
            for (int b = 0; b < nb; b++) {
                if (room[b] < size) continue;
                if (pick < 0) {
                    pick = b;
                    if (strategy == Strategy.FIRST_FIT) break;
                    continue;
                }
                if (strategy == Strategy.BEST_FIT && room[b] < room[pick]) pick = b;
                if (strategy == Strategy.WORST_FIT && room[b] > room[pick]) pick = b;
            }
            if (pick < 0) {
                r.placements.add(new Placement(p, size, -1, 0, null, false));
                continue;
            }
            placed.get(pick).add(new Cell(Kind.PROCESS, size, p));
            int internal = 0;
            String note;
            if (fixed) {
                internal = blocks[pick] - size;
                usedInternal[pick] = internal;
                room[pick] = 0;
                note = "Block " + (pick + 1) + " (" + blocks[pick] + " KB)" + (internal > 0 ? ": " + internal + " KB wasted inside the block (internal fragmentation)" : ": a perfect fit");
            } else {
                room[pick] -= size;
                note = "Block " + (pick + 1) + ": the block is split and " + room[pick] + " KB stays free";
            }
            r.allocatedMemory += size;
            r.internalFragmentation += internal;
            r.placements.add(new Placement(p, size, pick, internal, note, false));
        }

        for (int b = 0; b < nb; b++) {
            List<Cell> row = new ArrayList<>(placed.get(b));
            if (fixed) {
                if (usedInternal[b] > 0) row.add(new Cell(Kind.INTERNAL, usedInternal[b], -1));
                if (placed.get(b).isEmpty()) row.add(new Cell(Kind.FREE, blocks[b], -1));
            } else if (room[b] > 0) row.add(new Cell(Kind.FREE, room[b], -1));
            r.cells.add(row);
            boolean freeBlock = fixed ? placed.get(b).isEmpty() : room[b] > 0;
            if (freeBlock) {
                int free = fixed ? blocks[b] : room[b];
                r.freeMemory += free;
                r.largestFree = Math.max(r.largestFree, free);
            }
        }

        List<Placement> fixedUp = new ArrayList<>();
        boolean external = false;
        for (Placement pl : r.placements) {
            if (pl.allocated()) {
                fixedUp.add(pl);
                continue;
            }
            r.waiting++;
            String note;
            boolean ext = pl.size <= r.freeMemory;
            if (ext) {
                external = true;
                note = "Needs " + pl.size + " KB. " + r.freeMemory + " KB is free in total, but the biggest piece is only " + r.largestFree + " KB: external fragmentation";
            } else {
                note = "Needs " + pl.size + " KB but only " + r.freeMemory + " KB is free in total: not enough memory (not fragmentation)";
            }
            fixedUp.add(new Placement(pl.process, pl.size, -1, 0, note, ext));
        }
        r.placements.clear();
        r.placements.addAll(fixedUp);
        r.externalFragmentation = external ? r.freeMemory : 0;
        return r;
    }
}
