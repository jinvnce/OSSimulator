import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** CPU scheduling tab: process table, randomizer, results table and animated Gantt chart. */
public class CpuPanel extends ModulePanel {
    private final DefaultTableModel in = new DefaultTableModel(new String[]{"J#", "AT", "BT", "Priority"}, 0) {
        public boolean isCellEditable(int r, int c) { return c > 0; }
    };
    private final DefaultTableModel out = Util.readOnlyModel(
            new String[]{"J#", "AT", "BT", "ST", "FT", "WT", "TAT"});
    private final JTable inTable = new JTable(in) {
        @Override protected javax.swing.table.JTableHeader createDefaultTableHeader() {
            return new javax.swing.table.JTableHeader(columnModel) {
                @Override public String getToolTipText(java.awt.event.MouseEvent e) {
                    return columnModel.getColumn(columnModel.getColumnIndexAtX(e.getX())).getModelIndex() == 3
                            ? "Priority: a SMALLER number means HIGHER priority (runs first). Used only by the Priority algorithm."
                            : null;
                }
            };
        }
    };
    private final JComboBox<Scheduler> algo = new JComboBox<>(new Scheduler[]{
            new FCFSScheduler(), new SJFScheduler(), new PriorityScheduler(), new RoundRobinScheduler(2)});
    private final JComboBox<String> mode = new JComboBox<>(new String[]{"Non-preemptive", "Preemptive"});
    private final Scheduler sjfPre = new SRTFScheduler(), priPre = new PreemptivePriorityScheduler();
    private final JTextField quantum = new JTextField("2", 4), size = new JTextField("5", 5);
    private final JLabel avg = new JLabel("AWT and ATAT will appear here after you click Run.");
    private final GanttChart gantt = new GanttChart();

    public CpuPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new GridLayout(2, 1));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4)), r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton rnd = new JButton("Randomize"), add = new JButton("Add row"), del = new JButton("Remove selected"),
                clr = new JButton("Clear"), run = new JButton("Run");
        r1.add(new JLabel("Algorithm:")); r1.add(algo);
        r1.add(new JLabel("Type:")); r1.add(mode);
        r1.add(new JLabel("Time quantum:")); r1.add(quantum);
        r1.add(new JLabel("Size:")); r1.add(size); r1.add(rnd);
        r1.add(add); r1.add(del); r1.add(clr); r1.add(run);
        JLabel hint = new JLabel("Priority: a smaller number = higher priority (runs first). Only the Priority algorithm uses it.");
        hint.setForeground(Color.DARK_GRAY);
        r2.add(hint);
        top.add(r1); top.add(r2);
        add(top, BorderLayout.NORTH);

        JSplitPane tables = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                Util.scroll(inTable, "Jobs (double-click a cell to edit)"), Util.scroll(new JTable(out), "Results (AT arrival, BT burst, ST start, FT finish, WT waiting, TAT turnaround)"));
        tables.setResizeWeight(0.4);
        JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tables, Util.scroll(gantt, "Gantt chart"));
        main.setResizeWeight(0.55);
        add(main, BorderLayout.CENTER);
        add(avg, BorderLayout.SOUTH);

        // grey out the Priority column unless the Priority algorithm is selected
        inTable.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) comp.setForeground(algo.getSelectedItem() instanceof PriorityScheduler ? Color.BLACK : Color.GRAY);
                return comp;
            }
        });
        algo.addActionListener(e -> updateControls());
        updateControls();
        add.addActionListener(e -> { Util.stopEditing(inTable); in.addRow(randomRow(in.getRowCount() + 1)); renumber(); });
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

    private void updateControls() {
        Object a = algo.getSelectedItem();
        boolean choosable = a instanceof SJFScheduler || a instanceof PriorityScheduler;
        if (a instanceof FCFSScheduler) mode.setSelectedIndex(0);
        if (a instanceof RoundRobinScheduler) mode.setSelectedIndex(1);
        mode.setEnabled(choosable);
        quantum.setEnabled(a instanceof RoundRobinScheduler);
        inTable.repaint();
    }

    private Scheduler chosenScheduler() {
        Scheduler base = (Scheduler) algo.getSelectedItem();
        if (mode.getSelectedIndex() == 1) {
            if (base instanceof SJFScheduler) return sjfPre;
            if (base instanceof PriorityScheduler) return priPre;
        }
        return base;
    }

    /** A random job: AT 0..max(5, n-1), BT 1..10, Priority 1..10. */
    private Object[] randomRow(int n) {
        return new Object[]{"", "" + Util.RNG.nextInt(Math.max(6, n)), "" + (1 + Util.RNG.nextInt(10)), "" + (1 + Util.RNG.nextInt(10))};
    }

    private void renumber() { for (int i = 0; i < in.getRowCount(); i++) in.setValueAt("J" + (i + 1), i, 0); }

    @Override
    protected void randomizeData() {
        try {
            int n = Util.parseInt(size.getText(), "Size", 1);
            Util.stopEditing(inTable);
            in.setRowCount(0);
            for (int i = 0; i < n; i++) in.addRow(randomRow(n));
            renumber();
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
    }

    @Override
    protected void runSimulation() {
        Util.stopEditing(inTable);
        if (in.getRowCount() == 0) { Util.error(this, "Add at least one job (or use Randomize)."); return; }
        List<PCB> procs = new ArrayList<>();
        Scheduler scheduler = chosenScheduler();
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
        avg.setText(String.format(scheduler.getName() + ":   AWT (average waiting time) = %.2f        ATAT (average turnaround time) = %.2f",
                sumW / procs.size(), sumT / procs.size()));
        gantt.play(timeline);
    }
}
