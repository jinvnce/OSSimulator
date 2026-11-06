import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;


public class DiskPanel extends ModulePanel {
    private final JTextField requests = new JTextField("98 183 37 122 14 124 65 67", 34),
            head = new JTextField("53", 5), maxCyl = new JTextField("199", 5), len = new JTextField("8", 4);
    private final JComboBox<DiskScheduler> algo = new JComboBox<>(new DiskScheduler[]{
            new FIFODisk(), new DDTFDisk(), new LookDisk(), new ElevatorDisk(), new CScanDisk()});
    private final JComboBox<String> dir = new JComboBox<>(new String[]{"Toward higher cylinders", "Toward lower cylinders"});
    private final DefaultTableModel steps = Util.readOnlyModel(new String[]{"Step", "From", "To", "Movement"});
    private final DefaultTableModel compare = Util.readOnlyModel(new String[]{"Algorithm", "Total movement", "Average per seek"});
    private final JLabel totalValue = big(), avgValue = big();
    private final JLabel description = new JLabel(" ");
    private final DiskChart chart = new DiskChart();

    public DiskPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JPanel top = new JPanel(new GridLayout(3, 1));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        r1.add(new JLabel("Request queue (cylinders, in arrival order):")); r1.add(requests);
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        JButton rnd = new JButton("Randomize"), run = new JButton("Run"), all = new JButton("Compare All");
        r2.add(new JLabel("Head start:")); r2.add(head);
        r2.add(new JLabel("Last cylinder:")); r2.add(maxCyl);
        r2.add(new JLabel("Length:")); r2.add(len); r2.add(rnd);
        JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        r3.add(new JLabel("Algorithm:")); r3.add(algo);
        r3.add(new JLabel("First direction:")); r3.add(dir);
        r3.add(run); r3.add(all);
        top.add(r1); top.add(r2); top.add(r3);
        JPanel north = new JPanel(new BorderLayout());
        north.add(top, BorderLayout.CENTER);
        description.setForeground(Color.DARK_GRAY);
        description.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        north.add(description, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Util.scroll(new JTable(steps), "Seeks (head movement per step)"),
                Util.scroll(new JTable(compare), "Comparison of all algorithms"));
        left.setResizeWeight(0.6);
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, Util.scroll(chart, "Head path"));
        main.setResizeWeight(0.4);
        add(main, BorderLayout.CENTER);

        JPanel summary = new JPanel(new GridLayout(1, 2, 8, 0));
        summary.add(card("Total head movement", totalValue));
        summary.add(card("Average head movement per seek", avgValue));
        add(summary, BorderLayout.SOUTH);

        algo.addActionListener(e -> description.setText(((DiskScheduler) algo.getSelectedItem()).getDescription()));
        description.setText(((DiskScheduler) algo.getSelectedItem()).getDescription());
        rnd.addActionListener(e -> randomizeData());
        run.addActionListener(e -> runSimulation());
        all.addActionListener(e -> compareAll());
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
            int n = Util.parseInt(len.getText(), "Length", 1);
            int max = Util.parseInt(maxCyl.getText(), "Last cylinder", 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) sb.append(Util.RNG.nextInt(max + 1)).append(' ');
            requests.setText(sb.toString().trim());
            head.setText(String.valueOf(Util.RNG.nextInt(max + 1)));
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
    }

    private static class Input { int head, max; List<Integer> reqs = new ArrayList<>(); }

    private Input read() {
        Input in = new Input();
        in.max = Util.parseInt(maxCyl.getText(), "Last cylinder", 1);
        in.head = Util.parseInt(head.getText(), "Head start", 0);
        if (in.head > in.max) throw new IllegalArgumentException("Head start cannot be more than the last cylinder (" + in.max + ").");
        String text = requests.getText().trim();
        if (text.isEmpty()) throw new IllegalArgumentException("Request queue is blank. Type cylinders or use Randomize.");
        String[] parts = text.split("[\\s,]+");
        for (int i = 0; i < parts.length; i++) {
            int c = Util.parseInt(parts[i], "Cylinder #" + (i + 1), 0);
            if (c > in.max) throw new IllegalArgumentException("Cylinder #" + (i + 1) + " (" + c + ") is more than the last cylinder (" + in.max + ").");
            in.reqs.add(c);
        }
        return in;
    }

    @Override
    protected void runSimulation() {
        Input in;
        try { in = read(); } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }
        DiskScheduler alg = (DiskScheduler) algo.getSelectedItem();
        DiskResult r = alg.schedule(in.head, in.reqs, dir.getSelectedIndex() == 0, in.max);   // polymorphic call
        steps.setRowCount(0);
        for (int i = 0; i < r.getSteps(); i++)
            steps.addRow(new Object[]{i + 1, r.getPath().get(i), r.getPath().get(i + 1), r.getMove(i)});
        totalValue.setText(r.getTotal() + " cylinders");
        avgValue.setText(String.format("%.2f", r.getAverage()));
        chart.show(r.getPath(), in.max);
    }

    private void compareAll() {
        Input in;
        try { in = read(); } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }
        compare.setRowCount(0);
        for (int i = 0; i < algo.getItemCount(); i++) {
            DiskResult r = algo.getItemAt(i).schedule(in.head, in.reqs, dir.getSelectedIndex() == 0, in.max);
            compare.addRow(new Object[]{r.getName(), r.getTotal(), String.format("%.2f", r.getAverage())});
        }
    }
}
