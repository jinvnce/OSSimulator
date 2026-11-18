package disk;

import ui.ModuleHeader;
import ui.ModulePanel;
import ui.Theme;
import util.Util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DiskPanel extends ModulePanel {

    private static final Color DARK_ROW = new Color(0x16293F);

    private final JTextField requests = new Theme.RoundField("98 183 37 122 14 124 65 67", 22);
    private final JTextField head = new Theme.RoundField("53", 4);
    private final JTextField maxCyl = new Theme.RoundField("199", 4);
    private final JTextField len = new Theme.RoundField("8", 3);

    private final JComboBox<DiskScheduler> algo = new JComboBox<>(new DiskScheduler[]{
            new FIFODisk(), new DDTFDisk(), new LookDisk(), new ElevatorDisk(), new CScanDisk()});

    private final DefaultTableModel steps = Util.readOnlyModel(new String[]{"Seq", "Track", "Traversed"});
    private final DefaultTableModel compare = Util.readOnlyModel(new String[]{"Algorithm", "Total", "Avg / seek"});

    private final JTable stepsTable = new JTable(steps);
    private final JTable compareTable = new JTable(compare);

    private final JLabel totalValue = big();
    private final JLabel avgValue = big();
    private final JLabel description = Theme.label(" ", Theme.GOLD, 14f);
    private final DiskChart chart = new DiskChart();

    public DiskPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        // ---------------------------------------------------------- HEADER + TOOL BAR
        JButton rnd = ModuleHeader.navyButton("Randomize", 130);
        JButton run = ModuleHeader.runButton("Run");

        requests.setToolTipText("Cylinders in arrival order, separated by spaces or commas");
        head.setToolTipText("Starting head position");
        maxCyl.setToolTipText("Highest cylinder number");
        len.setToolTipText("Number of requests to generate");
        rnd.setToolTipText("Generate a random queue and head position");

        Theme.styleCombo(algo);
        algo.setPreferredSize(new Dimension(130, 36));

        JPanel row1 = ModuleHeader.toolRow();
        row1.add(Theme.label("Queue", Theme.CREAM, 15f));
        row1.add(requests);
        row1.add(rnd);

        JPanel row2 = ModuleHeader.toolRow();
        row2.add(Theme.label("Head", Theme.CREAM, 15f));
        row2.add(head);
        row2.add(Theme.label("Max", Theme.CREAM, 15f));
        row2.add(maxCyl);
        row2.add(Theme.label("Count", Theme.CREAM, 15f));
        row2.add(len);
        row2.add(Theme.label("Algorithm", Theme.CREAM, 15f));
        row2.add(algo);

        description.setFont(description.getFont().deriveFont(Font.ITALIC));
        JPanel row3 = ModuleHeader.toolRow();
        row3.add(description);

        JPanel tools = new JPanel();
        tools.setOpaque(false);
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{row1, row2, row3}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            tools.add(c);
        }

        add(ModuleHeader.north(
                "Disk Scheduling",
                "Compare how far the head travels under each algorithm",
                "gear", tools, run), BorderLayout.NORTH);

        // ---------------------------------------------------------- TABLE STYLING
        Theme.styleTable(stepsTable, true);
        Theme.styleTable(compareTable, true);

        stepsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        stepsTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        stepsTable.getColumnModel().getColumn(2).setPreferredWidth(140);

        // ---------------------------------------------------------- RESULT CARDS (navy)
        Theme.SkinPanel seqCard = new Theme.SkinPanel("Sequence", null, true);
        seqCard.body.add(Theme.scroll(stepsTable, DARK_ROW), BorderLayout.CENTER);

        Theme.SkinPanel cmpCard = new Theme.SkinPanel("Comparison", null, true);
        cmpCard.body.add(Theme.scroll(compareTable, DARK_ROW), BorderLayout.CENTER);

        Theme.SkinPanel chartCard = new Theme.SkinPanel("Head path", null, true);
        chartCard.body.add(Theme.scroll(chart, DARK_ROW), BorderLayout.CENTER);

        // ---------------------------------------------------------- LAYOUT
        JSplitPane left = Theme.split(seqCard, cmpCard);
        left.setResizeWeight(0.65);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, chartCard);
        main.setResizeWeight(0.35);
        main.setBorder(null);
        main.setOpaque(false);
        main.setDividerSize(10);
        main.setContinuousLayout(true);
        add(main, BorderLayout.CENTER);

        // BOTTOM: totals
        JPanel summary = new JPanel(new GridLayout(1, 2, 10, 0));
        summary.setOpaque(false);
        summary.add(card("Total", totalValue));
        summary.add(card("Avg / seek", avgValue));
        add(summary, BorderLayout.SOUTH);

        // ---------------------------------------------------------- ACTIONS
        algo.addActionListener(e -> description.setText(descriptionOf()));
        description.setText(descriptionOf());
        rnd.addActionListener(e -> randomizeData());
        run.addActionListener(e -> runSimulation());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.paintBackground(g, this);
    }

    @Override public String getTitle() { return "Disk Scheduling"; }

    private String descriptionOf() {
        String d = ((DiskScheduler) algo.getSelectedItem()).getDescription();
        return d == null || d.isEmpty() ? " " : d;
    }

    private static JLabel big() {
        JLabel l = new JLabel("-", SwingConstants.CENTER);
        l.setFont(Theme.heading(28f));
        l.setForeground(Theme.GOLD);
        return l;
    }

    private Theme.SkinPanel card(String title, JLabel v) {
        Theme.SkinPanel p = new Theme.SkinPanel(title, null, true);
        p.body.add(v, BorderLayout.CENTER);
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