import javax.swing.*;
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
    private final JTable inTable = tipTable(in, IN_TIPS, false), outTable = tipTable(out, OUT_TIPS, true);
    private final Scheduler fcfs = new FCFSScheduler(), sjf = new SJFScheduler(), srt = new SRTScheduler(),
            rr = new RoundRobinScheduler(2);
    private final JComboBox<Scheduler> algo = new JComboBox<>(new Scheduler[]{fcfs, sjf, srt, rr});
    private final JComboBox<String> mode = new JComboBox<>(new String[]{"Non-preemptive", "Preemptive"});
    private final JLabel quantumLabel = Theme.label("Time quantum:", Theme.CREAM, 15f);
    private final JTextField quantum = new Theme.RoundField("2", 5), size = new Theme.RoundField("5", 4), maxValue = new Theme.RoundField("10", 6);
    private final JCheckBox decimalValues = new JCheckBox("Use decimals");
    private final JComboBox<String> inUnit = new JComboBox<>(TimeFmt.IN_UNITS), outUnit = new JComboBox<>(TimeFmt.OUT_UNITS);
    private final JComboBox<Integer> places = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    private final JLabel awtValue = bigValue(), atatValue = bigValue();
    private final JLabel note = Theme.label(" ", Theme.CREAM_SHADOW, 14f);
    private final Theme.SkinPanel resultsPanel = new Theme.SkinPanel("Results", "cpu", false);
    private final GanttChart gantt = new GanttChart();

    public CpuPanel() {
        setLayout(new BorderLayout(10, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton run = new Theme.SkinButton("Run Simulation", "red");
        run.setIcon(Theme.playIcon(12, Theme.CREAM));
        run.setFont(Theme.body(17f).deriveFont(Font.BOLD));
        run.setPreferredSize(new Dimension(210, 44));
        add(northPanel(run), BorderLayout.NORTH);

        JPanel left = new LeftPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(jobsPanel());
        left.add(Box.createVerticalStrut(2));
        left.add(randomPanel());
        left.add(Box.createVerticalStrut(2));
        left.add(unitsPanel());
        JScrollPane leftScroll = new JScrollPane(left, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setBorder(BorderFactory.createEmptyBorder());
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setOpaque(false);
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftScroll.setPreferredSize(new Dimension(400, 100));
        Theme.styleScroll(leftScroll);
        add(leftScroll, BorderLayout.WEST);

        // right side: Gantt chart on top, results below (as in the design)
        Theme.SkinPanel ganttPanel = new Theme.SkinPanel("Gantt Chart", "diamond", false);
        JScrollPane ganttScroll = Theme.scroll(gantt, Theme.DEEP);
        ganttScroll.setPreferredSize(new Dimension(400, 170));
        ganttPanel.body.add(ganttScroll, BorderLayout.CENTER);
        ganttPanel.body.setToolTipText("Gantt chart: who uses the CPU, and when");

        JScrollPane resultsScroll = Theme.scroll(outTable, Theme.DEEP);
        resultsScroll.setPreferredSize(new Dimension(400, 150));
        resultsPanel.body.add(resultsScroll, BorderLayout.CENTER);

        JSplitPane right = Theme.split(ganttPanel, resultsPanel);
        right.setResizeWeight(0.45);

        JPanel summary = new JPanel(new GridLayout(1, 2, 12, 0));
        summary.setOpaque(false);
        summary.add(card("Average waiting time (AWT)", awtValue, "Average of every job's waiting time", "clock"));
        summary.add(card("Average turnaround time (ATAT)", atatValue, "Average of every job's turnaround time", "hourglass"));
        JPanel bottom = new JPanel(new BorderLayout(0, 0));
        bottom.setOpaque(false);
        bottom.add(summary, BorderLayout.CENTER);
        bottom.add(note, BorderLayout.SOUTH);
        note.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);
        center.add(right, BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        Theme.styleCombo(algo);
        Theme.styleCombo(inUnit);
        Theme.styleCombo(outUnit);
        Theme.styleCombo(places);
        Theme.styleCombo(mode);
        Theme.styleCheck(decimalValues);

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

    @Override protected void paintComponent(Graphics g) {
        Theme.paintBackground(g, this);
        super.paintComponent(g);
    }

    private static class LeftPanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ---------------------------------------------------------------- top area
    private JPanel northPanel(JButton run) {
        // title banner (cream card with spade)
        Theme.SkinPanel banner = new Theme.SkinPanel(null, null, false);
        banner.body.setLayout(new BorderLayout(12, 0));
        banner.body.add(new JLabel(Theme.icon("spade", 30, Theme.INK)), BorderLayout.WEST);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("CPU Scheduling");
        title.setFont(Theme.heading(28f));
        title.setForeground(Theme.INK);
        JLabel sub = Theme.label("Choose an algorithm and run the simulation!", Theme.MUTED, 14f);
        titles.add(title);
        titles.add(sub);
        banner.body.add(titles, BorderLayout.CENTER);
        banner.setBorder(BorderFactory.createEmptyBorder(8, 14, 16, 18));
        banner.setPreferredSize(new Dimension(400, 86));
        banner.setMinimumSize(new Dimension(200, 86));

        // toolbar: algorithm, mode, quantum, Run
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        tools.setOpaque(false);
        tools.add(Theme.label("Algorithm:", Theme.CREAM, 15f));
        algo.setPreferredSize(new Dimension(160, 34));
        tools.add(algo);
        tools.add(Theme.label("Mode:", Theme.CREAM, 15f));
        mode.setPreferredSize(new Dimension(170, 34));
        tools.add(mode);
        tools.add(quantumLabel);
        tools.add(quantum);
        quantum.setToolTipText("Round Robin only: the longest a job may run before the next job gets a turn");
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(tools, BorderLayout.CENTER);
        bar.add(run, BorderLayout.EAST);

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{banner, bar}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            north.add(c);
        }
        return north;
    }

    // ---------------------------------------------------------------- left column
    private JPanel jobsPanel() {
        Theme.SkinPanel p = new Theme.SkinPanel("Process Input", "cards", false);
        p.body.setLayout(new BorderLayout(0, 8));
        JScrollPane sp = Theme.scroll(inTable, new Color(0xFBEBD2));
        sp.setPreferredSize(new Dimension(200, 190));
        p.body.add(sp, BorderLayout.CENTER);
        p.body.setToolTipText("Double-click a cell to edit");

        JButton add = new Theme.SkinButton("Add row", "navy"), del = new Theme.SkinButton("Remove", "navy"),
                clr = new Theme.SkinButton("Clear all", "navy");
        JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 0));
        buttons.setOpaque(false);
        for (JButton b : new JButton[]{add, del, clr}) { b.setMargin(new Insets(4, 4, 8, 4)); b.setPreferredSize(new Dimension(100, 38)); buttons.add(b); }
        p.body.add(buttons, BorderLayout.SOUTH);

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
        return p;
    }

    private JPanel randomPanel() {
        Theme.SkinPanel p = new Theme.SkinPanel("Generate Random Jobs", "club", false);
        p.body.setLayout(new GridBagLayout());
        GridBagConstraints c = gbc();
        c.gridy = 0; c.gridx = 0; c.weightx = 0; p.body.add(Theme.label("How many jobs:", Theme.INK, 15f), c);
        c.gridy = 1; c.weightx = 1; p.body.add(size, c);
        c.gridy = 2; c.weightx = 0; p.body.add(Theme.label("Largest value:", Theme.INK, 15f), c);
        c.gridy = 3; c.weightx = 1; p.body.add(maxValue, c);
        c.gridy = 4; p.body.add(decimalValues, c);
        JButton rnd = new Theme.SkinButton("Randomize", "navy");
        rnd.setPreferredSize(new Dimension(150, 40));
        c.gridy = 5; c.insets = new Insets(6, 4, 3, 4); p.body.add(rnd, c);
        maxValue.setToolTipText("Largest number the randomizer uses for a time, e.g. 10 or 1000000");
        decimalValues.setToolTipText("Make the random times decimal numbers such as 3.7");
        rnd.addActionListener(e -> randomizeData());
        return p;
    }

    private JPanel unitsPanel() {
        Theme.SkinPanel p = new Theme.SkinPanel("Time Unit", "clock", false);
        p.body.setLayout(new GridBagLayout());
        GridBagConstraints c = gbc();
        c.gridy = 0; c.gridx = 0; c.weightx = 0; p.body.add(Theme.label("My times are in:", Theme.INK, 15f), c);
        c.gridy = 1; c.weightx = 1; inUnit.setPreferredSize(new Dimension(150, 34)); p.body.add(inUnit, c);
        c.gridy = 2; c.weightx = 0; p.body.add(Theme.label("Convert to:", Theme.INK, 15f), c);
        c.gridy = 3; c.weightx = 1; outUnit.setPreferredSize(new Dimension(150, 34)); p.body.add(outUnit, c);
        c.gridy = 4; c.weightx = 0; p.body.add(Theme.label("Decimal places:", Theme.INK, 15f), c);
        c.gridy = 5; c.weightx = 1; places.setPreferredSize(new Dimension(150, 34)); p.body.add(places, c);
        inUnit.setToolTipText("The unit of the times you type: seconds, minutes or hours");
        outUnit.setToolTipText("What to show the results as: clock time (h:mm:ss), or seconds / minutes / hours");
        places.setToolTipText("Decimal places in the averages and in converted numbers");
        return p;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 4, 3, 4);
        return c;
    }

    // ---------------------------------------------------------------- small builders
    private JPanel card(String title, JLabel value, String tip, String iconName) {
        Theme.SkinPanel p = new Theme.SkinPanel(null, null, true);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 18, 20));
        p.setPreferredSize(new Dimension(200, 104));
        p.body.setLayout(new BorderLayout(14, 0));
        p.body.add(new JLabel(Theme.icon(iconName, 38, Theme.CREAM)), BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = Theme.label(title, Theme.CREAM, 14f);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(t);
        text.add(value);
        p.body.add(text, BorderLayout.CENTER);
        p.setToolTipText(tip);
        return p;
    }

    private static JLabel bigValue() {
        JLabel l = new JLabel("-", SwingConstants.LEFT);
        l.setFont(Theme.heading(30f));
        l.setForeground(Theme.CREAM);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        return l;
    }

    private static JTable tipTable(DefaultTableModel model, String[] tips, boolean dark) {
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
        Theme.styleTable(t, dark);
        return t;
    }

    @Override public String getTitle() { return "CPU Scheduling"; }

    private boolean adjustingMode;

    private void updateControls() {
        Scheduler s = (Scheduler) algo.getSelectedItem();
        adjustingMode = true;
        mode.setSelectedItem(s.isPreemptive() ? "Preemptive" : "Non-preemptive");
        adjustingMode = false;
        mode.setEnabled(s == sjf || s == srt);   // only SJF and SRT can run either way
        algo.setToolTipText(s.getDescription());
        boolean isRR = s instanceof RoundRobinScheduler;
        quantumLabel.setVisible(isRR);
        quantum.setVisible(isRR);
    }

    /** The scheduler to run: SJF + Preemptive is SRT, and SRT + Non-preemptive is SJF. */
    private Scheduler chosenScheduler() {
        Scheduler s = (Scheduler) algo.getSelectedItem();
        boolean pre = "Preemptive".equals(mode.getSelectedItem());
        if (s == sjf && pre) return srt;
        if (s == srt && !pre) return sjf;
        return s;
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
        resultsPanel.setTitle("Results");
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
        Scheduler scheduler = chosenScheduler();
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
        resultsPanel.setTitle("Results: " + scheduler.getName() + " (" + (scheduler.isPreemptive() ? "preemptive" : "non-preemptive") + ")");
        gantt.play(timeline, fmt, scale);
    }

    private static int decimalsOf(BigDecimal v) { return Math.max(0, v.stripTrailingZeros().scale()); }

    private static BigInteger fixed(BigDecimal v, int scale) { return v.movePointRight(scale).toBigIntegerExact(); }
}