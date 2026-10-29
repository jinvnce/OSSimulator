import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** CPU scheduling tab: process table, randomizer, results table and animated Gantt chart. */
public class CpuPanel extends ModulePanel {
    private final DefaultTableModel in = new DefaultTableModel(new String[]{"J#", "AT", "BT", "Priority (Priority algorithm only)"}, 0) {
        public boolean isCellEditable(int r, int c) { return c > 0; }
    };
    private final DefaultTableModel out = Util.readOnlyModel(
            new String[]{"J#", "AT", "BT", "ST", "FT", "WT", "TAT"});
    private final JTable inTable = new JTable(in);
    private final JComboBox<Scheduler> algo = new JComboBox<>(new Scheduler[]{
            new FCFSScheduler(), new SJFScheduler(), new PriorityScheduler(), new RoundRobinScheduler(2)});
    private final JTextField quantum = new JTextField("2", 4), size = new JTextField("5", 5);
    private final JLabel avg = new JLabel("AWT and ATAT will appear here after you click Run.");
    private final GanttChart gantt = new GanttChart();

    public CpuPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton rnd = new JButton("Randomize"), add = new JButton("Add row"), del = new JButton("Remove selected"),
                clr = new JButton("Clear"), run = new JButton("Run");
        top.add(new JLabel("Algorithm:")); top.add(algo);
        top.add(new JLabel("Time quantum:")); top.add(quantum);
        top.add(new JLabel("Size:")); top.add(size); top.add(rnd);
        top.add(add); top.add(del); top.add(clr); top.add(run);
        add(top, BorderLayout.NORTH);

        JSplitPane tables = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                Util.scroll(inTable, "Jobs (double-click a cell to edit)"), Util.scroll(new JTable(out), "Results (AT arrival, BT burst, ST start, FT finish, WT waiting, TAT turnaround)"));
        tables.setResizeWeight(0.4);
        JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tables, Util.scroll(gantt, "Gantt chart"));
        main.setResizeWeight(0.55);
        add(main, BorderLayout.CENTER);
        add(avg, BorderLayout.SOUTH);

        quantum.setEnabled(false);
        algo.addActionListener(e -> quantum.setEnabled(algo.getSelectedItem() instanceof RoundRobinScheduler));
        add.addActionListener(e -> { in.addRow(new Object[]{"", "0", "1", "1"}); renumber(); });
        del.addActionListener(e -> {
            Util.stopEditing(inTable);
            int[] rows = inTable.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) in.removeRow(rows[i]);
            renumber();
        });
        clr.addActionListener(e -> {
            Util.stopEditing(inTable);
            in.setRowCount(0); out.setRowCount(0); gantt.play(new ArrayList<>());
        });
        rnd.addActionListener(e -> randomizeData());
        run.addActionListener(e -> runSimulation());
    }

    @Override public String getTitle() { return "CPU Scheduling"; }

    private void renumber() { for (int i = 0; i < in.getRowCount(); i++) in.setValueAt("J" + (i + 1), i, 0); }

    @Override
    protected void randomizeData() {
        try {
            int n = Util.parseInt(size.getText(), "Size", 1);
            Util.stopEditing(inTable);
            in.setRowCount(0);
            for (int i = 0; i < n; i++)
                in.addRow(new Object[]{"", "" + Util.RNG.nextInt(Math.max(6, n)), "" + (1 + Util.RNG.nextInt(10)), "" + (1 + Util.RNG.nextInt(10))});
            renumber();
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
    }

    @Override
    protected void runSimulation() {
        Util.stopEditing(inTable);
        if (in.getRowCount() == 0) { Util.error(this, "Add at least one job (or use Randomize)."); return; }
        List<PCB> procs = new ArrayList<>();
        Scheduler scheduler = (Scheduler) algo.getSelectedItem();
        try {
            for (int i = 0; i < in.getRowCount(); i++) {
                String row = " (row " + (i + 1) + ")";
                procs.add(new PCB(i,
                        Util.parseInt(Util.str(in.getValueAt(i, 1)), "Arrival time (AT)" + row, 0),
                        Util.parseInt(Util.str(in.getValueAt(i, 2)), "Burst time (BT)" + row, 1),
                        Util.parseInt(Util.str(in.getValueAt(i, 3)), "Priority" + row, 0)));
            }
            if (scheduler instanceof RoundRobinScheduler)
                ((RoundRobinScheduler) scheduler).setQuantum(Util.parseInt(quantum.getText(), "Time quantum", 1));
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }

        List<Segment> timeline = scheduler.schedule(procs);   // polymorphic call

        out.setRowCount(0);
        double sumT = 0, sumW = 0;
        for (PCB p : procs) {
            sumT += p.getTurnaround(); sumW += p.getWaiting();
            out.addRow(new Object[]{p.getLabel(), p.getArrival(), p.getBurst(), p.getStart(),
                    p.getCompletion(), p.getWaiting(), p.getTurnaround()});
        }
        avg.setText(String.format("AWT (average waiting time) = %.2f        ATAT (average turnaround time) = %.2f",
                sumW / procs.size(), sumT / procs.size()));
        gantt.play(timeline);
    }
}
