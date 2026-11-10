import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;


public class DiskPanel extends ModulePanel {

    private final JTextField requests = new JTextField("98 183 37 122 14 124 65 67", 30);
    private final JTextField head = new JTextField("53", 4);
    private final JTextField maxCyl = new JTextField("199", 4);
    private final JTextField len = new JTextField("8", 3);

    private final JComboBox<DiskScheduler> algo = new JComboBox<>(new DiskScheduler[]{
            new FIFODisk(), new DDTFDisk(), new LookDisk(), new ElevatorDisk(), new CScanDisk()});

    private final DefaultTableModel steps = Util.readOnlyModel(new String[]{"Seq", "Track", "Traversed"});
    private final DefaultTableModel compare = Util.readOnlyModel(new String[]{"Algorithm", "Total", "Avg / seek"});

    private final JTable stepsTable = new JTable(steps);
    private final JTable compareTable = new JTable(compare);

    private final JLabel totalValue = big();
    private final JLabel avgValue = big();
    private final JLabel description = new JLabel(" ");
    private final DiskChart chart = new DiskChart();

    public DiskPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // TOP BAR (row 1: data, row 2: algorithm + run)
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        JButton rnd = new JButton("Randomize");
        JButton run = new JButton("▶ Run");

        requests.setToolTipText("Cylinders in arrival order, separated by spaces or commas");
        head.setToolTipText("Starting head position");
        maxCyl.setToolTipText("Highest cylinder number");
        len.setToolTipText("Number of requests to generate");
        rnd.setToolTipText("Generate a random queue and head position");
        run.setFont(run.getFont().deriveFont(Font.BOLD));

        row1.add(new JLabel("Queue"));
        row1.add(requests);
        row1.add(new JLabel("Head"));
        row1.add(head);
        row1.add(new JLabel("Max"));
        row1.add(maxCyl);
        row1.add(new JLabel("Count"));
        row1.add(len);
        row1.add(rnd);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        row2.add(new JLabel("Algorithm"));
        row2.add(algo);
        row2.add(run);

        description.setForeground(Color.DARK_GRAY);
        description.setFont(description.getFont().deriveFont(Font.ITALIC));
        description.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 0));

        JPanel top = new JPanel(new GridLayout(3, 1));
        top.add(row1);
        top.add(row2);
        top.add(description);
        add(top, BorderLayout.NORTH);

        // TABLE TWEAKS
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        for (JTable t : new JTable[]{stepsTable, compareTable}) {
            t.setRowHeight(22);
            t.setFillsViewportHeight(true);
            for (int c = 0; c < t.getColumnCount(); c++) {
                t.getColumnModel().getColumn(c).setCellRenderer(center);
            }
        }

        stepsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        stepsTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        stepsTable.getColumnModel().getColumn(2).setPreferredWidth(140);

        // LEFT: sequence + comparison
        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Util.scroll(stepsTable, "Sequence"),
                Util.scroll(compareTable, "Comparison"));
        left.setResizeWeight(0.65);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                left, Util.scroll(chart, "Head path"));
        main.setResizeWeight(0.35);
        add(main, BorderLayout.CENTER);

        // BOTTOM: totals
        JPanel summary = new JPanel(new GridLayout(1, 2, 8, 0));
        summary.add(card("Total", totalValue));
        summary.add(card("Avg / seek", avgValue));
        add(summary, BorderLayout.SOUTH);

        algo.addActionListener(e -> description.setText(((DiskScheduler) algo.getSelectedItem()).getDescription()));
        description.setText(((DiskScheduler) algo.getSelectedItem()).getDescription());
        rnd.addActionListener(e -> randomizeData());
        run.addActionListener(e -> runSimulation());
    }

    @Override public String getTitle() { return "Disk Scheduling"; }

    private static JLabel big() {
        JLabel l = new JLabel("-", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 24f));
        return l;
    }

    private JPanel card(String title, JLabel v) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    @Override
    protected void randomizeData() {
        try {
            int n = Util.parseInt(len.getText(), "Count", 1);
            int max = Util.parseInt(maxCyl.getText(), "Max", 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) sb.append(Util.RNG.nextInt(max + 1)).append(' ');
            requests.setText(sb.toString().trim());
            head.setText(String.valueOf(Util.RNG.nextInt(max + 1)));
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
    }

    private static class Input { int head, max; List<Integer> reqs = new ArrayList<>(); }

    private Input read() {
        Input in = new Input();
        in.max = Util.parseInt(maxCyl.getText(), "Max", 1);
        in.head = Util.parseInt(head.getText(), "Head", 0);
        if (in.head > in.max) throw new IllegalArgumentException("Head cannot be more than Max (" + in.max + ").");
        String text = requests.getText().trim();
        if (text.isEmpty()) throw new IllegalArgumentException("Queue is empty.");
        String[] parts = text.split("[\\s,]+");
        for (int i = 0; i < parts.length; i++) {
            int c = Util.parseInt(parts[i], "Cylinder #" + (i + 1), 0);
            if (c > in.max) throw new IllegalArgumentException("Cylinder #" + (i + 1) + " (" + c + ") is more than Max (" + in.max + ").");
            in.reqs.add(c);
        }
        return in;
    }

    @Override
    protected void runSimulation() {
        Input in;
        try { in = read(); } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }

        DiskScheduler alg = (DiskScheduler) algo.getSelectedItem();
        DiskResult r = alg.schedule(in.head, in.reqs, true, in.max);

        // SEQUENCE: track served and the subtraction (larger - smaller)
        steps.setRowCount(0);
        steps.addRow(new Object[]{"Start", r.getPath().get(0), ""});

        for (int i = 0; i < r.getSteps(); i++) {
            int from = r.getPath().get(i);
            int to = r.getPath().get(i + 1);
            steps.addRow(new Object[]{
                    i + 1,
                    to,
                    Math.max(from, to) + " − " + Math.min(from, to) + " = " + r.getMove(i)
            });
        }

        totalValue.setText(String.valueOf(r.getTotal()));
        avgValue.setText(String.format("%.2f", r.getAverage()));
        chart.show(r.getPath(), in.max);

        compareAll(in);
    }

    // COMPARISON (best first)
    private void compareAll(Input in) {
        List<DiskResult> results = new ArrayList<>();

        for (int i = 0; i < algo.getItemCount(); i++) {
            results.add(algo.getItemAt(i).schedule(in.head, in.reqs, true, in.max));
        }

        results.sort(Comparator.comparingInt(DiskResult::getTotal));

        compare.setRowCount(0);
        for (DiskResult r : results) {
            compare.addRow(new Object[]{r.getName(), r.getTotal(), String.format("%.2f", r.getAverage())});
        }
    }
}