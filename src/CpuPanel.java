import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.*;
import java.util.*;
import java.util.List;


public class CpuPanel extends ModulePanel {
    private static final int MAX_SCALE = 6;
    private static final int MAX_SLICES = 200_000;

    private static final String[] IN_NAMES = {"Job", "Arrival (AT)", "Burst (BT)"};
    private static final String[] IN_TIPS = {"Job number", "Arrival time (AT): when the job arrives and is ready to run",
            "Burst time (BT): how long the job needs the CPU in total"};
    private static final String[] OUT_NAMES = {"Job", "Arrival", "Burst", "Start", "Finish", "Waiting", "Turnaround"};
    private static final String[] OUT_TIPS = {"Job number", "Arrival time (AT): when the job arrived",
            "Burst time (BT): total CPU time the job needs", "Start time (ST): when the job first got the CPU",
            "Finish time (FT): when the job completed", "Waiting time (WT) = Turnaround - Burst: time spent waiting in the queue",
            "Turnaround time (TAT) = Finish - Arrival: total time from arrival to completion"};

    private final DefaultTableModel in = new DefaultTableModel(IN_NAMES, 0) {
        public boolean isCellEditable(int r, int c) { return c > 0; }
    };
    private final DefaultTableModel out = Util.readOnlyModel(OUT_NAMES);
    private final JTable inTable = tipTable(in, IN_TIPS), outTable = tipTable(out, OUT_TIPS);
    private final JComboBox<Scheduler> algo = new JComboBox<>(new Scheduler[]{
            new FCFSScheduler(), new SJFScheduler(), new SRTScheduler(), new RoundRobinScheduler(2)});
    private final JLabel type = new JLabel(), description = new JLabel();
    private final JLabel quantumLabel = new JLabel("Time quantum:");
    private final JTextField quantum = new JTextField("2", 6), size = new JTextField("5", 4), maxValue = new JTextField("10", 6);
    private final JCheckBox decimalValues = new JCheckBox("Use decimals");
    private final JComboBox<String> inUnit = new JComboBox<>(TimeFmt.IN_UNITS), outUnit = new JComboBox<>(TimeFmt.OUT_UNITS);
    private final JComboBox<Integer> places = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    private final JLabel awtValue = bigValue(), atatValue = bigValue();
    private final JLabel note = new JLabel(" ");
    private final JScrollPane resultsScroll = Util.scroll(outTable, "Results");
    private final GanttChart gantt = new GanttChart();

    public CpuPanel() {
        setLayout(new BorderLayout(10, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel steps = new JLabel("How to use:  1. Choose an algorithm   2. Add or edit the jobs   3. Click Run, then read the results and the Gantt chart.");
        steps.setFont(steps.getFont().deriveFont(Font.BOLD));
        steps.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 0));
        add(steps, BorderLayout.NORTH);

        JPanel left = new LeftPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(algorithmPanel());
        left.add(Box.createVerticalStrut(6));
        left.add(jobsPanel());
        left.add(Box.createVerticalStrut(6));
        left.add(unitsPanel());
        left.add(Box.createVerticalStrut(8));
        JButton run = new JButton("Run Simulation");
        run.setFont(run.getFont().deriveFont(Font.BOLD, 16f));
        run.setAlignmentX(Component.LEFT_ALIGNMENT);
        run.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        run.setPreferredSize(new Dimension(200, 42));
        left.add(run);
        JScrollPane leftScroll = new JScrollPane(left, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setBorder(BorderFactory.createEmptyBorder());
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftScroll.setPreferredSize(new Dimension(430, 100));
        add(leftScroll, BorderLayout.WEST);

        JLabel formulas = new JLabel("<html>Waiting = Turnaround - Burst &nbsp;&nbsp;&nbsp; Turnaround = Finish - Arrival<br>Hover over a column title to see what it means.</html>");
        formulas.setForeground(Color.DARK_GRAY);
        formulas.setBorder(BorderFactory.createEmptyBorder(0, 6, 2, 0));
        resultsScroll.setPreferredSize(new Dimension(400, 170));
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.add(resultsScroll, BorderLayout.CENTER);
        resultsPanel.add(formulas, BorderLayout.SOUTH);
        JScrollPane ganttScroll = Util.scroll(gantt, "Gantt chart (who uses the CPU, and when)");
        ganttScroll.setPreferredSize(new Dimension(400, 220));
        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultsPanel, ganttScroll);
        right.setResizeWeight(0.4);

        JPanel summary = new JPanel(new GridLayout(1, 2, 8, 0));
        summary.add(card("Average waiting time (AWT)", awtValue, "Average of every job's waiting time"));
        summary.add(card("Average turnaround time (ATAT)", atatValue, "Average of every job's turnaround time"));
        JPanel bottom = new JPanel(new BorderLayout(0, 2));
        bottom.add(summary, BorderLayout.CENTER);
        bottom.add(note, BorderLayout.SOUTH);
        note.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.add(right, BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        inUnit.setSelectedIndex(0);
        outUnit.setSelectedIndex(0);
        places.setSelectedItem(2);
        algo.addActionListener(e -> updateControls());
        inUnit.addActionListener(e -> updateHeaders());
        updateControls();
        updateHeaders();
        loadExample();
        run.addActionListener(e -> runSimulation());
    }

    private static class LeftPanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private JPanel algorithmPanel() {
        JPanel p = titled("1. Choose algorithm");
        GridBagConstraints c = gbc();
        c.gridy = 0; c.gridx = 0; p.add(new JLabel("Algorithm:"), c);
        c.gridx = 1; c.weightx = 1; p.add(algo, c);
        c.gridy = 1; c.gridx = 0; c.weightx = 0; p.add(new JLabel("Type:"), c);
        type.setFont(type.getFont().deriveFont(Font.BOLD));
        c.gridx = 1; p.add(type, c);
        c.gridy = 2; c.gridx = 0; p.add(quantumLabel, c);
        c.gridx = 1; p.add(quantum, c);
        c.gridy = 3; c.gridx = 0; c.gridwidth = 2; c.insets = new Insets(8, 4, 2, 4);
        description.setForeground(Color.DARK_GRAY);
        p.add(description, c);
        quantum.setToolTipText("Round Robin only: the longest a job may run before the next job gets a turn");
        return p;
    }

    private JPanel jobsPanel() {
        JPanel p = titled("2. Jobs (double-click a cell to edit)");
        p.setLayout(new BorderLayout(0, 6));
        JScrollPane sp = new JScrollPane(inTable);
        sp.setPreferredSize(new Dimension(200, 130));
        p.add(sp, BorderLayout.CENTER);

        JButton add = new JButton("Add row"), del = new JButton("Remove selected"), clr = new JButton("Clear all");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(add); buttons.add(del); buttons.add(clr);

        JPanel random = titled("Generate random jobs");
        GridBagConstraints c = gbc();
        c.gridy = 0; c.gridx = 0; random.add(new JLabel("How many jobs:"), c);
        c.gridx = 1; random.add(size, c);
        c.gridx = 2; random.add(new JLabel("Largest value:"), c);
        c.gridx = 3; random.add(maxValue, c);
        JButton rnd = new JButton("Randomize");
        c.gridy = 1; c.gridx = 0; c.gridwidth = 2; random.add(decimalValues, c);
        c.gridx = 2; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; random.add(rnd, c);
        maxValue.setToolTipText("Largest number the randomizer uses for a time, e.g. 10 or 1000000");
        decimalValues.setToolTipText("Make the random times decimal numbers such as 3.7");

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(buttons, BorderLayout.NORTH);
        south.add(random, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);

        add.addActionListener(e -> {
            try { Util.stopEditing(inTable); in.addRow(randomRow()); renumber(); }
            catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
        });
        del.addActionListener(e -> {
            Util.stopEditing(inTable);
            int[] rows = inTable.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) in.removeRow(rows[i]);
            renumber();
        });
        clr.addActionListener(e -> {
            Util.stopEditing(inTable);
            in.setRowCount(0);
            clearResults();
        });
        rnd.addActionListener(e -> randomizeData());
        return p;
    }

    private JPanel unitsPanel() {
        JPanel p = titled("3. Time unit");
        GridBagConstraints c = gbc();
        c.gridy = 0; c.gridx = 0; p.add(new JLabel("My times are in:"), c);
        c.gridx = 1; c.weightx = 1; p.add(inUnit, c);
        c.gridy = 1; c.gridx = 0; c.weightx = 0; p.add(new JLabel("Convert to:"), c);
        c.gridx = 1; p.add(outUnit, c);
        c.gridy = 2; c.gridx = 0; p.add(new JLabel("Decimal places:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.NONE; p.add(places, c);
        inUnit.setToolTipText("The unit of the times you type: seconds, minutes or hours");
        outUnit.setToolTipText("What to show the results as: clock time (h:mm:ss), or seconds / minutes / hours");
        places.setToolTipText("Decimal places in the averages and in converted numbers");
        return p;
    }

    private JPanel titled(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 4, 3, 4);
        return c;
    }

    private JPanel card(String title, JLabel value, String tip) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(value, BorderLayout.CENTER);
        p.setToolTipText(tip);
        return p;
    }

    private static JLabel bigValue() {
        JLabel l = new JLabel("-", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 24f));
        l.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        return l;
    }

    private static JTable tipTable(DefaultTableModel model, String[] tips) {
        JTable t = new JTable(model) {
            @Override protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override public String getToolTipText(MouseEvent e) {
                        int col = columnModel.getColumnIndexAtX(e.getX());
                        if (col < 0) return null;
                        int mi = columnModel.getColumn(col).getModelIndex();
                        return mi < tips.length ? tips[mi] : null;
                    }
                };
            }
        };
        t.setRowHeight(24);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        t.setDefaultRenderer(Object.class, center);
        ((DefaultTableCellRenderer) t.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        return t;
    }

    @Override public String getTitle() { return "CPU Scheduling"; }

    private void updateControls() {
        Scheduler s = (Scheduler) algo.getSelectedItem();
        type.setText(s.isPreemptive() ? "Preemptive (can interrupt a job)" : "Non-preemptive (no interrupts)");
        boolean rr = s instanceof RoundRobinScheduler;
        quantumLabel.setVisible(rr);
        quantum.setVisible(rr);
        description.setText("<html><body style='width:280px'>" + s.getDescription() + "</body></html>");
    }

    private void updateHeaders() {
        String u = " (" + TimeFmt.ABBR[inUnit.getSelectedIndex()] + ")";
        in.setColumnIdentifiers(new String[]{IN_NAMES[0], IN_NAMES[1] + u, IN_NAMES[2] + u});
        inTable.getColumnModel().getColumn(0).setPreferredWidth(40);
    }

    private void loadExample() {
        String[][] d = {{"0", "8"}, {"1", "4"}, {"2", "9"}, {"3", "5"}};
        for (String[] r : d) in.addRow(new Object[]{"", r[0], r[1]});
        renumber();
    }

    private void clearResults() {
        out.setRowCount(0);
        awtValue.setText("-");
        atatValue.setText("-");
        note.setText(" ");
        ((TitledBorder) resultsScroll.getBorder()).setTitle("Results");
        resultsScroll.repaint();
        gantt.play(new ArrayList<>(), null, 0);
    }

    private long maxRandom() {
        try {
            BigInteger v = Util.parseDecimal(maxValue.getText(), "Max value").toBigIntegerExact();
            if (v.signum() < 1 || v.compareTo(BigInteger.valueOf(1_000_000_000_000L)) > 0)
                throw new IllegalArgumentException("Max value must be a whole number from 1 to 1,000,000,000,000.");
            return v.longValue();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Max value must be a whole number (got \"" + maxValue.getText().trim() + "\").");
        }
    }

    private Object[] randomRow() {
        long max = maxRandom();
        long arrMax = Math.max(1, max / 2);
        String at, bt;
        if (decimalValues.isSelected()) {
            at = BigDecimal.valueOf((long) (Util.RNG.nextDouble() * (arrMax * 10 + 1)), 1).toPlainString();
            bt = BigDecimal.valueOf(1 + (long) (Util.RNG.nextDouble() * (max * 10)), 1).toPlainString();
        } else {
            at = "" + (long) (Util.RNG.nextDouble() * (arrMax + 1));
            bt = "" + (1 + (long) (Util.RNG.nextDouble() * max));
        }
        return new Object[]{"", at, bt};
    }

    private void renumber() { for (int i = 0; i < in.getRowCount(); i++) in.setValueAt("J" + (i + 1), i, 0); }

    @Override
    protected void randomizeData() {
        try {
            int n = Util.parseInt(size.getText(), "Size", 1);
            Util.stopEditing(inTable);
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) rows.add(randomRow());
            in.setRowCount(0);
            for (Object[] r : rows) in.addRow(r);
            renumber();
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
    }

    @Override
    protected void runSimulation() {
        Util.stopEditing(inTable);
        int n = in.getRowCount();
        if (n == 0) { Util.error(this, "Add at least one job (or use Randomize)."); return; }
        Scheduler scheduler = (Scheduler) algo.getSelectedItem();
        List<PCB> procs = new ArrayList<>();
        int scale;
        TimeFmt fmt;
        try {
            BigDecimal[] at = new BigDecimal[n], bt = new BigDecimal[n];
            BigDecimal q = null;
            scale = 0;
            for (int i = 0; i < n; i++) {
                String row = " (row " + (i + 1) + ")";
                at[i] = Util.parseDecimal(Util.str(in.getValueAt(i, 1)), "Arrival time (AT)" + row);
                bt[i] = Util.parseDecimal(Util.str(in.getValueAt(i, 2)), "Burst time (BT)" + row);
                if (at[i].signum() < 0) throw new IllegalArgumentException("Arrival time (AT)" + row + " cannot be negative.");
                if (bt[i].signum() <= 0) throw new IllegalArgumentException("Burst time (BT)" + row + " must be greater than 0.");
                scale = Math.max(scale, decimalsOf(at[i])); scale = Math.max(scale, decimalsOf(bt[i]));
            }
            if (scheduler instanceof RoundRobinScheduler) {
                q = Util.parseDecimal(quantum.getText(), "Time quantum");
                if (q.signum() <= 0) throw new IllegalArgumentException("Time quantum must be greater than 0.");
                scale = Math.max(scale, decimalsOf(q));
            }
            if (scale > MAX_SCALE)
                throw new IllegalArgumentException("Use at most " + MAX_SCALE + " decimal places in the times.");

            BigInteger sum = BigInteger.ZERO, maxAt = BigInteger.ZERO;
            long[] a = new long[n], b = new long[n];
            for (int i = 0; i < n; i++) {
                BigInteger ai = fixed(at[i], scale), bi = fixed(bt[i], scale);
                sum = sum.add(bi); maxAt = maxAt.max(ai);
                if (maxAt.add(sum).bitLength() > 61) throw new IllegalArgumentException("The numbers are too big. Please use smaller times.");
                a[i] = ai.longValue(); b[i] = bi.longValue();
            }
            long ql = 0;
            if (q != null) {
                BigInteger qi = fixed(q, scale);
                if (qi.bitLength() > 61) throw new IllegalArgumentException("Time quantum is too big.");
                ql = qi.longValue();
                if (sum.divide(qi).compareTo(BigInteger.valueOf(MAX_SLICES)) > 0)
                    throw new IllegalArgumentException("The time quantum is too small for these burst times (over "
                            + MAX_SLICES + " slices). Please use a larger quantum.");
            }
            for (int i = 0; i < n; i++) procs.add(new PCB(i, a[i], b[i]));
            if (scheduler instanceof RoundRobinScheduler) ((RoundRobinScheduler) scheduler).setQuantum(ql);

            int decimals = (Integer) places.getSelectedItem();
            fmt = new TimeFmt(scale, inUnit.getSelectedIndex(), outUnit.getSelectedIndex(), decimals);
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }

        List<Segment> timeline = scheduler.schedule(procs);   // polymorphic call

        String u = " (" + TimeFmt.outLabel(outUnit.getSelectedIndex()) + ")";
        String[] names = new String[OUT_NAMES.length];
        for (int i = 0; i < names.length; i++) names[i] = i == 0 ? OUT_NAMES[0] : OUT_NAMES[i] + u;
        out.setColumnIdentifiers(names);
        out.setRowCount(0);
        BigInteger sumT = BigInteger.ZERO, sumW = BigInteger.ZERO;
        for (PCB p : procs) {
            sumT = sumT.add(BigInteger.valueOf(p.getTurnaround()));
            sumW = sumW.add(BigInteger.valueOf(p.getWaiting()));
            out.addRow(new Object[]{p.getLabel(), fmt.format(p.getArrival()), fmt.format(p.getBurst()), fmt.format(p.getStart()),
                    fmt.format(p.getCompletion()), fmt.format(p.getWaiting()), fmt.format(p.getTurnaround())});
        }
        String inU = TimeFmt.ABBR[inUnit.getSelectedIndex()];
        awtValue.setText(fmt.average(sumW, n));
        atatValue.setText(fmt.average(sumT, n));
        note.setText("In the unit you entered: AWT = " + fmt.averageInput(sumW, n) + " " + inU + ",  ATAT = " + fmt.averageInput(sumT, n) + " " + inU);
        ((TitledBorder) resultsScroll.getBorder()).setTitle("Results: " + scheduler.getName() + " (" + (scheduler.isPreemptive() ? "preemptive" : "non-preemptive") + ")");
        resultsScroll.repaint();
        gantt.play(timeline, fmt, scale);
    }

    private static int decimalsOf(BigDecimal v) { return Math.max(0, v.stripTrailingZeros().scale()); }

    private static BigInteger fixed(BigDecimal v, int scale) { return v.movePointRight(scale).toBigIntegerExact(); }
}
