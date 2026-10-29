import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Banker's Algorithm tab: input tables, safety trace (like the lecture example) and resource requests. */
public class BankerPanel extends ModulePanel {
    private final JTextField np = new JTextField("5", 4), nr = new JTextField("4", 4), reqField = new JTextField("0 4 2 0", 12);
    private final JComboBox<String> reqProc = new JComboBox<>();
    private final JTable ta = new JTable(), tm = new JTable(), tn = new JTable(), tv = new JTable();
    private DefaultTableModel alloc, max, need, avail;
    private final DefaultTableModel trace = Util.readOnlyModel(
            new String[]{"Step", "Process", "Need", "Work (Available)", "Need <= Work?", "New Work"});
    private final JTextArea result = new JTextArea();

    public BankerPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new GridLayout(2, 1));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        JButton gen = new JButton("Generate Tables"), rnd = new JButton("Randomize"),
                sample = new JButton("Load Sample"), chk = new JButton("Check Safety");
        r1.add(new JLabel("Processes (n):")); r1.add(np);
        r1.add(new JLabel("Resource types (m):")); r1.add(nr);
        r1.add(gen); r1.add(rnd); r1.add(sample); r1.add(chk);
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        JButton req = new JButton("Request Resources");
        r2.add(new JLabel("Resource request:  Process")); r2.add(reqProc);
        r2.add(new JLabel("Request (one value per resource, e.g. 0 4 2 0):")); r2.add(reqField); r2.add(req);
        top.add(r1); top.add(r2);
        add(top, BorderLayout.NORTH);

        JPanel left = new JPanel(new GridLayout(4, 1, 4, 4));
        left.add(Util.scroll(ta, "Allocation"));
        left.add(Util.scroll(tm, "Max"));
        left.add(Util.scroll(tn, "Need = Max - Allocation (calculated)"));
        left.add(Util.scroll(tv, "Available"));

        result.setEditable(false);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Util.scroll(new JTable(trace), "Safety algorithm trace"), Util.scroll(result, "Result"));
        right.setResizeWeight(0.55);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.4);
        add(split, BorderLayout.CENTER);

        gen.addActionListener(e -> generateSafely());
        rnd.addActionListener(e -> randomizeData());
        sample.addActionListener(e -> loadSample());
        chk.addActionListener(e -> runSimulation());
        req.addActionListener(e -> requestResources());
        generate();
    }

    @Override public String getTitle() { return "Banker's Algorithm"; }

    // ---------- table setup ----------
    private DefaultTableModel matrix(int p, int r, boolean editable) {
        String[] cols = new String[r + 1];
        cols[0] = "Process";
        for (int j = 0; j < r; j++) cols[j + 1] = Util.resourceName(j);
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int c) { return editable && c > 0; }
        };
        for (int i = 0; i < p; i++) {
            Object[] row = new Object[r + 1];
            row[0] = "P" + i;
            for (int j = 1; j <= r; j++) row[j] = "0";
            m.addRow(row);
        }
        return m;
    }

    private void generate() {
        int p = Util.parseInt(np.getText(), "Number of processes", 1);
        int r = Util.parseInt(nr.getText(), "Number of resource types", 1);
        alloc = matrix(p, r, true);
        max = matrix(p, r, true);
        need = matrix(p, r, false);
        String[] cols = new String[r];
        for (int j = 0; j < r; j++) cols[j] = Util.resourceName(j);
        avail = new DefaultTableModel(cols, 1);
        for (int j = 0; j < r; j++) avail.setValueAt("0", 0, j);
        ta.setModel(alloc); tm.setModel(max); tn.setModel(need); tv.setModel(avail);
        reqProc.removeAllItems();
        for (int i = 0; i < p; i++) reqProc.addItem("P" + i);
        trace.setRowCount(0);
        result.setText("");
    }

    private boolean generateSafely() {
        try { generate(); return true; }
        catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return false; }
    }

    @Override
    protected void randomizeData() {
        if (!generateSafely()) return;
        int p = alloc.getRowCount(), r = avail.getColumnCount();
        for (int i = 0; i < p; i++)
            for (int j = 0; j < r; j++) {
                int m = 1 + Util.RNG.nextInt(9);
                max.setValueAt("" + m, i, j + 1);
                alloc.setValueAt("" + Util.RNG.nextInt(m + 1), i, j + 1);
            }
        for (int j = 0; j < r; j++) avail.setValueAt("" + Util.RNG.nextInt(6), 0, j);
    }

    /** The example from the lecture slides (5 processes, resources A-D). */
    private void loadSample() {
        np.setText("5"); nr.setText("4");
        generate();
        int[][] a = {{0, 0, 1, 2}, {1, 0, 0, 0}, {1, 3, 5, 4}, {0, 6, 3, 2}, {0, 0, 1, 4}};
        int[][] m = {{0, 0, 1, 2}, {1, 7, 5, 0}, {2, 3, 5, 6}, {0, 6, 5, 2}, {0, 6, 5, 6}};
        int[] av = {1, 5, 2, 0};
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 4; j++) { alloc.setValueAt("" + a[i][j], i, j + 1); max.setValueAt("" + m[i][j], i, j + 1); }
        for (int j = 0; j < 4; j++) avail.setValueAt("" + av[j], 0, j);
        reqProc.setSelectedIndex(1);
        reqField.setText("0 4 2 0");
    }

    // ---------- reading and showing state ----------
    private BankersAlgorithm readState() {
        Util.stopEditing(ta); Util.stopEditing(tm); Util.stopEditing(tv);
        int p = alloc.getRowCount(), r = avail.getColumnCount();
        int[][] a = new int[p][r], m = new int[p][r];
        int[] av = new int[r];
        for (int i = 0; i < p; i++)
            for (int j = 0; j < r; j++) {
                String where = "P" + i + " " + Util.resourceName(j);
                a[i][j] = Util.parseInt(Util.str(alloc.getValueAt(i, j + 1)), "Allocation of " + where, 0);
                m[i][j] = Util.parseInt(Util.str(max.getValueAt(i, j + 1)), "Max of " + where, 0);
            }
        for (int j = 0; j < r; j++) av[j] = Util.parseInt(Util.str(avail.getValueAt(0, j)), "Available " + Util.resourceName(j), 0);
        return new BankersAlgorithm(a, m, av);
    }

    private void showNeed(BankersAlgorithm b) {
        for (int i = 0; i < b.getProcessCount(); i++) {
            int[] n = b.getNeed(i);
            for (int j = 0; j < n.length; j++) need.setValueAt("" + n[j], i, j + 1);
        }
    }

    private void showTrace(BankersAlgorithm.SafetyResult s) {
        trace.setRowCount(0);
        for (BankersAlgorithm.TraceRow t : s.trace)
            trace.addRow(new Object[]{t.step, "P" + t.process, BankersAlgorithm.format(t.need),
                    BankersAlgorithm.format(t.workBefore), t.ok ? "Yes" : "No (skip)",
                    t.ok ? BankersAlgorithm.format(t.workAfter) : BankersAlgorithm.format(t.workBefore)});
    }

    private String verdict(BankersAlgorithm.SafetyResult s) {
        if (s.safe) return "SYSTEM IS SAFE.\nSafe sequence: " + s.sequenceText();
        StringBuilder sb = new StringBuilder("SYSTEM IS NOT SAFE (possible deadlock).\nProcesses that cannot finish:");
        for (int i : s.stuck) sb.append(" P").append(i);
        return sb.toString();
    }

    // ---------- actions ----------
    @Override
    protected void runSimulation() {
        BankersAlgorithm b;
        try { b = readState(); } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }
        BankersAlgorithm.SafetyResult s = b.checkSafety();
        showNeed(b);
        showTrace(s);
        result.setText("Safety algorithm\nWork = Available = " + BankersAlgorithm.format(b.getAvailable())
                + "\nRepeat: find an unfinished Pi with Need <= Work, then Work = Work + Allocation.\n\n" + verdict(s));
        result.setCaretPosition(0);
    }

    private void requestResources() {
        BankersAlgorithm b;
        BankersAlgorithm.RequestResult res;
        try {
            b = readState();
            int m = avail.getColumnCount();
            String text = reqField.getText().trim();
            if (text.isEmpty()) throw new IllegalArgumentException("Request is blank. Enter one value per resource type.");
            String[] parts = text.split("[\\s,]+");
            if (parts.length != m) throw new IllegalArgumentException("Request needs exactly " + m + " values (got " + parts.length + ").");
            int[] req = new int[m];
            for (int j = 0; j < m; j++) req[j] = Util.parseInt(parts[j], "Request " + Util.resourceName(j), 0);
            res = b.request(reqProc.getSelectedIndex(), req);
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }

        StringBuilder sb = new StringBuilder();
        for (String line : res.log) sb.append(line).append('\n');
        if (res.status == BankersAlgorithm.Status.GRANTED) {
            BankersAlgorithm a = res.after;                      // apply the new state to the tables
            for (int i = 0; i < a.getProcessCount(); i++) {
                int[] al = a.getAllocation(i);
                for (int j = 0; j < al.length; j++) alloc.setValueAt("" + al[j], i, j + 1);
            }
            int[] av = a.getAvailable();
            for (int j = 0; j < av.length; j++) avail.setValueAt("" + av[j], 0, j);
            showNeed(a);
        } else showNeed(b);
        if (res.safety != null) showTrace(res.safety); else trace.setRowCount(0);
        result.setText(sb.toString());
        result.setCaretPosition(0);
    }
}
