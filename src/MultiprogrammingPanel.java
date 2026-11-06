import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.*;
import java.util.*;
import java.util.List;

/**
 * Multiprogramming tab. Each job runs CPU burst 1, then does I/O, then CPU burst 2.
 * Uniprogramming runs one job at a time, so the CPU sits idle during I/O.
 * Multiprogramming lets another ready job use the CPU while a job waits for I/O (one CPU, one I/O device, both first-come first-served).
 * Times are whole numbers. Shows finish, turnaround, waiting, totals/averages and CPU utilization for both.
 */
public class MultiprogrammingPanel extends ModulePanel {
    private static final int MAX_TICKS = 1_000_000;

    private final DefaultTableModel in = new DefaultTableModel(new String[]{"Job", "Arrival", "CPU burst 1", "I/O burst", "CPU burst 2"}, 0) {
        public boolean isCellEditable(int r, int c) { return c > 0; }
    };
    private final JTable inTable = new JTable(in);
    private final DefaultTableModel out = Util.readOnlyModel(new String[]{"Job", "Arrival", "Finish (Multi)", "Turnaround (Multi)", "Waiting (Multi)",
            "Finish (Uni)", "Turnaround (Uni)", "Waiting (Uni)"});
    private final DefaultTableModel sum = Util.readOnlyModel(new String[]{"Measure", "Uniprogramming", "Multiprogramming"});
    private final JTextArea timeline = new JTextArea();
    private final JTextField size = new JTextField("4", 4), maxValue = new JTextField("8", 4);
    private final JComboBox<String> inUnit = new JComboBox<>(TimeFmt.IN_UNITS), outUnit = new JComboBox<>(TimeFmt.OUT_UNITS);
    private final JComboBox<Integer> places = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5, 6});

    public MultiprogrammingPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton add = new JButton("Add row"), del = new JButton("Remove selected"), rnd = new JButton("Randomize"), run = new JButton("Run");
        top.add(add); top.add(del);
        top.add(new JLabel("How many jobs:")); top.add(size);
        top.add(new JLabel("Largest burst:")); top.add(maxValue); top.add(rnd);
        JPanel units = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        units.add(new JLabel("My times are in:")); units.add(inUnit);
        units.add(new JLabel("Convert to:")); units.add(outUnit);
        units.add(new JLabel("Decimals:")); units.add(places); units.add(run);
        JPanel north = new JPanel(new GridLayout(2, 1));
        north.add(top); north.add(units);
        add(north, BorderLayout.NORTH);
        inUnit.setSelectedIndex(0); outUnit.setSelectedIndex(0); places.setSelectedItem(2);   // Seconds -> clock time

        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Util.scroll(inTable, "Jobs (double-click a cell to edit)"),
                Util.scroll(new JTable(out), "Results per job"));
        left.setResizeWeight(0.4);
        timeline.setEditable(false);
        timeline.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Util.scroll(new JTable(sum), "Totals, averages and CPU utilization"),
                Util.scroll(timeline, "Timeline (Multiprogramming)"));
        right.setResizeWeight(0.4);
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        main.setResizeWeight(0.55);
        add(main, BorderLayout.CENTER);
        JLabel f = new JLabel("Turnaround = Finish - Arrival     Waiting = Turnaround - (CPU1 + I/O + CPU2)     CPU utilization = CPU busy time / total time");
        f.setForeground(Color.DARK_GRAY);
        add(f, BorderLayout.SOUTH);

        add.addActionListener(e -> { Util.stopEditing(inTable); in.addRow(randomRow()); renumber(); });
        del.addActionListener(e -> {
            Util.stopEditing(inTable);
            int[] rows = inTable.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) in.removeRow(rows[i]);
            renumber();
        });
        rnd.addActionListener(e -> randomizeData());
        run.addActionListener(e -> runSimulation());
        String[][] d = {{"0", "3", "4", "2"}, {"1", "2", "5", "3"}, {"2", "4", "2", "1"}, {"3", "1", "3", "2"}};
        for (String[] r : d) in.addRow(new Object[]{"", r[0], r[1], r[2], r[3]});
        renumber();
    }

    @Override public String getTitle() { return "Multiprogramming"; }

    private void renumber() { for (int i = 0; i < in.getRowCount(); i++) in.setValueAt("J" + (i + 1), i, 0); }

    private Object[] randomRow() {
        int max = Util.parseInt(maxValue.getText(), "Largest burst", 1);
        return new Object[]{"", Util.RNG.nextInt(max + 1), 1 + Util.RNG.nextInt(max), 1 + Util.RNG.nextInt(max), 1 + Util.RNG.nextInt(max)};
    }

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

    /** One job: arrival, CPU burst 1, I/O burst, CPU burst 2, plus results. */
    private static class Job {
        final int id, at, c1, io, c2;
        int phase, left, finish;
        Job(int id, int at, int c1, int io, int c2) { this.id = id; this.at = at; this.c1 = c1; this.io = io; this.c2 = c2; }
        int service() { return c1 + io + c2; }
        String label() { return "J" + (id + 1); }
    }

    @Override
    protected void runSimulation() {
        Util.stopEditing(inTable);
        int n = in.getRowCount();
        if (n == 0) { Util.error(this, "Add at least one job (or use Randomize)."); return; }
        List<Job> jobs = new ArrayList<>();
        long total = 0;
        try {
            for (int i = 0; i < n; i++) {
                String row = " (row " + (i + 1) + ")";
                int at = Util.parseInt(Util.str(in.getValueAt(i, 1)), "Arrival" + row, 0);
                int c1 = Util.parseInt(Util.str(in.getValueAt(i, 2)), "CPU burst 1" + row, 1);
                int io = Util.parseInt(Util.str(in.getValueAt(i, 3)), "I/O burst" + row, 1);
                int c2 = Util.parseInt(Util.str(in.getValueAt(i, 4)), "CPU burst 2" + row, 1);
                total += (long) at + c1 + io + c2;
                if (total > MAX_TICKS) throw new IllegalArgumentException("The numbers are too big. Please use smaller times.");
                jobs.add(new Job(i, at, c1, io, c2));
            }
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }

        TimeFmt tf = new TimeFmt(0, inUnit.getSelectedIndex(), outUnit.getSelectedIndex(), (Integer) places.getSelectedItem());
        String u = " (" + TimeFmt.outLabel(outUnit.getSelectedIndex()) + ")";
        out.setColumnIdentifiers(new String[]{"Job", "Arrival" + u, "Finish, Multi" + u, "Turnaround, Multi" + u, "Waiting, Multi" + u,
                "Finish, Uni" + u, "Turnaround, Uni" + u, "Waiting, Uni" + u});

        int[] uniFinish = uniprogramming(jobs);
        StringBuilder tl = new StringBuilder();
        int[] multi = multiprogramming(jobs, tl, tf);          // finish times
        int multiTotal = 0, uniTotal = 0, cpuBusy = 0;
        long mT = 0, mW = 0, uT = 0, uW = 0;
        out.setRowCount(0);
        for (Job j : jobs) {
            int mf = multi[j.id], uf = uniFinish[j.id];
            int mTat = mf - j.at, uTat = uf - j.at;
            mT += mTat; mW += mTat - j.service(); uT += uTat; uW += uTat - j.service();
            multiTotal = Math.max(multiTotal, mf); uniTotal = Math.max(uniTotal, uf);
            cpuBusy += j.c1 + j.c2;
            out.addRow(new Object[]{j.label(), tf.format(j.at), tf.format(mf), tf.format(mTat), tf.format(mTat - j.service()),
                    tf.format(uf), tf.format(uTat), tf.format(uTat - j.service())});
        }
        sum.setRowCount(0);
        sum.addRow(new Object[]{"Total time to finish all jobs", tf.format(uniTotal), tf.format(multiTotal)});
        sum.addRow(new Object[]{"Total turnaround time", tf.format(uT), tf.format(mT)});
        sum.addRow(new Object[]{"Average turnaround time", tf.average(BigInteger.valueOf(uT), n), tf.average(BigInteger.valueOf(mT), n)});
        sum.addRow(new Object[]{"Total waiting time", tf.format(uW), tf.format(mW)});
        sum.addRow(new Object[]{"Average waiting time", tf.average(BigInteger.valueOf(uW), n), tf.average(BigInteger.valueOf(mW), n)});
        sum.addRow(new Object[]{"CPU busy time", tf.format(cpuBusy), tf.format(cpuBusy)});
        sum.addRow(new Object[]{"CPU utilization", fmt(100.0 * cpuBusy / uniTotal) + "%", fmt(100.0 * cpuBusy / multiTotal) + "%"});
        timeline.setText(tl.toString());
        timeline.setCaretPosition(0);
    }

    private static String fmt(double v) { return String.format("%.2f", v); }

    /** One job at a time in order of arrival. The CPU is idle while the running job does I/O. */
    private int[] uniprogramming(List<Job> jobs) {
        List<Job> order = new ArrayList<>(jobs);
        order.sort(Comparator.comparingInt((Job j) -> j.at).thenComparingInt(j -> j.id));
        int[] fin = new int[jobs.size()];
        int t = 0;
        for (Job j : order) { t = Math.max(t, j.at) + j.service(); fin[j.id] = t; }
        return fin;
    }

    /** Tick-by-tick simulation: one CPU and one I/O device, each first-come first-served, running in parallel. */
    private int[] multiprogramming(List<Job> jobs, StringBuilder tl, TimeFmt tf) {
        int n = jobs.size(), done = 0, t = 0;
        int[] fin = new int[n];
        ArrayDeque<Job> cpuQ = new ArrayDeque<>(), ioQ = new ArrayDeque<>();
        Job cpu = null, io = null;
        for (Job j : jobs) { j.phase = 0; j.left = j.c1; }
        StringBuilder cpuLine = new StringBuilder(), ioLine = new StringBuilder();
        String lastCpu = null, lastIo = null;
        int cpuStart = 0, ioStart = 0;
        List<String> cpuSeg = new ArrayList<>(), ioSeg = new ArrayList<>();
        while (done < n) {
            List<Job> arriving = new ArrayList<>();
            for (Job j : jobs) if (j.at == t) arriving.add(j);        // equal arrivals keep job order
            cpuQ.addAll(arriving);
            if (cpu == null && !cpuQ.isEmpty()) cpu = cpuQ.poll();
            if (io == null && !ioQ.isEmpty()) io = ioQ.poll();
            String cl = cpu == null ? "Idle" : cpu.label(), il = io == null ? "Idle" : io.label();
            if (!cl.equals(lastCpu)) { if (lastCpu != null) cpuSeg.add(lastCpu + " " + tf.format(cpuStart) + "-" + tf.format(t)); lastCpu = cl; cpuStart = t; }
            if (!il.equals(lastIo)) { if (lastIo != null) ioSeg.add(lastIo + " " + tf.format(ioStart) + "-" + tf.format(t)); lastIo = il; ioStart = t; }
            if (cpu != null && --cpu.left == 0) {
                if (cpu.phase == 0) { cpu.phase = 1; cpu.left = cpu.io; ioQ.add(cpu); }
                else { cpu.finish = t + 1; fin[cpu.id] = t + 1; done++; }
                cpu = null;
            }
            if (io != null && --io.left == 0) { io.phase = 2; io.left = io.c2; cpuQ.add(io); io = null; }
            t++;
        }
        cpuSeg.add(lastCpu + " " + tf.format(cpuStart) + "-" + tf.format(t));
        ioSeg.add(lastIo + " " + tf.format(ioStart) + "-" + tf.format(t));
        tl.append("CPU:\n  ").append(String.join("  |  ", cpuSeg)).append("\n\n");
        tl.append("I/O device:\n  ").append(String.join("  |  ", ioSeg)).append("\n");
        return fin;
    }
}
