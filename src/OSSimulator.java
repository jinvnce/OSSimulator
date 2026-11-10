import javax.swing.*;
/**
 * OS Simulator - CPU Scheduling, Banker's Algorithm, Paging (Java Swing, object-oriented design)
 *
 * HOW TO RUN:   javac *.java      then      java OSSimulator
 *
 * HOW TO USE
 *   CPU Scheduling : choose an algorithm, type a Size and click Randomize (or add/edit rows), click Run. The result is drawn like the lecture table: reference string on top, one row per frame
 *                      (the page just loaded is red), an F / H row, then Total Page faults and Total Hits.
 *                    Results table columns: J#, AT, BT, ST, FT, WT, TAT, plus AWT and ATAT (the averages).
 *                    The animated Gantt chart shows the word Idle whenever the CPU has no job to run.
 *                    Algorithms: FCFS and SJF (non-preemptive), SRT = Shortest Remaining Time and Round Robin
 *                    (preemptive). The Type label shows which kind is selected. Round Robin uses the time quantum.
 *                    Times can be big and can have decimals (up to 6 decimal places). Random jobs use Max value
 *                    and the Decimals box. Add row fills the new job with random AT and BT.
 *                    Time unit: pick the unit you type the times in (seconds / minutes / hours), then Convert to: clock time
 *                    H:MM:SS (3725 s = 1:02:05) or seconds / minutes / hours (90 s = 1.50 min). Decimals sets the decimal places.
 *                    The Multiprogramming tab has the same From / Convert to choices.
 *   Banker's       : enter processes and resource types (resources are named A, B, C ...), Generate Tables,
 *                    Randomize or Load Sample (the slide example), type the Total of each resource and edit values (Available = Total - sum of Allocation, on the P0 row),
 *                    click Check Safety. One table shows
 *                    Allocation, Max, Available and Need. Skipped processes are listed again in extra rows when the
 *                    check goes back around, and the Result line gives the safe sequence or says NOT SAFE.
 *   Paging         : set page size, number of pages and frames, then Generate Table, Randomize or Load Sample
 *                    (the lecture example). Edit the Frame column of the page table, enter logical addresses
 *                    and click Translate (or Translate All). The memory diagram shows the page -> frame mapping.
 *   Page Replacement : enter a page reference string (or Randomize), set frames and FIFO / LRU / Optimal, click Run.
 *                      The result is drawn like the lecture table: reference string on top, one row per frame
 *                      (the page just loaded is red), an F / H row (F red, H black), then Total Page faults and Total Hits.
 *   Multiprogramming : jobs have CPU burst 1, I/O burst, CPU burst 2. Compares uniprogramming (CPU idle during I/O) with
 *                      multiprogramming (another job uses the CPU during I/O): totals, averages and CPU utilization.
 *   Disk Scheduling  : enter cylinder requests, head start and last cylinder, pick FIFO / DDTF / LOOK / Elevator (SCAN) / C-SCAN
 *                      and click Run for each seek, the total head movement and the average per seek. Compare All shows every algorithm.
 *
 * OOP DESIGN
 *   Abstraction / Inheritance : Scheduler (abstract) -> NonPreemptiveScheduler -> FCFS, SJF;
 *                               Scheduler -> SRTScheduler, RoundRobinScheduler.
 *                               PageReplacement (abstract) -> FIFO, LRU, Optimal.
 *                               DiskScheduler (abstract) -> FIFODisk, DDTFDisk, LookDisk, ElevatorDisk, CScanDisk.
 *                               ModulePanel (abstract) -> CpuPanel, BankerPanel, PagingPanel, ReplacementPanel.
 *   Polymorphism              : scheduler.schedule(...) and pageAlg.simulate(...) run whichever algorithm is selected.
 *   Encapsulation             : PCB, BankersAlgorithm, PageTable, PagingResult keep data private and expose getters.
 *   Template Method pattern   : Scheduler.schedule() and PageReplacement.simulate() fix the steps;
 *                               subclasses supply only the policy.
 *
 * ALGORITHMS
 *   FCFS / SJF            : PriorityQueue ready queue, O(n log n). Ties break by arrival, then PID.
 *   Round Robin           : ArrayDeque queue; each process runs up to one quantum, then re-queues.
 *   Banker's Safety       : Need = Max - Allocation. Work = Available. Find an unfinished Pi with Need <= Work,
 *                           Work += Allocation[i], mark finished; repeat. All finished = safe sequence.
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class OSSimulator extends JFrame {
    private static final String[] ICONS = {"cpu", "diamond", "club", "heart", "memory", "gear"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OSSimulator().setVisible(true));
    }

    public OSSimulator() {
        super("OS Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ModulePanel[] modules = {new CpuPanel(), new BankerPanel(), new PagingPanel(), new ReplacementPanel(),
                new MultiprogrammingPanel(), new DiskPanel()};

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) { Theme.paintBackground(g, this); }
        };

        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);
        content.setOpaque(false);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 8));
        side.setPreferredSize(new Dimension(250, 100));
        side.add(logo());
        side.add(Box.createVerticalStrut(14));

        List<Theme.SkinButton> buttons = new ArrayList<>();
        for (int i = 0; i < modules.length; i++) {
            ModulePanel m = modules[i];
            content.add(m, m.getTitle());
            Theme.SkinButton b = new Theme.SkinButton(m.getTitle(), "navy");
            b.setIcon(Theme.icon(ICONS[i % ICONS.length], 22, Theme.CREAM));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMargin(new Insets(4, 16, 8, 10));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setPreferredSize(new Dimension(226, 50));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            b.addActionListener(e -> {
                for (Theme.SkinButton o : buttons) o.setKind(o == b ? "red" : "navy");
                cards.show(content, m.getTitle());
            });
            buttons.add(b);
            side.add(b);
            side.add(Box.createVerticalStrut(6));
        }
        side.add(Box.createVerticalGlue());
        buttons.get(0).setKind("red");

        JPanel divider = new JPanel();
        divider.setBackground(new Color(0x050D18));
        divider.setPreferredSize(new Dimension(2, 10));

        JPanel west = new JPanel(new BorderLayout());
        west.setOpaque(false);
        west.add(side, BorderLayout.CENTER);
        west.add(divider, BorderLayout.EAST);

        root.add(west, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
    }

    private JComponent logo() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        BufferedImage img = Theme.image("logos/os_simulator_jester_card_logo.png");
        if (img != null) p.add(new JLabel(new ImageIcon(img.getScaledInstance(56, 56, Image.SCALE_SMOOTH))));
        JLabel name = new JLabel("<html>OS &nbsp;&#8226;<br>SIMULATOR</html>");
        name.setFont(Theme.heading(18f));
        name.setForeground(Theme.CREAM);
        p.add(name);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        return p;
    }
}
