package multiprogramming;

import util.Util;
import ui.ModuleHeader;
import ui.ModulePanel;
import ui.Theme;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MultiprogrammingPanel extends ModulePanel {

    private static final double EPS = 0.0000001;
    private static final Color DARK_ROW = new Color(0x16293F);

    private final DefaultTableModel input = new DefaultTableModel(
            new String[]{"Job", "Arrival", "Burst"}, 0
    ) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c > 0;
        }
    };

    private final DefaultTableModel simulation = Util.readOnlyModel(
            new String[]{"Time", "Event", "Jobs", "%CPU/job", "Elapsed Time", "Headway/Job", "Job#", "Left"}
    );

    private final DefaultTableModel summary = Util.readOnlyModel(
            new String[]{"Measure", "Value"}
    );

    private final JTable inputTable = new JTable(input);
    private final JTable simulationTable = new JTable(simulation);
    private final JTable summaryTable = new JTable(summary);

    private final JTextArea timeline = new JTextArea();

    private final JTextField jobCount = new Theme.RoundField("4", 4);
    private final JTextField maxBurst = new Theme.RoundField("10", 4);

    private final JComboBox<Integer> decimals = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5, 6});

    public MultiprogrammingPanel() {

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        JPanel tools = ModuleHeader.toolRow();
        JButton random = ModuleHeader.withIcon(ModuleHeader.navyButton("Randomize", 140), "dice", Theme.CREAM);
        random.setToolTipText("Generate jobs using the values on the left");
        jobCount.setToolTipText("How many jobs to generate");
        maxBurst.setToolTipText("Largest burst time a generated job can have");

        Theme.styleCombo(decimals);
        decimals.setSelectedItem(3);
        decimals.setPreferredSize(new Dimension(70, 34));
        decimals.setToolTipText("How many decimal places to show in the results (0 to 6)");

        tools.add(Theme.label("Jobs", Theme.CREAM, 15f));
        tools.add(jobCount);
        tools.add(Theme.label("Max burst", Theme.CREAM, 15f));
        tools.add(maxBurst);
        tools.add(Theme.label("Decimals", Theme.CREAM, 15f));
        tools.add(decimals);
        tools.add(random);

        JButton run = ModuleHeader.runButton("Run");
        add(ModuleHeader.north(
                "Multiprogramming",
                "Jobs share the CPU equally; watch the headway of each job",
                "memory", tools, run), BorderLayout.NORTH);

        Theme.styleTable(inputTable, false);
        Theme.styleTable(simulationTable, true);
        Theme.styleTable(summaryTable, true);
        inputTable.setAutoCreateRowSorter(false);
        installInputEditor();

        TableColumnModel cm = simulationTable.getColumnModel();
        int[] w = {60, 200, 45, 65, 80, 80, 160, 200};
        for (int i = 0; i < w.length; i++) {
            cm.getColumn(i).setPreferredWidth(w[i]);
        }

        for (int c : new int[]{1, 6, 7}) {
            cm.getColumn(c).setCellRenderer(new WrapRenderer(true));
        }

        timeline.setEditable(false);
        timeline.setFont(Theme.body(14f));
        timeline.setBackground(DARK_ROW);
        timeline.setForeground(Theme.CREAM);
        timeline.setCaretColor(Theme.CREAM);
        timeline.setSelectionColor(Theme.GOLD);
        timeline.setSelectedTextColor(Theme.INK);
        timeline.setMargin(new Insets(6, 8, 6, 8));

        Theme.SkinPanel jobsCard = new Theme.SkinPanel("Jobs", "table", false);
        jobsCard.body.setLayout(new BorderLayout(0, 8));
        jobsCard.body.add(Theme.scroll(inputTable, Theme.CREAM), BorderLayout.CENTER);

        JButton add = ModuleHeader.withIcon(ModuleHeader.navyButton("Add", 110), "plus", Theme.CREAM);
        JButton remove = ModuleHeader.withIcon(ModuleHeader.dangerButton("Remove", 110), "minus", Theme.CRIMSON);
        JButton clear = ModuleHeader.withIcon(ModuleHeader.dangerButton("Clear all", 110), "trash", Theme.CRIMSON);
        add.setToolTipText("Add a random job");

        Runnable doRemove = () -> {
            Util.stopEditing(inputTable);
            int[] rows = inputTable.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) {
                input.removeRow(inputTable.convertRowIndexToModel(rows[i]));
            }
            renumber();
        };
        Runnable doClear = () -> {
            Util.stopEditing(inputTable);
            input.setRowCount(0);
        };
        jobsCard.body.add(ModuleHeader.tableButtons(add, remove, clear, inputTable, input, doRemove, doClear, this),
                BorderLayout.SOUTH);

        Theme.SkinPanel summaryCard = new Theme.SkinPanel("Summary", "sheet", true);

        summaryCard.body.add(Theme.scroll(summaryTable, DARK_ROW), BorderLayout.CENTER);

        Theme.SkinPanel simCard = new Theme.SkinPanel("Simulation", "chip", true);

        simCard.body.add(Theme.scroll(simulationTable, DARK_ROW), BorderLayout.CENTER);

        Theme.SkinPanel timelineCard = new Theme.SkinPanel("Timeline", "list", true);
        timelineCard.body.add(Theme.scroll(timeline, DARK_ROW), BorderLayout.CENTER);

        JSplitPane left = Theme.split(jobsCard, summaryCard);
        left.setResizeWeight(0.55);

        JSplitPane right = Theme.split(simCard, timelineCard);
        right.setResizeWeight(0.75);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        main.setResizeWeight(0.3);
        main.setBorder(null);
        main.setOpaque(false);
        main.setDividerSize(10);
        main.setContinuousLayout(true);

        add(main, BorderLayout.CENTER);

        add.addActionListener(e -> {

            Random rng = new Random();
            int max;

            try {
                max = Integer.parseInt(maxBurst.getText().trim());
                if (max < 1) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ex) {
                Util.error(this, "Largest Burst must be a positive number.");
                return;
            }

            double arrival = rng.nextInt(20);
            double burst = randomBurst(rng, max);

            input.addRow(new Object[]{
                    "J" + (input.getRowCount() + 1),
                    fmt(arrival),
                    fmt(burst)
            });
        });

        random.addActionListener(e -> randomizeData());
        run.addActionListener(e -> runSimulation());

        decimals.addActionListener(e -> {
            if (simulation.getRowCount() > 0) {
                runSimulation();
            }
        });

        input.addRow(new Object[]{"J1", "7.5", "0.5"});
        input.addRow(new Object[]{"J2", "8.1", "0.6"});
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.paintBackground(g, this);
    }

    @Override
    public String getTitle() {
        return "Multiprogramming";
    }

    private void installInputEditor() {
        JTextField tf = new JTextField();
        tf.setFont(Theme.body(15f));
        tf.setHorizontalAlignment(SwingConstants.CENTER);
        tf.setBackground(Theme.FIELD);
        tf.setForeground(Theme.INK);
        tf.setCaretColor(Theme.INK);
        tf.setSelectionColor(Theme.GOLD);
        tf.setSelectedTextColor(Theme.INK);
        tf.setBorder(new LineBorder(Theme.CRIMSON, 2));
        inputTable.setDefaultEditor(Object.class, new DefaultCellEditor(tf));
    }

    private void renumber() {
        for (int i = 0; i < input.getRowCount(); i++) {
            input.setValueAt("J" + (i + 1), i, 0);
        }
    }

    private static class Job {

        int id;
        double arrival;
        double burst;
        double remaining;
        double finish;

        boolean started = false;

        Job(int id, double arrival, double burst) {
            this.id = id;
            this.arrival = arrival;
            this.burst = burst;
            this.remaining = burst;
        }

        String label() {
            return "J" + (id + 1);
        }
    }

    @Override
    protected void randomizeData() {

        try {

            int n = Integer.parseInt(jobCount.getText().trim());
            int max = Integer.parseInt(maxBurst.getText().trim());

            if (n < 1 || max < 1) {
                throw new IllegalArgumentException(
                        "Values must be greater than zero."
                );
            }

            Util.stopEditing(inputTable);
            input.setRowCount(0);

            Random rng = new Random();

            for (int i = 0; i < n; i++) {

                double arrival = rng.nextInt(20);
                double burst = randomBurst(rng, max);

                input.addRow(new Object[]{
                        "J" + (i + 1),
                        fmt(arrival),
                        fmt(burst)
                });
            }

        } catch (NumberFormatException ex) {
            Util.error(this, "Enter valid numbers.");
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
        }
    }


    private double randomBurst(Random rng, int max) {
        int d = (Integer) decimals.getSelectedItem();
        double step = Math.pow(10, -d);
        double raw = 0.5 + rng.nextDouble() * (max - 0.5);
        double rounded = Math.round(raw / step) * step;
        return Math.max(step, rounded);
    }

    @Override
    protected void runSimulation() {

        Util.stopEditing(inputTable);

        int n = input.getRowCount();

        if (n == 0) {
            Util.error(this, "Add at least one job.");
            return;
        }

        List<Job> jobs = new ArrayList<>();

        try {

            for (int i = 0; i < n; i++) {

                double arrival = Double.parseDouble(
                        input.getValueAt(i, 1).toString()
                );

                double burst = Double.parseDouble(
                        input.getValueAt(i, 2).toString()
                );

                if (!Double.isFinite(arrival) ||
                        !Double.isFinite(burst) ||
                        arrival < 0 || burst <= 0) {

                    throw new IllegalArgumentException(
                            "Arrival must be nonnegative and burst must be positive."
                    );
                }

                jobs.add(new Job(i, arrival, burst));
            }

        } catch (NumberFormatException ex) {
            Util.error(this, "Invalid arrival or burst time.");
            return;
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
            return;
        }

        jobs.sort(
                Comparator.comparingDouble((Job j) -> j.arrival)
                        .thenComparingInt(j -> j.id)
        );

        simulate(jobs);
    }

    private void simulate(List<Job> jobs) {

        simulation.setRowCount(0);
        summary.setRowCount(0);
        timeline.setText("");

        List<Job> active = new ArrayList<>();

        double time = jobs.get(0).arrival;

        int nextJob = 0;
        int completed = 0;

        double totalTurnaround = 0;
        double totalWaiting = 0;
        double cpuBusy = 0;

        StringBuilder log = new StringBuilder();

        while (completed < jobs.size()) {

            if (active.isEmpty()) {

                time = jobs.get(nextJob).arrival;

                StringJoiner arrivedJobs = new StringJoiner(", ");

                while (nextJob < jobs.size()
                        && jobs.get(nextJob).arrival <= time + EPS) {

                    Job j = jobs.get(nextJob++);

                    j.started = true;
                    active.add(j);
                    arrivedJobs.add(j.label());

                    log.append(j.label())
                            .append(" arrived/started at ")
                            .append(fmt(time))
                            .append("\n");
                }

                simulation.addRow(new Object[]{
                        fmt(time),
                        arrivedJobs + " Arrived/Started",
                        0, "", "", "", "", ""
                });

                continue;
            }

            double nextArrival = nextJob < jobs.size()
                    ? jobs.get(nextJob).arrival
                    : Double.POSITIVE_INFINITY;

            double elapsed = nextArrival - time;

            for (Job j : active) {
                elapsed = Math.min(elapsed, j.remaining * active.size());
            }

            if (!Double.isFinite(elapsed) || elapsed <= EPS) {
                break;
            }

            int activeCount = active.size();

            double cpuPercentage = 100.0 / activeCount;
            double headway = elapsed / activeCount;

            double previousTime = time;

            for (Job j : active) {

                j.remaining -= headway;

                if (Math.abs(j.remaining) < EPS) {
                    j.remaining = 0;
                }
            }

            time += elapsed;
            cpuBusy += elapsed;

            StringJoiner activeJobs = new StringJoiner(", ");
            StringJoiner remainingTimes = new StringJoiner(", ");

            for (Job j : active) {
                activeJobs.add(j.label());
                remainingTimes.add(fmt(j.remaining));
            }

            List<Job> finished = new ArrayList<>();
            StringJoiner terminatedJobs = new StringJoiner(", ");

            for (Job j : active) {

                if (j.remaining <= EPS) {

                    j.remaining = 0;
                    j.finish = time;

                    finished.add(j);
                    terminatedJobs.add(j.label());

                    double turnaround = j.finish - j.arrival;
                    double waiting = turnaround - j.burst;

                    totalTurnaround += turnaround;
                    totalWaiting += Math.max(0, waiting);

                    completed++;

                    log.append(j.label())
                            .append(" terminated at ")
                            .append(fmt(time))
                            .append("\n");
                }
            }

            List<Job> arriving = new ArrayList<>();
            StringJoiner arrivedJobs = new StringJoiner(", ");

            while (nextJob < jobs.size()
                    && jobs.get(nextJob).arrival <= time + EPS) {

                Job j = jobs.get(nextJob++);

                j.started = true;
                arriving.add(j);
                arrivedJobs.add(j.label());

                log.append(j.label())
                        .append(" arrived/started at ")
                        .append(fmt(time))
                        .append("\n");
            }

            StringJoiner event = new StringJoiner("; ");

            if (!finished.isEmpty()) {
                event.add(terminatedJobs + " Terminated");
            }

            if (!arriving.isEmpty()) {
                event.add(arrivedJobs + " Arrived/Started");
            }

            simulation.addRow(new Object[]{
                    fmt(time),
                    event.toString(),
                    activeCount,
                    fmt(cpuPercentage) + "%",
                    fmt(time - previousTime),
                    fmt(headway),
                    activeJobs.toString(),
                    remainingTimes.toString()
            });

            active.removeAll(finished);
            active.addAll(arriving);
        }

        double totalTime = time - jobs.get(0).arrival;

        summary.addRow(new Object[]{"Jobs", jobs.size()});
        summary.addRow(new Object[]{"Finish time", fmt(time)});
        summary.addRow(new Object[]{"Avg turnaround", fmt(totalTurnaround / jobs.size())});
        summary.addRow(new Object[]{"Avg waiting", fmt(totalWaiting / jobs.size())});
        summary.addRow(new Object[]{"CPU busy", fmt(cpuBusy)});
        summary.addRow(new Object[]{"CPU utilization",
                fmt(totalTime > 0 ? (cpuBusy / totalTime) * 100 : 0) + "%"});

        timeline.setText(log.toString());
        timeline.setCaretPosition(0);
    }

    private static class WrapRenderer extends JTextArea implements TableCellRenderer {

        private static final JTextArea MEASURE = new JTextArea();

        private final Color rowA, rowB, fg;

        WrapRenderer(boolean dark) {
            rowA = dark ? new Color(0x16293F) : new Color(0xFBEBD2);
            rowB = dark ? new Color(0x1C344F) : Theme.CREAM;
            fg = dark ? Theme.CREAM : Theme.INK;
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected,
                boolean focus, int row, int col) {

            setFont(table.getFont());
            setText(value == null ? "" : value.toString());
            setBackground(selected ? Theme.GOLD : (row % 2 == 0 ? rowA : rowB));
            setForeground(selected ? Theme.INK : fg);

            int h = rowHeightFor(table, row);

            if (table.getRowHeight(row) != h) {
                table.setRowHeight(row, h);
            }

            return this;
        }

        private static int rowHeightFor(JTable table, int row) {

            int h = table.getRowHeight();

            MEASURE.setFont(table.getFont());
            MEASURE.setLineWrap(true);
            MEASURE.setWrapStyleWord(true);
            MEASURE.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

            for (int c = 0; c < table.getColumnCount(); c++) {

                if (!(table.getColumnModel().getColumn(c).getCellRenderer() instanceof WrapRenderer)) {
                    continue;
                }

                Object v = table.getValueAt(row, c);

                MEASURE.setText(v == null ? "" : v.toString());
                MEASURE.setSize(table.getColumnModel().getColumn(c).getWidth(), Short.MAX_VALUE);

                h = Math.max(h, MEASURE.getPreferredSize().height);
            }

            return h;
        }
    }

    private String fmt(double value) {
        int d = (Integer) decimals.getSelectedItem();
        return String.format(Locale.US, "%." + d + "f", value);
    }
}