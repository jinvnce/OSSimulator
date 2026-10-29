import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;


public class BankerPanel extends ModulePanel {
    private static final Color LINE = Color.DARK_GRAY, HEADER = new Color(0xEE, 0xEE, 0xEE);

    private final JTextField np = new JTextField("5", 4), nr = new JTextField("4", 4), reqField = new JTextField("0 4 2 0", 12);
    private final JComboBox<String> reqProc = new JComboBox<>();
    private final JTable tbl = new JTable();
    private DefaultTableModel model;
    private int p = 0, m = 0;                 // current number of processes and resource types
    private boolean updating = false;         // true while the program (not the user) changes cells
    private final DefaultTableModel trace = Util.readOnlyModel(
            new String[]{"Step", "Round", "Process", "Need", "Work (free now)", "Need <= Work?", "What happens"});
    private final JTextArea result = new JTextArea();

    // column helpers: Process | Allocation | Max | Available | Need
    private int allocCol(int j) { return 1 + j; }
    private int maxCol(int j) { return 1 + m + j; }
    private int availCol(int j) { return 1 + 2 * m + j; }
    private int needCol(int j) { return 1 + 3 * m + j; }

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

        // ----- the slide-style table with a two-level header -----
        setupTable();
        JPanel header = new JPanel(new BorderLayout());
        header.add(new GroupBar(), BorderLayout.NORTH);
        header.add(tbl.getTableHeader(), BorderLayout.CENTER);
        JScrollPane tableScroll = new JScrollPane(tbl);
        tableScroll.setColumnHeaderView(header);
        tableScroll.setPreferredSize(new Dimension(500, 24 * 2 + 24 * 6 + 4));
        JLabel hint = new JLabel("<html><body style='width:760px'>Type <b>Allocation</b> and <b>Max</b>, and the starting <b>Available</b> on the P0 row. "
                + "Need = Max - Allocation is calculated. <b>Check Safety</b> fills the other Available cells: they show the "
                + "Work (resources free) when that process is checked.</body></html>");
        hint.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Banker's table"));
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        tablePanel.add(hint, BorderLayout.SOUTH);

        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JTable traceTable = new JTable(trace);
        traceTable.setRowHeight(22);
        traceTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        int[] w = {45, 55, 65, 95, 120, 95, 700};
        for (int i = 0; i < w.length; i++) traceTable.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < 6; i++) traceTable.getColumnModel().getColumn(i).setCellRenderer(center);
        JScrollPane resultScroll = Util.scroll(result, "Result");
        resultScroll.setPreferredSize(new Dimension(500, 120));
        JScrollPane traceScroll = Util.scroll(traceTable, "Safety algorithm, step by step");
        traceScroll.setPreferredSize(new Dimension(500, 190));
        JSplitPane bottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT, traceScroll, resultScroll);
        bottom.setResizeWeight(0.65);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, bottom);
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

    // ---------- table look ----------
    private void setupTable() {
        tbl.setRowHeight(24);
        tbl.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tbl.setShowGrid(true);
        tbl.setGridColor(Color.LIGHT_GRAY);
        tbl.setCellSelectionEnabled(true);
        tbl.getTableHeader().setReorderingAllowed(false);
        tbl.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(c == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBackground(HEADER);
                setForeground(Color.BLACK);
                setFont(getFont().deriveFont(Font.BOLD));
                setBorder(groupBorder(c, LINE));
                return this;
            }
        });
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                setHorizontalAlignment(c == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setFont(getFont().deriveFont(c == 0 ? Font.BOLD : Font.PLAIN));
                setBorder(groupBorder(c, LINE));
                return this;
            }
        });
        tbl.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnAdded(TableColumnModelEvent e) { tbl.getTableHeader().getParent().repaint(); }
            public void columnRemoved(TableColumnModelEvent e) { tbl.getTableHeader().getParent().repaint(); }
            public void columnMoved(TableColumnModelEvent e) { }
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                if (tbl.getTableHeader().getParent() != null) tbl.getTableHeader().getParent().repaint();
            }
            public void columnSelectionChanged(ListSelectionEvent e) { }
        });
    }

    /** A thick line on the left of the first column of each group (Allocation, Max, Available, Need). */
    private MatteBorder groupBorder(int c, Color color) {
        boolean first = m > 0 && (c == allocCol(0) || c == maxCol(0) || c == availCol(0) || c == needCol(0));
        return BorderFactory.createMatteBorder(0, first ? 2 : 0, 0, 0, color);
    }

    /** The blue top header row (Allocation / Max / Available / Need) that spans several columns. */
    private class GroupBar extends JComponent {
        GroupBar() { setPreferredSize(new Dimension(100, 24)); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            TableColumnModel cm = tbl.getColumnModel();
            String[] names = {"", "Allocation", "Max", "Available", "Need"};
            int[] starts = {0, 1, 1 + m, 1 + 2 * m, 1 + 3 * m, 1 + 4 * m};
            int x = 0;
            Graphics2D g2 = (Graphics2D) g;
            g2.setFont(getFont().deriveFont(Font.BOLD));
            FontMetrics fm = g2.getFontMetrics();
            for (int k = 0; k < 5; k++) {
                int w = 0;
                for (int c = starts[k]; c < starts[k + 1] && c < cm.getColumnCount(); c++) w += cm.getColumn(c).getWidth();
                g2.setColor(HEADER);
                g2.fillRect(x, 0, w, getHeight());
                g2.setColor(LINE);
                g2.drawLine(x, 0, x, getHeight());
                g2.drawString(names[k], x + (w - fm.stringWidth(names[k])) / 2, getHeight() / 2 + fm.getAscent() / 2 - 1);
                x += w;
            }
            g2.setColor(HEADER);
            g2.fillRect(x, 0, Math.max(0, getWidth() - x), getHeight());
        }
    }

    // ---------- table setup ----------
    private void generate() {
        int np2 = Util.parseInt(np.getText(), "Number of processes", 1);
        int nr2 = Util.parseInt(nr.getText(), "Number of resource types", 1);
        p = np2; m = nr2;
        String[] cols = new String[1 + 4 * m];
        cols[0] = "Process";
        for (int g = 0; g < 4; g++) for (int j = 0; j < m; j++) cols[1 + g * m + j] = Util.resourceName(j);
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int c) {
                return (c >= allocCol(0) && c < availCol(0)) || (row == 0 && c >= availCol(0) && c < needCol(0));
            }
        };
        for (int i = 0; i < p; i++) {
            Object[] row = new Object[cols.length];
            row[0] = "P" + i;
            for (int c = 1; c < cols.length; c++) row[c] = (c < availCol(0)) ? "0" : (c < needCol(0) ? (i == 0 ? "0" : "") : "0");
            model.addRow(row);
        }
        model.addTableModelListener(e -> {          // the user edited a cell: old workings are out of date
            if (updating) return;
            updating = true;
            clearWorking();
            computeLiveNeed();
            updating = false;
            tbl.repaint();
        });
        tbl.setModel(model);
        tbl.getColumnModel().getColumn(0).setMinWidth(60);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(60);
        for (int c = 1; c < model.getColumnCount(); c++) tbl.getColumnModel().getColumn(c).setPreferredWidth(40);
        reqProc.removeAllItems();
        for (int i = 0; i < p; i++) reqProc.addItem("P" + i);
        StringBuilder zeros = new StringBuilder();
        for (int j = 0; j < m; j++) zeros.append(j == 0 ? "" : " ").append("0");
        reqField.setText(zeros.toString());
        trace.setRowCount(0);
        result.setText(intro());
        tbl.getTableHeader().getParent().revalidate();
        tbl.getTableHeader().getParent().repaint();
    }

    private boolean generateSafely() {
        try { generate(); return true; }
        catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return false; }
    }

    @Override
    protected void randomizeData() {
        if (!generateSafely()) return;
        updating = true;
        for (int i = 0; i < p; i++)
            for (int j = 0; j < m; j++) {
                int mx = 1 + Util.RNG.nextInt(9);
                model.setValueAt("" + mx, i, maxCol(j));
                model.setValueAt("" + Util.RNG.nextInt(mx + 1), i, allocCol(j));
            }
        for (int j = 0; j < m; j++) model.setValueAt("" + Util.RNG.nextInt(6), 0, availCol(j));
        computeLiveNeed();
        updating = false;
        tbl.repaint();
    }

    /** The example from the lecture slides (5 processes, resources A-D). */
    private void loadSample() {
        np.setText("5"); nr.setText("4");
        generate();
        int[][] a = {{0, 0, 1, 2}, {1, 0, 0, 0}, {1, 3, 5, 4}, {0, 6, 3, 2}, {0, 0, 1, 4}};
        int[][] mx = {{0, 0, 1, 2}, {1, 7, 5, 0}, {2, 3, 5, 6}, {0, 6, 5, 2}, {0, 6, 5, 6}};
        int[] av = {1, 5, 2, 0};
        updating = true;
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 4; j++) { model.setValueAt("" + a[i][j], i, allocCol(j)); model.setValueAt("" + mx[i][j], i, maxCol(j)); }
        for (int j = 0; j < 4; j++) model.setValueAt("" + av[j], 0, availCol(j));
        computeLiveNeed();
        updating = false;
        reqProc.setSelectedIndex(1);
        reqField.setText("0 4 2 0");
        tbl.repaint();
    }

    // ---------- reading and showing state ----------
    private BankersAlgorithm readState() {
        Util.stopEditing(tbl);
        int[][] a = new int[p][m], mx = new int[p][m];
        int[] av = new int[m];
        for (int i = 0; i < p; i++)
            for (int j = 0; j < m; j++) {
                String where = "P" + i + " " + Util.resourceName(j);
                a[i][j] = Util.parseInt(Util.str(model.getValueAt(i, allocCol(j))), "Allocation of " + where, 0);
                mx[i][j] = Util.parseInt(Util.str(model.getValueAt(i, maxCol(j))), "Max of " + where, 0);
            }
        for (int j = 0; j < m; j++) av[j] = Util.parseInt(Util.str(model.getValueAt(0, availCol(j))), "Available " + Util.resourceName(j), 0);
        return new BankersAlgorithm(a, mx, av);
    }

    /** Fills Need as soon as Allocation and Max are valid numbers (blank otherwise). */
    private void computeLiveNeed() {
        for (int i = 0; i < p; i++)
            for (int j = 0; j < m; j++) {
                String need = "";
                try {
                    int a = Integer.parseInt(Util.str(model.getValueAt(i, allocCol(j))).trim());
                    int mx = Integer.parseInt(Util.str(model.getValueAt(i, maxCol(j))).trim());
                    if (a >= 0 && mx >= a) need = "" + (mx - a);
                } catch (NumberFormatException ignored) { }
                model.setValueAt(need, i, needCol(j));
            }
    }

    private void showNeed(BankersAlgorithm b) {
        for (int i = 0; i < b.getProcessCount(); i++) {
            int[] n = b.getNeed(i);
            for (int j = 0; j < n.length; j++) model.setValueAt("" + n[j], i, needCol(j));
        }
    }

    /** Removes the Work values and colours (keeps the starting Available typed on the P0 row). */
    private void clearWorking() {
        for (int i = 0; i < p; i++) {
            for (int j = 0; j < m; j++) {
                if (i > 0) model.setValueAt("", i, availCol(j));
            }
        }
    }

    /** Puts into each row the Work at the first moment that process is checked, like the lecture slide. */
    private void fillWorking(BankersAlgorithm b, BankersAlgorithm.SafetyResult s) {
        clearWorking();
        boolean[] done = new boolean[p];
        for (BankersAlgorithm.TraceRow t : s.trace) {
            int i = t.process;
            if (done[i]) continue;
            done[i] = true;
            for (int j = 0; j < m; j++) {
                if (i > 0) model.setValueAt("" + t.workBefore[j], i, availCol(j));
            }
        }
    }

    private void showTrace(BankersAlgorithm.SafetyResult s) {
        trace.setRowCount(0);
        int round = 1, prev = -1;
        for (BankersAlgorithm.TraceRow t : s.trace) {
            if (t.process <= prev) round++;
            prev = t.process;
            String P = "P" + t.process, need = BankersAlgorithm.format(t.need), work = BankersAlgorithm.format(t.workBefore);
            String text;
            if (t.ok) {
                text = P + " fits. It runs to the end and gives back " + BankersAlgorithm.format(alloc(t.process))
                        + ", so free becomes " + BankersAlgorithm.format(t.workAfter) + ".";
            } else {
                text = P + " needs more than is free (short on " + shortOn(t.need, t.workBefore) + "). Skip it for now.";
            }
            trace.addRow(new Object[]{t.step, round, P, need, work, t.ok ? "Yes" : "No (skip)", text});
        }
    }

    private int[] alloc(int i) {
        int[] a = new int[m];
        for (int j = 0; j < m; j++) a[j] = Integer.parseInt(Util.str(model.getValueAt(i, allocCol(j))).trim());
        return a;
    }

    private String shortOn(int[] need, int[] work) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < need.length; j++)
            if (need[j] > work[j]) sb.append(sb.length() == 0 ? "" : ", ").append(Util.resourceName(j));
        return sb.toString();
    }

    private String intro() {
        return "HOW THE SAFETY CHECK WORKS\n"
                + "1. Need = Max - Allocation: what each process may still ask for.\n"
                + "2. Work starts as Available: the resources free right now.\n"
                + "3. Find a process whose Need fits in Work. It can run to the end and then gives back its Allocation, so Work grows.\n"
                + "4. Repeat. If every process finishes, the state is SAFE. If none of the rest fits, it is NOT safe.\n\n"
                + "Click Check Safety to see it.";
    }

    private String verdict(BankersAlgorithm.SafetyResult s) {
        if (s.safe) return "RESULT: SYSTEM IS SAFE.\nSafe sequence: " + s.sequenceText()
                + "\n(Every process can finish if they run in this order.)";
        StringBuilder sb = new StringBuilder("RESULT: SYSTEM IS NOT SAFE (possible deadlock).\nThese processes cannot finish:");
        for (int i : s.stuck) sb.append(" P").append(i);
        return sb.append("\n(Each of them needs more than what is free, and no other process can give resources back.)").toString();
    }

    // ---------- actions ----------
    @Override
    protected void runSimulation() {
        BankersAlgorithm b;
        try { b = readState(); } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }
        BankersAlgorithm.SafetyResult s = b.checkSafety();
        updating = true;
        showNeed(b);
        fillWorking(b, s);
        updating = false;
        showTrace(s);
        result.setText("Work = Available = " + BankersAlgorithm.format(b.getAvailable())
                + "\nWe look for a process whose Need fits in Work, let it finish, and add its Allocation to Work.\n"
                + "The Available column shows the Work at the moment each process is checked. "
                + "The step-by-step table lists every check in order.\n\n" + verdict(s));
        result.setCaretPosition(0);
        tbl.repaint();
    }

    private void requestResources() {
        BankersAlgorithm b;
        BankersAlgorithm.RequestResult res;
        try {
            b = readState();
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
        updating = true;
        if (res.status == BankersAlgorithm.Status.GRANTED) {
            BankersAlgorithm a = res.after;                      // apply the new state to the table
            for (int i = 0; i < a.getProcessCount(); i++) {
                int[] al = a.getAllocation(i);
                for (int j = 0; j < al.length; j++) model.setValueAt("" + al[j], i, allocCol(j));
            }
            int[] av = a.getAvailable();
            for (int j = 0; j < av.length; j++) model.setValueAt("" + av[j], 0, availCol(j));
            showNeed(a);
            fillWorking(a, res.safety);
        } else { showNeed(b); clearWorking(); }
        updating = false;
        if (res.safety != null) showTrace(res.safety); else trace.setRowCount(0);
        result.setText(sb.toString());
        result.setCaretPosition(0);
        tbl.repaint();
    }
}
