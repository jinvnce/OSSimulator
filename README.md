# OS Simulator

A Java Swing desktop application that simulates the core algorithms of an Operating Systems course. Eight modules are available from a single sidebar. Each module accepts a variable amount of user input, validates it, runs the algorithm, and shows the results as tables, charts, and step-by-step traces.

## Table of Contents

1. [Features](#features)
2. [Requirements](#requirements)
3. [How to Run](#how-to-run)
4. [Project Structure](#project-structure)
5. [Module Guide](#module-guide)
6. [Input Validation and Dynamic Input](#input-validation-and-dynamic-input)
7. [Object-Oriented Design](#object-oriented-design)
8. [Sample Test Cases](#sample-test-cases)
9. [Known Limitations](#known-limitations)

---

## Features

| # | Module | Algorithms / Functions |
|---|--------|------------------------|
| 1 | CPU Scheduling | FCFS, SJF, SRT, Round Robin, Priority (preemptive and non-preemptive) |
| 2 | Banker's Algorithm | Need matrix, safety check, safe sequence with a step trace |
| 3 | Paging | Page table, logical-to-physical address translation, page-fault detection |
| 4 | Page Replacement | FIFO, LRU, Optimal, NFU, NRU |
| 5 | Segmentation | Segment table, base/limit validation, overlap detection |
| 6 | Fragmentation | First/Best/Worst Fit, fixed and dynamic partitions, internal and external fragmentation |
| 7 | Disk Scheduling | FIFO, SSTF, LOOK, Elevator, C-SCAN |
| 8 | Multiprogramming | Multiple jobs sharing the CPU, with a timeline and utilization summary |

Every module also has a **Randomize** button for generating small or large datasets quickly.

---

## Requirements

- **JDK 20** (the version the IDE project is configured for). A newer JDK should also work.
- No external libraries. The app uses only the Java standard library (Swing/AWT).
- The `assets/` folder (images, icons, fonts) must be present. The app looks for it on the classpath or in the working directory.

---

## How to Run

### Option A: IntelliJ IDEA (easiest)

1. Open the project folder in IntelliJ IDEA.
2. Make sure the Project SDK is set to JDK 20 or newer (*File > Project Structure*).
3. Open `src/ossim/OSSimulator.java` and click the green run arrow next to `main`.

### Option B: Command line

Run these from the **project root**, which is the folder that contains `src/` and `assets/`:

```bash
# Linux / macOS
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out ossim.OSSimulator
```

```bat
:: Windows (Command Prompt)
mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
java -cp out ossim.OSSimulator
```

> **Important:** run from the project root so the `assets/` folder is found. If images or fonts are missing, the UI may look plain or fall back to default fonts.

---

## Project Structure

```
OSSimulator/
├── assets/                  UI resources (backgrounds, buttons, icons, fonts, logos)
└── src/
    ├── ossim/               OSSimulator.java: main window and sidebar navigation
    ├── ui/                  Shared UI: Theme, ModulePanel, ModuleHeader, Icons
    ├── util/                Util (parsing helpers), TimeFmt (time formatting)
    ├── cpu/                 CPU scheduling
    ├── banker/              Banker's algorithm
    ├── paging/              Paging and address translation
    ├── replacement/         Page replacement algorithms
    ├── segmentation/        Segment table and address translation
    ├── fragmentation/       Memory allocation and fragmentation
    ├── disk/                Disk scheduling
    └── multiprogramming/    Multiprogramming simulation
```

Most modules follow the same layout: **logic classes** (the algorithm) plus a `*Panel` class (the Swing UI) and sometimes a custom drawing component (Gantt chart, memory diagram, disk chart).

---

## Module Guide

### 1. CPU Scheduling (`cpu/`)

Add any number of jobs in the input table. The table has columns **Job**, **Arrival (AT)**, **Burst (BT)**, and **Priority**. Priority is used only by Priority scheduling, where a **lower number runs first**.

- Choose an algorithm. Round Robin also needs a **time quantum**.
- Output: a **Gantt chart**, plus per-job **completion, turnaround, and waiting time**, and averages.
- Idle gaps are shown on the chart when no job has arrived yet.
- The output unit can be switched (clock time, seconds, minutes, hours), and the number of decimal places can be set.

### 2. Banker's Algorithm (`banker/`)

Set the number of **processes** and **resource types**, then fill in the **Allocation** and **Max** tables and the system's **Total** resources.

- Available is computed as `Total − sum of Allocation`.
- Need is computed as `Need = Max − Allocation`.
- Output: the **Need matrix**, a **step-by-step safety trace**, and whether the system is **safe**. If it is safe, the **safe sequence** is shown. If not, the stuck processes are listed.
- Rows (processes) and columns (resources) can be added or deleted.

### 3. Paging (`paging/`)

Enter the **page size**, the **number of frames**, and edit the **page table** (the Frame column; leave it blank for "not in memory"). Then enter **logical addresses**.

- Output: page number, offset, frame, and physical address (`frame × page size + offset`), with the calculation steps shown.
- A page that is not in memory is reported as a **page fault**.
- A memory diagram shows which frames hold which pages.

### 4. Page Replacement (`replacement/`)

Enter a **page reference string** (numbers separated by spaces or commas) and the **number of frames**.

- Algorithms: **FIFO, LRU, Optimal, NFU, NRU**.
- Output: a grid showing the frame contents at every step, **hits and faults** marked, and the total fault count and hit ratio.

### 5. Segmentation (`segmentation/`)

Define any number of segments with a **base** and **limit** (double-click a cell to edit), and set the memory size. Enter addresses as `segment:offset`, for example `2:53 3:852 0:1222`.

- A valid address shows `physical = base + offset`.
- An invalid address (the segment does not exist, or `offset >= limit`) is reported as an **addressing error / trap**.
- Overlapping segments are detected, and a diagram shows the layout.

### 6. Fragmentation (`fragmentation/`)

Enter the **memory block sizes** and the **process sizes** (in KB, MB, or GB).

- Strategy: **First Fit, Best Fit, Worst Fit**.
- Memory type: **Fixed** partitions (one process per block, so wasted space inside a block is *internal fragmentation*) or **Dynamic** partitions (a block is split, so leftover pieces are free).
- Output: where each process was placed, which processes are waiting, and whether a waiting process is blocked by **external fragmentation** (enough total free memory but no single block big enough) or simply by **not enough memory**.

### 7. Disk Scheduling (`disk/`)

Enter the **request queue** (cylinders in arrival order), the **initial head position**, and the **maximum cylinder**.

- Algorithms: **FIFO, SSTF, LOOK, Elevator, C-SCAN**.
- Output: the service order, **head movement** for each seek, total movement, and average seek. A chart plots the head path, and a table compares all algorithms.

### 8. Multiprogramming (`multiprogramming/`)

Enter any number of jobs with **arrival** and **burst** times. The simulation models several jobs sharing the CPU at the same time, with each active job receiving an equal share.

- Output: an event-by-event simulation table, a timeline log, and a summary (average turnaround, average waiting, CPU utilization).

---

## Input Validation and Dynamic Input

There is **no fixed limit** on the number of processes, requests, frames, blocks, or segments. Rows and entries can be added freely, and the practical limit is the computer's memory. The only built-in cap is **200 jobs for the Randomize button** in CPU Scheduling.

The program validates input and shows a clear message instead of crashing for:

- Blank fields
- Non-numeric input (letters, symbols, decimals where a whole number is required)
- Negative values where they are not allowed (for example, a negative burst time or base)
- Zero or negative counts (frames, page size, quantum, limits)
- Inconsistent data, for example:
  - Banker's: Max is less than Allocation
  - Disk: a cylinder or head position greater than the maximum cylinder
  - Paging: a page mapped to a frame that doesn't exist, or two pages sharing one frame
  - Segmentation: a limit below 1, or segments that don't fit in memory
  - CPU: a process with burst time below 1 or arrival time below 0

---

## Object-Oriented Design

| Concept | Where it is used |
|---------|------------------|
| **Abstraction** | `Scheduler`, `NonPreemptiveScheduler`, `PageReplacement`, `DiskScheduler`, and `ModulePanel` define the contract and hide the details. |
| **Inheritance** | `FCFSScheduler` and `SJFScheduler` extend `NonPreemptiveScheduler`, which extends `Scheduler`. `FIFO/LRU/Optimal/NFU/NRU` extend `PageReplacement`. The disk algorithms extend `DiskScheduler`. All panels extend `ModulePanel`. |
| **Polymorphism** | Algorithm objects are stored in a `JComboBox` and called through their base type, so the UI doesn't care which algorithm is selected. |
| **Template Method pattern** | `Scheduler.schedule()` and `PageReplacement.simulate()` are `final` and fix the overall flow. Subclasses supply only the algorithm-specific steps (`execute`, `chooseVictim`, `onHit`, `onLoad`). |
| **Encapsulation** | Data classes such as `PCB`, `PageTable`, `SegmentTable`, and `BankersAlgorithm` keep their fields private. They validate input in constructors and expose only getters. |
| **Enums** | `MemoryAllocator.Strategy` and `MemoryAllocator.Kind` hold the allocation strategy names and descriptions. |
| **Immutable result objects** | `PagingResult`, `DiskResult`, `SegmentTable.Result`, `MemoryAllocator.Result` and similar classes carry results back to the UI. |
| **Separation of concerns** | Algorithm classes are independent of Swing, and panels only collect input and display output. |

---

## Sample Test Cases

These are classic textbook cases with results worked out by hand. Use them to verify each module, then use **Randomize** with large values to test bigger datasets.

### CPU Scheduling: SJF (non-preemptive)

| Job | AT | BT |
|-----|----|----|
| J1 | 0 | 7 |
| J2 | 2 | 4 |
| J3 | 4 | 1 |
| J4 | 5 | 4 |

Expected execution order: **J1, J3, J2, J4**. Completion times: 7, 12, 8, 16. Turnaround times: 7, 10, 4, 11. Waiting times: 0, 6, 3, 7. Average waiting time = **4.0**. Average turnaround time = **8.0**.

### Banker's Algorithm

- Processes: 5. Resource types: 3. Total resources: `10 5 7`.
- Allocation: P0 `0 1 0`, P1 `2 0 0`, P2 `3 0 2`, P3 `2 1 1`, P4 `0 0 2`.
- Max: P0 `7 5 3`, P1 `3 2 2`, P2 `9 0 2`, P3 `2 2 2`, P4 `4 3 3`.

Expected: Available = `3 3 2`. The system is **safe**, with safe sequence **P1 → P3 → P4 → P0 → P2**.

To test an unsafe state, raise P0's Max or lower Total so that no process's Need fits in Available.

### Paging

Page size `4`, frames `4`, page table `0→5, 1→6, 2→1, 3→2`. Logical address `13`: page 3, offset 1, frame 2, physical address **9**.

### Page Replacement

Reference string `7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1` with **3 frames**.

| Algorithm | Expected page faults |
|-----------|----------------------|
| FIFO | 15 |
| LRU | 12 |
| Optimal | 9 |

### Segmentation

Segments (base, limit): S0 `(219, 600)`, S1 `(2300, 14)`, S2 `(90, 100)`, S3 `(1327, 580)`, S4 `(1952, 96)`. Addresses `2:53 3:852 0:1222`:

| Address | Result |
|---------|--------|
| 2:53 | Valid, physical **143** |
| 3:852 | Valid, physical **2179** |
| 0:1222 | **Trap** (offset 1222 is not less than limit 600) |

### Fragmentation

Blocks `100 500 200 300 600`, processes `212 417 112 426`, **Fixed** partitions.

| Strategy | Expected |
|----------|----------|
| First Fit | 212→B2, 417→B5, 112→B3, **426 waits** (free memory 400 KB is less than 426 KB, so not fragmentation) |
| Best Fit | 212→B4, 417→B2, 112→B3, 426→B5 (**all placed**) |
| Worst Fit | 212→B5, 417→B2, 112→B4, **426 waits** |

### Disk Scheduling

Queue `98 183 37 122 14 124 65 67`, head `53`, max `199`.

| Algorithm | Expected total head movement |
|-----------|------------------------------|
| FIFO | **640** |
| SSTF | **236** (order: 65, 67, 37, 14, 98, 122, 124, 183) |

### Invalid-Input Checks (every module)

Try: an empty field, letters, a negative number, `0` where at least 1 is required, and an inconsistent entry (such as Banker's Max less than Allocation). Each should produce an error message, not a crash.

### Large-Dataset Checks

Use **Randomize** with big values. For example, generate 200 CPU jobs, a long page reference string with a few frames, or hundreds of disk requests. Check that the program stays responsive and that the totals are consistent.

---

## Known Limitations

- **C-SCAN** does not currently sweep to cylinder 0 when it wraps around, so its head-movement totals can differ from textbook C-SCAN.
- **Elevator** and **LOOK** use the same logic and produce identical results. A full SCAN (which travels to the disk end) is not included.
- **Priority** scheduling is implemented in a separate helper (`PriorityRunner`) instead of as a `Scheduler` subclass, so it doesn't share the other algorithms' class hierarchy.
- **Multiprogramming** logic lives inside its panel class instead of a separate logic class.

---

## Credits

Course project for Operating Systems. UI artwork and fonts are in `assets/` (see `assets/fonts/FONT_NOTES.txt` for font licensing notes).
