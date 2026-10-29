import javax.swing.*;

/**
 * OS Simulator - CPU Scheduling, Banker's Algorithm, Paging (Java Swing, object-oriented design)
 *
 * HOW TO RUN:   javac *.java      then      java OSSimulator
 *
 * HOW TO USE
 *   CPU Scheduling : choose an algorithm, type a Size and click Randomize (or add/edit rows), click Run.
 *                    Results table columns: J#, AT, BT, ST, FT, WT, TAT, plus AWT and ATAT (the averages).
 *                    The animated Gantt chart shows the word Idle whenever the CPU has no job to run.
 *                    Round Robin uses the time quantum. For Priority, a smaller number = higher priority.
 *   Banker's       : enter processes and resource types (resources are named A, B, C ...), Generate Tables,
 *                    Randomize or Load Sample (the slide example), edit values, click Check Safety to see Need,
 *                    the step-by-step safety trace and the safe sequence. Use the Resource Request section to
 *                    test whether a request from one process can be granted immediately.
 *   Paging         : set page size, number of pages and frames, then Generate Table, Randomize or Load Sample
 *                    (the lecture example). Edit the Frame column of the page table, enter logical addresses
 *                    and click Translate (or Translate All). The memory diagram shows the page -> frame mapping.
 *   Page Replacement : enter a page reference string (or Randomize), set frames and FIFO / LRU / Optimal, click Run.
 *
 * OOP DESIGN
 *   Abstraction / Inheritance : Scheduler (abstract) -> NonPreemptiveScheduler -> FCFS, SJF, Priority;
 *                               Scheduler -> RoundRobinScheduler.
 *                               PageReplacement (abstract) -> FIFO, LRU, Optimal.
 *                               ModulePanel (abstract) -> CpuPanel, BankerPanel, PagingPanel, ReplacementPanel.
 *   Polymorphism              : scheduler.schedule(...) and pageAlg.simulate(...) run whichever algorithm is selected.
 *   Encapsulation             : PCB, BankersAlgorithm, PageTable, PagingResult keep data private and expose getters.
 *   Template Method pattern   : Scheduler.schedule() and PageReplacement.simulate() fix the steps;
 *                               subclasses supply only the policy.
 *
 * ALGORITHMS
 *   FCFS / SJF / Priority : PriorityQueue ready queue, O(n log n). Ties break by arrival, then PID.
 *   Round Robin           : ArrayDeque queue; each process runs up to one quantum, then re-queues.
 *   Banker's Safety       : Need = Max - Allocation. Work = Available. Find an unfinished Pi with Need <= Work,
 *                           Work += Allocation[i], mark finished; repeat. All finished = safe sequence.
 *   Banker's Request      : if Request <= Need and Request <= Available, pretend to allocate
 *                           (Available -= Request, Allocation += Request, Need -= Request) and run the safety
 *                           algorithm; grant only if the new state is safe, otherwise restore and make Pi wait.
 *   FIFO / LRU / Optimal  : FIFO evicts the oldest page, LRU the least recently used, Optimal the page
 *                           needed farthest in the future (next-use positions precomputed, O(n * frames)).
 *   Paging translation    : page = address / size, offset = address % size, frame = pageTable[page],
 *                           physical = frame * size + offset. A blank frame means the page is not in memory (page fault).
 *
 * SAMPLE
 *   FCFS: J1 (AT 0, BT 5), J2 (1, 3), J3 (2, 8) -> Gantt J1 0-5, J2 5-8, J3 8-16, AWT 3.33
 *   Page Replacement "7 0 1 2 0 3 0 4 2 3 0 3 2", 3 frames -> FIFO 10 faults, LRU 9, Optimal 7
 *   Page size 4, page table 5,6,1,2: logical 0 -> 20, 3 -> 23, 4 -> 24, 13 -> 9 (lecture example)
 */
public class OSSimulator extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OSSimulator().setVisible(true));
    }

    public OSSimulator() {
        super("OS Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ModulePanel[] modules = {new CpuPanel(), new BankerPanel(), new PagingPanel(), new ReplacementPanel()};
        JTabbedPane tabs = new JTabbedPane();
        for (ModulePanel m : modules) tabs.addTab(m.getTitle(), m);
        add(tabs);
        setSize(1050, 720);
        setLocationRelativeTo(null);
    }
}
